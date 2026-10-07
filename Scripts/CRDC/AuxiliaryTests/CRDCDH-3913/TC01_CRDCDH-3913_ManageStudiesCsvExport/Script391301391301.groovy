import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * CRDCDH-3913 TC01 — Manage Studies CSV export
 *
 * Roles: Admin (full flow including filtered export), then Federal Lead (export UI + default-list CSV).
 * Env data: study filter token autoQA, program AutoTest-Program, status Active, accessType Open.
 */

// --- Setup: fresh browser, Admin login ---
WebUI.closeBrowser()
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Admin')
WebUI.delay(2)

// Step 1: Open Manage Studies and confirm list page structure (container, Add Study, table).
CustomKeywords.'utilities.ManageStudies.navigateToManageStudies'()
CustomKeywords.'utilities.ManageStudies.verifyManageStudiesListPage'()

// Step 2: With default Active filter and rows present — verify export icon is shown, enabled, and tooltip
//         "Export the current list of Studies to CSV."
CustomKeywords.'utilities.ManageStudies.verifyManageStudiesExportUiWhenHasData'()

// Step 3: Apply a study filter that matches nothing — verify pagination total is 0, export is disabled, and tooltip
//         "No results to export. No studies match your filters."
String noMatchStudy = 'CRDCDH3913-no-match-' + System.currentTimeMillis()
CustomKeywords.'utilities.ManageStudies.applyManageStudiesFilters'([studyName: noMatchStudy])
WebUI.delay(3)
CustomKeywords.'utilities.ManageStudies.verifyManageStudiesExportUiWhenEmpty'()

// Step 4: Reset filters, then apply study/program/status/access filters used in QA — verify table has results.
CustomKeywords.'utilities.ManageStudies.resetManageStudiesFilters'()
CustomKeywords.'utilities.ManageStudies.applyManageStudiesFilters'([
	studyName: 'autoQA',
	program: 'AutoTest-Program',
	status: 'Active',
	accessType: 'Open'
])
WebUI.delay(3)

int filteredTotal = CustomKeywords.'utilities.WebUIUtils.getTableTotalEntryCount'(15)
if (filteredTotal <= 0) {
	KeywordUtil.markFailed('TC01 CRDCDH-3913: expected filtered studies for autoQA/AutoTest-Program; table total=' + filteredTotal)
}

// Step 5: If more than one page of results — set rows-per-page to 10 so visible rows < total; export must still
//         include all filtered rows (verified in Step 6 row count).
if (filteredTotal > 10) {
	CustomKeywords.'utilities.WebUIUtils.changeRowsPerPage'(10)
	WebUI.delay(2)
}

// Step 6: Export CSV — verify filename crdc-manage-studies-YYYY-MM-DD-HH-mm-ss.csv, header columns (no Action),
//         data row count equals filtered UI total, and each row Name or Acronym contains study filter token autoQA.
String csvPath = CustomKeywords.'utilities.ManageStudies.clickManageStudiesExportAndVerifyDownloadedFileName'()
CustomKeywords.'utilities.ManageStudies.verifyManageStudiesExportCsvHeaders'(csvPath)
CustomKeywords.'utilities.ManageStudies.verifyManageStudiesExportCsvRowCount'(filteredTotal, csvPath)
CustomKeywords.'utilities.ManageStudies.verifyManageStudiesExportCsvNameColumnContains'(csvPath, 'autoQA')
CustomKeywords.'utilities.Utils.clearProjectDownloadsFolder'()

// --- Federal Lead: export smoke on default list (no custom study/program filters) ---
CustomKeywords.'utilities.Login.switchUser'('Fedlead')
WebUI.delay(2)

// Step 7: Fed Lead — Manage Studies export visible, enabled, and enabled tooltip when the list has rows.
CustomKeywords.'utilities.ManageStudies.navigateToManageStudies'()
CustomKeywords.'utilities.ManageStudies.verifyManageStudiesExportUiWhenHasData'()

// Step 8: Fed Lead — export without applying filters (page default view only) — verify filename, CSV headers,
//         and data row count matches pagination total for that view.
WebUI.delay(2)
int fedListTotal = CustomKeywords.'utilities.WebUIUtils.getTableTotalEntryCount'(15)
String fedCsvPath = CustomKeywords.'utilities.ManageStudies.clickManageStudiesExportAndVerifyDownloadedFileName'()
CustomKeywords.'utilities.ManageStudies.verifyManageStudiesExportCsvHeaders'(fedCsvPath)
CustomKeywords.'utilities.ManageStudies.verifyManageStudiesExportCsvRowCount'(fedListTotal, fedCsvPath)
CustomKeywords.'utilities.Utils.clearProjectDownloadsFolder'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
