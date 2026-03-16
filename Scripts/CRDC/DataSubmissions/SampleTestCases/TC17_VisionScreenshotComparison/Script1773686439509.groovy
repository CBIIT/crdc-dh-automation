import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.util.KeywordUtil
import internal.GlobalVariable as GlobalVariable

/** RUN IN HEADLESS MODE
 * Example: Screenshot comparison against baseline using SSIM (structural similarity).
 * - Logs in as Submitter, navigates to a data submission, takes a screenshot.
 * - On failure, saves a diff image to Reports/vision_diff.png. Optional: set explainWithLlm true for a one-sentence LLM explanation.
 *
 * Prerequisites:
 * - Baselines: DataSubmission_Summary_baseline-TC05.png (Jenkins), DataSubmission_Summary_baseline-TC05-local.png (local).
 * - Python with Pillow, numpy, scikit-image (see requirements.txt). AWS only if explainWithLlm is true.
 */
WebUI.closeBrowser()

CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

//Create DS
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC','autoQA - AutomationQA_Study')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

//Upload data via folder
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC05')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
WebUI.refresh()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Statistics/Charts-Container')
// Wait for "Validation process is starting" snackbar message to disappear before screenshot
//CustomKeywords.'utilities.Utils.waitForElementNotVisible'('CRDC/AILocators/Global/Notistack-Snackbar', 60)
WebUI.delay(3)

String projectDir = RunConfiguration.getProjectDir()
String currentPath = "${projectDir}/Reports/vision_current.png"
//String baselineSuffix = utilities.Utils.isJenkinsRun() ? "TC05" : "TC05-local"
//String baselinePath = "${projectDir}/InputFiles/CRDC/Screenshots/DataSubmission_Summary_baseline-${baselineSuffix}.png"
String baselinePath = "${projectDir}/InputFiles/CRDC/Screenshots/DataSubmission_Summary_baseline-TC05.png"
//KeywordUtil.logInfo("Using baseline: ${baselineSuffix} (Jenkins=${utilities.Utils.isJenkinsRun()})")

//Take screenshot of element and verify the chart matches baseline image -- can also take screenshot of entire page via WebUI.takeScreenshot(currentPath)
WebUI.takeElementScreenshot(currentPath, findTestObject('CRDC/AILocators/DataSubmissions/Detail/Statistics/Charts-Container'))
CustomKeywords.'utilities.WebUIUtils.verifyScreenshotMatchesBaseline'(currentPath, baselinePath, "Data Submission Charts", null, true)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
