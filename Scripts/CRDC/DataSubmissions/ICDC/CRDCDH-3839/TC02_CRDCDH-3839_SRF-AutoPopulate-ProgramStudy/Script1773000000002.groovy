import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * CRDCDH-3839 TC02 — ICDC happy path: validator auto-populates program/study metadata from approved SRF (CRDCDH-3652).
 *
 * Preconditions (CRDC_QA):
 *   - Same approved SRF study as GC TC01: AUTOQA - AutomationQA_CRDCDH-3839 (AutoTest-Program / ATP).
 *   - program_full_description must be on the SRF at approval time (e.g. test program description).
 *
 * Test data folder: InputFiles/CRDC/MetadataData/CRDCDH-3839_TC02/
 *   Upload (top-level TSVs only — uploaded via uploadFolder):
 *     ICDC_program_v2.0.0.tsv — omits system-populated-props: program_name, program_acronym, program_full_description.
 *     ICDC_study_v2.0.0.tsv   — omits system-populated-props: clinical_study_name, clinical_study_designation,
 *                              clinical_study_description; includes other required ICDC fields (e.g. accession_id N/A).
 *   Golden (expected/ — not uploaded; used after validate):
 *     expected/ICDC_program_v2.0.0_expected.tsv — full program row as Data View/export after validate (incl. auto-pop).
 *     expected/ICDC_study_v2.0.0_expected.tsv   — full study row as Data View/export after validate (incl. auto-pop).
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

// --- Create metadata-only ICDC submission for fixture study (SRF not automated in this TC) ---
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.DataSubmissions.createDataSubmission'('New', 'metadata only', 'ICDC', 'AUTOQA - AutomationQA_CRDCDH-3839')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')

// --- Upload minimal program/study TSVs (columns omitted intentionally; validator fills from SRF on validate) ---
CustomKeywords.'utilities.DataSubmissions.uploadFolder'('CRDCDH-3839_TC02')
CustomKeywords.'utilities.DataSubmissions.clickValidateButton'()

// --- Validation Results: aggregated view, expect zero issues ---
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults-Tab')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults_AggregatedExpanded-Btn')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('Object Repository/CRDC/DataSubmissions/Loading-Icon', 90)
CustomKeywords.'utilities.DataSubmissions.verifyNoValidationErrors'()

// --- Data View: UI vs expected/ICDC_*_expected.tsv; then export download vs same golden files ---
CustomKeywords.'utilities.DataSubmissions.verifyDataViewMatchesExpectedTsvForTC'('CRDCDH-3839_TC02')
CustomKeywords.'utilities.DataSubmissions.downloadAndCompareExpectedForTC'('CRDCDH-3839_TC02')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
