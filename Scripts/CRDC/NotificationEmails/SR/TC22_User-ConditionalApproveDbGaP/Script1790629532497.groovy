import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

/*
 * Helper (not an inbox assertion): User submits one Controlled-access SR
 * with dbGaP unregistered, Fedlead Approves it with no pending checkboxes, then
 * logEmails appends the dbGaP-pending row to OutputFiles/NotificationEmail_Checklist.xlsx.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('User')

CustomKeywords.'utilities.SubmissionRequest.startSubmissionRequest'()
CustomKeywords.'utilities.SubmissionRequest.fillSubmissionRequestAllSections'(2, 'Other', 6, 1, 1, 'Controlled',
	'No', 'Cholangiocarcinoma', 'Homo', 1, 'genomics', 'proteomics', 5, 6, 1, 1)
String srId = CustomKeywords.'utilities.SubmissionRequest.getApplicationIdFromUrl'()
String studyName = SubmissionRequest.lastEnteredStudyTitle

CustomKeywords.'utilities.WebUIUtils.waitUntilEnabledOrStop'(findTestObject('CRDC/SubmissionRequest/Submit-Btn'), 30, 'SR Submit')
CustomKeywords.'utilities.SubmissionRequest.clickSubmitButton'()

CustomKeywords.'utilities.Login.switchUser'('Fedlead')
CustomKeywords.'utilities.SubmissionRequest.approveSubmissionRequest'(studyName)
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('SR Conditional dbGaP', 'Fedlead', srId, studyName, 'User')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('SR Conditional dbGaP', 'Fedlead', srId, studyName, 'FedleadBcc')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('SR Conditional dbGaP', 'Fedlead', srId, studyName, 'DcpBcc')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('SR Conditional dbGaP', 'Fedlead', srId, studyName, 'AdminBcc')

WebUI.closeBrowser()
