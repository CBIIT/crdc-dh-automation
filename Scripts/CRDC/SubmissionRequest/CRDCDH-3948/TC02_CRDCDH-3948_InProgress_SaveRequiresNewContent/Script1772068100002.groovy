/*
 * CRDCDH-3948 TC02 — Status: In Progress
 * Submitter: save partial form, leave and reopen from list, then same dirty-state checks as TC01 (req 2–4).
 * Req 3 imports InputFiles fixture with invalid/incomplete data (not export round-trip).
 */
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.configuration.RunConfiguration as RunConfiguration
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import utilities.SubmissionRequest

WebUI.closeBrowser()

// Step 1 — Login and create an In Progress SR (Sections A + B including funding, save twice)
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

// Step 2 — Leave form, filter list by study, Resume/View back into the questionnaire
String studyName = SubmissionRequest.lastEnteredStudyTitle
CustomKeywords.'utilities.SubmissionRequest.openSubmissionRequestFormForStudy'(studyName)
CustomKeywords.'utilities.SubmissionRequest.verifyStatusBar'('IN PROGRESS')

// Step 3 — Req 2: Save disabled and tooltip
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'disabled', 30)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', false)
CustomKeywords.'utilities.SubmissionRequest.verifySubmissionRequestSaveDisabledTooltip'()

// Step 4 — Req 3: Import fixture Excel with validation gaps; snackbar; Save disabled after import persists (skipSave: false)
String importFixture = RunConfiguration.getProjectDir() + '/' + SubmissionRequest.SR_IMPORT_VALIDATION_ERRORS_FIXTURE
CustomKeywords.'utilities.SubmissionRequest.importSubmissionRequestExcel'(importFixture)
CustomKeywords.'utilities.SubmissionRequest.verifySubmissionRequestSnackbarError'(
	SubmissionRequest.SR_IMPORT_VALIDATION_ERRORS_MESSAGE, 20, false, 'CRDCDH-3948', 'success')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'disabled', 15)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', false)

// Step 5 — Req 4: Edit field; Save enabled
CustomKeywords.'utilities.WebUIUtils.clearTextfieldByOs'('CRDC/AILocators/SubmissionRequests/Form/SectionA/PI-Address-TxtBx')
WebUI.sendKeys(findTestObject('CRDC/AILocators/SubmissionRequests/Form/SectionA/PI-Address-TxtBx'), '2 Automation Test Address')
CustomKeywords.'utilities.WebUIUtils.waitUntilStableForButtonEnabledOrDisabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', 'enabled', 15)
CustomKeywords.'utilities.WebUIUtils.verifyButtonEnabled'('CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn', true)

CustomKeywords.'utilities.ValidationReport.generateValidationReport'()
WebUI.closeBrowser()
