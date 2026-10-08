package utilities

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions

import java.util.ArrayList
import java.util.List
import java.util.Map
import java.util.concurrent.TimeUnit

/**
 * Submission Requests List: Keywords for navigating, filtering, and verifying the
 * Submission Requests list page. Mirrors DataSubmissionsList/ManageStudies/ManagePrograms pattern.
 * Call applySubmissionRequestsListFilters before verifySubmissionRequestsFilterResults when filters are needed.
 * Expectation keys (expectAtLeastRows, rowContains) are optional.
 */
class SubmissionRequestList extends TestRunner {

	private static final String SECTION_SUBMISSION_REQUESTS = "Submission Requests List"
	private static final int DEFAULT_TIMEOUT = 30

	/**
	 * Navigate to the Submission Requests list via Nav Bar and wait for the list page to be ready.
	 * Fails if the page does not load.
	 */
	@Keyword
	void navigateToSubmissionRequestsList() {
		KeywordUtil.logInfo("--- navigateToSubmissionRequestsList ---")
		try {
			clickTab('CRDC/NavBar/SubmissionRequest-Tab')
			WebUI.delay(2)
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/SubmissionRequests/List/Container"), DEFAULT_TIMEOUT)
			ValidationReport.addValidationReportItem(SECTION_SUBMISSION_REQUESTS, "Navigate to Submission Requests", "page loaded", "page loaded", "Pass")
			KeywordUtil.logInfo("Submission Requests list page loaded")
		} catch (Exception e) {
			ValidationReport.addValidationReportItem(SECTION_SUBMISSION_REQUESTS, "Navigate to Submission Requests", "page loaded", "failed", "Fail")
			KeywordUtil.markFailed("navigateToSubmissionRequestsList: page did not load - " + e.getMessage())
		}
	}

	/**
	 * Verify the Submission Requests list page shows core UI: container, Start a Submission Request button, table rows.
	 * Reports each check to ValidationReport. Fails if container not found.
	 */
	@Keyword
	void verifySubmissionRequestsListPage() {
		KeywordUtil.logInfo("--- verifySubmissionRequestsListPage ---")
		try {
			boolean containerPresent = WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/SubmissionRequests/List/Container"), 10)
			ValidationReport.addValidationReportItem(SECTION_SUBMISSION_REQUESTS, "List container", "present", containerPresent ? "present" : "not found", containerPresent ? "Pass" : "Fail")
			if (!containerPresent) {
				KeywordUtil.markFailed("verifySubmissionRequestsListPage: list container not found")
				return
			}

			boolean startBtnPresent = WebUI.waitForElementPresent(findTestObject("CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn"), 5)
			ValidationReport.addValidationReportItem(SECTION_SUBMISSION_REQUESTS, "Start a Submission Request button", "present", startBtnPresent ? "present" : "not found", startBtnPresent ? "Pass" : "Fail")

			List<WebElement> rows = Utils.getListWebElements("CRDC/AILocators/GenericTable/TableRow")
			int rowCount = (rows == null) ? 0 : rows.size()
			ValidationReport.addValidationReportItem(SECTION_SUBMISSION_REQUESTS, "Table rows visible", ">= 0", String.valueOf(rowCount), "Pass")
			KeywordUtil.logInfo("verifySubmissionRequestsListPage: container and Start button verified; rows=" + rowCount)
		} catch (Exception e) {
			KeywordUtil.markFailed("verifySubmissionRequestsListPage: " + e.getMessage())
		}
	}

	/**
	 * Apply filter criteria to the Submission Requests list. Skips null or empty values.
	 * Supported criteria keys: submitterName, program, study, status.
	 * status: may be a single value or comma-separated list (e.g. "New, In Progress"). When provided,
	 * all currently selected options in the Status multi-select are cleared, then only the given option(s) are selected.
	 *
	 * @param criteria Map with optional keys: submitterName (String), program (String), study (String), status (String)
	 */
	@Keyword
	static void applySubmissionRequestsListFilters(Map<String, String> criteria) {
		if (criteria == null || criteria.isEmpty()) {
			KeywordUtil.logInfo("applySubmissionRequestsListFilters: no criteria; skipping")
			return
		}
		KeywordUtil.logInfo("--- applySubmissionRequestsListFilters: " + criteria + " ---")
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			String listFilters = "CRDC/AILocators/SubmissionRequests/List/Filters"

			if (criteria.submitterName != null && !criteria.submitterName.trim().isEmpty()) {
				String v = criteria.submitterName.trim()
				if (WebUI.waitForElementPresent(findTestObject(listFilters + "/SubmitterName-TxtBx"), 5)) {
					WebUI.clearText(findTestObject(listFilters + "/SubmitterName-TxtBx"))
					WebUI.setText(findTestObject(listFilters + "/SubmitterName-TxtBx"), v)
					WebUI.delay(1)
				}
			}

			if (criteria.program != null && !criteria.program.trim().isEmpty()) {
				String v = criteria.program.trim().replace("'", "''")
				if (WebUI.waitForElementPresent(findTestObject(listFilters + "/Program-Ddn"), 5)) {
					WebUI.waitForElementClickable(findTestObject(listFilters + "/Program-Ddn"), 5)
					WebUI.click(findTestObject(listFilters + "/Program-Ddn"))
					WebUI.delay(1)
					WebElement opt = driver.findElement(By.xpath("//li[normalize-space(text())='" + v + "']"))
					opt.click()
					WebUI.delay(1)
				}
			}

			if (criteria.study != null && !criteria.study.trim().isEmpty()) {
				String v = criteria.study.trim()
				if (WebUI.waitForElementPresent(findTestObject(listFilters + "/Study-TxtBx"), 5)) {
					WebUI.clearText(findTestObject(listFilters + "/Study-TxtBx"))
					WebUI.setText(findTestObject(listFilters + "/Study-TxtBx"), v)
					WebUI.delay(1)
				}
			}

			if (criteria.status != null && !criteria.status.trim().isEmpty()) {
				List<String> statusValues = criteria.status.split(",").collect { it.trim() }.findAll { it }
				if (!statusValues.isEmpty() && WebUI.waitForElementPresent(findTestObject(listFilters + "/Status-Ddn"), 5)) {
					WebUI.waitForElementClickable(findTestObject(listFilters + "/Status-Ddn"), 5)
					WebUI.click(findTestObject(listFilters + "/Status-Ddn"))
					WebUI.delay(1)
					List<WebElement> listboxes = driver.findElements(By.xpath("//*[@role='listbox']"))
					WebElement listbox = (listboxes != null && !listboxes.isEmpty()) ? listboxes.get(0) : null
					if (listbox == null) {
						listboxes = driver.findElements(By.cssSelector("ul.MuiList-root"))
						listbox = (listboxes != null && !listboxes.isEmpty()) ? listboxes.get(0) : null
					}
					if (listbox != null) {
						try {
							driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS)
							int maxDeselect = 20
							while (maxDeselect-- > 0) {
								List<WebElement> listboxCandidates = driver.findElements(By.xpath("//*[@role='listbox']"))
								if (listboxCandidates.isEmpty()) listboxCandidates = driver.findElements(By.cssSelector("ul.MuiList-root"))
								if (listboxCandidates.isEmpty()) break
									WebElement listboxRef = listboxCandidates.get(0)
								List<WebElement> selected = listboxRef.findElements(By.cssSelector("li[aria-selected='true']"))
								if (selected.isEmpty()) selected = listboxRef.findElements(By.cssSelector("li.Mui-selected"))
								if (selected.isEmpty()) break
									WebElement toClick = selected.get(0)
								((JavascriptExecutor) driver).executeScript("arguments[0].click();", toClick)
							}
						} finally {
							driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)
						}
						int optionWaitSeconds = 5
						for (String raw : statusValues) {
							String escaped = raw.replace("'", "''")
							try {
								List<WebElement> listboxCandidates = driver.findElements(By.xpath("//*[@role='listbox']"))
								if (listboxCandidates.isEmpty()) listboxCandidates = driver.findElements(By.cssSelector("ul.MuiList-root"))
								if (listboxCandidates.isEmpty()) break
									WebElement listboxRef = listboxCandidates.get(0)
								WebElement opt = new WebDriverWait(driver, optionWaitSeconds).until(ExpectedConditions.presenceOfNestedElementLocatedBy(listboxRef, By.xpath(".//li[normalize-space(text())='" + escaped + "']")))
								opt.click()
								WebUI.delay(1)
							} catch (Exception ex) {
								KeywordUtil.logInfo("applySubmissionRequestsListFilters: Status option not found: '" + raw + "' - " + ex.getMessage())
							}
						}
						WebUI.delay(1)
						try {
							new Actions(driver).sendKeys(Keys.ESCAPE).perform()
						} catch (Exception ex) {
							KeywordUtil.logInfo("applySubmissionRequestsListFilters: ESC to close Status dropdown: " + ex.getMessage())
						}
						WebUI.delay(1)
					} else {
						KeywordUtil.logInfo("applySubmissionRequestsListFilters: listbox not found; using single global li for first status value")
						String v = statusValues.get(0).replace("'", "''")
						WebElement opt = driver.findElement(By.xpath("//li[normalize-space(text())='" + v + "']"))
						opt.click()
						WebUI.delay(1)
					}
				}
			}
			WebUI.delay(2)
			KeywordUtil.logInfo("applySubmissionRequestsListFilters: completed")
		} catch (Exception e) {
			KeywordUtil.markFailed("applySubmissionRequestsListFilters: " + e.getMessage())
		}
	}

	/**
	 * Read currently visible Submission Requests list table rows (GenericTable). Returns list of row data.
	 *
	 * @return List of rows, each row a List<String> of cell values in header order
	 */
	@Keyword
	List<List<String>> readSubmissionRequestsTableRows() {
		List<List<String>> result = new ArrayList<>()
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			String tableXpath = TestRunner.givexpath("CRDC/AILocators/GenericTable/Table")
			WebElement table = driver.findElement(By.xpath(tableXpath))
			List<WebElement> headerEls = table.findElements(By.xpath(".//thead//th"))
			List<String> headers = new ArrayList<>()
			for (WebElement h : headerEls) headers.add(h.getText().trim())
			List<WebElement> rows = Utils.getListWebElements("CRDC/AILocators/GenericTable/TableRow")
			if (rows == null) {
				KeywordUtil.logInfo("Submission Requests table row count: 0")
				return result
			}
			for (WebElement row : rows) {
				List<WebElement> cells = row.findElements(By.tagName("td"))
				List<String> cellTexts = new ArrayList<>()
				for (WebElement cell : cells) cellTexts.add(cellTextForTableMatch(cell))
				result.add(cellTexts)
			}
			KeywordUtil.logInfo("Submission Requests table row count: " + result.size())
		} catch (Exception e) {
			KeywordUtil.logInfo("readSubmissionRequestsTableRows: " + e.getMessage())
		}
		return result
	}

	/**
	 * Verify that the current table results match expectations. Call applySubmissionRequestsListFilters first when filters are needed.
	 *
	 * @param expectations Map with optional keys: expectAtLeastRows (Integer), rowContains (String)
	 * @param reportSection Optional section name for ValidationReport (default: SECTION_SUBMISSION_REQUESTS)
	 */
	@Keyword
	void verifySubmissionRequestsFilterResults(Map<String, Object> expectations, String reportSection = SECTION_SUBMISSION_REQUESTS) {
		KeywordUtil.logInfo("--- verifySubmissionRequestsFilterResults ---")
		try {
			List<List<String>> rows = readSubmissionRequestsTableRows()
			int count = rows.size()

			if (expectations != null) {
				if (expectations.expectAtLeastRows != null) {
					int min = ((Number) expectations.expectAtLeastRows).intValue()
					boolean pass = count >= min
					ValidationReport.addValidationReportItem(reportSection, "Row count >= " + min, String.valueOf(min), String.valueOf(count), pass ? "Pass" : "Fail")
					if (!pass) KeywordUtil.markFailed("verifySubmissionRequestsFilterResults: expected at least " + min + " rows, got " + count)
				}
				if (expectations.rowContains != null) {
					String substring = expectations.rowContains.toString().trim()
					boolean found = false
					for (List<String> row : rows) {
						for (String cell : row) {
							if (cell != null && cell.contains(substring)) {
								found = true; break
							}
						}
						if (found) break
					}
					ValidationReport.addValidationReportItem(reportSection, "Row contains '" + substring + "'", "at least one", found ? "found" : "not found", found ? "Pass" : "Fail")
					if (!found) KeywordUtil.markFailed("verifySubmissionRequestsFilterResults: no row contains '" + substring + "'")
				}
			}
			KeywordUtil.logInfo("verifySubmissionRequestsFilterResults: completed; rows=" + count)
		} catch (Exception e) {
			KeywordUtil.markFailed("verifySubmissionRequestsFilterResults: " + e.getMessage())
		}
	}

	/**
	 * Reset Submission Requests list filters by clicking the reset button.
	 */
	@Keyword
	void resetSubmissionRequestsFilters() {
		KeywordUtil.logInfo("--- resetSubmissionRequestsFilters ---")
		try {
			if (WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/SubmissionRequests/List/Filters/ResetFilters-Btn"), 5)) {
				WebUI.click(findTestObject("CRDC/AILocators/SubmissionRequests/List/Filters/ResetFilters-Btn"))
				WebUI.delay(2)
				KeywordUtil.logInfo("resetSubmissionRequestsFilters: completed")
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("resetSubmissionRequestsFilters: " + e.getMessage())
		}
	}

	private static int indexOfHeaderIgnoreCase(List<String> headers, String headerLabel) {
		if (headers == null || headerLabel == null) return -1
		String target = headerLabel.trim().toLowerCase()
		for (int i = 0; i < headers.size(); i++) {
			if (headers[i] != null && headers[i].trim().toLowerCase() == target) return i
		}
		return -1
	}

	/** Full value for table matching when the UI shows truncated-text-label but aria-label has the full string. */
	private static String cellTextForTableMatch(WebElement cell) {
		if (cell == null) return ''
		List<WebElement> wrappers = cell.findElements(By.cssSelector("[data-testid='truncated-text-wrapper']"))
		for (WebElement w : wrappers) {
			String aria = w.getAttribute('aria-label')
			if (aria != null && !aria.trim().isEmpty()) return aria.trim()
		}
		return cell.getText().trim()
	}

	/**
	 * Toggle Show All Versions on the Submission Requests list filter header.
	 */
	@Keyword
	static void setShowAllVersions(boolean showAll) {
		KeywordUtil.logInfo("setShowAllVersions: " + showAll)
		String locator = 'CRDC/AILocators/SubmissionRequests/List/Filters/ShowAllVersions-Chkbx'
		if (!WebUI.waitForElementPresent(findTestObject(locator), 10)) {
			KeywordUtil.markFailed("setShowAllVersions: Show All Versions control not found (data-testid show-all-versions-checkbox).")
			return
		}
		WebElement checkbox = DriverFactory.getWebDriver().findElement(By.xpath(TestRunner.givexpath(locator)))
		boolean checked = checkbox.isSelected()
		if (showAll != checked) {
			WebUI.click(findTestObject(locator))
			WebUI.delay(2)
		}
	}

	@Keyword
	static void verifyShowAllVersionsDefaultUnchecked() {
		String locator = 'CRDC/AILocators/SubmissionRequests/List/Filters/ShowAllVersions-Chkbx'
		if (!WebUI.waitForElementPresent(findTestObject(locator), 10)) {
			KeywordUtil.markFailed("verifyShowAllVersionsDefaultUnchecked: checkbox not found.")
			return
		}
		WebElement checkbox = DriverFactory.getWebDriver().findElement(By.xpath(TestRunner.givexpath(locator)))
		boolean checked = checkbox.isSelected()
		ValidationReport.addValidationReportItem(SECTION_SUBMISSION_REQUESTS, 'Show All Versions default', 'unchecked', checked ? 'checked' : 'unchecked', checked ? 'Fail' : 'Pass')
		if (checked) KeywordUtil.markFailed("Show All Versions should be unchecked by default.")
	}

	@Keyword
	static void verifySubmissionRequestsSrfVersionColumnVisible() {
		boolean present = WebUI.waitForElementPresent(findTestObject('CRDC/AILocators/SubmissionRequests/List/Table/SRFVersion-Header'), 10)
		ValidationReport.addValidationReportItem(SECTION_SUBMISSION_REQUESTS, 'SRF Version column', 'visible', present ? 'visible' : 'missing', present ? 'Pass' : 'Fail')
		if (!present) KeywordUtil.markFailed("SRF Version column header not found.")
	}

	@Keyword
	static void verifySubmissionRequestsFormVersionColumnHiddenInGui() {
		WebDriver driver = DriverFactory.getWebDriver()
		boolean formVersionVisible = !driver.findElements(By.xpath("//th[normalize-space()='Form Version']")).isEmpty()
		boolean legacyVersionVisible = !driver.findElements(By.xpath("//th[normalize-space()='Version']")).isEmpty()
		boolean hidden = !formVersionVisible && !legacyVersionVisible
		ValidationReport.addValidationReportItem(SECTION_SUBMISSION_REQUESTS, 'Form/legacy Version column hidden in GUI', 'hidden', hidden ? 'hidden' : 'visible', hidden ? 'Pass' : 'Fail')
		if (!hidden) KeywordUtil.markFailed("Form Version or legacy Version column should not be visible in the GUI table.")
	}

	/**
	 * Returns SRF Version values for rows whose Study column contains studyToken (case-insensitive).
	 * Study cells may show truncated visible text; cell values use truncated-text-wrapper aria-label when present.
	 */
	@Keyword
	static List<Integer> getSrVersionNumbersForStudy(String studyToken) {
		List<Integer> versions = new ArrayList<>()
		if (studyToken == null || studyToken.trim().isEmpty()) return versions
		String token = studyToken.trim().toLowerCase()
		List<List<String>> rows = new SubmissionRequestList().readSubmissionRequestsTableRows()
		if (rows.isEmpty()) return versions
		WebDriver driver = DriverFactory.getWebDriver()
		String tableXpath = TestRunner.givexpath('CRDC/AILocators/GenericTable/Table')
		WebElement table = driver.findElement(By.xpath(tableXpath))
		List<WebElement> headerEls = table.findElements(By.xpath('.//thead//th'))
		List<String> headers = new ArrayList<>()
		for (WebElement h : headerEls) headers.add(h.getText().trim())
		int studyCol = indexOfHeaderIgnoreCase(headers, 'Study')
		int srfCol = indexOfHeaderIgnoreCase(headers, 'SRF Version')
		if (srfCol < 0) {
			srfCol = indexOfHeaderIgnoreCase(headers, 'Version')
		}
		if (studyCol < 0 || srfCol < 0) {
			KeywordUtil.logInfo("getSrVersionNumbersForStudy: Study col=" + studyCol + ", SRF Version col=" + srfCol)
			return versions
		}
		for (List<String> row : rows) {
			if (row.size() <= Math.max(studyCol, srfCol)) continue
			String studyCell = row[studyCol] ?: ''
			if (!studyCell.toLowerCase().contains(token)) continue
			String verText = row[srfCol]?.trim()
			if (verText?.isInteger()) versions.add(verText.toInteger())
		}
		return versions
	}

	@Keyword
	static void verifySrVersionsDescendingForStudy(String studyToken, int minRowCount) {
		List<Integer> versions = getSrVersionNumbersForStudy(studyToken)
		boolean countOk = versions.size() >= minRowCount
		ValidationReport.addValidationReportItem(SECTION_SUBMISSION_REQUESTS, 'SRF version row count', ">= " + minRowCount, String.valueOf(versions.size()), countOk ? 'Pass' : 'Fail')
		if (!countOk) {
			KeywordUtil.markFailed("Expected at least " + minRowCount + " version rows, got " + versions.size())
			return
		}
		for (int i = 1; i < versions.size(); i++) {
			if (versions[i] > versions[i - 1]) {
				KeywordUtil.markFailed("SRF versions not descending: " + versions)
				return
			}
		}
		ValidationReport.addValidationReportItem(SECTION_SUBMISSION_REQUESTS, 'SRF versions sort', 'descending', versions.toString(), 'Pass')
	}

	/**
	 * 4b: Reopen is visible only on the row with the highest SRF Version for the study; hidden on all other version rows.
	 * studyToken should match list Study cells (typically study abbreviation; see truncated-text-wrapper aria-label).
	 */
	@Keyword
	static void verifyReopenOnlyOnLatestSrVersionForStudy(String studyToken) {
		List<Integer> versions = getSrVersionNumbersForStudy(studyToken)
		if (versions.isEmpty()) {
			KeywordUtil.markFailed('verifyReopenOnlyOnLatestSrVersionForStudy: no SRF versions found for study token: ' + studyToken)
			return
		}
		int latest = versions.max()
		for (Integer v : versions) {
			assertReopenOnRowWithSrVersion(studyToken, v, v == latest)
		}
	}

	/**
	 * Assert Reopen control visibility on the table row matching study token and SRF Version number.
	 */
	@Keyword
	static void assertReopenOnRowWithSrVersion(String studyToken, int srfVersion, boolean expectedVisible) {
		if (studyToken == null || studyToken.trim().length() < 3) {
			KeywordUtil.markFailed("assertReopenOnRowWithSrVersion: studyToken must be at least 3 characters.")
			return
		}
		String escapedStudy = studyToken.trim().replace("'", "''")
		String tokenLower = escapedStudy.toLowerCase()
		String reopenXpath = "//tr[(.//td[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" +
			tokenLower + "')] or .//*[@data-testid='truncated-text-wrapper' and contains(translate(@aria-label, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" +
			tokenLower + "')]) and .//td[normalize-space()='" + srfVersion + "']]//*[@data-testid='reopen-application-button']"
		boolean present = DriverFactory.getWebDriver().findElements(By.xpath(reopenXpath)).size() > 0
		String label = "Reopen on SRF version " + srfVersion
		if (present == expectedVisible) {
			ValidationReport.addValidationReportItem(SECTION_SUBMISSION_REQUESTS, label, String.valueOf(expectedVisible), String.valueOf(present), 'Pass')
		} else {
			ValidationReport.addValidationReportItem(SECTION_SUBMISSION_REQUESTS, label, String.valueOf(expectedVisible), String.valueOf(present), 'Fail')
			KeywordUtil.markFailed(label + ": expected " + expectedVisible + ", actual " + present)
		}
	}
}
