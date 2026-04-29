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

/*This test script:
 - Opens the browser of choice: Chrome, Firefox or Edge
 - Driver opened by Katalon is used in Selenium.
 - Logs in as Submitter
 - Creates DS, uploads, and validates
 - Verifies data submission dashboard
 */
WebUI.closeBrowser()

//Login as Submitter and create data submisstion
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only','GC','autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/ValidationControls/ValidationType-Both-RadioBtn'), false)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/ValidationControls/ValidationType-DataFiles-RadioBtn'), false)
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-1042')
WebUI.delay(3)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "In Progress")
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'(findTestObject('Object Repository/CRDC/AILocators/DataSubmissions/Detail/Actions/Submit-Btn'), true)
WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Submit-Btn')
WebUI.delay(2) //race condition for Yes button
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
String deleteTargetSubId = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()
CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
WebUI.delay(3)
WebUI.closeBrowser()

//Login as DCP and release plus complete data submission
//If release is not successful, make sure another Submitted DS in the same study does not exist -- Reject it if so
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Dcp')
CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(deleteTargetSubId)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Release-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/ReleaseConfirmationYes-Btn')
WebUI.delay(3)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Complete-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/CompleteConfirmationYes-Btn')
CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
WebUI.closeBrowser()

//Login as submitter and create delete submission
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('delete', 'metadata only','GC','autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-1042-2')
WebUI.delay(3)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "In Progress")
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
WebUI.delay(3)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Submit-Btn'), false)
CustomKeywords.'utilities.DataSubmissions.verifyValidationResultsVsExpectedForTC'('CRDCDH-1042-2')
CustomKeywords.'utilities.DataSubmissions.deleteNodeFromDataView'('diagnosis')
String deleteSubId = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
WebUI.delay(2) 
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
WebUI.delay(3)
CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
WebUI.closeBrowser()

//Login as DCP and release the delete data submission
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Dcp')
CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(deleteSubId)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Release-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/ReleaseConfirmationYes-Btn')
WebUI.delay(3)
//Database verification prior deleting
CustomKeywords.'utilities.DatabaseUtils.connectFromGlobalVariable'()
Map statusReleased = [
	status       : "Released",
	submissionID : deleteTargetSubId,
	nodeID	 : "auto_cons_1"
]
def record1 = CustomKeywords.'utilities.DatabaseUtils.findRecordFromDataFile'("Data Files/CRDC/DataSubmissions/DBQueries", "status", statusReleased)
CustomKeywords.'utilities.DatabaseUtils.verifyRecordReturned'(record1)
WebUI.delay(120)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Complete-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/CompleteConfirmationYes-Btn')
WebUI.delay(5)
//Database verification after deletion
Map statusDeleted = [
	status       : "Deleted",
	submissionID : deleteTargetSubId,
	nodeID	 : "auto_cons_1"
]
def record2 = CustomKeywords.'utilities.DatabaseUtils.findRecordFromDataFile'("Data Files/CRDC/DataSubmissions/DBQueries", "status", statusDeleted)
CustomKeywords.'utilities.DatabaseUtils.verifyRecordReturned'(record2)
CustomKeywords.'utilities.DatabaseUtils.close'()
CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()