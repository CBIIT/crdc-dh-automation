/*
 * CRDCDH-3798 TC01 — Step 1: Reopen confirmation dialog, Submitter owner reopen, non-owner list access, In Progress no Reopen (4a).
 * Fed Lead approves only; Submitter performs Reopen from the list (not Admin / not Fed Lead reopen).
 */
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

WebUI.closeBrowser()

// --- Submitter: full questionnaire (In Progress), 4a list check, then submit ---
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.startSubmissionRequest'()
CustomKeywords.'utilities.SubmissionRequest.fillSubmissionRequestAllSections'(2, 'Other', 6, 1, 1, 'Open',
	'Yes', 'Cholangiocarcinoma', 'Homo', 1, 'genomics', 'proteomics', 5, 6, 1, 1)
String studyName = SubmissionRequest.lastEnteredStudyTitle

// Step 1 (4a): On list while still In Progress (before submit) — verify Reopen is not on this row.
CustomKeywords.'utilities.SubmissionRequest.assertReopenButtonVisibleForStudy'(studyName, false, 'In Progress')
CustomKeywords.'utilities.SubmissionRequest.openSubmissionRequestFormForStudy'(studyName)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Review-Btn')
CustomKeywords.'utilities.WebUIUtils.waitUntilEnabledOrStop'(findTestObject('CRDC/SubmissionRequest/Submit-Btn'), 30, 'SR Submit')
CustomKeywords.'utilities.SubmissionRequest.clickSubmitButton'()

// Step 2: Fed Lead approves from the list (no Reopen — owner Submitter will reopen later).
CustomKeywords.'utilities.Login.switchUser'('Fedlead')
CustomKeywords.'utilities.SubmissionRequest.approveSubmissionRequest'(studyName)

// Step 3: Submitter opens Reopen dialog — no Owner field; verify title, message, study/program; Cancel — row still Approved with Reopen for owner.
CustomKeywords.'utilities.Login.switchUser'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.openReopenDialogForStudy'(studyName)
CustomKeywords.'utilities.SubmissionRequest.verifyReopenOwnerControlVisible'(false)
CustomKeywords.'utilities.SubmissionRequest.verifyReopenSubmissionRequestDialog'()
CustomKeywords.'utilities.SubmissionRequest.cancelReopenDialog'()
CustomKeywords.'utilities.SubmissionRequest.assertReopenButtonVisibleForStudy'(studyName, true, 'Approved')

// Step 4: User (non-owner) on Approved row — verify Reopen is not shown (ownership; status still Approved).
CustomKeywords.'utilities.Login.switchUser'('User')
CustomKeywords.'utilities.SubmissionRequest.assertReopenButtonVisibleForStudy'(studyName, false, 'Approved')

// Step 5: Submitter Reopen → Confirm — open form from list and verify status bar REOPENED.
CustomKeywords.'utilities.Login.switchUser'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.openReopenDialogForStudy'(studyName)
CustomKeywords.'utilities.SubmissionRequest.confirmReopenDialog'()
CustomKeywords.'utilities.SubmissionRequest.openSubmissionRequestFormForStudy'(studyName)
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('REOPENED')

// Step 6: Edit on Reopened form — wait for Save enabled (3948), then Save — verify REOPENED → IN PROGRESS.
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Review/ProgressBar-SectionA')
CustomKeywords.'utilities.WebUIUtils.clearTextfieldByOs'('CRDC/AILocators/SubmissionRequests/Form/SectionA/PrimaryContact-Position-TxtBx')
WebUI.sendKeys(findTestObject('CRDC/AILocators/SubmissionRequests/Form/SectionA/PrimaryContact-Position-TxtBx'), '3798 Reopen Position')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'enabled', 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn')
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('IN PROGRESS')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
