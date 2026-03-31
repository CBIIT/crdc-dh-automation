import com.kms.katalon.core.util.KeywordUtil

import internal.GlobalVariable

/*
 * CRDCDH-1526 / TC01_CreateSubmissionAndUpload
 *
 * End-to-end API flow (no browser). Flow order:
 *  1. createSubmission — new submission; save _id to GlobalVariable for reuse by other tests.
 *  2. createBatch — Hub returns batch _id + presigned S3 URL per metadata file name.
 *  3. PUT each local TSV to its signedURL (S3), build upload result maps for updateBatch.
 *  4. updateBatch — tell the Hub uploads finished; response reflects per-file and batch status.
 *  5. listBatches — query batches for that submission; verify same batch/files match expectations.
 *
 * GlobalVariable.LastApiSubmissionId stores the submission _id for other test cases.
 * Batch _id and signed URLs exist only in this script’s variables (not global).
 *
 * Prerequisites (execution profile):
 * - ApiUrl — GraphQL endpoint (e.g. .../api/graphql)
 * - SubmitterApiToken — JWT from Data Hub “API token” (sent as Authorization: Bearer ...)
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
	dataType   : 'Metadata and Data Files'
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


// Step 5 — listBatches (read back from the Hub)
Map listResp = CustomKeywords.'utilities.GraphQlApiClient.listBatches'([
	submissionID: lastApiSubmissionId,
	first       : 50
])

Map listPayload = CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackListBatches'(listResp, lastApiSubmissionId, batchId,
	fileBasenames, 'metadata')
KeywordUtil.logInfo("listBatches total: ${listPayload?.get('total')}, verified batch _id: ${batchId}")


CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
