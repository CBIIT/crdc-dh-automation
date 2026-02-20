import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC_ManageStudiesListAndFilter
 * Exercises all ManageStudies phase-1 keywords in an order typical for a real test.
 *
 * Flow:
 *   1. Login (Admin or user with Studies access)
 *   2. navigateToManageStudies() – open Manage Studies from header
 *   3. verifyManageStudiesListPage() – container, Add Study, table
 *   4. applyManageStudiesFilters(criteria) – e.g. study name and/or program
 *   5. readManageStudiesTableRows() – read current rows (logs row count internally)
 *   6. verifyManageStudiesFilterResults(expectations) – assert min row count / row contains (call applyManageStudiesFilters first when needed)
 *   7. resetManageStudiesFilters() – clear filters
 *   8. Generate validation report
 *
 * Criteria/expectations use existing study data (mixed data strategy). Adjust studyName/program
 * to match your environment so filter steps run against real data.
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Admin')
WebUI.delay(2)

// 1. Navigate to Manage Studies
CustomKeywords.'utilities.ManageStudies.navigateToManageStudies'()

// 2. Verify list page (container, Add Study button, table)
CustomKeywords.'utilities.ManageStudies.verifyManageStudiesListPage'()

// 3. Apply filters (use criteria that match your env; empty map skips applying)
CustomKeywords.'utilities.ManageStudies.applyManageStudiesFilters'([ studyName: 'autoQA', program: '', dbGaP: '' ])

// 4. Verify filter results (expectations = expectAtLeastRows and/or rowContains)
CustomKeywords.'utilities.ManageStudies.verifyManageStudiesFilterResults'([ expectAtLeastRows: 1, rowContains: 'autoQA' ])

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
