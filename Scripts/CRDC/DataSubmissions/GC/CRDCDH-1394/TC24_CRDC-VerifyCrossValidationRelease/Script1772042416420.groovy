import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.By as By
import org.openqa.selenium.WebDriver as WebDriver

/*
 * TC22_CRDC-VerifyCrossValidationRelease
 * Verifies Release button state: disabled after submit until Cross Validate is run, then enabled.
 *
 * Flow:
 *   1. Login as Dcp
 *   2. Navigate to Data Submissions, create a new submission
 *   3. Open submission, upload program metadata, validate, submit
 *   4. Verify Release button is disabled
 *   5. Click Cross Validate
 *   6. Verify Release button is enabled
 *   7. Generate validation report, close browser
 *
 * Prerequisites:
 *   - InputFiles/CRDC/MetadataData/PBAC/program.tsv (or path used by prepareUniqueMetadataAndUpload)
 *   - Profile must have valid G_Urlname and credentials
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)

//Login as Admin
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Admin')

//Navigate to Manage Users
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/Login/UserProfile-Dd')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/ManageUsers-Btn')

//Enable DCP permissions
CustomKeywords.'utilities.Pbac.editPbacPermissions'('Dcp', 'positive')

//Logout
CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
WebUI.closeBrowser()

//Login as DCP
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Dcp')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/Global/Header/Desktop/DataSubmissions-Tab')

//Create DS
CustomKeywords.'utilities.Pbac.createDataSubmission'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Loading-Icon', 30)

//Upload metadata file
CustomKeywords.'utilities.Pbac.prepareUniqueMetadataAndUpload'('InputFiles/CRDC/MetadataData/PBAC/program.tsv', "program_acronym")

//Validate
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validate-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validate-Btn')

//Submit
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')

//Verify Release button disabled
CustomKeywords.'utilities.Utils.waitForElementToAppear'('CRDC/DataSubmissions/Release-Btn', 30)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'(findTestObject('CRDC/DataSubmissions/Release-Btn'), false)

//Cross Validate
CustomKeywords.'utilities.DataSubmissions.clickCrossValidateButton'()

//Verify Release button enabled
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'(findTestObject('CRDC/DataSubmissions/Release-Btn'), true)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
