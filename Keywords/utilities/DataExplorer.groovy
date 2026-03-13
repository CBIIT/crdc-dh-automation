package utilities

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

import java.io.File
import java.io.FileFilter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.LinkedHashSet
import java.util.List
import java.util.Set

import org.bson.Document

/**
 * Data Explorer verification keywords. Standalone class (does not extend TestRunner).
 * Verifies the Data Explorer study metadata view for one or more node types by comparing
 * the UI table to expected TSVs from the uploaded/released submission metadata.
 *
 * Uses: WebUIUtils (clickTab, verifyElementDisplayed), ValidationReport, findTestObject, DriverFactory.
 * Does not modify any existing keyword classes.
 */
class DataExplorer {

	private static final String REPORT_SECTION = "Data Explorer"
	private static final String NODE_TYPE_DROPDOWN_OR = "CRDC/AILocators/DataExplorer/StudyView/NodeType-Ddn"
	private static final String STUDY_VIEW_CONTAINER_OR = "CRDC/AILocators/DataExplorer/StudyView/Container"
	private static final String COLUMN_VISIBILITY_BTN_OR = "CRDC/AILocators/DataExplorer/StudyView/ColumnVisibility-Btn"
	private static final String SHOW_ALL_CHKBX_OR = "CRDC/AILocators/DataExplorer/StudyView/ShowAll-Chkbx"
	private static final int DEFAULT_TIMEOUT = 20
	private static final String METADATA_BASE = "InputFiles/CRDC/MetadataData"

	/**
	 * Verifies the Data Explorer metadata table for each given node type.
	 * Caller must already be on the Data Explorer study view (after opening a study from the list).
	 *
	 * For each node type: selects it in the Node Type dropdown, waits for the table,
	 * verifies structure (container, headers, rows), then loads the expected TSV from
	 * InputFiles/CRDC/MetadataData/&lt;tcName&gt;/ (or .../Metadata/) and compares row count
	 * and cell values. Results are reported via ValidationReport.
	 *
	 * @param tcName   Test case / folder name under InputFiles/CRDC/MetadataData (e.g. "CRDCDH-3026")
	 * @param nodeTypes One or more node types to verify (e.g. "program", "study")
	 */
	@Keyword
	static void verifyDataExplorerMetadataView(String tcName, String... nodeTypes) {
		if (nodeTypes == null || nodeTypes.length == 0) {
			KeywordUtil.logInfo("verifyDataExplorerMetadataView: no node types provided; skipping.")
			return
		}
		KeywordUtil.logInfo("--- Data Explorer verification for tcName=${tcName}, nodeTypes=${nodeTypes.toList()} ---")
		WebDriver driver = DriverFactory.getWebDriver()

		for (String nodeType : nodeTypes) {
			String node = nodeType?.trim()
			if (node == null || node.isEmpty()) continue

				// 1. Select node type in dropdown
				selectNodeType(driver, node)

			// 1b. Open column visibility and select Show All so all columns are visible
			WebUI.waitForElementClickable(findTestObject(COLUMN_VISIBILITY_BTN_OR), DEFAULT_TIMEOUT)
			WebUI.click(findTestObject(COLUMN_VISIBILITY_BTN_OR))
			WebUI.delay(1)
			WebUI.waitForElementClickable(findTestObject(SHOW_ALL_CHKBX_OR), DEFAULT_TIMEOUT)
			WebUI.check(findTestObject(SHOW_ALL_CHKBX_OR))
			WebUI.delay(1)

			// 2. Wait for table and verify structure
			WebUI.delay(2)
			boolean containerVisible = WebUI.waitForElementVisible(findTestObject(STUDY_VIEW_CONTAINER_OR), DEFAULT_TIMEOUT)
			ValidationReport.addValidationReportItem(REPORT_SECTION, "Node '${node}' – study view container", "visible", containerVisible ? "visible" : "not visible", containerVisible ? "Pass" : "Fail")
			if (!containerVisible) {
				KeywordUtil.markFailed("Data Explorer: study view container not visible for node '${node}'.")
				continue
			}

			List<String> uiHeaders = getTableHeaderNames(driver)
			int headerCount = uiHeaders != null ? uiHeaders.size() : 0
			ValidationReport.addValidationReportItem(REPORT_SECTION, "Node '${node}' – table headers", ">= 1", String.valueOf(headerCount), headerCount >= 1 ? "Pass" : "Fail")

			List<List<String>> actualRows = readTableRows(driver, uiHeaders)
			int actualRowCount = actualRows != null ? actualRows.size() : 0

			// 3. Load expected TSV and compare
			String expectedTsvPath = findExpectedTsvForNode(tcName, node)
			if (expectedTsvPath == null) {
				ValidationReport.addValidationReportItem(REPORT_SECTION, "Node '${node}' – expected TSV", "found", "not found under ${METADATA_BASE}/${tcName}", "Fail")
				KeywordUtil.logInfo("No expected TSV for node '${node}'; skipping content comparison.")
				continue
			}

			Path path = Paths.get(RunConfiguration.getProjectDir(), expectedTsvPath)
			if (!Files.exists(path)) {
				ValidationReport.addValidationReportItem(REPORT_SECTION, "Node '${node}' – expected file", path.toString(), "not found", "Fail")
				continue
			}

			List<String> expectedHeaderNames = new ArrayList<>()
			List<List<String>> expectedRows = parseExpectedTsv(path, expectedHeaderNames)
			// Sort expected by first column using case-insensitive string order to match UI table
			sortExpectedRowsByFirstColumn(expectedRows)
			int expectedRowCount = expectedRows.size()
			boolean rowCountMatch = (actualRowCount == expectedRowCount)
			ValidationReport.addValidationReportItem(REPORT_SECTION, "Node '${node}' – row count", String.valueOf(expectedRowCount), String.valueOf(actualRowCount), rowCountMatch ? "Pass" : "Fail")
			if (!rowCountMatch) {
				KeywordUtil.markFailed("Data Explorer node '${node}': expected ${expectedRowCount} rows, actual ${actualRowCount}.")
			}

			// 3b. Verify each expected column header is present in the UI table
			int[] expectedToUiIndex = mapExpectedHeadersToUiIndices(expectedHeaderNames, uiHeaders)
			boolean allHeadersMatch = true
			for (int c = 0; c < expectedHeaderNames.size(); c++) {
				String expHeader = expectedHeaderNames.get(c) != null ? expectedHeaderNames.get(c).trim() : ""
				int uiIdx = c < expectedToUiIndex.length ? expectedToUiIndex[c] : -1
				String actHeader = (uiIdx >= 0 && uiIdx < uiHeaders.size()) ? (uiHeaders.get(uiIdx) != null ? uiHeaders.get(uiIdx).trim() : "") : ""
				boolean found = uiIdx >= 0 && actHeader != null && !actHeader.isEmpty()
				if (!found) allHeadersMatch = false
				ValidationReport.addValidationReportItem(REPORT_SECTION, "Node '${node}' – column '${expHeader}'", "present in table", found ? actHeader : "missing", found ? "Pass" : "Fail")
			}
			if (!allHeadersMatch) {
				KeywordUtil.markFailed("Data Explorer node '${node}': one or more expected column headers missing in table. See validation report.")
			}

			// 4. Map expected columns to UI column indices and compare cell-by-cell by row index
			if (actualRows != null && expectedRows != null && actualRowCount == expectedRowCount && expectedRowCount > 0) {
				boolean anyFail = false
				for (int r = 0; r < expectedRowCount; r++) {
					List<String> expRow = expectedRows.get(r)
					List<String> actRow = actualRows.get(r)
					for (int c = 0; c < expRow.size(); c++) {
						String exp = expRow.get(c) != null ? expRow.get(c).trim() : ""
						int uiIdx = c < expectedToUiIndex.length ? expectedToUiIndex[c] : -1
						String act = (uiIdx >= 0 && uiIdx < actRow.size()) ? (actRow.get(uiIdx) != null ? actRow.get(uiIdx).trim() : "") : ""
						boolean match = exp.equals(act) || (act != null && act.contains(exp.replace("...", "").trim()))
						if (!match) anyFail = true
						ValidationReport.addValidationReportItem(REPORT_SECTION, "Node '${node}' row ${r + 1} col ${c + 1}", exp, act, match ? "Pass" : "Fail")
					}
				}
				if (anyFail) {
					KeywordUtil.markFailed("Data Explorer node '${node}': one or more cell mismatches. See validation report.")
				}
			}
		}
		KeywordUtil.logInfo("--- Data Explorer verification complete ---")
	}

	/**
	 * Clicks the Node Type dropdown and selects the option with the given text.
	 */
	private static void selectNodeType(WebDriver driver, String node) {
		WebUI.waitForElementClickable(findTestObject(NODE_TYPE_DROPDOWN_OR), DEFAULT_TIMEOUT)
		WebUI.click(findTestObject(NODE_TYPE_DROPDOWN_OR))
		WebUI.delay(1)
		String safeNode = node.replace("'", "''")
		WebElement option = driver.findElement(By.xpath("//li[normalize-space(text())='" + safeNode + "']"))
		option.click()
		WebUI.delay(1)
	}

	/**
	 * Returns header display names from the generic table (data-testid^='generic-table-header-*').
	 */
	private static List<String> getTableHeaderNames(WebDriver driver) {
		List<WebElement> headers = driver.findElements(By.cssSelector("[data-testid^='generic-table-header-']"))
		List<String> names = new ArrayList<>()
		for (WebElement h : headers) {
			String text = h.getText()?.replaceAll(/\s+/, ' ')?.trim()
			if (text != null && !text.isEmpty()) {
				names.add(text)
			} else {
				String testId = h.getAttribute("data-testid") ?: ""
				names.add(testId.replace("generic-table-header-", ""))
			}
		}
		return names
	}

	/**
	 * Reads all data rows from the generic table. Returns list of rows; each row is a list of cell values
	 * in the same order as headerNames. Uses aria-label on truncated-text-wrapper when present.
	 */
	private static List<List<String>> readTableRows(WebDriver driver, List<String> headerNames) {
		if (headerNames == null || headerNames.isEmpty()) return Collections.emptyList()
		JavascriptExecutor js = (JavascriptExecutor) driver
		String script = "var rows = document.querySelectorAll(\"[data-testid='generic-table-row']\"); var out = []; for (var i = 0; i < rows.length; i++) { var cells = rows[i].querySelectorAll('td'); var row = []; for (var j = 0; j < cells.length; j++) { var cell = cells[j]; var trunc = cell.querySelector(\"[data-testid='truncated-text-wrapper']\"); var val = ''; if (trunc) { var aria = trunc.getAttribute('aria-label'); val = (aria && aria.trim()) ? aria.trim() : (cell.innerText || '').trim(); } else { val = (cell.innerText || '').trim(); } row.push(val); } out.push(row); } return out;"
		List<List<String>> raw = (List<List<String>>) js.executeScript(script)
		List<List<String>> result = new ArrayList<>()
		if (raw == null) return result
		for (List<String> row : raw) {
			result.add(row != null ? new ArrayList<>(row) : new ArrayList<>())
		}
		return result
	}

	/**
	 * Finds the first TSV under InputFiles/CRDC/MetadataData/&lt;tcName&gt;/ or .../tcName/Metadata/
	 * whose derived node name equals nodeType. Node is derived from filename: strip .tsv, strip _vX.Y.Z, take part after first '_' or whole name.
	 */
	private static String findExpectedTsvForNode(String tcName, String nodeType) {
		String baseDir = RunConfiguration.getProjectDir()
		for (String sub : ["", "Metadata"]) {
			String dirPath = sub.isEmpty() ? "${METADATA_BASE}/${tcName}" : "${METADATA_BASE}/${tcName}/${sub}"
			File dir = new File(baseDir, dirPath)
			if (!dir.exists() || !dir.isDirectory()) continue
				File[] files = dir.listFiles({ File f -> f.isFile() && f.name.toLowerCase().endsWith(".tsv") } as FileFilter)
			if (files == null) continue
				for (File f : files) {
					String node = nodeFromFilename(f.name)
					if (node != null && node.equalsIgnoreCase(nodeType.trim())) {
						return dirPath + "/" + f.name
					}
				}
		}
		return null
	}

	/**
	 * Derives node name from TSV filename: e.g. program.tsv -> program, GC_program_v1.0.0.tsv -> program.
	 */
	private static String nodeFromFilename(String filename) {
		if (filename == null || !filename.endsWith(".tsv")) return null
		String base = filename.replaceFirst(~/\.[^.]+$/, "")
		base = base.replaceFirst(~/_v\d+\.\d+\.\d+$/, "")
		return base.contains("_") ? base.substring(base.indexOf("_") + 1) : base
	}

	/**
	 * Parses TSV: first line = headers (filter to names without '.'), remaining lines = data.
	 * Skips column 0 if it is 'type'. Fills expectedHeaderNames with the kept header names.
	 */
	private static List<List<String>> parseExpectedTsv(Path path, List<String> expectedHeaderNames) {
		List<List<String>> rows = new ArrayList<>()
		List<String> lines = Files.readAllLines(path)
		if (lines.isEmpty()) return rows
		String[] headerLine = lines.get(0).split("\t", -1)
		List<Integer> includedIndexes = new ArrayList<>()
		for (int j = 0; j < headerLine.length; j++) {
			String h = headerLine[j].trim()
			if (h.equalsIgnoreCase("type")) continue
				if (!h.contains(".")) {
					includedIndexes.add(j)
					expectedHeaderNames.add(h)
				}
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

	/**
	 * Sorts expected rows in-place by the first column (index 0) using case-insensitive
	 * string comparison. The UI table displays rows in this order (e.g. partsof1, partsof2,
	 * partsof3, partsof4, partsof5 and auto_sample1..auto_sample5).
	 */
	private static void sortExpectedRowsByFirstColumn(List<List<String>> expectedRows) {
		if (expectedRows == null || expectedRows.size() < 2) return
			Collections.sort(expectedRows, { List<String> a, List<String> b ->
				String va = (a != null && !a.isEmpty()) ? (a.get(0) != null ? a.get(0).trim() : "") : ""
				String vb = (b != null && !b.isEmpty()) ? (b.get(0) != null ? b.get(0).trim() : "") : ""
				return va.compareToIgnoreCase(vb)
			})
	}

	/**
	 * Maps expected header names to UI column indices. expectedHeaderNames and uiHeaders are in order.
	 * Returns an array where result[c] = UI column index for expected column c (or -1 if not found).
	 */
	private static int[] mapExpectedHeadersToUiIndices(List<String> expectedHeaderNames, List<String> uiHeaders) {
		int[] map = new int[expectedHeaderNames.size()]
		Arrays.fill(map, -1)
		for (int c = 0; c < expectedHeaderNames.size(); c++) {
			String exp = expectedHeaderNames.get(c)
			if (exp == null) continue
				String expNorm = exp.replace("_", " ").trim()
			for (int u = 0; u < uiHeaders.size(); u++) {
				String ui = uiHeaders.get(u)
				if (ui != null && (ui.equalsIgnoreCase(exp) || ui.replace("_", " ").trim().equalsIgnoreCase(expNorm))) {
					map[c] = u
					break
				}
			}
		}
		return map
	}

	/** Data Explorer list table: column index for Study Abbreviation (first column). */
	private static final int LIST_COL_STUDY_ABBREVIATION = 0
	/** Data Explorer list table: column index for Study Name (second column). */
	private static final int LIST_COL_STUDY_NAME = 1
	/** Data file path for MongoDB aggregation pipelines (Excel sheet "Pipelines" in MongoQueries.xlsx). Columns: key, collection, pipeline (JSON array). */
	private static final String DBPIPELINES_DATA_FILE = "Data Files/CRDC/DataSubmissions/DBPipelines"
	private static final String RELEASE_STUDIES_PIPELINE_KEY = "ReleaseStudies"

	/**
	 * Verifies that the Data Explorer list page table (Study Name and Study Abbreviation columns) matches exactly
	 * the records returned by the release aggregation. Pipeline is loaded from the Pipelines sheet (key "ReleaseStudies").
	 * Caller must already be on the Data Explorer list page. Uses DatabaseUtils for MongoDB (connect, aggregate, close).
	 * Compares set of (studyName, studyAbbreviation) pairs; count and content must match with no extra or missing rows.
	 */
	@Keyword
	static void verifyDataExplorerStudyListMatchesMongo() {
		KeywordUtil.logInfo("--- Data Explorer study list vs release aggregation verification ---")
		WebDriver driver = DriverFactory.getWebDriver()

		// 1. MongoDB: run aggregation from MongoQueries excel, Pipelines sheet (key ReleaseStudies), build expected set
		DatabaseUtils.connectFromGlobalVariable()
		List<Document> mongoDocs
		try {
			mongoDocs = DatabaseUtils.aggregateFromDataFile(DBPIPELINES_DATA_FILE, RELEASE_STUDIES_PIPELINE_KEY)
			if (mongoDocs == null) {
				KeywordUtil.markFailed("ReleaseStudies pipeline not found. Ensure MongoQueries.xlsx has a 'Pipelines' sheet with key 'ReleaseStudies', collection, and pipeline (JSON array).")
				return
			}
		} finally {
			DatabaseUtils.close()
		}

		// Raw MongoDB response (full document per record)
		KeywordUtil.logInfo("--- Raw MongoDB aggregation response (${mongoDocs.size()} document(s)) ---")
		for (int i = 0; i < mongoDocs.size(); i++) {
			Document doc = mongoDocs.get(i)
			KeywordUtil.logInfo("  [${i + 1}] " + doc.toJson())
		}

		Set<String> expectedPairs = new LinkedHashSet<>()
		List<String> mongoLogLines = []
		for (Document doc : mongoDocs) {
			String studyName = doc.get("studyName") != null ? doc.get("studyName").toString().trim() : ""
			String studyAbbreviation = doc.get("studyAbbreviation") != null ? doc.get("studyAbbreviation").toString().trim() : ""
			expectedPairs.add(studyName + "|" + studyAbbreviation)
			mongoLogLines.add("  studyAbbreviation=${studyAbbreviation}, studyName=${studyName}")
		}
		int expectedCount = expectedPairs.size()
		KeywordUtil.logInfo("MongoDB release aggregation – derived (studyAbbreviation, studyName) – ${expectedCount} record(s):")
		for (String line : mongoLogLines) {
			KeywordUtil.logInfo(line)
		}

		// 2. UI: get total, optionally increase rows per page, collect all rows (with pagination if needed)
		int total = WebUIUtils.getTableTotalEntryCount(15, null)
		if (total < 0) {
			ValidationReport.addValidationReportItem(REPORT_SECTION, "Data Explorer list – table total", ">= 0", "could not read", "Fail")
			KeywordUtil.markFailed("verifyDataExplorerStudyListMatchesReleaseAggregate: could not get table total from UI.")
			return
		}

		if (total > 20) {
			boolean changed = WebUIUtils.changeRowsPerPage(100)
			if (!changed && total > 50) {
				WebUIUtils.changeRowsPerPage(50)
			}
			WebUI.delay(2)
		}

		List<List<String>> allRows = new ArrayList<>()
		while (allRows.size() < total) {
			List<String> headers = getTableHeaderNames(driver)
			List<List<String>> pageRows = readTableRows(driver, headers)
			if (pageRows.isEmpty() && allRows.isEmpty()) {
				break
			}
			for (List<String> row : pageRows) {
				allRows.add(new ArrayList<>(row))
			}
			if (allRows.size() >= total) break
			List<WebElement> nextButtons = driver.findElements(By.cssSelector("button[aria-label='Go to next page']"))
			if (nextButtons.isEmpty() || !nextButtons.get(0).isEnabled()) {
				break
			}
			nextButtons.get(0).click()
			WebUI.delay(2)
		}

		// Raw UI table (headers + full row data)
		List<String> headers = getTableHeaderNames(driver)
		KeywordUtil.logInfo("--- Raw UI Data Explorer table – headers: ${headers} – ${allRows.size()} row(s) ---")
		for (int i = 0; i < allRows.size(); i++) {
			KeywordUtil.logInfo("  [${i + 1}] ${allRows.get(i)}")
		}

		Set<String> actualPairs = new LinkedHashSet<>()
		List<String> uiLogLines = []
		for (List<String> row : allRows) {
			String studyAbbreviation = (row.size() > LIST_COL_STUDY_ABBREVIATION && row.get(LIST_COL_STUDY_ABBREVIATION) != null) ? row.get(LIST_COL_STUDY_ABBREVIATION).trim() : ""
			String studyName = (row.size() > LIST_COL_STUDY_NAME && row.get(LIST_COL_STUDY_NAME) != null) ? row.get(LIST_COL_STUDY_NAME).trim() : ""
			actualPairs.add(studyName + "|" + studyAbbreviation)
			uiLogLines.add("  studyAbbreviation=${studyAbbreviation}, studyName=${studyName}")
		}
		KeywordUtil.logInfo("UI Data Explorer table – derived (studyAbbreviation, studyName) – ${allRows.size()} row(s):")
		for (String line : uiLogLines) {
			KeywordUtil.logInfo(line)
		}
		// 3. Compare count (UI table total vs Mongo aggregation result count)
		boolean countMatch = (expectedCount == total)
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Study list row count (Mongo vs UI)", String.valueOf(expectedCount), String.valueOf(total), countMatch ? "Pass" : "Fail")
		if (!countMatch) {
			KeywordUtil.markFailed("verifyDataExplorerStudyListMatchesReleaseAggregate: count mismatch. Expected ${expectedCount}, actual UI total ${total}.")
		}

		// 4. Compare sets
		Set<String> onlyInMongo = new LinkedHashSet<>(expectedPairs)
		onlyInMongo.removeAll(actualPairs)
		Set<String> onlyInUi = new LinkedHashSet<>(actualPairs)
		onlyInUi.removeAll(expectedPairs)
		boolean setsMatch = onlyInMongo.isEmpty() && onlyInUi.isEmpty()
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Study list matches release aggregation", "exact match", setsMatch ? "exact match" : "mismatch", setsMatch ? "Pass" : "Fail")
		if (!setsMatch) {
			if (!onlyInMongo.isEmpty()) {
				KeywordUtil.logInfo("Only in MongoDB (${onlyInMongo.size()}): " + onlyInMongo.take(10))
			}
			if (!onlyInUi.isEmpty()) {
				KeywordUtil.logInfo("Only in UI (${onlyInUi.size()}): " + onlyInUi.take(10))
			}
			KeywordUtil.markFailed("verifyDataExplorerStudyListMatchesReleaseAggregate: study list content does not match release aggregation. See report and log.")
		}

		KeywordUtil.logInfo("--- Data Explorer study list verification complete ---")
	}
}
