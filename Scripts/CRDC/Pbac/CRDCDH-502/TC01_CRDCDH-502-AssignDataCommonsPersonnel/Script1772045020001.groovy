import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC01_CRDCDH-502-AssignDataCommonsPersonnel
 * Admin Edit User: full Data Commons checks on a DCP user, then on Submitter cycle Role
 * through User / Submitter / Federal Lead / Admin (field hidden) and Data Commons Personnel
 * (field appears). Does not Save role changes on pbac.*-automation users.
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Admin')

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/Login/UserProfile-Dd')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/ManageUsers-Btn')

CustomKeywords.'utilities.Pbac.verifyDCPFields'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
