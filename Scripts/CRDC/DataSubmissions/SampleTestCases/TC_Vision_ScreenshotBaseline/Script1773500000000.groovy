import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.configuration.RunConfiguration
import internal.GlobalVariable as GlobalVariable

/**
 * Example: Vision baseline screenshot verification on the data submission page.
 * - Logs in as Submitter, navigates to a data submission (creates one if needed).
 * - Sets viewport 1920x1080, waits for Summary/charts to load.
 * - Takes a screenshot and compares it to InputFiles/CRDC/Screenshots/DataSubmission_Summary_baseline.png via Claude on Bedrock.
 * - Pass/fail is recorded in ValidationReport; test fails if the LLM reports mismatch.
 *
 * Prerequisites:
 * - Baseline image exists: InputFiles/CRDC/Screenshots/DataSubmission_Summary_baseline.png (capture from a known-good run).
 * - AWS credentials for Bedrock; AppKey=CRDC; Python with boto3.
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
String baselinePath = "${projectDir}/InputFiles/CRDC/Screenshots/DataSubmission_Summary_baseline.png"

WebUI.takeScreenshot(currentPath)
// Pass true as 4th argument to log Bedrock call info and full LLM response to the Katalon log
CustomKeywords.'utilities.WebUIUtils.verifyScreenshotMatchesBaseline'(currentPath, baselinePath, "Data Submission Charts", true)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
