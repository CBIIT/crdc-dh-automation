import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC01_CRDCDH-502-AssignDataCommonsPersonnel
 *
 * Flow:
 * 1. Login as Admin; open Manage Users.
 * 2. Edit pbac.Dcp-automation. Verify Data Commons is present, visible, enabled,
 *    has a required asterisk, and the list has at least 2 options. Escape to close
 *    the list. Cancel (never Save).
 * 3. Edit pbac.Submitter-automation. Cycle Role to User, Submitter, Federal Lead,
 *    and Admin — Data Commons stays hidden. Switch Role to Data Commons Personnel —
 *    the field appears. Cancel (never Save).
 *
 * Does not cover DCP DS-activity email (CRDCDH-1810).
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
