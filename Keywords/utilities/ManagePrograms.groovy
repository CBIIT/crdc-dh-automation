package utilities

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.TimeoutException

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.LinkedHashMap
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
	// Xpaths for addNewProgram result messages (must match Object Repository CRDC/AILocators/Programs/Detail)
	private static final String XPATH_PROGRAM_ADDED_SUCCESS = "//*[contains(text(),'This program has been successfully added.')]"
	private static final String XPATH_DUPLICATE_NAME_ERROR = "//*[contains(text(),'An organization with the same name already exists')]"

	/** Returns true if element matching xpath appears within timeoutSeconds; false otherwise. No Katalon keyword logging. */
	private static boolean waitForElementPresentByXpath(WebDriver driver, String xpath, int timeoutSeconds) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, timeoutSeconds)
			wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)))
			return true
		} catch (TimeoutException e) {
			return false
		}
	}

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

	/**
	 * Filter Manage Programs by program name, then assert at least one row contains that name (reuses
	 * {@link #applyManageProgramsFilters} and {@link #verifyManageProgramsFilterResults}).
	 * Caller must already be on the Manage Programs list (e.g. {@link #navigateToManagePrograms()}).
	 *
	 * @param programName Full or unique program name string (e.g. {@code utilities.SubmissionRequest#lastEnteredProgramName})
	 */
	@Keyword
	void filterByProgramNameAndVerifyProgramAppearsInList(String programName) {
		if (programName == null || !programName.trim()) {
			KeywordUtil.markFailed("filterByProgramNameAndVerifyProgramAppearsInList: programName is required.")
			return
		}
		String name = programName.trim()
		KeywordUtil.logInfo("--- filterByProgramNameAndVerifyProgramAppearsInList: " + name + " ---")
		applyManageProgramsFilters([programName: name] as Map<String, String>)
		verifyManageProgramsFilterResults([expectAtLeastRows: 1, rowContains: name] as Map<String, Object>)
	}

	/**
	 * Private helper: fill the Program Add/Edit form with the given fields.
	 * @param fields Map with keys: name, abbreviation, description, dataConcierge (String), status (String: Active/Inactive; used only when skipStatusDropdown is false)
	 * @param onlyProvided If true (Edit), only set fields that appear in fields with non-null/non-empty values. If false (Add), set required and any optional present.
	 * @param skipStatusDropdown If true (Add Program), do not set or check Status dropdown. If false (Edit), set Status when provided and enabled.
	 */
	private void fillProgramForm(Map<String, Object> fields, boolean onlyProvided, boolean skipStatusDropdown) {
		WebDriver driver = DriverFactory.getWebDriver()
		int waitSec = 5

		if (!onlyProvided || (fields.name != null && fields.name.toString().trim().length() > 0)) {
			String v = fields.name != null ? fields.name.toString().trim() : null
			if (v != null && v.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/Detail/ProgramName-TxtBx"), waitSec)) {
				WebUI.clearText(findTestObject("CRDC/AILocators/Programs/Detail/ProgramName-TxtBx"))
				WebUI.setText(findTestObject("CRDC/AILocators/Programs/Detail/ProgramName-TxtBx"), v)
				WebUI.delay(1)
			}
		}
		if (!onlyProvided || (fields.abbreviation != null && fields.abbreviation.toString().trim().length() > 0)) {
			String v = fields.abbreviation != null ? fields.abbreviation.toString().trim() : null
			if (v != null && v.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/Detail/Abbreviation-TxtBx"), waitSec)) {
				WebUI.clearText(findTestObject("CRDC/AILocators/Programs/Detail/Abbreviation-TxtBx"))
				WebUI.setText(findTestObject("CRDC/AILocators/Programs/Detail/Abbreviation-TxtBx"), v)
				WebUI.delay(1)
			}
		}
		if (!onlyProvided || (fields.description != null && fields.description.toString().trim().length() > 0)) {
			String v = fields.description != null ? fields.description.toString().trim() : null
			if (v != null && v.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/Detail/Description-TxtBx"), waitSec)) {
				WebUI.clearText(findTestObject("CRDC/AILocators/Programs/Detail/Description-TxtBx"))
				WebUI.setText(findTestObject("CRDC/AILocators/Programs/Detail/Description-TxtBx"), v)
				WebUI.delay(1)
			}
		}
		if (!onlyProvided || (fields.dataConcierge != null && fields.dataConcierge.toString().trim().length() > 0)) {
			String v = fields.dataConcierge != null ? fields.dataConcierge.toString().trim() : null
			if (v != null && v.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/Detail/DataConcierge-Ddn"), waitSec)) {
				WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Programs/Detail/DataConcierge-Ddn"), waitSec)
				WebUI.click(findTestObject("CRDC/AILocators/Programs/Detail/DataConcierge-Ddn"))
				WebUI.delay(1)
				String escaped = v.replace("'", "''")
				WebElement option = driver.findElement(By.xpath("//li[normalize-space(text())='" + escaped + "']"))
				option.click()
				WebUI.delay(1)
			}
		}
		if (!skipStatusDropdown) {
			// Status: only click and set if dropdown is not grayed out (on Edit it may be enabled)
			String statusVal = (fields.status != null && fields.status.toString().trim().length() > 0) ? fields.status.toString().trim() : (onlyProvided ? null : "Active")
			if (statusVal != null && statusVal.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/Detail/Status-Ddn"), waitSec)) {
				String statusXpath = TestRunner.givexpath("CRDC/AILocators/Programs/Detail/Status-Ddn")
				WebElement statusEl = driver.findElement(By.xpath(statusXpath))
				if (statusEl.isEnabled()) {
					WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Programs/Detail/Status-Ddn"), waitSec)
					WebUI.click(findTestObject("CRDC/AILocators/Programs/Detail/Status-Ddn"))
					WebUI.delay(1)
					String escaped = statusVal.replace("'", "''")
					WebElement option = driver.findElement(By.xpath("//li[normalize-space(text())='" + escaped + "']"))
					option.click()
					WebUI.delay(1)
				}
			}
		}
	}

	/**
	 * Create a new program by filling the Add Program form with required and optional fields, then verify the program appears on the Manage Programs list.
	 * Caller must navigate to Manage Programs first (e.g. navigateToManagePrograms()). Call generateValidationReport() at the end of your test if desired.
	 *
	 * @param programData Map. Required: name (String), abbreviation (String). Optional: description, dataConcierge (String). Status is not set or checked on Add.
	 * Usage: CustomKeywords.'utilities.ManagePrograms.addNewProgram'([ name: 'My Program', abbreviation: 'MP' ])
	 */
	@Keyword
	void addNewProgram(Map<String, Object> programData) {
		KeywordUtil.logInfo("--- addNewProgram ---")
		if (programData == null) {
			KeywordUtil.markFailed("addNewProgram: programData is null")
			return
		}
		String name = programData.name != null ? programData.name.toString().trim() : ""
		String abbreviation = programData.abbreviation != null ? programData.abbreviation.toString().trim() : ""
		if (name.isEmpty()) {
			KeywordUtil.markFailed("addNewProgram: required field 'name' is missing or empty")
			return
		}
		if (abbreviation.isEmpty()) {
			KeywordUtil.markFailed("addNewProgram: required field 'abbreviation' is missing or empty")
			return
		}
		try {
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/List/AddProgram-Btn"), 10)
			WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Programs/List/AddProgram-Btn"), 5)
			WebUI.click(findTestObject("CRDC/AILocators/Programs/List/AddProgram-Btn"))
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/Detail/Container"), 15)
			WebUI.delay(1)
			fillProgramForm(programData, false, true)
			WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Programs/Detail/Save-Btn"), 5)
			WebUI.click(findTestObject("CRDC/AILocators/Programs/Detail/Save-Btn"))
			WebUI.delay(3)
			WebDriver driver = DriverFactory.getWebDriver()
			// 1) First look for success message ("This program has been successfully added.")
			// Use driver-based wait to avoid Katalon keyword "not present" warnings.
			boolean successMsgPresent = waitForElementPresentByXpath(driver, XPATH_PROGRAM_ADDED_SUCCESS, 10)
			if (successMsgPresent) {
				ValidationReport.addValidationReportItem(SECTION_MANAGE_PROGRAMS, "Program created", "success message", name, "Pass")
				KeywordUtil.logInfo("addNewProgram: success message verified for '" + name + "'")
				return
			}
			// 2) Success message not present; look for duplicate-name error ("An organization with the same name already exists").
			boolean duplicateErrorPresent = waitForElementPresentByXpath(driver, XPATH_DUPLICATE_NAME_ERROR, 3)
			if (duplicateErrorPresent) {
				ValidationReport.addValidationReportItem(SECTION_MANAGE_PROGRAMS, "Program creation", "unique name", "Program with the same name already exists: " + name, "Fail")
				KeywordUtil.logInfo("addNewProgram: duplicate name detected - '" + name + "' already exists; closing form via Cancel")
				WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Programs/Detail/Cancel-Btn"), 5)
				WebUI.click(findTestObject("CRDC/AILocators/Programs/Detail/Cancel-Btn"))
				WebUI.delay(2)
				return
			}
			// 3) Neither message present: fail the test.
			KeywordUtil.markFailed("addNewProgram: expected either success message 'This program has been successfully added.' or error 'An organization with the same name already exists'; neither was found")
		} catch (Exception e) {
			KeywordUtil.markFailed("addNewProgram: " + e.getMessage())
		}
	}

	/**
	 * Create a new program with no parameters: unique name "autoQA-Program-&lt;yyyyMMdd-HHmmss&gt;", abbreviation "autoQA-&lt;yyyyMMdd-HHmmss&gt;".
	 * Optional fields (description, dataConcierge) left empty; Status is not set on Add. Caller must navigate to Manage Programs first.
	 *
	 * Usage: CustomKeywords.'utilities.ManagePrograms.addNewProgram'()
	 */
	@Keyword
	void addNewProgram() {
		String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())
		String name = "autoQA-Program-" + ts
		String abbreviation = "autoQA-" + ts
		Map<String, Object> programData = new LinkedHashMap<>()
		programData.put("name", name)
		programData.put("abbreviation", abbreviation)
		KeywordUtil.logInfo("--- addNewProgram: creating program '" + name + "' ---")
		try {
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/List/AddProgram-Btn"), 10)
			WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Programs/List/AddProgram-Btn"), 5)
			WebUI.click(findTestObject("CRDC/AILocators/Programs/List/AddProgram-Btn"))
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/Detail/Container"), 15)
			WebUI.delay(1)
			fillProgramForm(programData, false, true)
			WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Programs/Detail/Save-Btn"), 5)
			WebUI.click(findTestObject("CRDC/AILocators/Programs/Detail/Save-Btn"))
			WebUI.delay(3)
			WebDriver driver = DriverFactory.getWebDriver()
			boolean successMsgPresent = waitForElementPresentByXpath(driver, XPATH_PROGRAM_ADDED_SUCCESS, 10)
			if (successMsgPresent) {
				ValidationReport.addValidationReportItem(SECTION_MANAGE_PROGRAMS, "Program created (autoQA)", "success message", name, "Pass")
				KeywordUtil.logInfo("addNewProgram: success message verified for '" + name + "'")
				return
			}
			boolean duplicateErrorPresent = waitForElementPresentByXpath(driver, XPATH_DUPLICATE_NAME_ERROR, 3)
			if (duplicateErrorPresent) {
				ValidationReport.addValidationReportItem(SECTION_MANAGE_PROGRAMS, "Program creation", "unique name", "Program with the same name already exists: " + name, "Fail")
				KeywordUtil.logInfo("addNewProgram: duplicate name detected - '" + name + "' already exists; closing form via Cancel")
				WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Programs/Detail/Cancel-Btn"), 5)
				WebUI.click(findTestObject("CRDC/AILocators/Programs/Detail/Cancel-Btn"))
				WebUI.delay(2)
				return
			}
			KeywordUtil.markFailed("addNewProgram: expected either success message 'This program has been successfully added.' or error 'An organization with the same name already exists'; neither was found")
		} catch (Exception e) {
			KeywordUtil.markFailed("addNewProgram: " + e.getMessage())
		}
	}

	/**
	 * Find a program by name on the Manage Programs list, open Edit, and apply only the provided updates (partial update).
	 * Caller must navigate to Manage Programs first (e.g. navigateToManagePrograms()). Call generateValidationReport() at the end of your test if desired.
	 *
	 * @param programName Exact or partial name to find in the table (first column).
	 * @param updates Map of field keys to new values (e.g. name, abbreviation, description, status, dataConcierge). Only non-null/non-empty entries are applied.
	 * Usage: CustomKeywords.'utilities.ManagePrograms.editExistingProgram'('My Program', [ abbreviation: 'MP2', status: 'Active' ])
	 */
	@Keyword
	void editExistingProgram(String programName, Map<String, Object> updates) {
		KeywordUtil.logInfo("--- editExistingProgram: " + programName + " ---")
		if (programName == null || programName.trim().isEmpty()) {
			KeywordUtil.markFailed("editExistingProgram: programName is null or empty")
			return
		}
		String searchName = programName.trim()
		if (updates == null || updates.isEmpty()) {
			KeywordUtil.logInfo("editExistingProgram: no updates provided; opening Edit only")
		}
		try {
			applyManageProgramsFilters([ programName: searchName ])
			WebUI.delay(2)
			WebElement matchingRow = null
			List<WebElement> rows = null
			for (int attempt = 0; attempt < 2 && matchingRow == null; attempt++) {
				if (attempt > 0) WebUI.delay(1)
				rows = Utils.getListWebElements("CRDC/AILocators/GenericTable/TableRow")
				if (rows == null || rows.isEmpty()) break
				if (rows.size() == 1) {
					matchingRow = rows.get(0)
					break
				}
				for (WebElement row : rows) {
					try {
						List<WebElement> cells = row.findElements(By.tagName("td"))
						boolean rowMatches = false
						for (WebElement cell : cells) {
							String cellText = cell.getText().trim()
							if (cellText != null && cellText.contains(searchName)) {
								rowMatches = true
								break
							}
						}
						if (!rowMatches && cells.size() >= 1) {
							String firstCell = cells.get(0).getText().trim()
							if (firstCell != null && firstCell.length() >= 10 && searchName.contains(firstCell))
								rowMatches = true
						}
						if (rowMatches) {
							matchingRow = row
							break
						}
					} catch (StaleElementReferenceException e) {
						KeywordUtil.logInfo("editExistingProgram: stale row, retrying...")
						matchingRow = null
						break
					}
				}
			}
			if (matchingRow == null) {
				KeywordUtil.markFailed("editExistingProgram: no row found for program name '" + searchName + "'")
				return
			}
			WebElement editBtn = matchingRow.findElement(By.xpath(".//button[contains(text(),'Edit')]"))
			editBtn.click()
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/Detail/Container"), 15)
			WebUI.delay(1)
			if (updates != null && !updates.isEmpty()) {
				fillProgramForm(updates, true, false)
			}
			WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Programs/Detail/Save-Btn"), 5)
			WebUI.click(findTestObject("CRDC/AILocators/Programs/Detail/Save-Btn"))
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Programs/List/Container"), 15)
			WebUI.delay(2)
			KeywordUtil.logInfo("editExistingProgram: completed for '" + searchName + "'")
		} catch (Exception e) {
			KeywordUtil.markFailed("editExistingProgram: " + e.getMessage())
		}
	}
}
