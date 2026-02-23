import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.By as By
import org.openqa.selenium.WebDriver as WebDriver

/*
 * TC21_CRDC-DeleteRowsFromDataView
 * Verifies the Data View delete button tooltip for metadata (records) and for data files.
 *
 * Flow:
 *   1. Login as Submitter
 *   2. Create data submission, open it, upload metadata (participant from PBAC)
 *   3. Go to Data View, verify delete button tooltip for metadata/records
 *   4. Upload data files via CLI (TC18)
 *   5. Go to Data View again, verify delete button tooltip for data files
 *   6. Generate validation report, close browser
 *
 * Prerequisites:
 *   - InputFiles/CRDC/MetadataData/PBAC/participant.tsv must exist
 *   - Profile must have valid G_Urlname and credentials
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/Global/Header/Desktop/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'both','GC','autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/List/Table/SubmissionName-Link')

CustomKeywords.'utilities.Pbac.uploadMetadataUI'('InputFiles/CRDC/MetadataData/PBAC/participant.tsv')
WebUI.delay(3)

// Navigate to Data View and verify delete button tooltip message for metadata
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Tabs/DataView-Tab')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
CustomKeywords.'utilities.WebUIUtils.verifyTooltipOnHover'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/DataView/Filters/DeleteNodeData-Btn'), 'Delete all the selected records from this data submission', 'Delete all the selected')


CustomKeywords.'utilities.CliUploader.uploadFilesViaCli'('TC18', 'datafile')

// Navigate to Data View and verify delete button tooltip message for data files
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Tabs/DataView-Tab')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
CustomKeywords.'utilities.WebUIUtils.verifyTooltipOnHover'(findTestObject('CRDC/AILocators/DataSubmissions/Detail/DataView/Filters/DeleteNodeData-Btn'), 'Delete all the selected data files from this data submission', 'Delete all the selected')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
