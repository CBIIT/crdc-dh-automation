import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.configuration.RunConfiguration

/**
 * Vision AI POC: compare statistics charts screenshot to baseline via Claude on Bedrock (Sonnet 4.6).
 * Report-only phase — logs structured JSON diff to ValidationReport and Reports/vision_diff.json.
 * Does not fail on diff content; only fails on infra errors (Bedrock, script, timeout).
 *
 * Prerequisites:
 * - Baseline: InputFiles/CRDC/Screenshots/DataSubmission_Summary_baseline-TC05.png
 * - Python with boto3 (see requirements.txt)
 * - AWS creds: AwsAccessKeyId, AwsSecretAccessKey, optional AwsRegion in Katalon profile
 * - Optional: BedrockVisionModelId profile global to override default us.anthropic.claude-sonnet-4-6
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC','autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC05')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
WebUI.refresh()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Statistics/Charts-Container')

CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/AILocators/Global/Notistack-Snackbar', 60)
WebUI.delay(1)

String projectDir = RunConfiguration.getProjectDir()
String currentPath = "${projectDir}/Reports/vision_current.png"
//String baselinePath = "${projectDir}/InputFiles/CRDC/Screenshots/DataSubmission_Summary_baseline-TC05.png"
String baselinePath = "${projectDir}/InputFiles/CRDC/Screenshots/current_QA2.png"

WebUI.takeElementScreenshot(currentPath, findTestObject('CRDC/AILocators/DataSubmissions/Detail/Statistics/Charts-Container'))
CustomKeywords.'utilities.WebUIUtils.compareScreenshotsWithVisionAi'(
	currentPath,
	baselinePath,
	"Data Submission Charts",
	"Statistics charts container after TC05 metadata validation; ignore minor rendering differences",
	true
)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
