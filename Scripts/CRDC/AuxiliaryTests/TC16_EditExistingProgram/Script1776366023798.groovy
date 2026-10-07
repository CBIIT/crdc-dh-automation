import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.Utils

/*
 * TC16_EditExistingProgram
 * Logs in as Admin, navigates to Manage Programs, finds an existing program by name, and edits it with the given updates.
 * Run TC15_CreateNewProgram first to create "AutoProgram-TC15", or set programNameToEdit to an existing program.
 *
 * Flow:
 *   1. Login as Admin
 *   2. navigateToManagePrograms() – open Manage Programs list
 *   3. editExistingProgram(programNameToEdit, updates) – find program, open Edit, apply updates, Save
 *   4. Generate validation report
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Admin')
WebUI.delay(2)

CustomKeywords.'utilities.ManagePrograms.navigateToManagePrograms'()
WebUI.delay(2)

String programNameToEdit = 'AutoProgram-TC15-10'
String descriptionTs = Utils.getCurrentDate('yyyyMMdd_HHmmss')
Map updates = [
	abbreviation: 'TC15-edited-10',
	description: "updated description ${descriptionTs}",
	status: 'Active'
]
CustomKeywords.'utilities.ManagePrograms.editExistingProgram'(programNameToEdit, updates)
WebUI.delay(2)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()