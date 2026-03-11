import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI


/**
 * TC202_VerifyDataExplorerStudyListVsMongo: Verifies the Data Explorer list page table matches
 * the MongoDB release aggregation exactly (studyName and studyAbbreviation; no more, no less).
 * - Logs in as Admin, navigates to Data Explorer tab (list view),
 * - Runs verifyDataExplorerStudyListMatchesReleaseAggregate() which queries release collection
 *   and compares to the UI table.
 * - Generates ValidationReport HTML at the end.
 * Requires G_MongoUri and G_MongoDatabase set in the selected profile.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Admin')
WebUI.delay(2)

CustomKeywords.'utilities.ValidationReport.clearValidationReportCollector'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataExplorer-Tab')
WebUI.delay(2)

//Verify studies in Data Explorer page match MongoDB via aggregate query
CustomKeywords.'utilities.DataExplorer.verifyDataExplorerStudyListMatchesMongo'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
