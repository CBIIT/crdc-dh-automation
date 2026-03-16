import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC13_CreateNewStudy
 * Logs in as Admin, creates a new study via ManageStudies.addNewStudy, and verifies it on the Manage Studies list.
 *
 * Flow:
 *   1. Login as Admin (Manage Studies typically requires admin)
 *   2. navigateToManageStudies() – open Manage Studies list
 *   3. addNewStudy(studyData) – fill required fields, Save, verify study appears in list
 *   4. Generate validation report
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Admin')
WebUI.delay(2)

CustomKeywords.'utilities.ManageStudies.navigateToManageStudies'()
WebUI.delay(2)
//CustomKeywords.'utilities.ManageStudies.addNewStudy'([
//	name: 'AutoStudy-TC13-7',
//	acronym: 'AutoStudy-7',
//	accessTypes: 'Open',
//	dbGaPID: 'phs001333',
//	gpa: 'QA Auto Agent',
//	gpa: 'QA Auto Agent2',
//	ORCID: '0000-0001-2345-6789',
//	program: 'auto_program',
//	DataConcierge: 'Same as the Program Data Concierge'
//	
//])

CustomKeywords.'utilities.ManageStudies.addNewStudy'()

WebUI.delay(2)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
