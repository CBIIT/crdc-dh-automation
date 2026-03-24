import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * CRDCDH-887 TC104: Upload Errors modal verification after negative-scenario upload for ICDC.
 *
 * Uploads invalid metadata via the UI (DataSubmissions.uploadFolder), then verifies
 * the Upload Errors link in the Upload Activities table opens the error-details modal
 * and the displayed errors match the expected errors from the expected TSV.
 *
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'ICDC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-887_TC104')

CustomKeywords.'utilities.DataSubmissions.verifyUploadActivitiesBatchRow'('Metadata', 4, 'Failed')

CustomKeywords.'utilities.DataSubmissions.verifyUploadActivitiesFailedErrorsModal'('CRDCDH-887_TC104')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()