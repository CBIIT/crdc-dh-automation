import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/*
 * TC05_DeleteRowsFromDataView
 * Sample test to exercise the deleteRowsFromDataView keyword.
 *
 * Flow:
 *   1. Login as Submitter
 *   2. Navigate to Data Submissions, create a new submission
 *   3. Open the submission, upload metadata (multiple program rows from PBAC folder)
 *   4. Go to Data View and delete only 1 row from the "program" node
 *   5. Close browser
 *
 * Prerequisites:
 *   - InputFiles/CRDC/MetadataData/PBAC/program.tsv must exist and contain multiple rows
 *   - Profile must have valid G_Urlname and credentials
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/Global/Header/Desktop/DataSubmissions-Tab')
CustomKeywords.'utilities.Pbac.createDataSubmission'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/List/Table/SubmissionName-Link')

CustomKeywords.'utilities.Pbac.uploadMetadataUI'('InputFiles/CRDC/MetadataData/PBAC/participant.tsv')
WebUI.delay(3)

CustomKeywords.'utilities.DataSubmissions.deleteRowsFromDataView'('participant', 37)

WebUI.closeBrowser()
