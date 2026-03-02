import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * CRDCDH-1195 TC101: Upload Errors modal verification after negative-scenario upload.
 *
 * Uploads invalid metadata via the UI (DataSubmissions.uploadFolder), then verifies
 * the Upload Errors link in the Upload Activities table opens the error-details modal
 * and the displayed errors match the expected errors from the expected TSV.
 *
 * Pre-requisite: InputFiles/CRDC/MetadataData/CRDCDH-1195_TC101/ contains invalid metadata
 * TSV(s) (e.g. invalid file_id) and expected/*.tsv with expected_errors column.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-1195_TC101')

CustomKeywords.'utilities.DataSubmissions.verifyUploadActivitiesBatchRow'('Metadata', 1, 'Failed')

CustomKeywords.'utilities.DataSubmissions.verifyUploadActivitiesFailedErrorsModal'('CRDCDH-1195_TC101')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()