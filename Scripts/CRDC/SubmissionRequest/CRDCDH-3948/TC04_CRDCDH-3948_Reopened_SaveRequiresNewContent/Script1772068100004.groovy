/*
 * CRDCDH-3948 TC04 — Status: Reopened (after Fedlead Approve + Reopen on list)
 * Same dirty-state checks as TC01–TC03; save after edit moves to In Progress (req 5).
 */
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

WebUI.closeBrowser()

// Step 1 — Submitter: complete SR and submit
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.startSubmissionRequest'()
CustomKeywords.'utilities.SubmissionRequest.fillSubmissionRequestAllSections'(2, 'Other', 6, 1, 1, 'Open',
	'Yes', 'Cholangiocarcinoma', 'Homo', 1, 'genomics', 'proteomics', 5, 6, 1, 1)
String studyName = SubmissionRequest.lastEnteredStudyTitle
CustomKeywords.'utilities.WebUIUtils.waitUntilEnabledOrStop'(findTestObject('CRDC/SubmissionRequest/Submit-Btn'), 30, 'SR Submit')
CustomKeywords.'utilities.SubmissionRequest.clickSubmitButton'()

// Step 2 — Fedlead: approve then Reopen on list (does not open the questionnaire)
CustomKeywords.'utilities.Login.switchUser'('Fedlead')
CustomKeywords.'utilities.SubmissionRequest.approveSubmissionRequest'(studyName)
CustomKeywords.'utilities.SubmissionRequest.reopenApprovedSubmissionRequest'(studyName)

// Step 3 — Submitter: open form from list (Resume/View); expect Reopened
CustomKeywords.'utilities.Login.switchUser'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.openSubmissionRequestFormForStudy'(studyName)
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('REOPENED')

// Step 4 — Req 2: Save disabled and tooltip
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'disabled', 30)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', false)
CustomKeywords.'utilities.SubmissionRequest.verifySubmissionRequestSaveDisabledTooltip'()

// Step 5 — Req 3: Export / import unchanged file
String exportedExcel = CustomKeywords.'utilities.SubmissionRequest.exportSubmissionRequestFormExcel'(60)
CustomKeywords.'utilities.SubmissionRequest.importSubmissionRequestExcel'(exportedExcel)
CustomKeywords.'utilities.SubmissionRequest.verifySubmissionRequestSnackbarError'(
	SubmissionRequest.SR_IMPORT_ALL_PASSED_VALIDATION_MESSAGE, 20, false, 'CRDCDH-3948', 'success')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'disabled', 15)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', false)

// Step 6 — Req 4: Edit editable Reopened field (Primary Contact position; PI address is read-only)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Review/ProgressBar-SectionA')
CustomKeywords.'utilities.WebUIUtils.clearTextfieldByOs'('CRDC/AILocators/SubmissionRequests/Form/SectionA/PrimaryContact-Position-TxtBx')
WebUI.sendKeys(findTestObject('CRDC/AILocators/SubmissionRequests/Form/SectionA/PrimaryContact-Position-TxtBx'), '3948 Reopened Position')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'enabled', 15)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', true)

// Step 7 — Req 5: Save after Reopened transitions to In Progress
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn')
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('IN PROGRESS')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
