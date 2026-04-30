import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * CRDCDH-1405 TC102: Verify CLI creates metadata batches
 * visible in the UI.
 *
 * Uploads metadata via CLI and verifies the
 * metadata batch appears. 
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'both', 'GC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.CliUploader.uploadFilesViaCli'('CRDCDH-1405_TC102', 'metadata')
WebUI.delay(3)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Tabs/UploadActivities-Tab')
WebUI.delay(2)

CustomKeywords.'utilities.DataSubmissions.waitForUploadActivitiesTableNoUploading'(120)
CustomKeywords.'utilities.DataSubmissions.verifyUploadActivitiesBatchRow'('Metadata', 1, 'Uploaded')

CustomKeywords.'utilities.DatabaseUtils.verifyRecordsExistInMongoForSubmission'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
