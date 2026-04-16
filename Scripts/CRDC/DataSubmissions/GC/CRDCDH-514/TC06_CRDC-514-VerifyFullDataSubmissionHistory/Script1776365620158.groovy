import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.By as By
import org.openqa.selenium.WebDriver as WebDriver
import org.openqa.selenium.WebElement as WebElement
import org.openqa.selenium.chrome.ChromeDriver as ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions as ChromeOptions
import org.openqa.selenium.firefox.FirefoxBinary as FirefoxBinary
import org.openqa.selenium.firefox.FirefoxDriver as FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions as FirefoxOptions
import org.openqa.selenium.support.ui.ExpectedConditions as ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait as WebDriverWait
import java.nio.file.Path as Path
import java.nio.file.Paths as Paths
import com.kms.katalon.core.testdata.TestDataFactory
import com.kms.katalon.core.configuration.RunConfiguration

/*This test script:
 - Opens the browser of choice: Chrome, Firefox or Edge
 - Driver opened by Katalon is used in Selenium.
 - Logs in as Submitter
 - Creates DS, uploads, and validates
 - Verifies data submission dashboard
 */
WebUI.closeBrowser()

//*************************** Login as Submitter *********************************
//Login as Submitter
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.Pbac.createDataSubmission'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "New")
//Upload metadata file
CustomKeywords.'utilities.Pbac.prepareUniqueMetadataAndUpload'('InputFiles/CRDC/MetadataData/PBAC/program.tsv', "program_acronym")
//Validate data
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "In Progress")
//Submit
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
WebUI.delay(2) //race condition for Yes button
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
//Get Submission ID
String submissionIdMetadataOnly = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()
//Logout
CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
WebUI.closeBrowser()

//*************************** Login as DCP *********************************

//Login as DCP
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Dcp')
//Go to submitter's submission - Metadata only
CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(submissionIdMetadataOnly)
WebUI.delay(2)
//After data submission is submitted, Status = Submitted
CustomKeywords.'utilities.Utils.waitForElementToAppear'('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl', 30)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "Submitted")
//Click on reject button
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Reject-Btn')
//enter justification for rejection
WebUI.sendKeys(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/RejectDsComment-Txtbx'), 'Rejecting')
//Confirm to reject
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/ConfirmToReject-Btn')
WebUI.delay(2)
//Logout
CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
WebUI.closeBrowser()

//***************************** Login as Submitter *******************************

//Log back in as a submitter revalidate and submit
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(submissionIdMetadataOnly)
CustomKeywords.'utilities.Utils.waitForElementToAppear'('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl', 30)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "Rejected")
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
WebUI.refresh()
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
WebUI.delay(2) //race condition for Yes button
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
WebUI.delay(2)
//Logout
CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
WebUI.closeBrowser()

//****************************** Login as DCP ******************************

//Login as DCP
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Dcp')
//Go to submitter's submission - Metadata only
CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(submissionIdMetadataOnly)
CustomKeywords.'utilities.Utils.waitForElementToAppear'('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl', 30)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "Submitted")
//After data submission is submitted, Status = Submitted
CustomKeywords.'utilities.DataSubmissions.clickCrossValidateButton'()
//Click on Release button
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Release-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/ReleaseConfirmationYes-Btn')
WebUI.delay(10)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "Released")
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Complete-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/CompleteConfirmationYes-Btn')
WebUI.delay(10)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "Completed")
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Summary/FullHistory-Btn')

// Expected submission history: newest first (Create -> New -> Validate -> Submit -> Submitted -> Reject -> Rejected -> Submit again -> Submitted -> Release -> Released -> Complete -> Completed)
List<String> expectedHistoryNewestFirst = ["COMPLETED", "RELEASED", "SUBMITTED", "REJECTED", "SUBMITTED", "INPROGRESS", "NEW"]
CustomKeywords.'utilities.DataSubmissions.verifyFullSubmissionHistory'(expectedHistoryNewestFirst)

//Generate HTML report
CustomKeywords.'utilities.ValidationReport.generateValidationReport'()


WebUI.closeBrowser()