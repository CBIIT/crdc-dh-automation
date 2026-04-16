import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * CRDCDH-3138 — TC27: Cross-validation improvements (Release enable/disable, Confirm Release path).
 * Also covers CRDCDH-1390 export-with-no-issues (merged from former TC02): Export control disabled
 * and tooltip when the Cross Validation table shows no issues; traceability for 1390 with issues
 * remains in TC01.
 *
 * Flow: DS A Released -> DS B Submitted + cross-validate -> DS A Completed -> verify DS B
 * (Release state, empty CV table, export UI) -> reject DS B cleanup.
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)

//Login as Admin and set up DCP user
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Admin')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/Login/UserProfile-Dd')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/ManageUsers-Btn')
CustomKeywords.'utilities.Pbac.editPbacPermissions'('Dcp', 'positive')
CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
WebUI.closeBrowser()

//Login as DCP
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Dcp')
WebUI.delay(2)

//Create DS A to be in Released status
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/Global/Header/Desktop/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC','autoQA - AutomationQA_CV-CRDCDH-3138')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Loading-Icon', 30)
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC27')
//Validate
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
//Submit
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
WebUI.delay(5)
//Release
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Release-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/ReleaseCrossValidationConfirmRelease-Btn')
String submissionIdDsA = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()


//Create DS B to be in Submitted status
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/Global/Header/Desktop/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC','autoQA - AutomationQA_CV-CRDCDH-3138')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Loading-Icon', 30)
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC27')
//Validate
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
//Submit
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
//Cross Validate
CustomKeywords.'utilities.DataSubmissions.clickCrossValidateButton'()
String submissionIdDsB = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()


//Change DS A from Released -> Completed
CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(submissionIdDsA)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Complete-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/CompleteConfirmationYes-Btn')


//Run test case verifications on DS B
CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(submissionIdDsB)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Release-Btn'), false)
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Tabs/CrossValidation-Tab')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 30)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('Object Repository/CRDC/AILocators/GenericTable/TableBody'), 'No cross-validation issues found')

// CRDCDH-1390 (ex-TC02): export disabled when there are no cross-validation issues; tooltip copy
CustomKeywords.'utilities.WebUIUtils.verifyElementCount'(
		findTestObject('CRDC/AILocators/DataSubmissions/Detail/CrossValidation/Filters/ExportCrossValidation-Btn'), 2)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'(
		findTestObject('CRDC/AILocators/DataSubmissions/Detail/CrossValidation/Filters/ExportCrossValidation-Btn'), false)
CustomKeywords.'utilities.WebUIUtils.verifyTooltipOnHover'(
		findTestObject('CRDC/AILocators/DataSubmissions/Detail/CrossValidation/Filters/ExportCrossValidationTooltip'),
		'Export all cross validation issues to a CSV file',
		'Export all cross validation')

CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Release-Btn'), true)


//Clean up for future runs - Reject this DS so this test case can be run again next time 
CustomKeywords.'utilities.WebUIUtils.verifyElementDisplayed'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Reject-Btn'))
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Reject-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/RejectDsComment-Txtbx')
WebUI.setText(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/RejectDsComment-Txtbx'), 'Rejecting this DS to clean up data for future runs (CRDCDH-3138 TC27)')
CustomKeywords.'utilities.WebUIUtils.clickTab'('Object Repository/CRDC/AILocators/DataSubmissions/Detail/Actions/ConfirmToReject-Btn')

/*
 * Troubleshooting note in possible scenario that this test case fails:
 * Ensure that there are no DS that are in Submitted or Released status for program: auto_program-cv-CRDCDH-3138
 * If there are, manually Reject them and rerun test case
 */

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()