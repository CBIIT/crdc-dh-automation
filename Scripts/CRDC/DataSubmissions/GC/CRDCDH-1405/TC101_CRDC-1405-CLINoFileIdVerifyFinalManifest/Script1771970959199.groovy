import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * CRDCDH-1405 TC101: CLI data file upload without file_id column -- verify
 * API-generated IDs and final manifest.
 *
 * Uploads data files via CLI with a manifest that has NO file_id column.
 * After upload, verifies the final manifest (-final.tsv) is generated with:
 *   - A file_id column present
 *   - All file_id values in valid DRS ID format (dg.4DFC/<uuid>)
 *   - Row count matching the original manifest
 *   - Deterministic file_ids: re-runs upload and verifies identical file_ids across runs
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'both', 'GC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.CliUploader.uploadFilesViaCli'('CRDCDH-1405_TC101', 'datafile')

CustomKeywords.'utilities.CliUploader.verifyFinalManifest'('CRDCDH-1405_TC101')

CustomKeywords.'utilities.CliUploader.verifyFinalManifestDeterministic'('CRDCDH-1405_TC101')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
