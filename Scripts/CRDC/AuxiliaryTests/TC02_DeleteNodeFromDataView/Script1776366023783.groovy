import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/* TC02_DeleteNodeFromDataView
 * Sample test to exercise the deleteNodeFromDataView keyword.
 *
 * Flow:
 *   1. Login as Submitter
 *   2. Navigate to Data Submissions, create a new submission
 *   3. Open the submission, upload metadata (program.tsv from PBAC folder)
 *   4. Go to Data View and delete the "program" node
 *   5. Close browser
 *
 * Prerequisites:
 *   - InputFiles/CRDC/MetadataData/PBAC/program.tsv must exist
 *   - Profile must have valid G_Urlname and credentials
 */

WebUI.closeBrowser()

// Step 1: Login
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

// Step 2: Create a new Data Submission
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/Global/Header/Desktop/DataSubmissions-Tab')
CustomKeywords.'utilities.Pbac.createDataSubmission'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/List/Table/SubmissionName-Link')

// Step 3: Upload metadata
CustomKeywords.'utilities.Pbac.prepareUniqueMetadataAndUpload'('InputFiles/CRDC/MetadataData/PBAC/program.tsv', "program_acronym")
WebUI.delay(3)

// Step 4: Delete the "program" node from Data View
CustomKeywords.'utilities.DataSubmissions.deleteNodeFromDataView'('program')

// Step 5: Done
WebUI.closeBrowser()
