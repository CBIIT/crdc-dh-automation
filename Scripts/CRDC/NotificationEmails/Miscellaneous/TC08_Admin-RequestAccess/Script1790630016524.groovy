import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * Helper (not an inbox assertion): User submits Profile Request Access via
 * Pbac.submitAccessRequest, then logEmails('Request Access') appends the
 * expected-email row to OutputFiles/NotificationEmail_Checklist.xlsx.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('User')

def created = CustomKeywords.'utilities.Pbac.submitAccessRequest'()
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('Request Access', 'User', created.id, created.name, 'Admin')

WebUI.closeBrowser()
