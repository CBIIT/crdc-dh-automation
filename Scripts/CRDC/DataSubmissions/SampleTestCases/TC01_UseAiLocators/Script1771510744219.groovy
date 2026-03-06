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
 - NOTE: This is a duplicate of TC16 using AILocators library for all script-level locators
 */
WebUI.closeBrowser()

//Login as Submitter
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

//Create DS - Metadata only
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/Global/Header/Desktop/DataSubmissions-Tab')
CustomKeywords.'utilities.Pbac.createDataSubmission'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/List/Table/SubmissionName-Link')
//Upload metadata file
CustomKeywords.'utilities.Pbac.prepareUniqueMetadataAndUpload'('InputFiles/CRDC/MetadataData/PBAC/program.tsv', "program_acronym")
//Validate data
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
//Submit
WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Actions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Actions/Submit-Btn')
WebUI.delay(2) //race condition for Yes button
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/SubmitDialog/Confirm-Btn')
//Get Submission ID
String submissionIdMetadataOnly = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()

//Logout
CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
WebUI.closeBrowser()

//Login as DCP
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Dcp')

//Go to submitter's submission - Metadata only
CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(submissionIdMetadataOnly)
//After data submission is submitted, Status = Submitted
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Summary/Status-Lbl'), "Submitted")
//The Validate button should still be enabled for the DCP
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/ValidationControls/Validate-Btn'), true)
//A Metadata only submission will default to "Metadata"
CustomKeywords.'utilities.WebUIUtils.verifyButtonSelected'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/ValidationControls/ValidationType-Metadata-RadioBtn'), true)
//The Validation Target should be set to "All Uploaded Data"
CustomKeywords.'utilities.WebUIUtils.verifyButtonSelected'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/ValidationControls/ValidationTarget-AllUploadedData-RadioBtn'), true)
//"New Uploaded Data" will be disabled
CustomKeywords.'utilities.WebUIUtils.verifyButtonSelected'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/ValidationControls/ValidationTarget-NewUploadedData-RadioBtn'), false)

//Generate HTML report
CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
