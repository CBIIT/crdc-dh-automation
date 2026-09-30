import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

/*
 * Helper (not an inbox assertion): User submits one SR, switches to Fedlead,
 * Fedlead Rejects it, then logEmails('SR Reject') appends the expected-email row
 * to OutputFiles/NotificationEmail_Checklist.xlsx.
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

CustomKeywords.'utilities.WebUIUtils.waitUntilEnabledOrStop'(findTestObject('CRDC/SubmissionRequest/Submit-Btn'), 30, 'SR Submit')
CustomKeywords.'utilities.SubmissionRequest.clickSubmitButton'()

CustomKeywords.'utilities.Login.switchUser'('Fedlead')
CustomKeywords.'utilities.SubmissionRequest.rejectSubmissionRequest'(studyName)

CustomKeywords.'utilities.NotificationEmailLog.logEmails'('SR Reject', 'Fedlead', srId, studyName, 'User')

WebUI.closeBrowser()
