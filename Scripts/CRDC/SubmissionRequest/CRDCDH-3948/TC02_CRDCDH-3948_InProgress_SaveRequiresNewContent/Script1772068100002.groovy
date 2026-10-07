/*
 * CRDCDH-3948 TC02 — Submission Request status: In Progress
 * Submitter: partial save, leave and reopen from list; same Save/import rules as TC01 on an In Progress form.
 * Import uses QA fixture with validation gaps (not export round-trip).
 */
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.configuration.RunConfiguration as RunConfiguration
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

// --- Setup: Submitter creates and saves an In Progress SR (Sections A + B), then reopens from list ---
WebUI.closeBrowser()
CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
CustomKeywords.'utilities.SubmissionRequest.startSubmissionRequest'()
CustomKeywords.'utilities.SubmissionRequest.enterPrincipalInvestigatorInfo'(2)
CustomKeywords.'utilities.SubmissionRequest.enterPrimaryContactInfo'(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn')
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/SubmissionRequest/Next-Btn')
CustomKeywords.'utilities.SubmissionRequest.enterProgramInfo'('Other', 6)
CustomKeywords.'utilities.SubmissionRequest.enterStudyInfo'(1)
CustomKeywords.'utilities.SubmissionRequest.enterFundingAgencyInfo'(1)
WebUI.delay(2)
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn')
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('IN PROGRESS')

// Step 1: Leave questionnaire, filter Submission Requests list by study name, Resume/View — verify still IN PROGRESS.
String studyName = SubmissionRequest.lastEnteredStudyTitle
CustomKeywords.'utilities.SubmissionRequest.openSubmissionRequestFormForStudy'(studyName)
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('IN PROGRESS')

// Step 2: On reopened form with no new edits — verify Save disabled and tooltip.
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'disabled', 30)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', false)
CustomKeywords.'utilities.SubmissionRequest.verifySubmissionRequestSaveDisabledTooltip'()

// Step 3: Import fixture Excel (incomplete C/D) — verify validation snackbar; import persists; Save still disabled.
String importFixture = RunConfiguration.getProjectDir() + '/' + SubmissionRequest.SR_IMPORT_VALIDATION_ERRORS_FIXTURE
CustomKeywords.'utilities.SubmissionRequest.importSubmissionRequestExcel'(importFixture)
CustomKeywords.'utilities.SubmissionRequest.verifySubmissionRequestSnackbarError'(
	SubmissionRequest.SR_IMPORT_VALIDATION_ERRORS_MESSAGE, 20, false, 'CRDCDH-3948', 'success')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'disabled', 15)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', false)

// Step 4: Edit PI Address — verify Save enabled.
CustomKeywords.'utilities.WebUIUtils.clearTextfieldByOs'('CRDC/AILocators/SubmissionRequests/Form/SectionA/PI-Address-TxtBx')
WebUI.sendKeys(findTestObject('CRDC/AILocators/SubmissionRequests/Form/SectionA/PI-Address-TxtBx'), '2 Automation Test Address')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'enabled', 15)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', true)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
