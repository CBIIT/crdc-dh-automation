import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC01_CRDCDH-2175-RequestAccessStudiesInstitution
 *
 * Flow:
 * 1. Login as Submitter; open User Profile.
 * 2. Hover Request Access — tooltip is "Request role change, study access, or institution update."
 * 3. Open the Request Access dialog.
 * 4. Studies present with required asterisk.
 * 5. Institution present with required asterisk.
 * 6. Organization field not present.
 * 7. Empty Submit — Institution and Studies helpers are "This field is required";
 *    dialog stays open.
 * 8. Studies list: at least 2 options; labels are "Abbr - Full Name" or name only
 *    (sort is not asserted).
 * 9. Institution list: at least 2 options, sorted ascending.
 * 10. Type a unique name that is not in the list; the typed value is kept.
 * 11. Cancel. Never Submit.
 */

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/Login/UserProfile-Dd')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/Login/UserProfile-Link')

CustomKeywords.'utilities.Pbac.verifyRequestAccessFields'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
//WebUI.closeBrowser()
