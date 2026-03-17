import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * CRDCDH-1405 TC112: CLI data file upload with invalid size and md5 values for CTDC submission.
 *
 * Uploads data files via CLI with a manifest containing invalid size and md5 values
 * and verifies
 * the CLI produces the correct error messages in the upload report.
 *
 * Pre-requisite: manifest.tsv in CRDCDH-1405_TC102/DataFiles/
 * column with intentionally invalid values.
 *
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'both', 'CTDC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.CliUploader.uploadFilesViaCli'('CRDCDH-1405_TC112', 'datafile')

CustomKeywords.'utilities.CliUploader.verifyCliErrorMessages'('CRDCDH-1405_TC112')

CustomKeywords.'utilities.DatabaseUtils.verifyNoRecordsInMongoForSubmission'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
