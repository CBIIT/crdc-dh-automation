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
 - Creates a data submission and runs CLI upload with zip files but NO archive manifest (expectValidationErrors mode)
 - Verifies the CLI outputs the required error per zip via verifyCliErrorMessages
 - Aligns with CRDCDH-3027: missing archive manifest must produce error and block upload
 */
WebUI.closeBrowser()

// Login as Submitter
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

// Create DS and go to upload-activity
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'both', 'GC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

// Run CLI in "expect validation errors" mode with omitArchiveManifest so CLI emits "No archive manifest" error per zip
CustomKeywords.'utilities.CliUploader.uploadFilesViaCli'('CRDCDH-3027_TC30', 'datafile', [ expectValidationErrors: true, omitArchiveManifest: true ])

// Verify CLI error messages match expected (e.g. "No archive manifest found for X.zip, content of the zip archive cannot be validated.")
CustomKeywords.'utilities.CliUploader.verifyCliErrorMessages'('CRDCDH-3027_TC30')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
