import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.By as By
import org.openqa.selenium.WebDriver as WebDriver

/*
 * TC26_CRDC-VerifyCrossValidationReleaseMessage
 * Verifies special cross validation message for DS that has never been released
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

//Create DS 1 and leave In Progress
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/Global/Header/Desktop/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC','autoQA - AutomationQA_CV')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Loading-Icon', 30)
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC26')

//Create DS 2, submit, and verify special Release message
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/Global/Header/Desktop/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC','autoQA - AutomationQA_CV')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Loading-Icon', 30)
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC26')
//Validate
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
//Submit
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
WebUI.delay(3)

//Click on Release button and verify buttons and dialog text
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Release-Btn')
CustomKeywords.'utilities.WebUIUtils.verifyElementDisplayed'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/ReleaseCrossValidationConfirmRelease-Btn'))
CustomKeywords.'utilities.WebUIUtils.verifyElementDisplayed'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Cancel-Btn'))
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('Object Repository/CRDC/AILocators/DataSubmissions/Detail/Actions/ReleaseConfirmation-Txt'), 
	'There are other data submissions for the same study currently ongoing. Are you sure you want to release this data submission to Data Commons?')
CustomKeywords.'utilities.WebUIUtils.verifyElementDisplayed'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Cancel-Btn'))

//Clean up for future runs - Reject this DS so this test case can be run again next time 
CustomKeywords.'utilities.WebUIUtils.verifyElementDisplayed'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Reject-Btn'))
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Reject-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/RejectDsComment-Txtbx')
WebUI.setText(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/RejectDsComment-Txtbx'), 'Rejecting this DS to clean up data for future runs')
CustomKeywords.'utilities.WebUIUtils.clickTab'('Object Repository/CRDC/AILocators/DataSubmissions/Detail/Actions/ConfirmToReject-Btn')

/*
 * Troubleshooting note in possible scenario that this test case fails:
 * Ensure that there are no DS that are in Submitted status for program: auto_program-cv
 * If there are, manually Reject them and rerun test case
 */

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()