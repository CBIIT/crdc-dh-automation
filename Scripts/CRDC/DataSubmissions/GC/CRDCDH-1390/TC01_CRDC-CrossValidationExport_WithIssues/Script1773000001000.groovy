import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * CRDCDH-1390 TC01 — Cross Validation Results CSV export (DCP, with issues)
 *
 * Flow (same idea as TC06 / TC25):
 *   Submitter creates two metadata submissions for the same study, uploads the same PBAC program.tsv
 *   to both, validates and submits each. DCP opens the second submission, runs Cross Validate, then
 *   checks the Cross Validation Results tab.
 *
 * verifyCrossValidationResultsVsExpected('SampleTC_CrossValidation') uses expected rows under
 * InputFiles/CRDC/MetadataData/SampleTC_CrossValidation/expected-CrossValidationResults/ to assert
 * required conflicts appear in the UI (not a full row-count contract for the whole table).
 *
 * Export checks: two cloud export controls, enabled, tooltip text, download filename pattern, and
 * CSV data row count matches the Cross Validation table row count (lastCrossValidationTableRowCount).
 */

WebUI.closeBrowser()

// Submitter — submission 1: upload program metadata, validate, submit (ID not needed later)
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.Pbac.createDataSubmission'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/List/Table/SubmissionName-Link')

CustomKeywords.'utilities.Pbac.uploadMetadataUI'('InputFiles/CRDC/MetadataData/PBAC/program.tsv')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
WebUI.delay(2)

CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()

// Submitter — submission 2: same program data (cross-submission duplicate key for S001 / conflicts)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.Pbac.createDataSubmission'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/List/Table/SubmissionName-Link')

CustomKeywords.'utilities.Pbac.uploadMetadataUI'('InputFiles/CRDC/MetadataData/PBAC/program.tsv')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Submit-Btn')
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/SubmitYes-Btn')
WebUI.delay(2)

String submissionId2 = CustomKeywords.'utilities.DataSubmissions.getSubmissionIdFromUploadActivityUrl'()

CustomKeywords.'utilities.SubmissionRequest.logoutFromCrdc'()
WebUI.closeBrowser()

// DCP — navigate to submission 2, cross-validate, assert table vs expected TSV, then export UI
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Dcp')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(submissionId2)
CustomKeywords.'utilities.DataSubmissions.clickCrossValidateButton'()

CustomKeywords.'utilities.DataSubmissions.verifyCrossValidationResultsVsExpected'('SampleTC_CrossValidation')

// Export: two toolbar instances, tooltip, filename, CSV rows match UI table row count
CustomKeywords.'utilities.WebUIUtils.verifyElementCount'(
		findTestObject('CRDC/AILocators/DataSubmissions/Detail/CrossValidation/Filters/ExportCrossValidation-Btn'), 2)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'(
		findTestObject('CRDC/AILocators/DataSubmissions/Detail/CrossValidation/Filters/ExportCrossValidation-Btn'), true)
CustomKeywords.'utilities.WebUIUtils.verifyTooltipOnHover'(
		findTestObject('CRDC/AILocators/DataSubmissions/Detail/CrossValidation/Filters/ExportCrossValidationTooltip'),
		'Export all cross validation issues to a CSV file',
		'Export all cross validation')

// Submission name from summary for export filename prefix (full name may live on aria-label if truncated)
WebUI.waitForElementPresent(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Summary/SubmissionName-Lbl'), 15)
String submissionNameForExport = WebUI.getText(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Summary/SubmissionName-Lbl'))?.trim()
if (!submissionNameForExport) {
	submissionNameForExport = WebUI.getAttribute(findTestObject('CRDC/AILocators/DataSubmissions/Detail/Summary/SubmissionName-Lbl'), 'aria-label')
}
CustomKeywords.'utilities.DataSubmissions.clickCrossValidationExportAndVerifyDownloadedFileName'(submissionNameForExport)
CustomKeywords.'utilities.DataSubmissions.verifyCrossValidationExportCsvRowCountMatchesUi'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
