import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/* TC03_TableSortingVerification
 * Exercises the new table sorting verification keywords on the Data Submissions list table.
 *
 * Target table: Data Submissions list (/data-submissions)
 * - Default sort: "Last Updated" descending
 * - All columns are sortable (no non-sortable columns in this view)
 *
 * Steps:
 *   1. Login as Submitter
 *   2. Navigate to Data Submissions list
 *   3. verifyTableColumnSortability -- confirm all columns are sortable
 *   4. verifyDefaultSortColumn -- confirm "Last Updated" desc is the default
 *   5. verifyColumnSortOnClick -- click-sort "Status" column (both asc and desc)
 *   6. verifyColumnSortOnClick -- click-sort "Data Commons" column (both asc and desc)
 *   7. Generate validation report
 */

WebUI.closeBrowser()

// Step 1: Login
CustomKeywords.'utilities.Login.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.Login.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

// Step 2: Navigate to Data Submissions list
CustomKeywords.'utilities.WebUIUtils.clickTab'('CRDC/AILocators/Global/Header/Desktop/DataSubmissions-Tab')
WebUI.delay(3)

// Step 3: Verify column sortability (this table has no non-sortable columns)
CustomKeywords.'utilities.WebUIUtils.verifyTableColumnSortability'([])

// Step 4: Verify default sort column and direction
CustomKeywords.'utilities.WebUIUtils.verifyDefaultSortColumn'('Last Updated', 'desc')

// Step 5: Click-sort "Status" column -- verifies asc then desc
CustomKeywords.'utilities.WebUIUtils.verifyColumnSortOnClick'('Status')

// Step 6: Click-sort "Data Commons" column -- verifies asc then desc
CustomKeywords.'utilities.WebUIUtils.verifyColumnSortOnClick'('Data Commons')

// Step 7: Generate HTML report
CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
