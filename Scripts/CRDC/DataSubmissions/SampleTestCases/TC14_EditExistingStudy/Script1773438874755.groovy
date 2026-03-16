import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC14_EditExistingStudy
 * Logs in as Admin, finds an existing study by name on Manage Studies, and edits it with the given updates.
 * Run TC13_CreateNewStudy first to create "AutoStudy-TC13", or set studyNameToEdit to an existing study.
 *
 * Flow:
 *   1. Login as Admin (Manage Studies typically requires admin)
 *   2. navigateToManageStudies() – open Manage Studies list
 *   3. editExistingStudy(studyNameToEdit, updates) – find study, open Edit, apply updates, Save
 *   4. Generate validation report
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Admin')
WebUI.delay(2)

CustomKeywords.'utilities.ManageStudies.navigateToManageStudies'()
WebUI.delay(2)
// Edit an existing study: provide study name (exact or partial match in table) and fields to update
String studyNameToEdit = 'AutoStudy-TC13-6'
Map updates = [
	acronym: 'Edited-Existing-3',
	accessTypes: 'Open Access',
	program: 'Not Applicable'
]
CustomKeywords.'utilities.ManageStudies.editExistingStudy'(studyNameToEdit, updates)
WebUI.delay(2)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
