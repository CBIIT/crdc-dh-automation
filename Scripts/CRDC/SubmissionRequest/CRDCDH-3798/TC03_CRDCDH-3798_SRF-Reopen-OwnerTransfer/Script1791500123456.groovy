/*
 * CRDCDH-3798 TC03 — Phase 5: Fed Lead owner transfer on Reopen; new owner edit; prior owner no list access to latest.
 */
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

WebUI.closeBrowser()

// --- Submitter: create and submit SR (initial owner) ---
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.startSubmissionRequest'()
CustomKeywords.'utilities.SubmissionRequest.fillSubmissionRequestAllSections'(2, 'Other', 6, 1, 1, 'Open',
	'Yes', 'Cholangiocarcinoma', 'Homo', 1, 'genomics', 'proteomics', 5, 6, 1, 1)
String studyName = SubmissionRequest.lastEnteredStudyTitle
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Review-Btn')
CustomKeywords.'utilities.WebUIUtils.waitUntilEnabledOrStop'(findTestObject('CRDC/SubmissionRequest/Submit-Btn'), 30, 'SR Submit')
CustomKeywords.'utilities.SubmissionRequest.clickSubmitButton'()

// Step 1: Fed Lead approves.
CustomKeywords.'utilities.Login.switchUser'('Fedlead')
CustomKeywords.'utilities.SubmissionRequest.approveSubmissionRequest'(studyName)

// Step 2: Fed Lead Reopen — Owner shown; assign User profile; confirm (Submitter no Owner field: TC01 Step 3).
CustomKeywords.'utilities.SubmissionRequest.openReopenDialogForStudy'(studyName)
CustomKeywords.'utilities.SubmissionRequest.verifyReopenOwnerControlVisible'(true)
CustomKeywords.'utilities.SubmissionRequest.selectReopenDialogOwner'('pbac.user-automation')
CustomKeywords.'utilities.SubmissionRequest.confirmReopenDialog'()

// Step 3: New owner (User) — Resume/View on Reopened row; open form, edit, and Save to IN PROGRESS.
CustomKeywords.'utilities.Login.switchUser'('User')
CustomKeywords.'utilities.SubmissionRequest.assertResumeOrViewVisibleForStudy'(studyName, true, 'Reopened')
CustomKeywords.'utilities.SubmissionRequest.openSubmissionRequestFormForStudy'(studyName)
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('REOPENED')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Review/ProgressBar-SectionA')
CustomKeywords.'utilities.WebUIUtils.clearTextfieldByOs'('CRDC/AILocators/SubmissionRequests/Form/SectionA/PrimaryContact-Position-TxtBx')
WebUI.sendKeys(findTestObject('CRDC/AILocators/SubmissionRequests/Form/SectionA/PrimaryContact-Position-TxtBx'), '3798 TC03 Owner Position')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'enabled', 30)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn')
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('IN PROGRESS')

// Step 4: Prior owner (Submitter) — cannot open latest SRF from list (no Resume/View on In Progress row after User save).
CustomKeywords.'utilities.Login.switchUser'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.assertResumeOrViewVisibleForStudy'(studyName, false, 'In Progress')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
