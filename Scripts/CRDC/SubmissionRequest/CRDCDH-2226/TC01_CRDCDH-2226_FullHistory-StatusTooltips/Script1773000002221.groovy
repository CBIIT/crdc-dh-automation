/*
 * CRDCDH-2226 TC01 — Full History status tooltips (CRDCDH-2158)
 *
 * What we test: Hovering status labels in Submission History shows the correct explanatory tooltip
 * for each application status (New, Approved, Rejected, etc.).
 *
 * Data strategy: Filter by status and open the first matching row (no fixed SR id).
 * Runs Full History tooltip checks on Approved, then on Rejected, to cover happy-path and rejected timelines.
 */
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// --- Setup: Submitter session, desktop viewport ---
WebUI.closeBrowser()
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')

// --- Step 1: Open first Approved SR from list (View) ---
// Verifies: filter returns at least one row; first row opens the questionnaire form.
CustomKeywords.'utilities.SubmissionRequestList.openFirstSubmissionRequestByStatus'('Approved', 'view')

// --- Step 2: Open Full History from status bar ---
// Verifies: status-bar-history-dialog appears (ValidationReport entry).
CustomKeywords.'utilities.SubmissionRequest.openFullHistoryDialog'()

// --- Step 3: Hover each history status and match tooltip copy ---
// Verifies: each known status in the timeline shows STATUS_DESCRIPTIONS text from the product.
CustomKeywords.'utilities.SubmissionRequest.verifyFullHistoryStatusTooltips'()

// --- Step 4: Rejected SR — same Full History tooltip coverage (includes Rejected status in timeline) ---
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/DataSubmissions/HistoryDialog/Close-Btn')
CustomKeywords.'utilities.SubmissionRequestList.openFirstSubmissionRequestByStatus'('Rejected', 'view')
CustomKeywords.'utilities.SubmissionRequest.openFullHistoryDialog'()
CustomKeywords.'utilities.SubmissionRequest.verifyFullHistoryStatusTooltips'()

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
