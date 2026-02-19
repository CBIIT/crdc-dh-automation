import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * TC07_ValidationErrorDetailsVerification
 * Sample test demonstrating verifyErrorDetailsForSubmittedIdForTC.
 *
 * Opens the "See details" error dialog from the Validation Results table and asserts
 * that the displayed error/warning messages match expected values from the TC's expected-ValidationResults TSV.
 *
 * Test data:
 *   TC04 metadata produces a participant row (study_participant_id=auto_key5) with "Missing required property" (Error).
 *
 * Flow:
 *   1. Login as Submitter
 *   2. Create data submission, navigate to it
 *   3. Upload TC04 metadata, click Validate
 *   4. verifyErrorDetailsForSubmittedIdForTC discovers all nodes/rows from TC04 expected-ValidationResults, applies filters, verifies each dialog
 *   5. Generate validation report
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
//CustomKeywords.'utilities.Pbac.createDataSubmission'()
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC','autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/List/Table/SubmissionName-Link')

CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC04')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

CustomKeywords.'utilities.DataSubmissions.verifyErrorDetailsForSubmittedIdForTC'('TC04')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
