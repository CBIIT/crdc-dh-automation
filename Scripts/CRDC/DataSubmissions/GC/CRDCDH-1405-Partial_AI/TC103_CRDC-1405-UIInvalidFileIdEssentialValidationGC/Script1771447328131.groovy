import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * CRDCDH-1405 TC103: UI metadata upload with invalid file_id -- Essential
 * Validation batch error (GC data commons).
 *
 * Uploads a file manifest TSV via UI with invalid file_id values for the GC
 * data commons. Triggers validation and verifies Essential Validation produces
 * batch errors with expected error messages:
 *   "Line N: "value" is not in correct format for file ID property file_id.
 *    A correct file ID should look like "dg.4DFC/e041576e-...""
 *
 * The entire template should fail essential validation and no data will be
 * loaded into the database.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metada only', 'GC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-1405_TC103')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Tabs/ValidationResults-Tab')
WebUI.delay(2)

CustomKeywords.'utilities.DataSubmissions.verifyValidationResultsVsExpectedForTC'('CRDCDH-1405_TC103')

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
