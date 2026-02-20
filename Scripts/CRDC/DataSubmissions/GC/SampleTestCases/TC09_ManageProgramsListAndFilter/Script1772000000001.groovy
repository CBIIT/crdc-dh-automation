import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC09_ManageProgramsListAndFilter
 * Exercises all ManagePrograms phase-1 keywords in an order typical for a real test.
 *
 * Flow:
 *   1. Login (Admin or user with Programs access)
 *   2. navigateToManagePrograms() – open Manage Programs from header
 *   3. verifyManageProgramsListPage() – container, Add Program, table
 *   4. applyManageProgramsFilters(criteria) – e.g. program name and/or abbreviation
 *   5. verifyManageProgramsFilterResults(expectations) – assert min row count / row contains (call applyManageProgramsFilters first when needed)
 *   6. Generate validation report
 *
 * Criteria/expectations use existing program data (mixed data strategy). Adjust programName/abbreviation
 * to match your environment so filter steps run against real data.
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Admin')
WebUI.delay(2)

// 1. Navigate to Manage Programs
CustomKeywords.'utilities.ManagePrograms.navigateToManagePrograms'()

// 2. Verify list page (container, Add Program button, table)
CustomKeywords.'utilities.ManagePrograms.verifyManageProgramsListPage'()

// 3. Apply filters (use criteria that match your env; empty map skips applying)
CustomKeywords.'utilities.ManagePrograms.applyManageProgramsFilters'([ programName: 'AutoTest', studyName: 'autoQA' ])

// 4. Verify filter results (expectations = expectAtLeastRows and/or rowContains)
CustomKeywords.'utilities.ManagePrograms.verifyManageProgramsFilterResults'([ expectAtLeastRows: 1, rowContains: 'auto' ])

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
