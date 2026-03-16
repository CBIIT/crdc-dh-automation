import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC15_CreateNewProgram
 * Logs in as Admin, navigates to Manage Programs, creates a new program via addNewProgram, and verifies it on the list.
 *
 * Flow:
 *   1. Login as Admin
 *   2. navigateToManagePrograms() – open Manage Programs list
 *   3. addNewProgram(programData) – fill required fields, Save, verify program appears in list
 *   4. Generate validation report
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Admin')
WebUI.delay(2)

CustomKeywords.'utilities.ManagePrograms.navigateToManagePrograms'()
WebUI.delay(2)

//CustomKeywords.'utilities.ManagePrograms.addNewProgram'([
//	name: 'AutoProgram-TC15-10',
//	abbreviation: 'APTC15-10',
//	description: 'auto program-10',
//	dataConcierge: 'pbac.dcp-automation Please Do Not Edit',
//	status: 'Active'
//	
//])

CustomKeywords.'utilities.ManagePrograms.addNewProgram'()

WebUI.delay(2)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
