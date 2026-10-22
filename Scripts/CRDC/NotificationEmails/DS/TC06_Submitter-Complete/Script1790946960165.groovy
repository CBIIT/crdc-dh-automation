import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.DataSubmissions

/*
 * Helper (not an inbox assertion): Submitter submits one DS, switches to DCP,
 * DCP Releases and Completes it, then logEmails('DS Complete') appends the
 * expected-email row to OutputFiles/NotificationEmail_Checklist.xlsx.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')

WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'Metadata Only', 'GC', 'ATS - AutoTest-Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Loading-Icon', 60)
String dsId = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()
String dsName = DataSubmissions.expectedSubmissionName

CustomKeywords.'utilities.Pbac.prepareUniqueMetadataAndUpload'('InputFiles/CRDC/MetadataData/PBAC/program.tsv', 'program_acronym')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
CustomKeywords.'utilities.DataSubmissions.submitDataSubmission'()

CustomKeywords.'utilities.Login.switchUser'('Dcp')
CustomKeywords.'utilities.DataSubmissions.releaseDataSubmission'(dsId)
WebUI.delay(5) //account for race condition where DS Completed email is not received if completing immediately after release 
CustomKeywords.'utilities.DataSubmissions.completeDataSubmission'()

CustomKeywords.'utilities.NotificationEmailLog.logEmails'('DS Complete', 'Dcp', dsId, dsName, 'Submitter')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('DS Complete', 'Dcp', dsId, dsName, 'FedleadBcc')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('DS Complete', 'Dcp', dsId, dsName, 'DcpBcc')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('DS Complete', 'Dcp', dsId, dsName, 'AdminBcc')

WebUI.closeBrowser()
