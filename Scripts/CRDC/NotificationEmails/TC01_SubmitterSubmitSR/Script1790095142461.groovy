import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * Helper (not an inbox assertion): Submitter creates and submits one SR via
 * Pbac.createAndSubmitSubmissionRequest, then logEmails('SR Submit') appends
 * expected-email rows to OutputFiles/NotificationEmail_Checklist.xlsx.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
WebUI.setViewPortSize(1920, 1080)

def created = CustomKeywords.'utilities.Pbac.createAndSubmitSubmissionRequest'()
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('SR Submit', 'Submitter', created.id, created.name)

WebUI.closeBrowser()
