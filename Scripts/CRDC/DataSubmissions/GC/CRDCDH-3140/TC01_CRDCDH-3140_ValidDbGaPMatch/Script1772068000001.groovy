import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * CRDCDH-3140: phs_accession matches registered dbGaP ID (phs123456) on study
 * autoQAdbGaPID - AutomationQA_dbGaPID. Expect no Validation Results errors.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC', 'autoQAdbGaPID - AutomationQA_dbGaPID')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-3140_TC01')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults-Tab')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults_AggregatedExpanded-Btn')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.DataSubmissions.verifyNoValidationErrors'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
