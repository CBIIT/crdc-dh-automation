import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/**
 * Applitools vision demo: same flow as TC17 but uses Katalon Applitools plugin instead of Bedrock.
 * Compares statistics charts container via checkTestObject (element-level, less noise than checkWindow).
 *
 * Prerequisites:
 * - Applitools Integration plugin installed; API key set in Project > Settings > Plugins > Applitools Integration
 * - Application Name e.g. CRDC Data Hub Automation; viewport 1920x1080 set below
 *
 * Calibration (first-time):
 * 1. Run once to create Applitools baseline for test name below.
 * 2. Re-run 2+ times; review diffs in Applitools dashboard (accept/reject as needed).
 * 3. Compare behavior vs TC17 Bedrock POC on same charts state.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC', 'autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC05')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
WebUI.refresh()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Statistics/Charts-Container')

CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/AILocators/Global/Notistack-Snackbar', 60)
WebUI.delay(1)

CustomKeywords.'com.kms.katalon.keyword.applitools.BasicKeywords.checkTestObject'(
	findTestObject('CRDC/AILocators/DataSubmissions/Detail/Statistics/Charts-Container'),
	'CRDC Data Submission Statistics Charts - TC05'
)

WebUI.closeBrowser()
