import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC03_SubmissionRequestsListAndFilter
 * Exercises all SubmissionRequestList keywords in an order typical for a real test.
 *
 * Flow:
 *   1. Login as Submitter
 *   2. navigateToSubmissionRequestsList() – open Submission Requests from nav bar
 *   3. verifySubmissionRequestsListPage() – container, Start button, row count
 *   4. applySubmissionRequestsListFilters(criteria) – e.g. status filter
 *   5. verifySubmissionRequestsFilterResults(expectations) – assert min row count
 *   6. resetSubmissionRequestsFilters() – clear filters
 *   7. Generate validation report
 *
 * Criteria/expectations use existing data (mixed data strategy). Adjust status/program/study
 * to match your environment so filter steps run against real data.
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

// 1. Navigate to Submission Requests list
CustomKeywords.'utilities.SubmissionRequestList.navigateToSubmissionRequestsList'()

// 2. Verify list page (container, Start button, rows)
CustomKeywords.'utilities.SubmissionRequestList.verifySubmissionRequestsListPage'()

// 3. Apply filters (use criteria that match your env; empty values are skipped)
String loggedInUserName = CustomKeywords.'utilities.WebUIUtils.getProfileName'()
CustomKeywords.'utilities.SubmissionRequestList.applySubmissionRequestsListFilters'([
	submitterName: loggedInUserName,
	program: '',
	study: '',
	status: 'New, In Progress'
])

// 4. Verify filter results
CustomKeywords.'utilities.SubmissionRequestList.verifySubmissionRequestsFilterResults'([
	expectAtLeastRows: 1
])

// 5. Reset filters
CustomKeywords.'utilities.SubmissionRequestList.resetSubmissionRequestsFilters'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
