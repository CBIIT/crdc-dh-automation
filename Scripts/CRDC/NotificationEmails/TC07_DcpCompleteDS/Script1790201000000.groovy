import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * Helper (not an inbox assertion): Submitter submits one DS, DCP Releases and
 * Completes via Pbac.createSubmitReleaseAndCompleteDataSubmission, then
 * logEmails('DS Complete') appends the expected-email row to
 * OutputFiles/NotificationEmail_Checklist.xlsx.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')

def created = CustomKeywords.'utilities.Pbac.createSubmitReleaseAndCompleteDataSubmission'()
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('DS Complete', 'Dcp', created.id, created.name)

WebUI.closeBrowser()
