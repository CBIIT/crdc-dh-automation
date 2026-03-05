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

/* This test script:
 - Logs in as Submitter
 - Creates a data submission and runs CLI upload with a zip file and archive manifest that has a WRONG COLUMN NAME (e.g. "archive" instead of "archive_name") (expectValidationErrors mode)
 - Verifies the CLI outputs the expected error via verifyCliErrorMessages
 - Aligns with requirement: archive manifest must contain archive_name, file_path, file_size, md5
 */
WebUI.closeBrowser()

// Login as Submitter
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

// Create DS and go to upload-activity
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'both', 'GC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

// Run CLI in "expect validation errors" mode (archive manifest has wrong column name - 'archive' instead of 'archive_name')
CustomKeywords.'utilities.CliUploader.uploadFilesViaCli'('CRDCDH-3027_TC33', 'datafile', [ expectValidationErrors: true ])

// Verify CLI error messages match expected
CustomKeywords.'utilities.CliUploader.verifyCliErrorMessages'('CRDCDH-3027_TC33')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
