import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
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
import utilities.SubmissionRequest

/*This test script:
 - Opens the browser of choice: Chrome, Firefox or Edge
 - Driver opened by Katalon is used in Selenium.
 - Takes the Query from input excel and fetches data from Neo4j database.
   Saves the results from neo4j and application in the same name mentioned in the input excel.
 - Clicks on the Cases button in the Navbar of ICDC's homepage.
 - Clicks on the Filter 'Breed' from left pane
 - Selects the specific check box from 'Breed' filter.
 - Reads the results displayed for the selected filter (from all the pages in UI) and saves in the excel mentioned in Input file
 - Reads Neo4j DB using the query from Input file and saves the data in the excel mentioned in Input file
 - Reads Neo4j excel and Webdata excel as lists and compares the data.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
WebUI.setViewPortSize(1920, 1080)
WebUI.delay(2)
WebUI.waitForElementPresent(findTestObject('CRDC/NavBar/SubmissionRequest-Tab'), 10)
WebUI.waitForElementVisible(findTestObject('CRDC/NavBar/SubmissionRequest-Tab'), 10)
WebUI.click(findTestObject('CRDC/NavBar/SubmissionRequest-Tab'))
WebUI.waitForElementPresent(findTestObject('CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn'), 5)
WebUI.click(findTestObject('CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn'))
WebUI.delay(1)
WebUI.waitForElementPresent(findTestObject('CRDC/SubmissionRequest/ReadAndAcceptPopUp-Btn'), 5)
WebUI.click(findTestObject('CRDC/SubmissionRequest/ReadAndAcceptPopUp-Btn'))
CustomKeywords.'utilities.SubmissionRequest.fillSubmissionRequestAllSections'(2, 'Other', 6, 1, 1, 'Open', 
    'Yes', 'Cholangiocarcinoma', 'Homo', 1, 'genomics', 'proteomics', 5, 6, 1, 1)
CustomKeywords.'utilities.SubmissionRequest.clickSubmitButton'()
WebUI.closeBrowser()

//Login as Fedlead and approve submission request
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Fedlead')
WebUI.waitForElementPresent(findTestObject('CRDC/NavBar/SubmissionRequest-Tab'), 10)
WebUI.waitForElementVisible(findTestObject('CRDC/NavBar/SubmissionRequest-Tab'), 10)
WebUI.click(findTestObject('CRDC/NavBar/SubmissionRequest-Tab'))
WebUI.delay(2)
CustomKeywords.'utilities.SubmissionRequest.approveSubmissionRequest'(SubmissionRequest.lastEnteredStudyTitle)
WebUI.closeBrowser()

//Login as a submitter and create a submission with older
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
WebUI.setViewPortSize(1920, 1080)
WebUI.delay(2)
WebUI.waitForElementPresent(findTestObject('CRDC/NavBar/SubmissionRequest-Tab'), 10)
WebUI.waitForElementVisible(findTestObject('CRDC/NavBar/SubmissionRequest-Tab'), 10)
WebUI.click(findTestObject('CRDC/NavBar/SubmissionRequest-Tab'))
WebUI.waitForElementPresent(findTestObject('CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn'), 5)
WebUI.click(findTestObject('CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn'))
WebUI.delay(1)
WebUI.waitForElementPresent(findTestObject('CRDC/SubmissionRequest/ReadAndAcceptPopUp-Btn'), 5)
WebUI.click(findTestObject('CRDC/SubmissionRequest/ReadAndAcceptPopUp-Btn'))
WebUI.delay(1)
CustomKeywords.'utilities.SubmissionRequest.enterPrincipalInvestigatorInfo'(2)
CustomKeywords.'utilities.SubmissionRequest.enterPrimaryContactInfo'(2)
WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
WebUI.click(findTestObject('CRDC/SubmissionRequest/Next-Btn'))
CustomKeywords.'utilities.SubmissionRequest.enterProgramInfo'('Other', 6)
CustomKeywords.'utilities.SubmissionRequest.enterOldStudyInfo'(SubmissionRequest.lastEnteredStudyTitle, SubmissionRequest.lastEnteredStudyAbbreviation)
CustomKeywords.'utilities.SubmissionRequest.enterFundingAgencyInfo'(1)
WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
WebUI.click(findTestObject('CRDC/SubmissionRequest/Next-Btn'))
CustomKeywords.'utilities.SubmissionRequest.enterDataAccessTypeAndDbGapRegInfo'('Controlled', 'Yes')
CustomKeywords.'utilities.SubmissionRequest.enterCancerTypeAndSubjectsInfo'('Cholangiocarcinoma', 'Homo', 1)
WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
WebUI.click(findTestObject('CRDC/SubmissionRequest/Next-Btn'))
WebUI.delay(2)
CustomKeywords.'utilities.SubmissionRequest.enterTargetDeliveryAndExpectedPublicationDate'()
CustomKeywords.'utilities.SubmissionRequest.selectDataTypes'('genomics', 'proteomics')
CustomKeywords.'utilities.SubmissionRequest.selectFileTypes'(5, 6, 1, 1)
WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
WebUI.click(findTestObject('CRDC/SubmissionRequest/Next-Btn'))
CustomKeywords.'utilities.SubmissionRequest.clickSubmitButton'()
WebUI.closeBrowser()

//Login as Fedlead and approve the submission requst
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Fedlead')
WebUI.waitForElementPresent(findTestObject('CRDC/NavBar/SubmissionRequest-Tab'), 10)
WebUI.waitForElementVisible(findTestObject('CRDC/NavBar/SubmissionRequest-Tab'), 10)
WebUI.click(findTestObject('CRDC/NavBar/SubmissionRequest-Tab'))
WebUI.delay(2)
CustomKeywords.'utilities.SubmissionRequest.approveSubmissionRequest'(SubmissionRequest.lastEnteredStudyTitle)
CustomKeywords.'utilities.SubmissionRequest.verifySubmissionRequestSnackbarError'("This is a duplicate study name. The '" + SubmissionRequest.lastEnteredStudyTitle + "' study already exists in the system.")
CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
