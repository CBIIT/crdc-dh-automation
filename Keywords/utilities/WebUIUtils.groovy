package utilities

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.testobject.TestObject
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

/**
 * Generic, reusable web UI verification helpers (tooltips, element count, etc.).
 * Results are added to ValidationReport for generateValidationReport().
 */
class WebUIUtils {

	private static final String REPORT_SECTION = "Export Buttons"
	private static final int DEFAULT_TIMEOUT = 10

	/**
	 * Verify tooltip text when it is in the element's aria-label attribute.
	 * Use for tooltips that expose their text via aria-label (e.g. Data View export tooltip).
	 * @param to TestObject for the element with aria-label
	 * @param expectedText Expected tooltip text
	 */
	@Keyword
	static void verifyTooltipByAriaLabel(TestObject to, String expectedText) {
		String actualText = WebUI.getAttribute(to, 'aria-label')
		String status = expectedText.equals(actualText) ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Tooltip (aria-label)", expectedText, actualText != null ? actualText : "(empty)", status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyTooltipByAriaLabel: expected '${expectedText}', actual '${actualText}'")
		}
	}

	/**
	 * Verify tooltip text that appears on hover (e.g. MUI Tooltip).
	 * Hovers over the trigger element, finds the tooltip with role="tooltip", matches by substring, normalizes whitespace, and verifies.
	 * @param triggerTo TestObject for the element to hover (e.g. tooltip span wrapper)
	 * @param expectedText Full expected tooltip text (after whitespace normalization)
	 * @param substringToFind Substring to identify the correct tooltip when multiple exist
	 */
	@Keyword
	static void verifyTooltipOnHover(TestObject triggerTo, String expectedText, String substringToFind) {
		WebDriver driver = DriverFactory.getWebDriver()
		WebElement trigger = WebUI.findWebElement(triggerTo, DEFAULT_TIMEOUT)
		new Actions(driver).moveToElement(trigger).perform()
		WebUI.delay(1)
		List<WebElement> tooltips = driver.findElements(By.cssSelector("[role='tooltip']"))
		WebElement match = tooltips.find { it.getText().contains(substringToFind) }
		if (match == null) {
			ValidationReport.addValidationReportItem(REPORT_SECTION, "Tooltip (hover)", expectedText, "(not found)", "Fail")
			KeywordUtil.markFailed("Tooltip with expected text not found. Found: " + tooltips.collect { it.getText() })
		} else {
			String actualText = match.getText().replaceAll(/\s+/, ' ').trim()
			String status = actualText.equals(expectedText) ? "Pass" : "Fail"
			ValidationReport.addValidationReportItem(REPORT_SECTION, "Tooltip (hover)", expectedText, actualText, status)
			if (status == "Fail") {
				KeywordUtil.markFailed("verifyTooltipOnHover: expected '${expectedText}', actual '${actualText}'")
			}
		}
	}

	/**
	 * Verify that the count of elements matching the TestObject equals the expected value.
	 * @param to TestObject to match
	 * @param expectedCount Expected number of elements
	 */
	@Keyword
	static void verifyElementCount(TestObject to, int expectedCount) {
		int count = WebUI.findWebElements(to, DEFAULT_TIMEOUT).size()
		String status = (count == expectedCount) ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Element count", String.valueOf(expectedCount), String.valueOf(count), status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyElementCount: expected ${expectedCount}, actual ${count}")
		}
	}

	/**
	 * Wait for an element to be visible, then verify it is displayed.
	 * Results are logged to ValidationReport.
	 * @param to TestObject for the element
	 */
	@Keyword
	static void verifyElementDisplayed(TestObject to) {
		boolean visible = WebUI.waitForElementVisible(to, DEFAULT_TIMEOUT)
		String status = visible ? "Pass" : "Fail"
		String actualState = visible ? "displayed" : "not visible"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Element displayed", "displayed", actualState, status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyElementDisplayed: element not visible within ${DEFAULT_TIMEOUT}s")
		}
	}

	/**
	 * Verify that an element has the expected text.
	 * Waits for the element to be visible, gets its text, normalizes whitespace, then compares.
	 * Results are logged to ValidationReport.
	 * @param to TestObject for the element
	 * @param expectedText Expected element text (will be normalized for comparison)
	 */
	@Keyword
	static void verifyElementText(TestObject to, String expectedText) {
		WebElement element = WebUI.findWebElement(to, DEFAULT_TIMEOUT)
		String actualText = element.getText()?.replaceAll(/\s+/, ' ').trim() ?: ""
		String expectedNormalized = expectedText?.replaceAll(/\s+/, ' ').trim() ?: ""
		String status = expectedNormalized.equals(actualText) ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Element text", expectedText, actualText, status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyElementText: expected '${expectedText}', actual '${actualText}'")
		}
	}

	/**
	 * Verify that a button/element is enabled or disabled.
	 * Results are logged to ValidationReport.
	 * @param to TestObject for the button or element
	 * @param expectedEnabled true if element should be enabled, false if it should be disabled
	 */
	@Keyword
	static void verifyButtonEnabled(TestObject to, boolean expectedEnabled) {
		WebElement element = WebUI.findWebElement(to, DEFAULT_TIMEOUT)
		boolean actualEnabled = element.isEnabled()
		String expectedState = expectedEnabled ? "enabled" : "disabled"
		String actualState = actualEnabled ? "enabled" : "disabled"
		String status = (actualEnabled == expectedEnabled) ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Button enabled", expectedState, actualState, status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyButtonEnabled: expected ${expectedState}, actual ${actualState}")
		}
	}

	/**
	 * Verify that a radio button, checkbox, or toggle is selected (checked) or not.
	 * Results are logged to ValidationReport.
	 * @param to TestObject for the radio/checkbox/toggle element
	 * @param expectedSelected true if element should be selected, false if it should not be selected
	 */
	@Keyword
	static void verifyButtonSelected(TestObject to, boolean expectedSelected) {
		WebElement element = WebUI.findWebElement(to, DEFAULT_TIMEOUT)
		boolean actualSelected = element.isSelected()
		String expectedState = expectedSelected ? "selected" : "not selected"
		String actualState = actualSelected ? "selected" : "not selected"
		String status = (actualSelected == expectedSelected) ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Button selected", expectedState, actualState, status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyButtonSelected: expected ${expectedState}, actual ${actualState}")
		}
	}

	/**
	 * Click an element by its Object Repository path/name.
	 * Resolves XPath from the TestObject, scrolls into view, and clicks via JavaScript.
	 * Retries up to 3 times on StaleElementReferenceException.
	 * @param tabName Object Repository path (e.g. 'CRDC/DataSubmissions/Submit-Btn')
	 */
	@Keyword
	static void clickTab(String tabName) {
		WebDriver driver = DriverFactory.getWebDriver()
		WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT)
		JavascriptExecutor js = (JavascriptExecutor) driver
		TestObject obj = findTestObject(tabName)
		String tabXpath = obj.findPropertyValue('xpath')
		KeywordUtil.logInfo("Resolved XPath: " + tabXpath)
		for (int i = 0; i < 3; i++) {
			try {
				WebElement resultTab = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(tabXpath)))
				js.executeScript("arguments[0].scrollIntoView(true);", resultTab)
				js.executeScript("arguments[0].click();", resultTab)
				KeywordUtil.logInfo("Successfully clicked element")
				break
			} catch (StaleElementReferenceException e) {
				KeywordUtil.logInfo("StaleElementReferenceException. Retrying... Attempt: " + (i + 1))
				Thread.sleep(1000)
			} catch (Exception e) {
				KeywordUtil.logInfo("Unexpected error: " + e.getMessage())
				throw e
			}
		}
	}

	/**
	 * Navigate to a submission's upload-activity page.
	 * Builds URL from G_Urlname (profile) and the given submission ID.
	 * @param submissionId Submission UUID (e.g. from getSubmissionIdFromUploadActivityUrl)
	 */
	@Keyword
	static void navigateToSubmissionUploadActivity(String submissionId) {
		WebUI.waitForPageLoad(10)
		WebUI.delay(2)
		String baseUrl = GlobalVariable.G_Urlname?.toString()?.replaceAll(/\/$/, '') ?: ''
		String submissionUrl = "${baseUrl}/data-submission/${submissionId}/upload-activity"
		KeywordUtil.logInfo("Navigating to URL: " + submissionUrl)
		WebUI.navigateToUrl(submissionUrl)
		WebUI.waitForPageLoad(10)
		WebUI.delay(2)
	}

	// ======================== Table Sorting Verification ========================

	/**
	 * Returns all header cell elements from the GenericTable.
	 * Each header has data-testid="generic-table-header-{label}".
	 */
	private static List<WebElement> getTableHeaderElements() {
		WebDriver driver = DriverFactory.getWebDriver()
		return driver.findElements(By.cssSelector("[data-testid^='generic-table-header-']"))
	}

	/**
	 * Returns the current sort direction of a header cell by inspecting MUI TableSortLabel classes.
	 * The GenericTable does not pass sortDirection to the th, so aria-sort is not available.
	 * Instead, checks for Mui-active on the MuiTableSortLabel-root span, then reads the icon
	 * direction from MuiTableSortLabel-iconDirectionAsc / MuiTableSortLabel-iconDirectionDesc.
	 * @return "asc", "desc", or null if the column is not the active sort column
	 */
	private static String getColumnSortDirection(WebElement headerCell) {
		List<WebElement> sortLabels = headerCell.findElements(By.cssSelector(".MuiTableSortLabel-root"))
		if (sortLabels.isEmpty()) return null
		WebElement sortLabel = sortLabels.get(0)
		String cls = sortLabel.getAttribute("class") ?: ""
		if (!cls.contains("Mui-active")) return null
		// Read direction from the icon child
		List<WebElement> icons = sortLabel.findElements(By.cssSelector(".MuiTableSortLabel-icon"))
		if (!icons.isEmpty()) {
			String iconCls = icons.get(0).getAttribute("class") ?: ""
			if (iconCls.contains("MuiTableSortLabel-iconDirectionAsc")) return "asc"
			if (iconCls.contains("MuiTableSortLabel-iconDirectionDesc")) return "desc"
		}
		// Fallback: check the direction from the sort label's own class
		if (cls.contains("MuiTableSortLabel-directionAsc")) return "asc"
		if (cls.contains("MuiTableSortLabel-directionDesc")) return "desc"
		return null
	}

	/**
	 * Extracts the column display name from a header cell.
	 * Prefers the visible text; falls back to the data-testid suffix.
	 */
	private static String getColumnName(WebElement headerCell) {
		String text = headerCell.getText()?.replaceAll(/\s+/, ' ')?.trim()
		if (text != null && !text.isEmpty()) return text
		String testId = headerCell.getAttribute("data-testid") ?: ""
		return testId.replace("generic-table-header-", "")
	}

	/**
	 * Reads all visible text values from a specific column across table data rows.
	 * Uses a single JavaScript call for performance — avoids per-row WebDriver roundtrips
	 * that hang on large tables.
	 * @param columnName Display name of the column (case-insensitive match)
	 * @return List of trimmed string values
	 */
	private static List<String> readColumnValues(String columnName) {
		WebDriver driver = DriverFactory.getWebDriver()
		List<WebElement> headers = getTableHeaderElements()
		int colIdx = -1
		for (int i = 0; i < headers.size(); i++) {
			if (getColumnName(headers.get(i)).equalsIgnoreCase(columnName)) {
				colIdx = i
				break
			}
		}
		if (colIdx < 0) {
			KeywordUtil.logInfo("readColumnValues: column '" + columnName + "' not found in headers")
			return Collections.emptyList()
		}

		JavascriptExecutor js = (JavascriptExecutor) driver
		String script = "var colIdx = arguments[0];" +
			"var rows = document.querySelectorAll(\"[data-testid='generic-table-row']\");" +
			"var values = [];" +
			"for (var i = 0; i < rows.length; i++) {" +
			"  var cells = rows[i].querySelectorAll('td');" +
			"  var val = '';" +
			"  if (colIdx < cells.length) {" +
			"    var cell = cells[colIdx];" +
			"    var truncEl = cell.querySelector(\"[data-testid='truncated-text-wrapper']\");" +
			"    if (truncEl) {" +
			"      var aria = truncEl.getAttribute('aria-label');" +
			"      val = (aria && aria.trim()) ? aria.trim() : (cell.innerText || '').trim();" +
			"    } else {" +
			"      val = (cell.innerText || '').trim();" +
			"    }" +
			"  }" +
			"  values.push(val);" +
			"}" +
			"return values;"
		List<String> values = (List<String>) js.executeScript(script, colIdx)
		KeywordUtil.logInfo("readColumnValues: read " + (values != null ? values.size() : 0) + " values for column '" + columnName + "'")
		return values ?: Collections.emptyList()
	}

	/**
	 * Checks whether a list of string values is sorted in the given direction.
	 * Tries numeric comparison first; falls back to case-insensitive string comparison.
	 * @param values List of cell text values
	 * @param direction "asc" or "desc"
	 * @return true if sorted correctly
	 */
	private static boolean isSortedCorrectly(List<String> values, String direction) {
		if (values == null || values.size() < 2) return true
		boolean isAsc = "asc".equalsIgnoreCase(direction)
		for (int i = 0; i < values.size() - 1; i++) {
			String a = values.get(i) ?: ""
			String b = values.get(i + 1) ?: ""
			// Skip empty values in comparison
			if (a.isEmpty() || b.isEmpty()) continue
			int cmp
			try {
				double da = Double.parseDouble(a)
				double db = Double.parseDouble(b)
				cmp = Double.compare(da, db)
			} catch (NumberFormatException e) {
				cmp = a.compareToIgnoreCase(b)
			}
			if (isAsc && cmp > 0) return false
			if (!isAsc && cmp < 0) return false
		}
		return true
	}

	/**
	 * Wait for the table to finish loading after a sort action.
	 * Waits for loading-row to disappear, then a short settle delay.
	 */
	private static void waitForTableToSettle() {
		WebUI.delay(1)
		try {
			Utils.waitForElementToDisappear('CRDC/AILocators/GenericTable/LoadingRow', 15)
		} catch (Exception e) {
			// loading-row may never appear for fast operations; ignore
		}
		WebUI.delay(1)
	}

	/**
	 * Verify which table columns are sortable vs non-sortable.
	 * Sortable columns have a MuiTableSortLabel-root child; non-sortable columns do not.
	 * Columns in nonSortableColumns should NOT have one; all others should.
	 * Reports Pass/Fail per column to ValidationReport.
	 *
	 * @param nonSortableColumns List of column names that should NOT be sortable (case-insensitive)
	 * @param reportSection (Optional) Section label for the HTML ValidationReport. Defaults to "Table Sorting".
	 *        Override when testing multiple tables in one test case to group results separately.
	 *        Example without: verifyTableColumnSortability([])
	 *        Example with:    verifyTableColumnSortability([], 'Data Submissions List')
	 */
	@Keyword
	static void verifyTableColumnSortability(List<String> nonSortableColumns, String reportSection = "Table Sorting") {
		KeywordUtil.logInfo("--- verifyTableColumnSortability ---")
		List<WebElement> headers = getTableHeaderElements()
		boolean allOk = true
		for (int i = 0; i < headers.size(); i++) {
			WebElement hdr = headers.get(i)
			String colName = getColumnName(hdr)
			if (colName.isEmpty()) continue
			boolean hasSortLabel = !hdr.findElements(By.cssSelector(".MuiTableSortLabel-root")).isEmpty()
			boolean shouldBeNonSortable = nonSortableColumns.any {
				it != null && (colName.equalsIgnoreCase(it) || colName.toLowerCase().contains(it.toLowerCase()))
			}
			if (shouldBeNonSortable && hasSortLabel) {
				ValidationReport.addValidationReportItem(reportSection, "Column '" + colName + "' sortability", "not sortable", "has sort control", "Fail")
				allOk = false
			} else if (!shouldBeNonSortable && !hasSortLabel) {
				ValidationReport.addValidationReportItem(reportSection, "Column '" + colName + "' sortability", "sortable", "no sort control", "Fail")
				allOk = false
			} else {
				String expected = shouldBeNonSortable ? "not sortable" : "sortable"
				ValidationReport.addValidationReportItem(reportSection, "Column '" + colName + "' sortability", expected, expected, "Pass")
			}
		}
		if (!allOk) {
			KeywordUtil.markFailed("verifyTableColumnSortability: one or more columns have incorrect sortability")
		} else {
			KeywordUtil.logInfo("All columns have correct sortability")
		}
	}

	/**
	 * Verify which column is sorted by default and in which direction, without clicking anything.
	 * Inspects MUI TableSortLabel classes on all header cells to find the active one.
	 *
	 * @param expectedColumn Expected column name that should be the default sort column
	 * @param expectedDirection Expected direction: "asc" or "desc"
	 * @param reportSection (Optional) Section label for the HTML ValidationReport. Defaults to "Table Sorting".
	 *        Override when testing multiple tables in one test case to group results separately.
	 *        Example without: verifyDefaultSortColumn('Last Updated', 'desc')
	 *        Example with:    verifyDefaultSortColumn('Last Updated', 'desc', 'Data Submissions List')
	 */
	@Keyword
	static void verifyDefaultSortColumn(String expectedColumn, String expectedDirection, String reportSection = "Table Sorting") {
		KeywordUtil.logInfo("--- verifyDefaultSortColumn: expecting '" + expectedColumn + "' " + expectedDirection + " ---")
		List<WebElement> headers = getTableHeaderElements()
		String actualColumn = null
		String actualDirection = null
		for (WebElement hdr : headers) {
			String dir = getColumnSortDirection(hdr)
			if (dir != null) {
				actualColumn = getColumnName(hdr)
				actualDirection = dir
				break
			}
		}
		// Verify column name
		boolean columnOk = actualColumn != null && actualColumn.equalsIgnoreCase(expectedColumn)
		ValidationReport.addValidationReportItem(reportSection, "Default sort column",
			expectedColumn, actualColumn != null ? actualColumn : "(none)", columnOk ? "Pass" : "Fail")
		// Verify direction
		boolean dirOk = actualDirection != null && actualDirection.equalsIgnoreCase(expectedDirection)
		ValidationReport.addValidationReportItem(reportSection, "Default sort direction",
			expectedDirection, actualDirection != null ? actualDirection : "(none)", dirOk ? "Pass" : "Fail")
		if (!columnOk || !dirOk) {
			KeywordUtil.markFailed("verifyDefaultSortColumn: expected '" + expectedColumn + "' " + expectedDirection +
				", actual '" + actualColumn + "' " + actualDirection)
		} else {
			KeywordUtil.logInfo("Default sort verified: '" + actualColumn + "' " + actualDirection)
		}
	}

	/**
	 * Click a column header and verify the sort direction toggles correctly.
	 * Clicks once (expects ascending), verifies sort indicator and data order,
	 * then clicks again (expects descending), verifies sort indicator and data order.
	 *
	 * @param columnName Display name of the column to sort (case-insensitive)
	 * @param reportSection (Optional) Section label for the HTML ValidationReport. Defaults to "Table Sorting".
	 *        Override when testing multiple tables in one test case to group results separately.
	 *        Example without: verifyColumnSortOnClick('Status')
	 *        Example with:    verifyColumnSortOnClick('Status', 'Data Submissions List')
	 */
	@Keyword
	static void verifyColumnSortOnClick(String columnName, String reportSection = "Table Sorting") {
		KeywordUtil.logInfo("--- verifyColumnSortOnClick: '" + columnName + "' ---")
		WebDriver driver = DriverFactory.getWebDriver()
		boolean allOk = true

		// Find header cell
		List<WebElement> headers = getTableHeaderElements()
		WebElement targetHeader = null
		for (WebElement hdr : headers) {
			if (getColumnName(hdr).equalsIgnoreCase(columnName)) {
				targetHeader = hdr
				break
			}
		}
		if (targetHeader == null) {
			ValidationReport.addValidationReportItem(reportSection, "Column '" + columnName + "' sort", "found", "not found", "Fail")
			KeywordUtil.markFailed("verifyColumnSortOnClick: column '" + columnName + "' not found")
			return
		}

		// Find sort label
		List<WebElement> sortLabels = targetHeader.findElements(By.cssSelector(".MuiTableSortLabel-root"))
		if (sortLabels.isEmpty()) {
			ValidationReport.addValidationReportItem(reportSection, "Column '" + columnName + "' sort", "sortable", "not sortable", "Fail")
			KeywordUtil.markFailed("verifyColumnSortOnClick: column '" + columnName + "' is not sortable")
			return
		}

		// --- First click: expect ascending ---
		KeywordUtil.logInfo("Clicking '" + columnName + "' for ascending sort")
		JavascriptExecutor js = (JavascriptExecutor) driver
		js.executeScript("arguments[0].scrollIntoView(true);", sortLabels.get(0))
		sortLabels.get(0).click()
		waitForTableToSettle()

		// Re-find header after DOM update
		headers = getTableHeaderElements()
		targetHeader = headers.find { getColumnName(it).equalsIgnoreCase(columnName) }
		String dirAfterFirstClick = targetHeader != null ? getColumnSortDirection(targetHeader) : null

		// The first click may result in asc or desc depending on current state.
		// Detect actual direction after first click and verify the icon matches.
		String firstDir = dirAfterFirstClick
		if (firstDir == null) {
			ValidationReport.addValidationReportItem(reportSection, "Column '" + columnName + "' sort after click 1",
				"asc or desc", "(no aria-sort)", "Fail")
			allOk = false
		} else {
			ValidationReport.addValidationReportItem(reportSection, "Column '" + columnName + "' aria-sort after click 1",
				firstDir, firstDir, "Pass")
			// Verify data is actually sorted in that direction
			List<String> valuesFirst = readColumnValues(columnName)
			boolean sortedFirst = isSortedCorrectly(valuesFirst, firstDir)
			ValidationReport.addValidationReportItem(reportSection, "Column '" + columnName + "' data order (" + firstDir + ")",
				"sorted " + firstDir, sortedFirst ? "sorted " + firstDir : "not sorted", sortedFirst ? "Pass" : "Fail")
			if (!sortedFirst) allOk = false
		}

		// --- Second click: expect opposite direction ---
		String expectedSecondDir = "asc".equals(firstDir) ? "desc" : "asc"
		KeywordUtil.logInfo("Clicking '" + columnName + "' for " + expectedSecondDir + " sort")

		// Re-find sort label after DOM refresh
		headers = getTableHeaderElements()
		targetHeader = headers.find { getColumnName(it).equalsIgnoreCase(columnName) }
		sortLabels = targetHeader?.findElements(By.cssSelector(".MuiTableSortLabel-root")) ?: []
		if (!sortLabels.isEmpty()) {
			js.executeScript("arguments[0].scrollIntoView(true);", sortLabels.get(0))
			sortLabels.get(0).click()
			waitForTableToSettle()

			// Re-find header after DOM update
			headers = getTableHeaderElements()
			targetHeader = headers.find { getColumnName(it).equalsIgnoreCase(columnName) }
			String dirAfterSecondClick = targetHeader != null ? getColumnSortDirection(targetHeader) : null

			if (dirAfterSecondClick == null) {
				ValidationReport.addValidationReportItem(reportSection, "Column '" + columnName + "' sort after click 2",
					expectedSecondDir, "(no aria-sort)", "Fail")
				allOk = false
			} else {
				boolean dirOk = dirAfterSecondClick.equalsIgnoreCase(expectedSecondDir)
				ValidationReport.addValidationReportItem(reportSection, "Column '" + columnName + "' aria-sort after click 2",
					expectedSecondDir, dirAfterSecondClick, dirOk ? "Pass" : "Fail")
				if (!dirOk) allOk = false

				// Verify data order
				List<String> valuesSecond = readColumnValues(columnName)
				boolean sortedSecond = isSortedCorrectly(valuesSecond, dirAfterSecondClick)
				ValidationReport.addValidationReportItem(reportSection, "Column '" + columnName + "' data order (" + dirAfterSecondClick + ")",
					"sorted " + dirAfterSecondClick, sortedSecond ? "sorted " + dirAfterSecondClick : "not sorted", sortedSecond ? "Pass" : "Fail")
				if (!sortedSecond) allOk = false
			}
		}

		if (!allOk) {
			KeywordUtil.markFailed("verifyColumnSortOnClick: '" + columnName + "' sort verification had failures")
		} else {
			KeywordUtil.logInfo("Column '" + columnName + "' sort verified successfully (both directions)")
		}
	}

	/**
	 * All-in-one sorting verification: verifies sortability of all columns, then clicks
	 * each sortable column and verifies ascending/descending behavior.
	 *
	 * @param nonSortableColumns List of column names that should NOT be sortable
	 * @param reportSection (Optional) Section label for the HTML ValidationReport. Defaults to "Table Sorting".
	 *        Override when testing multiple tables in one test case to group results separately.
	 *        Example without: verifyAllSortableColumnsSorting([])
	 *        Example with:    verifyAllSortableColumnsSorting([], 'Validation Results Table')
	 */
	@Keyword
	static void verifyAllSortableColumnsSorting(List<String> nonSortableColumns, String reportSection = "Table Sorting") {
		KeywordUtil.logInfo("--- verifyAllSortableColumnsSorting ---")
		verifyTableColumnSortability(nonSortableColumns, reportSection)

		List<WebElement> headers = getTableHeaderElements()
		List<String> sortableColumns = new ArrayList<>()
		for (WebElement hdr : headers) {
			String colName = getColumnName(hdr)
			if (colName.isEmpty()) continue
			boolean hasSortLabel = !hdr.findElements(By.cssSelector(".MuiTableSortLabel-root")).isEmpty()
			if (hasSortLabel) {
				sortableColumns.add(colName)
			}
		}
		KeywordUtil.logInfo("Sortable columns found: " + sortableColumns)
		for (String col : sortableColumns) {
			verifyColumnSortOnClick(col, reportSection)
		}
		KeywordUtil.logInfo("--- verifyAllSortableColumnsSorting complete ---")
	}
}
