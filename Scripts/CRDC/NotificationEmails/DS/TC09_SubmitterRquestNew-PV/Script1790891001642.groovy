import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.DataSubmissions

/*
 * Helper (not an inbox assertion): Submitter creates one DS, uploads metadata,
 * validates, and requests a new permissible value. logEmails('DS PV Request')
 * appends the expected-email row to OutputFiles/NotificationEmail_Checklist.xlsx.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')

WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'Metadata Only', 'GC', 'ATS - AutoTest-Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Loading-Icon', 60)
String dsId = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()
String dsName = DataSubmissions.expectedSubmissionName

//Upload data via folder
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC09_SubmitterRquestNew-PV')

//Validate data
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

CustomKeywords.'utilities.DataSubmissions.requestNewPermissibleValue'('chek234', 'Auto Test Justification')

CustomKeywords.'utilities.NotificationEmailLog.logEmails'('DS PV Request', 'Submitter', dsId, dsName, 'Dcp')

WebUI.closeBrowser()
