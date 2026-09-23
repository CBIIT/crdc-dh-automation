import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * Helper (not an inbox assertion): Submitter submits one DS, DCP Rejects via
 * Pbac.createSubmitAndRejectDataSubmission, then logEmails('DS Reject')
 * appends the expected-email row to OutputFiles/NotificationEmail_Checklist.xlsx.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
WebUI.setViewPortSize(1920, 1080)

def created = CustomKeywords.'utilities.Pbac.createSubmitAndRejectDataSubmission'()
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('DS Reject', 'Dcp', created.id, created.name)

WebUI.closeBrowser()
