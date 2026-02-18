import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * CRDCDH-1405 TC105: CLI metadata upload with invalid file_id -- Essential
 * Validation batch error (GC data commons).
 *
 * Uploads a file manifest TSV as metadata via CLI (metadata config type)
 * with invalid file_id values. Then verifies Essential Validation in the UI
 * produces batch errors. This tests the same essential validation as TC103
 * but uses the CLI metadata upload path instead of the UI.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metada only', 'GC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.CliUploader.uploadFilesViaCli'('CRDCDH-1405_TC105', 'metadata')
WebUI.delay(3)

//TODO: Before triggering validation, verify the CLI metadata batch appears in Upload Activities.
//      Navigate to the Upload Activities tab and confirm a metadata batch row is visible,
//      confirming the CLI created the batch on behalf of the submitter.
// CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Tabs/UploadActivities-Tab')
// WebUI.delay(2)
// -- assert metadata batch row exists with expected type and file count --

CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Tabs/ValidationResults-Tab')
WebUI.delay(2)

CustomKeywords.'utilities.DataSubmissions.verifyValidationResultsVsExpectedForTC'('CRDCDH-1405_TC105')

//TODO: Verify no data was loaded into the database after failed essential validation.
//      Per Jira: "the entire template should fail the essential validation and no data
//      will be loaded into database." Navigate to the Data View tab and confirm it shows
//      0 records for the 'file' node (or that the Data View tab is empty/disabled).
// CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Tabs/DataView-Tab')
// WebUI.delay(2)
// -- assert file node row count is 0 or Data View table is empty --

//TODO: Verify the submission status reflects the failed essential validation.
//      Check the submission detail header or status element shows an appropriate
//      state (e.g. not "Submitted" or "Released") after the batch error.

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
