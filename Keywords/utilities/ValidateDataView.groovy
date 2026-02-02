package utilities

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.configuration.RunConfiguration
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import java.io.File
import groovy.json.JsonSlurper
import internal.GlobalVariable

/**
 * Validate data view table against TSV files (user-uploaded vs UI-exported).
 * @param folderPath - Path to folder containing TSV files (relative to project root or absolute)
 * @param dropdownXPath - XPath to node type dropdown
 * @param tableXPath - XPath to data view table (optional)
 * @param sortByColumn - Column name or JSON mapping per node type (optional)
 * @param waitTime - Wait time in ms for status updates (default: 3000)
 * @return Map with success (boolean), nodeResults (List), mismatches (List)
 */
class ValidateDataView {

	@Keyword
	static Map<String, Object> validate(String folderPath, String dropdownXPath,
		String tableXPath = null, String sortByColumn = null, int waitTime = 3000) {
		println("🔍 ValidateDataView: Validating data view for folder '${folderPath}'")

		String projectDir = RunConfiguration.getProjectDir()
		File folder = folderPath.startsWith('/') ? new File(folderPath) : new File(projectDir, folderPath)
		if (!folder.exists() || !folder.isDirectory()) {
			throw new Exception("Folder not found: ${folder.absolutePath}")
		}

		def tsvFiles = folder.listFiles()?.findAll { it.isFile() && it.name.toLowerCase().endsWith('.tsv') }?.sort { it.name } ?: []
		if (!tsvFiles || tsvFiles.isEmpty()) {
			throw new Exception("No TSV files found in: ${folder.absolutePath}")
		}
		println("📁 Found ${tsvFiles.size()} TSV file(s): ${tsvFiles.collect { it.name }.join(', ')}")

		List<String> nodeTypes = []
		for (File tsvFile : tsvFiles) {
			String basename = tsvFile.name.replace('.tsv', '').toLowerCase()
			def versionMatch = basename =~ /^(.+?)_v\d+\.\d+\.\d+$/
			if (versionMatch) {
				String beforeVersion = versionMatch[0][1]
				int lastUnderscore = beforeVersion.lastIndexOf('_')
				nodeTypes.add(lastUnderscore >= 0 ? beforeVersion.substring(lastUnderscore + 1) : beforeVersion)
			} else {
				def match = basename =~ /^[^_]+_([^_]+)_/
				if (match) {
					nodeTypes.add(match[0][1])
				} else {
					def matchNoTrailing = basename =~ /^[^_]+_([^_]+)$/
					if (matchNoTrailing) {
						nodeTypes.add(matchNoTrailing[0][1])
					} else {
						int firstUnderscore = basename.indexOf('_')
						int lastUnderscore = basename.lastIndexOf('_')
						if (firstUnderscore >= 0 && lastUnderscore > firstUnderscore) {
							nodeTypes.add(basename.substring(firstUnderscore + 1, lastUnderscore))
						} else if (firstUnderscore >= 0) {
							nodeTypes.add(basename.substring(firstUnderscore + 1))
						} else {
							nodeTypes.add(basename)
						}
					}
				}
			}
		}

		try {
			switchToTab('Data View')
			println("✅ Switched to Data View tab")
		} catch (Exception tabError) {
			println("⚠️  Could not switch to Data View tab (may already be active): ${tabError.message}")
		}
		WebUI.delay(1)

		println("🖱️  Clicking dropdown: ${dropdownXPath}")
		TestObject dropdownObj = FindObjectByXPath.findObject(dropdownXPath)
		WebUI.waitForElementPresent(dropdownObj, 10)
		def driver = DriverFactory.getWebDriver()
		WebElement dropdownElement = driver.findElement(By.xpath(dropdownXPath))
		if (dropdownElement.getAttribute('aria-expanded') != 'true') {
			WebUI.click(dropdownObj)
			WebUI.delay(0.5)
		} else {
			println("✅ Dropdown is already open")
		}

		List<Map<String, Object>> nodeResults = []
		List<Map<String, Object>> allMismatches = []
		String validationMethod = 'ui'
		try {
			if (GlobalVariable.hasProperty('validationMethod') && GlobalVariable.validationMethod) {
				validationMethod = GlobalVariable.validationMethod
			}
		} catch (Exception e) { /* use default */ }

		for (int i = 0; i < nodeTypes.size(); i++) {
			String nodeType = nodeTypes[i]
			File tsvFile = tsvFiles[i]
			println("\n📊 Validating node type: ${nodeType} (file: ${tsvFile.name})")

			try {
				String optionXPath = "//li[contains(@role, 'option') and contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '${nodeType.toLowerCase()}')]"
				TestObject optionObj = FindObjectByXPath.findObject(optionXPath)
				WebUI.waitForElementPresent(optionObj, 5)
				WebUI.click(optionObj)
				WebUI.delay(1)
				waitForTableStable(tableXPath)
				WebUI.delay(waitTime / 1000)
				println("⏱️  Waited ${waitTime}ms for status values to update")

				Map<String, Object> uiTable
				if (validationMethod == 'download') {
					println("📥 Using download method for validation")
					String downloadButtonXPath = '//button[@data-testid="export-node-data-button"]'
					Map<String, Object> downloadResult = DownloadTableData.downloadAndRead(downloadButtonXPath, 30)
					if (!downloadResult['success']) {
						throw new Exception("Download failed: ${downloadResult['error']}")
					}
					String downloadedFilePath = downloadResult['filePath']
					uiTable = DownloadTableData.readTSVFile(downloadedFilePath)
					println("✅ Read downloaded file - ${uiTable['headers'].size()} columns, ${uiTable['rows'].size()} rows")
				} else {
					println("🖥️  Using UI table method for validation")
					uiTable = readUITable(tableXPath)
					println("✅ Read UI table: ${uiTable['headers'].size()} columns, ${uiTable['rows'].size()} rows")
				}

				Map<String, Object> tsvData = readTSV(tsvFile)
				println("✅ Read TSV file: ${tsvData['headers'].size()} columns, ${tsvData['rows'].size()} rows")

				String sortColumnForNode = getSortColumn(sortByColumn, nodeType, tsvData['headers'] as List<String>)
				if (sortColumnForNode) {
					println("🔍 Using sort column '${sortColumnForNode}' - sorting both before comparison")
					tsvData = sortTable(tsvData, sortColumnForNode)
					uiTable = sortTable(uiTable, sortColumnForNode)
				} else {
					println("⚠️  No sort column detected - position-based row matching")
				}

				Map<String, Object> comparisonResult = compareTables(uiTable, tsvData, nodeType, validationMethod, sortColumnForNode)
				List<Map<String, Object>> mismatches = comparisonResult['mismatches'] ?: []
				List<Map<String, Object>> allResults = comparisonResult['allResults'] ?: []

				if (mismatches.size() > 0) {
					println("❌ Node type '${nodeType}' validation failed: ${mismatches.size()} mismatch(es)")
					nodeResults.add([ nodeType: nodeType, success: false, mismatches: mismatches, allResults: allResults ])
					allMismatches.addAll(mismatches)
				} else {
					println("✅ Node type '${nodeType}' validation passed: ${allResults.size()} comparison(s)")
					nodeResults.add([ nodeType: nodeType, success: true, allResults: allResults ])
				}
			} catch (Exception error) {
				println("❌ Error validating node type '${nodeType}': ${error.message}")
				nodeResults.add([ nodeType: nodeType, success: false, error: error.message ])
			}

			if (i < nodeTypes.size() - 1) {
				WebUI.click(dropdownObj)
				WebUI.delay(0.5)
			}
		}

		boolean allSuccess = nodeResults.every { it['success'] == true }
		int successCount = nodeResults.count { it['success'] == true }
		if (allSuccess) {
			println("✅ ValidateDataView passed: All ${nodeResults.size()} node type(s) validated successfully")
		} else {
			println("❌ ValidateDataView failed: ${successCount}/${nodeResults.size()} node type(s) passed")
		}

		return [ success: allSuccess, nodeResults: nodeResults, mismatches: allMismatches ]
	}

	private static void switchToTab(String tabName) {
		try {
			String xpath = "//*[@role='tab' and contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '${tabName.toLowerCase()}')]"
			TestObject tabObj = FindObjectByXPath.findObject(xpath)
			WebUI.click(tabObj)
			WebUI.delay(1)
		} catch (Exception e) {
			String xpath = "//button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '${tabName.toLowerCase()}')]"
			TestObject tabObj = FindObjectByXPath.findObject(xpath)
			WebUI.click(tabObj)
			WebUI.delay(1)
		}
	}

	private static void waitForTableStable(String tableXPath) {
		String xpath = tableXPath ?: "//table[@data-testid='generic-table']"
		TestObject tableObj = FindObjectByXPath.findObject(xpath)
		WebUI.waitForElementPresent(tableObj, 10)
		WebUI.delay(0.5)
	}

	private static Map<String, Object> readUITable(String tableXPath) {
		def driver = DriverFactory.getWebDriver()
		WebElement table = tableXPath ? driver.findElement(By.xpath(tableXPath)) : driver.findElement(By.xpath("//table[@data-testid='generic-table']"))
		List<String> headers = []
		table.findElements(By.xpath(".//thead//th | .//thead//td")).each { header ->
			String text = header.getText()?.trim()
			if (text) headers.add(text)
		}
		def rows = []
		table.findElements(By.xpath(".//tbody//tr")).each { row ->
			def rowData = []
			row.findElements(By.xpath(".//td")).each { cell ->
				String cellText = cell.getText()?.trim() ?: ''
				if (cellText.contains('...') || (cellText.length() > 0 && cellText.length() < 15)) {
					String titleAttr = cell.getAttribute('title') ?: ''
					if (titleAttr && titleAttr.length() > cellText.length() && !titleAttr.contains('...')) {
						cellText = titleAttr.trim()
					} else {
						String dataValue = cell.getAttribute('data-value') ?: ''
						if (dataValue && dataValue.length() > cellText.length() && !dataValue.contains('...')) {
							cellText = dataValue.trim()
						} else {
							try {
								String fullText = driver.executeScript('return arguments[0].textContent || arguments[0].innerText || \'\';', cell)
								if (fullText && fullText.length() > cellText.length() && !fullText.contains('...')) {
									cellText = fullText.trim()
								}
							} catch (Exception e) { }
						}
					}
				}
				rowData.add(cellText)
			}
			if (rowData.size() > 0) rows.add(rowData)
		}
		return [ headers: headers, rows: rows ]
	}

	private static Map<String, Object> readTSV(File tsvFile) {
		def headers = []
		def rows = []
		tsvFile.eachLine { line, lineNum ->
			String[] parts = line.split('\t')
			if (lineNum == 1) {
				headers = parts.collect { it.trim() }
			} else {
				rows.add(parts.collect { it.trim() })
			}
		}
		return [ headers: headers, rows: rows ]
	}

	private static String getSortColumn(String sortByColumn, String nodeType, List<String> tsvHeaders) {
		if (!sortByColumn) {
			for (String header : tsvHeaders) {
				String lower = header.toLowerCase()
				if (lower.endsWith('_id') || lower.endsWith('id') || lower == 'id') return header
			}
			return null
		}
		try {
			def sortMapping = new JsonSlurper().parseText(sortByColumn)
			if (sortMapping instanceof Map) {
				String mapped = sortMapping[nodeType] ?: sortMapping[nodeType.toLowerCase()] ?: sortMapping[nodeType.replace('_', '')]
				if (mapped) {
					println("📋 Using node-specific sort column for '${nodeType}': ${mapped}")
					return mapped
				}
			}
		} catch (Exception e) { }
		return sortByColumn
	}

	private static Map<String, Object> sortTable(Map<String, Object> table, String sortColumn) {
		List<String> headers = table['headers'] as List<String>
		int sortColIdx = findColumnIndex(headers, sortColumn)
		if (sortColIdx < 0) {
			println("⚠️  Sort column '${sortColumn}' not found in headers")
			return table
		}
		def sortedRows = new ArrayList<>(table['rows'])
		sortedRows.sort { row1, row2 ->
			String val1 = (row1[sortColIdx] ?: '').trim().toLowerCase()
			String val2 = (row2[sortColIdx] ?: '').trim().toLowerCase()
			return val1.compareTo(val2)
		}
		return [ headers: headers, rows: sortedRows ]
	}

	private static int findColumnIndex(List<String> headers, String columnName) {
		String colLower = columnName.toLowerCase().trim()
		for (int i = 0; i < headers.size(); i++) {
			if (headers[i].toLowerCase().trim() == colLower) return i
		}
		for (int i = 0; i < headers.size(); i++) {
			if (headers[i].toLowerCase().contains(colLower)) return i
		}
		return -1
	}

	private static Map<String, Object> compareTables(Map<String, Object> uiTable, Map<String, Object> tsvData,
		String nodeType, String validationMethod = 'ui', String sortColumn = null) {
		def mismatches = []
		def allResults = []
		def uiHeaders = uiTable['headers']
		def tsvHeaders = tsvData['headers']
		def uiRows = uiTable['rows']
		def tsvRows = tsvData['rows']

		Map<String, Integer> expectedColumnMap = [:]
		Map<String, String> expectedToDownloadedMap = [:]
		for (int i = 0; i < tsvHeaders.size(); i++) {
			String tsvHeader = tsvHeaders[i]
			String tsvHeaderLower = tsvHeader.toLowerCase().trim()
			if (tsvHeaderLower.startsWith('expected_')) {
				String expectedSuffix = tsvHeader.substring(9)
				expectedColumnMap[tsvHeader] = i
				String expectedSuffixLower = expectedSuffix.toLowerCase()
				int downloadedColIndex = -1
				for (int j = 0; j < uiHeaders.size(); j++) {
					String uiHeaderLower = uiHeaders[j].toLowerCase().trim()
					if (uiHeaderLower.startsWith('expected_')) continue
					if (uiHeaderLower == expectedSuffixLower) { downloadedColIndex = j; break }
				}
				if (downloadedColIndex < 0) {
					for (int j = 0; j < uiHeaders.size(); j++) {
						String uiHeaderLower = uiHeaders[j].toLowerCase().trim()
						if (uiHeaderLower.startsWith('expected_')) continue
						if (uiHeaderLower.contains(expectedSuffixLower)) { downloadedColIndex = j; break }
					}
				}
				if (downloadedColIndex >= 0) {
					expectedToDownloadedMap[tsvHeader] = uiHeaders[downloadedColIndex]
				}
			}
		}

		int tsvRegularColumns = tsvHeaders.count { String h -> h.toLowerCase().trim() != 'type' && !h.toLowerCase().trim().startsWith('expected_') }
		int uiRegularColumns = uiHeaders.count { String h -> h.toLowerCase().trim() != 'type' && !h.toLowerCase().trim().startsWith('expected_') }

		if (validationMethod == 'download' && expectedColumnMap.size() > 0) {
			int mappedExpectedCount = expectedToDownloadedMap.size()
			if (uiRegularColumns != tsvRegularColumns + mappedExpectedCount) {
				mismatches.add([ row: 0, column: 'Header Count', expected: String.valueOf(tsvRegularColumns + mappedExpectedCount), actual: String.valueOf(uiRegularColumns), matchType: 'exact', nodeType: nodeType ])
			}
		} else {
			if (uiRegularColumns != tsvRegularColumns) {
				mismatches.add([ row: 0, column: 'Header Count', expected: String.valueOf(tsvRegularColumns), actual: String.valueOf(uiRegularColumns), matchType: 'exact', nodeType: nodeType ])
			}
		}

		if (uiRows.size() != tsvRows.size()) {
			mismatches.add([ row: 0, column: 'Row Count', expected: String.valueOf(tsvRows.size()), actual: String.valueOf(uiRows.size()), matchType: 'exact', nodeType: nodeType ])
		}

		int expectedRowCount = tsvRows.size()
		boolean headerCountsOk = (validationMethod == 'download' && expectedColumnMap.size() > 0) ?
			(uiRegularColumns == tsvRegularColumns + expectedToDownloadedMap.size()) : (uiRegularColumns == tsvRegularColumns)

		if (uiRows.size() == expectedRowCount) {
			List<Integer> headerMap = []
			for (String tsvHeader : tsvHeaders) {
				String tsvHeaderLower = tsvHeader.toLowerCase().trim()
				if (tsvHeaderLower == 'type') { headerMap.add(-1); continue }
				if (tsvHeaderLower.startsWith('expected_')) { headerMap.add(-2); continue }
				String mappedHeader = (tsvHeaderLower == 'automation_status') ? 'Status' : tsvHeader
				headerMap.add(findColumnIndex(uiHeaders, mappedHeader))
			}

			int maxRows = Math.min(tsvRows.size(), uiRows.size())
			for (int rowIdx = 0; rowIdx < maxRows; rowIdx++) {
				def tsvRow = tsvRows[rowIdx]
				def uiRow = uiRows[rowIdx]

				if (headerCountsOk) {
					for (int colIdx = 0; colIdx < tsvHeaders.size(); colIdx++) {
						int uiColIdx = headerMap[colIdx]
						String tsvHeaderLower = tsvHeaders[colIdx].toLowerCase().trim()
						if (tsvHeaderLower == 'type' || tsvHeaderLower.startsWith('expected_')) continue
						if (uiColIdx >= 0 && uiColIdx < uiRow.size()) {
							String expected = (tsvRow[colIdx] ?: '').trim()
							String actual = (uiRow[uiColIdx] ?: '').trim()
							if (actual.endsWith('...')) {
								String actualWithoutEllipsis = actual.replaceAll(/\\.\\.\\.$/, '').trim()
								if (expected.startsWith(actualWithoutEllipsis)) continue
							}
							boolean isMatch = (expected == actual)
							Map<String, Object> result = [ row: rowIdx + 1, column: tsvHeaders[colIdx], expected: expected, actual: actual, matchType: 'exact', nodeType: nodeType, result: isMatch ? 'PASS' : 'FAIL' ]
							allResults.add(result)
							if (!isMatch) mismatches.add(result)
						}
					}
				}

				expectedColumnMap.each { expectedTsvCol, expectedTsvColIdx ->
					String downloadedColName = expectedToDownloadedMap[expectedTsvCol]
					if (downloadedColName) {
						int downloadedColIdx = findColumnIndex(uiHeaders, downloadedColName)
						if (downloadedColIdx >= 0 && downloadedColIdx < uiRow.size()) {
							String expectedValue = (tsvRow[expectedTsvColIdx] ?: '').trim()
							String actualValue = (uiRow[downloadedColIdx] ?: '').trim()
							boolean isMatch = (expectedValue == actualValue)
							Map<String, Object> result = [ row: rowIdx + 1, column: expectedTsvCol, expected: expectedValue, actual: actualValue, matchType: 'exact', nodeType: nodeType, note: "Mapped from '${downloadedColName}'", result: isMatch ? 'PASS' : 'FAIL' ]
							allResults.add(result)
							if (!isMatch) mismatches.add(result)
						}
					}
				}
			}

			if (tsvRows.size() > uiRows.size()) {
				for (int extraRowIdx = uiRows.size(); extraRowIdx < tsvRows.size(); extraRowIdx++) {
					def result = [ row: extraRowIdx + 1, column: 'Row Count', expected: 'Row exists in TSV', actual: 'Row missing in UI/downloaded', matchType: 'exact', nodeType: nodeType, result: 'FAIL' ]
					mismatches.add(result)
					allResults.add(result)
				}
			} else if (uiRows.size() > tsvRows.size()) {
				for (int extraRowIdx = tsvRows.size(); extraRowIdx < uiRows.size(); extraRowIdx++) {
					def result = [ row: extraRowIdx + 1, column: 'Row Count', expected: 'Row exists in TSV', actual: 'Extra row in UI/downloaded', matchType: 'exact', nodeType: nodeType, result: 'FAIL' ]
					mismatches.add(result)
					allResults.add(result)
				}
			}
		}

		return [ mismatches: mismatches, allResults: allResults ]
	}
}
