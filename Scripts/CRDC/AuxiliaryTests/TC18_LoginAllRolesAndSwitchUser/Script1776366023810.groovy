import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/**
 * TC18_LoginAllRolesAndSwitchUser
 * Exercises Login.loginToCrdcOtp and Login.switchUser for all six automation roles.
 * Profile must define Admin, Fedlead, Dcp, Submitter, User, and Master email/password/secret globals.
 */

final List<String> roles = ['Admin', 'Fedlead', 'Dcp', 'Submitter', 'User', 'Master']

WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'(roles[0])
KeywordUtil.logInfo("Login smoke: signed in as ${roles[0]}")

for (int i = 1; i < roles.size(); i++) {
	String role = roles[i]
	CustomKeywords.'utilities.Login.switchUser'(role)
	KeywordUtil.logInfo("Login smoke: signed in as ${role}")
}

WebUI.closeBrowser()
KeywordUtil.logInfo("Login smoke: completed all ${roles.size()} roles")
