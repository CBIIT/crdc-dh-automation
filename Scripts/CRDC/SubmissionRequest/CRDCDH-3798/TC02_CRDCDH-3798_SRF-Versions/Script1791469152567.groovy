/*
 * CRDCDH-3798 TC02 — Step 2: Show All Versions, SRF Version column, Form Version hidden in GUI; 4b Reopen only on latest approved row.
 * Phase 5 (ownership transfer / prior-owner visibility) deferred until Reopen dialog Owner control is on QA.
 */
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

WebUI.closeBrowser()

// --- Submitter: full questionnaire and submit (SRF version 1) ---
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.startSubmissionRequest'()
CustomKeywords.'utilities.SubmissionRequest.fillSubmissionRequestAllSections'(2, 'Other', 6, 1, 1, 'Open',
	'Yes', 'Cholangiocarcinoma', 'Homo', 1, 'genomics', 'proteomics', 5, 6, 1, 1)
String studyName = SubmissionRequest.lastEnteredStudyTitle
String studyAbbrev = SubmissionRequest.lastEnteredStudyAbbreviation
CustomKeywords.'utilities.WebUIUtils.waitUntilEnabledOrStop'(findTestObject('CRDC/SubmissionRequest/Submit-Btn'), 30, 'SR Submit')
CustomKeywords.'utilities.SubmissionRequest.clickSubmitButton'()

// Step 1: Fed Lead approves version 1.
CustomKeywords.'utilities.Login.switchUser'('Fedlead')
CustomKeywords.'utilities.SubmissionRequest.approveSubmissionRequest'(studyName)

// Step 2: Submitter Reopen → Confirm — edit and Save to IN PROGRESS — Review and Submit for version 2.
CustomKeywords.'utilities.Login.switchUser'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.openReopenDialogForStudy'(studyName)
CustomKeywords.'utilities.SubmissionRequest.confirmReopenDialog'()
CustomKeywords.'utilities.SubmissionRequest.openSubmissionRequestFormForStudy'(studyName)
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('REOPENED')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Review/ProgressBar-SectionA')
CustomKeywords.'utilities.WebUIUtils.clearTextfieldByOs'('CRDC/AILocators/SubmissionRequests/Form/SectionA/PrimaryContact-Position-TxtBx')
WebUI.sendKeys(findTestObject('CRDC/AILocators/SubmissionRequests/Form/SectionA/PrimaryContact-Position-TxtBx'), '3798 V2 Position')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'enabled', 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn')
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('IN PROGRESS')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Review-Btn')
CustomKeywords.'utilities.WebUIUtils.waitUntilEnabledOrStop'(findTestObject('CRDC/SubmissionRequest/Submit-Btn'), 30, 'SR Submit')
CustomKeywords.'utilities.SubmissionRequest.clickSubmitButton'()

// Step 3: Fed Lead approves version 2 (two approved versions for same study).
CustomKeywords.'utilities.Login.switchUser'('Fedlead')
CustomKeywords.'utilities.SubmissionRequest.approveSubmissionRequest'(studyName)

// Step 4: Submission Requests list — Show All Versions default off; SRF Version visible; Form Version / legacy Version hidden in GUI.
CustomKeywords.'utilities.Login.switchUser'('Submitter')
CustomKeywords.'utilities.SubmissionRequestList.navigateToSubmissionRequestsList'()
CustomKeywords.'utilities.SubmissionRequestList.applySubmissionRequestsListFilters'([study: studyName, status: 'Approved'] as Map<String, String>)
CustomKeywords.'utilities.SubmissionRequestList.verifyShowAllVersionsDefaultUnchecked'()
CustomKeywords.'utilities.SubmissionRequestList.verifySubmissionRequestsSrfVersionColumnVisible'()
CustomKeywords.'utilities.SubmissionRequestList.verifySubmissionRequestsFormVersionColumnHiddenInGui'()

// Step 5: Show All Versions — verify two rows for study, SRF versions descending; Reopen only on latest approved row (4b).
CustomKeywords.'utilities.SubmissionRequestList.setShowAllVersions'(true)
WebUI.delay(2)
// List Study column shows truncated abbrev; full value is on truncated-text-wrapper aria-label (matches studyAbbrev).
CustomKeywords.'utilities.SubmissionRequestList.verifySrVersionsDescendingForStudy'(studyAbbrev, 2)
CustomKeywords.'utilities.SubmissionRequestList.verifyReopenOnlyOnLatestSrVersionForStudy'(studyAbbrev)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
