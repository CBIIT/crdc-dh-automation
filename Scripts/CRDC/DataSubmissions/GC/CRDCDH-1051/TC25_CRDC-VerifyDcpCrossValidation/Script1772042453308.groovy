import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * Scenario:
 *   Cross-submission conflict: the Submitter creates TWO data submissions for
 *   the SAME study, uploads identical program records into both (same
 *   program_acronym values), validates and submits each. After the second
 *   submission is submitted, the system runs cross-validation and flags the
 *   duplicate program nodes. A DCP user then navigates to the second
 *   submission, triggers cross-validation, and the test verifies the expected
 *   "Conflict Data found" errors appear in the Cross Validation Results tab.
 *   Reject and verify status
 *
 * The expected TSV contains program_acronym values (e.g. CDS-595435) from
 * program.tsv that will conflict across the two submissions. Update the TSV
 * if your program records differ.
 *
 * Flow:
 *   === Submitter ===
 *   1. Login as Submitter
 *   2. Create Submission 1 (metadata only, ATS study), upload program.tsv,
 *      validate, submit -> capture submission ID
 *   3. Go back to DS list, create Submission 2 for the SAME study, upload the
 *      same program.tsv (same program_acronym values -- that is the cross-validation
 *      key for duplicate program nodes across submissions)
 *      validate, submit -> capture submission ID
 *   4. Logout
 *   === DCP ===
 *   5. Login as DCP
 *   6. Navigate to Submission 2 (the second submission)
 *   7. Click Cross Validate button (DCP-only; triggers cross-submission checks)
 *      and wait for cross-validation to finish
 *   8. Navigate to Cross Validation Results tab, verify against expected TSV
 *   9. Reject and verify status
 *   10. Generate validation report
 *   11. Close browser
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC','autoQA - AutomationQA_CV')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/List/Table/SubmissionName-Link')

// Upload program metadata -- same program_acronym across both submissions (cross-validation key)
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC25')

CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

// Submit Submission 1
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
WebUI.delay(2)

// Capture Submission 1 ID for later navigation
String submissionId1 = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()

// Ccreate Submission 2, upload the SAME program data, validate, submit
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC','autoQA - AutomationQA_CV')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/List/Table/SubmissionName-Link')

// Upload program metadata again -- program_acronym values are identical to Submission 1, which will trigger cross-validation conflicts
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC25')

CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

// Submit Submission 2
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
WebUI.delay(2)

// Capture Submission 2 ID -- DCP will navigate here to see cross-validation results
String submissionId2 = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()

// Logout Submitter
CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
WebUI.closeBrowser()

// DCP: login, navigate to Submission 2, trigger cross-validation, verify
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Dcp')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(submissionId2)

// Click the Cross Validate button and wait for cross-validation to complete.
CustomKeywords.'utilities.DataSubmissions.clickCrossValidateButton'()

// Verify the Cross Validation Results tab shows the expected conflicts.
CustomKeywords.'utilities.DataSubmissions.verifyCrossValidationResultsVsExpected'('TC25')

//Reject and verify
CustomKeywords.'utilities.WebUIUtils.verifyElementDisplayed'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Reject-Btn'))
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Reject-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/RejectDsComment-Txtbx')
WebUI.setText(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/RejectDsComment-Txtbx'), 'testing Reject')
CustomKeywords.'utilities.WebUIUtils.clickTab'('Object Repository/CRDC/AILocators/DataSubmissions/Detail/Actions/ConfirmToReject-Btn')
WebUI.delay(3)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "Rejected")

CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(submissionId1)
CustomKeywords.'utilities.WebUIUtils.verifyElementDisplayed'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Reject-Btn'))
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Reject-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/RejectDsComment-Txtbx')
WebUI.setText(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/RejectDsComment-Txtbx'), 'testing Reject')
CustomKeywords.'utilities.WebUIUtils.clickTab'('Object Repository/CRDC/AILocators/DataSubmissions/Detail/Actions/ConfirmToReject-Btn')
WebUI.delay(3)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "Rejected")

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()