import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * Helper (not an inbox assertion): Submitter creates one New DS via
 * Pbac.createAndCancelDataSubmission, then logEmails('DS Cancel') appends
 * the expected-email row to OutputFiles/NotificationEmail_Checklist.xlsx.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')

def created = CustomKeywords.'utilities.Pbac.createAndCancelDataSubmission'()
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('DS Cancel', 'Submitter', created.id, created.name)

WebUI.closeBrowser()
