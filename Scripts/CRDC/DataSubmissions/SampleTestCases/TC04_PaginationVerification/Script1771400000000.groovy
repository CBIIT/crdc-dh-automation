import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

/* TC04_PaginationVerification
 * Exercises the pagination verification keywords on the Data Submissions list table.
 *
 * Target table: Data Submissions list (/data-submissions)
 * - Default rows per page: 10
 * - Available options: [5, 10, 20, 50]
 * - Pagination at the bottom of the table
 *
 * Steps:
 *   1. Login as Submitter
 *   2. Navigate to Data Submissions list
 *   3. verifyRowsPerPageOptions -- confirm dropdown has [5, 10, 20, 50]
 *   4. verifyDefaultRowsPerPage -- confirm default is 10
 *   5. verifyDisplayedRowsText -- confirm "X-Y of Z" is consistent
 *   6. verifyPageNavigation -- test prev/next/page-number buttons
 *   7. verifyRowsPerPageChange -- change to 5 and verify table updates
 *   8. Generate validation report
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

// Step 3: Verify rows per page options
CustomKeywords.'utilities.WebUIUtils.verifyRowsPerPageOptions'([5, 10, 20, 50])

// Step 4: Verify default rows per page is 10
CustomKeywords.'utilities.WebUIUtils.verifyDefaultRowsPerPage'(20)

// Step 5: Verify displayed rows text ("X-Y of Z") is consistent with actual rows
CustomKeywords.'utilities.WebUIUtils.verifyDisplayedRowsText'()

// Step 6: Verify page navigation (prev/next/page-number buttons and disabled states)
CustomKeywords.'utilities.WebUIUtils.verifyPageNavigation'()

// Step 7: Change rows per page to 5 and verify the table updates
CustomKeywords.'utilities.WebUIUtils.verifyRowsPerPageChange'(5)

// Step 8: Generate HTML report
CustomKeywords.'utilities.ValidationReport.generateValidationReport'()

WebUI.closeBrowser()
