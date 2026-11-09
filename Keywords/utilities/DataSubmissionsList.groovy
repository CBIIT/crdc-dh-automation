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
import org.openqa.selenium.interactions.Actions

import java.util.ArrayList
import java.util.List
import java.util.Map
import java.time.Duration

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
		WebUI.delay(2)
		if (criteria == null || criteria.isEmpty()) {
			KeywordUtil.logInfo("applyDataSubmissionsListFilters: no criteria; skipping")
			return
		}
		KeywordUtil.logInfo("--- applyDataSubmissionsListFilters: " + criteria + " ---")
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			String listFilters = "CRDC/AILocators/DataSubmissions/List/Filters"

			if (criteria.submitterName != null && !criteria.submitterName.trim().isEmpty()) {
				String v = criteria.submitterName.trim().replace("'", "''")
				if (WebUI.waitForElementPresent(findTestObject(listFilters + "/SubmitterName-Ddn"), 5)) {
					WebUI.waitForElementClickable(findTestObject(listFilters + "/SubmitterName-Ddn"), 5)
					WebUI.delay(1)
					WebElement submitterDdn = WebUI.findWebElement(findTestObject(listFilters + "/SubmitterName-Ddn"), 5)
					new Actions(driver).moveToElement(submitterDdn).click().perform()
					WebUI.delay(1)
					WebElement opt = driver.findElement(By.xpath("//li[normalize-space(text())='" + v + "']"))
					opt.click()
					WebUI.delay(1)
				}
			}
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
							driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0))
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
							driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10))
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
								WebElement opt = new WebDriverWait(driver, Duration.ofSeconds(optionWaitSeconds)).until(ExpectedConditions.presenceOfNestedElementLocatedBy(listboxRef, By.xpath(".//li[normalize-space(text())='" + escaped + "']")))
								opt.click()
								WebUI.delay(1)
							} catch (Exception ex) {
								KeywordUtil.logInfo("applyDataSubmissionsListFilters: Status option not found: '" + raw + "' - " + ex.getMessage())
							}
						}
						// Close dropdown			
						try {
							new Actions(driver).sendKeys(Keys.ESCAPE).perform()
						} catch (Exception ex) {
							KeywordUtil.logInfo("applySubmissionRequestsListFilters: ESC to close Status dropdown: " + ex.getMessage())
						}
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
			waitForGenericTableReady(DriverFactory.getWebDriver(), DEFAULT_TIMEOUT)
			KeywordUtil.logInfo("applyDataSubmissionsListFilters: completed")
		} catch (Exception e) {
			KeywordUtil.markFailed("applyDataSubmissionsListFilters: " + e.getMessage())
		}
	}

	/**
	 * Wait until GenericTable finishes refetch: suspense overlay and loading rows clear, then row text is stable.
	 * During refetch with existing rows the UI keeps old generic-table-row nodes until swap; the suspense loader
	 * (data-testid generic-table-suspense-loader) is the reliable in-flight signal.
	 */
	private static void waitForGenericTableReady(WebDriver driver, int timeoutSeconds) {
		JavascriptExecutor js = (JavascriptExecutor) driver
		long deadline = System.currentTimeMillis() + (long) timeoutSeconds * 1000L
		String snapshotScript =
			"var rows = document.querySelectorAll(\"[data-testid='generic-table-row']\");" +
			"var parts = [];" +
			"for (var i = 0; i < rows.length; i++) { parts.push((rows[i].innerText || '').trim()); }" +
			"return parts.join('\\n');"

		String baselineSnapshot = (String) js.executeScript(snapshotScript)
		if (baselineSnapshot == null) baselineSnapshot = ""

		// Debounced filter handlers (e.g. submission name) may start refetch shortly after the last interaction
		Thread.sleep(600)

		long activityDeadline = Math.min(deadline, System.currentTimeMillis() + 5000)
		while (System.currentTimeMillis() < activityDeadline) {
			Boolean loaderVisible = (Boolean) js.executeScript(
				"var el = document.querySelector(\"[data-testid='generic-table-suspense-loader']\");" +
				"return el != null && el.offsetParent !== null;"
			)
			Long loadingRows = (Long) js.executeScript(
				"return document.querySelectorAll(\"[data-testid='loading-row']\").length;"
			)
			String snapshot = (String) js.executeScript(snapshotScript)
			if (snapshot == null) snapshot = ""
			if (Boolean.TRUE.equals(loaderVisible) || (loadingRows != null && loadingRows > 0) || !snapshot.equals(baselineSnapshot)) {
				break
			}
			Thread.sleep(250)
		}

		while (System.currentTimeMillis() < deadline) {
			Boolean loaderVisible = (Boolean) js.executeScript(
				"var el = document.querySelector(\"[data-testid='generic-table-suspense-loader']\");" +
				"return el != null && el.offsetParent !== null;"
			)
			if (!Boolean.TRUE.equals(loaderVisible)) break
			Thread.sleep(250)
		}

		try {
			Utils.waitForElementToDisappear('CRDC/AILocators/GenericTable/LoadingRow', timeoutSeconds)
		} catch (Exception e) {
			KeywordUtil.logInfo("waitForGenericTableReady: loading row wait: " + e.getMessage())
		}

		String lastSnapshot = null
		int stableSamples = 0
		while (System.currentTimeMillis() < deadline && stableSamples < 2) {
			String snapshot = (String) js.executeScript(snapshotScript)
			if (snapshot == null) snapshot = ""
			if (snapshot.equals(lastSnapshot)) {
				stableSamples++
			} else {
				stableSamples = 0
				lastSnapshot = snapshot
			}
			if (stableSamples < 2) Thread.sleep(500)
		}
		KeywordUtil.logInfo("waitForGenericTableReady: content stable=" + (stableSamples >= 2))
	}

	/**
	 * Reads all GenericTable rows in one JS call (same approach as readDataViewTableRows in DataSubmissions).
	 * Avoids StaleElementReferenceException from holding WebElement references across React re-renders.
	 */
	private static List<List<String>> readGenericTableRowsViaJs(WebDriver driver) {
		JavascriptExecutor js = (JavascriptExecutor) driver
		String script =
			"var rows = document.querySelectorAll(\"[data-testid='generic-table-row']\");" +
			"var out = [];" +
			"for (var i = 0; i < rows.length; i++) {" +
			"  var cells = rows[i].querySelectorAll('td');" +
			"  var row = [];" +
			"  for (var j = 0; j < cells.length; j++) {" +
			"    var cell = cells[j];" +
			"    var trunc = cell.querySelector(\"[data-testid='truncated-text-wrapper']\");" +
			"    var val = '';" +
			"    if (trunc) {" +
			"      var aria = trunc.getAttribute('aria-label');" +
			"      val = (aria && aria.trim()) ? aria.trim() : (cell.innerText || '').trim();" +
			"    } else {" +
			"      val = (cell.innerText || '').trim();" +
			"    }" +
			"    row.push(val);" +
			"  }" +
			"  out.push(row);" +
			"}" +
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
	 * Read currently visible Data Submissions list table rows (GenericTable). Returns list of row data.
	 * Waits for table refetch to finish, then reads all rows in one JS batch to avoid stale element flakes.
	 *
	 * @return List of rows, each row a List<String> of cell values in header order
	 */
	@Keyword
	List<List<String>> readDataSubmissionsListTableRows() {
		List<List<String>> result = new ArrayList<>()
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			try {
				Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
			} catch (Exception e) {
				KeywordUtil.logInfo("readDataSubmissionsListTableRows: loading icon wait: " + e.getMessage())
			}
			waitForGenericTableReady(driver, DEFAULT_TIMEOUT)

			for (int readAttempt = 1; readAttempt <= 3; readAttempt++) {
				result = readGenericTableRowsViaJs(driver)
				if (!result.isEmpty()) break
				KeywordUtil.logInfo("readDataSubmissionsListTableRows: 0 rows on attempt " + readAttempt + ", re-waiting for table...")
				waitForGenericTableReady(driver, 10)
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
	 * Preflight: fail and stop if any Data Submissions for the given study are already Submitted or Released.
	 * Filters the list by Status Submitted and Released and the Study box (dbGaP / study name / acronym).
	 * Does not write ValidationReport items; leftover rows are environment state, not a product assertion.
	 *
	 * @param studyFilter Study name, acronym, or dbGaP ID token for the Study filter (must be unique enough to avoid other studies)
	 */
	@Keyword
	void failIfSubmittedOrReleasedExistForStudy(String studyFilter) {
		if (studyFilter == null || studyFilter.trim().isEmpty()) {
			KeywordUtil.markFailedAndStop("failIfSubmittedOrReleasedExistForStudy: studyFilter is required")
		}
		String token = studyFilter.trim()
		KeywordUtil.logInfo("--- failIfSubmittedOrReleasedExistForStudy: " + token + " ---")
		navigateToDataSubmissionsList()
		applyDataSubmissionsListFilters([status: 'Submitted, Released', dbGaP: token])
		List<List<String>> rows = readDataSubmissionsListTableRows()
		int count = (rows == null) ? 0 : rows.size()
		if (count > 0) {
			int logLimit = Math.min(count, 5)
			for (int i = 0; i < logLimit; i++) {
				KeywordUtil.logInfo("leftover DS row " + (i + 1) + ": " + rows.get(i).join(" | "))
			}
			KeywordUtil.markFailedAndStop("There are existing data submissions in Submitted or Released status for study '" + token + "' that need to be cleared.")
		}
		KeywordUtil.logInfo("failIfSubmittedOrReleasedExistForStudy: no Submitted or Released DS for study '" + token + "'")
		resetDataSubmissionsListFilters()
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
