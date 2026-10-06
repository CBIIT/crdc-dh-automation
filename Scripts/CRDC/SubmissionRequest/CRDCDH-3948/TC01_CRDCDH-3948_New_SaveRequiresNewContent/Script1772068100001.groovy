/*
 * CRDCDH-3948 TC01 — Status: New
 * Submitter: Save disabled until form is dirty; import unchanged Excel; save moves to In Progress.
 */
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

WebUI.closeBrowser()

// Step 1 — Login as Submitter and start a new Submission Request
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.startSubmissionRequest'()
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('NEW')

// Step 2 — Req 2: Save disabled and tooltip when form has no unsaved changes
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'disabled', 30)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', false)
CustomKeywords.'utilities.SubmissionRequest.verifySubmissionRequestSaveDisabledTooltip'()

// Step 3 — Req 3: Export form, re-import same file; no-change message; Save still disabled
String exportedExcel = CustomKeywords.'utilities.SubmissionRequest.exportSubmissionRequestFormExcel'(60)
CustomKeywords.'utilities.SubmissionRequest.importSubmissionRequestExcel'(exportedExcel)
CustomKeywords.'utilities.SubmissionRequest.verifySubmissionRequestSnackbarError'(
	SubmissionRequest.SR_IMPORT_NO_CHANGES_MESSAGE, 20, false, 'CRDCDH-3948', 'info')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'disabled', 15)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', false)

// Step 4 — Req 4: Edit a field; Save becomes enabled
CustomKeywords.'utilities.WebUIUtils.clearTextfieldByOs'('CRDC/AILocators/SubmissionRequests/Form/SectionA/PI-Address-TxtBx')
WebUI.sendKeys(findTestObject('CRDC/AILocators/SubmissionRequests/Form/SectionA/PI-Address-TxtBx'), '1 Automation Test Address')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'enabled', 15)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', true)

// Step 5 — Req 5: Save persists and status becomes In Progress
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn')
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('IN PROGRESS')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
