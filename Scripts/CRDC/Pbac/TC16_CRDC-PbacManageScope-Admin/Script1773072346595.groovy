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

/*This test script:
 - Opens the browser of choice: Chrome, Firefox or Edge
 - Driver opened by Katalon is used in Selenium.
 - Logs in as Admin
 - Goes to Manage Users and Edit the user being tested
 - Verifies the default permissions 
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)

//Login as Admin
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Admin')

//Navigate to Manage Users
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/Login/UserProfile-Dd')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/ManageUsers-Btn')

//Edit user permissions and write expected checkbox statuses to output sheet 
CustomKeywords.'utilities.Pbac.editPbacPermissions'('Admin', 'positive')

//Verify admin is able to manage all studies, programs, institutions, and users as per DB
CustomKeywords.'utilities.SubmissionRequest.clickAccountDropdown'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/ManageStudies-Btn')
// Default Status filter is Active; Mongo approvedStudies includes Inactive. Switch to All before comparing.
CustomKeywords.'utilities.ManageStudies.applyManageStudiesFilters'([status: 'All'])
int uiCountStudies = CustomKeywords.'utilities.WebUIUtils.getTableTotalEntryCount'()
CustomKeywords.'utilities.DatabaseUtils.verifyTableCountMatchesMongoCollection'(uiCountStudies, "approvedStudies")

CustomKeywords.'utilities.SubmissionRequest.clickAccountDropdown'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/ManagePrograms-Btn')
int uiCountPrograms = CustomKeywords.'utilities.WebUIUtils.getTableTotalEntryCount'()
CustomKeywords.'utilities.DatabaseUtils.verifyTableCountMatchesMongoCollection'(uiCountPrograms, "organization", [status: "Active"])

CustomKeywords.'utilities.SubmissionRequest.clickAccountDropdown'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/ManageInstitutions-Btn')
int uiCountInstitutions = CustomKeywords.'utilities.WebUIUtils.getTableTotalEntryCount'()
CustomKeywords.'utilities.DatabaseUtils.verifyTableCountMatchesMongoCollection'(uiCountInstitutions, "institutions", [status: "Active"])

CustomKeywords.'utilities.SubmissionRequest.clickAccountDropdown'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/ManageUsers-Btn')
int uiCountUsers = CustomKeywords.'utilities.WebUIUtils.getTableTotalEntryCount'()
CustomKeywords.'utilities.DatabaseUtils.verifyTableCountMatchesMongoCollection'(uiCountUsers, "users")

WebUI.closeBrowser()