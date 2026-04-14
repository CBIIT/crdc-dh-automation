import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import internal.GlobalVariable

/*
 * CRDCDH-1551 / TC02_CreateUpload_VerifySubmissionNodes
 *
 * API flow (same metadata pipeline as CRDCDH-1526 TC01 steps 1–6):
 *  1. createSubmission — new submission; save _id to GlobalVariable.LastApiSubmissionId.
 *  2. createBatch — presigned URLs per metadata file.
 *  3. PUT local TSVs to S3; build upload results for updateBatch.
 *  4. updateBatch — finalize uploads.
 *  5. validateSubmission — metadata validation run.
 *  6. listBatches — verify batch and files.
 *  7. getSubmissionNodes — last step: for each node type (program, study, participant), verify totals,
 *     IDPropName, properties, and node rows (nodeID, status, props) match InputFiles/CRDC/API/*-stage.tsv.
 *
 * Prerequisites: ApiUrl, SubmitterApiToken; InputFiles/CRDC/API/program-stage.tsv, study-stage.tsv, participant-stage.tsv
 * GraphQL: GraphQLOperations.xlsx includes getSubmissionNodes.
 */


// Step 1 — createSubmission
String timestamp = new Date().format("yyyyMMdd_HHmmss")
String submissionName = "auto-api_" + timestamp

KeywordUtil.logInfo("createSubmission name: ${submissionName}")

Map subResp = CustomKeywords.'utilities.GraphQlApiClient.createSubmission'([
	studyID    : '2ef65a9b-7f9f-42e9-8e65-d45c7b3ca74e',
	dataCommons: 'CDS',
	name       : submissionName,
	intention  : 'New/Update',
	dataType   : 'Metadata Only'
])

Map submission = CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackCreateSubmission'(subResp)
String lastApiSubmissionId = submission?.get('_id')?.toString()

GlobalVariable.LastApiSubmissionId = lastApiSubmissionId
KeywordUtil.logInfo("GlobalVariable.LastApiSubmissionId: ${GlobalVariable.LastApiSubmissionId}")


// Step 2–4 — batch, upload, updateBatch
String apiInputDir = CustomKeywords.'utilities.GraphQlApiClient.defaultApiInputDir'()
List<String> fileBasenames = ['program-stage.tsv', 'study-stage.tsv', 'participant-stage.tsv']

CustomKeywords.'utilities.GraphQlApiClient.verifyLocalFilesExist'(apiInputDir, fileBasenames)

Map batchResp = CustomKeywords.'utilities.GraphQlApiClient.createBatch'([
	submissionID: lastApiSubmissionId,
	type         : 'metadata',
	files        : fileBasenames
])

Map newBatch = CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackCreateBatch'(batchResp)
String batchId = newBatch?.get('_id')?.toString()

List fileRows = CustomKeywords.'utilities.GraphQlApiClient.requireNonEmptyNewBatchFiles'(newBatch)
List<Map> uploadResults = CustomKeywords.'utilities.GraphQlApiClient.putMetadataTsvsAndBuildUploadResults'(fileRows, apiInputDir)

Map updResp = CustomKeywords.'utilities.GraphQlApiClient.updateBatch'([
	batchID: batchId,
	files  : uploadResults
])

CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackUpdateBatch'(updResp, batchId)

WebUI.delay(5)

//// Step 5 — validateSubmission
//Map valResp = CustomKeywords.'utilities.GraphQlApiClient.validateSubmission'([
//	_id   : lastApiSubmissionId,
//	types : ['metadata'],
//	scope : 'All'
//])
//
//CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackValidateSubmission'(valResp)
//
//
//// Step 6 — listBatches
//Map listResp = CustomKeywords.'utilities.GraphQlApiClient.listBatches'([
//	submissionID: lastApiSubmissionId,
//	first       : 50
//])
//
//CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackListBatches'(listResp, lastApiSubmissionId, batchId,
//	fileBasenames, 'metadata')


// Step 7 — getSubmissionNodes (expected IDs from InputFiles/CRDC/API/*-stage.tsv)
List<String> expectedProgramIds = ['AUTOPROGRAMAPI']
List<String> expectedStudyIds = ['coffee2api']
List<String> expectedParticipantIds = [
	'CDS-CASE-1075999api',
	'CDS-CASE-10784999api',
	'CDS-CASE-10782999api',
	'CDS-CASE-1078999999api',
	'CDS-CASE-10783439999api',
	'CDS-CASE-1078324999api',
	'CDS-CASE-1078567599996api',
	'CDS-CASE-107678969997978api',
	'CDS-CASE-107853439999api'
]

Map progResp = CustomKeywords.'utilities.GraphQlApiClient.getSubmissionNodes'([
	_id      : lastApiSubmissionId,
	nodeType : 'program',
	status   : 'All',
	first    : -1,
	offset   : 0
])

CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackGetSubmissionNodes'(progResp, lastApiSubmissionId, 'program', 1,
	expectedProgramIds)

Map studyResp = CustomKeywords.'utilities.GraphQlApiClient.getSubmissionNodes'([
	_id      : lastApiSubmissionId,
	nodeType : 'study',
	status   : 'All',
	first    : -1,
	offset   : 0
])

CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackGetSubmissionNodes'(studyResp, lastApiSubmissionId, 'study', 1,
	expectedStudyIds)

Map partResp = CustomKeywords.'utilities.GraphQlApiClient.getSubmissionNodes'([
	_id      : lastApiSubmissionId,
	nodeType : 'participant',
	status   : 'All',
	first    : -1,
	offset   : 0
])

CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackGetSubmissionNodes'(partResp, lastApiSubmissionId, 'participant',
	9, expectedParticipantIds)

KeywordUtil.logInfo("getSubmissionNodes verification complete for submission ${lastApiSubmissionId}")

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
