import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * CRDCDH-3839 TC03 — CTDC happy path: validator auto-populates program/study metadata from approved SRF (CRDCDH-3652).
 *
 * Preconditions (CRDC_QA) — CTDC-specific approved SRF study (not the GC/ICDC fixture):
 *   Study picker label: AUTOQA - AutomationQA_CRDCDH-3839-CTDC
 *   SRF at approval must include:
 *     - Program short name: NCTN (CTDC permissible value)
 *     - Program name: NCI National Clinical Trials Network
 *     - program_full_description: test program description (on SRF at approval; edits after approval do not backfill)
 *     - Registered dbGaP / study accession: phs003839 (must match upload study_accession; SRF/Manage Studies: phs + 6 digits)
 *     - Study name / acronym / description for auto-pop: AutomationQA_CRDCDH-3839-CTDC, AUTOQA, CRDCDH-3839
 *
 * Test data folder: InputFiles/CRDC/MetadataData/CRDCDH-3839_TC03/
 *   Upload (top-level TSVs only — uploaded via uploadFolder):
 *     CTDC_program_v1.22.1.tsv — omits system-populated-props: program_name, program_short_name, program_full_description
 *                              (type-only program row).
 *     CTDC_study_v1.22.1.tsv   — omits system-populated-props: study_name, study_short_name, study_description;
 *                              includes program.program_short_name NCTN, study_accession phs003839, and other required fields.
 *   Golden (expected/ — not uploaded; used after validate):
 *     expected/CTDC_program_v1.22.1_expected.tsv — full program row as Data View/export after validate (incl. auto-pop).
 *     expected/CTDC_study_v1.22.1_expected.tsv   — full study row as Data View/export after validate (incl. auto-pop).
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

// --- Create metadata-only CTDC submission for CTDC fixture study (SRF not automated in this TC) ---
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'CTDC', 'AUTOQA - AutomationQA_CRDCDH-3839-CTDC')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

// --- Upload minimal program/study TSVs (columns omitted intentionally; validator fills from SRF on validate) ---
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-3839_TC03')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

// --- Validation Results: aggregated view, expect zero issues ---
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults-Tab')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults_AggregatedExpanded-Btn')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.DataSubmissions.verifyNoValidationErrors'()

// --- Data View: UI vs expected/CTDC_*_expected.tsv; then export download vs same golden files ---
CustomKeywords.'utilities.DataSubmissions.verifyDataViewMatchesExpectedTsvForTC'('CRDCDH-3839_TC03')
CustomKeywords.'utilities.DataSubmissions.downloadAndCompareExpectedForTC'('CRDCDH-3839_TC03')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
