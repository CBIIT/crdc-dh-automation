import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

/*
 * Helper (not an inbox assertion): Submitter submits one Open-access SR, Fedlead
 * Approves it with Require Data Model changes, Admin clears that pending flag,
 * then logEmails appends the model-pending and pending-cleared rows to
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

CustomKeywords.'utilities.WebUIUtils.waitUntilEnabledOrStop'(findTestObject('CRDC/SubmissionRequest/Submit-Btn'), 30, 'SR Submit')
CustomKeywords.'utilities.SubmissionRequest.clickSubmitButton'()

CustomKeywords.'utilities.Login.switchUser'('Fedlead')
CustomKeywords.'utilities.SubmissionRequest.approveSubmissionRequestWithModelChangePending'(studyName)
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('SR Conditional Model', 'Fedlead', srId, studyName, 'Submitter')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('SR Conditional Model', 'Fedlead', srId, studyName, 'FedleadBcc')

CustomKeywords.'utilities.Login.switchUser'('Admin')
CustomKeywords.'utilities.ManageStudies.navigateToManageStudies'()
CustomKeywords.'utilities.ManageStudies.editExistingStudy'(studyName, [pendingConditions: [false]])
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('SR Pending Cleared', 'Admin', srId, studyName, 'Submitter')
CustomKeywords.'utilities.NotificationEmailLog.logBccEmails'('SR Pending Cleared', 'Admin', srId, studyName, 'FedleadBcc')

WebUI.closeBrowser()
