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
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions

import java.util.ArrayList
import java.util.List
import java.util.Map
import java.util.concurrent.TimeUnit

/**
 * Data Submissions List: Keywords for navigating, filtering, and verifying the main Data Submissions list page
 * (table of data submissions and filters). Mirrors ManageStudies/ManagePrograms pattern.
 * Call applyDataSubmissionsListFilters before verifyDataSubmissionsListFilterResults when filters are needed.
 * Expectation keys (expectAtLeastRows, rowContains) are optional.
 */
class DataSubmissionsList extends TestRunner {

	private static final String SECTION_DATA_SUBMISSIONS_LIST = "Data Submissions List"
	private static final int DEFAULT_TIMEOUT = 30

	/**
	 * Navigate to the Data Submissions list via Nav Bar and wait for the list page to be ready.
	 * Fails if the page does not load.
	 */
	@Keyword
	void navigateToDataSubmissionsList() {
		KeywordUtil.logInfo("--- navigateToDataSubmissionsList ---")
		try {
			clickTab('CRDC/NavBar/DataSubmissions-Tab')
			WebUI.delay(2)
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/DataSubmissions/List/Table/CreateDataSubmission-Btn"), DEFAULT_TIMEOUT)
			KeywordUtil.logInfo("Data Submissions list page loaded")
		} catch (Exception e) {
			KeywordUtil.markFailed("navigateToDataSubmissionsList: page did not load - " + e.getMessage())
		}
	}

	/**
	 * Verify the Data Submissions list page shows core UI: table (or Create button), Create button, and table rows count.
	 * Reports each check to ValidationReport. Fails if table/Create button not found.
	 */
	@Keyword
	void verifyDataSubmissionsListPage() {
		KeywordUtil.logInfo("--- verifyDataSubmissionsListPage ---")
		try {
			boolean tablePresent = WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/DataSubmissions/List/Table/Table"), 10)
			ValidationReport.addValidationReportItem(SECTION_DATA_SUBMISSIONS_LIST, "List table", "present", tablePresent ? "present" : "not found", tablePresent ? "Pass" : "Fail")
			if (!tablePresent) {
				KeywordUtil.markFailed("verifyDataSubmissionsListPage: list table not found")
				return
			}

			boolean createBtnPresent = WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/DataSubmissions/List/Table/CreateDataSubmission-Btn"), 5)
			ValidationReport.addValidationReportItem(SECTION_DATA_SUBMISSIONS_LIST, "Create Data Submission button", "present", createBtnPresent ? "present" : "not found", createBtnPresent ? "Pass" : "Fail")

			List<WebElement> rows = Utils.getListWebElements("CRDC/AILocators/GenericTable/TableRow")
			int rowCount = (rows == null) ? 0 : rows.size()
			ValidationReport.addValidationReportItem(SECTION_DATA_SUBMISSIONS_LIST, "Table rows visible", ">= 0", String.valueOf(rowCount), "Pass")
			KeywordUtil.logInfo("verifyDataSubmissionsListPage: table and Create button verified; rows=" + rowCount)
		} catch (Exception e) {
			KeywordUtil.markFailed("verifyDataSubmissionsListPage: " + e.getMessage())
		}
	}

	/**
	 * Apply filter criteria to the Data Submissions list. Skips null or empty values.
	 * Supported criteria keys: submissionName, status, program, submitterName, dataCommons, dbGaP.
	 * status: may be a single value or comma-separated list (e.g. "Submitted, Validated"). When provided,
	 * all currently selected options in the Status multi-select are cleared, then only the given option(s) are selected.
	 *
	 * @param criteria Map with optional keys: submissionName, status, program, submitterName, dataCommons, dbGaP (all String)
	 */
	@Keyword
	void applyDataSubmissionsListFilters(Map<String, String> criteria) {
		if (criteria == null || criteria.isEmpty()) {
			KeywordUtil.logInfo("applyDataSubmissionsListFilters: no criteria; skipping")
			return
		}
		KeywordUtil.logInfo("--- applyDataSubmissionsListFilters: " + criteria + " ---")
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			String listFilters = "CRDC/AILocators/DataSubmissions/List/Filters"

			if (criteria.submissionName != null && !criteria.submissionName.trim().isEmpty()) {
				String v = criteria.submissionName.trim()
				if (WebUI.waitForElementPresent(findTestObject(listFilters + "/SubmissionName-TxtBx"), 5)) {
					WebUI.clearText(findTestObject(listFilters + "/SubmissionName-TxtBx"))
					WebUI.setText(findTestObject(listFilters + "/SubmissionName-TxtBx"), v)
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
						// Deselect all currently selected options
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
								((JavascriptExecutor) driver).executeScript("arguments[0].click();", toClick) // much faster than regular Selenium
							}
						} finally {
							driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)
						}
						// Select only the requested option(s)
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
								KeywordUtil.logInfo("applyDataSubmissionsListFilters: Status option not found: '" + raw + "' - " + ex.getMessage())
							}
						}
						// Close dropdown
						WebUI.waitForElementClickable(findTestObject(listFilters + "/Status-Ddn"), 5)
						WebUI.click(findTestObject(listFilters + "/Status-Ddn"))
					} else {
						KeywordUtil.logInfo("applyDataSubmissionsListFilters: listbox not found; using single global li for first status value")
						String v = statusValues.get(0).replace("'", "''")
						WebElement opt = driver.findElement(By.xpath("//li[normalize-space(text())='" + v + "']"))
						opt.click()
						WebUI.delay(1)
					}
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
			if (criteria.submitterName != null && !criteria.submitterName.trim().isEmpty()) {
				String v = criteria.submitterName.trim().replace("'", "''")
				if (WebUI.waitForElementPresent(findTestObject(listFilters + "/SubmitterName-Ddn"), 5)) {
					WebUI.waitForElementClickable(findTestObject(listFilters + "/SubmitterName-Ddn"), 5)
					WebUI.click(findTestObject(listFilters + "/SubmitterName-Ddn"))
					WebUI.delay(1)
					WebElement opt = driver.findElement(By.xpath("//li[normalize-space(text())='" + v + "']"))
					opt.click()
					WebUI.delay(1)
				}
			}
			if (criteria.dataCommons != null && !criteria.dataCommons.trim().isEmpty()) {
				String v = criteria.dataCommons.trim().replace("'", "''")
				if (WebUI.waitForElementPresent(findTestObject(listFilters + "/DataCommons-Ddn"), 5)) {
					WebUI.waitForElementClickable(findTestObject(listFilters + "/DataCommons-Ddn"), 5)
					WebUI.click(findTestObject(listFilters + "/DataCommons-Ddn"))
					WebUI.delay(1)
					WebElement opt = driver.findElement(By.xpath("//li[normalize-space(text())='" + v + "']"))
					opt.click()
					WebUI.delay(1)
				}
			}
			if (criteria.dbGaP != null && !criteria.dbGaP.trim().isEmpty()) {
				String v = criteria.dbGaP.trim()
				if (WebUI.waitForElementPresent(findTestObject(listFilters + "/DbGaPID-TxtBx"), 5)) {
					WebUI.clearText(findTestObject(listFilters + "/DbGaPID-TxtBx"))
					WebUI.setText(findTestObject(listFilters + "/DbGaPID-TxtBx"), v)
					WebUI.delay(1)
				}
			}
			WebUI.delay(2)
			KeywordUtil.logInfo("applyDataSubmissionsListFilters: completed")
		} catch (Exception e) {
			KeywordUtil.markFailed("applyDataSubmissionsListFilters: " + e.getMessage())
		}
	}

	/**
	 * Read currently visible Data Submissions list table rows (GenericTable). Returns list of row data.
	 *
	 * @return List of rows, each row a List<String> of cell values in header order
	 */
	@Keyword
	List<List<String>> readDataSubmissionsListTableRows() {
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
				KeywordUtil.logInfo("Data Submissions list table row count: 0")
				return result
			}
			for (WebElement row : rows) {
				List<WebElement> cells = row.findElements(By.tagName("td"))
				List<String> cellTexts = new ArrayList<>()
				for (WebElement cell : cells) cellTexts.add(cell.getText().trim())
				result.add(cellTexts)
			}
			KeywordUtil.logInfo("Data Submissions list table row count: " + result.size())
		} catch (Exception e) {
			KeywordUtil.logInfo("readDataSubmissionsListTableRows: " + e.getMessage())
		}
		return result
	}

	/**
	 * Verify that the current table results match expectations. Call applyDataSubmissionsListFilters first when filters are needed.
	 *
	 * @param expectations Map with optional keys: expectAtLeastRows (Integer), rowContains (String)
	 * @param reportSection Optional section name for ValidationReport (default: SECTION_DATA_SUBMISSIONS_LIST)
	 */
	@Keyword
	void verifyDataSubmissionsListFilterResults(Map<String, Object> expectations, String reportSection = SECTION_DATA_SUBMISSIONS_LIST) {
		KeywordUtil.logInfo("--- verifyDataSubmissionsListFilterResults ---")
		try {
			List<List<String>> rows = readDataSubmissionsListTableRows()
			int count = rows.size()

			if (expectations != null) {
				if (expectations.expectAtLeastRows != null) {
					int min = ((Number) expectations.expectAtLeastRows).intValue()
					boolean pass = count >= min
					ValidationReport.addValidationReportItem(reportSection, "Row count >= " + min, String.valueOf(min), String.valueOf(count), pass ? "Pass" : "Fail")
					if (!pass) KeywordUtil.markFailed("verifyDataSubmissionsListFilterResults: expected at least " + min + " rows, got " + count)
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
					if (!found) KeywordUtil.markFailed("verifyDataSubmissionsListFilterResults: no row contains '" + substring + "'")
				}
			}
			KeywordUtil.logInfo("verifyDataSubmissionsListFilterResults: completed; rows=" + count)
		} catch (Exception e) {
			KeywordUtil.markFailed("verifyDataSubmissionsListFilterResults: " + e.getMessage())
		}
	}

	/**
	 * Reset Data Submissions list filters by clicking the reset button.
	 */
	@Keyword
	void resetDataSubmissionsListFilters() {
		KeywordUtil.logInfo("--- resetDataSubmissionsListFilters ---")
		try {
			if (WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/DataSubmissions/List/Filters/ResetFilters-Btn"), 5)) {
				WebUI.click(findTestObject("CRDC/AILocators/DataSubmissions/List/Filters/ResetFilters-Btn"))
				WebUI.delay(2)
				KeywordUtil.logInfo("resetDataSubmissionsListFilters: completed")
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("resetDataSubmissionsListFilters: " + e.getMessage())
		}
	}
}
