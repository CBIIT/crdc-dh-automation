import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * Helper (not an inbox assertion): Master inactivates each PBAC automation
 * user, logEmails('Account Disabled') appends the expected-email row, then
 * restores Active. Inactive is the only send; Active is restore only.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Master')

//Manage users
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/Login/UserProfile-Dd')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/ManageUsers-Btn')

CustomKeywords.'utilities.Pbac.disableThenRestoreUserAccounts'(['Admin', 'Fedlead', 'Dcp', 'Submitter', 'User'])

WebUI.closeBrowser()
