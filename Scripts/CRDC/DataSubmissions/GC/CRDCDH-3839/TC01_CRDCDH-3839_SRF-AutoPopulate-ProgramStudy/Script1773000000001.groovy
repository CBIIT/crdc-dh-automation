import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * CRDCDH-3839 TC01 — GC happy path: validator auto-populates program/study metadata from approved SRF (CRDCDH-3652).
 *
 * Preconditions (CRDC_QA):
 *   - Approved SRF study AUTOQA - AutomationQA_CRDCDH-3839; program AutoTest-Program (ATP) includes program_full_description
 *     on the SRF at approval time (edits after approval do not backfill auto-pop for existing studies).
 *   - Open-access study: registered phs_accession on SRF is N/A (upload must use literal N/A, not blank).
 *
 * Test data folder: InputFiles/CRDC/MetadataData/CRDCDH-3839_TC01/
 *   Upload (top-level TSVs only — uploaded via uploadFolder):
 *     GC_program_v11.0.3.tsv — omits SRF auto-pop columns: program_name, program_acronym, program_full_description.
 *     GC_study_v11.0.3.tsv   — omits SRF auto-pop columns: study_name, study_acronym, study_description,
 *                              program.program_acronym; includes phs_accession N/A and other required GC fields.
 *   Golden (expected/ — not uploaded; used after validate):
 *     expected/GC_program_v11.0.3_expected.tsv — full program row as Data View/export after validate (incl. auto-pop).
 *     expected/GC_study_v11.0.3_expected.tsv   — full study row as Data View/export after validate (incl. auto-pop).
 *
 * What we verify:
 *   - After validate, no Validation Results errors (auto-pop satisfies required SRF-mapped properties).
 *   - Data View UI matches expected/*.tsv per node (verifyDataViewMatchesExpectedTsvForTC).
 *   - Data View download TSV matches same expected/*.tsv (downloadAndCompareExpectedForTC).
 */

WebUI.closeBrowser()

// --- Login as Submitter ---
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

// --- Create metadata-only GC submission for fixture study (SRF not automated in this TC) ---
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'GC', 'AUTOQA - AutomationQA_CRDCDH-3839')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

// --- Upload minimal program/study TSVs (columns omitted intentionally; validator fills from SRF on validate) ---
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-3839_TC01')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

// --- Validation Results: aggregated view, expect zero issues ---
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults-Tab')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults_AggregatedExpanded-Btn')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.DataSubmissions.verifyNoValidationErrors'()

// --- Data View: UI vs expected/GC_*_expected.tsv; then export download vs same golden files ---
CustomKeywords.'utilities.DataSubmissions.verifyDataViewMatchesExpectedTsvForTC'('CRDCDH-3839_TC01')
CustomKeywords.'utilities.DataSubmissions.downloadAndCompareExpectedForTC'('CRDCDH-3839_TC01')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
