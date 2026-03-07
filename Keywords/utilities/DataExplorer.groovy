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
import java.util.LinkedHashMap
import java.util.List
import java.util.Map

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

			// 4. Map expected columns to UI column indices and compare cell-by-cell
			// Match rows by first-column value (primary key) so order doesn't matter
			if (actualRows != null && expectedRows != null && actualRowCount == expectedRowCount && expectedRowCount > 0) {
				int[] expectedToUiIndex = mapExpectedHeadersToUiIndices(expectedHeaderNames, uiHeaders)
				Map<String, List<String>> expectedByKey = new LinkedHashMap<>()
				for (List<String> row : expectedRows) {
					String key = (row != null && !row.isEmpty() && row.get(0) != null) ? row.get(0).trim() : ""
					expectedByKey.put(key, row)
				}
				boolean anyFail = false
				for (int r = 0; r < actualRowCount; r++) {
					List<String> actRow = actualRows.get(r)
					String actKey = (actRow != null && !actRow.isEmpty() && actRow.get(0) != null) ? actRow.get(0).trim() : ""
					List<String> expRow = expectedByKey.get(actKey)
					if (expRow == null) {
						ValidationReport.addValidationReportItem(REPORT_SECTION, "Node '${node}' row ${r + 1} (key=${actKey})", "expected row with this key", "no matching expected row", "Fail")
						anyFail = true
						continue
					}
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
}
