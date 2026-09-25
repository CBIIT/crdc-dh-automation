import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

/*
 * Helper (not an inbox assertion): Submitter fills one In Progress SR and Cancels it,
 * then logEmails('SR Cancel') appends the expected-email row to
 * OutputFiles/NotificationEmail_Checklist.xlsx.
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

CustomKeywords.'utilities.SubmissionRequest.cancelSubmissionRequestButton'()

CustomKeywords.'utilities.NotificationEmailLog.logEmails'('SR Cancel', 'Submitter', srId, studyName, 'Submitter')

WebUI.closeBrowser()
