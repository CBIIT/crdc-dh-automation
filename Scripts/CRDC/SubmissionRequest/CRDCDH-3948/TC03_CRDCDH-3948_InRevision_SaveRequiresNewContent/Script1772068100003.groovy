/*
 * CRDCDH-3948 TC03 — Status: In Revision (after Fedlead Inquire)
 * Submitter submits; Fedlead inquires; Submitter reopens form from list; dirty-state checks; save stays In Revision.
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

// Step 2 — Fedlead: inquire (moves SR to In Revision on QA with CRDCDH-3948; legacy env may show Inquired)
CustomKeywords.'utilities.Login.switchUser'('Fedlead')
CustomKeywords.'utilities.SubmissionRequest.inquireSubmissionRequest'(studyName)

// Step 3 — Submitter: open form from list and confirm status
CustomKeywords.'utilities.Login.switchUser'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.openSubmissionRequestFormForStudy'(studyName)
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('IN REVISION')

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

// Step 6 — Req 4: Edit field; Save enabled
CustomKeywords.'utilities.WebUIUtils.clearTextfieldByOs'('CRDC/AILocators/SubmissionRequests/Form/SectionA/PI-Address-TxtBx')
WebUI.sendKeys(findTestObject('CRDC/AILocators/SubmissionRequests/Form/SectionA/PI-Address-TxtBx'), '3 Automation Test Address')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'enabled', 15)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', true)

// Step 7 — Req 5: Save while In Revision
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn')
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('IN REVISION')

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
