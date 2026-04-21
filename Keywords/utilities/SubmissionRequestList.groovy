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
				for (WebElement cell : cells) cellTexts.add(cell.getText().trim())
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
}
