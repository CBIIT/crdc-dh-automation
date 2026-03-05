import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * CRDCDH-1405 TC103: CLI data file upload with invalid data_file_uuid format for CTDC submission.
 *
 * Uploads data files via CLI with a manifest containing invalid data_file_uuid values
 * and verifies
 * the CLI produces the correct error messages in the upload report.
 *
 * Pre-requisite: manifest.tsv in CRDCDH-1405_TC103/DataFiles/ has a data_file_uuid
 * column with intentionally invalid values.
 *
 * Expected CLI error format:
 *   Line N: "data_file_uuid": "dg.4DFC/test" is not in correct format. A correct "data_file_uuid" should look like "dg.4DFC/e041576e-3595-5c8b-b0b3-272bc7cb6aa8". You can provide correct "data_file_uuid" or remove the column and let the system generate it for you.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'both', 'CTDC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.CliUploader.uploadFilesViaCli'('CRDCDH-1405_TC103', 'datafile')

CustomKeywords.'utilities.CliUploader.verifyCliErrorMessages'('CRDCDH-1405_TC103')

CustomKeywords.'utilities.DatabaseUtils.verifyNoRecordsInMongoForSubmission'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
