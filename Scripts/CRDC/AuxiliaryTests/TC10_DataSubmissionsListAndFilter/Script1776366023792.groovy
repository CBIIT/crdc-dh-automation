import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC10_DataSubmissionsListAndFilter
 * Exercises all DataSubmissionsList keywords in an order typical for a real test.
 *
 * Flow:
 *   1. Login (Admin or user with Data Submissions access)
 *   2. navigateToDataSubmissionsList() – open Data Submissions from nav bar
 *   3. verifyDataSubmissionsListPage() – table, Create button, row count
 *   4. applyDataSubmissionsListFilters(criteria) – e.g. submission name and/or status/program
 *   5. readDataSubmissionsListTableRows() – read current rows (logs row count internally)
 *   6. verifyDataSubmissionsListFilterResults(expectations) – assert min row count / row contains
 *   7. resetDataSubmissionsListFilters() – clear filters
 *   8. Generate validation report
 *
 * Criteria/expectations use existing data (mixed data strategy). Adjust submissionName/status/program
 * to match your environment so filter steps run against real data.
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

// 1. Navigate to Data Submissions list
CustomKeywords.'utilities.DataSubmissionsList.navigateToDataSubmissionsList'()

// 2. Verify list page (table, Create button, rows)
CustomKeywords.'utilities.DataSubmissionsList.verifyDataSubmissionsListPage'()

// 3. Apply filters (use criteria that match your env; empty map skips applying)
CustomKeywords.'utilities.DataSubmissionsList.applyDataSubmissionsListFilters'([ 
	submissionName: 'auto', 
	status: 'Submitted, In Progress', 
	program: 'AutoTest-Program', 
	submitterName: 'pbac.submitter-automation Please Do Not Edit', 
	dataCommons: 'GC', 
	dbGaP: '' ])

// 4. Verify filter results (expectations = expectAtLeastRows and/or rowContains)
CustomKeywords.'utilities.DataSubmissionsList.verifyDataSubmissionsListFilterResults'([ 
	expectAtLeastRows: 1, 
	rowContains: 'auto' ])

// 5. Reset filters
CustomKeywords.'utilities.DataSubmissionsList.resetDataSubmissionsListFilters'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
