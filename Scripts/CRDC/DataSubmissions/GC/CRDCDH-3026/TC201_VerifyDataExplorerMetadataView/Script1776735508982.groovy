import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable
import utilities.DataSubmissions;

/**
 * TC201_CRDCDH-3026: Verifies Data Explorer metadata view for released study data.
 * QA seed (manual): Hub study automation_CompletedStudy - AutomationCompletedStudy (program/submitter linked);
 * upload InputFiles/CRDC/MetadataData/CRDCDH-3026/, validate, release. TC does not create the DS.
 * - Logs in as Submitter, filters Data Explorer by study acronym, opens first list row,
 * - Verifies metadata for program, study, participant, sample vs CRDCDH-3026 expected TSVs.
 * - Generates ValidationReport HTML at the end.
 */
WebUI.closeBrowser()

//Login to verify data explorer page
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)
CustomKeywords.'utilities.ValidationReport.clearValidationReportCollector'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataExplorer-Tab')
WebUI.delay(2)
WebUI.sendKeys(findTestObject('CRDC/AILocators/DataExplorer/List/Filters/StudyName-TxtBx'), 'automation_CompletedStudy')
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataExplorer/StudyView/DataSubmissionName-Link')
WebUI.delay(2)
// Verify Data Explorer metadata view for program and study node types; expected TSVs under CRDCDH-3026
CustomKeywords.'utilities.DataExplorer.verifyDataExplorerMetadataView'('CRDCDH-3026', 'program', 'study', 'participant','sample')
CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
