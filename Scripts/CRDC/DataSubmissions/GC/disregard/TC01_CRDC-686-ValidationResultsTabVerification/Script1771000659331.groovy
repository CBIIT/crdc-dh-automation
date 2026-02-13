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

/* CRDCDH-686: Validation Results Tab Verification (one-to-one mapping from Jira)
 * Jira Row 1: Navigate, Login as Submitter, Data Submission tab
 * Jira Row 2: Verify User can access Data Submission Dashboard, Validation Results tab
 * Jira Row 3: PBAC - Verify role visibility (Admin, Fed Lead, DCP, Submitter, General User)
 * Jira Row 4: Verify Validation Table columns
 * Jira Row 5: Verify column sorting (all sortable except Reasons; default Batch ID desc, Type asc)
 * Jira Row 6: Verify pagination (Rows Per Page: 5, 10, 20, 50; default 20)
 * Jira Row 7: Verify multiple rows per Node ID (same record, multiple errors)
 */
WebUI.closeBrowser()

// ----- Jira Row 1: Setup - Navigate, Login, Data Submission tab -----
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.Pbac.createDataSubmission'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC04')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

// ----- Jira Row 2: Verify access to Validation Results tab -----
//CustomKeywords.'utilities.DataSubmissions.verifyValidationResultsTabDisplayed'()

// ----- Jira Row 3: PBAC - skipped -----

// ----- Jira Row 4: Validation Table columns -----
//CustomKeywords.'utilities.DataSubmissions.verifyDataSubmissionTableHeaders'('ValidationResults')

// ----- Jira Row 5: Column sorting -----
CustomKeywords.'utilities.DataSubmissions.verifyValidationResultsColumnSorting'()

// ----- Jira Row 6: Pagination options -----
CustomKeywords.'utilities.DataSubmissions.verifyValidationResultsPaginationOptions'()

// ----- Jira Row 7: Multiple rows per Node ID -----
CustomKeywords.'utilities.DataSubmissions.verifyMultipleRowsPerNodeIdInValidationResults'()

// Generate HTML report
CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

//WebUI.closeBrowser()
