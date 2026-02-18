package utilities

import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.List
import java.util.Map
import java.util.Set
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Action;
import com.kms.katalon.core.testobject.TestObject

/**
 * Data Submissions: Data View and Validation Results verification keywords.
 * Contains all keywords for verifying Data View tab (headers, row content vs metadata, download vs expected TSV)
 * and Validation Results tab (headers, UI vs expected TSV by node/severity/issue type).
 * Verify-only and upload-only are separate: use uploadFolder(tcName) then verifyDataViewMatchesMetadataForTC(tcName) for a two-step flow.
 * Moved from SubmissionRequest and Pbac.
 */
class DataSubmissions extends TestRunner {

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

	/**
	 * This function verifies the rows of the Data View tab match the metadata file for the given node.
	 * Loads TSV from metadataFile, filters columns (skips 'type' and fields with '.'), selects node in dropdown,
	 * then compares expected vs actual row-by-row. Uses aria-label for truncated cells when present.
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

			// Step 2: Determine which columns to keep (skip 'type' and fields with '.')
			String[] headers = lines.get(0).split("\t", -1)
			List<Integer> includedIndexes = new ArrayList<>()
			for (int j = 1; j < headers.length; j++) {
				if (!headers[j].contains(".")) {
					includedIndexes.add(j)
				}
			}

			// Step 3: Load expected values from filtered columns
			List<List<String>> expectedRows = new ArrayList<>()
			for (int i = 1; i < lines.size(); i++) {
				String[] fields = lines.get(i).split("\t", -1)
				List<String> row = new ArrayList<>()
				for (int index : includedIndexes) {
					String val = fields[index].trim()
					if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
						val = val.substring(1, val.length() - 1)
					}
					row.add(val)
				}
				expectedRows.add(row)
			}

			// Step 4: Select node from dropdown and wait for table refresh
			WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validation/NodeType-Ddn'), 30)
			WebUI.click(findTestObject('CRDC/DataSubmissions/Validation/NodeType-Ddn'))
			WebUI.delay(1)
			WebElement option = driver.findElement(By.xpath("//li[normalize-space(text())='" + node + "']"))
			option.click()
			WebUI.delay(1)

			// Step 5: Get fresh list of actual UI rows
			List<WebElement> actualRowElements = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Row")
			if (actualRowElements.size() == 20) {
				KeywordUtil.logInfo("Full table")
			} else if (actualRowElements.size() != expectedRows.size()) {
				KeywordUtil.markFailed("Row count mismatch: UI has " + actualRowElements.size() + ", but file has " + expectedRows.size())
			}

			// Step 6: Compare row by row and collect for report
			String sectionName = "Data View"
			boolean anyFail = false
			for (int i = 0; i < actualRowElements.size(); i++) {
				WebElement row = actualRowElements.get(i)
				List<WebElement> cells = row.findElements(By.tagName("td"))
				List<String> actualValues = new ArrayList<>()
				List<String> uiValues = new ArrayList<>()
				int uiColIndex = 0
				for (int j = 0; j < cells.size(); j++) {
					if (j == 0 || j == 2) continue  // skip checkbox and status
						if (uiColIndex >= includedIndexes.size()) break
						WebElement cell = cells.get(j)
					String actual
					String uiText = ""
					try {
						WebElement span = cell.findElement(By.tagName("span"))
						String ariaLabel = span.getAttribute("aria-label")
						uiText = span.getText().trim()
						actual = (ariaLabel != null && !ariaLabel.isEmpty()) ? ariaLabel.trim() : cell.getText().trim()
					} catch (Exception e) {
						actual = cell.getText().trim()
						uiText = actual
					}
					actualValues.add(actual)
					uiValues.add(uiText)
					uiColIndex++
				}
				List<String> expectedValues = i < expectedRows.size() ? expectedRows.get(i) : new ArrayList<>()
				if (actualValues.size() != expectedValues.size()) {
					anyFail = true
					ValidationReport.addValidationReportItem(sectionName, "Row " + (i + 1), "columns=" + expectedValues.size(), "columns=" + actualValues.size(), "Fail")
					continue
				}
				for (int j = 0; j < expectedValues.size(); j++) {
					String expected = expectedValues.get(j)
					String actual = actualValues.get(j)
					String actualUi = uiValues.get(j)
					String cleanedUi = actualUi.replace("...", "").trim()
					boolean match = actual.equals(expected) || expected.contains(cleanedUi)
					if (!match) anyFail = true
					ValidationReport.addValidationReportItem(sectionName, "Row " + (i + 1) + " Col " + (j + 1), expected, actual, match ? "Pass" : "Fail")
				}
			}
			if (anyFail) {
				KeywordUtil.markFailed("Data View: one or more row/column mismatches. See validation report.")
			} else {
				KeywordUtil.logInfo("Finished verifying-- Data View tab in UI matches the contents of the TSV (ignoring type and reference columns).")
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("Exception in verifyDataViewMatchesMetadata(): " + e.getMessage())
		}
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
		return files.sort { -it.lastModified() }.get(0)
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

	/** Load node -> submitted-identifier column name from Data File (Data Files/CRDC/DataSubmissions/ValidationResults-Mappings). Falls back to Excel if Data File not available. */
	private Map<String, String> loadValidationResultsMappings() {
		Map<String, String> nodeToColumn = new HashMap<>()
		try {
			def data = findTestData("Data Files/CRDC/DataSubmissions/ValidationResults-Mappings")
			int rows = data.getRowNumbers()
			String[] colNames = data.getColumnNames()
			String nodeCol = (colNames != null && colNames.length >= 1) ? colNames[0] : null
			String columnCol = (colNames != null && colNames.length >= 2) ? colNames[1] : null
			for (int i = 1; i <= rows; i++) {
				String nodeVal = nodeCol ? data.getValue(nodeCol, i) : data.getValue(1, i)
				String colVal = columnCol ? data.getValue(columnCol, i) : data.getValue(2, i)
				if (nodeVal != null && colVal != null) nodeToColumn.put(nodeVal.trim(), colVal.trim())
			}
		} catch (Exception e) {

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

	/** Load unique non-empty submitted identifier values from a TSV column. Returns List<String>. */
	private List<String> loadSubmittedIdentifiersFromTsv(String tsvPath, String submittedIdentifierColumn) {
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

	/**
	 * Apply Validation Results filters (node, severity, issue type) and read table rows: [submittedId, issueType, severity].
	 * Waits for Loading to disappear, stabilizes table row count, then re-queries rows by index and retries on StaleElementReferenceException to avoid flakiness.
	 */
	private List<List<String>> readValidationResultsTableFromUI(String node, String severity, String issueType) {
		WebDriver driver = DriverFactory.getWebDriver()
		WebUI.delay(1)
		WebUI.waitForElementClickable(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_NodeType-Ddn"), 15)
		WebUI.click(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_NodeType-Ddn"))
		WebUI.delay(1)
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
		WebUI.click(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_IssueType-Ddn"))
		WebUI.delay(1)
		WebElement issueOpt = driver.findElement(By.xpath("//li[normalize-space(text())='" + issueType + "']"))
		issueOpt.click()
		WebUI.delay(2)
		// Wait for table to finish loading after filter change; then a short delay so content can settle.
		Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
		WebUI.delay(1)
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
	 * Empty lists if folder missing or no TSV files. Used by uploadFolder and verifyDataViewMatchesMetadataForTC.
	 */
	private List<List<String>> listTsvFilesAndNodesForTC(String tcName) {
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
		}

		if(dataType?.toLowerCase()?.contains("only")) {
			WebUI.click(findTestObject('CRDC/DataSubmissions/Create/MetadataOnly-Btn'))
		}

		WebUI.delay(5)

		//Select the Data Commons
		WebDriver driver = DriverFactory.getWebDriver()
		WebUI.click(findTestObject('Object Repository/CRDC/DataSubmissions/Create/DataCommons-Ddn'))
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

		//Select the study
		WebUI.click(findTestObject('CRDC/DataSubmissions/Create/Study-Ddn'))
		WebUI.delay(1)
		List<WebElement> studyDropdownOptions = WebUI.findWebElements(findTestObject('Object Repository/CRDC/DataSubmissions/Create/StudyDdn-Option'), 10)
		for (WebElement option : studyDropdownOptions) {
			String studyName = option.getAttribute("innerText")?.trim()
			KeywordUtil.logInfo("Dropdown option is: " + studyName)
		}
		KeywordUtil.logInfo("Number of study options in dropdown: " + studyDropdownOptions.size())

		WebElement matchStudy = studyDropdownOptions.find {it.getAttribute("innerText")?.trim() == study}
		if (matchStudy != null) {
			try {
				new Actions(driver).moveToElement(matchStudy).click().perform()
			} catch (Exception e) {
				KeywordUtil.logInfo("Click blocked, retrying after overlay removal...")
				WebUI.executeJavaScript("""document.querySelectorAll('.MuiBackdrop-root, .MuiDialog-container').forEach(e => e.remove())""", null)
				WebUI.delay(1)
				new Actions(driver).moveToElement(matchStudy).click().perform()
			}
		} else {
			KeywordUtil.markFailed("Study option '${study}' not found in dropdown.")
		}

		//Enter the submission name -- setText() is not working, need to use Actions class
		String timestamp = new Date().format("yyyyMMdd_HHmmss")
		String submissionName = "auto-test_" + timestamp
		KeywordUtil.logInfo("Creating data submission with name " + submissionName)

		WebUI.click(findTestObject('CRDC/DataSubmissions/Create/SubmissionName-Field'))
		Actions actions = new Actions(DriverFactory.getWebDriver())
		WebElement el = WebUI.findWebElement(findTestObject('CRDC/DataSubmissions/Create/SubmissionName-Field'), 10)
		actions.moveToElement(el).click().sendKeys(submissionName).perform()

		//Create
		TestRunner.clickTab('CRDC/DataSubmissions/Create/Create-Btn')
		WebUI.delay(3)
	}

	/**
	 * Verify records in MongoDB for each node from the given test case.
	 * Gets submissionID from CliUploader.getSubmissionIdFromUploadActivityUrl(), derives nodes from
	 * listTsvFilesAndNodesForTC(tcName), uses loadValidationResultsMappings to get the submitted-identifier
	 * column per node, reads metadata TSV to extract submitted IDs, then queries MongoDB for each
	 * (submissionID, nodeID) where nodeID is the Submitted Identifier value.
	 * @param tcName Test case name (e.g. TC09) - used to derive nodes and metadata paths from InputFiles/CRDC/MetadataData/&lt;tcName&gt;/
	 * @param queryKey Query key in DBQueries data file (e.g. "released")
	 * @param expectedStatus Expected value for the "status" field (e.g. "Deleted")
	 */
	@Keyword
	void verifyRecordsInMongo(String tcName, String queryKey, String expectedStatus) {
		// Get submission ID from Upload Activity URL
		String submissionID = DataSubmissions.getSubmissionIdFromUploadActivityUrl()
		if (submissionID == null || submissionID.isEmpty()) {
			KeywordUtil.markFailed("verifyRecordsInMongo: Could not get submissionID from Upload Activity URL.")
			return
		}
		// Get metadata TSV paths and node names for this test case
		List<List<String>> listResult = listTsvFilesAndNodesForTC(tcName)
		List<String> relativePaths = listResult.get(0)
		List<String> nodes = listResult.get(1)
		if (nodes.isEmpty()) {
			KeywordUtil.markFailed("verifyRecordsInMongo: No nodes found for tcName '${tcName}'.")
			return
		}
		// Load node -> submitted-identifier column mapping from ValidationResults-Mappings
		Map<String, String> mappings = loadValidationResultsMappings()
		String baseDir = RunConfiguration.getProjectDir()
		// Connect to MongoDB
		DatabaseUtils.connectFromGlobalVariable()
		List<String> errors = new ArrayList<>()
		String sectionName = "MongoDB"
		try {
			for (int i = 0; i < nodes.size(); i++) {
				String node = nodes.get(i)
				String relativePath = relativePaths.get(i)
				// Resolve submitted-identifier column for this node
				String submittedIdColumn = mappings.get(node)
				if (submittedIdColumn == null || submittedIdColumn.isEmpty()) {
					KeywordUtil.logInfo("[WARNING] verifyRecordsInMongo: no mapping for node '${node}' in ValidationResults-Mappings; skipping.")
					continue
				}
				// Read metadata TSV and extract unique submitted identifier values
				String tsvPath = new File(baseDir, relativePath).absolutePath
				List<String> submittedIds = loadSubmittedIdentifiersFromTsv(tsvPath, submittedIdColumn)
				if (submittedIds.isEmpty()) {
					KeywordUtil.logInfo("[WARNING] verifyRecordsInMongo: no submitted identifiers for node '${node}' in ${relativePath}; skipping.")
					continue
				}
				// For each submitted ID, query MongoDB, verify record, and log to ValidationReport
				for (String submittedId : submittedIds) {
					Map params = [status: expectedStatus, submissionID: submissionID, nodeID: submittedId]
					def doc = DatabaseUtils.findRecordFromDataFile(
							"Data Files/CRDC/DataSubmissions/DBQueries", queryKey, params)
					String actualStatus = doc != null ? doc.get("status")?.toString() : null
					boolean recordOk = doc != null
					boolean statusOk = expectedStatus.equals(actualStatus)
					String status = (recordOk && statusOk) ? "Pass" : "Fail"
					String actualDisplay = actualStatus != null ? actualStatus : "(not found)"
					ValidationReport.addValidationReportItem(sectionName, "Submitted ID " + submittedId, expectedStatus, actualDisplay, status)
					if (status == "Fail") {
						errors.add("Submitted ID '${submittedId}': expected status '${expectedStatus}', actual '${actualDisplay}'")
					}
				}
			}
		} finally {
			DatabaseUtils.close()
		}
		if (!errors.isEmpty()) {
			KeywordUtil.logInfo("--- MongoDB verification - " + errors.size() + " mismatch(es) ---")
			for (String err : errors) KeywordUtil.logInfo(err)
			KeywordUtil.markFailed("verifyRecordsInMongo: " + errors.size() + " record(s) failed. See ValidationReport and log above.")
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
}
