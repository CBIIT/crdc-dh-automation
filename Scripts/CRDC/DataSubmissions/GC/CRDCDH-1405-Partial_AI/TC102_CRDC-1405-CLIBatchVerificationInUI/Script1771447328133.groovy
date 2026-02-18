import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * CRDCDH-1405 TC102: Verify CLI creates metadata and data file batches
 * visible in the UI.
 *
 * Uploads data files via CLI, then verifies the data file batch appears in
 * the Upload Activities tab. Then uploads metadata via CLI and verifies the
 * metadata batch also appears. Confirms CLI creates batches on behalf of
 * the submitter that are reflected in the UI.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'both', 'GC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.CliUploader.uploadFilesViaCli'('CRDCDH-1405_TC102', 'datafile')
WebUI.delay(3)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Tabs/UploadActivities-Tab')
WebUI.delay(2)

//TODO: After navigating to Upload Activities, verify a datafile batch row is visible.
//      Check that a row exists with batch type "Data File", correct file count (3 files),
//      and a non-error status (e.g. "Uploaded" or "Loaded").
//      Use WebUIUtils.verifyElementText or Utils.getListWebElements to read table rows
//      and assert the expected batch attributes.

CustomKeywords.'utilities.CliUploader.uploadFilesViaCli'('CRDCDH-1405_TC102', 'metadata')
WebUI.delay(3)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Tabs/UploadActivities-Tab')
WebUI.delay(2)

//TODO: After the metadata CLI upload, verify a metadata batch row now also appears in
//      Upload Activities. Confirm batch type is "Metadata", file count matches the
//      uploaded metadata TSV count, and status is appropriate.

//TODO: Verify that both batches were created "on behalf of the submitter" -- check the
//      batch owner/submitter column in the Upload Activities table matches the logged-in user.

//TODO: Add ValidationReport.generateValidationReport() so results are captured in the HTML report
// CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
