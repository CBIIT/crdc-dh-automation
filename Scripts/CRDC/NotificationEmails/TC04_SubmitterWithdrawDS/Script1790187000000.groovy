import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * Helper (not an inbox assertion): Submitter submits one DS via
 * Pbac.createSubmitAndWithdrawDataSubmission, then logEmails('DS Withdraw')
 * appends the expected-email row to OutputFiles/NotificationEmail_Checklist.xlsx.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
WebUI.setViewPortSize(1920, 1080)

def created = CustomKeywords.'utilities.Pbac.createSubmitAndWithdrawDataSubmission'()
CustomKeywords.'utilities.NotificationEmailLog.logEmails'('DS Withdraw', 'Submitter', created.id, created.name)

WebUI.closeBrowser()
