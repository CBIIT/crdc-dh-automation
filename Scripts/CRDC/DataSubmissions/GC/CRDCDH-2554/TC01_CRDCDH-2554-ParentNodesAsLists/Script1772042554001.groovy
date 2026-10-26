import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC01_CRDCDH-2554-ParentNodesAsLists
 *
 * Flow:
 * 1. Login as Submitter; open Data Submissions.
 * 2. Create metadata-only GC DS for autoQA - AutomationQA_Study.
 * 3. Upload InputFiles/CRDC/MetadataData/CRDCDH-2554 (file rows with pipe parent sample IDs).
 * 4. Data View matches the upload TSVs, including sample.sample_id pipe lists.
 * 5. Validate.
 * 6. Validation Results Expanded: no Error rows (verifyNoValidationErrors). Never Submit.
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-2554')
CustomKeywords.'utilities.DataSubmissions.verifyDataViewMatchesMetadataForTC'('CRDCDH-2554')

//Optional data validation and check no errors
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults-Tab')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults_AggregatedExpanded-Btn')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.DataSubmissions.verifyNoValidationErrors'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
