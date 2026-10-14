/*
 * CRDCDH-2226 TC02 + TC03 (CRDCDH-3594): Reject-dialog comment rules and Submitter View Comments after rejection.
 * Submitter submits a new request; Fed Lead checks the reject comment field, then rejects with a long comment;
 * Submitter confirms the comment appears read-only in View Comments.
 */
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

WebUI.closeBrowser()
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')

// Step 1 — Submitter completes and submits a new submission request.
CustomKeywords.'utilities.SubmissionRequest.startSubmissionRequest'()
CustomKeywords.'utilities.SubmissionRequest.fillSubmissionRequestAllSections'(2, 'Other', 6, 1, 1, 'Open',
	'Yes', 'Cholangiocarcinoma', 'Homo', 1, 'genomics', 'proteomics', 5, 6, 1, 1)
CustomKeywords.'utilities.SubmissionRequest.clickSubmitButton'()

String studyName = SubmissionRequest.lastEnteredStudyTitle
String commentPrefix = 'CRDCDH-2226-TC02-long-comment-'
String longComment = commentPrefix + 'x'.repeat(2500)

// Step 2 — Fed Lead opens Reject for that study (dialog only; no decision yet).
CustomKeywords.'utilities.Login.switchUser'('Fedlead')
CustomKeywords.'utilities.SubmissionRequest.openReviewDecisionDialog'(studyName, 'reject')

// Step 3 — Comment counter after hello; over 10,000 blocked on Confirm with validation message; Cancel without rejecting.
CustomKeywords.'utilities.SubmissionRequest.verifyReviewCommentFieldUi'()
CustomKeywords.'utilities.SubmissionRequest.cancelReviewDecisionDialog'()

// Step 4 — Fed Lead rejects with a long comment; request status becomes Rejected.
CustomKeywords.'utilities.SubmissionRequest.rejectSubmissionRequest'(studyName, longComment)

// Step 5 — Submitter opens the request, uses View Comments, and sees the rejection comment (read-only).
CustomKeywords.'utilities.Login.switchUser'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.openSubmissionRequestFormForStudy'(studyName, 'Rejected')
CustomKeywords.'utilities.SubmissionRequest.verifyViewCommentsButtonDisplayed'(true)
CustomKeywords.'utilities.SubmissionRequest.openViewCommentsDialog'()
CustomKeywords.'utilities.SubmissionRequest.verifyViewCommentsDialog'(longComment, true)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
