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


//Login as Submitter
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

//Create DS
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.Pbac.createDataSubmission'("CTDC")
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

//CustomKeywords.'utilities.Pbac.prepareUniqueMetadataAndUpload'('InputFiles/CRDC/MetadataData/program.tsv', "program_acronym")

////Upload data
//CustomKeywords.'utilities.Pbac.prepareUniqueMetadataAndUpload'('InputFiles/CRDC/MetadataData/TC01_program .tsv', "program_acronym")
//CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Validation/Uploading-Text', 15)
////CustomKeywords.'utilities.Pbac.prepareUniqueMetadataAndUpload'('InputFiles/CRDC/MetadataData/study.tsv', "study_id")
////CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Validation/Uploading-Text', 15)
//
////Wait for uploading to finish
//CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Validation/Uploading-Text', 15)
//
////Validate data
//WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validate-Btn'), 30)
//CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/DataSubmissions/Validate-Btn')
//
//
////Verify Validation Results Tab - Default  Message
//WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validation/ValidationResults-Tab'), 30)
//CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults-Tab')
//CustomKeywords.'utilities.SubmissionRequest.verifyDataSubmissionTableHeaders'('ValidationResults')
//WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults-DefaultMesssage"), 15, FailureHandling.OPTIONAL)
////CustomKeywords.'utilities.Pbac.verifyValidationResultsTable'()
//
//
////Verify if submit button is enabled
//CustomKeywords.'utilities.Pbac.verifyButtonState'('CRDC/DataSubmissions/Submit-Btn', 'ENABLE')


WebUI.delay(10)
WebUI.closeBrowser()
