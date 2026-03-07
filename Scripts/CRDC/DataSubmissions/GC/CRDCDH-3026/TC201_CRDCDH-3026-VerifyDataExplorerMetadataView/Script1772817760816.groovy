import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/**
 * TC201_CRDCDH-3026: Verifies Data Explorer metadata view for released study data.
 * - Logs in as Submitter, navigates to Data Explorer, opens the first study from the list,
 * - Verifies metadata table for specified node types (program, study) against expected TSVs
 *   from InputFiles/CRDC/MetadataData/CRDCDH-3026/ (or Metadata/ subfolder).
 * - Generates ValidationReport HTML at the end.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.ValidationReport.clearValidationReportCollector'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataExplorer-Tab')
WebUI.delay(2)

// Wait for Data Explorer list and open the first study (click first row)
WebUI.waitForElementPresent(findTestObject('CRDC/AILocators/GenericTable/TableRow'), 15, FailureHandling.CONTINUE_ON_FAILURE)
List<?> rows = WebUI.findWebElements(findTestObject('CRDC/AILocators/GenericTable/TableRow'), 10)
if (rows == null || rows.isEmpty()) {
	KeywordUtil.markFailed("Data Explorer: no studies found in list. Cannot open study view.")
} else {
	rows.get(0).click()
	WebUI.delay(2)
	// Verify Data Explorer metadata view for program and study node types; expected TSVs under CRDCDH-3026
	CustomKeywords.'utilities.DataExplorer.verifyDataExplorerMetadataView'('CRDCDH-3026', 'program', 'study', 'participant','sample','file')
}

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
