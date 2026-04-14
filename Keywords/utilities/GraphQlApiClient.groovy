package utilities

import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Matcher
import java.util.regex.Pattern

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
	 * @return true if HTTP 2xx; false after markFailed (invalid args or non-2xx response).
	 */
	private boolean putTsvToPresignedUrl(File file, String presignedUrl) {
		if (file == null || !file.isFile()) {
			KeywordUtil.markFailed("GraphQlApiClient.putTsvToPresignedUrl: file is missing or not a regular file: ${file}")
			return false
		}
		String url = presignedUrl?.trim()
		if (!url) {
			KeywordUtil.markFailed("GraphQlApiClient.putTsvToPresignedUrl: presignedUrl is required.")
			return false
		}
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()
		try {
			conn.setRequestMethod("PUT")
			conn.setRequestProperty("Content-Type", PRESIGNED_TSV_CONTENT_TYPE)
			conn.setDoOutput(true)
			byte[] bytes = file.getBytes()
			conn.outputStream.withCloseable {
				output ->
				output.write(bytes)
				output.flush()
			}
			int code = conn.responseCode
			InputStream stream = code >= 400 ? conn.errorStream : conn.inputStream
			String text = ""
			if (stream != null) {
				stream.withCloseable {
					input ->
					text = input.getText("UTF-8")
				}
			}
			if (code < 200 || code >= 300) {
				KeywordUtil.markFailed("GraphQlApiClient.putTsvToPresignedUrl: HTTP ${code} for ${file.name}. Body: ${text}")
				return false
			}
			return true
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
					// Do not cache or return an empty query; signal error via null.
					return null
				}
				graphqlDocumentCache.put(key, q)
				return q
			}
		}
		KeywordUtil.markFailed("GraphQlApiClient.loadGraphQlDocument: no row for operationKey '${key}' in ${GRAPHQL_OPERATIONS_DATA}.")
		return null
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
		for (Object nameObj : fileBasenames) {
			String basename = nameObj?.toString()?.trim()
			if (!basename) {
				KeywordUtil.markFailed("GraphQlApiClient.verifyLocalFilesExist: each basename must be non-empty.")
				return
			}
			File f = new File(dir, basename)
			// Each basename must exist as a file; report Fail then abort on first missing.
			if (!f.isFile()) {
				ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Local metadata file: ${basename}", "file exists",
				"missing: ${f.absolutePath}", "Fail")
				KeywordUtil.markFailed("Missing input file for metadata upload: ${f.absolutePath}")
				return
			}
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Local metadata file: ${basename}", "file exists",
			f.absolutePath, "Pass")
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
			return null
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
			return null
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
			return null
		}
		if (!statusIsNew) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackCreateSubmission: expected status New, got: ${st}")
			return null
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
			return null
		}
		String batchId = payload?.get('_id')?.toString() ?: ""
		// Verify batch _id returned and non-empty.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Batch _id present", "non-empty",
		batchId?.trim() ?: "(empty)", batchId?.trim() ? "Pass" : "Fail")
		if (!batchId?.trim()) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackCreateBatch: batch _id is empty.")
			return null
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
			return null
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
					return null
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
			return null
		}
		String expBid = expectedBatchId?.toString()?.trim()
		if (expBid) {
			String pBid = payload?.get('_id')?.toString()?.trim() ?: ""
			// When expectedBatchId passed, verify response batch _id matches request.
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "updateBatch batch _id matches request", expBid,
			pBid ?: "(empty)", expBid == pBid ? "Pass" : "Fail")
			if (expBid != pBid) {
				KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackUpdateBatch: expected batch _id '${expBid}' but got '${pBid}'")
				return null
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
					return null
				}
			}
		} else {
			// Batch must return at least one file row after update.
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Batch files after updateBatch", "non-empty list",
			"(missing or empty)", "Fail")
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackUpdateBatch: files list is missing or empty in updateBatch response")
			return null
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
			return payload
		}
		String expSub = expectedSubmissionId?.toString()?.trim()
		String expBid = expectedBatchId?.toString()?.trim()
		// Preconditions: caller must supply submission id, batch id, and non-empty expected file name list.
		if (!expSub || !expBid) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: expectedSubmissionId and expectedBatchId are required.")
			return null
		}
		if (!(expectedFileNames instanceof List) || expectedFileNames.isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: expectedFileNames must be a non-empty list.")
			return null
		}
		List<String> expFiles = expectedFileNames.collect {
			it?.toString()?.trim()
		}.findAll {
			it
		}
		if (expFiles.size() != expectedFileNames.size()) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: each expected file name must be non-empty.")
			return null
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
			return null
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
			return null
		}
		String pSub = matched?.get('submissionID')?.toString()?.trim() ?: ""
		boolean subOk = expSub == pSub
		// Verify batch row is tied to the expected submission.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch submissionID", expSub,
		pSub ?: "(empty)", subOk ? "Pass" : "Fail")
		if (!subOk) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: submissionID mismatch.")
			return null
		}
		String expType = expectedBatchType?.toString()?.trim()
		if (expType) {
			String pType = matched?.get('type')?.toString()?.trim() ?: ""
			// Optional: when expectedBatchType set, verify batch type (e.g. metadata).
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch type", expType,
			pType ?: "(empty)", expType == pType ? "Pass" : "Fail")
			if (expType != pType) {
				KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: expected type ${expType}, got ${pType}")
				return null
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
			return null
		}
		String bStat = matched?.get('status')?.toString()?.trim() ?: ""
		// Verify batch-level status string present.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch status", "non-empty",
		bStat ?: "(empty)", bStat ? "Pass" : "Fail")
		if (!bStat) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: batch status is empty.")
			return null
		}
		Object bErrs = matched.get('errors')
		boolean batchNoErrs = bErrs == null || (bErrs instanceof List && ((List) bErrs).isEmpty())
		// Verify no batch-level error strings returned.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch errors", "empty or absent",
		bErrs != null ? bErrs.toString() : "(absent)", batchNoErrs ? "Pass" : "Fail")
		if (!batchNoErrs) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: batch has errors: ${bErrs}")
			return null
		}
		Object disp = matched.get('displayID')
		boolean dispOk = disp != null && disp.toString().trim()
		// Verify human-visible batch display id present.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch displayID", "present",
		disp != null ? disp.toString() : "(missing)", dispOk ? "Pass" : "Fail")
		if (!dispOk) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: batch displayID missing.")
			return null
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
			return null
		}
		String submName = matched?.get('submitterName')?.toString()?.trim() ?: ""
		// Verify submitter display name populated (Upload Activity parity with UI).
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch submitterName", "non-empty",
		submName ?: "(empty)", submName ? "Pass" : "Fail")
		if (!submName) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: submitterName empty.")
			return null
		}
		List batchFiles = matched.get('files') as List
		boolean filesOk = batchFiles && !batchFiles.isEmpty()
		// Verify nested files list exists for the batch.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "listBatches batch files array", "non-empty",
		filesOk ? String.valueOf(batchFiles.size()) + " file(s)" : "(missing or empty)", filesOk ? "Pass" : "Fail")
		if (!filesOk) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: batch files list missing or empty.")
			return null
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
				return null
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
				return null
			}
			if (!sizePass) {
				KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackListBatches: file ${expFn} size present but not > 0.")
				return null
			}
		}
		return payload
	}

	/**
	 * Verify validateSubmission response: success indicates the validation run was initialized (queued), not QC outcome.
	 * @param result Full parsed GraphQL body from validateSubmission
	 * @param expectSuccess When true, require success == true (happy path).
	 * @return data.validateSubmission map or null on failure
	 */
	@Keyword
	Map verifyAndUnpackValidateSubmission(Map result, boolean expectSuccess = true) {
		Map payload = (result?.data as Map)?.get('validateSubmission') as Map
		if (!payload) {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "data.validateSubmission", "present in response",
			"missing", "Fail")
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackValidateSubmission: missing data.validateSubmission in ${result}")
			return null
		}
		Object succObj = payload.get('success')
		boolean succIsBool = succObj instanceof Boolean
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "validateSubmission success (Boolean)", "Boolean",
		succIsBool ? succObj.toString() : "not Boolean: ${succObj}", succIsBool ? "Pass" : "Fail")
		if (!succIsBool) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackValidateSubmission: success is not Boolean: ${succObj}")
			return null
		}
		boolean ok = (Boolean) succObj
		if (expectSuccess) {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "validateSubmission success (initialized)", "true",
			ok ? "true" : "false", ok ? "Pass" : "Fail")
			if (!ok) {
				String msg = payload.get('message')?.toString()
				KeywordUtil.markFailed(
				"GraphQlApiClient.verifyAndUnpackValidateSubmission: expected success true; got false. message: ${msg}")
				return null
			}
		} else {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "validateSubmission success (initialized)", "(any)",
			ok ? "true" : "false", "Pass")
		}
		Object msgObj = payload.get('message')
		if (msgObj != null && msgObj.toString().trim()) {
			KeywordUtil.logInfo("validateSubmission message: ${msgObj}")
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
		if (!query?.toString()?.trim()) {
			KeywordUtil.markFailed("GraphQlApiClient.post: query is required.")
			return null
		}
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

			conn.outputStream.withWriter("UTF-8") {
				Writer w -> w << body
			}

			int code = conn.responseCode
			stream = code >= 400 ? conn.errorStream : conn.inputStream
			String text = stream ? stream.getText("UTF-8") : ""

			String httpActual = String.valueOf(code)
			// Transport: expect HTTP 200 before interpreting JSON.
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "GraphQL HTTP status (Jira: 200)", "200", httpActual,
			code == 200 ? "Pass" : "Fail")
			if (code < 200 || code >= 300) {
				KeywordUtil.markFailed("GraphQlApiClient: HTTP ${code}. Body: ${text}")
				return null
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
				return null
			} else {
				ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "GraphQL errors key", "absent on success", "absent", "Pass")
			}
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
			return null
		}
		// Variables map matches Hub createSubmission mutation inputs.
		Map vars = [
			studyID    : args.studyID?.toString(),
			dataCommons: args.dataCommons?.toString(),
			name       : args.name?.toString(),
			intention  : args.intention?.toString(),
			dataType   : args.dataType?.toString()
		]
		if (vars.values().any {
			!it
		}) {
			KeywordUtil.markFailed("GraphQlApiClient.createSubmission: studyID, dataCommons, name, intention, and dataType are required.")
			return null
		}
		String queryDoc = loadGraphQlDocument("createSubmission")
		if (!queryDoc) {
			return null
		}
		Map result = post("createSubmission", queryDoc, vars)
		// Minimal response sanity before verifyAndUnpackCreateSubmission.
		if (!result || !result.data || !result.data.get('createSubmission')) {
			KeywordUtil.markFailed("GraphQlApiClient.createSubmission: expected data.createSubmission in response: ${result}")
			return null
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
			return null
		}
		String submissionID = args.submissionID?.toString()?.trim()
		String type = args.type?.toString()?.trim()
		def filesRaw = args.files
		if (!(filesRaw instanceof List) || filesRaw.isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: files must be a non-empty list of file names.")
			return null
		}
		List<String> files = filesRaw.collect {
			it?.toString()?.trim()
		}.findAll {
			it
		}
		if (files.size() != filesRaw.size()) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: each file name must be non-empty.")
			return null
		}
		if (!submissionID) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: submissionID is required.")
			return null
		}
		if (!type) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: type is required.")
			return null
		}
		String queryDoc = loadGraphQlDocument("createBatch")
		if (!queryDoc) {
			return null
		}
		Map vars = [submissionID: submissionID, type: type, files: files]
		Map result = post("createBatch", queryDoc, vars)
		Map data = result?.data as Map
		if (!data || !data.get('createBatch')) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: expected data.createBatch in response: ${result}")
			return null
		}
		Map batchNode = data.get('createBatch') as Map
		String respSubId = batchNode?.get('submissionID')?.toString()?.trim() ?: ""
		// Call-site checks: batch echoes submission, has status string, type matches caller request.
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Batch submissionID matches request", submissionID,
		respSubId ?: "(empty)", submissionID == respSubId ? "Pass" : "Fail")
		if (submissionID != respSubId) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: response submissionID '${respSubId}' does not match request '${submissionID}'.")
			return null
		}
		String batchStatCreate = batchNode?.get('status')?.toString() ?: ""
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Batch status (createBatch)", "non-empty",
		batchStatCreate?.trim() ?: "(empty)", batchStatCreate?.trim() ? "Pass" : "Fail")
		if (!batchStatCreate?.trim()) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: response status is empty or missing.")
			return null
		}
		String respType = batchNode?.get('type')?.toString() ?: "(empty)"
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "Batch type matches request", type, respType,
		type == respType?.trim() ? "Pass" : "Fail")
		if (type != respType?.trim()) {
			KeywordUtil.markFailed("GraphQlApiClient.createBatch: response batch type '${respType?.trim()}' does not match request '${type}'.")
			return null
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
	 * @return List of [fileName: String, succeeded: boolean] maps for updateBatch variables
	 */
	@Keyword
	List<Map> putMetadataTsvsAndBuildUploadResults(List fileRows, String apiInputDir) {
		if (!(fileRows instanceof List) || fileRows.isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient.putMetadataTsvsAndBuildUploadResults: fileRows must be a non-empty list.")
			return []
		}
		String dir = apiInputDir?.toString()?.trim()
		if (!dir) {
			KeywordUtil.markFailed("GraphQlApiClient.putMetadataTsvsAndBuildUploadResults: apiInputDir is required.")
			return []
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
				return uploadResults
			}
			File localTsv = new File(dir, fn)
			// Local file must exist before PUT; then upload bytes to presigned URL (no Hub auth on S3).
			if (!localTsv.isFile()) {
				KeywordUtil.markFailed("GraphQlApiClient.putMetadataTsvsAndBuildUploadResults: no local file for ${fn}: ${localTsv.absolutePath}")
				return uploadResults
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
			boolean success = putTsvToPresignedUrl(localTsv, signed)
			String message = success ? "HTTP 2xx success" : "HTTP non-2xx or network failure (see S3 PUT logs)"
			String status = success ? "succeeded" : "failed"
			String result = success ? "Pass" : "Fail"
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "S3 PUT presigned upload: ${fn}", message,
			status, result)
			uploadResults.add([fileName: fn, succeeded: success])
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
			return null
		}
		String batchID = args.batchID?.toString()?.trim()
		def filesRaw = args.files
		if (!batchID) {
			KeywordUtil.markFailed("GraphQlApiClient.updateBatch: batchID is required.")
			return null
		}
		if (!(filesRaw instanceof List) || filesRaw.isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient.updateBatch: files must be a non-empty list of UploadResult maps.")
			return null
		}
		List<Map> files = filesRaw.collect {
			(it instanceof Map) ? (it as Map) : null
		}.findAll {
			it != null
		}
		if (files.size() != filesRaw.size()) {
			KeywordUtil.markFailed("GraphQlApiClient.updateBatch: each item in files must be a Map.")
			return null
		}
		for (Map f : files) {
			if (!f.get('fileName')?.toString()?.trim()) {
				KeywordUtil.markFailed("GraphQlApiClient.updateBatch: each file entry needs fileName.")
				return null
			}
			if (f.get('succeeded') == null) {
				KeywordUtil.markFailed("GraphQlApiClient.updateBatch: each file entry needs succeeded (boolean).")
				return null
			}
		}
		String queryDoc = loadGraphQlDocument("updateBatch")
		if (!queryDoc) {
			return null
		}
		Map vars = [batchID: batchID, files: files]
		Map result = post("updateBatch", queryDoc, vars)
		Map data = result?.data as Map
		// Minimal response sanity before verifyAndUnpackUpdateBatch.
		if (!data || !data.get('updateBatch')) {
			KeywordUtil.markFailed("GraphQlApiClient.updateBatch: expected data.updateBatch in response: ${result}")
			return null
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
			return null
		}
		String submissionID = args.submissionID?.toString()?.trim()
		if (!submissionID) {
			KeywordUtil.markFailed("GraphQlApiClient.listBatches: submissionID is required.")
			return null
		}
		String queryDoc = loadGraphQlDocument("listBatches")
		if (!queryDoc) {
			return null
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
		Map result = post("listBatches", queryDoc, vars)
		Map data = result?.data as Map
		// Minimal response sanity before verifyAndUnpackListBatches.
		if (!data || !data.get('listBatches')) {
			KeywordUtil.markFailed("GraphQlApiClient.listBatches: expected data.listBatches in response: ${result}")
			return null
		}
		return result
	}

	/**
	 * Run getSubmissionNodes query (Data View nodes for a submission and node type).
	 * Required args: _id or submissionID (submission _id), nodeType (e.g. program, study, participant).
	 * Optional: status (default All on server if omitted), submittedID (maps to nodeID filter), first, offset, orderBy, sortDirection.
	 */
	@Keyword
	Map getSubmissionNodes(Map args) {
		if (args == null) {
			KeywordUtil.markFailed("GraphQlApiClient.getSubmissionNodes: args map is required.")
			return null
		}
		String sid = args._id?.toString()?.trim() ?: args.submissionID?.toString()?.trim()
		String nodeType = args.nodeType?.toString()?.trim()
		if (!sid) {
			KeywordUtil.markFailed("GraphQlApiClient.getSubmissionNodes: _id or submissionID is required.")
			return null
		}
		if (!nodeType) {
			KeywordUtil.markFailed("GraphQlApiClient.getSubmissionNodes: nodeType is required.")
			return null
		}
		String queryDoc = loadGraphQlDocument("getSubmissionNodes")
		if (!queryDoc) {
			return null
		}
		Map vars = [_id: sid, nodeType: nodeType]
		if (args.status != null && args.status.toString().trim()) {
			vars.status = args.status.toString().trim()
		}
		if (args.submittedID != null && args.submittedID.toString().trim()) {
			vars.submittedID = args.submittedID.toString().trim()
		}
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
		Map result = post("getSubmissionNodes", queryDoc, vars)
		Map data = result?.data as Map
		if (!data || !data.get('getSubmissionNodes')) {
			KeywordUtil.markFailed("GraphQlApiClient.getSubmissionNodes: expected data.getSubmissionNodes in response: ${result}")
			return null
		}
		return result
	}

	/**
	 * Verify getSubmissionNodes payload: total, IDPropName, properties, and each node matches submission/type/expected IDs.
	 * Expects unpaged or full page: nodes list length equals total and expectedTotal when first is -1.
	 * @param result Full GraphQL body from getSubmissionNodes
	 * @param expectedSubmissionId Submission _id
	 * @param expectedNodeType Node type filter used in the query (e.g. program)
	 * @param expectedTotal Expected row count for that node type
	 * @param expectedNodeIds List of expected nodeID values (Data View key column per model)
	 * @return data.getSubmissionNodes map or null on failure
	 */
	@Keyword
	Map verifyAndUnpackGetSubmissionNodes(Map result, String expectedSubmissionId, String expectedNodeType,
			int expectedTotal, List expectedNodeIds) {
		Map data = result?.data as Map
		Map payload = data?.get('getSubmissionNodes') as Map
		if (!payload) {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "data.getSubmissionNodes", "present in response",
			"missing", "Fail")
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackGetSubmissionNodes: missing data.getSubmissionNodes in ${result}")
			return null
		}
		String expSub = expectedSubmissionId?.toString()?.trim()
		String expNt = expectedNodeType?.toString()?.trim()
		Object totObj = payload.get('total')
		int totalVal = totObj instanceof Number ? ((Number) totObj).intValue() : -1
		String idProp = payload.get('IDPropName')?.toString()?.trim()
		def propsRaw = payload.get('properties')
		boolean propsOk = propsRaw instanceof List && !(propsRaw as List).isEmpty()
		def nodesRaw = payload.get('nodes')
		boolean nodesListOk = nodesRaw instanceof List
		List nodes = nodesListOk ? (nodesRaw as List) : []

		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "getSubmissionNodes(${expNt}) total", String.valueOf(expectedTotal),
		String.valueOf(totalVal), totalVal == expectedTotal ? "Pass" : "Fail")
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "getSubmissionNodes(${expNt}) IDPropName", "non-empty",
		idProp ?: "(empty)", idProp ? "Pass" : "Fail")
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "getSubmissionNodes(${expNt}) properties", "non-empty list",
		propsOk ? "size ${(propsRaw as List).size()}" : String.valueOf(propsRaw), propsOk ? "Pass" : "Fail")

		if (totalVal != expectedTotal) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackGetSubmissionNodes: expected total ${expectedTotal}, got ${totalVal}")
			return null
		}
		if (!idProp) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackGetSubmissionNodes: IDPropName is empty.")
			return null
		}
		if (!propsOk) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackGetSubmissionNodes: properties must be a non-empty list.")
			return null
		}
		if (!nodesListOk) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackGetSubmissionNodes: nodes is not a list: ${nodesRaw}")
			return null
		}
		if (nodes.size() != expectedTotal) {
			KeywordUtil.markFailed(
			"GraphQlApiClient.verifyAndUnpackGetSubmissionNodes: expected ${expectedTotal} nodes, got ${nodes.size()}")
			return null
		}

		Set<String> expectedIds = (expectedNodeIds ?: []).collect {
			it?.toString()?.trim()
		}.findAll {
			it
		}.toSet()
		if (expectedIds.size() != expectedTotal) {
			KeywordUtil.markFailed(
			"GraphQlApiClient.verifyAndUnpackGetSubmissionNodes: expectedNodeIds size ${expectedIds.size()} != expectedTotal ${expectedTotal}")
			return null
		}

		Set<String> seen = [] as Set
		JsonSlurper slurper = new JsonSlurper()
		for (Object nObj : nodes) {
			if (!(nObj instanceof Map)) {
				KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackGetSubmissionNodes: node entry is not a map: ${nObj}")
				return null
			}
			Map n = nObj as Map
			String nSub = n.get('submissionID')?.toString()?.trim()
			String nType = n.get('nodeType')?.toString()?.trim()
			String nId = n.get('nodeID')?.toString()?.trim()
			String nStat = n.get('status')?.toString()?.trim()
			Object propsField = n.get('props')

			boolean subOk = expSub && expSub == nSub
			boolean typeOk = expNt && expNt == nType
			boolean idOk = nId && expectedIds.contains(nId)
			boolean statOk = nStat && (nStat == 'Passed' || nStat == 'Warning' || nStat == 'New' || nStat == 'Error')
			boolean propsParseOk = false
			if (propsField != null && propsField.toString().trim()) {
				try {
					slurper.parseText(propsField.toString())
					propsParseOk = true
				} catch (Exception ignored) {
					propsParseOk = false
				}
			}

			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "node ${nId} submissionID", expSub ?: "(empty)",
			nSub ?: "(empty)", subOk ? "Pass" : "Fail")
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "node ${nId} nodeType", expNt ?: "(empty)",
			nType ?: "(empty)", typeOk ? "Pass" : "Fail")
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "node ${nId} status", "non-empty valid",
			nStat ?: "(empty)", statOk ? "Pass" : "Fail")
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "node ${nId} props JSON", "parseable",
			propsParseOk ? "ok" : "(invalid or empty)", propsParseOk ? "Pass" : "Fail")

			if (!subOk || !typeOk || !idOk || !statOk || !propsParseOk) {
				KeywordUtil.markFailed(
				"GraphQlApiClient.verifyAndUnpackGetSubmissionNodes: node validation failed for nodeID=${nId} subOk=${subOk} typeOk=${typeOk} idOk=${idOk} statOk=${statOk} propsParseOk=${propsParseOk}")
				return null
			}
			if (seen.contains(nId)) {
				KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackGetSubmissionNodes: duplicate nodeID ${nId}")
				return null
			}
			seen.add(nId)
		}

		if (seen != expectedIds) {
			KeywordUtil.markFailed(
			"GraphQlApiClient.verifyAndUnpackGetSubmissionNodes: nodeID set mismatch. Expected ${expectedIds}, got ${seen}")
			return null
		}

		return payload
	}

	/**
	 * Run deleteDataRecords mutation (async delete of metadata nodes or data-file rows per Hub rules).
	 * Required args: _id or submissionID, nodeType (e.g. participant, or "data file" for file deletes), nodeIDs (non-empty list of node IDs or file names).
	 * Optional: deleteAll (Boolean), exclusiveIDs (list of strings) — see GraphQL schema.
	 */
	@Keyword
	Map deleteDataRecords(Map args) {
		if (args == null) {
			KeywordUtil.markFailed("GraphQlApiClient.deleteDataRecords: args map is required.")
			return null
		}
		String sid = args._id?.toString()?.trim() ?: args.submissionID?.toString()?.trim()
		String nodeType = args.nodeType?.toString()?.trim()
		def idsRaw = args.nodeIDs
		if (!sid) {
			KeywordUtil.markFailed("GraphQlApiClient.deleteDataRecords: _id or submissionID is required.")
			return null
		}
		if (!nodeType) {
			KeywordUtil.markFailed("GraphQlApiClient.deleteDataRecords: nodeType is required.")
			return null
		}
		if (!(idsRaw instanceof List) || idsRaw.isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient.deleteDataRecords: nodeIDs must be a non-empty list.")
			return null
		}
		List nodeIDs = idsRaw.collect {
			it?.toString()?.trim()
		}.findAll {
			it
		}
		if (nodeIDs.isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient.deleteDataRecords: each nodeID must be non-empty.")
			return null
		}
		String queryDoc = loadGraphQlDocument("deleteDataRecords")
		if (!queryDoc) {
			return null
		}
		Map vars = [_id: sid, nodeType: nodeType, nodeIDs: nodeIDs]
		if (args.containsKey('deleteAll') && args.deleteAll != null) {
			vars.deleteAll = args.deleteAll
		}
		if (args.exclusiveIDs != null) {
			vars.exclusiveIDs = args.exclusiveIDs
		}
		Map result = post("deleteDataRecords", queryDoc, vars)
		Map data = result?.data as Map
		if (!data || !data.get('deleteDataRecords')) {
			KeywordUtil.markFailed("GraphQlApiClient.deleteDataRecords: expected data.deleteDataRecords in response: ${result}")
			return null
		}
		return result
	}

	/**
	 * Verify deleteDataRecords returns DataValidation: success true and log message.
	 * @param result Full GraphQL body from deleteDataRecords
	 * @return data.deleteDataRecords map or null on failure
	 */
	@Keyword
	Map verifyAndUnpackDeleteDataRecords(Map result) {
		Map data = result?.data as Map
		Map payload = data?.get('deleteDataRecords') as Map
		if (!payload) {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "data.deleteDataRecords", "present in response",
			"missing", "Fail")
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackDeleteDataRecords: missing data.deleteDataRecords in ${result}")
			return null
		}
		Object succObj = payload.get('success')
		boolean ok = succObj instanceof Boolean ? (Boolean) succObj : Boolean.TRUE.equals(succObj)
		String msg = payload.get('message')?.toString() ?: ""
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "deleteDataRecords success", "true",
		String.valueOf(succObj), ok ? "Pass" : "Fail")
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "deleteDataRecords message", "(logged)",
		msg ?: "(empty)", ok ? "Pass" : "Fail")
		if (!ok) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackDeleteDataRecords: success is not true. message: ${msg}")
			return null
		}
		return payload
	}

	/**
	 * Run submissionQCResults query (QC issue rows for a submission).
	 * Required args: id (submission ID). Optional: nodeTypes, batchIDs, severities,
	 * first, offset, orderBy, sortDirection (omit to use API defaults).
	 * Pagination: total is usually the count across all matching rows; results is only the current page.
	 * With a finite first argument, the results list length may be less than total.
	 */
	@Keyword
	Map submissionQCResults(Map args) {
		if (args == null) {
			KeywordUtil.markFailed("GraphQlApiClient.submissionQCResults: args map is required.")
			return null
		}
		String sid = args.id?.toString()?.trim()
		if (!sid) {
			KeywordUtil.markFailed("GraphQlApiClient.submissionQCResults: id is required.")
			return null
		}
		String queryDoc = loadGraphQlDocument("submissionQCResults")
		if (!queryDoc) {
			return null
		}
		Map vars = [id: sid]
		if (args.nodeTypes != null) {
			vars.nodeTypes = args.nodeTypes
		}
		if (args.batchIDs != null) {
			vars.batchIDs = args.batchIDs
		}
		if (args.containsKey('severities') && args.severities != null) {
			vars.severities = args.severities.toString().trim()
		}
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
		Map result = post("submissionQCResults", queryDoc, vars)
		Map data = result?.data as Map
		if (!data || !data.get('submissionQCResults')) {
			KeywordUtil.markFailed("GraphQlApiClient.submissionQCResults: expected data.submissionQCResults in response: ${result}")
			return null
		}
		return result
	}

	/**
	 * Verify submissionQCResults response: total and results list size both equal expectedTotal.
	 * Use only for unpaged full-list queries (e.g. first: -1 so the Hub returns all rows). For paginated queries,
	 * total and page size differ; use different assertions instead of this helper.
	 * @param result Full parsed GraphQL body from submissionQCResults
	 * @param expectedTotal Expected total and expected results list length (e.g. 0 when no QC rows for filters)
	 * @return data.submissionQCResults map or null on failure
	 */
	@Keyword
	Map verifyAndUnpackSubmissionQCResults(Map result, int expectedTotal) {
		Map data = result?.data as Map
		Map payload = data?.get('submissionQCResults') as Map
		if (!payload) {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "data.submissionQCResults", "present in response",
			"missing", "Fail")
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackSubmissionQCResults: missing data.submissionQCResults in ${result}")
			return null
		}
		Object totObj = payload.get('total')
		int totalVal = totObj instanceof Number ? ((Number) totObj).intValue() : -1
		def resultsRaw = payload.get('results')
		boolean listOk = resultsRaw instanceof List
		int resultSize = listOk ? (resultsRaw as List).size() : -1

		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "submissionQCResults total", String.valueOf(expectedTotal),
		String.valueOf(totalVal), totalVal == expectedTotal ? "Pass" : "Fail")
		String expSize = String.valueOf(expectedTotal)
		String actSize = listOk ? String.valueOf(resultSize) : "(not a list)"
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "submissionQCResults results size", expSize,
		actSize, (listOk && resultSize == expectedTotal) ? "Pass" : "Fail")

		if (totalVal != expectedTotal) {
			KeywordUtil.markFailed(
			"GraphQlApiClient.verifyAndUnpackSubmissionQCResults: expected total ${expectedTotal}, got ${totalVal}")
			return null
		}
		if (!listOk) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackSubmissionQCResults: results is not a list: ${resultsRaw}")
			return null
		}
		if (resultSize != expectedTotal) {
			KeywordUtil.markFailed(
			"GraphQlApiClient.verifyAndUnpackSubmissionQCResults: expected results size ${expectedTotal}, got ${resultSize}")
			return null
		}
		return payload
	}

	/**
	 * Run submissionAction mutation (e.g. Submit, Withdraw, Cancel).
	 * Required args: submissionID, action (trimmed strings).
	 */
	@Keyword
	Map submissionAction(Map args) {
		if (args == null) {
			KeywordUtil.markFailed("GraphQlApiClient.submissionAction: args map is required.")
			return null
		}
		String submissionID = args.submissionID?.toString()?.trim()
		String action = args.action?.toString()?.trim()
		if (!submissionID) {
			KeywordUtil.markFailed("GraphQlApiClient.submissionAction: submissionID is required.")
			return null
		}
		if (!action) {
			KeywordUtil.markFailed("GraphQlApiClient.submissionAction: action is required.")
			return null
		}
		String queryDoc = loadGraphQlDocument("submissionAction")
		if (!queryDoc) {
			return null
		}
		Map vars = [submissionID: submissionID, action: action]
		Map result = post("submissionAction", queryDoc, vars)
		Map data = result?.data as Map
		if (!data || !data.get('submissionAction')) {
			KeywordUtil.markFailed("GraphQlApiClient.submissionAction: expected data.submissionAction in response: ${result}")
			return null
		}
		return result
	}

	/**
	 * Verify submissionAction response payload: _id matches request; optionally status matches expectedStatus.
	 * @param result Full parsed GraphQL body from submissionAction
	 * @param expectedSubmissionId Submission _id from the request
	 * @param expectedStatus When non-null/non-blank, require returned status to equal this (e.g. Submitted after action Submit)
	 * @return data.submissionAction map or null on failure
	 */
	@Keyword
	Map verifyAndUnpackSubmissionAction(Map result, String expectedSubmissionId, String expectedStatus = null) {
		Map data = result?.data as Map
		Map payload = data?.get('submissionAction') as Map
		if (!payload) {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "data.submissionAction", "present in response",
			"missing", "Fail")
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackSubmissionAction: missing data.submissionAction in ${result}")
			return null
		}
		String expId = expectedSubmissionId?.toString()?.trim()
		String actId = payload.get('_id')?.toString()?.trim()
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "submissionAction _id", expId ?: "(empty)",
		actId ?: "(empty)", expId && expId == actId ? "Pass" : "Fail")
		if (!expId || expId != actId) {
			KeywordUtil.markFailed(
			"GraphQlApiClient.verifyAndUnpackSubmissionAction: expected _id '${expId}', got '${actId}'")
			return null
		}
		String expSt = expectedStatus?.toString()?.trim()
		if (expSt) {
			String actSt = payload.get('status')?.toString()?.trim()
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "submissionAction status", expSt,
			actSt ?: "(empty)", expSt == actSt ? "Pass" : "Fail")
			if (expSt != actSt) {
				KeywordUtil.markFailed(
				"GraphQlApiClient.verifyAndUnpackSubmissionAction: expected status '${expSt}', got '${actSt}'")
				return null
			}
		}
		return payload
	}

	/**
	 * Run validateSubmission mutation (trigger metadata/file validation via Hub).
	 * Required args: _id (submission ID), types (non-empty list of strings, e.g. a one-element list for metadata), scope (e.g. New or All).
	 */
	@Keyword
	Map validateSubmission(Map args) {
		if (args == null) {
			KeywordUtil.markFailed("GraphQlApiClient.validateSubmission: args map is required.")
			return null
		}
		String sid = args._id?.toString()?.trim()
		def typesRaw = args.types
		String scope = args.scope?.toString()?.trim()
		if (!sid) {
			KeywordUtil.markFailed("GraphQlApiClient.validateSubmission: _id is required.")
			return null
		}
		if (!(typesRaw instanceof List) || typesRaw.isEmpty()) {
			KeywordUtil.markFailed("GraphQlApiClient.validateSubmission: types must be a non-empty list of strings.")
			return null
		}
		List<String> types = typesRaw.collect {
			it?.toString()?.trim()
		}.findAll {
			it
		}
		if (types.size() != typesRaw.size()) {
			KeywordUtil.markFailed("GraphQlApiClient.validateSubmission: each type must be non-empty.")
			return null
		}
		if (!scope) {
			KeywordUtil.markFailed("GraphQlApiClient.validateSubmission: scope is required.")
			return null
		}
		String queryDoc = loadGraphQlDocument("validateSubmission")
		if (!queryDoc) {
			return null
		}
		Map vars = [_id: sid, types: types, scope: scope]
		Map result = post("validateSubmission", queryDoc, vars)
		Map data = result?.data as Map
		if (!data || !data.get('validateSubmission')) {
			KeywordUtil.markFailed("GraphQlApiClient.validateSubmission: expected data.validateSubmission in response: ${result}")
			return null
		}
		return result
	}

	/**
	 * Extract JWT from retrieveCLIConfig YAML after a line starting with token: (first line of token value).
	 */
	private static String extractJwtFromCliConfigYaml(String yaml) {
		if (!yaml) {
			return null
		}
		Matcher m = Pattern.compile('(?m)^\\s*token:\\s*(.+)\\s*$').matcher(yaml)
		String candidate = null
		while (m.find()) {
			candidate = m.group(1).trim()
			// First token line is the JWT; ignore later lines if any.
			break
		}
		if (!candidate) {
			return null
		}
		// JWT is three dot-separated segments (base64url); strip trailing comment if present.
		Matcher jwtM = Pattern.compile("^([A-Za-z0-9_\\-=]+\\.[A-Za-z0-9_\\-=]+\\.[A-Za-z0-9_\\-=]+)").matcher(candidate)
		return jwtM.find() ? jwtM.group(1) : null
	}

	/**
	 * Run retrieveCLIConfig query (YAML template for CLI uploader).
	 * Required args: submissionID, apiURL, dataFolder, manifest (strings). Optional: archive_manifest (default empty).
	 */
	@Keyword
	Map retrieveCLIConfig(Map args) {
		if (args == null) {
			KeywordUtil.markFailed("GraphQlApiClient.retrieveCLIConfig: args map is required.")
			return null
		}
		String submissionID = args.submissionID?.toString()?.trim()
		String apiURL = args.apiURL?.toString()?.trim()
		String dataFolder = args.dataFolder?.toString()?.trim()
		String manifest = args.manifest?.toString()?.trim()
		if (!submissionID || !apiURL) {
			KeywordUtil.markFailed("GraphQlApiClient.retrieveCLIConfig: submissionID and apiURL are required.")
			return null
		}
		if (!dataFolder || !manifest) {
			KeywordUtil.markFailed("GraphQlApiClient.retrieveCLIConfig: dataFolder and manifest are required.")
			return null
		}
		String archiveManifest = args.archive_manifest != null ? args.archive_manifest.toString() : ""
		String queryDoc = loadGraphQlDocument("retrieveCLIConfig")
		if (!queryDoc) {
			return null
		}
		Map vars = [
			submissionID    : submissionID,
			apiURL          : apiURL,
			dataFolder      : dataFolder,
			manifest        : manifest,
			archive_manifest: archiveManifest
		]
		Map result = post("retrieveCLIConfig", queryDoc, vars)
		Map data = result?.data as Map
		Object cfg = data?.get("retrieveCLIConfig")
		if (!(cfg instanceof CharSequence) || !cfg.toString().trim()) {
			KeywordUtil.markFailed("GraphQlApiClient.retrieveCLIConfig: expected non-empty data.retrieveCLIConfig string in ${result}")
			return null
		}
		return result
	}

	/**
	 * Verify retrieveCLIConfig YAML: api-url, submission id, data and manifest paths, and token matches profile JWT.
	 * @param result Full GraphQL body from retrieveCLIConfig
	 * @param expectedSubmissionId Submission _id used in the query
	 * @param expectedApiUrl GraphQL endpoint (same as GlobalVariable.ApiUrl)
	 * @param expectedDataFolder dataFolder string passed in the query
	 * @param expectedManifest manifest path string passed in the query
	 * @return The YAML string or null on failure
	 */
	@Keyword
	String verifyAndUnpackRetrieveCLIConfig(Map result, String expectedSubmissionId, String expectedApiUrl,
			String expectedDataFolder, String expectedManifest) {
		Map data = result?.data as Map
		Object raw = data?.get("retrieveCLIConfig")
		if (!(raw instanceof CharSequence)) {
			ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "retrieveCLIConfig string", "present",
				"missing", "Fail")
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackRetrieveCLIConfig: missing data.retrieveCLIConfig in ${result}")
			return null
		}
		String yaml = raw.toString()
		String expSub = expectedSubmissionId?.toString()?.trim()
		String expApi = expectedApiUrl?.toString()?.trim()
		String expData = expectedDataFolder?.toString()?.trim()
		String expMan = expectedManifest?.toString()?.trim()
		if (!expSub || !expApi || !expData || !expMan) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackRetrieveCLIConfig: expectedSubmissionId, expectedApiUrl, expectedDataFolder, expectedManifest are required.")
			return null
		}

		boolean hasApi = yaml.contains(expApi)
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "CLI config api-url contains ApiUrl", expApi,
			hasApi ? "contains" : "(absent)", hasApi ? "Pass" : "Fail")
		boolean hasSub = yaml.contains("submission:") && yaml.contains(expSub)
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "CLI config submission id", expSub,
			hasSub ? "present" : "(absent)", hasSub ? "Pass" : "Fail")
		boolean hasData = yaml.contains(expData)
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "CLI config data folder path", expData,
			hasData ? "present" : "(absent)", hasData ? "Pass" : "Fail")
		boolean hasMan = yaml.contains(expMan)
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "CLI config manifest path", expMan,
			hasMan ? "present" : "(absent)", hasMan ? "Pass" : "Fail")

		String expectedJwt = normalizeBearerToken(GlobalVariable.SubmitterApiToken.toString())
		String actualJwt = extractJwtFromCliConfigYaml(yaml)
		boolean tokenOk = expectedJwt && actualJwt && expectedJwt == actualJwt
		ValidationReport.addValidationReportItem(VALIDATION_REPORT_SECTION, "CLI config token matches SubmitterApiToken",
			"(matches profile)", tokenOk ? "match" : "mismatch or missing", tokenOk ? "Pass" : "Fail")

		if (!hasApi || !hasSub || !hasData || !hasMan) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackRetrieveCLIConfig: YAML missing expected api-url, submission, data, or manifest content.")
			return null
		}
		if (!tokenOk) {
			KeywordUtil.markFailed("GraphQlApiClient.verifyAndUnpackRetrieveCLIConfig: token in YAML does not match SubmitterApiToken.")
			return null
		}
		return yaml
	}
}
