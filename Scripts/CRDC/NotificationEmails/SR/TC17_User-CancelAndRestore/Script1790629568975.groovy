import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

/*
 * Helper (not an inbox assertion): User fills one In Progress SR, Cancels it,
 * Restores it from the list, then logEmails appends cancel and restore rows to
 * OutputFiles/NotificationEmail_Checklist.xlsx.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('User')

CustomKeywords.'utilities.SubmissionRequest.startSubmissionRequest'()
CustomKeywords.'utilities.SubmissionRequest.fillSubmissionRequestAllSections'(2, 'Other', 6, 1, 1, 'Open',
	'Yes', 'Cholangiocarcinoma', 'Homo', 1, 'genomics', 'proteomics', 5, 6, 1, 1)
String srId = CustomKeywords.'utilities.SubmissionRequest.getApplicationIdFromUrl'()
String studyName = SubmissionRequest.lastEnteredStudyTitle

CustomKeywords.'utilities.SubmissionRequest.cancelSubmissionRequestButton'()
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('SR Cancel', 'User', srId, studyName, 'User')

CustomKeywords.'utilities.SubmissionRequest.restoreSubmissionRequest'(studyName)
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('SR Restore', 'User', srId, studyName, 'User')

WebUI.closeBrowser()
