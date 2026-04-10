import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import internal.GlobalVariable

/*
 * CRDCDH-1526 / TC01_CreateSubmissionAndUpload
 *
 * API + UI flow:
 *  1. createSubmission — new submission; save _id to GlobalVariable for reuse by other tests.
 *  2. createBatch — Hub returns batch _id + presigned S3 URL per metadata file name.
 *  3. PUT each local TSV to its signedURL (S3), build upload result maps for updateBatch.
 *  4. updateBatch — tell the Hub uploads finished; response reflects per-file and batch status.
 *  5. validateSubmission — trigger metadata validation (success = run initialized, not QC outcome).
 *  6. listBatches — query batches for that submission; verify same batch/files match expectations.
 *  7. UI — login, navigate to submission, Validation Results tab + expanded view, then {@code DataSubmissions.verifyNoValidationErrors()}.
 *  8. submissionQCResults — GraphQL query cross-check: no QC rows ({@code total} 0, empty {@code results}) after UI.
 *  9. submissionAction — Submit; verify {@code status} is Submitted (API check after UI + QC query).
 *
 * GlobalVariable.LastApiSubmissionId stores the submission _id for other test cases.
 * Batch _id and signed URLs exist only in this script’s variables (not global).
 *
 * Prerequisites (execution profile):
 * - ApiUrl — GraphQL endpoint (e.g. .../api/graphql)
 * - SubmitterApiToken — JWT from Data Hub “API token” (sent as Authorization: Bearer ...)
 * - G_Urlname — Hub base URL for UI (e.g. https://…/bento) for step 7
 * - On disk: InputFiles/CRDC/API/program-stage.tsv, study-stage.tsv, participant-stage.tsv
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
	dataType   : 'Metadata Only'
])

Map submission = CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackCreateSubmission'(subResp)
String lastApiSubmissionId = submission?.get('_id')?.toString()
String submissionStatus = submission?.get('status')?.toString()

GlobalVariable.LastApiSubmissionId = lastApiSubmissionId
KeywordUtil.logInfo("GlobalVariable.LastApiSubmissionId: ${GlobalVariable.LastApiSubmissionId}")
KeywordUtil.logInfo("New submission status: ${submissionStatus}")


// Step 2 — createBatch 
String apiInputDir = CustomKeywords.'utilities.GraphQlApiClient.defaultApiInputDir'()
List<String> fileBasenames = ['program-stage.tsv', 'study-stage.tsv', 'participant-stage.tsv']

CustomKeywords.'utilities.GraphQlApiClient.verifyLocalFilesExist'(apiInputDir, fileBasenames)

KeywordUtil.logInfo("createBatch submissionID=${lastApiSubmissionId} type=metadata files=${fileBasenames}")

Map batchResp = CustomKeywords.'utilities.GraphQlApiClient.createBatch'([
	submissionID: lastApiSubmissionId,
	type         : 'metadata',
	files        : fileBasenames
])

Map newBatch = CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackCreateBatch'(batchResp)
String batchId = newBatch?.get('_id')?.toString()
String batchStatusAfterCreate = newBatch?.get('status')?.toString()
KeywordUtil.logInfo("Batch _id (local): ${batchId}, status after createBatch: ${batchStatusAfterCreate}")

List fileRows = CustomKeywords.'utilities.GraphQlApiClient.requireNonEmptyNewBatchFiles'(newBatch)


// Step 3 — Upload to S3 using presigned URLs
List<Map> uploadResults = CustomKeywords.'utilities.GraphQlApiClient.putMetadataTsvsAndBuildUploadResults'(fileRows, apiInputDir)


// Step 4 — updateBatch
Map updResp = CustomKeywords.'utilities.GraphQlApiClient.updateBatch'([
	batchID: batchId,
	files  : uploadResults
])

Map updatedBatch = CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackUpdateBatch'(updResp, batchId)
String finalBatchStatus = updatedBatch?.get('status')?.toString()
KeywordUtil.logInfo("updateBatch final batch status: ${finalBatchStatus}")

WebUI.delay(5) //handle race condition before validation

// Step 5 — validateSubmission (initialize validation run; success is not QC pass/fail)
KeywordUtil.logInfo("validateSubmission submissionID=${lastApiSubmissionId} types=[metadata] scope=All")

Map valResp = CustomKeywords.'utilities.GraphQlApiClient.validateSubmission'([
	_id   : lastApiSubmissionId,
	types : ['metadata'],
	scope : 'All'
])

CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackValidateSubmission'(valResp)


// Step 6 — listBatches (read back from the Hub)
Map listResp = CustomKeywords.'utilities.GraphQlApiClient.listBatches'([
	submissionID: lastApiSubmissionId,
	first       : 50
])

Map listPayload = CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackListBatches'(listResp, lastApiSubmissionId, batchId,
	fileBasenames, 'metadata')
KeywordUtil.logInfo("listBatches total: ${listPayload?.get('total')}, verified batch _id: ${batchId}")


// Step 7 — UI: login, open submission, Validation Results expanded; keyword verifies no QC rows on current page
WebUI.closeBrowser()
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

CustomKeywords.'utilities.WebUIUtils.navigateToSubmissionUploadActivity'(lastApiSubmissionId)
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 300)

CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults-Tab')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults_AggregatedExpanded-Btn')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)

CustomKeywords.'utilities.DataSubmissions.verifyNoValidationErrors'()


// Step 8 — submissionQCResults: API cross-check (no QC issues)
KeywordUtil.logInfo("submissionQCResults submissionID=${lastApiSubmissionId} (post-UI)")

Map qcResp = CustomKeywords.'utilities.GraphQlApiClient.submissionQCResults'([
	id             : lastApiSubmissionId,
	first          : -1,
	offset         : 0,
	sortDirection  : 'DESC',
	orderBy        : 'displayID',
	severities     : 'All'
])

CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackSubmissionQCResults'(qcResp, 0) //expected 0 errors or warnings


// Step 9 — submissionAction (Submit); verify status Submitted
KeywordUtil.logInfo("submissionAction Submit submissionID=${lastApiSubmissionId}")

//Submit and verify
Map submitResp = CustomKeywords.'utilities.GraphQlApiClient.submissionAction'([
	submissionID: lastApiSubmissionId,
	action       : 'Submit'
])

CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackSubmissionAction'(submitResp, lastApiSubmissionId, 'Submitted')

CustomKeywords.'utilities.WebUIUtils.refreshPageAndWaitForLoad'()
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "Submitted")

//Withdraw and verify
Map withdrawResp = CustomKeywords.'utilities.GraphQlApiClient.submissionAction'([
	submissionID: lastApiSubmissionId,
	action       : 'Withdraw'
])

CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackSubmissionAction'(withdrawResp, lastApiSubmissionId, 'Withdrawn')

CustomKeywords.'utilities.WebUIUtils.refreshPageAndWaitForLoad'()
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.WebUIUtils.verifyElementText'(findTestObject('CRDC/DataSubmissions/Validation/DSDashboard-Status-Lbl'), "Withdrawn")

WebUI.closeBrowser()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
