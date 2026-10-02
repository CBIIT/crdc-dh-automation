import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

/*
 * Helper (not an inbox assertion): Submitter submits one SR, switches to Fedlead,
 * Fedlead Approves it, Fedlead Reopens the Approved request from the list, then
 * logEmails appends approve and reopen rows to OutputFiles/NotificationEmail_Checklist.xlsx.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')

CustomKeywords.'utilities.SubmissionRequest.startSubmissionRequest'()
CustomKeywords.'utilities.SubmissionRequest.fillSubmissionRequestAllSections'(2, 'Other', 6, 1, 1, 'Open',
	'Yes', 'Cholangiocarcinoma', 'Homo', 1, 'genomics', 'proteomics', 5, 6, 1, 1)
String srId = CustomKeywords.'utilities.SubmissionRequest.getApplicationIdFromUrl'()
String studyName = SubmissionRequest.lastEnteredStudyTitle

CustomKeywords.'utilities.WebUIUtils.waitUntilEnabledOrStop'(findTestObject('CRDC/SubmissionRequest/Submit-Btn'), 30, 'SR Submit')
CustomKeywords.'utilities.SubmissionRequest.clickSubmitButton'()

CustomKeywords.'utilities.Login.switchUser'('Fedlead')
CustomKeywords.'utilities.SubmissionRequest.approveSubmissionRequest'(studyName)
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('SR Approve', 'Fedlead', srId, studyName, 'Submitter')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('SR Approve', 'Fedlead', srId, studyName, 'FedleadBcc')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('SR Approve', 'Fedlead', srId, studyName, 'DcpBcc')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('SR Approve', 'Fedlead', srId, studyName, 'AdminBcc')

CustomKeywords.'utilities.SubmissionRequest.reopenApprovedSubmissionRequest'(studyName)
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('SR Reopen', 'Fedlead', srId, studyName, 'Submitter')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('SR Reopen', 'Fedlead', srId, studyName, 'FedleadBcc')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('SR Reopen', 'Fedlead', srId, studyName, 'DcpBcc')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('SR Reopen', 'Fedlead', srId, studyName, 'AdminBcc')

WebUI.closeBrowser()
