import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.configuration.RunConfiguration
import internal.GlobalVariable as GlobalVariable

/**
 * Example: Screenshot comparison against baseline using SSIM (structural similarity).
 * - Logs in as Submitter, navigates to a data submission, takes a screenshot.
 * - Compares to InputFiles/CRDC/Screenshots/DataSubmission_Summary_baseline.png locally (no LLM for pass/fail).
 * - On failure, saves a diff image to Reports/vision_diff.png. Optional: set explainWithLlm true for a one-sentence LLM explanation.
 *
 * Prerequisites:
 * - Baseline exists: InputFiles/CRDC/Screenshots/DataSubmission_Summary_baseline.png
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
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('TC04')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/Statistics/Charts-Container')
WebUI.delay(3)

String projectDir = RunConfiguration.getProjectDir()
String currentPath = "${projectDir}/Reports/vision_current.png"
String baselinePath = "${projectDir}/InputFiles/CRDC/Screenshots/DataSubmission_Summary_baseline-original.png"
String diffPath = "${projectDir}/Reports/vision_diff.png"

WebUI.takeScreenshot(currentPath)
// verbose=true; saveDiffPath so failures write Reports/vision_diff.png; explainWithLlm=true for LLM reason on fail
CustomKeywords.'utilities.WebUIUtils.verifyScreenshotMatchesBaseline'(currentPath, baselinePath, "Data Submission Charts", true, null, true, diffPath)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
