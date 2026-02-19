import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * TC06_CrossValidationVerification
 * Sample test demonstrating the verifyCrossValidationResultsVsExpected keyword.
 *
 * Scenario:
 *   S001 cross-submission conflict: the Submitter creates TWO data submissions for
 *   the SAME study, uploads identical participant records into both (same
 *   study_participant_id values), validates and submits each. After the second
 *   submission is submitted, the system runs cross-validation and flags the
 *   duplicate participant nodes. A DCP user then navigates to the second
 *   submission, triggers cross-validation, and the test verifies the expected
 *   S001 "Conflict Data found" errors appear in the Cross Validation Results tab.
 *
 * Test data:
 *   Upload file: InputFiles/CRDC/MetadataData/PBAC/participant.tsv
 *   Expected CV results: InputFiles/CRDC/MetadataData/SampleTC_CrossValidation/
 *                        expected-CrossValidationResults/GC_expected-CrossValidationResults.tsv
 *
 * The expected TSV contains study_participant_id values (GC-CASE-992050,
 * GC-CASE-987680) from participant.tsv that will conflict across the two
 * submissions. Update the TSV if your participant records differ.
 *
 * Flow:
 *   === Submitter ===
 *   1. Login as Submitter
 *   2. Create Submission 1 (metadata only, ATS study), upload participant.tsv,
 *      validate, submit -> capture submission ID
 *   3. Go back to DS list, create Submission 2 for the SAME study, upload the
 *      same participant.tsv (prepareUniqueMetadataAndUpload keeps IDs identical
 *      but only modifies participant_id to avoid duplicate-ID essential validation;
 *      study_participant_id stays the same -- that is the cross-validation key)
 *      validate, submit -> capture submission ID
 *   4. Logout
 *   === DCP ===
 *   5. Login as DCP
 *   6. Navigate to Submission 2 (the second submission)
 *   7. Click Cross Validate button (DCP-only; triggers S001 cross-submission checks)
 *      and wait for cross-validation to finish
 *   8. Navigate to Cross Validation Results tab, verify against expected TSV
 *   9. Generate validation report
 *   10. Close browser
 */

WebUI.closeBrowser()

// -----------------------------------------------------------------------
// Submitter: create Submission 1, upload participant data, validate, submit
// -----------------------------------------------------------------------
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.Pbac.createDataSubmission'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/List/Table/SubmissionName-Link')

// Upload participant metadata -- keeps study_participant_id unchanged (cross-validation key)
// participant_id is modified per row to avoid essential-validation duplicate-ID errors
CustomKeywords.'utilities.Pbac.uploadMetadataUI'('InputFiles/CRDC/MetadataData/PBAC/program.tsv')

CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

// Submit Submission 1
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
WebUI.delay(2)

// Capture Submission 1 ID for later navigation
String submissionId1 = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()

// -----------------------------------------------------------------------
// Submitter: create Submission 2, upload the SAME participant data, validate, submit
// -----------------------------------------------------------------------
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.Pbac.createDataSubmission'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/List/Table/SubmissionName-Link')

// Upload participant metadata again -- study_participant_id values are identical to
// Submission 1, which will trigger S001 cross-validation conflicts
CustomKeywords.'utilities.Pbac.uploadMetadataUI'('InputFiles/CRDC/MetadataData/PBAC/program.tsv')

CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

// Submit Submission 2
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
WebUI.delay(2)

// Capture Submission 2 ID -- DCP will navigate here to see cross-validation results
String submissionId2 = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()

// -----------------------------------------------------------------------
// Logout Submitter
// -----------------------------------------------------------------------
CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
WebUI.closeBrowser()

// -----------------------------------------------------------------------
// DCP: login, navigate to Submission 2, trigger cross-validation, verify
// -----------------------------------------------------------------------
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Dcp')
WebUI.delay(2)

// Navigate directly to Submission 2's upload-activity page
CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(submissionId2)

// Click the Cross Validate button and wait for cross-validation to complete.
// This triggers S001 duplicate-node checks across submissions in the same study.
CustomKeywords.'utilities.DataSubmissions.clickCrossValidateButton'()

// Verify the Cross Validation Results tab shows the expected S001 conflicts.
// Expected TSV: InputFiles/CRDC/MetadataData/SampleTC_CrossValidation/
//               expected-CrossValidationResults/GC_expected-CrossValidationResults.tsv
// Note: verifyCrossValidationResultsVsExpected navigates to the Cross Validation tab internally.
CustomKeywords.'utilities.DataSubmissions.verifyCrossValidationResultsVsExpected'('SampleTC_CrossValidation')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
