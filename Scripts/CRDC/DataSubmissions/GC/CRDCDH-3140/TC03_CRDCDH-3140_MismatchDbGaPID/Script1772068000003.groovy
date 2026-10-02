import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * CRDCDH-3140: phs_accession phs001234 does not match registered dbGaP phs123456.
 * Expect dbGaPID mismatch on Validation Results (study node).
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC', 'autoQAdbGaPID - AutomationQA_dbGaPID')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-3140_TC03')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

CustomKeywords.'utilities.DataSubmissions.verifyErrorDetailsForSubmittedIdForTC'('CRDCDH-3140_TC03')
CustomKeywords.'utilities.DataSubmissions.verifyValidationResultsVsExpectedForTC'('CRDCDH-3140_TC03')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
