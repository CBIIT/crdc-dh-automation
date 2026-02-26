import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.By as By
import org.openqa.selenium.WebDriver as WebDriver

/*
 * TC27_CRDC-VerifyCrossValidationReleaseMessage
 * Verifies improved cross validation behavior:
 * Create DS A to be in Released status
Create DS B to be in Submitted status
Change DS A from Released -> Completed
Run test case verifications on DS B
Clean up data by rejecting DS B
 *
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)

//Login as Admin and set up DCP user
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Admin')
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/Login/UserProfile-Dd')
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/NavBar/ManageUsers-Btn')
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
//Release
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Release-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/ReleaseConfirmationYes-Btn')
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
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Release-Btn'), true)


//Clean up for future runs - Reject this DS so this test case can be run again next time 
CustomKeywords.'utilities.WebUIUtils.verifyElementDisplayed'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Reject-Btn'))
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Reject-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/RejectDsComment-Txtbx')
WebUI.setText(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/RejectDsComment-Txtbx'), 'Rejecting this DS to clean up data for future runs')
CustomKeywords.'utilities.WebUIUtils.clickTab'('Object Repository/CRDC/AILocators/DataSubmissions/Detail/Actions/ConfirmToReject-Btn')

/*
 * Troubleshooting note in possible scenario that this test case fails:
 * Ensure that there are no DS that are in Submitted or Released status for program: auto_program-cv-CRDCDH-3138
 * If there are, manually Reject them and rerun test case
 */

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()