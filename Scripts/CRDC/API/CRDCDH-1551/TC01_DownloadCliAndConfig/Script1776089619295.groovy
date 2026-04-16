import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import internal.GlobalVariable

/*
 * CRDCDH-1551 — TC01 Download CLI and config
 *
 *  1. createSubmission — new submission; save _id to GlobalVariable.LastApiSubmissionId.
 *  2. UI — clear project Downloads, open Uploader CLI dialog, download four platform zips; close dialog.
 *  3. retrieveCLIConfig — GraphQL query; verify YAML contains api-url, submission, placeholder paths, and profile token.
 *  4. generateValidationReport
 *
 * Prerequisites (execution profile):
 * - ApiUrl — GraphQL endpoint (e.g. .../api/graphql), same env as UI
 * - SubmitterApiToken — JWT from Data Hub API token (Authorization: Bearer ...)
 * - G_Urlname — Hub base URL for UI (e.g. https://…/bento)
 */


// Step 1 - createSubmission
String timestamp = new Date().format("yyyyMMdd_HHmmss")
String submissionName = "auto-api_" + timestamp

KeywordUtil.logInfo("createSubmission name: ${submissionName} (length ${submissionName.length()})")

Map subResp = CustomKeywords.'utilities.GraphQlApiClient.createSubmission'([
	studyID    : '2ef65a9b-7f9f-42e9-8e65-d45c7b3ca74e',
	dataCommons: 'CDS',
	name       : submissionName,
	intention  : 'New/Update',
	dataType   : 'Metadata and Data Files'
])

Map submission = CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackCreateSubmission'(subResp)
String lastApiSubmissionId = submission?.get('_id')?.toString()
String submissionStatus = submission?.get('status')?.toString()

GlobalVariable.LastApiSubmissionId = lastApiSubmissionId
KeywordUtil.logInfo("GlobalVariable.LastApiSubmissionId: ${GlobalVariable.LastApiSubmissionId}")
KeywordUtil.logInfo("New submission status: ${submissionStatus}")


// Step 2 — UI: login, navigate to created DS, download CLI tool
WebUI.closeBrowser()
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(lastApiSubmissionId)
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Loading-Icon', 300)

final String CLI_ZIP_SRC = 'crdc-datahub-cli-uploader-src.zip'
final String CLI_ZIP_WIN = 'crdc-datahub-cli-uploader-windows.zip'
final String CLI_ZIP_MAC_X64 = 'crdc-datahub-cli-uploader-mac-x64.zip'
final String CLI_ZIP_MAC_ARM = 'crdc-datahub-cli-uploader-mac-arm.zip'
final int CLI_DOWNLOAD_TIMEOUT_SEC = 120

CustomKeywords.'utilities.Utils.clearProjectDownloadsFolder'()

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/Detail/DataUpload/CLIDownload-Link')
WebUI.delay(1)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/UploaderToolDialog/DownloadLink-SourceAny')
CustomKeywords.'utilities.Utils.waitForFileInProjectDownloads'(CLI_ZIP_SRC, CLI_DOWNLOAD_TIMEOUT_SEC)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/UploaderToolDialog/DownloadLink-WindowsX64')
CustomKeywords.'utilities.Utils.waitForFileInProjectDownloads'(CLI_ZIP_WIN, CLI_DOWNLOAD_TIMEOUT_SEC)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/UploaderToolDialog/DownloadLink-MacOSX64')
CustomKeywords.'utilities.Utils.waitForFileInProjectDownloads'(CLI_ZIP_MAC_X64, CLI_DOWNLOAD_TIMEOUT_SEC)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/UploaderToolDialog/DownloadLink-MacOSARM')
CustomKeywords.'utilities.Utils.waitForFileInProjectDownloads'(CLI_ZIP_MAC_ARM, CLI_DOWNLOAD_TIMEOUT_SEC)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/UploaderToolDialog/Close-Btn')


// Step 3 — retrieveCLIConfig (paths are placeholders; Hub only substitutes strings into YAML)
final String cliConfigDataFolder = '/tmp/crdc_automation_cli_data/'
final String cliConfigManifest = '/tmp/crdc_automation_cli_manifest.tsv'
String apiUrlForCli = GlobalVariable.ApiUrl.toString().trim()

Map cliConfigResp = CustomKeywords.'utilities.GraphQlApiClient.retrieveCLIConfig'([
	submissionID: lastApiSubmissionId,
	apiURL       : apiUrlForCli,
	dataFolder   : cliConfigDataFolder,
	manifest     : cliConfigManifest
])
CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackRetrieveCLIConfig'(cliConfigResp, lastApiSubmissionId, apiUrlForCli,
	cliConfigDataFolder, cliConfigManifest)


WebUI.closeBrowser()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
