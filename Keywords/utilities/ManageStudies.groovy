package utilities

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

import java.util.ArrayList
import java.util.List
import java.util.Map

/**
 * Manage Studies: Keywords for navigating, filtering, and verifying the Manage Study list page.
 * Phase 1: navigation, list readiness, and search/filter verification using AILocators.
 * Use mixed data strategy: default to existing study records; criteria/expectations can be fed from test data later.
 *
 * Call applyManageStudiesFilters before verifyManageStudiesFilterResults when filters are needed.
 * Expectation keys (expectAtLeastRows, rowContains) are optional.
 */
class ManageStudies extends TestRunner {

	private static final String SECTION_MANAGE_STUDIES = "Manage Studies"
	private static final int DEFAULT_TIMEOUT = 30

	/**
	 * Navigate to Manage Studies from the global header and wait for the list page to be ready.
	 * Clicks the Manage Studies link and waits for the list container. Fails if the page does not load.
	 *
	 * Usage: CustomKeywords.'utilities.ManageStudies.navigateToManageStudies'()
	 */
	@Keyword
	void navigateToManageStudies() {
		KeywordUtil.logInfo("--- navigateToManageStudies ---")
		try {
			clickTab('CRDC/Login/UserProfile-Dd')
			clickTab("CRDC/AILocators/Global/Header/Desktop/ManageStudies-Link")
			WebUI.delay(2)
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/Container"), DEFAULT_TIMEOUT)
			KeywordUtil.logInfo("Manage Studies list page loaded")
		} catch (Exception e) {
			KeywordUtil.markFailed("navigateToManageStudies: page did not load - " + e.getMessage())
		}
	}

	/**
	 * Verify the Manage Studies list page shows core UI: list container, Add Study button, and table (or empty state).
	 * Reports each check to ValidationReport. Fails only if list container or required structure is missing.
	 *
	 * Usage: CustomKeywords.'utilities.ManageStudies.verifyManageStudiesListPage'()
	 */
	@Keyword
	void verifyManageStudiesListPage() {
		KeywordUtil.logInfo("--- verifyManageStudiesListPage ---")
		try {
			boolean containerPresent = WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/Container"), 10)
			ValidationReport.addValidationReportItem(SECTION_MANAGE_STUDIES, "List container", "present", containerPresent ? "present" : "not found", containerPresent ? "Pass" : "Fail")
			if (!containerPresent) {
				KeywordUtil.markFailed("verifyManageStudiesListPage: list container not found")
				return
			}

			boolean addStudyPresent = WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/AddStudy-Btn"), 5)
			ValidationReport.addValidationReportItem(SECTION_MANAGE_STUDIES, "Add Study button", "present", addStudyPresent ? "present" : "not found", addStudyPresent ? "Pass" : "Fail")

			List<WebElement> rows = Utils.getListWebElements("CRDC/AILocators/GenericTable/TableRow")
			int rowCount = (rows == null) ? 0 : rows.size()
			ValidationReport.addValidationReportItem(SECTION_MANAGE_STUDIES, "Table rows visible", ">= 0", String.valueOf(rowCount), "Pass")
			KeywordUtil.logInfo("verifyManageStudiesListPage: container and Add Study verified; rows=" + rowCount)
		} catch (Exception e) {
			KeywordUtil.markFailed("verifyManageStudiesListPage: " + e.getMessage())
		}
	}

	/**
	 * Apply filter criteria to the Manage Studies list. Skips null or empty values.
	 * Supported criteria keys: studyName, program, dbGaP. Program is applied via dropdown (click then select option by text).
	 *
	 * @param criteria Map with optional keys: studyName (String), program (String), dbGaP (String)
	 */
	@Keyword
	void applyManageStudiesFilters(Map<String, String> criteria) {
		if (criteria == null || criteria.isEmpty()) {
			KeywordUtil.logInfo("applyManageStudiesFilters: no criteria; skipping")
			return
		}
		KeywordUtil.logInfo("--- applyManageStudiesFilters: " + criteria + " ---")
		try {
			WebDriver driver = DriverFactory.getWebDriver()

			if (criteria.studyName != null && !criteria.studyName.trim().isEmpty()) {
				String studyName = criteria.studyName.trim()
				if (WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/Filters/StudyName-TxtBx"), 5)) {
					WebUI.clearText(findTestObject("CRDC/AILocators/Studies/List/Filters/StudyName-TxtBx"))
					WebUI.setText(findTestObject("CRDC/AILocators/Studies/List/Filters/StudyName-TxtBx"), studyName)
					WebUI.delay(1)
				}
			}
			if (criteria.program != null && !criteria.program.trim().isEmpty()) {
				String program = criteria.program.trim()
				if (WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/Filters/Program-Ddn"), 5)) {
					WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Studies/List/Filters/Program-Ddn"), 5)
					WebUI.click(findTestObject("CRDC/AILocators/Studies/List/Filters/Program-Ddn"))
					WebUI.delay(1)
					WebElement option = driver.findElement(By.xpath("//li[normalize-space(text())='" + program.replace("'", "''") + "']"))
					option.click()
					WebUI.delay(1)
				}
			}
			if (criteria.dbGaP != null && !criteria.dbGaP.trim().isEmpty()) {
				String dbGaP = criteria.dbGaP.trim()
				if (WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/Filters/DbGaPID-TxtBx"), 5)) {
					WebUI.clearText(findTestObject("CRDC/AILocators/Studies/List/Filters/DbGaPID-TxtBx"))
					WebUI.setText(findTestObject("CRDC/AILocators/Studies/List/Filters/DbGaPID-TxtBx"), dbGaP)
					WebUI.delay(1)
				}
			}
			WebUI.delay(2)
			KeywordUtil.logInfo("applyManageStudiesFilters: completed")
		} catch (Exception e) {
			KeywordUtil.markFailed("applyManageStudiesFilters: " + e.getMessage())
		}
	}

	/**
	 * Read currently visible Manage Studies table rows and return a list of row data (list of cell texts per row).
	 * Uses GenericTable; reads headers from table thead then each row's td text. Assumes caller is on Manage Studies list.
	 *
	 * @return List of rows, each row a List<String> of cell values in header order
	 */
	@Keyword
	List<List<String>> readManageStudiesTableRows() {
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
				KeywordUtil.logInfo("Manage Studies table row count: 0")
				return result
			}
			for (WebElement row : rows) {
				List<WebElement> cells = row.findElements(By.tagName("td"))
				List<String> cellTexts = new ArrayList<>()
				for (WebElement cell : cells) cellTexts.add(cell.getText().trim())
				result.add(cellTexts)
			}
			KeywordUtil.logInfo("Manage Studies table row count: " + result.size())
		} catch (Exception e) {
			KeywordUtil.logInfo("readManageStudiesTableRows: " + e.getMessage())
		}
		return result
	}

	/**
	 * Verify that the current table results match expectations. Call applyManageStudiesFilters first when filters are needed;
	 * otherwise verification runs on the current (possibly unfiltered) view.
	 * Expectations: expectAtLeastRows (Integer, optional), rowContains (String, optional – at least one row cell must contain this substring).
	 *
	 * @param expectations Map with optional keys: expectAtLeastRows (Integer), rowContains (String)
	 * @param reportSection Optional section name for ValidationReport (default: SECTION_MANAGE_STUDIES)
	 */
	@Keyword
	void verifyManageStudiesFilterResults(Map<String, Object> expectations, String reportSection = SECTION_MANAGE_STUDIES) {
		KeywordUtil.logInfo("--- verifyManageStudiesFilterResults ---")
		try {
			List<List<String>> rows = readManageStudiesTableRows()
			int count = rows.size()

			if (expectations != null) {
				if (expectations.expectAtLeastRows != null) {
					int min = ((Number) expectations.expectAtLeastRows).intValue()
					boolean pass = count >= min
					ValidationReport.addValidationReportItem(reportSection, "Row count >= " + min, String.valueOf(min), String.valueOf(count), pass ? "Pass" : "Fail")
					if (!pass) KeywordUtil.markFailed("verifyManageStudiesFilterResults: expected at least " + min + " rows, got " + count)
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
					if (!found) KeywordUtil.markFailed("verifyManageStudiesFilterResults: no row contains '" + substring + "'")
				}
			}
			KeywordUtil.logInfo("verifyManageStudiesFilterResults: completed; rows=" + count)
		} catch (Exception e) {
			KeywordUtil.markFailed("verifyManageStudiesFilterResults: " + e.getMessage())
		}
	}
}
