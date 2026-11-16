import com.kms.katalon.core.util.KeywordUtil

import internal.GlobalVariable

/*
 * CRDCDH-1551 / TC02_GetSubmissionNodes
 *
 * Flow:
 *  1. Upload pipeline: createSubmission → createBatch → PUT TSVs → updateBatch
 *  2. waitForMetadataBatchReady — do not validate until batch/files are Uploaded (replaces fixed sleep)
 *  3. validateSubmission + verify (rejects "No metadata for validation")
 *  4. getSubmissionNodes × 3 — baseline program / study / participant counts vs InputFiles/CRDC/API/*-stage.tsv
 *  5. deleteDataRecords (one participant) — success = queued, not immediate
 *  6. waitForGetSubmissionNodesAfterDelete — poll until delete visible, then verifyAndUnpackGetSubmissionNodes
 *
 * GraphQLOperations.xlsx: getSubmissionNodes, deleteDataRecords.
 *
 * Prerequisites (execution profile): ApiUrl, SubmitterApiToken, ApiStudyId (CDS fixture study UUID).
 */


// Step 1 — createSubmission (keyword fails if data.createSubmission is missing)
String timestamp = new Date().format("yyyyMMdd_HHmmss")
String submissionName = "auto-api_" + timestamp
String apiStudyId = GlobalVariable.ApiStudyId?.toString()?.trim()
if (!apiStudyId) {
	KeywordUtil.markFailed('ApiStudyId is not set in the execution profile.')
}

KeywordUtil.logInfo("createSubmission name: ${submissionName}")

Map subResp = CustomKeywords.'utilities.GraphQlApiClient.createSubmission'([
	studyID    : apiStudyId,
	dataCommons: 'CDS',
	name       : submissionName,
	intention  : 'New/Update',
	dataType   : 'Metadata Only'
])

Map submission = (subResp?.data as Map)?.get('createSubmission') as Map
String lastApiSubmissionId = submission?.get('_id')?.toString()?.trim()

GlobalVariable.LastApiSubmissionId = lastApiSubmissionId
KeywordUtil.logInfo("GlobalVariable.LastApiSubmissionId: ${GlobalVariable.LastApiSubmissionId}")


// Step 2–4 — batch, upload, updateBatch (keywords fail on bad responses)
String apiInputDir = CustomKeywords.'utilities.GraphQlApiClient.defaultApiInputDir'()
List<String> fileBasenames = ['program-stage.tsv', 'study-stage.tsv', 'participant-stage.tsv']

Map batchResp = CustomKeywords.'utilities.GraphQlApiClient.createBatch'([
	submissionID: lastApiSubmissionId,
	type         : 'metadata',
	files        : fileBasenames
])

Map newBatch = (batchResp?.data as Map)?.get('createBatch') as Map
String batchId = newBatch?.get('_id')?.toString()?.trim()

List fileRows = newBatch?.get('files') as List
List<Map> uploadResults = CustomKeywords.'utilities.GraphQlApiClient.putMetadataTsvsAndBuildUploadResults'(fileRows, apiInputDir)

CustomKeywords.'utilities.GraphQlApiClient.updateBatch'([
	batchID: batchId,
	files  : uploadResults
])

// Hub ingests TSVs asynchronously after updateBatch; validate too early → empty nodes / "No metadata for validation".
CustomKeywords.'utilities.GraphQlApiClient.waitForMetadataBatchReady'([
	submissionID   : lastApiSubmissionId,
	batchID        : batchId,
	files          : fileBasenames,
	timeoutSeconds : 180,
	stableSeconds  : 10
])

Map valResp = CustomKeywords.'utilities.GraphQlApiClient.validateSubmission'([
	_id   : lastApiSubmissionId,
	types : ['metadata'],
	scope : 'All'
])

CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackValidateSubmission'(valResp)


// --- getSubmissionNodes (baseline) ---
// GraphQL query: Data View rows per nodeType for this submission. _id = submission ID; status 'All' = no filter;
// first: -1 asks the Hub for the full list (see GraphQlApiClient.getSubmissionNodes).
// Expected ID lists match InputFiles/CRDC/API/*-stage.tsv (program_acronym, study_id, study_participant_id).
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

// One call per node type; verifyAndUnpackGetSubmissionNodes checks total, IDPropName, properties, and each node's
// submissionID, nodeType, nodeID, status, and parseable props JSON against the expected ID set.
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


// --- deleteDataRecords + getSubmissionNodes (post-delete) ---
// Mutation queues an async delete; success=true means the request was accepted
// We delete one participant by nodeID (study_participant_id); program/study are left alone to avoid cascade issues.
String participantToDelete = expectedParticipantIds[0]
// Remaining participant nodeIDs we expect after delete (same list as baseline minus the deleted id).
List<String> expectedParticipantIdsAfterDelete = expectedParticipantIds.size() > 1 ? expectedParticipantIds[1..-1] : []

Map delResp = CustomKeywords.'utilities.GraphQlApiClient.deleteDataRecords'([
	_id      : lastApiSubmissionId,
	nodeType : 'participant',
	nodeIDs  : [participantToDelete]
])

// Assert DataValidation.success and log message to ValidationReport.
CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackDeleteDataRecords'(delResp)

int expectedParticipantTotalAfterDelete = expectedParticipantIdsAfterDelete.size()

KeywordUtil.logInfo("deleteDataRecords queued for participant nodeID=${participantToDelete}; poll getSubmissionNodes until absent (async delete)")

// Polls getSubmissionNodes internally; final payload is verified the same way as baseline participant read above.
Map partRespAfter = CustomKeywords.'utilities.GraphQlApiClient.waitForGetSubmissionNodesAfterDelete'([
	submissionID   : lastApiSubmissionId,
	nodeType       : 'participant',
	expectedTotal  : expectedParticipantTotalAfterDelete,
	absentNodeIds  : [participantToDelete],
	timeoutSeconds : 120
])

CustomKeywords.'utilities.GraphQlApiClient.verifyAndUnpackGetSubmissionNodes'(partRespAfter, lastApiSubmissionId, 'participant',
	expectedParticipantTotalAfterDelete, expectedParticipantIdsAfterDelete)

KeywordUtil.logInfo("getSubmissionNodes + deleteDataRecords verification complete for submission ${lastApiSubmissionId}")

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
