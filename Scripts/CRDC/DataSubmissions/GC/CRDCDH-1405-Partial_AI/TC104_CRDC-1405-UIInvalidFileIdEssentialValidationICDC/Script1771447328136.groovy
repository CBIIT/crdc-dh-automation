import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * CRDCDH-1405 TC104: UI metadata upload with invalid file_id -- Essential
 * Validation batch error (ICDC data commons).
 *
 * Same as TC103 but for ICDC data commons where the DCF prefix is omitted.
 * Expected error message uses bare UUID format:
 *   "Line N: "value" is not in correct format for file ID property file_id.
 *    A correct file ID should look like "e041576e-3595-5c8b-b0b3-272bc7cb6aa8""
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metada only', 'ICDC', 'autoQA - AutomationQA_ICDC_Study')
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-1405_TC104')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Tabs/ValidationResults-Tab')
WebUI.delay(2)

CustomKeywords.'utilities.DataSubmissions.verifyValidationResultsVsExpectedForTC'('CRDCDH-1405_TC104')

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
