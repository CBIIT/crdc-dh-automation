import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.DataSubmissions

/*
 * Helper (not an inbox assertion): Submitter creates one DS, switches to DCP,
 * DCP changes the submitter, then logEmails / logCcEmails / logBccEmails append
 * expected rows (To new submitter, Cc prior submitter) to the checklist.
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

CustomKeywords.'utilities.Login.switchUser'('Dcp')
CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(dsId)

CustomKeywords.'utilities.DataSubmissions.updateSubmitterName'()

CustomKeywords.'utilities.NotificationEmailLog.logEmails'('DS Config Change', 'Dcp', dsId, dsName, 'AlternateSubmitter')
CustomKeywords.'utilities.NotificationEmailLog.logCcEmails'('DS Config Change', 'Dcp', dsId, dsName, 'SubmitterCc')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('DS Config Change', 'Dcp', dsId, dsName, 'FedleadBcc')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('DS Config Change', 'Dcp', dsId, dsName, 'DcpBcc')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('DS Config Change', 'Dcp', dsId, dsName, 'AdminBcc')

WebUI.closeBrowser()
