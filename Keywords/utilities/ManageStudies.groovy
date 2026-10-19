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

import com.kms.katalon.core.testobject.TestObject

import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.LinkedHashMap
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
	private static final String SECTION_MANAGE_STUDIES_EXPORT = "Manage Studies Export"
	private static final int DEFAULT_TIMEOUT = 30

	private static final String TOOLTIP_EXPORT_ENABLED = "Export the current list of Studies to CSV."
	private static final String TOOLTIP_EXPORT_EMPTY = "No results to export. No studies match your filters."
	private static final List<String> EXPECTED_MANAGE_STUDIES_CSV_HEADERS = Arrays.asList(
			"Name", "Acronym", "dbGaPID", "Access Type", "Principal Investigator", "ORCID",
			"Program", "Data Concierge", "Status", "Created Date")

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
	 * Supported criteria keys: studyName, program, dbGaP, status (All|Active|Inactive), accessType (All|Open|Controlled).
	 * Program is applied via dropdown (click then select option by text).
	 *
	 * @param criteria Map with optional keys: studyName (String), program (String), dbGaP (String), status (String), accessType (String)
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
			if (criteria.status != null && !criteria.status.trim().isEmpty()) {
				selectManageStudiesFilterOption("CRDC/AILocators/Studies/List/Filters/Status-Ddn", "status-option-" + criteria.status.trim())
			}
			if (criteria.accessType != null && !criteria.accessType.trim().isEmpty()) {
				selectManageStudiesFilterOption("CRDC/AILocators/Studies/List/Filters/AccessType-Ddn", "accessType-option-" + criteria.accessType.trim())
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

	/**
	 * Private helper: fill the Study Add/Edit form with the given fields.
	 * @param fields Map with keys: name, acronym, gpa, program, accessTypes (String or List: "Open"/"Controlled"), dbGaPID, piName, orcid, sameAsProgramDataConcierge (Boolean), dataConcierge (String), pendingConditions (List)
	 * @param onlyProvided If true (Edit), only set fields that appear in fields with non-null/non-empty values. If false (Add), set required fields and any optional present.
	 */
	private void fillStudyForm(Map<String, Object> fields, boolean onlyProvided) {
		WebDriver driver = DriverFactory.getWebDriver()
		int waitSec = 5

		if (!onlyProvided || (fields.name != null && fields.name.toString().trim().length() > 0)) {
			String v = fields.name != null ? fields.name.toString().trim() : null
			if (v != null && v.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/StudyName-TxtBx"), waitSec)) {
				WebUI.clearText(findTestObject("CRDC/AILocators/Studies/Detail/StudyName-TxtBx"))
				WebUI.setText(findTestObject("CRDC/AILocators/Studies/Detail/StudyName-TxtBx"), v)
				WebUI.delay(1)
			}
		}
		if (!onlyProvided || (fields.acronym != null && fields.acronym.toString().trim().length() > 0)) {
			String v = fields.acronym != null ? fields.acronym.toString().trim() : null
			if (v != null && v.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/Abbreviation-TxtBx"), waitSec)) {
				WebUI.clearText(findTestObject("CRDC/AILocators/Studies/Detail/Abbreviation-TxtBx"))
				WebUI.setText(findTestObject("CRDC/AILocators/Studies/Detail/Abbreviation-TxtBx"), v)
				WebUI.delay(1)
			}
		}
		if (!onlyProvided || fields.accessTypes != null) {
			String at = fields.accessTypes != null ? (fields.accessTypes instanceof List ? (fields.accessTypes as List).join(" ") : fields.accessTypes.toString()) : ""
			boolean doOpen = at.toLowerCase().contains("open")
			boolean doControlled = at.toLowerCase().contains("controlled")
			if (doOpen || doControlled) {
				if (WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/OpenAccess-Chkbx"), waitSec)) {
					if (doOpen) WebUI.check(findTestObject("CRDC/AILocators/Studies/Detail/OpenAccess-Chkbx"))
					else WebUI.uncheck(findTestObject("CRDC/AILocators/Studies/Detail/OpenAccess-Chkbx"))
					WebUI.delay(1)
				}
				if (WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/ControlledAccess-Chkbx"), waitSec)) {
					if (doControlled) WebUI.check(findTestObject("CRDC/AILocators/Studies/Detail/ControlledAccess-Chkbx"))
					else WebUI.uncheck(findTestObject("CRDC/AILocators/Studies/Detail/ControlledAccess-Chkbx"))
					WebUI.delay(1)
				}
			}
		}
		if (!onlyProvided || (fields.dbGaPID != null && fields.dbGaPID.toString().trim().length() > 0)) {
			String v = fields.dbGaPID != null ? fields.dbGaPID.toString().trim() : null
			if (v != null && v.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/DbGaPID-TxtBx"), waitSec)) {
				WebUI.clearText(findTestObject("CRDC/AILocators/Studies/Detail/DbGaPID-TxtBx"))
				WebUI.setText(findTestObject("CRDC/AILocators/Studies/Detail/DbGaPID-TxtBx"), v)
				WebUI.delay(1)
			}
		}
		if (!onlyProvided || (fields.gpa != null && fields.gpa.toString().trim().length() > 0)) {
			String v = fields.gpa != null ? fields.gpa.toString().trim() : null
			if (v != null && v.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/GPAName-TxtBx"), waitSec)) {
				WebUI.clearText(findTestObject("CRDC/AILocators/Studies/Detail/GPAName-TxtBx"))
				WebUI.setText(findTestObject("CRDC/AILocators/Studies/Detail/GPAName-TxtBx"), v)
				WebUI.delay(1)
			}
		}
		if (!onlyProvided || (fields.piName != null && fields.piName.toString().trim().length() > 0)) {
			String v = fields.piName != null ? fields.piName.toString().trim() : null
			if (v != null && v.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/PI-TxtBx"), waitSec)) {
				WebUI.clearText(findTestObject("CRDC/AILocators/Studies/Detail/PI-TxtBx"))
				WebUI.setText(findTestObject("CRDC/AILocators/Studies/Detail/PI-TxtBx"), v)
				WebUI.delay(1)
			}
		}
		if (!onlyProvided || (fields.orcid != null && fields.orcid.toString().trim().length() > 0)) {
			String v = fields.orcid != null ? fields.orcid.toString().trim() : null
			if (v != null && v.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/ORCID-TxtBx"), waitSec)) {
				WebUI.clearText(findTestObject("CRDC/AILocators/Studies/Detail/ORCID-TxtBx"))
				WebUI.setText(findTestObject("CRDC/AILocators/Studies/Detail/ORCID-TxtBx"), v)
				WebUI.delay(1)
			}
		}
		if (!onlyProvided || (fields.program != null && fields.program.toString().trim().length() > 0)) {
			String v = fields.program != null ? fields.program.toString().trim() : null
			if (v != null && v.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/Program-Ddn"), waitSec)) {
				WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Studies/Detail/Program-Ddn"), waitSec)
				WebUI.click(findTestObject("CRDC/AILocators/Studies/Detail/Program-Ddn"))
				WebUI.delay(1)
				String escaped = v.replace("'", "''")
				WebElement option = driver.findElement(By.xpath("//li[normalize-space(text())='" + escaped + "']"))
				option.click()
				WebUI.delay(1)
			}
		}
		if (!onlyProvided || fields.sameAsProgramDataConcierge != null || (fields.dataConcierge != null && fields.dataConcierge.toString().trim().length() > 0)) {
			boolean sameAs = true
			if (onlyProvided) {
				sameAs = (fields.sameAsProgramDataConcierge != null && fields.sameAsProgramDataConcierge == true) || (fields.dataConcierge == null || fields.dataConcierge.toString().trim().isEmpty())
			} else {
				sameAs = !(fields.sameAsProgramDataConcierge == false && fields.dataConcierge != null && fields.dataConcierge.toString().trim().length() > 0)
			}
			if (WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/SameAsProgramContact-Chkbx"), waitSec)) {
				if (sameAs) WebUI.check(findTestObject("CRDC/AILocators/Studies/Detail/SameAsProgramContact-Chkbx"))
				else {
					WebUI.uncheck(findTestObject("CRDC/AILocators/Studies/Detail/SameAsProgramContact-Chkbx"))
					WebUI.delay(1)
					String dc = fields.dataConcierge != null ? fields.dataConcierge.toString().trim() : null
					if (dc != null && dc.length() > 0 && WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/DataConcierge-Ddn"), waitSec)) {
						WebUI.click(findTestObject("CRDC/AILocators/Studies/Detail/DataConcierge-Ddn"))
						WebUI.delay(1)
						String escaped = dc.replace("'", "''")
						WebElement opt = driver.findElement(By.xpath("//li[normalize-space(text())='" + escaped + "']"))
						opt.click()
						WebUI.delay(1)
					}
				}
			}
		}
		if (!onlyProvided || (fields.pendingConditions != null && (fields.pendingConditions instanceof List ? (fields.pendingConditions as List).size() > 0 : true))) {
			if (fields.pendingConditions != null && fields.pendingConditions instanceof List) {
				List<?> pcs = fields.pendingConditions as List
				List<WebElement> chks = Utils.getListWebElements("CRDC/AILocators/Studies/Detail/PendingConditions-Chkbx")
				if (chks == null) chks = []
				for (int i = 0; i < chks.size() && i < pcs.size(); i++) {
					boolean wantChecked = (pcs.get(i) instanceof Boolean && pcs.get(i)) || (pcs.get(i) != null && pcs.get(i).toString().trim().toLowerCase() in ["true", "1", "yes"])
					boolean isChecked = chks.get(i).isSelected()
					if (!isChecked) {
						String ariaChecked = chks.get(i).getAttribute("aria-checked")
						if (ariaChecked != null && ariaChecked.equalsIgnoreCase("true")) isChecked = true
						String cls = chks.get(i).getAttribute("class")
						if (cls != null && cls.contains("Mui-checked")) isChecked = true
					}
					if (wantChecked != isChecked) {
						chks.get(i).click()
						WebUI.delay(1)
					}
				}
			}
		}
	}

	/**
	 * Create a new study by filling the Add Study form with required and optional fields, then verify the study appears on the Manage Studies list.
	 * Caller must navigate to Manage Studies first (e.g. navigateToManageStudies()). Call generateValidationReport() at the end of your test if desired.
	 *
	 * @param studyData Map. Required: name (String), gpa (String), program (String, e.g. "Not Applicable"), accessTypes (String or List: "Open", "Controlled", or both).
	 *        Optional: acronym, dbGaPID, piName, orcid, sameAsProgramDataConcierge (Boolean, default true), dataConcierge (String), pendingConditions (List).
	 * Usage: CustomKeywords.'utilities.ManageStudies.addNewStudy'([ name: 'My Study', gpa: 'GPA Name', program: 'Not Applicable', accessTypes: 'Open' ])
	 */
	@Keyword
	void addNewStudy(Map<String, Object> studyData) {
		KeywordUtil.logInfo("--- addNewStudy ---")
		if (studyData == null) {
			KeywordUtil.markFailed("addNewStudy: studyData is null")
			return
		}
		String name = studyData.name != null ? studyData.name.toString().trim() : ""
		String gpa = studyData.gpa != null ? studyData.gpa.toString().trim() : ""
		String program = studyData.program != null ? studyData.program.toString().trim() : ""
		Object at = studyData.accessTypes
		String accessTypesStr = at != null ? (at instanceof List ? (at as List).join(" ") : at.toString()) : ""
		if (name.isEmpty()) {
			KeywordUtil.markFailed("addNewStudy: required field 'name' is missing or empty")
			return
		}
		if (gpa.isEmpty()) {
			KeywordUtil.markFailed("addNewStudy: required field 'gpa' is missing or empty")
			return
		}
		if (program.isEmpty()) {
			KeywordUtil.markFailed("addNewStudy: required field 'program' is missing or empty")
			return
		}
		if (!accessTypesStr.toLowerCase().contains("open") && !accessTypesStr.toLowerCase().contains("controlled")) {
			KeywordUtil.markFailed("addNewStudy: required field 'accessTypes' must include 'Open' or 'Controlled'")
			return
		}
		try {
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/AddStudy-Btn"), 10)
			WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Studies/List/AddStudy-Btn"), 5)
			WebUI.click(findTestObject("CRDC/AILocators/Studies/List/AddStudy-Btn"))
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/Container"), 15)
			WebUI.delay(1)
			fillStudyForm(studyData, false)
			WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Studies/Detail/Save-Btn"), 5)
			WebUI.click(findTestObject("CRDC/AILocators/Studies/Detail/Save-Btn"))
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/Container"), 15)
			WebUI.delay(2)
			applyManageStudiesFilters([ studyName: name ])
			WebUI.delay(2)
			// Verify by row count: filter by full name returns 1+ row (Name column may be truncated in UI)
			verifyManageStudiesFilterResults([ expectAtLeastRows: 1 ])
			ValidationReport.addValidationReportItem(SECTION_MANAGE_STUDIES, "Study created", "visible in list", name, "Pass")
			KeywordUtil.logInfo("addNewStudy: completed and verified for '" + name + "'")
		} catch (Exception e) {
			KeywordUtil.markFailed("addNewStudy: " + e.getMessage())
		}
	}

	/** Hardcoded program name, dbGaP ID, and GPA for parameterless addNewStudy. */
	private static final String AUTO_PROGRAM = "auto_program"
	private static final String AUTO_PROGRAM_DBGAP_ID = "phs000001"
	private static final String AUTO_GPA = "Auto-GPA"
	public static String autoStudyName

	/**
	 * Create a new study with no parameters: unique name "autoQA-Study-&lt;yyyyMMdd-HHmmss&gt;", acronym "autoQA-&lt;yyyyMMdd-HHmmss&gt;",
	 * program "auto_program", dbGaP ID phs000001, access Open. Optional fields left empty. Caller must navigate to Manage Studies first.
	 *
	 * Usage: CustomKeywords.'utilities.ManageStudies.addNewStudy'()
	 */
	@Keyword
	void addNewStudy() {
		String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())
		String name = "autoQA-Study-" + ts
		autoStudyName = name
		String acronym = "autoQA-" + ts
		Map<String, Object> studyData = new LinkedHashMap<>()
		studyData.put("name", name)
		studyData.put("acronym", acronym)
		studyData.put("gpa", AUTO_GPA)
		studyData.put("program", "auto_program")
		studyData.put("accessTypes", "Open")
		studyData.put("dbGaPID", "phs000001")
		KeywordUtil.logInfo("--- addNewStudy: creating study '" + name + "' ---")
		try {
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/AddStudy-Btn"), 10)
			WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Studies/List/AddStudy-Btn"), 5)
			WebUI.click(findTestObject("CRDC/AILocators/Studies/List/AddStudy-Btn"))
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/Container"), 15)
			WebUI.delay(1)
			fillStudyForm(studyData, false)
			WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Studies/Detail/Save-Btn"), 5)
			WebUI.click(findTestObject("CRDC/AILocators/Studies/Detail/Save-Btn"))
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/Container"), 15)
			WebUI.delay(2)
			applyManageStudiesFilters([ studyName: name ])
			WebUI.delay(2)
			verifyManageStudiesFilterResults([ expectAtLeastRows: 1 ])
			ValidationReport.addValidationReportItem(SECTION_MANAGE_STUDIES, "Study created (autoQA)", "visible in list", name, "Pass")
			KeywordUtil.logInfo("addNewStudy: completed and verified for '" + name + "'")
		} catch (Exception e) {
			KeywordUtil.markFailed("addNewStudy: " + e.getMessage())
		}
	}

	/**
	 * Find a study by name on the Manage Studies list, open Edit, and apply only the provided updates (partial update).
	 * Caller must navigate to Manage Studies first (e.g. navigateToManageStudies()). Call generateValidationReport() at the end of your test if desired.
	 *
	 * @param studyName Exact or partial name to find in the table (first column).
	 * @param updates Map of field keys to new values (e.g. name, acronym, gpa, program, accessTypes, dbGaPID, piName, orcid). Only non-null/non-empty entries are applied.
	 * Usage: CustomKeywords.'utilities.ManageStudies.editExistingStudy'('My Study', [ acronym: 'MS', program: 'CTDC' ])
	 */
	@Keyword
	void editExistingStudy(String studyName, Map<String, Object> updates) {
		KeywordUtil.logInfo("--- editExistingStudy: " + studyName + " ---")
		if (studyName == null || studyName.trim().isEmpty()) {
			KeywordUtil.markFailed("editExistingStudy: studyName is null or empty")
			return
		}
		String searchName = studyName.trim()
		if (updates == null || updates.isEmpty()) {
			KeywordUtil.logInfo("editExistingStudy: no updates provided; opening Edit only")
		}
		try {
			applyManageStudiesFilters([ studyName: searchName ])
			WebUI.delay(2)
			WebElement matchingRow = null
			List<WebElement> rows = null
			for (int attempt = 0; attempt < 2 && matchingRow == null; attempt++) {
				if (attempt > 0) WebUI.delay(1)
				rows = Utils.getListWebElements("CRDC/AILocators/GenericTable/TableRow")
				if (rows == null || rows.isEmpty()) break
				// If filter returned exactly one row, it is the study we searched for
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
						// Truncated name column: visible text may be a prefix of searchName (e.g. "AutoStudy-TC1" for "AutoStudy-TC13")
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
						KeywordUtil.logInfo("editExistingStudy: stale row, retrying...")
						matchingRow = null
						break
					}
				}
			}
			if (matchingRow == null) {
				KeywordUtil.markFailed("editExistingStudy: no row found for study name '" + searchName + "'")
				return
			}
			WebElement editBtn = matchingRow.findElement(By.xpath(".//button[contains(text(),'Edit')]"))
			editBtn.click()
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/Detail/Container"), 15)
			WebUI.delay(1)
			if (updates != null && !updates.isEmpty()) {
				fillStudyForm(updates, true)
			}
			WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/Studies/Detail/Save-Btn"), 5)
			WebUI.click(findTestObject("CRDC/AILocators/Studies/Detail/Save-Btn"))
			WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/Container"), 15)
			WebUI.delay(2)
			KeywordUtil.logInfo("editExistingStudy: completed for '" + searchName + "'")
		} catch (Exception e) {
			KeywordUtil.markFailed("editExistingStudy: " + e.getMessage())
		}
	}

	/**
	 * Resets Manage Studies list filters to defaults: empty study/dbGaP, program All, status Active, access All.
	 */
	@Keyword
	void resetManageStudiesFilters() {
		KeywordUtil.logInfo("--- resetManageStudiesFilters ---")
		try {
			if (WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/Filters/StudyName-TxtBx"), 5)) {
				WebUI.clearText(findTestObject("CRDC/AILocators/Studies/List/Filters/StudyName-TxtBx"))
			}
			if (WebUI.waitForElementPresent(findTestObject("CRDC/AILocators/Studies/List/Filters/DbGaPID-TxtBx"), 5)) {
				WebUI.clearText(findTestObject("CRDC/AILocators/Studies/List/Filters/DbGaPID-TxtBx"))
			}
			selectManageStudiesFilterOption("CRDC/AILocators/Studies/List/Filters/Program-Ddn", "programID-option-All")
			selectManageStudiesFilterOption("CRDC/AILocators/Studies/List/Filters/Status-Ddn", "status-option-Active")
			selectManageStudiesFilterOption("CRDC/AILocators/Studies/List/Filters/AccessType-Ddn", "accessType-option-All")
			WebUI.delay(2)
			KeywordUtil.logInfo("resetManageStudiesFilters: completed")
		} catch (Exception e) {
			KeywordUtil.markFailed("resetManageStudiesFilters: " + e.getMessage())
		}
	}

	/**
	 * Opens a Manage Studies MUI filter dropdown and selects a menu item by its data-testid
	 * (e.g. status-option-Active, accessType-option-Open, programID-option-All).
	 *
	 * @param dropdownRepositoryPath Object Repository path to the filter select control
	 * @param optionDataTestId Full data-testid value of the MenuItem to click
	 */
	private void selectManageStudiesFilterOption(String dropdownRepositoryPath, String optionDataTestId) {
		WebDriver driver = DriverFactory.getWebDriver()
		if (WebUI.waitForElementPresent(findTestObject(dropdownRepositoryPath), 5)) {
			WebUI.waitForElementClickable(findTestObject(dropdownRepositoryPath), 5)
			WebUI.click(findTestObject(dropdownRepositoryPath))
			WebUI.delay(1)
			WebElement option = driver.findElement(By.xpath("//*[@data-testid='" + optionDataTestId.replace("'", "''") + "']"))
			option.click()
			WebUI.delay(1)
		}
	}

	/**
	 * Verifies the export control is visible, enabled, and shows the enabled-state tooltip when the table has rows.
	 */
	@Keyword
	void verifyManageStudiesExportUiWhenHasData() {
		KeywordUtil.logInfo("--- verifyManageStudiesExportUiWhenHasData ---")
		WebUIUtils.verifyElementDisplayed(findTestObject('CRDC/AILocators/Studies/List/ExportStudies-Btn'))
		TestObject exportBtn = findTestObject('CRDC/AILocators/Studies/List/ExportStudies-Btn')
		WebUIUtils.waitUntilStableForButtonEnabledOrDisabled(exportBtn, 'enabled', 30)
		WebUIUtils.verifyButtonEnabled(exportBtn, true)
		WebUIUtils.verifyTooltipOnHover(
				findTestObject('CRDC/AILocators/Studies/List/ExportStudies-Tooltip'),
				TOOLTIP_EXPORT_ENABLED,
				'Export the current list of Studies')
	}

	/**
	 * Verifies export is disabled with the empty-result tooltip. Call after filters yield zero rows.
	 */
	@Keyword
	void verifyManageStudiesExportUiWhenEmpty() {
		KeywordUtil.logInfo("--- verifyManageStudiesExportUiWhenEmpty ---")
		int total = WebUIUtils.getTableTotalEntryCount(0)
		if (total != 0) {
			KeywordUtil.markFailed("verifyManageStudiesExportUiWhenEmpty: expected table total 0, got " + total)
			return
		}
		TestObject exportBtn = findTestObject('CRDC/AILocators/Studies/List/ExportStudies-Btn')
		WebUIUtils.waitUntilStableForButtonEnabledOrDisabled(exportBtn, 'disabled', 30)
		WebUIUtils.verifyButtonEnabled(exportBtn, false)
		WebUIUtils.verifyTooltipOnHover(
				findTestObject('CRDC/AILocators/Studies/List/ExportStudies-Tooltip'),
				TOOLTIP_EXPORT_EMPTY,
				'No results to export')
	}

	/**
	 * Clears downloads, clicks export, verifies crdc-manage-studies-YYYY-MM-DD-HH-mm-ss.csv filename pattern.
	 *
	 * @return Absolute path to the downloaded CSV, or null on failure
	 */
	@Keyword
	String clickManageStudiesExportAndVerifyDownloadedFileName() {
		KeywordUtil.logInfo("--- clickManageStudiesExportAndVerifyDownloadedFileName ---")
		Utils.clearProjectDownloadsFolder()
		TestObject exportBtn = findTestObject('CRDC/AILocators/Studies/List/ExportStudies-Btn')
		if (!WebUIUtils.waitForElementClickableWithStaleRetry(exportBtn, 60)) {
			KeywordUtil.markFailed("clickManageStudiesExportAndVerifyDownloadedFileName: export button not clickable")
			return null
		}
		WebUI.click(exportBtn)
		String path = Utils.waitForFileInProjectDownloadsByPrefix('crdc-manage-studies-', '.csv', 60)
		if (path == null || path.isEmpty()) {
			KeywordUtil.markFailed("clickManageStudiesExportAndVerifyDownloadedFileName: no CSV in Downloads")
			return null
		}
		String fileName = new File(path).getName()
		def matcher = (fileName =~ /^crdc-manage-studies-\d{4}-\d{2}-\d{2}-\d{2}-\d{2}-\d{2}\.csv$/)
		if (!matcher.matches()) {
			ValidationReport.addValidationReportItem(SECTION_MANAGE_STUDIES_EXPORT, "Downloaded file name",
					"pattern: crdc-manage-studies-YYYY-MM-DD-HH-mm-ss.csv", fileName, "Fail")
			KeywordUtil.markFailed("clickManageStudiesExportAndVerifyDownloadedFileName: filename does not match pattern. Got: " + fileName)
			return null
		}
		ValidationReport.addValidationReportItem(SECTION_MANAGE_STUDIES_EXPORT, "Downloaded file name",
				"pattern: crdc-manage-studies-YYYY-MM-DD-HH-mm-ss.csv", fileName, "Pass")
		KeywordUtil.logInfo("clickManageStudiesExportAndVerifyDownloadedFileName: verified " + fileName)
		return path
	}

	/**
	 * Verifies CSV header columns match expected export columns and excludes Action.
	 *
	 * @param csvFilePath Absolute path to downloaded CSV
	 */
	@Keyword
	void verifyManageStudiesExportCsvHeaders(String csvFilePath) {
		KeywordUtil.logInfo("--- verifyManageStudiesExportCsvHeaders ---")
		if (csvFilePath == null || csvFilePath.isEmpty()) {
			KeywordUtil.markFailed("verifyManageStudiesExportCsvHeaders: csvFilePath is empty")
			return
		}
		File csvFile = new File(csvFilePath)
		List<String> headers = Utils.readCsvHeaderFields(csvFile)
		if (headers.isEmpty()) {
			KeywordUtil.markFailed("verifyManageStudiesExportCsvHeaders: no header row in " + csvFilePath)
			return
		}
		boolean hasAction = headers.any { it != null && "Action".equalsIgnoreCase(it.trim()) }
		String actionStatus = hasAction ? "Fail" : "Pass"
		ValidationReport.addValidationReportItem(SECTION_MANAGE_STUDIES_EXPORT, "CSV excludes Action column", "absent", hasAction ? "present" : "absent", actionStatus)
		if (hasAction) {
			KeywordUtil.markFailed("verifyManageStudiesExportCsvHeaders: Action column must not be exported")
			return
		}
		boolean headersMatch = headers.equals(EXPECTED_MANAGE_STUDIES_CSV_HEADERS)
		ValidationReport.addValidationReportItem(SECTION_MANAGE_STUDIES_EXPORT, "CSV header columns",
				EXPECTED_MANAGE_STUDIES_CSV_HEADERS.toString(), headers.toString(), headersMatch ? "Pass" : "Fail")
		if (!headersMatch) {
			KeywordUtil.markFailed("verifyManageStudiesExportCsvHeaders: header mismatch. Expected ${EXPECTED_MANAGE_STUDIES_CSV_HEADERS}, got ${headers}")
		}
	}

	/**
	 * Verifies CSV data row count matches the filtered UI total (export is not limited to the current page).
	 *
	 * @param expectedRows Total from getTableTotalEntryCount for current filters
	 * @param csvFilePath Absolute path to downloaded CSV
	 */
	@Keyword
	void verifyManageStudiesExportCsvRowCount(int expectedRows, String csvFilePath) {
		KeywordUtil.logInfo("--- verifyManageStudiesExportCsvRowCount: expected ${expectedRows} ---")
		if (csvFilePath == null || csvFilePath.isEmpty()) {
			KeywordUtil.markFailed("verifyManageStudiesExportCsvRowCount: csvFilePath is empty")
			return
		}
		File csvFile = new File(csvFilePath)
		int csvRows = Utils.countCsvDataRowsExcludingHeader(csvFile)
		String status = (csvRows == expectedRows) ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(SECTION_MANAGE_STUDIES_EXPORT, "CSV data rows vs filtered UI total",
				String.valueOf(expectedRows), String.valueOf(csvRows), status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyManageStudiesExportCsvRowCount: UI total ${expectedRows}, CSV data rows ${csvRows} in ${csvFile.name}")
		}
	}

	/**
	 * When the study filter is applied, verifies each exported row reflects that filter. The UI study search
	 * matches study name or acronym (often case-insensitive), so the token must appear in CSV Name or Acronym.
	 * Comparison is case-insensitive so fixtures such as acronym AUTOQA still match filter token autoQA.
	 *
	 * @param csvFilePath Absolute path to downloaded CSV
	 * @param studyFilterToken Substring from the study filter field (matched case-insensitively in Name or Acronym)
	 */
	@Keyword
	void verifyManageStudiesExportCsvNameColumnContains(String csvFilePath, String studyFilterToken) {
		if (studyFilterToken == null || studyFilterToken.trim().isEmpty()) {
			KeywordUtil.logInfo("verifyManageStudiesExportCsvNameColumnContains: skipped (no token)")
			return
		}
		String token = studyFilterToken.trim()
		String tokenLower = token.toLowerCase(java.util.Locale.ROOT)
		KeywordUtil.logInfo("--- verifyManageStudiesExportCsvNameColumnContains: token '" + token + "' (case-insensitive) ---")
		File csvFile = new File(csvFilePath)
		if (!csvFile.exists()) {
			KeywordUtil.markFailed("verifyManageStudiesExportCsvNameColumnContains: file not found")
			return
		}
		String text = new String(java.nio.file.Files.readAllBytes(csvFile.toPath()), java.nio.charset.StandardCharsets.UTF_8)
		List<String> records = Utils.splitCsvIntoRecords(text)
		if (records.size() <= 1) {
			ValidationReport.addValidationReportItem(SECTION_MANAGE_STUDIES_EXPORT, "CSV rows match study filter",
					"each row Name or Acronym contains '" + token + "' (ignore case)", "no data rows", "Pass")
			return
		}
		int violations = 0
		for (int i = 1; i < records.size(); i++) {
			List<String> fields = Utils.parseCsvRecordFields(records.get(i))
			String name = fields.size() > 0 ? (fields.get(0) ?: "") : ""
			String acronym = fields.size() > 1 ? (fields.get(1) ?: "") : ""
			String nameLower = name.toLowerCase(java.util.Locale.ROOT)
			String acronymLower = acronym.toLowerCase(java.util.Locale.ROOT)
			boolean rowMatches = nameLower.contains(tokenLower) || acronymLower.contains(tokenLower)
			if (!rowMatches) {
				violations++
			}
		}
		String status = violations == 0 ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(SECTION_MANAGE_STUDIES_EXPORT, "CSV rows match study filter",
				"each row Name or Acronym contains '" + token + "' (ignore case)", violations == 0 ? "all match" : violations + " row(s) mismatch", status)
		if (violations > 0) {
			KeywordUtil.markFailed("verifyManageStudiesExportCsvNameColumnContains: ${violations} row(s) missing '" + token + "' in Name or Acronym (case-insensitive)")
		}
	}

}
