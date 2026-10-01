import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.DataSubmissions

/*
 * Helper (not an inbox assertion): Submitter submits one DS and Withdraws it,
 * then logEmails('DS Withdraw') appends the expected-email row to
 * OutputFiles/NotificationEmail_Checklist.xlsx.
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
CustomKeywords.'utilities.DataSubmissions.withdrawDataSubmission'()

CustomKeywords.'utilities.NotificationEmailLog.logEmails'('DS Withdraw', 'Submitter', dsId, dsName, 'Dcp')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('DS Withdraw', 'Submitter', dsId, dsName, 'FedleadBcc')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('DS Withdraw', 'Submitter', dsId, dsName, 'AdminBcc')

WebUI.closeBrowser()
