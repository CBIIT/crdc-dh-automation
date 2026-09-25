import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * CRDCDH-1405 TC113: Verify validation error message after deleting row from Data View for CTDC submission.
 *
 * Uploads metadata and data files via CLI, delete record from Data View and verifies
 * the the correct error message in Validation Results.
 *
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'both', 'CTDC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.CliUploader.uploadFilesViaCli'('CRDCDH-699_TC102', 'datafile')

CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-699_TC102')

CustomKeywords.'utilities.DataSubmissions.deleteRowsFromDataView'('data file', 1)

//Validate data
CustomKeywords.'utilities.DataSubmissions.selectValidationTypeBoth'()
WebUI.click(findTestObject('Object Repository/CRDC/DataSubmissions/Validation/ValidationTarget-AllUploadedData-RadioBtn'))
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

//Handle the persistent validation purple alert that never disappears and blocks the dropdown selection in the next step unless the focus is moved-- unique to this CTDC test data
CustomKeywords.'utilities.WebUIUtils.scrollToElement'(findTestObject('CRDC/NavBar/DataSubmissions-Tab'))

CustomKeywords.'utilities.DataSubmissions.verifyValidationResultsVsExpectedForTC'("CRDCDH-699_TC102")

CustomKeywords.'utilities.WebUIUtils.clickTab'("Object Repository/CRDC/DataSubmissions/Validation/ValidationResults_AggregatedExpanded-Btn")

CustomKeywords.'utilities.DataSubmissions.verifyErrorDetailsForSubmittedIdForTC'('CRDCDH-699_TC102')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
