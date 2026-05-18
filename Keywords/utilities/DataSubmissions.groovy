package utilities

import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory

import java.io.FileFilter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.List
import java.util.Map
import java.util.Set
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import com.kms.katalon.core.testobject.TestObject

/**
 * Data Submissions: Data View and Validation Results verification keywords.
 * Contains all keywords for verifying Data View tab (headers, row content vs metadata, download vs expected TSV)
 * and Validation Results tab (headers, UI vs expected TSV by node/severity/issue type).
 * Verify-only and upload-only are separate: use uploadFolder(tcName) then verifyDataViewMatchesMetadataForTC(tcName) for a two-step flow.
 * Moved from SubmissionRequest and Pbac.
 */
class DataSubmissions extends TestRunner {

	// Expected data submission header values (set at login, createDataSubmission, or hardcoded; used by verifyDataSubmissionHeaders)
	static String expectedSubmitter = null
	static String expectedSubmissionName = null
	static String expectedStudy = "autoQA"
	static String expectedStudyTooltip = null
	static String expectedProgram = "AUTOPROGRAM"
	static String expectedProgramTooltip = ""
	static String expectedSubmissionType = null  // set in createDataSubmission to "Delete" or "New/Update"
	static String expectedCollaboratorsCount = "0"
	static String expectedCollaboratorsTooltip = "Click to add new collaborators or view existing ones."
	static String expectedDataCommons = null
	static String expectedDataConcierge = "pbac.dcp-automation Please Do Not Edit"
	/** Set by readCrossValidationResultsTableFromUI (full table) or readCrossValidationRowsForConflictingSubmission (matched rows only). */
	static int lastCrossValidationTableRowCount = -1
	/** Maps tab internal name (e.g. UploadActivities, DataView) to section name used in ValidationReport. */
	private static String tabToSectionName(String tab) {
		if (tab == "UploadActivities") return "Upload Activities"
		if (tab == "ValidationResults") return "Validation Results"
		if (tab == "DataView") return "Data View"
		return tab
	}

	/**
	 * Extract submission ID from the current Data Hub upload-activity URL.
	 * Use this when you want to set CRDC_SUBMISSION_ID from the browser URL (e.g. in a script).
	 *
	 * @param url full URL (e.g. WebUI.getUrl())
	 * @return submission ID (UUID string) or null if not found
	 */
	@Keyword
	static String getSubmissionIdFromUploadActivityUrl() {
		String url = WebUI.getUrl()
		if (url == null || WebUI.getUrl().trim().isEmpty()) return null
		java.util.regex.Pattern p = ~/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/
		java.util.regex.Matcher m = p.matcher(url)
		String submissionId = m.find() ? m.group(0) : null
		KeywordUtil.logInfo("Submission ID: ${submissionId}")
		return submissionId
	}

	/**
	 * This function verifies the headers of the data submission dashboard's tabs: Upload Activities, Validation Results, Data View.
	 * Compares expected headers from Data Files/CRDC/DataSubmissions/Headers-&lt;tab&gt; to actual UI table headers and reports via ValidationReport.
	 * @param tab Tab name: UploadActivities, ValidationResults, or DataView
	 */
	@Keyword
	void verifyDataSubmissionTableHeaders(String tab) {
		try {
			//Get list of expected headers
			TestRunner.clickTab('CRDC/DataSubmissions/Validation/' + tab + '-Tab')
			List<String> expectedHeaders = Utils.getListFromDataFile('Data Files/CRDC/DataSubmissions/Headers-' + tab)

			//Get list of actual headers
			List<WebElement> headerList = Utils.getListWebElements('CRDC/DataSubmissions/Validation/Table-Headers')
			List<String> actualHeaders = new ArrayList<>()
			for (WebElement header : headerList) {
				actualHeaders.add(header.getText().trim())
			}
			String sectionName = tabToSectionName(tab)
			boolean anyFail = false
			int maxLen = Math.max(expectedHeaders.size(), actualHeaders.size())
			for (int i = 0; i < maxLen; i++) {
				String exp = i < expectedHeaders.size() ? expectedHeaders.get(i) : "(missing)"
				String act = i < actualHeaders.size() ? actualHeaders.get(i) : "(missing)"
				String status = (i < expectedHeaders.size() && i < actualHeaders.size() && exp.equals(act)) ? "Pass" : "Fail"
				if (status == "Fail") anyFail = true
				ValidationReport.addValidationReportItem(sectionName, "Header " + (i + 1), exp, act, status)
			}
			if (anyFail) {
				KeywordUtil.markFailed("Header mismatch. Expected: " + expectedHeaders + ", Actual: " + actualHeaders)
			} else {
				KeywordUtil.markPassed("Headers match: " + actualHeaders)
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("Exception occurred: " + e.getMessage())
		}
	}

	// --- Upload Activities verification: verifyUploadActivitiesBatchRow, verifyUploadActivitiesFailedErrorsModal, loadAggregatedExpectedErrorsForUi (expected TSV loading) ---

	/** Column name candidates for Upload Activities batch table (flexible matching). These constants are used for column resolution, not for validating headers. */
	private static final List<String> UPLOAD_ACTIVITIES_TYPE_CANDIDATES = ["Batch Type", "Type"]
	private static final List<String> UPLOAD_ACTIVITIES_FILE_COUNT_CANDIDATES = ["File Count", "Files"]
	private static final List<String> UPLOAD_ACTIVITIES_STATUS_CANDIDATES = ["Status"]

	/** Helper method for getting header index for verifyUploadActivitiesBatchRow */
	private static int findHeaderIndex(List<String> headers, List<String> candidates) {
		for (String cand : candidates) {
			int idx = headers.findIndexOf { it != null && it.equalsIgnoreCase(cand.trim()) }
			if (idx >= 0) return idx
		}
		return -1
	}

	/** Default subfolder under InputFiles/CRDC/MetadataData/<tcName> for expected TSV(s) used by Upload Errors modal verification. */
	private static final String EXPECTED_SUBFOLDER_UI = "expected"
	/** Column name in expected TSV for expected error(s); used by loadAggregatedExpectedErrorsForUi. */
	private static final List<String> EXPECTED_ERROR_COLUMN_CANDIDATES_UI = [
		"expected_errors",
		"expected_error"
	]
	/** Column names for file name in expected TSV; used by loadAggregatedExpectedErrorsForUi. */
	private static final List<String> FILE_NAME_COLUMN_CANDIDATES_UI = [
		"file_name",
		"internal_file_name",
		"fileName"
	]

	/** Find column index by name (case-insensitive) in a TSV header line; returns -1 if not found. Used for expected-errors TSV loading. Trims header cells so CRLF or spaces do not break matching. */
	private static int findColumnIndexForExpectedTsv(String headerLine, List<String> candidates) {
		String[] headers = headerLine.split("\t", -1)
		for (String cand : candidates) {
			String c = cand != null ? cand.trim() : ""
			if (c.isEmpty()) continue
				int idx = headers.findIndexOf { it != null && it.trim().equalsIgnoreCase(c) }
			if (idx >= 0) return idx
		}
		return -1
	}

	/** Parse an expected_errors cell value (e.g. "['msg1', 'msg2']" or "msg1") into a list of individual error strings. */
	private static List<String> parseExpectedErrorsToList(String raw) {
		List<String> out = []
		if (raw == null) return out
		String s = raw.trim()
		if (s.isEmpty()) return out
		if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
			s = s.substring(1, s.length() - 1).replace("\"\"", "\"").trim()
		}
		if (s.startsWith("[") && s.endsWith("]")) {
			String inner = s.substring(1, s.length() - 1).trim()
			if (inner.isEmpty()) return out
			String delim = "||SEP||"
			String innerNorm = inner.replace("', '", delim).replace("\", \"", delim).replace("', \"", delim).replace("\", '", delim)
			for (String segment : innerNorm.split(java.util.regex.Pattern.quote(delim), -1)) {
				String t = segment.trim()
				if (t.startsWith("'") && t.endsWith("'")) t = t.substring(1, t.length() - 1)
				else if (t.startsWith("\"") && t.endsWith("\"")) t = t.substring(1, t.length() - 1)
				t = t.trim()
				if (!t.isEmpty()) out.add(t)
			}
			return out
		}
		out.add(s)
		return out
	}

	/** Load and aggregate all expected error strings from TSV(s) in InputFiles/CRDC/MetadataData/<tcName>/expected/. Used by verifyUploadErrorsModalCompare. */
	private static List<String> loadAggregatedExpectedErrorsForUi(String tcName) {
		List<String> aggregated = []
		if (tcName == null || tcName.trim().isEmpty()) return aggregated
		String projectDir = RunConfiguration.getProjectDir()
		File expectedDir = new File(projectDir, "InputFiles/CRDC/MetadataData/" + tcName.trim() + "/" + EXPECTED_SUBFOLDER_UI)
		if (!expectedDir.exists() || !expectedDir.isDirectory()) return aggregated
		File[] tsvs = expectedDir.listFiles({ File f -> f.isFile() && f.name.toLowerCase().endsWith(".tsv") } as FileFilter)
		if (tsvs == null || tsvs.length == 0) return aggregated
		for (File tsv : tsvs) {
			List<String> lines = Files.readAllLines(tsv.toPath(), StandardCharsets.UTF_8)
			if (lines.size() < 2) continue
				String header = lines.get(0)
			int expectedColIdx = findColumnIndexForExpectedTsv(header, EXPECTED_ERROR_COLUMN_CANDIDATES_UI)
			if (expectedColIdx < 0) continue
				for (int i = 1; i < lines.size(); i++) {
					String[] cols = lines.get(i).split("\t", -1)
					String expectedVal = expectedColIdx < cols.length ? cols[expectedColIdx].trim() : ""
					if (expectedVal.isEmpty()) continue
						aggregated.addAll(parseExpectedErrorsToList(expectedVal))
				}
		}
		return aggregated
	}

	/**
	 * Polls the Upload Activities table until no row Status cell equals "Uploading" (case-insensitive),
	 * or until timeoutSeconds elapses (2 second poll intervals). Requires Upload Activities tab to be visible.
	 * Use before verifyUploadActivitiesBatchRow when CLI-uploaded batches may show Uploading before final status.
	 *
	 * @param timeoutSeconds Maximum seconds to wait (e.g. 120)
	 */
	@Keyword
	void waitForUploadActivitiesTableNoUploading(int timeoutSeconds) {
		int effectiveTimeout = timeoutSeconds > 0 ? timeoutSeconds : 120
		long deadlineMs = System.currentTimeMillis() + (long) effectiveTimeout * 1000L
		final String uploadingLabel = "Uploading"

		while (System.currentTimeMillis() < deadlineMs) {
			try {
				List<WebElement> headerList = Utils.getListWebElements('CRDC/DataSubmissions/Validation/Table-Headers')
				if (headerList == null || headerList.isEmpty()) {
					Thread.sleep(2000)
					continue
				}
				List<String> headerLabels = headerList.collect { it.getText()?.trim() ?: '' }
				int statusIdx = findHeaderIndex(headerLabels, UPLOAD_ACTIVITIES_STATUS_CANDIDATES)
				if (statusIdx < 0) {
					Thread.sleep(2000)
					continue
				}
				List<WebElement> rows = Utils.getListWebElements('CRDC/AILocators/GenericTable/TableRow')
				if (rows == null || rows.isEmpty()) {
					Thread.sleep(2000)
					continue
				}
				boolean anyUploading = false
				for (WebElement row : rows) {
					try {
						List<WebElement> cells = row.findElements(By.xpath(".//td"))
						if (statusIdx >= cells.size()) continue
						String rowStatus = cells.get(statusIdx).getText()?.trim() ?: ""
						if (uploadingLabel.equalsIgnoreCase(rowStatus)) {
							anyUploading = true
							break
						}
					} catch (StaleElementReferenceException e) {
						KeywordUtil.logInfo("waitForUploadActivitiesTableNoUploading: Stale row, retrying...")
					}
				}
				if (!anyUploading) {
					KeywordUtil.logInfo("waitForUploadActivitiesTableNoUploading: No 'Uploading' status in Upload Activities rows.")
					return
				}
				KeywordUtil.logInfo("waitForUploadActivitiesTableNoUploading: Still seeing Uploading, waiting...")
				Thread.sleep(2000)
			} catch (Exception e) {
				KeywordUtil.logInfo("waitForUploadActivitiesTableNoUploading: " + e.getMessage())
				Thread.sleep(2000)
			}
		}
		KeywordUtil.markFailed("waitForUploadActivitiesTableNoUploading: Timeout after " + effectiveTimeout + "s; Uploading still present in Upload Activities.")
	}

	/**
	 * Verifies that the Upload Activities table contains a batch row matching the given criteria.
	 * Caller must ensure the Upload Activities tab is visible before invoking.
	 *
	 * @param batchType Expected batch type (e.g. "Data File", "Metadata")
	 * @param expectedFileCount Expected file count in the batch
	 * @param expectedStatus Expected status value (e.g. "Uploaded", "Uploading", "Failed")
	 */
	@Keyword
	void verifyUploadActivitiesBatchRow(String batchType, int expectedFileCount, String expectedStatus) {
		String sectionName = "Upload Activities"
		String batchLabel = "Batch " + batchType + " (" + expectedFileCount + " files)"
		try {
			if (expectedStatus == null || expectedStatus.trim().isEmpty()) {
				ValidationReport.addValidationReportItem(sectionName, "Expected status provided", "non-empty", "(empty)", "Fail")
				KeywordUtil.markFailed("verifyUploadActivitiesBatchRow: expectedStatus is required.")
				return
			}
			WebDriver driver = DriverFactory.getWebDriver()

			List<WebElement> headerList = Utils.getListWebElements('CRDC/DataSubmissions/Validation/Table-Headers')
			if (headerList == null || headerList.isEmpty()) {
				ValidationReport.addValidationReportItem(sectionName, "Headers present", "present", "not found", "Fail")
				KeywordUtil.markFailed("verifyUploadActivitiesBatchRow: No table headers found. Ensure Upload Activities tab is visible.")
				return
			}
			ValidationReport.addValidationReportItem(sectionName, "Headers present", "present", "found", "Pass")
			List<String> headerLabels = headerList.collect { it.getText().trim() }
			int typeIdx = findHeaderIndex(headerLabels, UPLOAD_ACTIVITIES_TYPE_CANDIDATES)
			int fileCountIdx = findHeaderIndex(headerLabels, UPLOAD_ACTIVITIES_FILE_COUNT_CANDIDATES)
			int statusIdx = findHeaderIndex(headerLabels, UPLOAD_ACTIVITIES_STATUS_CANDIDATES)

			if (typeIdx < 0 || fileCountIdx < 0 || statusIdx < 0) {
				ValidationReport.addValidationReportItem(sectionName, "Required columns found", "Batch Type, File Count, Status", String.valueOf(headerLabels), "Fail")
				KeywordUtil.markFailed("verifyUploadActivitiesBatchRow: Could not find Batch Type, File Count, or Status columns. Headers: " + headerLabels)
				return
			}
			ValidationReport.addValidationReportItem(sectionName, "Required columns found", "Batch Type, File Count, Status", "found", "Pass")

			List<WebElement> rows = Utils.getListWebElements('CRDC/AILocators/GenericTable/TableRow')
			if (rows == null || rows.isEmpty()) {
				ValidationReport.addValidationReportItem(sectionName, batchLabel, "present", "0 rows", "Fail")
				KeywordUtil.markFailed("verifyUploadActivitiesBatchRow: No batch rows found. Expected a " + batchType + " batch with " + expectedFileCount + " file(s).")
				return
			}

			boolean found = false
			String mismatchDetail = ""
			for (WebElement row : rows) {
				try {
					List<WebElement> cells = row.findElements(By.xpath(".//td"))
					if (cells.size() <= Math.max(typeIdx, Math.max(fileCountIdx, statusIdx))) continue

						String rowType = (typeIdx < cells.size()) ? cells.get(typeIdx).getText().trim() : ""
					String rowFileCountStr = (fileCountIdx < cells.size()) ? cells.get(fileCountIdx).getText().trim() : ""
					String rowStatus = (statusIdx < cells.size()) ? cells.get(statusIdx).getText().trim() : ""

					boolean typeMatch = rowType.equalsIgnoreCase(batchType)
					int actualFileCount = -1
					try {
						actualFileCount = Integer.parseInt(rowFileCountStr.replaceAll("[^0-9]", ""))
					} catch (Exception e) {
						/* ignore */
					}
					boolean fileCountMatch = (actualFileCount >= 0 && actualFileCount == expectedFileCount)
					boolean statusMatch = rowStatus.equalsIgnoreCase(expectedStatus.trim())

					if (typeMatch && fileCountMatch && statusMatch) {
						found = true
						ValidationReport.addValidationReportItem(sectionName, batchLabel, "present", "found", "Pass")
						KeywordUtil.logInfo("verifyUploadActivitiesBatchRow: Found matching " + batchType + " batch: fileCount=" + actualFileCount + ", status=" + rowStatus)
						break
					}
					if (typeMatch && !fileCountMatch) {
						mismatchDetail = "fileCount expected=" + expectedFileCount + ", actual=" + rowFileCountStr
						break
					}
					if (typeMatch && !statusMatch) {
						mismatchDetail = "status expected='" + expectedStatus + "', actual=" + rowStatus
						break
					}
				} catch (StaleElementReferenceException e) {
					KeywordUtil.logInfo("verifyUploadActivitiesBatchRow: Stale element, retrying...")
				}
			}

			if (!found) {
				String msg = "verifyUploadActivitiesBatchRow: No matching " + batchType + " batch with " + expectedFileCount + " file(s)"
				if (mismatchDetail) msg += ". " + mismatchDetail
				ValidationReport.addValidationReportItem(sectionName, batchLabel, "present", "not found", "Fail")
				KeywordUtil.markFailed(msg)
			}
		} catch (Exception e) {
			ValidationReport.addValidationReportItem(sectionName, batchLabel, "no exception", e.getMessage(), "Fail")
			KeywordUtil.markFailed("verifyUploadActivitiesBatchRow: " + e.getMessage())
		}
	}

	/**
	 * Clicks the Upload Errors link in the Upload Activities table, reads the error-details modal content,
	 * and verifies the displayed errors match the expected errors from TSV(s) in
	 * InputFiles/CRDC/MetadataData/&lt;tcName&gt;/expected/. Assumes caller is on the data submission detail
	 * page with Upload Activities tab selected and at least one batch has errors (e.g. Failed with "X Errors" link).
	 * Uses loadAggregatedExpectedErrorsForUi(tcName) for expected list; compares sorted lists.
	 * @param tcName Test case name (e.g. "CRDCDH-1405_TC104") used to resolve expected folder
	 */
	@Keyword
	void verifyUploadActivitiesFailedErrorsModal(String tcName) {
		String sectionName = "Upload Errors Modal"
		if (tcName == null || tcName.trim().isEmpty()) {
			ValidationReport.addValidationReportItem(sectionName, "TC name provided", "non-empty", "(empty)", "Fail")
			KeywordUtil.markFailed("verifyUploadActivitiesFailedErrorsModal: tcName is required.")
			return
		}
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			// 1. Click first Upload Errors link
			TestObject errorCountBtn = findTestObject("CRDC/AILocators/DataSubmissions/Detail/UploadActivity/ErrorCount-Btn")
			boolean errorLinkPresent = WebUI.waitForElementPresent(errorCountBtn, 10)
			if (!errorLinkPresent) {
				ValidationReport.addValidationReportItem(sectionName, "Error link clickable", "present", "not found", "Fail")
				KeywordUtil.markFailed("verifyUploadActivitiesFailedErrorsModal: Upload Errors link not found.")
				return
			}
			ValidationReport.addValidationReportItem(sectionName, "Error link clickable", "present", "found", "Pass")
			WebUI.click(errorCountBtn)
			WebUI.delay(1)
			// 2. Wait for modal
			boolean modalPresent = WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/DataSubmissions/ErrorDetailsDialog/Dialog"), 10)
			if (!modalPresent) {
				ValidationReport.addValidationReportItem(sectionName, "Modal displayed", "displayed", "not displayed", "Fail")
				KeywordUtil.markFailed("verifyUploadActivitiesFailedErrorsModal: Error details modal did not appear.")
				return
			}
			ValidationReport.addValidationReportItem(sectionName, "Modal displayed", "displayed", "displayed", "Pass")
			// 3. Read modal error list (v1: items are "1. msg", "2. msg" inside error-details-error-list)
			String listContainerXpath = givexpath("CRDC/AILocators/DataSubmissions/ErrorDetailsDialog/ErrorList-Container")
			WebElement listContainer = driver.findElement(By.xpath(listContainerXpath))
			List<WebElement> itemElements = Utils.getListWebElements("CRDC/AILocators/DataSubmissions/ErrorDetailsDialog/ErrorList-Item")
			if (itemElements.isEmpty()) {
				// Fallback: container text may be in direct structure
				String fullText = listContainer.getText()
				List<String> actualErrors = []
				if (fullText != null && !fullText.trim().isEmpty()) {
					String[] parts = fullText.split(/\d+\.\s/)
					for (int i = 1; i < parts.length; i++) {
						String t = parts[i].trim()
						if (!t.isEmpty()) actualErrors.add(t)
					}
				}
				verifyUploadErrorsModalCompare(tcName, actualErrors)
				return
			}
			List<String> actualErrors = []
			for (WebElement el : itemElements) {
				String t = el.getText()?.trim() ?: ""
				if (t.matches(/^\d+\.\s+.+/)) {
					actualErrors.add(t.replaceFirst(/^\d+\.\s*/, ""))
				}
			}
			verifyUploadErrorsModalCompare(tcName, actualErrors)
		} catch (Exception e) {
			ValidationReport.addValidationReportItem(sectionName, "Modal verification exception", "no exception", e.getMessage(), "Fail")
			KeywordUtil.markFailed("verifyUploadActivitiesFailedErrorsModal: " + e.getMessage())
		} finally {
			try {
				WebUI.click(findTestObject("CRDC/AILocators/DataSubmissions/ErrorDetailsDialog/Close-Btn"))
			} catch (Exception ignore) {}
		}
	}

	/** Normalize Unicode curly/smart quotes to ASCII straight quote so UI (curly) and TSV (straight) match. */
	private static String normalizeQuotesForComparison(String s) {
		if (s == null) return ""
		return s.replace('\u201C', '"').replace('\u201D', '"').replace('\u201E', '"').replace('\u201F', '"')
	}

	private void verifyUploadErrorsModalCompare(String tcName, List<String> actualErrors) {
		List<String> expectedErrors = loadAggregatedExpectedErrorsForUi(tcName)
		if (expectedErrors.isEmpty()) {
			ValidationReport.addValidationReportItem("Upload Errors Modal", "Expected errors loaded", "at least 1 error", "0 loaded", "Fail")
			KeywordUtil.markFailed("verifyUploadActivitiesFailedErrorsModal: No expected errors loaded from InputFiles/CRDC/MetadataData/" + tcName + "/expected/")
			return
		}
		ValidationReport.addValidationReportItem("Upload Errors Modal", "Expected errors loaded", "at least 1 error", String.valueOf(expectedErrors.size()) + " loaded", "Pass")
		List<String> expectedSorted = new ArrayList<>(expectedErrors.collect { normalizeQuotesForComparison(it) }).sort()
		List<String> actualSorted = new ArrayList<>(actualErrors.collect { normalizeQuotesForComparison(it) }).sort()
		boolean match = expectedSorted == actualSorted
		String status = match ? "Pass" : "Fail"
		String expectedDisplay = expectedSorted.size() + " error(s)"
		String actualDisplay = actualSorted.size() + " error(s)" + (match ? "" : " (mismatch)")
		ValidationReport.addValidationReportItem("Upload Errors Modal", "Errors vs expected (" + tcName + ")", expectedDisplay, actualDisplay, status)
		if (match) {
			KeywordUtil.logInfo("[Upload Errors Modal] Expected: " + expectedSorted)
			KeywordUtil.logInfo("[Upload Errors Modal] Actual: " + actualSorted)
		}
		if (!match) {
			List<String> missing = expectedSorted.findAll { !actualSorted.contains(it) }
			List<String> extra = actualSorted.findAll { !expectedSorted.contains(it) }
			if (!missing.isEmpty()) KeywordUtil.logInfo("verifyUploadActivitiesFailedErrorsModal: Expected but not in modal: " + missing.take(3) + (missing.size() > 3 ? "..." : ""))
			if (!extra.isEmpty()) KeywordUtil.logInfo("verifyUploadActivitiesFailedErrorsModal: In modal but not expected: " + extra.take(3) + (extra.size() > 3 ? "..." : ""))
			KeywordUtil.markFailed("verifyUploadActivitiesFailedErrorsModal: Modal errors do not match expected. Expected " + expectedSorted.size() + ", actual " + actualSorted.size() + ".")
		}
	}

	/**
	 * Verifies Data View table rows match the upload metadata TSV for the given node.
	 * Keeps all TSV columns except type and status (relationship columns with '.' are included).
	 * Maps TSV headers to UI columns by name (not input file column index), checks alphabetical
	 * header order after ID and Status, then compares cell values row-by-row.
	 * Truncated cells use aria-label from truncated-text-wrapper when present.
	 * @param metadataFile Relative path to metadata TSV (e.g. InputFiles/CRDC/MetadataData/TC01/file.tsv)
	 * @param node Node name to select in Data View dropdown (e.g. consent_group, participant)
	 */
	@Keyword
	void verifyDataViewMatchesMetadata(String metadataFile, String node) {
		KeywordUtil.logInfo("--- Verifying Data View for node: " + node + " ---")
		try {
			WebDriver driver = DriverFactory.getWebDriver()

			// Step 1: Load and parse TSV
			Path filePath = Paths.get(RunConfiguration.getProjectDir(), metadataFile)
			List<String> lines = Files.readAllLines(filePath)
			if (lines.size() <= 1) {
				KeywordUtil.markFailed("TSV file does not contain data rows.")
			}

			// Step 2: All columns except type/status (include relationship columns shown in the UI)
			List<String> expectedHeaderNames = new ArrayList<>()
			List<List<String>> expectedRows = parseDataViewMetadataTsv(lines, expectedHeaderNames)

			// Step 3: Select node from dropdown and wait for table refresh
			WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validation/NodeType-Ddn'), 30)
			WebUI.click(findTestObject('CRDC/DataSubmissions/Validation/NodeType-Ddn'))
			WebUI.delay(1)
			WebElement option = driver.findElement(By.xpath("//li[normalize-space(text())='" + node + "']"))
			option.click()
			WebUI.delay(1)

			// Step 4: Read UI headers and rows (map by header name, not positional index)
			List<String> uiHeaders = getDataViewTableHeaderNames(driver)
			String sectionName = "Data View"
			boolean anyFail = false
			if (!verifyDataViewColumnHeaderOrder(uiHeaders, sectionName, node)) {
				anyFail = true
			}

			List<List<String>> uiRows = readDataViewTableRows(driver)
			if (uiRows.size() == 20) {
				KeywordUtil.logInfo("Full table")
			} else if (uiRows.size() != expectedRows.size()) {
				KeywordUtil.markFailed("Row count mismatch: UI has " + uiRows.size() + ", but file has " + expectedRows.size())
			}

			int[] expectedToUiIndex = mapDataViewHeadersToUiIndices(expectedHeaderNames, uiHeaders)
			for (int c = 0; c < expectedHeaderNames.size(); c++) {
				String expHeader = expectedHeaderNames.get(c)?.trim() ?: ""
				int uiIdx = c < expectedToUiIndex.length ? expectedToUiIndex[c] : -1
				String actHeader = (uiIdx >= 0 && uiIdx < uiHeaders.size()) ? (uiHeaders.get(uiIdx)?.trim() ?: "") : ""
				boolean found = uiIdx >= 0 && !actHeader.isEmpty()
				if (!found) {
					anyFail = true
					ValidationReport.addValidationReportItem(sectionName, "Column '" + expHeader + "'", "present in table", "missing", "Fail")
				}
			}

			int maxRows = Math.min(uiRows.size(), expectedRows.size())
			for (int i = 0; i < maxRows; i++) {
				List<String> expectedValues = expectedRows.get(i)
				List<String> actRow = uiRows.get(i)
				for (int j = 0; j < expectedValues.size(); j++) {
					String expected = expectedValues.get(j)
					int uiIdx = j < expectedToUiIndex.length ? expectedToUiIndex[j] : -1
					String actual = (uiIdx >= 0 && uiIdx < actRow.size()) ? (actRow.get(uiIdx) ?: "") : ""
					String cleanedUi = actual.replace("...", "").trim()
					boolean match = actual.equals(expected) || expected.contains(cleanedUi)
					if (!match) anyFail = true
					String colLabel = j < expectedHeaderNames.size() ? expectedHeaderNames.get(j) : String.valueOf(j + 1)
					ValidationReport.addValidationReportItem(sectionName, "Row " + (i + 1) + " " + colLabel, expected, actual, match ? "Pass" : "Fail")
				}
			}
			if (anyFail) {
				KeywordUtil.markFailed("Data View: one or more row/column mismatches. See validation report.")
			} else {
				KeywordUtil.logInfo("Finished verifying-- Data View tab in UI matches the contents of the TSV (ignoring type and status).")
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("Exception in verifyDataViewMatchesMetadata(): " + e.getMessage())
		}
	}

	/**
	 * Parse upload metadata TSV for Data View UI verification.
	 * Keeps all columns except type and status (relationship columns with '.' are included).
	 */
	private static List<List<String>> parseDataViewMetadataTsv(List<String> lines, List<String> expectedHeaderNames) {
		List<List<String>> rows = new ArrayList<>()
		if (lines == null || lines.isEmpty()) return rows
		String[] headerLine = lines.get(0).split("\t", -1)
		List<Integer> includedIndexes = new ArrayList<>()
		for (int j = 0; j < headerLine.length; j++) {
			String h = headerLine[j].trim()
			if (h.equalsIgnoreCase("type") || h.equalsIgnoreCase("status")) continue
			includedIndexes.add(j)
			expectedHeaderNames.add(h)
		}
		for (int i = 1; i < lines.size(); i++) {
			String[] fields = lines.get(i).split("\t", -1)
			List<String> row = new ArrayList<>()
			for (Integer idx : includedIndexes) {
				String val = idx < fields.length ? fields[idx].trim() : ""
				if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
					val = val.substring(1, val.length() - 1)
				}
				row.add(val)
			}
			rows.add(row)
		}
		return rows
	}

	/** Header labels from the Data View generic table (single JS read for speed). */
	private static List<String> getDataViewTableHeaderNames(WebDriver driver) {
		JavascriptExecutor js = (JavascriptExecutor) driver
		String script = "var headers = document.querySelectorAll(\"[data-testid^='generic-table-header-']\"); " +
				"var names = []; " +
				"for (var i = 0; i < headers.length; i++) { " +
				"  var h = headers[i]; " +
				"  var text = (h.innerText || '').replace(/\\s+/g, ' ').trim(); " +
				"  if (text) { names.push(text); } " +
				"  else { " +
				"    var tid = h.getAttribute('data-testid') || ''; " +
				"    if (tid.indexOf('generic-table-header-') === 0) names.push(tid.replace('generic-table-header-', '')); " +
				"  } " +
				"} " +
				"return names;"
		List<String> names = (List<String>) js.executeScript(script)
		if (names != null && !names.isEmpty()) return names
		// Fallback when generic-table-header is not used
		List<WebElement> headers = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Headers")
		List<String> fallback = new ArrayList<>()
		for (WebElement h : headers) {
			String text = h.getText()?.replaceAll(/\s+/, ' ')?.trim()
			if (text != null && !text.isEmpty()) fallback.add(text)
		}
		return fallback
	}

	/**
	 * Reads all Data View table rows in one JS call (same approach as DataExplorer.readTableRows).
	 * Per-cell WebDriver findElement was very slow due to implicit waits on missing nested elements.
	 */
	private static List<List<String>> readDataViewTableRows(WebDriver driver) {
		JavascriptExecutor js = (JavascriptExecutor) driver
		String script = "var rows = document.querySelectorAll(\"[data-testid='generic-table-row']\"); " +
				"var out = []; " +
				"for (var i = 0; i < rows.length; i++) { " +
				"  var cells = rows[i].querySelectorAll('td'); " +
				"  var row = []; " +
				"  for (var j = 0; j < cells.length; j++) { " +
				"    var cell = cells[j]; " +
				"    var trunc = cell.querySelector(\"[data-testid='truncated-text-wrapper']\"); " +
				"    var val = ''; " +
				"    if (trunc) { " +
				"      var aria = trunc.getAttribute('aria-label'); " +
				"      val = (aria && aria.trim()) ? aria.trim() : (cell.innerText || '').trim(); " +
				"    } else { " +
				"      val = (cell.innerText || '').trim(); " +
				"    } " +
				"    row.push(val); " +
				"  } " +
				"  out.push(row); " +
				"} " +
				"return out;"
		List<List<String>> raw = (List<List<String>>) js.executeScript(script)
		List<List<String>> result = new ArrayList<>()
		if (raw == null) return result
		for (List<String> row : raw) {
			result.add(row != null ? new ArrayList<>(row) : new ArrayList<>())
		}
		return result
	}

	/**
	 * Sort key for Data View column headers: case-insensitive, with '.' ordered after letters and '_'
	 * (matches UI/export so sample_type_for_implantation sorts before sample.sample_id).
	 */
	private static String dataViewColumnSortKey(String header) {
		if (header == null) return ""
		return header.toLowerCase().replace(".", "\u00ff")
	}

	/**
	 * Verifies Data View header order: optional checkbox column ("Select All" or "checkbox"), then primary key
	 * and Status pinned, then remaining columns alphabetically (case-insensitive; '.' after '_'). Skips Orphaned.
	 * When Status is not present in the header DOM read, skips only the checkbox (if any) and ID column.
	 * @return true when order matches
	 */
	private static boolean verifyDataViewColumnHeaderOrder(List<String> uiHeaders, String sectionName, String node) {
		String itemLabel = "Column order (after ID, Status)" + (node ? " [" + node + "]" : "")
		if (uiHeaders == null || uiHeaders.isEmpty()) {
			ValidationReport.addValidationReportItem(sectionName, itemLabel, "headers present", "none", "Fail")
			return false
		}
		int startIndex = 0
		String firstHeader = uiHeaders.get(0)?.trim() ?: ""
		if (firstHeader.equalsIgnoreCase("Select All") || firstHeader.equalsIgnoreCase("checkbox")) {
			startIndex = 1
		}
		if (uiHeaders.size() <= startIndex) {
			ValidationReport.addValidationReportItem(sectionName, itemLabel, "ID column", "missing", "Fail")
			return false
		}
		int statusIdx = -1
		for (int i = startIndex; i < uiHeaders.size(); i++) {
			String h = uiHeaders.get(i)?.trim() ?: ""
			if (h.equalsIgnoreCase("Status")) {
				statusIdx = i
				break
			}
		}
		int sortableStart = (statusIdx >= 0) ? statusIdx + 1 : startIndex + 1
		if (uiHeaders.size() <= sortableStart) {
			ValidationReport.addValidationReportItem(sectionName, itemLabel, "sortable columns", "none after pinned columns", "Fail")
			return false
		}
		List<String> sortableHeaders = new ArrayList<>()
		for (int i = sortableStart; i < uiHeaders.size(); i++) {
			String h = uiHeaders.get(i)?.trim() ?: ""
			if (h.isEmpty()) continue
			if (h.equalsIgnoreCase("Orphaned")) continue
			sortableHeaders.add(h)
		}
		List<String> expectedOrder = new ArrayList<>(sortableHeaders)
		Collections.sort(expectedOrder, { String a, String b ->
			dataViewColumnSortKey(a) <=> dataViewColumnSortKey(b)
		})
		boolean ok = true
		if (sortableHeaders.size() != expectedOrder.size()) {
			ok = false
		} else {
			for (int i = 0; i < sortableHeaders.size(); i++) {
				if (dataViewColumnSortKey(sortableHeaders.get(i)) != dataViewColumnSortKey(expectedOrder.get(i))) {
					ok = false
					break
				}
			}
		}
		String actualJoined = sortableHeaders.join("; ")
		String expectedJoined = expectedOrder.join("; ")
		ValidationReport.addValidationReportItem(sectionName, itemLabel, expectedJoined, actualJoined, ok ? "Pass" : "Fail")
		return ok
	}

	/**
	 * Maps TSV header names to UI column indices (case-insensitive; underscores vs spaces normalized).
	 */
	private static int[] mapDataViewHeadersToUiIndices(List<String> expectedHeaderNames, List<String> uiHeaders) {
		int[] map = new int[expectedHeaderNames.size()]
		Arrays.fill(map, -1)
		for (int c = 0; c < expectedHeaderNames.size(); c++) {
			String exp = expectedHeaderNames.get(c)
			if (exp == null) continue
			String expNorm = exp.replace("_", " ").trim()
			for (int u = 0; u < uiHeaders.size(); u++) {
				String ui = uiHeaders.get(u)
				if (ui == null) continue
				String uiNorm = ui.replace("_", " ").trim()
				if (ui.equalsIgnoreCase(exp) || uiNorm.equalsIgnoreCase(expNorm)) {
					map[c] = u
					break
				}
			}
		}
		return map
	}

	/**
	 * Project Downloads folder. Chrome is configured to save downloads here (see CustomBrowserDriver).
	 * Same path on local and CI so no move is needed.
	 */
	private static File getProjectDownloadsDir() {
		File dir = new File(RunConfiguration.getProjectDir(), "Downloads")
		if (!dir.exists()) dir.mkdirs()
		return dir
	}

	/**
	 * Extract node name from an expected TSV filename.
	 * e.g. GC_consent_group_v9.0.0_expected.tsv -> consent_group
	 */
	private static String nodeFromExpectedFilename(String filename) {
		if (!filename.endsWith("_expected.tsv")) return filename.replaceFirst(~/\.[^.]+$/, "").replaceFirst(/^[^_]+_/, "")
		String base = filename - ~/_expected\.tsv$/
		base = base.replaceFirst(/_v\d+\.\d+\.\d+$/, "")
		return base.contains("_") ? base.substring(base.indexOf("_") + 1) : base
	}

	/**
	 * List expected TSV files in InputFiles/CRDC/MetadataData/&lt;tcName&gt;/expected/.
	 * Returns list of [node, expectedFile] (node = extracted from filename).
	 */
	private List<List<?>> listExpectedFilesForTC(String tcName) {
		String baseDir = RunConfiguration.getProjectDir()
		File expectedDir = new File(baseDir, "InputFiles/CRDC/MetadataData/${tcName}/expected")
		if (!expectedDir.exists() || !expectedDir.isDirectory()) {
			KeywordUtil.markFailed("downloadAndCompareExpectedForTC: expected folder not found: ${expectedDir.absolutePath}")
			return []
		}
		List<File> fileList = expectedDir.listFiles()?.findAll { it.isFile() && it.name.endsWith("_expected.tsv") } ?: []
		if (fileList.isEmpty()) {
			KeywordUtil.markFailed("downloadAndCompareExpectedForTC: no *_expected.tsv files in ${expectedDir.absolutePath}")
			return []
		}
		List<List<?>> out = []
		for (File f in fileList) {
			String node = nodeFromExpectedFilename(f.name)
			out.add([node, f.absolutePath])
		}
		return out
	}

	/**
	 * Find the most recently modified file in a directory.
	 */
	private static File findLatestFile(File dir) {
		List<File> files = dir.listFiles()?.findAll { it.isFile() } ?: []
		if (files.isEmpty()) return null
		return files.max { it.lastModified() }
	}

	/**
	 * Compare two TSV files in full: headers and all data rows, cell-by-cell (trimmed).
	 * Collects all mismatches, logs them, then fails once at the end with the full output (no fail on first mismatch).
	 */
	private void compareTsvFullContent(String expectedPath, String actualPath) {
		Path expPath = Paths.get(expectedPath)
		Path actPath = Paths.get(actualPath)
		if (!Files.exists(expPath)) {
			KeywordUtil.markFailed("Expected file not found: " + expectedPath)
			return
		}
		if (!Files.exists(actPath)) {
			KeywordUtil.markFailed("Downloaded file not found: " + actualPath)
			return
		}
		List<String> expLines = Files.readAllLines(expPath)
		List<String> actLines = Files.readAllLines(actPath)
		List<String> errors = new ArrayList<>()
		String sectionName = "Data View (Expected vs Downloaded)"
		// Row count check
		if (expLines.size() != actLines.size()) {
			errors.add("TSV row count mismatch: expected ${expLines.size()}, actual ${actLines.size()} (${actualPath})")
			ValidationReport.addValidationReportItem(sectionName, "Row count", String.valueOf(expLines.size()), String.valueOf(actLines.size()), "Fail")
		}
		// Compare line by line (headers and data rows), cell-by-cell trimmed
		int maxRows = Math.min(expLines.size(), actLines.size())
		for (int i = 0; i < maxRows; i++) {
			String expLine = expLines.get(i)
			String actLine = actLines.get(i)
			String[] expCells = expLine.split("\t", -1)
			String[] actCells = actLine.split("\t", -1)
			boolean lineOk = true
			if (expCells.length != actCells.length) {
				errors.add("Line ${i + 1}: column count expected ${expCells.length}, actual ${actCells.length}")
				lineOk = false
			}
			int maxCols = Math.min(expCells.length, actCells.length)
			for (int j = 0; j < maxCols; j++) {
				String expVal = expCells[j].trim()
				String actVal = actCells[j].trim()
				if (expVal != actVal) {
					errors.add("Line ${i + 1}, column ${j + 1}: expected '${expVal}', actual '${actVal}'")
					lineOk = false
				}
			}
			if (expCells.length != actCells.length) {
				for (int j = maxCols; j < Math.max(expCells.length, actCells.length); j++) {
					String expVal = j < expCells.length ? expCells[j].trim() : "(missing)"
					String actVal = j < actCells.length ? actCells[j].trim() : "(missing)"
					errors.add("Line ${i + 1}, column ${j + 1}: expected '${expVal}', actual '${actVal}'")
				}
				lineOk = false
			}
			String expShort = expLine.length() > 120 ? expLine.substring(0, 120) + "..." : expLine
			String actShort = actLine.length() > 120 ? actLine.substring(0, 120) + "..." : actLine
			ValidationReport.addValidationReportItem(sectionName, "Line " + (i + 1), expShort, actShort, lineOk ? "Pass" : "Fail")
		}
		if (!errors.isEmpty()) {
			KeywordUtil.logInfo("--- TSV comparison output for ${actualPath} (${errors.size()} issue(s)) ---")
			for (String err in errors) {
				KeywordUtil.logInfo(err)
			}
			KeywordUtil.logInfo("--- end comparison output ---")
			KeywordUtil.markFailed("TSV comparison failed with ${errors.size()} mismatch(es). See log output above for full details.")
		} else {
			KeywordUtil.logInfo("TSV full content match: ${actualPath}")
		}
	}

	/**
	 * For a given TC (e.g. TC01): for each node with an expected file, select node, click Download,
	 * then compare expected vs downloaded TSV (headers + full content). Chrome is configured to
	 * download to project Downloads folder, so no move is needed.
	 * @param tcName Test case name used to find InputFiles/CRDC/MetadataData/&lt;tcName&gt;/expected/
	 */
	@Keyword
	void downloadAndCompareExpectedForTC(String tcName) {
		WebDriver driver = DriverFactory.getWebDriver()
		File projectDownloads = getProjectDownloadsDir()
		List<List<?>> expectedList = listExpectedFilesForTC(tcName)
		if (expectedList.isEmpty()) return
			for (List<?> pair in expectedList) {
				String node = (String) pair.get(0)
				String expectedPath = (String) pair.get(1)
				// Select node, click Download; file lands in project Downloads (Chrome prefs)
				WebUI.waitForElementClickable(findTestObject("CRDC/DataSubmissions/Validation/NodeType-Ddn"), 30)
				WebUI.click(findTestObject("CRDC/DataSubmissions/Validation/NodeType-Ddn"))
				WebUI.delay(1)
				WebElement option = driver.findElement(By.xpath("//li[normalize-space(text())='" + node + "']"))
				option.click()
				WebUI.delay(1)
				WebUI.waitForElementClickable(findTestObject("CRDC/DataSubmissions/Validation/Download-Btn"), 10)
				WebUI.click(findTestObject("CRDC/DataSubmissions/Validation/Download-Btn"))
				WebUI.delay(5)
				File latest = findLatestFile(projectDownloads)
				if (latest == null) {
					KeywordUtil.markFailed("downloadAndCompareExpectedForTC: no file found in project Downloads (${projectDownloads.absolutePath}) after downloading node ${node}")
					return
				}
				compareTsvFullContent(expectedPath, latest.absolutePath)
			}
	}

	/**
	 * Normalizes a submission name the same way the UI does for the export filename:
	 * trim, replace spaces with hyphen, then keep only alphanumeric and hyphen (matches filterAlphaNumeric(..., "-")).
	 */
	private static String normalizeSubmissionNameForExport(String name) {
		if (name == null) return ""
		return name.trim().replaceAll(" ", "-").replaceAll("[^a-zA-Z0-9-]", "")
	}

	/**
	 * Clicks the Validation Results export button (export-validation-button) and verifies that the
	 * downloaded file name follows the expected pattern: {submissionName}-{YYYY-MM-DDThhmmss}.csv
	 * (e.g. "auto-test20260309142645-2026-03-09T151725.csv"). File is expected in project Downloads folder.
	 * Assumes caller is on the Data Submission detail page with Validation Results tab in aggregated view.
	 *
	 * @param expectedSubmissionName The expected data submission name (first part of filename before the hyphen and timestamp).
	 *        If null or empty, only the timestamp pattern (YYYY-MM-DDThhmmss) is verified.
	 */
	@Keyword
	void clickValidationResultsExportAndVerifyDownloadedFileName(String expectedSubmissionName) {
		Utils.clearProjectDownloadsFolder()
		TestObject exportBtn = findTestObject("CRDC/DataSubmissions/Validation/Download-Btn-ValidationResults")
		if (!WebUIUtils.waitForElementClickableWithStaleRetry(exportBtn, 60)) {
			return
		}
		WebUI.click(exportBtn)
		WebUI.delay(8)
		File projectDownloads = getProjectDownloadsDir()
		File latest = findLatestFile(projectDownloads)
		if (latest == null) {
			KeywordUtil.markFailed("clickValidationResultsExportAndVerifyDownloadedFileName: no file found in project Downloads (${projectDownloads.absolutePath}) after clicking export")
			return
		}
		String fileName = latest.name
		// Pattern: {submissionName}-{YYYY-MM-DDThhmmss}.csv or without extension
		def matcher = (fileName =~ /^(.+)-(\d{4}-\d{2}-\d{2}T\d{6})(\.csv)?$/)
		if (!matcher.find()) {
			ValidationReport.addValidationReportItem("Validation Results Export", "Downloaded file name", "pattern: {submissionName}-YYYY-MM-DDThhmmss.csv", fileName, "Fail")
			KeywordUtil.markFailed("clickValidationResultsExportAndVerifyDownloadedFileName: downloaded file name does not match pattern. Got: " + fileName)
			return
		}
		String namePart = matcher.group(1)
		String timestampPart = matcher.group(2)
		if (expectedSubmissionName != null && !expectedSubmissionName.isEmpty()) {
			String normalizedExpected = normalizeSubmissionNameForExport(expectedSubmissionName)
			// Prefix match: UI label may be truncated while export uses full name; verify name part starts with expected
			if (!namePart.startsWith(normalizedExpected)) {
				ValidationReport.addValidationReportItem("Validation Results Export", "Submission name in filename", "starts with " + normalizedExpected, namePart, "Fail")
				KeywordUtil.markFailed("clickValidationResultsExportAndVerifyDownloadedFileName: filename submission name '" + namePart + "' does not start with expected '" + normalizedExpected + "'")
				return
			}
		}
		String expectedLabel = (expectedSubmissionName != null && !expectedSubmissionName.isEmpty())
				? "pattern match, submission: ${namePart}"
				: "pattern match (submission + timestamp)"
		ValidationReport.addValidationReportItem("Validation Results Export", "Downloaded file name", expectedLabel, fileName, "Pass")
		KeywordUtil.logInfo("clickValidationResultsExportAndVerifyDownloadedFileName: verified downloaded file: ${fileName} (submission name: ${namePart})")
	}

	// ------------------------------- Validation Results vs expected TSV -------------------------------

	/**
	 * Extracts the node name from an expected Validation Results TSV filename.
	 * Example: "GC_consent_group_v9.0.0_expected-ValidationResults.tsv" -> "consent_group".
	 * Strips the "_expected-ValidationResults.tsv" suffix, then the version (_vX.Y.Z), then returns the part after the first underscore (prefix like "GC_" is dropped).
	 * Returns empty string if filename does not end with "_expected-ValidationResults.tsv".
	 */
	private static String nodeFromExpectedValidationResultsFilename(String filename) {
		if (!filename.endsWith("_expected-ValidationResults.tsv")) return ""
		String base = filename - ~/_expected-ValidationResults\.tsv$/
		base = base.replaceFirst(/_v\d+\.\d+\.\d+$/, "")
		return base.contains("_") ? base.substring(base.indexOf("_") + 1) : base
	}

	/** Load node -> submitted-identifier column name from Data File (Data Files/CRDC/DataSubmissions/ValidationResults-Mappings). Falls back to Excel if Data File not available. Used by DatabaseUtils.verifyRecordsInMongo. */
	public static Map<String, String> loadValidationResultsMappings() {
		Map<String, String> nodeToColumn = new HashMap<>()
		try {			
			String testCaseId = RunConfiguration.getExecutionSourceId()
			String sheetName = "GC" // default

			if (testCaseId != null) {
				String idUpper = testCaseId.toUpperCase()
				if (idUpper.contains("GC")) {
					sheetName = "GC"
				} else if (idUpper.contains("CTDC")) {
					sheetName = "CTDC"
				} else if (idUpper.contains("ICDC")) {
					sheetName = "ICDC"
				}else if (idUpper.contains("PSDC")) {
					sheetName = "PSDC"
				}
			}

			KeywordUtil.logInfo("Loading ValidationResults-Mappings from Excel fallback. Sheet: " + sheetName)
			KeywordUtil.logInfo("Test Case ID: " + testCaseId)

			// Fallback to Excel if Data File not available
			String path = RunConfiguration.getProjectDir() + "/InputFiles/CRDC/DataSubmissionValidation/DSV_ValidationResults-mappings.xlsx"
			File f = new File(path)
			if (f.exists()) {
				Workbook wb = WorkbookFactory.create(f)
				Sheet sh = wb.getSheet(sheetName)
				if (sh != null) {
					for (Row r : sh) {
						if (r.getCell(0) != null && r.getCell(1) != null) {
							String nodeVal = r.getCell(0).toString().trim()
							String colVal = r.getCell(1).toString().trim()
							if (!nodeVal.isEmpty() && !colVal.isEmpty()) nodeToColumn.put(nodeVal, colVal)
						}
					}
				}else {
					KeywordUtil.markWarning("Sheet not found: " + sheetName)
				}
				wb.close()
			
			}
		} catch (Exception e) {
			KeywordUtil.markWarning("Not able to get mapping from DSV_ValidationResults-mappings.xlsx - " + e.message)
		}
		return nodeToColumn
	}

	/** Find expected TSV file path for the given node in InputFiles/CRDC/MetadataData/&lt;tcName&gt;/expected-ValidationResults/ */
	private String findExpectedValidationResultsTsv(String tcName, String node) {
		String baseDir = RunConfiguration.getProjectDir()
		File dir = new File(baseDir, "InputFiles/CRDC/MetadataData/${tcName}/expected-ValidationResults")
		if (!dir.exists() || !dir.isDirectory()) return null
		List<File> files = dir.listFiles()?.findAll { it.isFile() && it.name.endsWith("_expected-ValidationResults.tsv") } ?: []
		for (File f in files) {
			if (nodeFromExpectedValidationResultsFilename(f.name) == node) return f.absolutePath
		}
		return null
	}

	/** List unique node names from expected-ValidationResults folder for the given TC. Returns empty list if folder missing or no files. */
	private List<String> listNodesFromExpectedValidationResultsFolder(String tcName) {
		String baseDir = RunConfiguration.getProjectDir()
		File dir = new File(baseDir, "InputFiles/CRDC/MetadataData/${tcName}/expected-ValidationResults")
		KeywordUtil.logInfo("verifyValidationResultsVsExpectedForTC: looking in " + dir.absolutePath)
		if (!dir.exists() || !dir.isDirectory()) {
			KeywordUtil.logInfo("verifyValidationResultsVsExpectedForTC: folder does not exist or is not a directory.")
			return Collections.emptyList()
		}
		List<File> files = dir.listFiles()?.findAll { it.isFile() && it.name.endsWith("_expected-ValidationResults.tsv") } ?: []
		if (files.isEmpty()) KeywordUtil.logInfo("verifyValidationResultsVsExpectedForTC: no *_expected-ValidationResults.tsv files found.")
		Set<String> nodes = new LinkedHashSet<>()
		for (File f in files) {
			String node = nodeFromExpectedValidationResultsFilename(f.name)
			if (!node.isEmpty()) nodes.add(node)
		}
		return new ArrayList<>(nodes)
	}

	/** Get unique (severity, issueType) pairs from an expected Validation Results TSV. Returns list of [severity, issueType]. Skips rows where severity or issue type is empty. */
	private List<List<String>> getUniqueSeverityIssueTypePairsFromExpectedTsv(String tsvPath) {
		List<List<String>> out = new ArrayList<>()
		Path path = Paths.get(tsvPath)
		if (!Files.exists(path)) return out
		List<String> lines = Files.readAllLines(path)
		if (lines.size() < 2) return out
		String[] headers = lines.get(0).split("\t", -1)
		int idxIssue = -1, idxSev = -1
		for (int i = 0; i < headers.length; i++) {
			String h = headers[i].trim()
			if (h.equals("Issue_Type")) idxIssue = i
			if (h.equals("Expected_Severity")) idxSev = i
		}
		if (idxIssue < 0 || idxSev < 0) return out
		Set<String> seen = new LinkedHashSet<>()
		for (int i = 1; i < lines.size(); i++) {
			String[] cells = lines.get(i).split("\t", -1)
			String rowIssue = idxIssue < cells.length ? cells[idxIssue].trim() : ""
			String rowSev = idxSev < cells.length ? cells[idxSev].trim() : ""
			if (rowSev.isEmpty() || rowIssue.isEmpty()) continue
				String key = rowSev + "\t" + rowIssue
			if (seen.add(key)) out.add(Arrays.asList(rowSev, rowIssue))
		}
		return out
	}

	/** Load expected rows from TSV filtered by severity and issue type. Returns list of [submittedId, issueType, severity]. */
	private List<List<String>> loadExpectedValidationResultsTsv(String tsvPath, String submittedIdentifierColumn, String severity, String issueType) {
		List<List<String>> out = new ArrayList<>()
		Path path = Paths.get(tsvPath)
		if (!Files.exists(path)) return out
		List<String> lines = Files.readAllLines(path)
		if (lines.size() < 2) return out
		String[] headers = lines.get(0).split("\t", -1)
		int idxId = -1, idxIssue = -1, idxSev = -1
		for (int i = 0; i < headers.length; i++) {
			String h = headers[i].trim()
			if (h.equalsIgnoreCase(submittedIdentifierColumn)) idxId = i
			if (h.equals("Issue_Type")) idxIssue = i
			if (h.equals("Expected_Severity")) idxSev = i
		}
		if (idxId < 0 || idxIssue < 0 || idxSev < 0) return out
		for (int i = 1; i < lines.size(); i++) {
			String[] cells = lines.get(i).split("\t", -1)
			String rowIssue = idxIssue < cells.length ? cells[idxIssue].trim() : ""
			String rowSev = idxSev < cells.length ? cells[idxSev].trim() : ""
			if (!rowIssue.equals(issueType) || !rowSev.equalsIgnoreCase(severity)) continue
				String id = idxId < cells.length ? cells[idxId].trim() : ""
			out.add(Arrays.asList(id, rowIssue, rowSev))
		}
		return out
	}

	/** Load unique non-empty submitted identifier values from a TSV column. Returns a list of strings. Used by DatabaseUtils.verifyRecordsInMongo. */
	public static List<String> loadSubmittedIdentifiersFromTsv(String tsvPath, String submittedIdentifierColumn) {
		Set<String> seen = new LinkedHashSet<>()
		Path path = Paths.get(tsvPath)
		if (!Files.exists(path)) return new ArrayList<>(seen)
		List<String> lines = Files.readAllLines(path)
		if (lines.size() < 2) return new ArrayList<>(seen)
		String[] headers = lines.get(0).split("\t", -1)
		int idxId = -1
		for (int i = 0; i < headers.length; i++) {
			if (headers[i].trim().equalsIgnoreCase(submittedIdentifierColumn)) {
				idxId = i
				break
			}
		}
		if (idxId < 0) return new ArrayList<>(seen)
		for (int i = 1; i < lines.size(); i++) {
			String[] cells = lines.get(i).split("\t", -1)
			String id = idxId < cells.length ? cells[idxId].trim() : ""
			if (!id.isEmpty()) seen.add(id)
		}
		return new ArrayList<>(seen)
	}

	/** If the cell's visible text is this many characters or less, we use it as-is and skip DOM lookups for full value (truncated-text-wrapper / span). */
	private static final int TRUNCATED_CHECK_THRESHOLD = 15

	/**
	 * Returns the text to use for a table cell at the given column index. Used when reading Validation Results (and similar) tables from the UI.
	 * The UI may show truncated values (e.g. "auto_CDS2016_DN..."); the full value is in a [data-testid='truncated-text-wrapper'] element's aria-label, or in a span's aria-label/text.
	 * For short values (length &lt;= TRUNCATED_CHECK_THRESHOLD), returns the cell's getText() to avoid extra findElements. For longer values, tries truncated-text-wrapper aria-label, then span aria-label/text, then cell text.
	 * Uses findElements (not findElement) so we never block on implicit wait when the element is absent.
	 * @param cells list of td WebElements for one row
	 * @param index column index (0-based)
	 * @param debug if true, log which path was used to the console
	 * @return trimmed string for the cell, or "" if index invalid or on exception
	 */
	private static String getCellText(List<WebElement> cells, int index, boolean debug) {
		if (index < 0 || index >= cells.size()) return ""
		try {
			WebElement cell = cells.get(index)
			String cellRawText = cell.getText().trim()
			// If value is short enough, no need to look for truncated wrapper or span—use table text as-is
			if (cellRawText != null && cellRawText.length() <= TRUNCATED_CHECK_THRESHOLD) {
				if (debug) KeywordUtil.logInfo("[getCellText] cell index=" + index + ", short value (" + cellRawText.length() + " chars), using as-is='" + cellRawText + "'")
				return cellRawText
			}
			List<WebElement> truncatedWrappers = cell.findElements(By.cssSelector("[data-testid='truncated-text-wrapper']"))
			if (debug) {
				KeywordUtil.logInfo("[getCellText] cell index=" + index + ", raw cell.getText()='" + cellRawText + "', truncated-text-wrapper count=" + truncatedWrappers.size())
			}
			if (truncatedWrappers != null && !truncatedWrappers.isEmpty()) {
				WebElement first = truncatedWrappers.get(0)
				String aria = first.getAttribute("aria-label")
				if (debug) KeywordUtil.logInfo("[getCellText] using truncated-text-wrapper aria-label='" + (aria != null ? aria : "null") + "'")
				if (aria != null && !aria.isEmpty()) return aria.trim()
			}
			// Use findElements (not findElement) so we never block for implicit wait when cell has no span
			List<WebElement> spans = cell.findElements(By.tagName("span"))
			if (spans != null && !spans.isEmpty()) {
				WebElement span = spans.get(0)
				String aria = span.getAttribute("aria-label")
				if (debug) KeywordUtil.logInfo("[getCellText] fallback: span aria-label='" + (aria != null ? aria : "null") + "', span.getText()='" + span.getText().trim() + "'")
				if (aria != null && !aria.isEmpty()) return aria.trim()
				return span.getText().trim()
			}
			if (debug) KeywordUtil.logInfo("[getCellText] fallback: no span, using cell text='" + cellRawText + "'")
			return cellRawText
		} catch (Exception e) {
			if (debug) KeywordUtil.logInfo("[getCellText] exception: " + e.getMessage())
			return ""
		}
	}

	private static String normalizeCrossValidationIssue(String s) {
		if (s == null) return ""
		return s.trim().replaceAll(/[.]+$/, "").toLowerCase()
	}

	/**
	 * Shared Validation Results filter application used by both table verification flows.
	 * Keeps one source of truth for node/severity/issueType interactions.
	 */
	private void applyValidationResultsFiltersInternal(String node, String severity, String issueType, boolean includeLogs) {
		if (includeLogs) {
			KeywordUtil.logInfo("--- applyValidationResultsFilters: node=" + node + ", severity=" + severity + ", issueType=" + issueType + " ---")
		}
		WebUI.delay(1)
		Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
		WebUI.waitForElementClickable(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_NodeType-Ddn"), 35)
		WebUI.click(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_NodeType-Ddn"))
		WebUI.delay(1)
		WebDriver driver = DriverFactory.getWebDriver()
		if(node.equals("datafile")) {
			node=node.replaceAll("datafile", "data file")
		}
		WebElement nodeOpt = driver.findElement(By.xpath("//li[normalize-space(text())='" + node + "']"))
		nodeOpt.click()
		WebUI.delay(1)
		WebUI.waitForElementClickable(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_Severity-Ddn"), 10)
		WebUI.click(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_Severity-Ddn"))
		WebUI.delay(1)
		WebElement sevOpt = driver.findElement(By.xpath("//li[normalize-space(text())='" + severity + "']"))
		sevOpt.click()
		WebUI.delay(1)
		WebUI.waitForElementClickable(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_IssueType-Ddn"), 10)
		Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
		WebUI.click(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_IssueType-Ddn"))
		WebUI.delay(1)
		WebElement issueOpt = driver.findElement(By.xpath("//li[normalize-space(text())='" + issueType + "']"))
		issueOpt.click()
		WebUI.delay(2)
		Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
		WebUI.delay(1)
		if (includeLogs) {
			KeywordUtil.logInfo("applyValidationResultsFilters: completed")
		}
	}

	/**
	 * Apply Validation Results filters (node, severity, issue type) and read table rows: [submittedId, issueType, severity].
	 * Waits for Loading to disappear, stabilizes table row count, then re-queries rows by index and retries on StaleElementReferenceException to avoid flakiness.
	 */
	private List<List<String>> readValidationResultsTableFromUI(String node, String severity, String issueType) {
		applyValidationResultsFiltersInternal(node, severity, issueType, false)
		// Stabilize table: after changing node/severity/issueType the UI re-renders the table. If we read rows
		// before the DOM has settled, we can get StaleElementReferenceException when using row/cell references.
		// Wait until row count is unchanged across two samples 0.5s apart (up to 5 tries).
		int rowCount = 0
		for (int w = 0; w < 5; w++) {
			List<WebElement> waitRows = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Row")
			int n = waitRows != null ? waitRows.size() : 0
			WebUI.delay(0.5)
			waitRows = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Row")
			int n2 = waitRows != null ? waitRows.size() : 0
			if (n == n2) {
				rowCount = n
				break
			}
		}
		List<WebElement> headerEls = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Headers")
		List<String> headerNames = new ArrayList<>()
		for (WebElement h : headerEls) headerNames.add(h.getText().trim())
		int idxSubmitted = headerNames.findIndexOf { it.equalsIgnoreCase("Submitted Identifier") }
		int idxIssue = headerNames.findIndexOf { it.equalsIgnoreCase("Issue Type") }
		if (idxIssue < 0) idxIssue = headerNames.findIndexOf { it.equalsIgnoreCase("Issue(s)") }
		if (idxIssue < 0) idxIssue = headerNames.findIndexOf { it.toLowerCase().contains("issue") }
		int idxSev = headerNames.findIndexOf { it.equalsIgnoreCase("Severity") }
		if (idxSubmitted < 0 || idxSev < 0) {
			KeywordUtil.markFailed("Validation Results table missing required columns (Submitted Identifier, Severity). Headers: " + headerNames)
			return Collections.emptyList()
		}
		if (idxIssue < 0) idxIssue = -1
		List<WebElement> rows = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Row")
		// If stabilization loop never saw a stable count (e.g. table kept changing), use current row count
		if (rowCount <= 0 && rows != null) rowCount = rows.size()
		List<List<String>> out = new ArrayList<>()
		// Iterate by index and re-fetch the row list each time. Holding a single list of WebElement rows and
		// iterating over it can cause stale refs if the table re-renders (e.g. virtualization or delayed update).
		// Re-querying rows for each index gives us a fresh reference for that row.
		for (int i = 0; i < rowCount; i++) {
			boolean done = false
			// Retry up to 3 times on StaleElementReferenceException: the DOM may update while we read this row.
			for (int attempt = 1; attempt <= 3 && !done; attempt++) {
				try {
					List<WebElement> currentRows = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Row")
					if (i >= (currentRows != null ? currentRows.size() : 0)) break
						WebElement row = currentRows.get(i)
					List<WebElement> cells = row.findElements(By.tagName("td"))
					String submitted = getCellText(cells, idxSubmitted, true)
					String issue = getCellText(cells, idxIssue, false)
					String sev = getCellText(cells, idxSev, false)
					out.add(Arrays.asList(submitted, issue, sev))
					done = true
				} catch (StaleElementReferenceException e) {
					if (attempt == 3) throw e
					WebUI.delay(0.5)
				}
			}
		}
		return out
	}

	/**
	 * Compare Validation Results UI (filtered by node, severity, issue type) to expected TSV for that node.
	 * Expected TSV: InputFiles/CRDC/MetadataData/&lt;tcName&gt;/expected-ValidationResults/GC_*_node_*_expected-ValidationResults.tsv.
	 * Matching by Submitted Identifier (column per node from Data Files/CRDC/DataSubmissions/ValidationResults-Mappings).
	 * @param tcName Test case name for expected-ValidationResults folder
	 * @param node Node name (e.g. consent_group)
	 * @param severity Expected severity filter (e.g. Error, Warning)
	 * @param issueType Expected issue type filter
	 */
	@Keyword
	void verifyValidationResultsVsExpected(String tcName, String node, String severity, String issueType) {
		KeywordUtil.logInfo("--- Verifying Validation Results for node: " + node + ", severity: " + severity + ", issueType: " + issueType + " ---")
		Map<String, String> mappings = loadValidationResultsMappings()
		String submittedIdColumn = mappings.get(node)
		if (submittedIdColumn == null || submittedIdColumn.isEmpty()) {
			KeywordUtil.markFailed("verifyValidationResultsVsExpected: no mapping for node '" + node + "' in ValidationResults-Mappings")
			return
		}
		String tsvPath = findExpectedValidationResultsTsv(tcName, node)
		if (tsvPath == null) {
			KeywordUtil.markFailed("verifyValidationResultsVsExpected: no expected TSV for node '" + node + "' in expected-ValidationResults folder")
			return
		}
		List<List<String>> expectedRows = loadExpectedValidationResultsTsv(tsvPath, submittedIdColumn, severity, issueType)
		List<List<String>> actualRows = readValidationResultsTableFromUI(node, severity, issueType)
		Map<String, List<String>> actualBySubmittedId = new LinkedHashMap<>()
		for (List<String> row : actualRows) {
			if (row.size() >= 1 && !row.get(0).isEmpty()) actualBySubmittedId.put(row.get(0).trim(), row)
		}
		List<String> errors = new ArrayList<>()
		String sectionName = "Validation Results"
		for (List<String> exp : expectedRows) {
			String submittedId = exp.get(0)
			List<String> act = actualBySubmittedId.get(submittedId)
			if (act == null) {
				errors.add("Submitted Identifier '" + submittedId + "': expected in UI but not found")
				ValidationReport.addValidationReportItem(sectionName, "Submitted ID " + submittedId, "Issue=" + exp.get(1) + ", Severity=" + exp.get(2), "(not found in UI)", "Fail")
				continue
			}
			String expIssue = exp.get(1), expSev = exp.get(2)
			String actIssue = act.size() >= 2 ? act.get(1) : ""
			String actSev = act.size() >= 3 ? act.get(2) : ""
			boolean issueOk = actIssue.equals(expIssue)
			boolean sevOk = actSev.equalsIgnoreCase(expSev)
			if (!issueOk) errors.add("Submitted Identifier '" + submittedId + "': Issue Type expected '" + expIssue + "', actual '" + actIssue + "'")
			if (!sevOk) errors.add("Submitted Identifier '" + submittedId + "': Severity expected '" + expSev + "', actual '" + actSev + "'")
			String status = (issueOk && sevOk) ? "Pass" : "Fail"
			ValidationReport.addValidationReportItem(sectionName, "Submitted ID " + submittedId, "Issue=" + expIssue + ", Severity=" + expSev, "Issue=" + actIssue + ", Severity=" + actSev, status)
		}
		for (String submittedId : actualBySubmittedId.keySet()) {
			boolean found = expectedRows.any { it.get(0).equals(submittedId) }
			if (!found) {
				errors.add("Submitted Identifier '" + submittedId + "': present in UI but not in expected TSV")
				List<String> act = actualBySubmittedId.get(submittedId)
				String actIssue = act.size() >= 2 ? act.get(1) : ""
				String actSev = act.size() >= 3 ? act.get(2) : ""
				ValidationReport.addValidationReportItem(sectionName, "Submitted ID " + submittedId, "(not in expected)", "Issue=" + actIssue + ", Severity=" + actSev, "Fail")
			}
		}
		if (!errors.isEmpty()) {
			KeywordUtil.logInfo("--- Validation Results vs Expected (" + node + ", " + severity + ", " + issueType + ") - " + errors.size() + " issue(s) ---")
			for (String err : errors) KeywordUtil.logInfo(err)
			KeywordUtil.logInfo("--- end ---")
			KeywordUtil.markFailed("verifyValidationResultsVsExpected: " + errors.size() + " mismatch(es). See log above.")
		} else {
			KeywordUtil.logInfo("Validation Results match expected for node=" + node + ", severity=" + severity + ", issueType=" + issueType)
		}
	}

	/**
	 * One-call Validation Results verification for a test case: discovers all expected-ValidationResults TSV files
	 * under InputFiles/CRDC/MetadataData/&lt;tcName&gt;/expected-ValidationResults/, derives nodes and (severity, issueType)
	 * pairs from those files, and runs verifyValidationResultsVsExpected for each (node, severity, issueType).
	 * Use this in the test case instead of multiple 4-arg calls.
	 * @param tcName Test case name (e.g. TC99)
	 */
	@Keyword
	void verifyValidationResultsVsExpectedForTC(String tcName) {
		TestRunner.clickTab("CRDC/DataSubmissions/Validation/ValidationResults-Tab")
		TestRunner.clickTab("Object Repository/CRDC/DataSubmissions/Validation/ValidationResults_AggregatedExpanded-Btn")
		List<String> nodes = listNodesFromExpectedValidationResultsFolder(tcName)
		if (nodes.isEmpty()) {
			KeywordUtil.logInfo("verifyValidationResultsVsExpectedForTC: No expected Validation Results files for TC '" + tcName + "'; skipping.")
			return
		}
		for (String node : nodes) {
			String tsvPath = findExpectedValidationResultsTsv(tcName, node)
			if (tsvPath == null) {
				KeywordUtil.logInfo("verifyValidationResultsVsExpectedForTC: No expected TSV for node '" + node + "'; skipping.")
				continue
			}
			List<List<String>> pairs = getUniqueSeverityIssueTypePairsFromExpectedTsv(tsvPath)
			for (List<String> pair : pairs) {
				String severity = pair.get(0)
				String issueType = pair.get(1)
				verifyValidationResultsVsExpected(tcName, node, severity, issueType)
			}
		}
	}

	/**
	 * Apply Validation Results filters (node, severity, issue type). Call before verifyErrorDetailsForSubmittedId
	 * when the table must be filtered to a specific node/severity/issueType so the target row is visible.
	 * Assumes caller is on Validation Results tab (Expanded view).
	 *
	 * @param node Node type (e.g. participant)
	 * @param severity Severity filter (e.g. Error, Warning)
	 * @param issueType Issue type filter (e.g. Missing required property)
	 */
	@Keyword
	void applyValidationResultsFilters(String node, String severity, String issueType) {
		applyValidationResultsFiltersInternal(node, severity, issueType, true)
	}

	// ------------------------------- Validation Error Details (See details dialog) -------------------------------

	/**
	 * Click "See details" for the Validation Results row whose Submitted Identifier matches the given value.
	 * Assumes caller is on Validation Results tab with filters applied. Waits for the error details dialog to appear.
	 * Uses GenericTable row locator from AILocators.
	 *
	 * @param submittedIdentifier The Submitted Identifier value (case-insensitive, trimmed)
	 */
	@Keyword
	void clickSeeDetailsForSubmittedId(String submittedIdentifier) {
		KeywordUtil.logInfo("--- clickSeeDetailsForSubmittedId: " + submittedIdentifier + " ---")
		String target = submittedIdentifier?.trim() ?: ""

		List<WebElement> rows = Utils.getListWebElements("CRDC/AILocators/GenericTable/TableRow")
		if (rows == null || rows.isEmpty()) {
			KeywordUtil.markFailed("clickSeeDetailsForSubmittedId: no table rows found")
			return
		}

		// Get header indices (use same Table-Headers as readValidationResultsTableFromUI)
		List<WebElement> headerEls = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Headers")
		List<String> headerNames = new ArrayList<>()
		for (WebElement h : headerEls) headerNames.add(h.getText().trim())
		int idxSubmitted = headerNames.findIndexOf { it.equalsIgnoreCase("Submitted Identifier") }
		if (idxSubmitted < 0) {
			KeywordUtil.markFailed("clickSeeDetailsForSubmittedId: Submitted Identifier column not found. Headers: " + headerNames)
			return
		}

		WebElement matchingRow = null
		for (WebElement row : rows) {
			try {
				List<WebElement> cells = row.findElements(By.tagName("td"))
				if (idxSubmitted >= cells.size()) continue
					String cellText = getCellText(cells, idxSubmitted, true)
				if (cellText != null && cellText.trim().equalsIgnoreCase(target)) {
					matchingRow = row
					break
				}
			} catch (StaleElementReferenceException e) {
				// Retry by re-querying
				rows = Utils.getListWebElements("CRDC/AILocators/GenericTable/TableRow")
				if (rows == null || rows.isEmpty()) continue
					continue
			}
		}

		if (matchingRow == null) {
			KeywordUtil.markFailed("clickSeeDetailsForSubmittedId: no row with Submitted Identifier '" + submittedIdentifier + "'")
			return
		}

		// Row-scoped lookup is intentional: there can be many "See details" buttons on the page,
		// and we must click the one belonging to the matched Submitted Identifier row.
		WebElement seeDetailsBtn = matchingRow.findElement(By.xpath(".//button[contains(.,'See details')]"))
		seeDetailsBtn.click()
		WebUI.delay(1)
		WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/DataSubmissions/ErrorDetailsDialog/Dialog"), 10)
		KeywordUtil.logInfo("clickSeeDetailsForSubmittedId: opened error details dialog")
	}

	/**
	 * Read all issues from the open error details dialog. Returns a list of maps with keys severity and message.
	 * Assumes the error details dialog is open.
	 * Uses element lists (not WebUI.getText on one TestObject) to preserve row order and pair each
	 * severity with its corresponding message.
	 *
	 * @return List of maps [severity: String, message: String]
	 */
	@Keyword
	List<Map<String, String>> getErrorDetailsDialogIssues() {
		List<WebElement> severities = WebUI.findWebElements(
				findTestObject("CRDC/AILocators/DataSubmissions/ErrorDetailsDialog/IssueSeverity-Lbl"),
				10
				)
		List<WebElement> messages = WebUI.findWebElements(
				findTestObject("CRDC/AILocators/DataSubmissions/ErrorDetailsDialog/IssueMessage-Lbl"),
				10
				)

		List<Map<String, String>> issues = new ArrayList<>()
		int count = Math.min(severities.size(), messages.size())
		for (int i = 0; i < count; i++) {
			String sev = severities.get(i).getText()?.trim() ?: ""
			String msg = messages.get(i).getText()?.trim() ?: ""
			issues.add([severity: sev, message: msg])
		}
		KeywordUtil.logInfo("getErrorDetailsDialogIssues: found " + issues.size() + " issues")
		return issues
	}

	/**
	 * Verify the open error details dialog contains all expected issues. Relaxed check: each expected issue
	 * must appear in the dialog (at least one actual issue matches severity and message); actual may contain
	 * more issues (e.g. other files' errors/warnings grouped by submitted identifier). No strict count equality.
	 *
	 * @param expectedIssues List of maps [severity: String, message: String]. Message matching: actual contains expected.
	 * @param reportSection (Optional) Section for ValidationReport. Defaults to "Validation Error Details".
	 */
	@Keyword
	void verifyErrorDetailsDialogIssues(List<Map<String, String>> expectedIssues, String reportSection = "Validation Error Details") {
		KeywordUtil.logInfo("--- verifyErrorDetailsDialogIssues: expecting " + expectedIssues.size() + " issue(s) (all must appear in dialog) ---")
		List<Map<String, String>> actual = getErrorDetailsDialogIssues()
		boolean allOk = true

		for (int i = 0; i < expectedIssues.size(); i++) {
			Map<String, String> exp = expectedIssues.get(i)
			String expSev = (exp.get("severity") ?: "").trim()
			String expMsg = (exp.get("message") ?: "").trim()
			boolean found = false
			for (Map<String, String> actualIssue : actual) {
				String actSev = (actualIssue.get("severity") ?: "").trim()
				String actMsg = (actualIssue.get("message") ?: "").trim()
				if (actSev.equalsIgnoreCase(expSev) && actMsg.contains(expMsg)) {
					found = true
					break
				}
			}
			String status = found ? "Pass" : "Fail"
			ValidationReport.addValidationReportItem(reportSection, "Expected issue " + (i + 1) + " in dialog", expSev + ": " + (expMsg.length() > 50 ? expMsg.substring(0, 50) + "..." : expMsg), found ? "found" : "not found", status)
			if (!found) allOk = false
		}

		if (allOk) {
			KeywordUtil.logInfo("Error details dialog: all " + expectedIssues.size() + " expected issue(s) verified")
		} else {
			KeywordUtil.markFailed("verifyErrorDetailsDialogIssues: one or more expected issues did not appear in dialog")
		}
	}

	/**
	 * Close the error details dialog by clicking the Close button. Waits for the dialog to disappear.
	 */
	@Keyword
	void closeErrorDetailsDialog() {
		KeywordUtil.logInfo("--- closeErrorDetailsDialog ---")
		WebUI.click(findTestObject("CRDC/AILocators/DataSubmissions/ErrorDetailsDialog/Close-Btn"))
		WebUI.delay(1)
		try {
			Utils.waitForElementNotVisible("CRDC/AILocators/DataSubmissions/ErrorDetailsDialog/Dialog", 5)
		} catch (Exception e) {
			// Dialog may be removed from DOM; check if still present
			WebDriver driver = DriverFactory.getWebDriver()
			if (driver.findElements(By.cssSelector("[data-testid='error-details-dialog']")).isEmpty()) {
				KeywordUtil.logInfo("closeErrorDetailsDialog: dialog closed")
			} else {
				KeywordUtil.logInfo("closeErrorDetailsDialog: dialog may still be visible: " + e.getMessage())
			}
		}
	}

	/**
	 * Convenience: click See details for the row, verify issues, then close the dialog.
	 * Caller must ensure Validation Results tab is active and filters (node, severity, issue type) are applied first.
	 * For expected data from files, use verifyErrorDetailsForSubmittedIdForTC(tcName) instead.
	 *
	 * @param submittedIdentifier The Submitted Identifier of the row
	 * @param expectedIssues List of maps [severity: String, message: String]
	 * @param reportSection (Optional) Section for ValidationReport. Defaults to "Validation Error Details".
	 */
	@Keyword
	void verifyErrorDetailsForSubmittedId(String submittedIdentifier, List<Map<String, String>> expectedIssues, String reportSection = "Validation Error Details") {
		KeywordUtil.logInfo("--- verifyErrorDetailsForSubmittedId: " + submittedIdentifier + " ---")
		clickSeeDetailsForSubmittedId(submittedIdentifier)
		verifyErrorDetailsDialogIssues(expectedIssues, reportSection)
		closeErrorDetailsDialog()
		KeywordUtil.logInfo("verifyErrorDetailsForSubmittedId: completed")
	}

	/**
	 * One-call verification: discovers all nodes and rows with expected issues from the TC's expected-ValidationResults TSVs,
	 * applies filters for each, opens See details, verifies dialog content, and closes. Mirrors verifyValidationResultsVsExpectedForTC.
	 *
	 * Usage: CustomKeywords.'utilities.DataSubmissions.verifyErrorDetailsForSubmittedIdForTC'('TC04')
	 *
	 * @param tcName Test case name (e.g. TC04)
	 */
	@Keyword
	void verifyErrorDetailsForSubmittedIdForTC(String tcName) {
		KeywordUtil.logInfo("--- verifyErrorDetailsForSubmittedIdForTC: tcName=" + tcName + " ---")
		TestRunner.clickTab("CRDC/DataSubmissions/Validation/ValidationResults-Tab")
		TestRunner.clickTab("Object Repository/CRDC/DataSubmissions/Validation/ValidationResults_AggregatedExpanded-Btn")
		WebUI.delay(2)
		List<String> nodes = listNodesFromExpectedValidationResultsFolder(tcName)
		if (nodes.isEmpty()) {
			KeywordUtil.logInfo("verifyErrorDetailsForSubmittedIdForTC: No expected Validation Results files for TC '" + tcName + "'; skipping.")
			return
		}
		Map<String, String> mappings = loadValidationResultsMappings()
		for (String node : nodes) {
			String submittedIdColumn = mappings.get(node)
			if (submittedIdColumn == null || submittedIdColumn.isEmpty()) {
				KeywordUtil.logInfo("verifyErrorDetailsForSubmittedIdForTC: no mapping for node '" + node + "'; skipping.")
				continue
			}
			String tsvPath = findExpectedValidationResultsTsv(tcName, node)
			if (tsvPath == null) {
				KeywordUtil.logInfo("verifyErrorDetailsForSubmittedIdForTC: no expected TSV for node '" + node + "'; skipping.")
				continue
			}
			List<List<String>> rows = loadAllExpectedErrorDetailRowsFromTsv(tsvPath, submittedIdColumn)
			if (rows.isEmpty()) {
				KeywordUtil.logInfo("verifyErrorDetailsForSubmittedIdForTC: no rows with issues for node '" + node + "'; skipping.")
				continue
			}
			for (List<String> row : rows) {
				String submittedId = row.get(0)
				String severity = row.get(1)
				String issueType = row.get(2)
				String message = row.get(3)
				applyValidationResultsFilters(node, severity, issueType)
				verifyErrorDetailsForSubmittedId(submittedId, [
					[severity: severity, message: message]
				])
			}
		}
		TestRunner.clickTab("Object Repository/CRDC/DataSubmissions/Validation/ValidationResults_AggregatedExpanded-Btn")
		KeywordUtil.logInfo("verifyErrorDetailsForSubmittedIdForTC: completed")
	}

	/**
	 * Remove Excel/CSV-style quoting from a cell value. When TSV is edited in Excel,
	 * cells with double quotes get wrapped in "..." and internal " become "".
	 * This reverses that so the value matches the actual API/UI message format.
	 */
	private static String unquoteCsvStyle(String s) {
		if (s == null) return ""
		String t = s.trim()
		if (t.length() >= 2 && t.startsWith('"') && t.endsWith('"')) {
			t = t.substring(1, t.length() - 1).replace('""', '"')
		}
		return t
	}

	/** Load all rows with issues from expected TSV. Returns list of [submittedId, severity, issueType, message]. Message = Expected_Issues or Issue_Type. */
	private List<List<String>> loadAllExpectedErrorDetailRowsFromTsv(String tsvPath, String submittedIdColumn) {
		List<List<String>> out = new ArrayList<>()
		Path path = Paths.get(tsvPath)
		if (!Files.exists(path)) return out
		List<String> lines = Files.readAllLines(path)
		if (lines.size() < 2) return out
		String[] headers = lines.get(0).split("\t", -1)
		int idxId = -1, idxIssue = -1, idxSev = -1, idxExpectedIssues = -1
		for (int i = 0; i < headers.length; i++) {
			String h = headers[i].trim()
			if (h.equalsIgnoreCase(submittedIdColumn)) idxId = i
			if (h.equals("Issue_Type")) idxIssue = i
			if (h.equals("Expected_Severity")) idxSev = i
			if (h.equals("Expected_Issues")) idxExpectedIssues = i
		}
		if (idxId < 0 || idxIssue < 0 || idxSev < 0) return out
		if (idxExpectedIssues < 0) idxExpectedIssues = idxIssue
		for (int i = 1; i < lines.size(); i++) {
			String[] cells = lines.get(i).split("\t", -1)
			String rowIssue = idxIssue < cells.length ? cells[idxIssue].trim() : ""
			String rowSev = idxSev < cells.length ? cells[idxSev].trim() : ""
			if (rowSev.isEmpty() || rowIssue.isEmpty()) continue
				String submittedId = idxId < cells.length ? cells[idxId].trim() : ""
			String message = idxExpectedIssues >= 0 && idxExpectedIssues < cells.length ? unquoteCsvStyle(cells[idxExpectedIssues]) : ""
			if (message.isEmpty()) message = rowIssue
			out.add(Arrays.asList(submittedId, rowSev, rowIssue, message))
		}
		return out
	}

	// ------------------------------- From Pbac: upload + verify Data View for TC -------------------------------

	/**
	 * Extract node name from a TSV filename.
	 * Supports patterns like GC_consent_group_v9.0.0.tsv, ICDC_participant_v10.0.0.tsv (any prefix).
	 * Node is the part between the first underscore and _vX.Y.Z.
	 */
	private static String nodeFromFilename(String filename) {
		if (!filename.endsWith('.tsv')) return filename.replaceFirst(~/\.[^.]+$/, '')
		String base = filename - ~/\.tsv$/
		base = base.replaceFirst(/_v\d+\.\d+\.\d+$/, '')
		return base.contains('_') ? base.substring(base.indexOf('_') + 1) : base
	}

	/**
	 * List TSV files in InputFiles/CRDC/MetadataData/&lt;tcName&gt;/ and return [relativePaths, nodes].
	 * Empty lists if folder missing or no TSV files. Used by uploadFolder, verifyDataViewMatchesMetadataForTC, and DatabaseUtils.verifyRecordsInMongo.
	 */
	public static List<List<String>> listTsvFilesAndNodesForTC(String tcName) {
		String baseDir = RunConfiguration.getProjectDir()
		String folderPath = "InputFiles/CRDC/MetadataData/${tcName}"
		File folder = new File(baseDir, folderPath)
		List<String> relativePaths = new ArrayList<>()
		List<String> nodes = new ArrayList<>()
		if (!folder.exists() || !folder.isDirectory()) {
			KeywordUtil.markFailed("uploadFolder: folder not found or not a directory: ${folder.absolutePath}")
			return [relativePaths, nodes]
		}
		List<File> fileList = folder.listFiles()?.findAll { it.isFile() && it.name.endsWith('.tsv') } ?: []
		if (fileList.isEmpty()) {
			KeywordUtil.markFailed("uploadFolder: no .tsv files in ${folder.absolutePath}")
			return [relativePaths, nodes]
		}
		for (File f in fileList) {
			String node = nodeFromFilename(f.name)
			relativePaths.add("${folderPath}/${f.name}")
			nodes.add(node)
		}
		return [relativePaths, nodes]
	}

	/**
	 * Upload all TSV files from InputFiles/CRDC/MetadataData/&lt;tcName&gt;/ (choose files, click Upload, wait for Uploading to disappear).
	 * Does not open Data View or verify. Call verifyDataViewMatchesMetadataForTC(tcName) after this if you want to verify.
	 * @param tcName Test case name (e.g. TC02)
	 */
	@Keyword
	void uploadFolder(String tcName) {
		List<List<String>> listResult = listTsvFilesAndNodesForTC(tcName)
		List<String> relativePaths = listResult.get(0)
		List<String> nodes = listResult.get(1)
		if (relativePaths.isEmpty()) return
			String baseDir = RunConfiguration.getProjectDir()
		String folderPath = "InputFiles/CRDC/MetadataData/${tcName}"
		List<String> absolutePaths = new ArrayList<>()
		for (int i = 0; i < relativePaths.size(); i++) {
			absolutePaths.add(new File(baseDir, relativePaths.get(i)).absolutePath)
		}
		// Upload entire folder in one go (multiple paths separated by newline)
		String allPaths = absolutePaths.join('\n')
		WebUI.delay(2)
		WebUI.uploadFile(findTestObject('CRDC/DataSubmissions/ChooseFiles-Btn'), allPaths)
		WebUI.delay(5)
		TestRunner.clickTab('CRDC/DataSubmissions/Upload-Btn')
		WebUI.delay(10)
		Utils.waitForElementToDisappear('CRDC/DataSubmissions/Validation/Uploading-Text', 15)
	}

	/**
	 * Verify Data View tab matches metadata for each node in InputFiles/CRDC/MetadataData/&lt;tcName&gt;/.
	 * Assumes data has already been uploaded (e.g. via uploadFolder(tcName)). Does not perform upload.
	 * @param tcName Test case name (e.g. TC02)
	 */
	@Keyword
	void verifyDataViewMatchesMetadataForTC(String tcName) {
		List<List<String>> listResult = listTsvFilesAndNodesForTC(tcName)
		List<String> relativePaths = listResult.get(0)
		List<String> nodes = listResult.get(1)
		if (relativePaths.isEmpty()) return
			TestRunner.clickTab('CRDC/DataSubmissions/Validation/DataView-Tab')
		for (int i = 0; i < relativePaths.size(); i++) {
			verifyDataViewMatchesMetadata(relativePaths.get(i), nodes.get(i))
		}
	}

	/**
	 * Click on Validate button to validate data uploaded
	 */
	@Keyword
	private static clickValidateButton() {
		WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validate-Btn'), 30)
		TestRunner.clickTab('CRDC/DataSubmissions/Validate-Btn')
		//WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validate-Btn'), 30)
		KeywordUtil.logInfo("Waiting for Validation to finish - waiting up to 5 minutes")
		WebUI.waitForElementNotHasAttribute(findTestObject('CRDC/DataSubmissions/Validate-Btn'), 'disabled', 300)
	}

	/**
	 * Click the Cross Validate button and wait for cross-validation to complete.
	 * Cross-validation checks for duplicate nodes (S001 errors) across submissions
	 * within the same study. Only DCPs can trigger this on submitted/released submissions.
	 * After this returns, the Cross Validation Results tab will contain the S001 results.
	 */
	@Keyword
	void clickCrossValidateButton() {
		WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/DataSubmissions/Detail/ValidationControls/CrossValidate-Btn'), 30)
		WebUIUtils.clickTab('CRDC/AILocators/DataSubmissions/Detail/ValidationControls/CrossValidate-Btn')
		KeywordUtil.logInfo("Waiting for cross-validation to finish - waiting up to 5 minutes")
		WebUI.waitForElementNotHasAttribute(findTestObject('CRDC/AILocators/DataSubmissions/Detail/ValidationControls/CrossValidate-Btn'), 'disabled', 300)
	}

	private static final String VALIDATION_RESULTS_UI_SECTION = "UI / Validation Results"

	/**
	 * Verification only: caller must already have navigated to Data Submission → Validation Results → Expanded view
	 * with default All filters. Waits for CRDC/AILocators/GenericTable/LoadingRow to disappear so row counts
	 * are not read mid-load. Counts data rows via Utils.getListWebElements and Object Repository
	 * CRDC/AILocators/GenericTable/TableRow (data-testid='generic-table-row'); optional empty copy via
	 * CRDC/DataSubmissions/Validation/ValidationResults-DefaultMesssage. Records one ValidationReport row.
	 */
	@Keyword
	void verifyNoValidationErrors() {
		Utils.waitForElementToDisappear('CRDC/AILocators/GenericTable/LoadingRow', 15)
		List qcRows = Utils.getListWebElements('CRDC/AILocators/GenericTable/TableRow')
		int qcDataRows = qcRows != null ? qcRows.size() : 0
		List emptyMsgEls = Utils.getListWebElements('CRDC/DataSubmissions/Validation/ValidationResults-DefaultMesssage')
		boolean hasEmptyStateCopy = emptyMsgEls != null && !emptyMsgEls.isEmpty()
		ValidationReport.addValidationReportItem(VALIDATION_RESULTS_UI_SECTION, 'Validation Results data rows (generic-table-row)',
			"0", String.valueOf(qcDataRows), qcDataRows == 0 ? "Pass" : "Fail")
		if (qcDataRows > 0) {
			KeywordUtil.markFailed("verifyNoValidationErrors: expected 0 Validation Results data rows (generic-table-row), found ${qcDataRows}")
		} else {
			KeywordUtil.logInfo("verifyNoValidationErrors: empty-state message (ValidationResults-DefaultMesssage): ${hasEmptyStateCopy}")
			if (!hasEmptyStateCopy) {
				KeywordUtil.logInfo("[WARNING] verifyNoValidationErrors: empty-state Test Object not found; 0 data rows may still be valid.")
			}
		}
	}

	/**
	 * Creates a Data Submission in the CRDC application based on the
	 * specified submission type, data type, data model, and study.
	 *
	 * @param submiType  Type of submission (e.g., "New", "Update", "Delete")
	 * @param dataType   Type of data being submitted (e.g., "Metadata Only", "Metadata and Data files")
	 * @param dataModel  Target data model for the submission (GC, ICDC, CTDC)
	 * @param study      Study identifier associated with the submission
	 * 
	 * Usage Example:
	 * createDataSubmission("New", "Metadata Only", "GC", "phs002504")
	 */
	@Keyword
	public static void createDataSubmission(String submiType, String dataType, String dataModel, String study) {

		TestRunner.clickTab('CRDC/DataSubmissions/Create/CreateADataSubmission-Btn')

		if (submiType.equalsIgnoreCase("Delete")) {
			WebUI.click(findTestObject('CRDC/DataSubmissions/Create/SubTypeDelete-Btn'))
			expectedSubmissionType = "Delete"
		}else {
			expectedSubmissionType ="New/Update";
		}


		if(dataType?.toLowerCase()?.contains("only")) {
			WebUI.click(findTestObject('CRDC/DataSubmissions/Create/MetadataOnly-Btn'))
		}

		WebUI.delay(5)

		//Select the Data Commons
		WebDriver driver = DriverFactory.getWebDriver()
		WebUI.click(findTestObject('Object Repository/CRDC/DataSubmissions/Create/DataCommons-Ddn'))
		WebUI.delay(2)
		List<WebElement> commonsDropdownOptions = WebUI.findWebElements(findTestObject('Object Repository/CRDC/DataSubmissions/Create/DataCommonsDdn-Option'), 10)
		WebElement matchDataCommons = commonsDropdownOptions.find {it.getAttribute("innerText")?.trim() == dataModel}
		if (matchDataCommons != null) {
			try {
				new Actions(driver).moveToElement(matchDataCommons).click().perform()
			} catch (Exception e) {
				KeywordUtil.logInfo("Click blocked, retrying after overlay removal...")
				WebUI.executeJavaScript("""document.querySelectorAll('.MuiBackdrop-root, .MuiDialog-container').forEach(e => e.remove())""", null)
				WebUI.delay(1)
				new Actions(driver).moveToElement(matchDataCommons).click().perform()
			}
		} else {
			KeywordUtil.markFailed("Study option "+dataModel+" not found in dropdown.")
		}

		//Select the study (scoped MUI menu xpath, normalized match, re-find before click — same pattern as Pbac.createDataSubmission)
		WebUI.click(findTestObject('CRDC/DataSubmissions/Create/Study-Ddn'))

		String normalizedTarget = study.replaceAll(/\s+/, ' ').trim()
		String studyOptionXpath = "//div[contains(@class,'MuiMenu-paper')]//li[contains(@data-testid,'study-option')]"

		WebDriverWait menuWait = new WebDriverWait(driver, 10)
		menuWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(studyOptionXpath)))

		List<WebElement> studyDropdownOptions = driver.findElements(By.xpath(studyOptionXpath))
		WebElement matchStudy = studyDropdownOptions.find {
			String t = it.getAttribute("innerText")
			(t != null ? t.replaceAll(/\s+/, ' ').trim() : "") == normalizedTarget
		}
		if (matchStudy == null) {
			KeywordUtil.markFailed("Study option '${study}' not found in dropdown.")
			return
		}
		KeywordUtil.logInfo("Found the study to create DS: '${study}'")

		studyDropdownOptions = driver.findElements(By.xpath(studyOptionXpath))
		matchStudy = studyDropdownOptions.find {
			String t = it.getAttribute("innerText")
			(t != null ? t.replaceAll(/\s+/, ' ').trim() : "") == normalizedTarget
		}
		if (matchStudy == null) {
			KeywordUtil.markFailed("Study option '${study}' disappeared before click.")
			return
		}

		new Actions(driver).moveToElement(matchStudy).perform()
		WebUI.delay(1)

		try {
			matchStudy.click()
		} catch (Exception e) {
			KeywordUtil.logInfo("Native study option click failed: ${e.message}. Retrying with Actions move + click...")
			WebUI.delay(1)
			studyDropdownOptions = driver.findElements(By.xpath(studyOptionXpath))
			matchStudy = studyDropdownOptions.find {
				String t = it.getAttribute("innerText")
				(t != null ? t.replaceAll(/\s+/, ' ').trim() : "") == normalizedTarget
			}
			if (matchStudy != null) {
				new Actions(driver).moveToElement(matchStudy).perform()
				WebUI.delay(0.2)
				new Actions(driver).moveToElement(matchStudy).click().perform()
			}
		}

		//Enter the submission name -- setText() is not working, need to use Actions class
		String timestamp = new Date().format("yyyyMMdd_HHmmss")
		String submissionName = "auto-test_" + timestamp
		KeywordUtil.logInfo("Creating data submission with name " + submissionName)
		expectedSubmissionName = submissionName
		expectedDataCommons = dataModel

		WebUI.click(findTestObject('CRDC/DataSubmissions/Create/SubmissionName-Field'))
		Actions actions = new Actions(DriverFactory.getWebDriver())
		WebElement el = WebUI.findWebElement(findTestObject('CRDC/DataSubmissions/Create/SubmissionName-Field'), 10)
		actions.moveToElement(el).click().sendKeys(submissionName).perform()

		//Create
		TestRunner.clickTab('CRDC/DataSubmissions/Create/Create-Btn')
		WebUI.delay(3)
		WebUI.sendKeys(findTestObject('CRDC/AILocators/DataSubmissions/List/Filters/SubmissionName-TxtBx'), submissionName)
		WebUI.delay(1)
	}

	private static final String HEADERS_SECTION = "Data Submission Headers"
	private static final String SUBMISSION_HISTORY_SECTION = "Submission History"

	/** Get header value text by label (e.g. "Study", "Program"). Uses label text then sibling value with data-testid. */
	private static String getHeaderValueByLabel(WebDriver driver, String label) {
		try {
			String xpath = "//*[contains(normalize-space(text()),'" + label + "')]/following-sibling::*[1]//*[@data-testid='property-value' or @data-testid='submission-name-display']"
			List<WebElement> el = driver.findElements(By.xpath(xpath))
			if (el != null && !el.isEmpty()) return el.get(0).getText()?.trim() ?: ""
		} catch (Exception e) {
			KeywordUtil.logInfo("getHeaderValueByLabel('${label}'): " + e.getMessage())
		}
		return ""
	}

	/** Get full value from aria-label on truncated-text-wrapper (avoids truncated visible text). Returns empty if not found. */
	private static String getHeaderValueFromTruncatedAriaLabel(WebDriver driver, String label) {
		try {
			String xpath = "//*[contains(normalize-space(text()),'" + label + "')]/following-sibling::*[1]//*[@data-testid='truncated-text-wrapper']"
			List<WebElement> el = driver.findElements(By.xpath(xpath))
			if (el != null && !el.isEmpty()) {
				String aria = el.get(0).getAttribute("aria-label")?.trim()
				if (aria != null && !aria.isEmpty()) return aria
			}
		} catch (Exception e) {
			KeywordUtil.logInfo("getHeaderValueFromTruncatedAriaLabel('${label}'): " + e.getMessage())
		}
		return ""
	}

	/** Get header value: prefers full value from truncated-text-wrapper aria-label, else from property-value/submission-name-display text. */
	private static String getHeaderValueByLabelOrAria(WebDriver driver, String label) {
		String fromAria = getHeaderValueFromTruncatedAriaLabel(driver, label)
		if (fromAria != null && !fromAria.isEmpty()) return fromAria
		return getHeaderValueByLabel(driver, label)
	}

	private static String getTooltipTextByHover(WebDriver driver, String triggerXpath) {
		try {
			WebElement trigger = driver.findElement(By.xpath(triggerXpath))
			new Actions(driver).moveToElement(trigger).perform()
			WebUI.delay(1)
			List<WebElement> tooltips = driver.findElements(By.cssSelector("[role='tooltip']"))
			if (tooltips != null && !tooltips.isEmpty()) return tooltips.get(0).getText()?.replaceAll(/\s+/, ' ')?.trim() ?: ""
		} catch (Exception e) {
			KeywordUtil.logInfo("getTooltipTextByHover: " + e.getMessage())
		}
		return ""
	}

	/**
	 * Verify data submission header fields and tooltips (Study, Program, Collaborators) against expected* values.
	 * Expected values come from: login (expectedSubmitter), createDataSubmission (expectedSubmissionName, expectedDataCommons),
	 * hardcoded defaults (expectedStudy, expectedProgram, expectedDataConcierge), and captureDataSubmissionHeaderExpectedValues() for the rest.
	 */
	@Keyword
	static void verifyDataSubmissionHeaders() {
		WebDriver driver = DriverFactory.getWebDriver()
		try {
			WebUI.waitForElementPresent(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Summary/SubmissionName-Lbl'), 15)
		} catch (Exception e) {
			KeywordUtil.markFailed("verifyDataSubmissionHeaders: not on submission detail page. " + e.getMessage())
			return
		}
		String exp = expectedSubmissionName ?: ""
		String act = getHeaderValueByLabelOrAria(driver, "Submission Name")
		if (!act) act = WebUI.getAttribute(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Summary/SubmissionName-Lbl'), "aria-label") ?: ""
		if (!act) act = WebUI.getText(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Summary/SubmissionName-Lbl'))?.trim() ?: ""
		compareAndReport("Submission Name", exp, act)
		exp = expectedSubmitter.toLowerCase() +" Please Do Not Edit" ?: ""
		act = getHeaderValueByLabelOrAria(driver, "Submitter")
		compareAndReport("Submitter", exp, act)
		exp = expectedSubmissionType ?: ""
		act = getHeaderValueByLabel(driver, "Submission Type")
		compareAndReport("Submission Type", exp, act)
		exp = expectedCollaboratorsCount ?: ""
		try {
			act = WebUI.getText(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Summary/Collaborators-Btn'))?.trim() ?: ""
		} catch (Exception e) {
			act = ""
		}
		compareAndReport("Collaborators", exp, act)
		exp = expectedStudy ?: ""
		act = getHeaderValueByLabel(driver, "Study")
		compareAndReport("Study", exp, act)
		exp = expectedDataCommons ?: ""
		act = getHeaderValueByLabel(driver, "Data Commons")
		compareAndReport("Data Commons", exp, act)
		exp = expectedProgram ?: ""
		act = getHeaderValueByLabel(driver, "Program")
		compareAndReport("Program", exp, act)
		exp = expectedDataConcierge ?: ""
		act = getHeaderValueByLabelOrAria(driver, "Data Concierge")
		compareAndReport("Data Concierge", exp, act)
		// Tooltips (same pattern as Collaborators for each)
		try {
			TestObject collaboratorsBtn = findTestObject('CRDC/AILocators/DataSubmissions/Detail/Summary/Collaborators-Btn')
			WebUIUtils.verifyTooltipOnHover(collaboratorsBtn, expectedCollaboratorsTooltip, "add new collaborators")
		} catch (Exception e) {
			ValidationReport.addValidationReportItem(HEADERS_SECTION, "Collaborators tooltip", expectedCollaboratorsTooltip, "(error: " + e.getMessage() + ")", "Fail")
		}
		try {
			String submissionNameXpath = "//*[contains(normalize-space(text()),'Submission Name')]/following-sibling::*[1]"
			TestObject submissionNameTo = new com.kms.katalon.core.testobject.TestObject("SubmissionNameValue")
			submissionNameTo.addProperty("xpath", com.kms.katalon.core.testobject.ConditionType.EQUALS, submissionNameXpath)
			String expTip = expectedSubmissionName ?: ""
			WebUIUtils.verifyTooltipOnHover(submissionNameTo, expTip, (expTip.length() > 20 ? expTip.take(20) : expTip) ?: "auto-test")
		} catch (Exception e) {
			ValidationReport.addValidationReportItem(HEADERS_SECTION, "Submission Name tooltip", expectedSubmissionName ?: "(empty)", "(error: " + e.getMessage() + ")", "Fail")
		}
	}

	private static void compareAndReport(String label, String expected, String actual) {
		String normExp = expected?.replaceAll(/\s+/, ' ')?.trim() ?: ""
		String normAct = actual?.replaceAll(/\s+/, ' ')?.trim() ?: ""
		String status = normExp.equals(normAct) ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(HEADERS_SECTION, label, expected ?: "(empty)", actual ?: "(empty)", status)
	}

	/**
	 * Read the Full Submission History dialog (must be already open) and verify the list of statuses
	 * matches the expected list. Order is newest-first (top to bottom in the UI). Closes the dialog when done.
	 *
	 * @param expectedStatusesNewestFirst List of status strings in order from newest to oldest (e.g. ["COMPLETED", "RELEASED", "SUBMITTED", ...])
	 */
	@Keyword
	static void verifyFullSubmissionHistory(List<String> expectedStatusesNewestFirst) {
		WebDriver driver = DriverFactory.getWebDriver()
		WebElement dialog = null
		try {
			WebUI.waitForElementPresent(findTestObject('CRDC/AILocators/DataSubmissions/HistoryDialog/Dialog'), 10)
			dialog = driver.findElement(By.xpath("//*[@data-testid='history-dialog']"))
		} catch (Exception e) {
			KeywordUtil.markFailed("verifyFullSubmissionHistory: Submission History dialog not found. " + e.getMessage())
			return
		}
		// Wait for first row (newest = COMPLETED) so the full list is rendered before we read
		try {
			TestObject firstStatus = new com.kms.katalon.core.testobject.TestObject("HistoryItem0Status")
			firstStatus.addProperty("xpath", com.kms.katalon.core.testobject.ConditionType.EQUALS, "//*[@data-testid='history-dialog']//*[@data-testid='history-item-0-status']")
			WebUI.waitForElementPresent(firstStatus, 10)
			WebUI.delay(1)
		} catch (Exception e) {
			KeywordUtil.logInfo("verifyFullSubmissionHistory: first history row not found within 10s: " + e.getMessage())
		}
		List<String> actualStatuses = new ArrayList<>()
		try {
			dialog = driver.findElement(By.xpath("//*[@data-testid='history-dialog']"))
			// Dialog uses data-testid="history-item-N-status" for each row's status (N = 0,1,2,... newest first)
			List<WebElement> statusElements = dialog.findElements(By.xpath(".//*[contains(@data-testid,'history-item-') and contains(@data-testid,'-status')]"))
			if (statusElements != null) {
				for (WebElement el : statusElements) {
					String text = el.getText()?.trim() ?: ""
					if (!text.isEmpty()) actualStatuses.add(text)
				}
			}
		} catch (Exception e) {
			KeywordUtil.logInfo("verifyFullSubmissionHistory: row extraction " + e.getMessage())
		}
		List<String> expectedNormalized = new ArrayList<>()
		if (expectedStatusesNewestFirst != null) {
			for (String s : expectedStatusesNewestFirst) {
				expectedNormalized.add((s ?: "")?.replaceAll("\\s+", "")?.trim()?.toUpperCase() ?: "")
			}
		}
		List<String> actualNormalized = new ArrayList<>()
		for (String s : actualStatuses) {
			actualNormalized.add((s ?: "")?.replaceAll("\\s+", "")?.trim()?.toUpperCase() ?: "")
		}
		boolean match = expectedNormalized.size() == actualNormalized.size()
		if (match) {
			for (int i = 0; i < expectedNormalized.size(); i++) {
				if (!expectedNormalized.get(i).equals(actualNormalized.get(i))) {
					match = false
					break
				}
			}
		}
		String expectedSummary = expectedNormalized.join(", ")
		String actualSummary = actualNormalized.join(", ")
		String status = match ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(SUBMISSION_HISTORY_SECTION, "History (newest first)", expectedSummary, actualSummary, status)
		if (!match) {
			KeywordUtil.logInfo("verifyFullSubmissionHistory: expected " + expectedSummary + "; actual " + actualSummary)
		}
		try {
			WebUI.click(findTestObject('CRDC/AILocators/DataSubmissions/HistoryDialog/Close-Btn'))
			WebUI.delay(1)
		} catch (Exception e) {
			KeywordUtil.logInfo("verifyFullSubmissionHistory: could not close dialog: " + e.getMessage())
		}
	}

	/**
	 * Navigate to the Data View tab, select the specified node type, select all rows,
	 * click Delete, confirm the deletion dialog, and wait for deletion to complete.
	 * Assumes the user is already on the Data Submission detail page and that data
	 * for the given node has been uploaded.
	 *
	 * @param node Node type name as it appears in the dropdown (e.g. "program", "participant", "consent_group")
	 */
	@Keyword
	void deleteNodeFromDataView(String node) {
		KeywordUtil.logInfo("--- deleteNodeFromDataView: Deleting all rows for node '" + node + "' ---")
		try {
			WebDriver driver = DriverFactory.getWebDriver()

			// Step 1: Navigate to Data View tab
			KeywordUtil.logInfo("Step 1: Clicking Data View tab")
			WebUIUtils.clickTab('CRDC/AILocators/DataSubmissions/Detail/Tabs/DataView-Tab')
			Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
			WebUI.delay(2)

			// Step 2: Select node type from dropdown
			KeywordUtil.logInfo("Step 2: Selecting node type '" + node + "' from dropdown")
			WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/DataSubmissions/Detail/DataView/Filters/NodeType-Ddn'), 30)
			WebUI.click(findTestObject('CRDC/AILocators/DataSubmissions/Detail/DataView/Filters/NodeType-Ddn'))
			WebUI.delay(1)
			WebElement option = driver.findElement(By.xpath("//li[normalize-space(text())='" + node + "']"))
			option.click()
			WebUI.delay(2)
			Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
			WebUI.delay(1)

			// Step 3: Select all rows via header checkbox
			KeywordUtil.logInfo("Step 3: Selecting all rows via header checkbox")
			WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/DataSubmissions/Detail/DataView/HeaderCheckbox-Chkbx'), 10)
			WebUI.click(findTestObject('CRDC/AILocators/DataSubmissions/Detail/DataView/HeaderCheckbox-Chkbx'))
			WebUI.delay(1)

			// Step 4: Click Delete button
			KeywordUtil.logInfo("Step 4: Clicking Delete button")
			WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/DataSubmissions/Detail/DataView/Filters/DeleteNodeData-Btn'), 10)
			WebUI.click(findTestObject('CRDC/AILocators/DataSubmissions/Detail/DataView/Filters/DeleteNodeData-Btn'))
			WebUI.delay(1)

			// Step 5: Confirm deletion in dialog
			KeywordUtil.logInfo("Step 5: Confirming deletion in dialog")
			WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/DeleteDialog/Confirm-Btn'), 10)
			WebUI.click(findTestObject('CRDC/AILocators/DeleteDialog/Confirm-Btn'))

			// Step 6: Wait for deletion to complete
			KeywordUtil.logInfo("Step 6: Waiting for deletion to complete (up to 120s)")
			Utils.waitForElementNotVisible('CRDC/AILocators/DataSubmissions/Detail/DataView/DeletionAlert', 120)

			KeywordUtil.logInfo("deleteNodeFromDataView: Successfully deleted all rows for node '" + node + "'")
		} catch (Exception e) {
			KeywordUtil.markFailed("deleteNodeFromDataView: Failed to delete node '" + node + "'. Error: " + e.getMessage())
		}
	}

	/**
	 * Navigate to the Data View tab, select the specified node type, and delete
	 * a specific number of rows by clicking individual row checkboxes.
	 *
	 * If the requested rowCount exceeds the default visible rows, the method
	 * changes the rows-per-page setting to ensure enough rows are visible before
	 * selecting. The rows-per-page options available are [5, 10, 20, 50].
	 *
	 * If rowCount exceeds the total available rows, all visible rows are selected
	 * and a warning is logged.
	 *
	 * @param node     Node type name as it appears in the dropdown (e.g. "program", "file")
	 * @param rowCount Number of rows to select and delete (must be >= 1)
	 */
	@Keyword
	void deleteRowsFromDataView(String node, int rowCount) {
		KeywordUtil.logInfo("--- deleteRowsFromDataView: Deleting " + rowCount + " row(s) for node '" + node + "' ---")
		if (rowCount < 1) {
			KeywordUtil.markFailed("deleteRowsFromDataView: rowCount must be >= 1. Got: " + rowCount)
			return
		}
		try {
			WebDriver driver = DriverFactory.getWebDriver()

			WebUIUtils.clickTab('CRDC/AILocators/DataSubmissions/Detail/Tabs/DataView-Tab')
			Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
			WebUI.delay(2)

			KeywordUtil.logInfo("Selecting node type '" + node + "' from dropdown")
			WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/DataSubmissions/Detail/DataView/Filters/NodeType-Ddn'), 30)
			WebUI.click(findTestObject('CRDC/AILocators/DataSubmissions/Detail/DataView/Filters/NodeType-Ddn'))
			WebUI.delay(1)
			WebElement option = driver.findElement(By.xpath("//li[normalize-space(text())='" + node + "']"))
			option.click()
			WebUI.delay(2)
			Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
			WebUI.delay(1)

			int[] rpOptions = [5, 10, 20, 50]
			int targetRp = rpOptions.find { it >= rowCount } ?: 50
			KeywordUtil.logInfo("Setting rows per page to " + targetRp + " to accommodate " + rowCount + " row(s)")
			WebUIUtils.changeRowsPerPage(targetRp)
			Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
			WebUI.delay(1)

			List<WebElement> checkboxes = driver.findElements(By.xpath("//*[@data-testid='row-checkbox']"))
			int available = checkboxes.size()
			int toSelect = Math.min(rowCount, available)
			if (toSelect < rowCount) {
				KeywordUtil.logInfo("deleteRowsFromDataView: requested " + rowCount + " but only " + available + " row(s) visible. Selecting all " + available + ".")
			}
			KeywordUtil.logInfo("Clicking " + toSelect + " individual row checkbox(es)")
			for (int i = 0; i < toSelect; i++) {
				checkboxes.get(i).click()
				WebUI.delay(0.3)
			}
			WebUI.delay(1)

			KeywordUtil.logInfo("Clicking Delete button")
			WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/DataSubmissions/Detail/DataView/Filters/DeleteNodeData-Btn'), 10)
			WebUI.click(findTestObject('CRDC/AILocators/DataSubmissions/Detail/DataView/Filters/DeleteNodeData-Btn'))
			WebUI.delay(1)

			KeywordUtil.logInfo("Confirming deletion in dialog")
			WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/DeleteDialog/Confirm-Btn'), 10)
			WebUI.click(findTestObject('CRDC/AILocators/DeleteDialog/Confirm-Btn'))
			WebUI.delay(3)

			KeywordUtil.logInfo("Waiting for deletion to complete")
			Utils.waitForElementNotVisible('CRDC/AILocators/DataSubmissions/Detail/DataView/DeletionAlert', 60)

			KeywordUtil.logInfo("deleteRowsFromDataView: Successfully deleted " + toSelect + " row(s) for node '" + node + "'")
		} catch (Exception e) {
			KeywordUtil.markFailed("deleteRowsFromDataView: Failed to delete rows for node '" + node + "'. Error: " + e.getMessage())
		}
	}

	// ------------------------------- Cross Validation Results Verification -------------------------------

	/** Scope to the GenericTable that exposes Cross Validation column "Submitted Identifier" (avoids document-wide generic-table-row). */
	private static final String CV_GENERIC_TABLE_XPATH =
			"//*[@data-testid='generic-table'][.//*[starts-with(@data-testid,'generic-table-header-')][normalize-space()='Submitted Identifier']]"
	private static final String CV_TABLE_HEADER_CELLS_XPATH = CV_GENERIC_TABLE_XPATH + "//*[starts-with(@data-testid,'generic-table-header-')]"
	private static final String CV_TABLE_DATA_ROWS_XPATH = CV_GENERIC_TABLE_XPATH + "//*[@data-testid='generic-table-row']"

	private static List<WebElement> findCrossValidationTableHeaderCells(WebDriver driver) {
		return driver.findElements(By.xpath(CV_TABLE_HEADER_CELLS_XPATH))
	}

	private static List<WebElement> findCrossValidationTableDataRows(WebDriver driver) {
		return driver.findElements(By.xpath(CV_TABLE_DATA_ROWS_XPATH))
	}

	/** Rows in the CV table that reference this peer submission via conflicting-link data-testid (fast path; avoids scanning every td per page). */
	private static List<WebElement> findCrossValidationRowsWithConflictingLink(WebDriver driver, String conflictingSubmissionId) {
		if (conflictingSubmissionId == null || conflictingSubmissionId.trim().isEmpty()) {
			return Collections.emptyList()
		}
		String u = conflictingSubmissionId.trim()
		String xpath = CV_GENERIC_TABLE_XPATH + "//*[@data-testid='generic-table-row'][.//*[@data-testid='conflicting-link-" + u + "']]"
		return driver.findElements(By.xpath(xpath))
	}

	/**
	 * Read all rows from the Cross Validation Results table in the UI (full table, all pages).
	 * @return List of [submittedId, nodeType, severity, issueText] lists
	 */
	private List<List<String>> readCrossValidationResultsTableFromUI() {
		WebDriver driver = DriverFactory.getWebDriver()

		WebUIUtils.clickTab('CRDC/AILocators/DataSubmissions/Detail/Tabs/CrossValidation-Tab')
		WebUI.delay(2)
		Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 30)
		WebUI.delay(1)

		WebUIUtils.changeRowsPerPage(50)
		WebUI.delay(1)
		Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
		WebUI.delay(1)

		List<WebElement> headerEls = findCrossValidationTableHeaderCells(driver)
		List<String> headerNames = new ArrayList<>()
		for (WebElement h : headerEls) headerNames.add(h.getText().trim())
		KeywordUtil.logInfo("Cross Validation table headers: " + headerNames)

		int idxSubmitted = headerNames.findIndexOf { it.equalsIgnoreCase("Submitted Identifier") }
		int idxNodeType = headerNames.findIndexOf { it.equalsIgnoreCase("Node Type") }
		int idxSeverity = headerNames.findIndexOf { it.equalsIgnoreCase("Severity") }
		int idxIssues = headerNames.findIndexOf { it.equalsIgnoreCase("Issues") }
		if (idxIssues < 0) idxIssues = headerNames.findIndexOf { it.toLowerCase().contains("issue") }

		if (idxSubmitted < 0 || idxNodeType < 0 || idxSeverity < 0) {
			lastCrossValidationTableRowCount = 0
			KeywordUtil.markFailed("Cross Validation table missing required columns (Submitted Identifier, Node Type, Severity). Headers: " + headerNames)
			return Collections.emptyList()
		}

		List<List<String>> allRows = new ArrayList<>()
		boolean hasMorePages = true

		while (hasMorePages) {
			int rowCount = 0
			for (int w = 0; w < 5; w++) {
				List<WebElement> waitRows = findCrossValidationTableDataRows(driver)
				int n = waitRows != null ? waitRows.size() : 0
				WebUI.delay(0.5)
				waitRows = findCrossValidationTableDataRows(driver)
				int n2 = waitRows != null ? waitRows.size() : 0
				if (n == n2) {
					rowCount = n
					break
				}
			}

			List<WebElement> pageRows = findCrossValidationTableDataRows(driver)
			if (pageRows == null) {
				pageRows = Collections.emptyList()
			}

			for (int i = 0; i < pageRows.size(); i++) {
				boolean done = false
				for (int attempt = 1; attempt <= 3 && !done; attempt++) {
					try {
						if (i >= pageRows.size()) {
							break
						}
						WebElement row = pageRows.get(i)
						List<WebElement> cells = row.findElements(By.tagName("td"))
						String submitted = getCellText(cells, idxSubmitted, false)
						String nodeType = getCellText(cells, idxNodeType, false)
						String severity = getCellText(cells, idxSeverity, false)
						// For Cross Validation issue text, read visible cell text directly.
						// Using getCellText() here can trigger slow nested element lookups under implicit wait.
						String issues = (idxIssues >= 0 && idxIssues < cells.size()) ? cells.get(idxIssues).getText().trim() : ""
						allRows.add(Arrays.asList(submitted, nodeType, severity, issues))
						done = true
					} catch (StaleElementReferenceException e) {
						pageRows = findCrossValidationTableDataRows(driver)
						if (pageRows == null) {
							pageRows = Collections.emptyList()
						}
						if (attempt == 3) {
							throw e
						}
						WebUI.delay(0.5)
					}
				}
			}

			List<WebElement> nextBtns = driver.findElements(By.cssSelector("[aria-label='Go to next page']:not([disabled])"))
			if (nextBtns != null && !nextBtns.isEmpty()) {
				nextBtns.get(0).click()
				WebUI.delay(1)
				Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
				WebUI.delay(1)
			} else {
				hasMorePages = false
			}
		}

		lastCrossValidationTableRowCount = allRows.size()
		KeywordUtil.logInfo("readCrossValidationResultsTableFromUI: read " + allRows.size() + " total row(s)")
		return allRows
	}

	/** Multiset of submitted_id from expected TSV rows (non-empty id only). */
	private static Map<String, Integer> buildExpectedCountBySubmittedId(List<List<String>> allExpected) {
		Map<String, Integer> counts = new LinkedHashMap<>()
		for (List<String> exp : allExpected) {
			if (exp == null || exp.isEmpty()) continue
			String id = exp.get(0) != null ? exp.get(0).trim() : ""
			if (id.isEmpty()) continue
			counts.put(id, counts.getOrDefault(id, 0) + 1)
		}
		return counts
	}

	/** True when matched rows include at least the expected count per submitted_id. */
	private static boolean anchoredMatchedCountsSatisfyExpected(Map<String, Integer> expectedCountBySubmittedId, Map<String, Integer> matchedCountBySubmittedId) {
		for (Map.Entry<String, Integer> e : expectedCountBySubmittedId.entrySet()) {
			int need = e.getValue() != null ? e.getValue().intValue() : 0
			int have = matchedCountBySubmittedId.getOrDefault(e.getKey(), 0)
			if (have < need) return false
		}
		return true
	}

	/**
	 * Cross Validation rows whose Conflicting Submission matches conflictingSubmissionId.
	 * Paginates like readCrossValidationResultsTableFromUI. Each page uses one XPath for generic-table-row elements
	 * that contain a conflicting-link data-testid for the peer UUID (avoids scanning every td). Stops when expected
	 * per-submitted_id counts are satisfied, or when there is no next page. If the peer row never appears, returns empty after markFailed.
	 */
	private List<List<String>> readCrossValidationRowsForConflictingSubmission(String conflictingSubmissionId, Map<String, Integer> expectedCountBySubmittedId) {
		WebDriver driver = DriverFactory.getWebDriver()

		WebUIUtils.clickTab('CRDC/AILocators/DataSubmissions/Detail/Tabs/CrossValidation-Tab')
		WebUI.delay(2)
		Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 30)
		WebUI.delay(1)

		WebUIUtils.changeRowsPerPage(50)
		WebUI.delay(1)
		Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
		WebUI.delay(1)

		List<WebElement> headerEls = findCrossValidationTableHeaderCells(driver)
		List<String> headerNames = new ArrayList<>()
		for (WebElement h : headerEls) {
			headerNames.add(h.getText().trim())
		}
		KeywordUtil.logInfo("readCrossValidationRowsForConflictingSubmission: Cross Validation table headers: " + headerNames)

		int idxSubmitted = headerNames.findIndexOf { it.equalsIgnoreCase("Submitted Identifier") }
		int idxNodeType = headerNames.findIndexOf { it.equalsIgnoreCase("Node Type") }
		int idxSeverity = headerNames.findIndexOf { it.equalsIgnoreCase("Severity") }
		int idxIssues = headerNames.findIndexOf { it.equalsIgnoreCase("Issues") }
		if (idxIssues < 0) {
			idxIssues = headerNames.findIndexOf { it.toLowerCase().contains("issue") }
		}

		if (idxSubmitted < 0 || idxNodeType < 0 || idxSeverity < 0) {
			lastCrossValidationTableRowCount = 0
			KeywordUtil.logInfo("readCrossValidationRowsForConflictingSubmission: missing required columns (Submitted Identifier, Node Type, Severity). Headers: " + headerNames)
			return null
		}

		List<List<String>> matchedRows = new ArrayList<>()
		Map<String, Integer> matchedCountBySubmittedId = new HashMap<>()
		boolean hasMorePages = true
		boolean stopPagination = false

		while (hasMorePages && !stopPagination) {
			for (int w = 0; w < 5; w++) {
				List<WebElement> waitRows = findCrossValidationTableDataRows(driver)
				int n = waitRows != null ? waitRows.size() : 0
				WebUI.delay(0.5)
				waitRows = findCrossValidationTableDataRows(driver)
				int n2 = waitRows != null ? waitRows.size() : 0
				if (n == n2) {
					break
				}
			}

			List<WebElement> rowsToProcess = findCrossValidationRowsWithConflictingLink(driver, conflictingSubmissionId)
			if (rowsToProcess == null) {
				rowsToProcess = Collections.emptyList()
			}

			for (int i = 0; i < rowsToProcess.size() && !stopPagination; i++) {
				boolean done = false
				for (int attempt = 1; attempt <= 3 && !done; attempt++) {
					try {
						if (i >= rowsToProcess.size()) {
							break
						}
						WebElement row = rowsToProcess.get(i)
						List<WebElement> cells = row.findElements(By.tagName("td"))
						String submitted = getCellText(cells, idxSubmitted, false)
						String nodeType = getCellText(cells, idxNodeType, false)
						String severity = getCellText(cells, idxSeverity, false)
						String issues = (idxIssues >= 0 && idxIssues < cells.size()) ? cells.get(idxIssues).getText().trim() : ""
						matchedRows.add(Arrays.asList(submitted, nodeType, severity, issues))
						if (expectedCountBySubmittedId != null && !expectedCountBySubmittedId.isEmpty()) {
							String subTrim = submitted != null ? submitted.trim() : ""
							if (!subTrim.isEmpty()) {
								matchedCountBySubmittedId.put(subTrim, matchedCountBySubmittedId.getOrDefault(subTrim, 0) + 1)
							}
							if (anchoredMatchedCountsSatisfyExpected(expectedCountBySubmittedId, matchedCountBySubmittedId)) {
								KeywordUtil.logInfo("readCrossValidationRowsForConflictingSubmission: early stop — collected enough matching rows for expected submitted_id counts")
								stopPagination = true
							}
						}
						done = true
					} catch (StaleElementReferenceException e) {
						rowsToProcess = findCrossValidationRowsWithConflictingLink(driver, conflictingSubmissionId)
						if (rowsToProcess == null) {
							rowsToProcess = Collections.emptyList()
						}
						if (attempt == 3) {
							throw e
						}
						WebUI.delay(0.5)
					}
				}
			}

			if (stopPagination) {
				hasMorePages = false
				continue
			}
			List<WebElement> nextBtns = driver.findElements(By.cssSelector("[aria-label='Go to next page']:not([disabled])"))
			if (nextBtns != null && !nextBtns.isEmpty()) {
				nextBtns.get(0).click()
				WebUI.delay(1)
				Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
				WebUI.delay(1)
			} else {
				hasMorePages = false
			}
		}

		if (expectedCountBySubmittedId != null && !expectedCountBySubmittedId.isEmpty()
				&& !anchoredMatchedCountsSatisfyExpected(expectedCountBySubmittedId, matchedCountBySubmittedId)) {
			KeywordUtil.markFailed("readCrossValidationRowsForConflictingSubmission: no conflicting-link row for peer submission " + conflictingSubmissionId
					+ " after scanning all Cross Validation pages, or submitted identifiers did not match expected counts.")
			lastCrossValidationTableRowCount = 0
			return Collections.emptyList()
		}

		lastCrossValidationTableRowCount = matchedRows.size()
		KeywordUtil.logInfo("readCrossValidationRowsForConflictingSubmission: collected " + matchedRows.size() + " row(s) for conflicting submission " + conflictingSubmissionId)
		return matchedRows
	}

	/**
	 * Load expected cross-validation results from a TSV file.
	 * TSV must have a header row with columns: submitted_id, node_type, severity, issue.
	 * @param tsvPath absolute path to the expected TSV
	 * @return List of [submittedId, nodeType, severity, issue] lists
	 */
	private List<List<String>> loadExpectedCrossValidationResultsTsv(String tsvPath) {
		List<List<String>> out = new ArrayList<>()
		Path path = Paths.get(tsvPath)
		if (!Files.exists(path)) {
			KeywordUtil.logInfo("loadExpectedCrossValidationResultsTsv: file not found: " + tsvPath)
			return out
		}
		List<String> lines = Files.readAllLines(path)
		if (lines.size() < 2) return out

		String[] headers = lines.get(0).split("\t", -1)
		int idxId = -1, idxNode = -1, idxSev = -1, idxIssue = -1
		for (int i = 0; i < headers.length; i++) {
			String h = headers[i].trim().toLowerCase()
			if (h.equals("submitted_id")) idxId = i
			else if (h.equals("node_type")) idxNode = i
			else if (h.equals("severity")) idxSev = i
			else if (h.equals("issue")) idxIssue = i
		}
		if (idxId < 0 || idxNode < 0 || idxSev < 0 || idxIssue < 0) {
			KeywordUtil.logInfo("loadExpectedCrossValidationResultsTsv: missing required columns (submitted_id, node_type, severity, issue). Headers: " + Arrays.asList(headers))
			return out
		}

		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i).trim()
			if (line.isEmpty()) continue
				String[] cells = line.split("\t", -1)
			String id = idxId < cells.length ? cells[idxId].trim() : ""
			String node = idxNode < cells.length ? cells[idxNode].trim() : ""
			String sev = idxSev < cells.length ? cells[idxSev].trim() : ""
			String issue = idxIssue < cells.length ? cells[idxIssue].trim() : ""
			if (!id.isEmpty()) {
				out.add(Arrays.asList(id, node, sev, issue))
			}
		}
		KeywordUtil.logInfo("loadExpectedCrossValidationResultsTsv: loaded " + out.size() + " expected row(s) from " + tsvPath)
		return out
	}

	/**
	 * Loads all expected CV rows from InputFiles/CRDC/MetadataData/&lt;tcName&gt;/expected-CrossValidationResults.
	 * @return null if the folder or TSVs are missing (markFailed already called)
	 */
	private List<List<String>> loadAllExpectedCrossValidationRowsForTcOrNull(String tcName) {
		String baseDir = RunConfiguration.getProjectDir()
		File dir = new File(baseDir, "InputFiles/CRDC/MetadataData/${tcName}/expected-CrossValidationResults")
		if (!dir.exists() || !dir.isDirectory()) {
			KeywordUtil.markFailed("verifyCrossValidationResultsVsExpected: expected-CrossValidationResults folder not found: " + dir.absolutePath)
			return null
		}
		List<File> expectedFiles = dir.listFiles()?.findAll { it.isFile() && it.name.endsWith("_expected-CrossValidationResults.tsv") } ?: []
		if (expectedFiles.isEmpty()) {
			KeywordUtil.markFailed("verifyCrossValidationResultsVsExpected: no *_expected-CrossValidationResults.tsv files in " + dir.absolutePath)
			return null
		}
		List<List<String>> allExpected = new ArrayList<>()
		for (File f : expectedFiles) {
			allExpected.addAll(loadExpectedCrossValidationResultsTsv(f.absolutePath))
		}
		return allExpected
	}

	/**
	 * Compares expected TSV rows to actual UI rows (same 4-tuple shape), reports via ValidationReport, and markFailed on any mismatch.
	 * @param checkUnexpectedUiRows when true, fails on UI submitted IDs not present in expected TSV (full-table mode).
	 */
	private void runCrossValidationVerificationAgainstActualRows(String tcName, List<List<String>> allExpected, List<List<String>> actualRows, boolean checkUnexpectedUiRows) {
		KeywordUtil.logInfo("Total expected cross-validation rows: " + allExpected.size())
		KeywordUtil.logInfo("Total actual cross-validation rows from UI: " + actualRows.size())

		// Group actual rows by Submitted ID so we can compare expected rows by key.
		// We keep List<List<String>> because one submitted ID can have multiple CV entries.
		Map<String, List<List<String>>> actualBySubmittedId = new LinkedHashMap<>()
		for (List<String> row : actualRows) {
			String key = row.get(0).trim()
			if (!key.isEmpty()) {
				if (!actualBySubmittedId.containsKey(key)) {
					actualBySubmittedId.put(key, new ArrayList<>())
				}
				actualBySubmittedId.get(key).add(row)
			}
		}

		List<String> errors = new ArrayList<>()
		String sectionName = "Cross Validation Results"
		Set<String> matchedActualKeys = new LinkedHashSet<>()

		// 5) For each expected row:
		//    - ensure Submitted ID exists in UI,
		//    - compare Node Type + Severity (case-insensitive),
		//    - compare Issue with normalized match (and allow expected issue to be contained in actual issue).
		for (List<String> exp : allExpected) {
			String expId = exp.get(0)
			String expNode = exp.get(1)
			String expSev = exp.get(2)
			String expIssue = exp.get(3)

			List<List<String>> actList = actualBySubmittedId.get(expId)
			if (actList == null || actList.isEmpty()) {
				errors.add("Submitted ID '" + expId + "': expected in UI but not found")
				ValidationReport.addValidationReportItem(sectionName, "Submitted ID " + expId,
						"NodeType=" + expNode + ", Severity=" + expSev + ", Issue=" + expIssue,
						"(not found in UI)", "Fail")
				continue
			}

			matchedActualKeys.add(expId)

			// Choose the best candidate row for this submitted ID.
			// Prefer exact row match first; otherwise use the first available row and report detailed field-level diffs.
			List<String> bestMatch = null
			boolean exactMatch = false
			for (List<String> act : actList) {
				String actNode = act.get(1)
				String actSev = act.get(2)
				String actIssue = act.size() >= 4 ? act.get(3) : ""
				if (actNode.equalsIgnoreCase(expNode) && actSev.equalsIgnoreCase(expSev) && actIssue.equals(expIssue)) {
					bestMatch = act
					exactMatch = true
					break
				}
				if (bestMatch == null) bestMatch = act
			}

			String actNode = bestMatch.get(1)
			String actSev = bestMatch.get(2)
			String actIssue = bestMatch.size() >= 4 ? bestMatch.get(3) : ""
			String expIssueNorm = normalizeCrossValidationIssue(expIssue)
			String actIssueNorm = normalizeCrossValidationIssue(actIssue)

			boolean nodeOk = actNode.equalsIgnoreCase(expNode)
			boolean sevOk = actSev.equalsIgnoreCase(expSev)
			boolean issueOk = actIssueNorm.equals(expIssueNorm) || (!expIssueNorm.isEmpty() && actIssueNorm.contains(expIssueNorm))

			if (!nodeOk) errors.add("Submitted ID '" + expId + "': Node Type expected '" + expNode + "', actual '" + actNode + "'")
			if (!sevOk) errors.add("Submitted ID '" + expId + "': Severity expected '" + expSev + "', actual '" + actSev + "'")
			if (!issueOk) errors.add("Submitted ID '" + expId + "': Issue expected '" + expIssue + "', actual '" + actIssue + "'")

			String status = (nodeOk && sevOk && issueOk) ? "Pass" : "Fail"
			ValidationReport.addValidationReportItem(sectionName, "Submitted ID " + expId,
					"NodeType=" + expNode + ", Severity=" + expSev + ", Issue=" + expIssue,
					"NodeType=" + actNode + ", Severity=" + actSev + ", Issue=" + actIssue,
					status)
		}

		// 6) Flag unexpected UI rows (present in UI but missing from expected files).
		// Skipped in anchored mode (only a subset of UI rows was read).
		if (checkUnexpectedUiRows) {
			for (String actualKey : actualBySubmittedId.keySet()) {
				if (!matchedActualKeys.contains(actualKey)) {
					List<List<String>> unexpectedRows = actualBySubmittedId.get(actualKey)
					for (List<String> act : unexpectedRows) {
						String actNode = act.get(1)
						String actSev = act.get(2)
						String actIssue = act.size() >= 4 ? act.get(3) : ""
						errors.add("Submitted ID '" + actualKey + "': present in UI but not in expected TSV")
						ValidationReport.addValidationReportItem(sectionName, "Submitted ID " + actualKey,
								"(not in expected)",
								"NodeType=" + actNode + ", Severity=" + actSev + ", Issue=" + actIssue,
								"Fail")
					}
				}
			}
		}

		// 7) Final assertion:
		// if any mismatch was collected, fail once with a summarized error count.
		if (!errors.isEmpty()) {
			KeywordUtil.logInfo("--- Cross Validation Results vs Expected - " + errors.size() + " issue(s) ---")
			for (String err : errors) KeywordUtil.logInfo(err)
			KeywordUtil.logInfo("--- end ---")
			KeywordUtil.markFailed("verifyCrossValidationResultsVsExpected: " + errors.size() + " mismatch(es). See log above.")
		} else {
			KeywordUtil.logInfo("Cross Validation Results match expected for TC: " + tcName)
		}
	}

	/**
	 * Verify Cross Validation Results in the UI against expected TSV files.
	 * Discovers all *_expected-CrossValidationResults.tsv files under
	 * InputFiles/CRDC/MetadataData/&lt;tcName&gt;/expected-CrossValidationResults/,
	 * loads expected rows, reads the full Cross Validation table from the UI,
	 * and compares by Submitted Identifier as the matching key.
	 * Fails on unexpected extra UI rows not present in the expected TSVs.
	 * @param tcName Test case name (e.g. TC38)
	 */
	@Keyword
	void verifyCrossValidationResultsVsExpected(String tcName) {
		KeywordUtil.logInfo("--- verifyCrossValidationResultsVsExpected for TC: " + tcName + " (full table) ---")
		List<List<String>> allExpected = loadAllExpectedCrossValidationRowsForTcOrNull(tcName)
		if (allExpected == null) {
			return
		}
		List<List<String>> actualRows = readCrossValidationResultsTableFromUI()
		runCrossValidationVerificationAgainstActualRows(tcName, allExpected, actualRows, true)
	}

	/**
	 * Verify Cross Validation rows that reference a peer submission (Conflicting Submission column).
	 * Use when the active submission is not the one listed as conflicting (e.g. DCP on submission 2 verifying conflicts with submission 1).
	 * Only matching rows are compared to the expected TSV; other CV rows on the same page are ignored.
	 * @param tcName Test case folder under MetadataData (e.g. TC25)
	 * @param conflictingSubmissionId Full UUID of the peer submission (Conflicting Submission link target)
	 */
	@Keyword
	void verifyCrossValidationResultsVsExpected(String tcName, String conflictingSubmissionId) {
		KeywordUtil.logInfo("--- verifyCrossValidationResultsVsExpected for TC: " + tcName + ", conflictingSubmissionId=" + conflictingSubmissionId + " ---")
		if (conflictingSubmissionId == null || conflictingSubmissionId.trim().isEmpty()) {
			KeywordUtil.markFailed("verifyCrossValidationResultsVsExpected: conflictingSubmissionId is required (peer submission UUID).")
			return
		}
		List<List<String>> allExpected = loadAllExpectedCrossValidationRowsForTcOrNull(tcName)
		if (allExpected == null) {
			return
		}
		if (allExpected.isEmpty()) {
			KeywordUtil.markFailed("verifyCrossValidationResultsVsExpected: expected TSV has no data rows for TC: " + tcName)
			return
		}
		Map<String, Integer> expectedBySubmittedId = buildExpectedCountBySubmittedId(allExpected)
		if (expectedBySubmittedId.isEmpty()) {
			KeywordUtil.markFailed("verifyCrossValidationResultsVsExpected: expected TSV has no submitted_id values for TC: " + tcName)
			return
		}
		List<List<String>> actualRows = readCrossValidationRowsForConflictingSubmission(conflictingSubmissionId, expectedBySubmittedId)
		if (actualRows == null) {
			KeywordUtil.markFailed("verifyCrossValidationResultsVsExpected: could not read Cross Validation table (missing required columns).")
			return
		}
		if (actualRows.isEmpty()) {
			KeywordUtil.markFailed("verifyCrossValidationResultsVsExpected: no Cross Validation rows matched conflicting submission '" + conflictingSubmissionId + "'")
			return
		}
		runCrossValidationVerificationAgainstActualRows(tcName, allExpected, actualRows, false)
	}

	private static final String CROSS_VAL_EXPORT_SECTION = "Cross Validation Export"

	/**
	 * Clicks the Cross Validation Results export button (export-cross-validation-button), then verifies the
	 * downloaded file name matches {submissionName}-cross-validation-results-YYYY-MM-DD.csv in project Downloads.
	 * Clears project Downloads first (Utils.clearProjectDownloadsFolder) so the new export is unambiguous.
	 * Assumes caller is on Cross Validation Results tab with export enabled.
	 *
	 * @param expectedSubmissionName Submission display name (normalized prefix must match UI export filename).
	 */
	@Keyword
	void clickCrossValidationExportAndVerifyDownloadedFileName(String expectedSubmissionName) {
		Utils.clearProjectDownloadsFolder()
		TestObject exportBtn = findTestObject("CRDC/AILocators/DataSubmissions/Detail/CrossValidation/Filters/ExportCrossValidation-Btn")
		if (!WebUIUtils.waitForElementClickableWithStaleRetry(exportBtn, 60)) {
			return
		}
		WebUI.click(exportBtn)
		WebUI.delay(8)
		File projectDownloads = getProjectDownloadsDir()
		File latest = findLatestFile(projectDownloads)
		if (latest == null) {
			KeywordUtil.markFailed("clickCrossValidationExportAndVerifyDownloadedFileName: no file found in project Downloads (${projectDownloads.absolutePath}) after clicking export")
			return
		}
		String fileName = latest.name
		def matcher = (fileName =~ /(?i)^(.+)-cross-validation-results-(\d{4}-\d{2}-\d{2})\.csv$/)
		if (!matcher.find()) {
			ValidationReport.addValidationReportItem(CROSS_VAL_EXPORT_SECTION, "Downloaded file name",
					"pattern: {submissionName}-cross-validation-results-YYYY-MM-DD.csv", fileName, "Fail")
			KeywordUtil.markFailed("clickCrossValidationExportAndVerifyDownloadedFileName: downloaded file name does not match pattern. Got: " + fileName)
			return
		}
		String namePart = matcher.group(1)
		if (expectedSubmissionName != null && !expectedSubmissionName.isEmpty()) {
			String normalizedExpected = normalizeSubmissionNameForExport(expectedSubmissionName)
			if (!namePart.startsWith(normalizedExpected)) {
				ValidationReport.addValidationReportItem(CROSS_VAL_EXPORT_SECTION, "Submission name in filename",
						"starts with " + normalizedExpected, namePart, "Fail")
				KeywordUtil.markFailed("clickCrossValidationExportAndVerifyDownloadedFileName: filename submission name '" + namePart + "' does not start with expected '" + normalizedExpected + "'")
				return
			}
		}
		ValidationReport.addValidationReportItem(CROSS_VAL_EXPORT_SECTION, "Downloaded file name",
				"pattern match, submission: ${namePart}", fileName, "Pass")
		KeywordUtil.logInfo("clickCrossValidationExportAndVerifyDownloadedFileName: verified downloaded file: ${fileName}")
	}
	
	/**
	 * Counts data rows in a UTF-8 CSV file (first row treated as header). Uses quote-aware record boundaries
	 * so commas inside quoted fields do not inflate the count. Suitable for any hub CSV with that shape;
	 * used by verifyCrossValidationExportCsvRowCountMatchesUi.
	 */
	private static int countCsvDataRowsExcludingHeader(File csvFile) {
		String text = new String(Files.readAllBytes(csvFile.toPath()), StandardCharsets.UTF_8)
		if (text == null || text.isEmpty()) return 0
		List<String> records = new ArrayList<>()
		StringBuilder cur = new StringBuilder()
		boolean inQuotes = false
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i)
			if (c == '"') {
				if (inQuotes && i + 1 < text.length() && text.charAt(i + 1) == '"') {
					cur.append('"')
					i++
				} else {
					inQuotes = !inQuotes
					cur.append(c)
				}
			} else if (!inQuotes && (c == '\n' || c == '\r')) {
				if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
					i++
				}
				String rec = cur.toString().trim()
				if (!rec.isEmpty()) records.add(rec)
				cur.setLength(0)
			} else {
				cur.append(c)
			}
		}
		String last = cur.toString().trim()
		if (!last.isEmpty()) records.add(last)
		if (records.isEmpty()) return 0
		return Math.max(0, records.size() - 1)
	}

	/**
	 * Verifies the most recently downloaded Cross Validation CSV has the same number of data rows as the
	 * Cross Validation table in the UI. Uses lastCrossValidationTableRowCount set by
	 * readCrossValidationResultsTableFromUI (e.g. inside verifyCrossValidationResultsVsExpected).
	 * If that was never set (still -1), re-reads the table. Call after clickCrossValidationExportAndVerifyDownloadedFileName.
	 * Clears project Downloads in a finally block after reading the CSV.
	 *
	 * Note: lastCrossValidationTableRowCount is static. If another test in the same Katalon run set it and
	 * this keyword runs without a prior readCrossValidationResultsTableFromUI in that test, the count could
	 * be stale. Export coverage (e.g. CRDCDH-1390 TC40) is intended to run standalone or early in a suite;
	 * reorder or force a table re-read if you chain many CV tests in one session.
	 */
	@Keyword
	void verifyCrossValidationExportCsvRowCountMatchesUi() {
		int uiRows = lastCrossValidationTableRowCount
		if (uiRows < 0) {
			KeywordUtil.logInfo("verifyCrossValidationExportCsvRowCountMatchesUi: lastCrossValidationTableRowCount unset; re-reading Cross Validation table")
			uiRows = readCrossValidationResultsTableFromUI().size()
		}
		File projectDownloads = getProjectDownloadsDir()
		File latest = findLatestFile(projectDownloads)
		if (latest == null) {
			KeywordUtil.markFailed("verifyCrossValidationExportCsvRowCountMatchesUi: no file in Downloads")
			Utils.clearProjectDownloadsFolder()
			return
		}
		try {
			int csvDataRows = countCsvDataRowsExcludingHeader(latest)
			String status = (csvDataRows == uiRows) ? "Pass" : "Fail"
			ValidationReport.addValidationReportItem(CROSS_VAL_EXPORT_SECTION, "CSV data rows vs UI table rows",
					String.valueOf(uiRows), String.valueOf(csvDataRows), status)
			if (status == "Fail") {
				KeywordUtil.markFailed("verifyCrossValidationExportCsvRowCountMatchesUi: UI had ${uiRows} row(s), CSV has ${csvDataRows} data row(s) in ${latest.name}")
				return
			}
			KeywordUtil.logInfo("verifyCrossValidationExportCsvRowCountMatchesUi: ${csvDataRows} CSV data row(s) matches ${uiRows} UI row(s) in ${latest.name}")
		} finally {
			Utils.clearProjectDownloadsFolder()
		}
	}
}
