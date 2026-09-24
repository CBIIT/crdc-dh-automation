import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * Helper (not an inbox assertion): Submitter creates and submits one DS via
 * Pbac.createUploadValidateAndSubmitDataSubmission, then logEmails appends
 * create/submit expected-email rows to OutputFiles/NotificationEmail_Checklist.xlsx.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')

def created = CustomKeywords.'utilities.Pbac.createUploadValidateAndSubmitDataSubmission'()
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('DS Create', 'Submitter', created.id, created.name)
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('DS Submit', 'Submitter', created.id, created.name)

WebUI.closeBrowser()
