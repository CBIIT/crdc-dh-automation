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
 * Manage Programs: Keywords for navigating, filtering, and verifying the Manage Programs list page.
 * Phase 1: navigation, list readiness, and search/filter verification using AILocators.
 * Use mixed data strategy: default to existing program records; criteria/expectations can be fed from test data later.
 *
 * Call applyManageProgramsFilters before verifyManageProgramsFilterResults when filters are needed.
 * Expectation keys (expectAtLeastRows, rowContains) are optional.
 */
class ManagePrograms extends TestRunner {

	private static final String SECTION_MANAGE_PROGRAMS = "Manage Programs"
	private static final int DEFAULT_TIMEOUT = 30

	/**
	 * Navigate to Manage Programs from the global header and wait for the list page to be ready.
	 * Clicks the Manage Programs link and waits for the list container. Fails if the page does not load.
	 *
	 * Usage: CustomKeywords.'utilities.ManagePrograms.navigateToManagePrograms'()
	 */
	@Keyword
	void navigateToManagePrograms() {
		KeywordUtil.logInfo("--- navigateToManagePrograms ---")
		try {
			clickTab('CRDC/Login/UserProfile-Dd')
			clickTab("CRDC/AILocators/Global/Header/Desktop/ManagePrograms-Link")
			WebUI.delay(2)
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/List/Container"), DEFAULT_TIMEOUT)
			KeywordUtil.logInfo("Manage Programs list page loaded")
		} catch (Exception e) {
			KeywordUtil.markFailed("navigateToManagePrograms: page did not load - " + e.getMessage())
		}
	}

	/**
	 * Verify the Manage Programs list page shows core UI: list container, Add Program button, and table (or empty state).
	 * Reports each check to ValidationReport. Fails only if list container or required structure is missing.
	 *
	 * Usage: CustomKeywords.'utilities.ManagePrograms.verifyManageProgramsListPage'()
	 */
	@Keyword
	void verifyManageProgramsListPage() {
		KeywordUtil.logInfo("--- verifyManageProgramsListPage ---")
		try {
			boolean containerPresent = WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/List/Container"), 10)
			ValidationReport.addValidationReportItem(SECTION_MANAGE_PROGRAMS, "List container", "present", containerPresent ? "present" : "not found", containerPresent ? "Pass" : "Fail")
			if (!containerPresent) {
				KeywordUtil.markFailed("verifyManageProgramsListPage: list container not found")
				return
			}

			boolean addProgramPresent = WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/List/AddProgram-Btn"), 5)
			ValidationReport.addValidationReportItem(SECTION_MANAGE_PROGRAMS, "Add Program button", "present", addProgramPresent ? "present" : "not found", addProgramPresent ? "Pass" : "Fail")

			List<WebElement> rows = Utils.getListWebElements("CRDC/AILocators/GenericTable/TableRow")
			int rowCount = (rows == null) ? 0 : rows.size()
			ValidationReport.addValidationReportItem(SECTION_MANAGE_PROGRAMS, "Table rows visible", ">= 0", String.valueOf(rowCount), "Pass")
			KeywordUtil.logInfo("verifyManageProgramsListPage: container and Add Program verified; rows=" + rowCount)
		} catch (Exception e) {
			KeywordUtil.markFailed("verifyManageProgramsListPage: " + e.getMessage())
		}
	}

	/**
	 * Apply filter criteria to the Manage Programs list. Skips null or empty values.
	 * Supported criteria keys: programName, studyName.
	 *
	 * @param criteria Map with optional keys: programName (String), studyName (String)
	 */
	@Keyword
	void applyManageProgramsFilters(Map<String, String> criteria) {
		if (criteria == null || criteria.isEmpty()) {
			KeywordUtil.logInfo("applyManageProgramsFilters: no criteria; skipping")
			return
		}
		KeywordUtil.logInfo("--- applyManageProgramsFilters: " + criteria + " ---")
		try {
			if (criteria.programName != null && !criteria.programName.trim().isEmpty()) {
				String programName = criteria.programName.trim()
				if (WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/List/Filters/ProgramName-TxtBx"), 5)) {
					WebUI.clearText(findTestObject("CRDC/AILocators/Programs/List/Filters/ProgramName-TxtBx"))
					WebUI.setText(findTestObject("CRDC/AILocators/Programs/List/Filters/ProgramName-TxtBx"), programName)
					WebUI.delay(1)
				}
			}
			if (criteria.studyName != null && !criteria.studyName.trim().isEmpty()) {
				String studyName = criteria.studyName.trim()
				if (WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/List/Filters/StudyName-TxtBx"), 5)) {
					WebUI.clearText(findTestObject("CRDC/AILocators/Programs/List/Filters/StudyName-TxtBx"))
					WebUI.setText(findTestObject("CRDC/AILocators/Programs/List/Filters/StudyName-TxtBx"), studyName)
					WebUI.delay(1)
				}
			}
			WebUI.delay(2)
			KeywordUtil.logInfo("applyManageProgramsFilters: completed")
		} catch (Exception e) {
			KeywordUtil.markFailed("applyManageProgramsFilters: " + e.getMessage())
		}
	}

	/**
	 * Read currently visible Manage Programs table rows and return a list of row data (list of cell texts per row).
	 * Uses GenericTable; reads headers from table thead then each row's td text. Assumes caller is on Manage Programs list.
	 *
	 * @return List of rows, each row a List<String> of cell values in header order
	 */
	@Keyword
	List<List<String>> readManageProgramsTableRows() {
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
				KeywordUtil.logInfo("Manage Programs table row count: 0")
				return result
			}
			for (WebElement row : rows) {
				List<WebElement> cells = row.findElements(By.tagName("td"))
				List<String> cellTexts = new ArrayList<>()
				for (WebElement cell : cells) cellTexts.add(cell.getText().trim())
				result.add(cellTexts)
			}
			KeywordUtil.logInfo("Manage Programs table row count: " + result.size())
		} catch (Exception e) {
			KeywordUtil.logInfo("readManageProgramsTableRows: " + e.getMessage())
		}
		return result
	}

	/**
	 * Verify that the current table results match expectations. Call applyManageProgramsFilters first when filters are needed;
	 * otherwise verification runs on the current (possibly unfiltered) view.
	 * Expectations: expectAtLeastRows (Integer, optional), rowContains (String, optional – at least one row cell must contain this substring).
	 *
	 * @param expectations Map with optional keys: expectAtLeastRows (Integer), rowContains (String)
	 * @param reportSection Optional section name for ValidationReport (default: SECTION_MANAGE_PROGRAMS)
	 */
	@Keyword
	void verifyManageProgramsFilterResults(Map<String, Object> expectations, String reportSection = SECTION_MANAGE_PROGRAMS) {
		KeywordUtil.logInfo("--- verifyManageProgramsFilterResults ---")
		try {
			List<List<String>> rows = readManageProgramsTableRows()
			int count = rows.size()

			if (expectations != null) {
				if (expectations.expectAtLeastRows != null) {
					int min = ((Number) expectations.expectAtLeastRows).intValue()
					boolean pass = count >= min
					ValidationReport.addValidationReportItem(reportSection, "Row count >= " + min, String.valueOf(min), String.valueOf(count), pass ? "Pass" : "Fail")
					if (!pass) KeywordUtil.markFailed("verifyManageProgramsFilterResults: expected at least " + min + " rows, got " + count)
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
					if (!found) KeywordUtil.markFailed("verifyManageProgramsFilterResults: no row contains '" + substring + "'")
				}
			}
			KeywordUtil.logInfo("verifyManageProgramsFilterResults: completed; rows=" + count)
		} catch (Exception e) {
			KeywordUtil.markFailed("verifyManageProgramsFilterResults: " + e.getMessage())
		}
	}
}
