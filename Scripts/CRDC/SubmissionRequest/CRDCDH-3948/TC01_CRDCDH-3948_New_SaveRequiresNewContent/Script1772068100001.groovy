/*
 * CRDCDH-3948 TC01 — Submission Request status: New
 * Submitter: Save stays disabled until the form is dirty; unchanged import; save moves status to In Progress.
 */
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

// --- Setup: fresh browser, Submitter login, new questionnaire ---
WebUI.closeBrowser()
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')

// Step 1: Start a new Submission Request — verify status bar shows NEW.
CustomKeywords.'utilities.SubmissionRequest.startSubmissionRequest'()
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('NEW')

// Step 2: With no unsaved edits — verify Save is disabled and hover tooltip explains why.
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'disabled', 30)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', false)
CustomKeywords.'utilities.SubmissionRequest.verifySubmissionRequestSaveDisabledTooltip'()

// Step 3: Export form to Excel, re-import the same file — verify no-change snackbar (info);
//         Save remains disabled because nothing new was applied.
String exportedExcel = CustomKeywords.'utilities.SubmissionRequest.exportSubmissionRequestFormExcel'(60)
CustomKeywords.'utilities.SubmissionRequest.importSubmissionRequestExcel'(exportedExcel)
CustomKeywords.'utilities.SubmissionRequest.verifySubmissionRequestSnackbarError'(
	SubmissionRequest.SR_IMPORT_NO_CHANGES_MESSAGE, 20, false, 'CRDCDH-3948', 'info')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'disabled', 15)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', false)

// Step 4: Change PI Address — verify Save becomes enabled.
CustomKeywords.'utilities.WebUIUtils.clearTextfieldByOs'('CRDC/AILocators/SubmissionRequests/Form/SectionA/PI-Address-TxtBx')
WebUI.sendKeys(findTestObject('CRDC/AILocators/SubmissionRequests/Form/SectionA/PI-Address-TxtBx'), '1 Automation Test Address')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'enabled', 15)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', true)

// Step 5: Click Save — verify status bar changes from NEW to IN PROGRESS.
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn')
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('IN PROGRESS')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
