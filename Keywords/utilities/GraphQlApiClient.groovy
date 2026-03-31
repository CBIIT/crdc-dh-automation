package utilities

import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.testdata.TestData
import com.kms.katalon.core.testdata.TestDataFactory
import com.kms.katalon.core.util.KeywordUtil
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import internal.GlobalVariable

/**
 * HTTP GraphQL client for CRDC Data Hub API (same endpoint and token as CLI: ApiUrl, SubmitterApiToken).
 * Metadata batch uploads after createBatch use HTTP PUT of local TSV bytes to presigned S3 URLs (no Hub auth on S3).
 *
 * GraphQL document bodies live in test data CRDC/API/GraphQLOperations (Excel InputFiles/CRDC/API/GraphQLOperations.xlsx,
 * sheet Operations, columns operationKey, queryInline).
 *
 * Most ValidationReport rows and domain assertions on mutation/query payloads are in verifyAndUnpack* methods.
 * post() and some operation keywords (e.g. createBatch) add transport or call-site checks (HTTP, variables) before/after the call.
 */
class GraphQlApiClient {

	/** Section title for ValidationReport rows (same pattern as DataSubmissions tab sections). */
	private static final String VALIDATION_REPORT_SECTION = "API / GraphQL"

	private static final String GRAPHQL_OPERATIONS_DATA = "CRDC/API/GraphQLOperations"

	private static final Map<String, String> graphqlDocumentCache = [:]

	/** Content-Type for metadata TSV PUT to presigned S3 URLs (aligned with Data Hub UI). */
	private static final String PRESIGNED_TSV_CONTENT_TYPE = "text/tab-separated-values"

	/**
	 * HTTP PUT entire file body to presignedUrl. No Authorization header. Succeeds on HTTP 2xx.
	 */
	private void putTsvToPresignedUrl(File file, String presignedUrl) {
		if (file == null || !file.isFile()) {
			KeywordUtil.markFailed("GraphQlApiClient.putTsvToPresignedUrl: file is missing or not a regular file: ${file}")
			return
		}
		String url = presignedUrl?.trim()
		if (!url) {
			KeywordUtil.markFailed("GraphQlApiClient.putTsvToPresignedUrl: presignedUrl is required.")
			return
		}
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()
		try {
			conn.setRequestMethod("PUT")
			conn.setRequestProperty("Content-Type", PRESIGNED_TSV_CONTENT_TYPE)
			conn.setDoOutput(true)
			byte[] bytes = file.getBytes()
			conn.outputStream.withCloseable { output ->
				output.write(bytes)
				output.flush()
			}
			int code = conn.responseCode
			InputStream stream = code >= 400 ? conn.errorStream : conn.inputStream
			String text = ""
			if (stream != null) {
				stream.withCloseable { input ->
					text = input.getText("UTF-8")
				}
			}
			if (code < 200 || code >= 300) {
				KeywordUtil.markFailed("GraphQlApiClient.putTsvToPresignedUrl: HTTP ${code} for ${file.name}. Body: ${text}")
			}
		} finally {
			conn.disconnect()
		}
	}

	/**
	 * Load a GraphQL document by operationKey from CRDC/API/GraphQLOperations; result is cached per JVM.
	 */
	private static String loadGraphQlDocument(String operationKey) {
		// Resolve query string from Katalon test data (Excel) by operationKey.
		if (!operationKey?.toString()?.trim()) {
			KeywordUtil.markFailed("GraphQlApiClient.loadGraphQlDocument: operationKey is required.")
			return null
		}
		String key = operationKey.toString().trim()
		if (graphqlDocumentCache.containsKey(key)) {
			return graphqlDocumentCache.get(key)
		}
		// Load from Katalon TestData entity (Excel path in .dat).
		TestData data = TestDataFactory.findTestData(GRAPHQL_OPERATIONS_DATA)
		int rows = data.getRowNumbers()
		// Scan Operations sheet for matching operationKey and non-empty queryInline.
		for (int r = 1; r <= rows; r++) {
			String rowKey = data.getValue("operationKey", r)?.toString()?.trim()
			if (rowKey == key) {
				String q = data.getValue("queryInline", r)?.toString()?.trim()
				if (!q) {
					KeywordUtil.markFailed("GraphQlApiClient.loadGraphQlDocument: empty queryInline for operationKey '${key}'.")
				}
				graphqlDocumentCache.put(key, q)
				return q
			}
		}
		KeywordUtil.markFailed("GraphQlApiClient.loadGraphQlDocument: no row for operationKey '${key}' in ${GRAPHQL_OPERATIONS_DATA}.")
	}

	private void requireApiConfig() {
		// Execution profile must define GraphQL endpoint and API JWT.
		boolean hasError = false
		if (!GlobalVariable.ApiUrl || GlobalVariable.ApiUrl.toString().trim().isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient: ApiUrl is not set in profile.")
			hasError = true
		}
		if (!GlobalVariable.SubmitterApiToken || GlobalVariable.SubmitterApiToken.toString().trim().isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient: SubmitterApiToken is not set in profile.")
			hasError = true
		}
		if (hasError) {
			KeywordUtil.markFailedAndStop("GraphQlApiClient: required API configuration is missing in profile; see previous failures for details.")
		}
	}

	private static String normalizeBearerToken(String token) {
		// Strip optional "Bearer " prefix so Authorization header is consistent.
		String t = token?.toString()?.trim() ?: ""
		if (t.toLowerCase().startsWith("bearer ")) {
			t = t.substring(7).trim()
		}
		return t
	}

	/**
	 * Project-relative folder for CRDCDH-1526 API metadata TSVs (InputFiles/CRDC/API).
	 */
	@Keyword
	String defaultApiInputDir() {
		// Standard folder for API metadata TSVs.
		return RunConfiguration.getProjectDir() + "/InputFiles/CRDC/API"
	}

	/**
	 * Fail if any fileBasenames is missing under directory (regular file). Surfaces absolute paths for checkout/OSS issues.
	 */
	@Keyword
	void verifyLocalFilesExist(String directory, List fileBasenames) {
		String dir = directory?.toString()?.trim()
		if (!dir) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyLocalFilesExist: directory is required.")
			return
		}
		if (!(fileBasenames instanceof List) || fileBasenames.isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyLocalFilesExist: fileBasenames must be a non-empty list.")
			return
		}
		fileBasenames.each { def name ->
			String basename = name?.toString()?.trim()
			if (!basename) {
				KeywordUtil.markFailed("GraphQlApiClient.verifyLocalFilesExist: each basename must be non-empty.")
			}
			File f = new File(dir, basename)
			// Each basename must exist as a file; report Fail then abort on first missing.
			if (!f.isFile()) {
				ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Local metadata file: ${basename}", "file exists",
					"missing: ${f.absolutePath}", "Fail")
				KeywordUtil.markFailed("Missing input file for metadata upload: ${f.absolutePath}")
			} else {
				ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Local metadata file: ${basename}", "file exists",
					f.absolutePath, "Pass")
			}
		}
	}

	/**
	 * newBatch maps to GraphQL NewBatch; require non-empty files for presigned uploads.
	 */
	@Keyword
	List requireNonEmptyNewBatchFiles(Map newBatch) {
		// Gate before S3 uploads: createBatch payload must include file rows (signedURL list).
		List rows = newBatch?.get('files') as List
		if (!rows || rows.isEmpty()) {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "createBatch presigned file rows", "non-empty",
				"(empty)", "Fail")
			KeywordUtil.markFailed("createBatch returned no files with signedURL; cannot upload metadata.")
			return rows
		}
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "createBatch presigned file rows", "non-empty",
			String.valueOf(rows.size()) + " row(s)", "Pass")
		return rows
	}

	/**
	 * Verify createSubmission response fields, report to ValidationReport, and return data.createSubmission.
	 * @param result Full parsed GraphQL body from createSubmission (or equivalent).
	 */
	@Keyword
	Map verifyAndUnpackCreateSubmission(Map result) {
		Map payload = (result?.data as Map)?.get('createSubmission') as Map
		// Verify GraphQL envelope contains data.createSubmission; fail keyword if missing.
		if (!payload) {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "data.createSubmission", "present in response",
				"missing", "Fail")
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackCreateSubmission: missing data.createSubmission in ${result}")
		}
		String id = payload?.get('_id')?.toString() ?: ""
		String st = payload?.get('status')?.toString() ?: ""
		// Verify submission _id returned and non-empty; hard-fail below if empty.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Submission _id present", "non-empty",
			id?.trim() ?: "(empty)", id?.trim() ? "Pass" : "Fail")
		// Verify status field present and non-empty.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Submission status", "non-empty",
			st?.trim() ?: "(empty)", st?.trim() ? "Pass" : "Fail")
		boolean statusIsNew = st != null && "New".equalsIgnoreCase(st.trim())
		// Verify new submission workflow: status must be New.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Submission status is New", "New",
			st?.trim() ?: "(empty)", statusIsNew ? "Pass" : "Fail")
		if (!id?.trim()) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackCreateSubmission: submission _id is empty.")
		}
		if (!statusIsNew) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackCreateSubmission: expected status New, got: ${st}")
		}
		return payload
	}

	/**
	 * Verify createBatch response fields, report to ValidationReport, and return data.createBatch.
	 * @param result Full parsed GraphQL body from createBatch (or equivalent).
	 */
	@Keyword
	Map verifyAndUnpackCreateBatch(Map result) {
		Map payload = (result?.data as Map)?.get('createBatch') as Map
		// Verify GraphQL envelope contains data.createBatch; fail keyword if missing.
		if (!payload) {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "data.createBatch", "present in response",
				"missing", "Fail")
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackCreateBatch: missing data.createBatch in ${result}")
		}
		String batchId = payload?.get('_id')?.toString() ?: ""
		// Verify batch _id returned and non-empty.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Batch _id present", "non-empty",
			batchId?.trim() ?: "(empty)", batchId?.trim() ? "Pass" : "Fail")
		if (!batchId?.trim()) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackCreateBatch: batch _id is empty.")
		}
		List filesList = payload?.get('files') as List
		int filesSize = filesList ? filesList.size() : 0
		String fcActual = payload?.get('fileCount') != null ? String.valueOf(payload.get('fileCount')) : "(missing)"
		String fcExpectedFromRows = String.valueOf(filesSize)
		boolean countOk = fcActual == fcExpectedFromRows
		// Verify fileCount matches length of files array from API (internal consistency).
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Batch fileCount vs files list", "fileCount equals files.size()",
			"fileCount=${fcActual}, files.size()=${fcExpectedFromRows}", countOk ? "Pass" : "Fail")
		if (!countOk) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackCreateBatch: fileCount ${fcActual} does not match files list size ${fcExpectedFromRows}")
		}
		if (filesList) {
			for (Object fo : filesList) {
				Map fr = fo as Map
				String fn = fr?.get('fileName')?.toString() ?: "(unknown)"
				String su = fr?.get('signedURL')?.toString()?.trim() ?: ""
				// Verify each file row has a usable presigned URL for S3 upload.
				boolean urlOk = su.length() > 0 && (su.startsWith("https://") || su.startsWith("http://"))
				String urlSummary = urlOk ? "(non-empty URL)" : su.length() > 0 ? "(invalid scheme)" : "(empty)"
				ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "createBatch signedURL: ${fn}", "non-empty http(s) URL",
					urlSummary, urlOk ? "Pass" : "Fail")
				if (!urlOk) {
					KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackCreateBatch: file '${fn}' has invalid or missing signedURL (${urlSummary})")
				}
			}
		}
		return payload
	}

	/**
	 * Verify updateBatch response fields, report to ValidationReport, and return data.updateBatch.
	 * @param result Full parsed GraphQL body from updateBatch (or equivalent).
	 * @param expectedBatchId Batch _id from the request; pass null or empty to skip _id equality check.
	 */
	@Keyword
	Map verifyAndUnpackUpdateBatch(Map result, String expectedBatchId = null) {
		Map payload = (result?.data as Map)?.get('updateBatch') as Map
		// Verify GraphQL envelope contains data.updateBatch; fail keyword if missing.
		if (!payload) {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "data.updateBatch", "present in response",
				"missing", "Fail")
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackUpdateBatch: missing data.updateBatch in ${result}")
		}
		String expBid = expectedBatchId?.toString()?.trim()
		if (expBid) {
			String pBid = payload?.get('_id')?.toString()?.trim() ?: ""
			// When expectedBatchId passed, verify response batch _id matches request.
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "updateBatch batch _id matches request", expBid,
				pBid ?: "(empty)", expBid == pBid ? "Pass" : "Fail")
			if (expBid != pBid) {
				KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackUpdateBatch: expected batch _id '${expBid}' but got '${pBid}'")
			}
		}
		String pSub = payload?.get('submissionID')?.toString()?.trim() ?: ""
		// Verify parent submission id present on batch node.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "updateBatch response submissionID", "non-empty",
			pSub ?: "(empty)", pSub ? "Pass" : "Fail")
		String pType = payload?.get('type')?.toString()?.trim() ?: ""
		// Verify batch type (metadata vs file) present.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "updateBatch response type", "non-empty",
			pType ?: "(empty)", pType ? "Pass" : "Fail")
		String pFc = payload?.get('fileCount') != null ? String.valueOf(payload.get('fileCount')) : "(missing)"
		// Verify fileCount field present on batch.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "updateBatch response fileCount", "present", pFc,
			pFc != "(missing)" ? "Pass" : "Fail")
		String finalBatchStatus = payload?.get('status')?.toString() ?: ""
		// Verify batch-level status string present (e.g. Uploading / Uploaded).
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Final batch status", "non-empty",
			finalBatchStatus?.trim() ?: "(empty)", finalBatchStatus?.trim() ? "Pass" : "Fail")
		List updFiles = payload?.get('files') as List
		if (updFiles && !updFiles.isEmpty()) {
			for (Object rowObj : updFiles) {
				Map f = rowObj as Map
				String fn = f?.get('fileName')?.toString() ?: "(unknown)"
				String st = f?.get('status')?.toString() ?: ""
				Object errs = f?.get('errors')
				boolean noErrors = errs == null || (errs instanceof List && ((List) errs).isEmpty())
				String errPart = errs != null ? errs.toString() : ""
				Object szObj = f.get('size')
				String sizeSegment = ""
				if (f.containsKey('size')) {
					sizeSegment = (szObj == null) ? ", size=null" : ", size=" + szObj.toString()
				}
				String actualFile = "status=${st}${sizeSegment}, errors=${errPart}"
				boolean fileOk = st?.trim() && noErrors
				// Per file: non-empty status and empty errors; size only logged (null allowed).
				ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "File after updateBatch: ${fn}",
					"status set, no errors; size may be null", actualFile, fileOk ? "Pass" : "Fail")
				if (!fileOk) {
					KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackUpdateBatch: file '${fn}' failed validation (${actualFile})")
				}
			}
		} else {
			// Batch must return at least one file row after update.
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Batch files after updateBatch", "non-empty list",
				"(missing or empty)", "Fail")
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackUpdateBatch: files list is missing or empty in updateBatch response")
		}
		return payload
	}

	/**
	 * Verify listBatches response fields, report to ValidationReport, and return data.listBatches.
	 * @param result Full parsed GraphQL body from listBatches (or equivalent).
	 * @param expectedBatchType e.g. metadata; pass null or empty to skip type equality check.
	 */
	@Keyword
	Map verifyAndUnpackListBatches(Map result, String expectedSubmissionId, String expectedBatchId, List expectedFileNames,
			String expectedBatchType = null) {
		Map payload = (result?.data as Map)?.get('listBatches') as Map
		// Verify GraphQL envelope contains data.listBatches; fail keyword if missing.
		if (!payload) {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "data.listBatches", "present in response",
				"missing", "Fail")
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: missing data.listBatches in ${result}")
		}
		String expSub = expectedSubmissionId?.toString()?.trim()
		String expBid = expectedBatchId?.toString()?.trim()
		// Preconditions: caller must supply submission id, batch id, and non-empty expected file name list.
		if (!expSub || !expBid) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: expectedSubmissionId and expectedBatchId are required.")
		}
		if (!(expectedFileNames instanceof List) || expectedFileNames.isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: expectedFileNames must be a non-empty list.")
		}
		List<String> expFiles = expectedFileNames.collect { it?.toString()?.trim() }.findAll { it }
		if (expFiles.size() != expectedFileNames.size()) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: each expected file name must be non-empty.")
		}
		Object totalObj = payload.get('total')
		String totalStr = totalObj != null ? totalObj.toString() : "(missing)"
		int totalVal = 0
		if (totalObj != null && totalObj.toString().trim().isNumber()) {
			totalVal = totalObj.toInteger()
		}
		boolean totalOk = totalObj != null && totalVal >= 1
		// Verify paginated total count is present and at least one batch exists for the submission.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches total", ">= 1",
			totalStr, totalOk ? "Pass" : "Fail")
		List batches = payload.get('batches') as List
		boolean batchesNonEmpty = batches && !batches.isEmpty()
		// Verify batches array is non-empty (page includes rows).
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batches array", "non-empty",
			batchesNonEmpty ? String.valueOf(batches.size()) + " batch(es)" : "(missing or empty)",
			batchesNonEmpty ? "Pass" : "Fail")
		if (!totalOk || !batchesNonEmpty) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: total or batches invalid.")
		}
		Map matched = null
		// Locate the batch created in the test by _id (within first page / request window).
		for (Object bObj : batches) {
			Map b = bObj as Map
			String bid = b?.get('_id')?.toString()?.trim() ?: ""
			if (bid && bid == expBid) {
				matched = b
				break
			}
		}
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches contains expected batch _id", expBid,
			matched ? "found" : "not found", matched ? "Pass" : "Fail")
		if (!matched) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: no batch with _id ${expBid} in listBatches.")
		}
		String pSub = matched?.get('submissionID')?.toString()?.trim() ?: ""
		boolean subOk = expSub == pSub
		// Verify batch row is tied to the expected submission.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch submissionID", expSub,
			pSub ?: "(empty)", subOk ? "Pass" : "Fail")
		if (!subOk) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: submissionID mismatch.")
		}
		String expType = expectedBatchType?.toString()?.trim()
		if (expType) {
			String pType = matched?.get('type')?.toString()?.trim() ?: ""
			// Optional: when expectedBatchType set, verify batch type (e.g. metadata).
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch type", expType,
				pType ?: "(empty)", expType == pType ? "Pass" : "Fail")
			if (expType != pType) {
				KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: expected type ${expType}, got ${pType}")
			}
		}
		int expFc = expFiles.size()
		Object fcObj = matched.get('fileCount')
		String fcActual = fcObj != null ? fcObj.toString() : "(missing)"
		boolean fcOk = fcObj != null && fcObj.toString().trim().isNumber() && fcObj.toInteger() == expFc
		// Verify fileCount matches number of expected upload files for this test.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch fileCount", String.valueOf(expFc),
			fcActual, fcOk ? "Pass" : "Fail")
		if (!fcOk) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: fileCount expected ${expFc}, got ${fcActual}")
		}
		String bStat = matched?.get('status')?.toString()?.trim() ?: ""
		// Verify batch-level status string present.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch status", "non-empty",
			bStat ?: "(empty)", bStat ? "Pass" : "Fail")
		if (!bStat) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: batch status is empty.")
		}
		Object bErrs = matched.get('errors')
		boolean batchNoErrs = bErrs == null || (bErrs instanceof List && ((List) bErrs).isEmpty())
		// Verify no batch-level error strings returned.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch errors", "empty or absent",
			bErrs != null ? bErrs.toString() : "(absent)", batchNoErrs ? "Pass" : "Fail")
		if (!batchNoErrs) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: batch has errors: ${bErrs}")
		}
		Object disp = matched.get('displayID')
		boolean dispOk = disp != null && disp.toString().trim()
		// Verify human-visible batch display id present.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch displayID", "present",
			disp != null ? disp.toString() : "(missing)", dispOk ? "Pass" : "Fail")
		if (!dispOk) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: batch displayID missing.")
		}
		String cr = matched?.get('createdAt')?.toString()?.trim() ?: ""
		String up = matched?.get('updatedAt')?.toString()?.trim() ?: ""
		// Verify audit timestamps returned for the batch row.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch createdAt", "non-empty",
			cr ?: "(empty)", cr ? "Pass" : "Fail")
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch updatedAt", "non-empty",
			up ?: "(empty)", up ? "Pass" : "Fail")
		if (!cr || !up) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: batch createdAt or updatedAt empty.")
		}
		String submName = matched?.get('submitterName')?.toString()?.trim() ?: ""
		// Verify submitter display name populated (Upload Activity parity with UI).
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch submitterName", "non-empty",
			submName ?: "(empty)", submName ? "Pass" : "Fail")
		if (!submName) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: submitterName empty.")
		}
		List batchFiles = matched.get('files') as List
		boolean filesOk = batchFiles && !batchFiles.isEmpty()
		// Verify nested files list exists for the batch.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch files array", "non-empty",
			filesOk ? String.valueOf(batchFiles.size()) + " file(s)" : "(missing or empty)", filesOk ? "Pass" : "Fail")
		if (!filesOk) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: batch files list missing or empty.")
		}
		for (String expFn : expFiles) {
			Map found = null
			// For each expected uploaded file name, find matching FileInfo in batch.files.
			for (Object fo : batchFiles) {
				Map fm = fo as Map
				String fn = fm?.get('fileName')?.toString()?.trim() ?: ""
				if (fn == expFn) {
					found = fm
					break
				}
			}
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches file present: ${expFn}", "found",
				found ? "found" : "missing", found ? "Pass" : "Fail")
			if (!found) {
				KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: file ${expFn} not in batch.files.")
			}
			String st = found?.get('status')?.toString()?.trim() ?: ""
			Object ferrs = found.get('errors')
			// Per-file: status + errors; size/log line follows (null size allowed; numeric size must be > 0).
			boolean fNoErrs = ferrs == null || (ferrs instanceof List && ((List) ferrs).isEmpty())
			String ferrPart = ferrs != null ? ferrs.toString() : ""
			Object szObj = found.get('size')
			boolean sizePass = true
			String sizeActual
			if (!found.containsKey('size')) {
				sizeActual = "(omitted)"
				sizePass = true
			} else if (szObj == null) {
				sizeActual = "null"
				sizePass = true
			} else {
				double szNum
				try {
					szNum = szObj instanceof Number ? ((Number) szObj).doubleValue() : Double.parseDouble(szObj.toString())
				} catch (Exception e) {
					szNum = -1
				}
				sizePass = szNum > 0
				sizeActual = szObj.toString()
			}
			String nodeT = found.containsKey('nodeType') && found.get('nodeType') != null
				? found.get('nodeType').toString()
				: found.containsKey('nodeType') ? "null" : "(omitted)"
			String fpfx = found?.get('filePrefix')?.toString() ?: ""
			String detail = "status=${st}, nodeType=${nodeT}, filePrefix=${fpfx}, size=${sizeActual}, errors=${ferrPart}"
			boolean fileRowOk = st && fNoErrs && sizePass
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches file: ${expFn}",
				"status set, no errors; size may be null (if returned, must be > 0)", detail, fileRowOk ? "Pass" : "Fail")
			if (!st || !fNoErrs) {
				KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: file ${expFn} status/errors invalid.")
			}
			if (!sizePass) {
				KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: file ${expFn} size present but not > 0.")
			}
		}
		return payload
	}

	/**
	 * POST a GraphQL operation to GlobalVariable.ApiUrl with Authorization: Bearer SubmitterApiToken.
	 * @param operationName May be null for anonymous operations (not recommended)
	 * @param variables Map of GraphQL variables (empty map if none)
	 * @return Parsed JSON body as Map (keys typically data, errors)
	 */
	@Keyword
	Map post(String operationName, String query, Map variables) {
		requireApiConfig()
		URL url = new URL(GlobalVariable.ApiUrl.toString().trim())
		HttpURLConnection conn = (HttpURLConnection) url.openConnection()
		InputStream stream = null
		try {
			// Single GraphQL POST: JSON body with query, optional operationName, variables.
			conn.setRequestMethod("POST")
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
			String jwt = normalizeBearerToken(GlobalVariable.SubmitterApiToken.toString())
			conn.setRequestProperty("Authorization", "Bearer " + jwt)
			conn.setDoOutput(true)

			Map payload = [query: query]
			if (operationName != null && !operationName.isEmpty()) {
				payload.operationName = operationName
			}
			payload.variables = variables != null ? variables : [:]
			String body = JsonOutput.toJson(payload)

			conn.outputStream.withWriter("UTF-8") { Writer w -> w << body }

			int code = conn.responseCode
			stream = code >= 400 ? conn.errorStream : conn.inputStream
			String text = stream ? stream.getText("UTF-8") : ""

			String httpActual = String.valueOf(code)
			// Transport: expect HTTP 200 before interpreting JSON.
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "GraphQL HTTP status (Jira: 200)", "200", httpActual,
				code == 200 ? "Pass" : "Fail")
			if (code < 200 || code >= 300) {
				KeywordUtil.markFailed("GraphQlApiClient: HTTP ${code}. Body: ${text}")
			}

			def slurper = new JsonSlurper()
			Map parsed = (Map) slurper.parseText(text ?: "{}")

			boolean dataKeyPresent = parsed != null && parsed.containsKey("data")
			// GraphQL success envelope should include top-level data (may be null per field errors).
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "JSON body data key", "key present",
				dataKeyPresent ? "present" : "absent", dataKeyPresent ? "Pass" : "Fail")

			if (parsed.errors) {
				ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "GraphQL errors key", "absent on success",
					String.valueOf(parsed.errors), "Fail")
				KeywordUtil.markFailed("GraphQlApiClient: GraphQL errors: ${parsed.errors}")
			}
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "GraphQL errors key", "absent on success", "absent", "Pass")
			String opLabel = operationName != null && !operationName.isEmpty() ? operationName : "graphql"
			// Pretty-print full body for debugging (same as manual GraphQL clients).
			try {
				String pretty = JsonOutput.prettyPrint(JsonOutput.toJson(parsed))
				KeywordUtil.logInfo("GraphQL response JSON (${opLabel}):\n${pretty}")
			} catch (Exception e) {
				KeywordUtil.logInfo("GraphQL response (${opLabel}): ${parsed}")
			}
			return parsed
		} finally {
			if (stream != null) {
				try {
					stream.close()
				} catch (Exception ignored) {
					// ignore close exception
				}
			}
			if (conn != null) {
				conn.disconnect()
			}
		}
	}

	/**
	 * Run createSubmission mutation.
	 * Required args: studyID, dataCommons, name, intention, dataType (strings).
	 */
	@Keyword
	Map createSubmission(Map args) {
		if (args == null) {
			KeywordUtil.markFailed("GraphQlApiClient.createSubmission: args map is required.")
		}
		// Variables map matches Hub createSubmission mutation inputs.
		Map vars = [
			studyID    : args.studyID?.toString(),
			dataCommons: args.dataCommons?.toString(),
			name       : args.name?.toString(),
			intention  : args.intention?.toString(),
			dataType   : args.dataType?.toString()
		]
		if (vars.values().any { !it }) {
			KeywordUtil.markFailed("GraphQlApiClient.createSubmission: studyID, dataCommons, name, intention, and dataType are required.")
		}
		Map result = post("createSubmission", loadGraphQlDocument("createSubmission"), vars)
		// Minimal response sanity before verifyAndUnpackCreateSubmission.
		if (!result.data || !result.data.get('createSubmission')) {
			KeywordUtil.markFailed("GraphQlApiClient.createSubmission: expected data.createSubmission in response: ${result}")
		}
		return result
	}

	/**
	 * Run createBatch mutation (metadata or data file batch).
	 * Required args: submissionID (ID string), type (e.g. "metadata"), files (list of file name strings only).
	 */
	@Keyword
	Map createBatch(Map args) {
		if (args == null) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: args map is required.")
		}
		String submissionID = args.submissionID?.toString()?.trim()
		String type = args.type?.toString()?.trim()
		def filesRaw = args.files
		if (!(filesRaw instanceof List) || filesRaw.isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: files must be a non-empty list of file names.")
		}
		List<String> files = filesRaw.collect { it?.toString()?.trim() }.findAll { it }
		if (files.size() != filesRaw.size()) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: each file name must be non-empty.")
		}
		if (!submissionID) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: submissionID is required.")
		}
		if (!type) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: type is required.")
		}
		Map vars = [submissionID: submissionID, type: type, files: files]
		Map result = post("createBatch", loadGraphQlDocument("createBatch"), vars)
		Map data = result.data as Map
		if (!data || !data.get('createBatch')) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: expected data.createBatch in response: ${result}")
		}
		Map batchNode = data.get('createBatch') as Map
		String respSubId = batchNode?.get('submissionID')?.toString()?.trim() ?: ""
		// Call-site checks: batch echoes submission, has status string, type matches caller request.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Batch submissionID matches request", submissionID,
			respSubId ?: "(empty)", submissionID == respSubId ? "Pass" : "Fail")
		if (submissionID != respSubId) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: response submissionID '${respSubId}' does not match request '${submissionID}'.")
		}
		String batchStatCreate = batchNode?.get('status')?.toString() ?: ""
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Batch status (createBatch)", "non-empty",
			batchStatCreate?.trim() ?: "(empty)", batchStatCreate?.trim() ? "Pass" : "Fail")
		if (!batchStatCreate?.trim()) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: response status is empty or missing.")
		}
		String respType = batchNode?.get('type')?.toString() ?: "(empty)"
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Batch type matches request", type, respType,
			type == respType?.trim() ? "Pass" : "Fail")
		if (type != respType?.trim()) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: response batch type '${respType?.trim()}' does not match request '${type}'.")
		}
		return result
	}

	/**
	 * For each row in fileRows from createBatch (fileName, signedURL): verify a matching
	 * TSV exists under apiInputDir, PUT file bytes to the presigned URL (no Hub auth), and collect
	 * UploadResult-shaped maps (pass as files to updateBatch).
	 *
	 * @param fileRows List of Maps from data.createBatch.files
	 * @param apiInputDir Project directory path to folder containing local TSVs (e.g. .../InputFiles/CRDC/API)
	 * @return List of [fileName: String, succeeded: true] maps for updateBatch variables
	 */
	@Keyword
	List<Map> putMetadataTsvsAndBuildUploadResults(List fileRows, String apiInputDir) {
		if (!(fileRows instanceof List) || fileRows.isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient.putMetadataTsvsAndBuildUploadResults: fileRows must be a non-empty list.")
		}
		String dir = apiInputDir?.toString()?.trim()
		if (!dir) {
			KeywordUtil.markFailed("GraphQlApiClient.putMetadataTsvsAndBuildUploadResults: apiInputDir is required.")
		}
		KeywordUtil.logInfo(
			"putMetadataTsvsAndBuildUploadResults: S3 PUT log lines below show a redacted URL preview (scheme+host+path only, truncated); " +
			"query string and signing parameters are omitted and are not the literal presigned URL.")
		List<Map> uploadResults = []
		for (Object rowObj : fileRows) {
			Map row = rowObj as Map
			String fn = row?.get('fileName')?.toString()
			String signed = row?.get('signedURL')?.toString()
			if (!fn || !signed) {
				KeywordUtil.markFailed("GraphQlApiClient.putMetadataTsvsAndBuildUploadResults: row missing fileName or signedURL: ${row}")
			}
			File localTsv = new File(dir, fn)
			// Local file must exist before PUT; then upload bytes to presigned URL (no Hub auth on S3).
			if (!localTsv.isFile()) {
				KeywordUtil.markFailed("GraphQlApiClient.putMetadataTsvsAndBuildUploadResults: no local file for ${fn}: ${localTsv.absolutePath}")
			}
			String safeUrlPreview
			try {
				URL signedUrlObj = new URL(signed)
				String baseUrl = "${signedUrlObj.protocol}://${signedUrlObj.host}${signedUrlObj.path}"
				safeUrlPreview = baseUrl.length() > 80 ? baseUrl.substring(0, 80) + "..." : baseUrl
			} catch (Exception e) {
				safeUrlPreview = "<unparsable URL>"
			}
			KeywordUtil.logInfo("S3 PUT ${fn} (url preview: ${safeUrlPreview})")
			putTsvToPresignedUrl(localTsv, signed)
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "S3 PUT presigned upload: ${fn}", "HTTP 2xx success",
				"succeeded", "Pass")
			uploadResults.add([fileName: fn, succeeded: true])
		}
		return uploadResults
	}

	/**
	 * Run updateBatch after S3 PUTs to presigned URLs (metadata or file batch).
	 * Required args: batchID, files (list of maps with at least fileName, succeeded boolean).
	 */
	@Keyword
	Map updateBatch(Map args) {
		if (args == null) {
			KeywordUtil.markFailed("GraphQlApiClient.updateBatch: args map is required.")
		}
		String batchID = args.batchID?.toString()?.trim()
		def filesRaw = args.files
		if (!batchID) {
			KeywordUtil.markFailed("GraphQlApiClient.updateBatch: batchID is required.")
		}
		if (!(filesRaw instanceof List) || filesRaw.isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient.updateBatch: files must be a non-empty list of UploadResult maps.")
		}
		List<Map> files = filesRaw.collect { it instanceof Map ? it as Map : null }.findAll { it != null }
		if (files.size() != filesRaw.size()) {
			KeywordUtil.markFailed("GraphQlApiClient.updateBatch: each item in files must be a Map.")
		}
		for (Map f : files) {
			if (!f.get('fileName')?.toString()?.trim()) {
				KeywordUtil.markFailed("GraphQlApiClient.updateBatch: each file entry needs fileName.")
			}
			if (f.get('succeeded') == null) {
				KeywordUtil.markFailed("GraphQlApiClient.updateBatch: each file entry needs succeeded (boolean).")
			}
		}
		Map vars = [batchID: batchID, files: files]
		Map result = post("updateBatch", loadGraphQlDocument("updateBatch"), vars)
		Map data = result.data as Map
		// Minimal response sanity before verifyAndUnpackUpdateBatch.
		if (!data || !data.get('updateBatch')) {
			KeywordUtil.markFailed("GraphQlApiClient.updateBatch: expected data.updateBatch in response: ${result}")
		}
		return result
	}

	/**
	 * Run listBatches query (upload activity / batch list for a submission).
	 * Required args: submissionID (ID string). Optional: first, offset, orderBy, sortDirection (omit to use API defaults).
	 */
	@Keyword
	Map listBatches(Map args) {
		if (args == null) {
			KeywordUtil.markFailed("GraphQlApiClient.listBatches: args map is required.")
		}
		String submissionID = args.submissionID?.toString()?.trim()
		if (!submissionID) {
			KeywordUtil.markFailed("GraphQlApiClient.listBatches: submissionID is required.")
		}
		Map vars = [submissionID: submissionID]
		// Optional pagination / sort (omit keys to use API defaults).
		if (args.containsKey('first') && args.first != null) {
			vars.first = args.first
		}
		if (args.containsKey('offset') && args.offset != null) {
			vars.offset = args.offset
		}
		if (args.orderBy != null && args.orderBy.toString().trim()) {
			vars.orderBy = args.orderBy.toString().trim()
		}
		if (args.sortDirection != null && args.sortDirection.toString().trim()) {
			vars.sortDirection = args.sortDirection.toString().trim()
		}
		Map result = post("listBatches", loadGraphQlDocument("listBatches"), vars)
		Map data = result.data as Map
		// Minimal response sanity before verifyAndUnpackListBatches.
		if (!data || !data.get('listBatches')) {
			KeywordUtil.markFailed("GraphQlApiClient.listBatches: expected data.listBatches in response: ${result}")
		}
		return result
	}
}
