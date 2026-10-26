package utilities
import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.checkpoint.Checkpoint
import com.kms.katalon.core.checkpoint.CheckpointFactory
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testcase.TestCase
import com.kms.katalon.core.testcase.TestCaseFactory
import com.kms.katalon.core.testdata.TestData
import com.kms.katalon.core.testdata.TestDataFactory
import com.kms.katalon.core.testobject.ObjectRepository
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords
import internal.GlobalVariable
import org.openqa.selenium.WebElement
import org.openqa.selenium.WebDriver
import org.openqa.selenium.By
import com.kms.katalon.core.mobile.keyword.internal.MobileDriverFactory
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.testobject.RequestObject
import com.kms.katalon.core.testobject.ResponseObject
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.mobile.helper.MobileElementCommonHelper
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.exception.WebElementNotFoundException
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.ProfilesIni;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.util.Iterator;
import java.util.Set;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.openqa.selenium.Keys as Keys

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.ElementClickInterceptedException
import org.openqa.selenium.StaleElementReferenceException
import com.kms.katalon.core.exception.StepFailedException
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Cookie as Cookie
import com.kms.katalon.core.configuration.RunConfiguration
import org.apache.poi.ss.usermodel.*

import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import java.awt.Color
import utilities.Utils


class Pbac extends TestRunner {

	public static WebDriver driver

	private static final String PBAC_NOTISTACK_SNACKBAR_LOC = 'CRDC/AILocators/Global/Notistack-Snackbar'
	private static final int PBAC_NOTISTACK_WAIT_SEC = 20
	private static final int PBAC_MANAGE_USERS_TABLE_WAIT_SEC = 25
	/** Display labels in the Edit User Role dropdown, keyed by Pbac role (Fedlead, Dcp, Admin, Submitter, User). */
	private static final Map<String, String> ROLE_DISPLAY_MAP = [
		'User'     : 'User',
		'Submitter': 'Submitter',
		'Dcp'      : 'Data Commons Personnel',
		'Fedlead'  : 'Federal Lead',
		'Admin'    : 'Admin'
	]

	/**
	 * Notistack toasts (e.g. after Save) intercept PBAC checkbox clicks on slow CI; wait until hidden or gone.
	 * Non-fatal if the snackbar was never shown or Utils times out.
	 */
	private static void waitForPbacNotistackDismissed() {
		try {
			Utils.waitForElementNotVisible(PBAC_NOTISTACK_SNACKBAR_LOC, PBAC_NOTISTACK_WAIT_SEC)
		} catch (Throwable t) {
			KeywordUtil.logInfo("waitForPbacNotistackDismissed: ${t.message}")
		}
	}

	private static void waitForManageUsersTable(WebDriver driver) {
		WebDriverWait wait = new WebDriverWait(driver, PBAC_MANAGE_USERS_TABLE_WAIT_SEC)
		wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//table//tr[td]")))
	}

	/**
	 * Re-open Manage Users when the user list table is not found (e.g. slow redirect after Save on Jenkins).
	 */
	private static void navigateBackToManageUsers() {
		KeywordUtil.logInfo("Re-navigating to Manage Users via header.")
		WebUIUtils.clickTab('CRDC/Login/UserProfile-Dd')
		WebUIUtils.clickTab('CRDC/NavBar/ManageUsers-Btn')
		WebUI.delay(2)
		WebUI.waitForPageLoad(15)
	}

	/**
	 * Native-click an Edit User MUI select (Role by default; pass DataCommons-Ddn for that field).
	 * clickTab JS-click reports success but does not open the MUI list. Retries
	 * StaleElementReferenceException after the form remounts on Edit.
	 * @param locatorKey Object Repository path; defaults to CRDC/ManageUsers/Role-Ddn
	 */
	private static void clickPbacRoleDropdown(String locatorKey = 'CRDC/ManageUsers/Role-Ddn') {
		TestObject to = findTestObject(locatorKey)
		if (!WebUIUtils.waitForElementClickableWithStaleRetry(to, 10)) {
			return
		}
		for (int i = 0; i < 3; i++) {
			try {
				WebUI.click(to)
				return
			} catch (StepFailedException e) {
				if (e.getCause() instanceof StaleElementReferenceException) {
					KeywordUtil.logInfo("clickPbacRoleDropdown(${locatorKey}): stale element, retrying (attempt ${i + 1}/3)...")
					WebUI.delay(1)
					if (!WebUIUtils.waitForElementClickableWithStaleRetry(to, 10)) {
						return
					}
				} else {
					throw e
				}
			}
		}
		KeywordUtil.markFailed("Unable to click ${locatorKey} after stale retries")
	}

	private static void clickPbacPermissionCheckbox(WebElement cb) {
		try {
			cb.click()
		} catch (ElementClickInterceptedException e) {
			KeywordUtil.logInfo("Checkbox click intercepted (likely Notistack); waiting and retrying once. ${e.message}")
			WebUI.delay(1)
			waitForPbacNotistackDismissed()
			cb.click()
		}
		try {
			Thread.sleep(150)
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt()
		}
	}

	//*************** Input functions start here ****************

	/**
	 * Find a user in the Manage Users table by name
	 * @param User role Fedlead, Dcp, Admin, Submitter, User
	 */
	@Keyword
	public static void findAndEditUserByName(String userRole) {
		String userNameAutomation = "pbac." + userRole + "-automation"
		boolean userFound = false
		boolean manageUsersRenavAttempted = false
		WebUI.delay(2)
		WebUI.waitForPageLoad(15)

		while (!userFound) {
			driver = DriverFactory.getWebDriver()
			try {
				waitForManageUsersTable(driver)
			} catch (TimeoutException e) {
				if (!manageUsersRenavAttempted) {
					manageUsersRenavAttempted = true
					KeywordUtil.logInfo("Manage Users table wait timed out; retrying after navigation. ${e.message}")
					navigateBackToManageUsers()
					waitForManageUsersTable(driver)
				} else {
					throw e
				}
			}

			List<WebElement> rows = driver.findElements(By.xpath("//table//tr[td]"))

			for (WebElement row : rows) {
				try {
					List<WebElement> cells = row.findElements(By.tagName("td"))
					if (cells.size() < 6) continue // not enough columns, skip

						String nameText = cells[0].getText().trim()
					System.out.println("Name in cell: " + nameText)
					if (nameText.toLowerCase().contains(userNameAutomation.trim().toLowerCase())) {
						WebElement editBtn = cells[5].findElement(By.xpath(".//button[contains(text(),'Edit')]"))
						editBtn.click()
						System.out.println("User '${userNameAutomation}' found and Edit clicked.")
						userFound = true
						break
					}
				} catch (org.openqa.selenium.StaleElementReferenceException e) {
					System.out.println("Stale row element, trying next row...")
					continue
				}
			}

			// If not found, check for next page until last page
			if (!userFound) {
				TestObject nextButton = findTestObject('CRDC/ManageUsers/Next-Btn')
				boolean isDisabled = WebUI.getAttribute(nextButton, 'disabled') != null

				if (isDisabled) {
					KeywordUtil.markFailedAndStop("Reached the last page. User '${userNameAutomation}' not found in Manage Users table. Stopping test.")
				} else {
					WebUI.click(nextButton)
					WebUI.delay(2) // wait for new page
				}
			}
		}
	}

	/**
	 * Set a PBAC automation user's account status to Active or Inactive and Save.
	 * Opens the user with findAndEditUserByName. Save returns to the Manage Users list.
	 * @param userRole Fedlead, Dcp, Admin, Submitter, User
	 * @param status Active or Inactive
	 */
	@Keyword
	public static void setUserAccountStatus(String userRole, String status) {
		if (status != 'Active' && status != 'Inactive') {
			KeywordUtil.markFailedAndStop("setUserAccountStatus: status must be Active or Inactive, got '${status}'")
		}
		findAndEditUserByName(userRole)
		WebUI.click(findTestObject('CRDC/AILocators/Users/Profile/AccountStatus-Ddn'))
		TestObject statusOption = findTestObject('CRDC/AILocators/Users/Profile/AccountStatus-Option', [('status') : status])
		WebUI.waitForElementClickable(statusOption, 10)
		WebUI.click(statusOption)
		WebUIUtils.clickTab('CRDC/AILocators/Users/Profile/Save-Btn')
		waitForPbacNotistackDismissed()
		KeywordUtil.logInfo("setUserAccountStatus: ${userRole} -> ${status}")
	}

	/**
	 * For each role, set Inactive, log Account Disabled, then restore Active.
	 * Caller must already be on Manage Users. Inactive is the only send; Active is restore only.
	 * Do not pass Master.
	 * @param userRoles Admin, Fedlead, Dcp, Submitter, User
	 */
	@Keyword
	public static void disableThenRestoreUserAccounts(List<String> userRoles) {
		if (userRoles == null || userRoles.isEmpty()) {
			KeywordUtil.markFailedAndStop("disableThenRestoreUserAccounts requires at least one role")
		}
		for (String role : userRoles) {
			if (role == null || role.trim().isEmpty()) {
				KeywordUtil.markFailedAndStop("disableThenRestoreUserAccounts: blank role")
			}
			if (role.equalsIgnoreCase('Master')) {
				KeywordUtil.markFailedAndStop("disableThenRestoreUserAccounts: do not inactivate Master")
			}
			String email = GlobalVariable."${role}Email"
			String nameToken = "pbac." + role.toLowerCase() + "-automation"
			try {
				setUserAccountStatus(role, 'Inactive')
				NotificationEmailLog.logEmails('Account Disabled', 'Master', email, nameToken, role)
			} finally {
				try {
					setUserAccountStatus(role, 'Active')
				} catch (Throwable t) {
					KeywordUtil.markFailedAndStop("Failed to restore ${role} to Active: ${t.message}")
				}
			}
		}
	}

	/**
	 * Write the expected PBAC permissions for the role to Output Excel sheet
	 * @param User role (Fedlead, Dcp, Admin, Submitter, User), path of Katalon Data File
	 */
	@Keyword
	def static void writePbacExpectedPermissionsForRole(String userRole, String dataFilePath) {
		def testData = TestDataFactory.findTestData(dataFilePath)
		int rowCount = testData.getRowNumbers()

		String outputPath = RunConfiguration.getProjectDir() + "/OutputFiles/PBAC_Defaults_Results.xlsx"
		File file = new File(outputPath)
		File outputDir = file.getParentFile()
		if (!outputDir.exists()) {
			outputDir.mkdirs()
		}
		Workbook workbook = file.exists() ? new XSSFWorkbook(new FileInputStream(file)) : new XSSFWorkbook()
		Sheet sheet = workbook.getSheet(userRole) ?: workbook.createSheet(userRole)

		// Header
		Row header = sheet.createRow(0)
		CellStyle headerStyle = workbook.createCellStyle()
		Font headerFont = workbook.createFont()
		headerFont.setBold(true)
		headerStyle.setFont(headerFont)
		headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		String[] headers = [
			"Permission",
			//ADDED (restored original: internal name in col 0)
			"Expected",
			"Actual",
			"Name",
			//ADDED (expected display label in col 3)
			"Actual Name",
			//ADDED (UI-captured label in col 4)
			"Result"
		]
		for (int i = 0; i < headers.length; i++) {
			Cell cell = header.createCell(i)
			cell.setCellValue(headers[i])
			cell.setCellStyle(headerStyle)
		}

		// Adjust column widths
		for (int columnIndex = 0; columnIndex <= 5; columnIndex++) {
			//was columnIndex <= 3
			sheet.setColumnWidth(columnIndex, 8000)
		}

		for (int i = 1; i <= rowCount; i++) {
			String expected = testData.getValue(userRole, i)
			String internalName = testData.getValue('Permission Internal Name', i) //ADDED (restore internal name)
			String displayName  = testData.getValue('Name', i)                    //ADDED (expected display label)

			if (!internalName) continue //ADDED (ensure mapping compatibility)

				Row row = sheet.createRow(i)
			row.createCell(0).setCellValue(internalName) //ADDED (write internal name back to col 0)
			row.createCell(1).setCellValue(expected)
			row.createCell(3).setCellValue(displayName ?: "") //ADDED (stash expected display label for name check)
			// col 2 (Actual), col 4 (Actual Name), col 5 (Result) will be filled in verification
		}

		FileOutputStream fos = new FileOutputStream(file)
		workbook.write(fos)
		workbook.close()
		fos.close()

		KeywordUtil.logInfo("Expected permissions for ${userRole} written to ${outputPath}")
	}


	/**
	 * Get state and name of each PBAC permission, write to output sheet, and compare both state and name
	 * @param User role Fedlead, Dcp, Admin, Submitter, User
	 */
	@Keyword
	public static void verifyPbacActualPermissionsForRole(String userRole) {
		String outputPath = RunConfiguration.getProjectDir() + "/OutputFiles/PBAC_Defaults_Results.xlsx"
		FileInputStream fis = new FileInputStream(outputPath)
		Workbook workbook = new XSSFWorkbook(fis)
		Sheet sheet = workbook.getSheet(userRole)
		if (!sheet) {
			KeywordUtil.markFailed("Sheet '${userRole}' not found in output file.")
		}

		// Set up colors
		XSSFCellStyle greenStyle = (XSSFCellStyle) workbook.createCellStyle()
		greenStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.index)
		greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)

		XSSFCellStyle redStyle = (XSSFCellStyle) workbook.createCellStyle()
		redStyle.setFillForegroundColor(IndexedColors.RED.index)
		redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)

		WebUI.delay(5)

		// Locate checkboxes
		List<WebElement> checkboxes = WebUI.findWebElements(findTestObject('CRDC/ManageUsers/PbacOptions-Chkbx'), 10)

		// Locate checkbox labels
		List<WebElement> labels = WebUI.findWebElements(findTestObject('CRDC/ManageUsers/PbacOptions-ChkbxLabel'), 10)

		if (labels.size() != checkboxes.size()) {
			KeywordUtil.logInfo("WARNING: label/checkbox count mismatch: labels=${labels.size()} checkboxes=${checkboxes.size()}")
		}

		// Default values
		boolean overallResult = true

		for (int i = 1; i <= sheet.getLastRowNum(); i++) {
			Row row = sheet.getRow(i)
			if (row == null) continue

				Cell actualCell = row.getCell(2) ?: row.createCell(2)
			Cell expectedCell = row.getCell(1)
			Cell passFailCell = row.getCell(5) ?: row.createCell(5)

			// Prepare "Actual Name" column (col 4)
			Cell actualNameCell = row.getCell(4) ?: row.createCell(4)

			// Expected display Name from sheet (col 3)
			Cell expectedNameCell = row.getCell(3)
			String expectedName = expectedNameCell?.getStringCellValue()?.trim() ?: ""

			// Handle if fewer checkboxes in UI compared to expected
			if (i - 1 >= checkboxes.size()) {
				actualCell.setCellValue("MISSING_UI")
				passFailCell.setCellValue("Fail")
				passFailCell.setCellStyle(redStyle)
				actualNameCell.setCellValue("MISSING_UI")
				KeywordUtil.logInfo("Row ${i} - ${expectedName} | Expected: ${expectedCell?.getStringCellValue()} | Actual: CHECKBOX_MISSING_UI | NameActual: MISSING_UI | Result: Fail")
				overallResult = false
				continue
			}

			// Also guard labels size
			String uiLabelText = ""
			if (i - 1 < labels.size()) {
				uiLabelText = labels[i - 1]?.getText()?.trim() ?: ""
			} else {
				uiLabelText = "MISSING_LABEL_UI"
			}
			actualNameCell.setCellValue(uiLabelText)

			WebElement cb = checkboxes[i - 1]
			boolean isEnabled = cb.isEnabled()
			boolean isChecked = cb.isSelected()

			String actualStatus = ""
			if (!isEnabled && isChecked) actualStatus = "fixed_checked"
			else if (!isEnabled && !isChecked) actualStatus = "fixed_unchecked"
			else if (isEnabled && isChecked) actualStatus = "checked"
			else if (isEnabled && !isChecked) actualStatus = "unchecked"
			else actualStatus = "UNKNOWN"

			actualCell.setCellValue(actualStatus)

			// Permission display name now tracked via expectedName (col 3)
			String permissionName = expectedName

			String expected = expectedCell?.getStringCellValue()
			boolean stateMatches = (expected && expected.equalsIgnoreCase(actualStatus))
			boolean nameMatches = (permissionName && uiLabelText && permissionName.equalsIgnoreCase(uiLabelText))

			if (stateMatches && nameMatches) {
				passFailCell.setCellValue("Pass")
				passFailCell.setCellStyle(greenStyle)
			} else {
				passFailCell.setCellValue("Fail")
				passFailCell.setCellStyle(redStyle)
				overallResult = false
			}

			// Console output (now includes name comparison)
			KeywordUtil.logInfo(
					"Row ${i} - ${permissionName} | ExpectedState: ${expected} | ActualState: ${actualStatus} | " +
					"ExpectedName: ${permissionName} | ActualName: ${uiLabelText} | " +
					"StateMatch: ${stateMatches} | NameMatch: ${nameMatches} | Result: ${passFailCell.getStringCellValue()}"
					)
		}

		fis.close()
		FileOutputStream fos = new FileOutputStream(outputPath)
		workbook.write(fos)
		workbook.close()
		fos.close()

		if (overallResult == false) {
			KeywordUtil.markFailed("There is a FAILURE -- verify in output Excel: "+ outputPath)
		} else {
			KeywordUtil.logInfo("Actual permissions for '${userRole}' recorded and compared.")
		}
	}


	/**
	 * Resets the user's permissions to the default for the specified role by toggling away and back.
	 * @param role The target role to reset to (e.g., 'User', 'Submitter', 'Dcp', 'Fedlead', 'Admin')
	 */
	@Keyword
	public static void resetPermissions(String role) {
		KeywordUtil.logInfo("Resetting permissions for role: ${role}")
		String roleDisplay = ROLE_DISPLAY_MAP[role]
		if (!roleDisplay) {
			KeywordUtil.markFailed("Invalid role passed to resetPermissions: ${role}")
			return
		}

		clickPbacRoleDropdown()
		KeywordUtil.logInfo("Clicked role dropdown")

		// Find an alternate role to toggle to
		String alternateRole = ROLE_DISPLAY_MAP.values().find { it != roleDisplay }
		if (!alternateRole) {
			KeywordUtil.markFailed("Could not find alternate role to switch to.")
			return
		}

		// Select alternate role
		TestObject alternateOption = findTestObject('CRDC/ManageUsers/RoleDdn-Option', [('role') : alternateRole])
		WebUI.waitForElementClickable(alternateOption, 10)
		WebUI.click(alternateOption)
		KeywordUtil.logInfo("Selected alternate role: ${alternateRole}")
		WebUI.delay(2)

		// Open dropdown again
		clickPbacRoleDropdown()
		KeywordUtil.logInfo("Clicked role dropdown again")
		WebUI.delay(2)

		// Re-select target role
		TestObject targetOption = findTestObject('CRDC/ManageUsers/RoleDdn-Option', [('role') : roleDisplay])
		WebUI.waitForElementClickable(targetOption, 10)
		WebUI.click(targetOption)
		KeywordUtil.logInfo("Selected target role: ${roleDisplay}")
		WebUI.delay(2)

		// Click Save
		clickTab('CRDC/ManageUsers/Save-Btn')
		KeywordUtil.logInfo("Clicked Save")
		waitForPbacNotistackDismissed()

		KeywordUtil.logInfo("Reset permissions by re-selecting role: ${roleDisplay}")
	}

	/**
	 * Verifies the PBAC Permissions for the user
	 * @param User role (Fedlead, Dcp, Admin, Submitter, User)
	 */ 
	@Keyword
	public static void verifyPbacPermissionDefaults(String userRole) {
		String loggedInAs = WebUI.getText(findTestObject('CRDC/Login/UserProfile-Dd'))
		KeywordUtil.logInfo("Logged in as: '${loggedInAs}' and verifying permissions for: '${userRole}'")

		//As Submitter or User role verifying self, should not be able to see permissions in profile
		if (userRole.equalsIgnoreCase("Submitter-self") || userRole.equalsIgnoreCase("User-self")) {
			if (WebUI.verifyElementPresent(findTestObject("CRDC/ManageUsers/PermissionsPanel-Ddn"), 5, FailureHandling.OPTIONAL) ||
					WebUI.verifyElementPresent(findTestObject("CRDC/ManageUsers/NotificationsPanel-Ddn"), 5, FailureHandling.OPTIONAL)) {
				KeywordUtil.markFailed("FAIL -- " + userRole + " is able to see permissions in profile")
			}
			else {
				KeywordUtil.logInfo("PASS -- " + userRole + " is not able to view permissions in profile")
				return;
			}
		}

		//As admin user only, find user in Manage Users table
		if (loggedInAs.toLowerCase().contains("admin") && !userRole.equalsIgnoreCase("admin-self")) {
			// if verifying self as admin, skip search
			findAndEditUserByName(userRole)
			resetPermissions(userRole)
			WebUI.delay(2)
			findAndEditUserByName(userRole)
		}

		//Expand the Permissions panel and Email Notifications panel
		clickTab('CRDC/ManageUsers/PermissionsPanel-Ddn')
		clickTab('CRDC/ManageUsers/NotificationsPanel-Ddn')

		//Write expected results to sheet
		writePbacExpectedPermissionsForRole(userRole,"CRDC/Pbac/Permissions")

		//Verify results
		verifyPbacActualPermissionsForRole(userRole)
	}

	/**
	 * Check all enabled permission checkboxes on the Edit User page
	 */
	@Keyword
	public static void checkAllEnabledPermissionCheckboxes() {
		waitForPbacNotistackDismissed()
		List<WebElement> checkboxes = WebUI.findWebElements(findTestObject('CRDC/ManageUsers/PbacOptions-Chkbx'), 10)
		for (WebElement cb : checkboxes) {
			if (cb.isEnabled() && !cb.isSelected()) {
				clickPbacPermissionCheckbox(cb)
				KeywordUtil.logInfo("Checked permission checkbox")
			}
		}
	}

	/**
	 * Uncheck all enabled permission checkboxes on the Edit User page
	 */
	@Keyword
	public static void uncheckAllEnabledPermissionCheckboxes() {
		waitForPbacNotistackDismissed()
		List<WebElement> checkboxes = WebUI.findWebElements(findTestObject('CRDC/ManageUsers/PbacOptions-Chkbx'), 10)
		for (WebElement cb : checkboxes) {
			if (cb.isEnabled() && cb.isSelected()) {
				clickPbacPermissionCheckbox(cb)
				KeywordUtil.logInfo("Unchecked permission checkbox")
			}
		}
	}

	/**
	 * Edits the PBAC Permissions for the user and writes expected permissions to output sheet (must be logged in as admin role)
	 * @param User role (Fedlead, Dcp, Admin, Submitter, User), scenario (positive, negative)
	 */
	@Keyword
	public static void editPbacPermissions(String userRole, String scenario) {

		findAndEditUserByName(userRole)
		resetPermissions(userRole)
		WebUI.delay(2)
		findAndEditUserByName(userRole)

		//Expand the Permissions panel and Email Notifications panel
		clickTab('CRDC/ManageUsers/PermissionsPanel-Ddn')
		clickTab('CRDC/ManageUsers/NotificationsPanel-Ddn')

		if (scenario.equals("positive")) {
			checkAllEnabledPermissionCheckboxes()
			WebUI.delay(2)
			WebUI.click(findTestObject('CRDC/ManageUsers/Save-Btn'))
			waitForPbacNotistackDismissed()
		}
		if (scenario.equals("negative")) {
			uncheckAllEnabledPermissionCheckboxes()
			uncheckAllEnabledPermissionCheckboxes() //do it again to uncheck DCP's SR - View permission because it is disabled until higher permissions are unchecked first
			WebUI.delay(2)
			WebUI.click(findTestObject('CRDC/ManageUsers/Save-Btn'))
			waitForPbacNotistackDismissed()
		}

		//Write expected results to sheet
		writePbacExpectedPermissionsForRole(userRole + "-" + scenario,"CRDC/Pbac/Permissions")
	}


	/**
	 * Gets all the checked or unchecked PBAC Permissions for the user role from the output sheet
	 * @param User role sheet name, expected status (checked or unchecked)
	 */
	public static List<String> getPermissionsByStatus(String roleSheetName, String scenario) {
		List<String> permissions = new ArrayList<>();
		String outputPath = RunConfiguration.getProjectDir() + "/OutputFiles/PBAC_Defaults_Results.xlsx";

		FileInputStream fis = null
		Workbook workbook = null

		try {
			fis = new FileInputStream(outputPath)
			workbook = new XSSFWorkbook(fis)
			Sheet sheet = workbook.getSheet(roleSheetName + "-" + scenario)

			if (sheet == null) {
				KeywordUtil.markWarning("Sheet '${roleSheetName} + "-" + ${scenario}' not found.")
				return permissions
			}

			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i)
				if (row == null) continue

					Cell permissionCell = row.getCell(0)  // Permission name
				Cell expectedCell = row.getCell(1)    // Expected status

				if (permissionCell == null || expectedCell == null) continue

					String status = expectedCell.getStringCellValue()
				if (scenario.equals("positive")) {
					//positive scenario
					if (status != null && (status.equalsIgnoreCase("checked") || status.equalsIgnoreCase("fixed_checked"))) {
						String permission = permissionCell.getStringCellValue()
						permissions.add(permission)
					}
				} else {
					//negative scenario
					if (status != null && (status.equalsIgnoreCase("unchecked") || status.equalsIgnoreCase("fixed_unchecked"))) {
						String permission = permissionCell.getStringCellValue()
						permissions.add(permission)
					}
				}
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("Error reading permissions by status: " + e.getMessage())
		} finally {
			if (workbook != null) workbook.close()
			if (fis != null) fis.close()
		}
		return permissions
	}

	/**
	 * Create a data submission
	 */
	@Keyword
	public static void createDataSubmission() {
		TestRunner.clickTab('CRDC/DataSubmissions/Create/CreateADataSubmission-Btn')
		WebUI.click(findTestObject('CRDC/DataSubmissions/Create/SubTypeNewUpdate-Btn'))
		WebUI.click(findTestObject('CRDC/DataSubmissions/Create/MetadataOnly-Btn'))

		String automationStudy = 'ATS - AutoTest-Study'
		String normalizedTarget = automationStudy.replaceAll(/\s+/, ' ').trim()
		String studyOptionXpath = "//div[contains(@class,'MuiMenu-paper')]//li[contains(@data-testid,'study-option')]"
		String dataModel = 'GC'

		WebDriver driver = DriverFactory.getWebDriver()

		//Select the Data Commons
		WebUI.click(findTestObject('Object Repository/CRDC/DataSubmissions/Create/DataCommons-Ddn'))
		WebUI.delay(2)
		List<WebElement> commonsDropdownOptions = WebUI.findWebElements(findTestObject('Object Repository/CRDC/DataSubmissions/Create/DataCommonsDdn-Option'), 10)
		WebElement matchDataCommons = commonsDropdownOptions.find {it.getAttribute("innerText")?.trim() == dataModel}
		if (matchDataCommons != null) {
			try {
				new Actions(driver).moveToElement(matchDataCommons).click().perform()
			} catch (Exception e) {
				KeywordUtil.logInfo("Click blocked, retrying after overlay removal...")
				WebUI.executeJavaScript("""document.querySelectorAll('.MuiBackdrop-root, .MuiDialog-container').forEach(e => e.remove())""", null)
				WebUI.delay(1)
				new Actions(driver).moveToElement(matchDataCommons).click().perform()
			}
		} else {
			KeywordUtil.markFailed("Study option "+dataModel+" not found in dropdown.")
		}

		//Select the Study
		WebUI.click(findTestObject('CRDC/DataSubmissions/Create/Study-Ddn'))
		WebDriverWait menuWait = new WebDriverWait(driver, 10)
		menuWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(studyOptionXpath)))

		List<WebElement> studyDropdownOptions = driver.findElements(By.xpath(studyOptionXpath))
		WebElement match = studyDropdownOptions.find {
			String t = it.getAttribute("innerText")
			(t != null ? t.replaceAll(/\s+/, ' ').trim() : "") == normalizedTarget
		}
		if (match == null) {
			KeywordUtil.markFailed("Study option '${automationStudy}' not found in dropdown.")
			return
		}
		KeywordUtil.logInfo("Found the study to create DS: '${automationStudy}'")

		// Re-query and match again right before interacting so the handle is not stale after React updates.
		studyDropdownOptions = driver.findElements(By.xpath(studyOptionXpath))
		match = studyDropdownOptions.find {
			String t = it.getAttribute("innerText")
			(t != null ? t.replaceAll(/\s+/, ' ').trim() : "") == normalizedTarget
		}
		if (match == null) {
			KeywordUtil.markFailed("Study option '${automationStudy}' disappeared before click.")
			return
		}

		new Actions(driver).moveToElement(match).perform()
		WebUI.delay(1)

		try {
			match.click()
		} catch (Exception e) {
			KeywordUtil.logInfo("Native study option click failed: ${e.message}. Retrying with Actions move + click...")
			WebUI.delay(1)
			studyDropdownOptions = driver.findElements(By.xpath(studyOptionXpath))
			match = studyDropdownOptions.find {
				String t = it.getAttribute("innerText")
				(t != null ? t.replaceAll(/\s+/, ' ').trim() : "") == normalizedTarget
			}
			if (match != null) {
				new Actions(driver).moveToElement(match).perform()
				WebUI.delay(0.2)
				new Actions(driver).moveToElement(match).click().perform()
			}
		}

		//Enter the submission name -- setText() is not working, need to use Actions class
		WebUI.click(findTestObject('CRDC/DataSubmissions/Create/SubmissionName-Field'))
		TestObject field = findTestObject('CRDC/DataSubmissions/Create/SubmissionName-Field')
		WebElement el = WebUI.findWebElement(field, 10)
		String timestamp = new Date().format("yyyyMMdd_HHmmss")
		String submissionName = "auto-test_" + timestamp
		KeywordUtil.logInfo("Creating data submission with name " + submissionName)
		Actions actions = new Actions(DriverFactory.getWebDriver())
		actions.moveToElement(el).click().sendKeys(submissionName).perform()

		//Create
		TestRunner.clickTab('CRDC/DataSubmissions/Create/Create-Btn')
		WebUI.delay(3)
		DataSubmissions.expectedSubmissionName = submissionName
		DataSubmissions.captureAndLogDataSubmissionIdFromList(submissionName)
	}

	/**
	 * Upload a metadata file in the data submission via the UI.
	 * Native-clicks Upload only after it is enabled (file must be attached), then waits until
	 * Validate enables (metadata ingest finished) or stops the test.
	 */
	@Keyword
	public static void uploadMetadataUI(String relativePath) {
		String absolutePath = Paths.get(relativePath).toAbsolutePath().toString()
		WebUI.delay(2)
		WebUI.uploadFile(findTestObject('CRDC/DataSubmissions/ChooseFiles-Btn'), absolutePath)
		WebUIUtils.waitUntilEnabledOrStop(findTestObject('CRDC/DataSubmissions/Upload-Btn'), 30, 'Upload')
		WebUI.click(findTestObject('CRDC/DataSubmissions/Upload-Btn'))
		WebUIUtils.waitUntilEnabledOrStop(findTestObject('CRDC/DataSubmissions/Validate-Btn'), 120, 'Validate after metadata upload')
	}

	/**
	 * Upload a unique metadata file (by editing existing file) in the data submission via the UI
	 */
	@Keyword
	static void prepareUniqueMetadataAndUpload(String originalFilePath, String fieldToModify) {
		String absolutePath = RunConfiguration.getProjectDir() + "/" + originalFilePath
		File inputFile = new File(absolutePath)
		if (!inputFile.exists()) {
			KeywordUtil.markFailed("Metadata TSV file not found at path: ${absolutePath}")
			return
		}

		String timestamp = new Date().format("yyyyMMdd_HHmmss")
		File tempFile = new File(absolutePath.replace(".tsv", "_mod_${timestamp}.tsv"))

		// Read header and find column index
		List<String> lines = inputFile.readLines("UTF-8")
		String headerLine = lines[0]
		String[] headers = headerLine.split("\t")
		int fieldIndex = headers.findIndexOf { it == fieldToModify }

		if (fieldIndex == -1) {
			KeywordUtil.markFailed("Field '${fieldToModify}' not found in header row")
			return
		}

		// Write modified file
		tempFile.withWriter("UTF-8") { writer ->
			writer.writeLine(headerLine)
			lines[1..-1].each { line ->
				String[] cols = line.split("\t")
				if (cols.length > fieldIndex) {
					cols[fieldIndex] = cols[fieldIndex] + "_" + timestamp
				}
				writer.writeLine(cols.join("\t"))
			}
		}

		// Upload the modified file. uploadMetadataUI waits until Validate enables, so the temp TSV
		// is not deleted while ingest is still in flight.
		WebUI.delay(2)
		uploadMetadataUI(tempFile.absolutePath)

		if (tempFile.exists()) {
			tempFile.delete()
			KeywordUtil.logInfo("Temporary metadata file deleted: ${tempFile.getAbsolutePath()}")
		}
	}

	/**
	 * Open Profile Request Access and submit Institution AutoTest-Institution plus study AutoTest-Study.
	 * Returns id and name (unique Additional Info token) for NotificationEmailLog.logEmails.
	 * Caller must already be logged in as User. Stops if the dialog does not close after Submit.
	 */
	@Keyword
	static Map submitAccessRequest() {
		String institutionName = 'AutoTest-Institution'
		String studyName = 'AutoTest-Study'
		String additionalInfo = 'TC08 Request Access ' + new SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(new Date())

		// Open Profile Request Access
		WebUI.delay(2)
		SubmissionRequest.clickHome()
		SubmissionRequest.clickAccountDropdown()
		WebUIUtils.clickTab('CRDC/AILocators/Global/Header/Desktop/UserProfile-Link')
		WebUIUtils.clickTab('CRDC/AILocators/AccessRequest/RequestAccess-Btn')
		Utils.waitForElementToAppear('CRDC/AILocators/AccessRequest/Dialog', 10)

		// Enter institution
		WebUIUtils.clickTab('CRDC/AILocators/AccessRequest/Institution-TxtBx')
		WebUIUtils.clearTextfieldByOs('CRDC/AILocators/AccessRequest/Institution-TxtBx')
		WebUI.sendKeys(findTestObject('CRDC/AILocators/AccessRequest/Institution-TxtBx'), institutionName)
		WebUI.delay(1)
		WebElement institutionMatch = Utils.getListWebElements('CRDC/AILocators/AccessRequest/Institution-Ddn-Option').find {
			String t = it.getAttribute("innerText")
			(t != null ? t.replaceAll(/\s+/, ' ').trim() : "") == institutionName
		}
		if (institutionMatch != null) {
			institutionMatch.click()
		} else {
			WebUI.sendKeys(findTestObject('CRDC/AILocators/AccessRequest/Institution-TxtBx'), Keys.chord(Keys.TAB))
		}
		Utils.waitForElementToDisappear('CRDC/AILocators/AccessRequest/Institution-Ddn-Option', 10)

		// Enter study. clickTab JS click reports success but does not open MUI Select; native click is the same pattern as Study-Ddn in createDataSubmission.
		WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/AccessRequest/Studies-Ddn'), 10, FailureHandling.STOP_ON_FAILURE)
		WebUI.click(findTestObject('CRDC/AILocators/AccessRequest/Studies-Ddn'))
		Utils.waitForElementToAppear('CRDC/AILocators/AccessRequest/Studies-Ddn-Option', 10)
		String normalizedStudy = studyName.replaceAll(/\s+/, ' ').trim()
		WebElement studyMatch = Utils.getListWebElements('CRDC/AILocators/AccessRequest/Studies-Ddn-Option').find {
			String t = it.getAttribute("innerText")
			String testId = it.getAttribute("data-testid") ?: ""
			String text = (t != null ? t.replaceAll(/\s+/, ' ').trim() : "")
			text == normalizedStudy || text.contains(normalizedStudy) || testId.contains(normalizedStudy)
		}
		if (studyMatch == null) {
			KeywordUtil.markFailedAndStop("Study option '${studyName}' not found in Request Access dialog.")
			return
		}
		studyMatch.click()
		// Close the multi-select by JS-clicking its invisible MUI backdrop. Escape closes the Request Access dialog. Native clicks on the header, Studies field, and Additional Info are intercepted: the backdrop covers the dialog, and a native click on the backdrop hits the menu at the viewport center.
		WebUIUtils.clickTab('CRDC/AILocators/AccessRequest/Studies-Menu-Backdrop')
		Utils.waitForElementToDisappear('CRDC/AILocators/AccessRequest/Studies-Ddn-Option', 10)

		// Enter additional info. data-testid is on the MUI wrapper; sendKeys needs the inner textarea, same pattern as Institution-TxtBx.
		WebUIUtils.clickTab('CRDC/AILocators/AccessRequest/AdditionalInfo-TxtBx')
		WebUIUtils.clearTextfieldByOs('CRDC/AILocators/AccessRequest/AdditionalInfo-TxtBx')
		WebUI.sendKeys(findTestObject('CRDC/AILocators/AccessRequest/AdditionalInfo-TxtBx'), additionalInfo)

		// Submit
		WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/AccessRequest/Submit-Btn'), 10, FailureHandling.STOP_ON_FAILURE)
		WebUI.click(findTestObject('CRDC/AILocators/AccessRequest/Submit-Btn'))

		// Wait for the dialog to close
		try {
			Utils.waitForElementToDisappear('CRDC/AILocators/AccessRequest/Dialog', 30)
		} catch (AssertionError e) {
			KeywordUtil.markFailedAndStop("Request Access dialog did not close after Submit")
		}
		return [id: additionalInfo, name: additionalInfo]
	}

	/**
	 * Create mappings between permissions and verification actions
	 */
	private static final Map<String, Closure> permissionToCheck = [:]
	static {
		permissionToCheck["submission_request:view"] = {
			KeywordUtil.logInfo("Verifying SR View...")
			try {
				WebUI.delay(1) //Race condition upon login
				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/SubmissionRequest-Tab')
				Utils.waitForElementToDisappear('CRDC/DataSubmissions/Loading-Icon', 30)
				List<WebElement> rows = WebUI.findWebElements(findTestObject("CRDC/SubmissionRequest/SubmReqListTable-Row"), 20)
				if (rows.size() == 1 && rows[0].getText().contains("do not have the appropriate permissions")) {
					KeywordUtil.logInfo("The empty table message is displayed as expected")
					return false
				}
				KeywordUtil.logInfo("Number of rows in SR list found: " + rows.size())
				if (rows.size() > 1) {
					String ts = new SimpleDateFormat('yyyyMMdd-HHmmss').format(new Date())
					KeywordUtil.logInfo("Capturing screenshot: SR list shows data rows instead of permission-denied message (${ts})")
					WebUI.takeScreenshot("PBAC_FAIL_submission_request_view_SR_list_${ts}")
				}
				return rows.size() > 0
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - submission_request:view -> ${e.message}")
				return false
			}
		}
		permissionToCheck["submission_request:create"] = {
			KeywordUtil.logInfo("Verifying SR Create...")
			try {
				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/SubmissionRequest-Tab')
				//return WebUI.verifyElementPresent(findTestObject("CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn"), 5, FailureHandling.OPTIONAL)
				return WebUIUtils.fastElementPresent('CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn')
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - submission_request:create -> ${e.message}")
				return false
			}
		}
		permissionToCheck["submission_request:submit"] = {
			KeywordUtil.logInfo("Verifying SR Submit...")
			try {
				SubmissionRequest.clickHome()
				WebUIUtils.clickTab('CRDC/NavBar/SubmissionRequest-Tab')
				//only run the create SR flow if the Create button is available -- verify the Resume button is displayed in SR list and the Submit button during SR creation
				if (WebUI.verifyElementPresent(findTestObject("CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn"), 5, FailureHandling.OPTIONAL)) {
					boolean srSubmitExists = false, srListResume = false;
					//SR resume flow
					WebUIUtils.clickTab('CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn')
					WebUI.delay(1)
					WebUIUtils.clickTab('CRDC/SubmissionRequest/ReadAndAcceptPopUp-Btn')
					// Save is disabled until the form is dirty; fill PI so Save can be clicked.
					SubmissionRequest.enterPrincipalInvestigatorInfo(2)
					WebUIUtils.clickTab('CRDC/SubmissionRequest/Save-Btn')
					WebUIUtils.clickTab('CRDC/NavBar/SubmissionRequest-Tab')
					// Leaving with incomplete Section A opens the unsaved-changes dialog; Save there to keep the draft.
					if (WebUI.verifyElementPresent(findTestObject('CRDC/AILocators/SubmissionRequests/UnsavedChangesDialog/Save-Btn'), 5, FailureHandling.OPTIONAL)) {
						KeywordUtil.logInfo("Unsaved changes dialog present; clicking Save")
						WebUIUtils.clickTab('CRDC/AILocators/SubmissionRequests/UnsavedChangesDialog/Save-Btn')
						WebUI.waitForElementNotPresent(findTestObject('CRDC/AILocators/SubmissionRequests/UnsavedChangesDialog/Save-Btn'), 10)
					}
					String loggedInUserName = WebUIUtils.getProfileName()
					SubmissionRequestList.applySubmissionRequestsListFilters([
						submitterName: loggedInUserName,
						program: '',
						study: '',
						status: 'In Progress'
					])
					srListResume = WebUI.verifyElementPresent(findTestObject("CRDC/AILocators/SubmissionRequests/List/Resume-Btn"), 5, FailureHandling.OPTIONAL)

					//SR create/submit flow
					WebUIUtils.clickTab('CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn')
					WebUI.delay(1)
					WebUIUtils.clickTab('CRDC/SubmissionRequest/ReadAndAcceptPopUp-Btn')

					SubmissionRequest.enterPrincipalInvestigatorInfo(2)
					SubmissionRequest.enterPrimaryContactInfo(2)
					//CrdcDH.enterAdditionalContactInfo(2)

					WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
					WebUI.click(findTestObject('CRDC/SubmissionRequest/Next-Btn'))

					SubmissionRequest.enterProgramInfo('Other', 6)
					SubmissionRequest.enterStudyInfo(1)
					SubmissionRequest.enterFundingAgencyInfo(1)
					//CrdcDH.enterExistingAndPlannedPublicationsInfo(1)
					//CrdcDH.enterRepositoryInfo('Clinical', 1)

					WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
					WebUI.click(findTestObject('CRDC/SubmissionRequest/Next-Btn'))

					SubmissionRequest.enterDataAccessTypeAndDbGapRegInfo('Controlled', 'Yes')
					SubmissionRequest.enterCancerTypeAndSubjectsInfo('Cholangiocarcinoma', 'Homo', 1)

					WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
					WebUI.click(findTestObject('CRDC/SubmissionRequest/Next-Btn'))

					SubmissionRequest.enterTargetDeliveryAndExpectedPublicationDate()
					SubmissionRequest.selectDataTypes('genomics', 'proteomics')
					SubmissionRequest.selectFileTypes(5, 6, 1, 1)

					WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
					WebUI.click(findTestObject('CRDC/SubmissionRequest/Next-Btn'))

					srSubmitExists = WebUI.verifyElementPresent(findTestObject("CRDC/SubmissionRequest/Submit-Btn"), 5, FailureHandling.OPTIONAL)

					return srListResume && srSubmitExists;
				}
				return false
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - submission_request:submit -> ${e.message}")
				return false
			}
		}
		permissionToCheck["submission_request:review"] = {
			KeywordUtil.logInfo("Verifying SR Review...")
			try {
				//Verify the Review button on SR list page exists and the Approve, Reject, Request Additional Information buttons on SR form review page exists
				boolean srReviewExists = false, srApproveRejectRequestInfoExist = false;
				//Review button flow
				Utils.waitForElementToDisappear('CRDC/DataSubmissions/Loading-Icon', 30)
				WebUI.delay(1) //Race condition upon login
				SubmissionRequest.clickHome()
				clickTab('CRDC/NavBar/SubmissionRequest-Tab')
				SubmissionRequestList.applySubmissionRequestsListFilters([
					submitterName: '',
					program: '',
					study: '',
					status: 'Submitted, In Review'
				])
				srReviewExists = WebUI.verifyElementPresent(findTestObject("CRDC/SubmissionRequest/Review-Btn"), 5, FailureHandling.OPTIONAL)

				//only run if the Review button is available
				if (srReviewExists) {
					//Approve, Reject, Request Additional Information buttons flow
					clickTab('CRDC/SubmissionRequest/Review-Btn')
					clickTab('CRDC/AILocators/SubmissionRequests/Form/Navigation/Review-Btn')
					srApproveRejectRequestInfoExist = WebUI.verifyElementPresent(findTestObject("CRDC/AILocators/SubmissionRequests/Form/Navigation/Approve-Btn"), 5, FailureHandling.OPTIONAL) &&
							WebUI.verifyElementPresent(findTestObject("CRDC/AILocators/SubmissionRequests/Form/Navigation/Reject-Btn"), 5, FailureHandling.OPTIONAL) &&
							WebUI.verifyElementPresent(findTestObject("CRDC/AILocators/SubmissionRequests/Form/Navigation/Inquire-Btn"), 5, FailureHandling.OPTIONAL);
					return srReviewExists && srApproveRejectRequestInfoExist;
				}
				return srReviewExists;
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - submission_request:review -> ${e.message}")
				return false
			}
		}
		permissionToCheck["submission_request:cancel"] = {
			KeywordUtil.logInfo("Verifying SR Cancel...")
			try {
				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/SubmissionRequest-Tab')
				//return WebUI.verifyElementPresent(findTestObject("CRDC/SubmissionRequest/Cancel-Btn"), 5, FailureHandling.OPTIONAL)
				String loggedInUserName = WebUIUtils.getProfileName()
				SubmissionRequestList.applySubmissionRequestsListFilters([
					submitterName: loggedInUserName,
					program: '',
					study: '',
					status: 'In Progress'
				])
				return WebUIUtils.fastElementPresent('CRDC/SubmissionRequest/Cancel-Btn')
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - submission_request:cancel -> ${e.message}")
				return false
			}
		}
		permissionToCheck["submission_request:reopen"] = { String userRole, String scenario ->
			KeywordUtil.logInfo("Verifying SR Reopen...")
			try {
				Utils.waitForElementToDisappear('CRDC/DataSubmissions/Loading-Icon', 30)
				WebUI.delay(1)
				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/SubmissionRequest-Tab')
				WebUI.delay(2)
				SubmissionRequestList.applySubmissionRequestsListFilters([
					submitterName: '',
					program: '',
					study: '',
					status: 'Approved'
				])
				List<WebElement> rows = WebUI.findWebElements(findTestObject('CRDC/SubmissionRequest/SubmReqListTable-Row'), 20)
				boolean noPermissionRow = rows.size() == 1 &&
					rows[0].getText().contains('do not have the appropriate permissions')
				boolean noApprovedRows = rows.isEmpty() || noPermissionRow
				if (scenario.equalsIgnoreCase('positive') && noApprovedRows) {
					KeywordUtil.markFailed(
						'Approved submission required for reopen PBAC check; seed QA with at least one Approved SR visible to this role.')
					return false
				}
				boolean reopenPresent = WebUIUtils.fastElementPresent('CRDC/AILocators/SubmissionRequests/List/Reopen-Btn')
				KeywordUtil.logInfo("SR Reopen list rows: ${rows.size()}, Reopen control present: ${reopenPresent}")
				return reopenPresent
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - submission_request:reopen -> ${e.message}")
				return false
			}
		}
		permissionToCheck["data_submission:view"] = {
			KeywordUtil.logInfo("Verifying DS View...")
			try {
				//Check if data submissions exist on DS list page and if studies exist on Data Explorer list page
				boolean dataSubmissionsExist = true, dataExplorerExist = true;
				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/DataSubmissions-Tab')
				Utils.waitForElementToDisappear('CRDC/DataSubmissions/Loading-Icon', 30)
				List<WebElement> rows = WebUI.findWebElements(findTestObject("CRDC/DataSubmissions/DataSubmListTable-Row"), 20)
				if (rows.size() == 1 && rows[0].getText().contains("do not have the appropriate permissions")) {
					KeywordUtil.logInfo("The Data Submissions empty table message is displayed")
					dataSubmissionsExist = false
				}
				KeywordUtil.logInfo("Number of rows in Data Submission list found: " + rows.size())

				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/DataExplorer-Tab')
				Utils.waitForElementToDisappear('CRDC/DataSubmissions/Loading-Icon', 30)
				List<WebElement> rowsDataExplorer = WebUI.findWebElements(findTestObject("CRDC/DataSubmissions/DataSubmListTable-Row"), 20)
				if (rowsDataExplorer.size() == 1 && rowsDataExplorer[0].getText().contains("do not have the appropriate permissions")) {
					KeywordUtil.logInfo("The Data Explorer empty table message is displayed")
					dataExplorerExist = false
				}
				KeywordUtil.logInfo("Number of rows in Data Explorer list found: " + rowsDataExplorer.size())

				return dataSubmissionsExist && dataExplorerExist && rows.size() > 0 && rowsDataExplorer.size() > 0;
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - data_submission:view -> ${e.message}")
				return false
			}
		}
		permissionToCheck["data_submission:create"] = {
			KeywordUtil.logInfo("Verifying DS Create...")
			try {
				//Check if Data Submission Create button exists and API Token menu exists
				boolean dataSubmissionCreateExist = false, apiTokenExist = false;
				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/DataSubmissions-Tab')
				dataSubmissionCreateExist = WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Create/CreateADataSubmission-Btn"), 5, FailureHandling.OPTIONAL)
				clickTab('CRDC/Login/UserProfile-Dd')
				apiTokenExist = WebUI.verifyElementPresent(findTestObject("CRDC/AILocators/Global/Header/Desktop/APIToken-Btn"), 5, FailureHandling.OPTIONAL)
				SubmissionRequest.clickHome()
				return dataSubmissionCreateExist && apiTokenExist;
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - data_submission:create -> ${e.message}")
				return false
			}
		}
		permissionToCheck["data_submission:cancel"] = {
			KeywordUtil.logInfo("Verifying DS Cancel...")
			try {
				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/DataSubmissions-Tab')
				//only run the create DS flow if the Create button is available
				if (WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Create/CreateADataSubmission-Btn"), 5, FailureHandling.OPTIONAL)) {
					createDataSubmission()
					TestRunner.clickTab('CRDC/DataSubmissions/DataSubmissionName-Link')
					Utils.waitForElementToDisappear('CRDC/DataSubmissions/Loading-Icon', 30)
					return WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Cancel-Btn"), 10, FailureHandling.OPTIONAL)
				}
				return false
			} catch (Exception e) {
				KeywordUtil.markFailed("Error verifying permission - data_submission:cancel -> ${e.message}")
				return false
			}
		}
		permissionToCheck["data_submission:review"] = {
			KeywordUtil.logInfo("Verifying DS Review...")
			try {
				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/DataSubmissions-Tab')
				//only run the DS flow if the Create button is available
				if (WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Create/CreateADataSubmission-Btn"), 5, FailureHandling.OPTIONAL)) {
					//Create DS
					createDataSubmission()

					//Access the DS
					TestRunner.clickTab('CRDC/DataSubmissions/DataSubmissionName-Link')
					Utils.waitForElementToDisappear('CRDC/DataSubmissions/Loading-Icon', 30)

					//Upload metadata file
					prepareUniqueMetadataAndUpload('InputFiles/CRDC/MetadataData/PBAC/program.tsv', "program_acronym")

					//Validate
					WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validate-Btn'), 30)
					TestRunner.clickTab('CRDC/DataSubmissions/Validate-Btn')

					//Submit
					WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
					TestRunner.clickTab('CRDC/DataSubmissions/Submit-Btn')
					WebUI.delay(2) //race condition for Yes button
					TestRunner.clickTab('CRDC/DataSubmissions/SubmitYes-Btn')

					//Verify
					return WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Release-Btn"), 5, FailureHandling.OPTIONAL) &&
							WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Reject-Btn"), 5, FailureHandling.OPTIONAL) &&
							WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Withdraw-Btn"), 5, FailureHandling.OPTIONAL)
				}
				return false
			} catch (Exception e) {
				KeywordUtil.markFailed("Error verifying permission - data_submission:review -> ${e.message}")
				return false
			}
		}
		permissionToCheck["data_submission:admin_submit"] = {
			KeywordUtil.logInfo("Verifying DS Admin Submit...")
			try {
				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/DataSubmissions-Tab')
				//only run the DS flow if the Create button is available
				if (WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Create/CreateADataSubmission-Btn"), 5, FailureHandling.OPTIONAL)) {
					//Create DS
					createDataSubmission()

					//Access the DS
					TestRunner.clickTab('CRDC/DataSubmissions/DataSubmissionName-Link')
					Utils.waitForElementToDisappear('CRDC/DataSubmissions/Loading-Icon', 30)

					//Upload invalid metadata file
					uploadMetadataUI('InputFiles/CRDC/MetadataData/PBAC/program_invalid.tsv')

					//Validate
					WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validate-Btn'), 30)
					TestRunner.clickTab('CRDC/DataSubmissions/Validate-Btn')

					//Verify
					return WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/AdminSubmit-Btn'), 30, FailureHandling.OPTIONAL)
				}
				return false
			} catch (Exception e) {
				KeywordUtil.markFailed("Error verifying permission - data_submission:admin_submit -> ${e.message}")
				return false
			}
		}
		permissionToCheck["data_submission:confirm"] = { String userRole, String scenario ->
			KeywordUtil.logInfo("Verifying DS Confirm...")
			try {
				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/DataSubmissions-Tab')
				//only run the DS flow if the Create button is available
				if (WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Create/CreateADataSubmission-Btn"), 5, FailureHandling.OPTIONAL)) {
					//Create DS
					createDataSubmission()

					//Access the DS
					TestRunner.clickTab('CRDC/DataSubmissions/DataSubmissionName-Link')
					Utils.waitForElementToDisappear('CRDC/DataSubmissions/Loading-Icon', 30)

					//Upload metadata file
					prepareUniqueMetadataAndUpload('InputFiles/CRDC/MetadataData/PBAC/program.tsv', "program_acronym")

					//Validate
					WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validate-Btn'), 30)
					TestRunner.clickTab('CRDC/DataSubmissions/Validate-Btn')

					//Submit
					WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Submit-Btn'), 30)
					TestRunner.clickTab('CRDC/DataSubmissions/Submit-Btn')
					TestRunner.clickTab('CRDC/DataSubmissions/SubmitYes-Btn')

					// If Submitter in negative scenario, verify here
					if (userRole.equalsIgnoreCase("Submitter") && scenario.equalsIgnoreCase("negative")) {
						KeywordUtil.logInfo("DS Confirm test for Submitter in negative scenario — role can create but not confirm - verifying...")
						return WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/CrossValidate-Btn"), 5, FailureHandling.OPTIONAL) ||
								WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Release-Btn"), 5, FailureHandling.OPTIONAL) ||
								WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Complete-Btn"), 5, FailureHandling.OPTIONAL)
					}

					//Cross Validate
					TestRunner.clickTab('CRDC/DataSubmissions/CrossValidate-Btn')

					//Release
					WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Release-Btn'), 30)
					TestRunner.clickTab('CRDC/DataSubmissions/Release-Btn')
					TestRunner.clickTab('CRDC/DataSubmissions/SubmitYes-Btn')

					//Verify
					return WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Complete-Btn"), 5, FailureHandling.OPTIONAL) &&
							WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/Reject-Btn"), 5, FailureHandling.OPTIONAL)
				}
				return false
			} catch (Exception e) {
				KeywordUtil.markFailed("Error verifying permission - data_submission:confirm -> ${e.message}")
				return false
			}
		}
		permissionToCheck["program:manage"] = {
			KeywordUtil.logInfo("Verifying Manage Programs...")
			try {
				SubmissionRequest.clickHome()
				SubmissionRequest.clickAccountDropdown()
				//return WebUI.verifyElementPresent(findTestObject("CRDC/NavBar/ManagePrograms-Btn"), 5, FailureHandling.OPTIONAL)
				return WebUIUtils.fastElementPresent('CRDC/NavBar/ManagePrograms-Btn')
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - program:manage -> ${e.message}")
				return false
			}
		}
		permissionToCheck["study:manage"] = {
			KeywordUtil.logInfo("Verifying Manage Studies...")
			try {
				SubmissionRequest.clickHome()
				SubmissionRequest.clickAccountDropdown()
				//return WebUI.verifyElementPresent(findTestObject("CRDC/NavBar/ManageStudies-Btn"), 5, FailureHandling.OPTIONAL)
				return WebUIUtils.fastElementPresent('CRDC/NavBar/ManageStudies-Btn')
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - study:manage -> ${e.message}")
				return false
			}
		}
		permissionToCheck["institution:manage"] = {
			KeywordUtil.logInfo("Verifying Manage Institutions...")
			try {
				SubmissionRequest.clickHome()
				SubmissionRequest.clickAccountDropdown()
				//return WebUI.verifyElementPresent(findTestObject("CRDC/NavBar/ManageInstitutions-Btn"), 5, FailureHandling.OPTIONAL)
				return WebUIUtils.fastElementPresent('CRDC/NavBar/ManageInstitutions-Btn')
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - institution:manage -> ${e.message}")
				return false
			}
		}
		permissionToCheck["user:manage"] = {
			KeywordUtil.logInfo("Verifying Manage Users...")
			try {
				SubmissionRequest.clickHome()
				SubmissionRequest.clickAccountDropdown()
				//return WebUI.verifyElementPresent(findTestObject("CRDC/NavBar/ManageUsers-Btn"), 5, FailureHandling.OPTIONAL)
				return WebUIUtils.fastElementPresent('CRDC/NavBar/ManageUsers-Btn')
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - user:manage -> ${e.message}")
				return false
			}
		}
		permissionToCheck["dashboard:view"] = {
			KeywordUtil.logInfo("Verifying Operation Dashboard...")
			try {
				SubmissionRequest.clickHome()
				SubmissionRequest.clickAccountDropdown()
				//return WebUI.verifyElementPresent(findTestObject("CRDC/NavBar/OperationDashboard-Btn"), 5, FailureHandling.OPTIONAL)
				return WebUIUtils.fastElementPresent('CRDC/NavBar/OperationDashboard-Btn')
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - dashboard:view -> ${e.message}")
				return false
			}
		}
		permissionToCheck["access:request"] = {
			KeywordUtil.logInfo("Verifying Request Access...")
			try {
				SubmissionRequest.clickHome()
				SubmissionRequest.clickAccountDropdown()
				TestRunner.clickTab('CRDC/Login/UserProfile-Link')
				//return WebUI.verifyElementPresent(findTestObject("CRDC/ManageUsers/RequestAccess-Btn"), 5, FailureHandling.OPTIONAL)
				return WebUIUtils.fastElementPresent('CRDC/ManageUsers/RequestAccess-Btn')
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - access:request -> ${e.message}")
				return false
			}
		}
	}

	/**
	 * Run all the verifications based on positive or negative scenario
	 * @param User role (Fedlead, Dcp, Admin, Submitter, User), scenario (positive, negative)
	 */	
	@Keyword
	public static void verifyPermissionsFunctional(String userRole, String scenario) {
		//For the scenario, get all the relevant permissions to verify for
		List<String> permissionsToCheck = getPermissionsByStatus(userRole, scenario)
		KeywordUtil.logInfo("Permissions to verify for '${userRole}' role for '${scenario}' scenario: ${permissionsToCheck}")

		for (String permission : permissionsToCheck) {
			if (permissionToCheck.containsKey(permission)) {
				try {
					def result
					def closure = permissionToCheck.get(permission)

					if (closure.maximumNumberOfParameters == 2) {
						result = closure.call(userRole, scenario)
					} else {
						result = closure.call()
					}
					//boolean result = permissionToCheck.get(permission).call()
					if (scenario.equalsIgnoreCase("positive") && result) {
						KeywordUtil.markPassed("[PASS] '${permission}' accessible as expected.")
					} else if (scenario.equalsIgnoreCase("negative") && !result) {
						KeywordUtil.markPassed("[PASS] '${permission}' correctly *not* accessible.")
					} else {
						KeywordUtil.markFailed("[FAIL] Unexpected result for permission '${permission}' in '${scenario}' scenario.")
					}
				} catch (Exception e) {
					KeywordUtil.markFailed("[ERROR] Exception during permission '${permission}': ${e.message}")
				}
			} else {
				KeywordUtil.logInfo(
					"Skipping '${permission}' — email/notification permission (see NotificationEmails suite).")
			}
		}
	}

	/**
	 * CRDCDH-502: Data Commons field on Edit User is only for Data Commons Personnel.
	 * Caller must already be on Manage Users as Admin. Never Saves.
	 * Opens pbac.Dcp-automation for the full field checks (present, enabled, asterisk, multi-select).
	 * Then opens pbac.Submitter-automation and cycles Role through User / Submitter / Federal Lead /
	 * Admin (field hidden) and Data Commons Personnel (field appears), then Cancel.
	 */
	@Keyword
	public static void verifyDCPFields() {
		findAndEditUserByName('Dcp')
		try {
			assertDataCommonsFieldVisible("Edit Dcp: Data Commons")
		} finally {
			cancelEditUserWithoutSaving()
		}

		findAndEditUserByName('Submitter')
		try {
			WebUI.waitForElementVisible(findTestObject('CRDC/AILocators/Users/Profile/Cancel-Btn'), 15)
			ROLE_DISPLAY_MAP.each { String roleKey, String roleDisplay ->
				if (roleKey == 'Dcp') {
					return
				}
				clickPbacRoleDropdown()
				TestObject roleOption = findTestObject('CRDC/ManageUsers/RoleDdn-Option', [('role') : roleDisplay])
				WebUI.waitForElementClickable(roleOption, 10)
				WebUI.click(roleOption)
				WebUI.delay(2)
				boolean hiddenPresent = WebUIUtils.fastElementPresent('CRDC/AILocators/Users/Profile/DataCommons-Ddn', 3)
				String hiddenActual = hiddenPresent ? "displayed" : "not present"
				ValidationReport.addValidationReportItem('CRDCDH-502 Data Commons', "Edit Submitter Role=${roleDisplay}: Data Commons hidden", "not present", hiddenActual, hiddenPresent ? "Fail" : "Pass")
				if (hiddenPresent) {
					KeywordUtil.markFailed("Edit Submitter Role=${roleDisplay}: Data Commons hidden: expected 'not present' but was '${hiddenActual}'")
				}
			}

			clickPbacRoleDropdown()
			TestObject dcpOption = findTestObject('CRDC/ManageUsers/RoleDdn-Option', [('role') : ROLE_DISPLAY_MAP['Dcp']])
			WebUI.waitForElementClickable(dcpOption, 10)
			WebUI.click(dcpOption)
			WebUI.delay(2)
			boolean shown = WebUI.waitForElementVisible(findTestObject('CRDC/AILocators/Users/Profile/DataCommons-Ddn'), 10, FailureHandling.OPTIONAL)
			ValidationReport.addValidationReportItem('CRDCDH-502 Data Commons', "Edit Submitter after Role=${ROLE_DISPLAY_MAP['Dcp']}: Data Commons appears", "displayed", shown ? "displayed" : "not present", shown ? "Pass" : "Fail")
			if (!shown) {
				KeywordUtil.markFailed("Edit Submitter after Role=${ROLE_DISPLAY_MAP['Dcp']}: Data Commons appears: expected 'displayed' but was 'not present'")
			}
		} finally {
			cancelEditUserWithoutSaving()
		}
	}

	/**
	 * Leave Edit User via Cancel (clickTab) and wait until the Manage Users table is back
	 * (waitForManageUsersTable). Never clicks Save. No-op if Cancel is not on the page.
	 */
	private static void cancelEditUserWithoutSaving() {
		if (!WebUI.waitForElementPresent(findTestObject('CRDC/AILocators/Users/Profile/Cancel-Btn'), 5, FailureHandling.OPTIONAL)) {
			KeywordUtil.logInfo("cancelEditUserWithoutSaving: Cancel not present; assuming already on Manage Users.")
			return
		}
		WebUIUtils.clickTab('CRDC/AILocators/Users/Profile/Cancel-Btn')
		WebUI.delay(1)
		WebUI.waitForPageLoad(15)
		driver = DriverFactory.getWebDriver()
		waitForManageUsersTable(driver)
	}

	/**
	 * CRDCDH-502 checks for a visible Data Commons field on the current Edit User form:
	 * present, not Mui-disabled, required asterisk shown, and at least two list options.
	 * Opens the MUI select with clickPbacRoleDropdown (same native click as Role), reads
	 * listbox options, then Escape so nothing is selected or saved.
	 * @param labelPrefix ValidationReport label prefix (which user / role state this is)
	 */
	private static void assertDataCommonsFieldVisible(String labelPrefix) {
		WebUI.waitForElementVisible(findTestObject('CRDC/AILocators/Users/Profile/Cancel-Btn'), 15)

		boolean present = WebUI.waitForElementPresent(findTestObject('CRDC/AILocators/Users/Profile/DataCommons-Ddn'), 10, FailureHandling.OPTIONAL)
		ValidationReport.addValidationReportItem('CRDCDH-502 Data Commons', "${labelPrefix} present", "displayed", present ? "displayed" : "not present", present ? "Pass" : "Fail")
		if (!present) {
			KeywordUtil.markFailed("${labelPrefix} present: expected 'displayed' but was 'not present'")
			return
		}

		boolean visible = WebUI.waitForElementVisible(findTestObject('CRDC/AILocators/Users/Profile/DataCommons-Ddn'), 10, FailureHandling.OPTIONAL)
		ValidationReport.addValidationReportItem('CRDCDH-502 Data Commons', "${labelPrefix} visible", "visible", visible ? "visible" : "not visible", visible ? "Pass" : "Fail")
		if (!visible) {
			KeywordUtil.markFailed("${labelPrefix} visible: expected 'visible' but was 'not visible'")
		}

		String cssClass = WebUI.getAttribute(findTestObject('CRDC/AILocators/Users/Profile/DataCommons-Ddn'), 'class') ?: ''
		boolean enabled = !cssClass.contains('Mui-disabled')
		ValidationReport.addValidationReportItem('CRDCDH-502 Data Commons', "${labelPrefix} enabled", "enabled", enabled ? "enabled" : "Mui-disabled", enabled ? "Pass" : "Fail")
		if (!enabled) {
			KeywordUtil.markFailed("${labelPrefix} enabled: expected 'enabled' but was 'Mui-disabled'")
		}

		boolean asteriskVisible = WebUI.waitForElementVisible(findTestObject('CRDC/AILocators/Users/Profile/DataCommons-Asterisk'), 10, FailureHandling.OPTIONAL)
		ValidationReport.addValidationReportItem('CRDCDH-502 Data Commons', "${labelPrefix} required asterisk", "asterisk visible", asteriskVisible ? "asterisk visible" : "asterisk not visible", asteriskVisible ? "Pass" : "Fail")
		if (!asteriskVisible) {
			KeywordUtil.markFailed("${labelPrefix} required asterisk: expected 'asterisk visible' but was 'asterisk not visible'")
		}

		boolean opened = false
		for (int i = 0; i < 2; i++) {
			clickPbacRoleDropdown('CRDC/AILocators/Users/Profile/DataCommons-Ddn')
			if (WebUI.waitForElementPresent(findTestObject('CRDC/AILocators/Users/Profile/DataCommons-Ddn-Option'), 5, FailureHandling.OPTIONAL)) {
				opened = true
				break
			}
		}
		if (!opened) {
			ValidationReport.addValidationReportItem('CRDCDH-502 Data Commons', "${labelPrefix} multi-select options", "at least 2 options", "dropdown did not open", "Fail")
			KeywordUtil.markFailed("${labelPrefix} multi-select options: expected 'at least 2 options' but was 'dropdown did not open'")
			return
		}
		try {
			List<WebElement> options = WebUI.findWebElements(findTestObject('CRDC/AILocators/Users/Profile/DataCommons-Ddn-Option'), 10)
			List<String> names = options.collect { it.getText()?.trim() }.findAll { it }
			boolean multi = names.size() >= 2
			ValidationReport.addValidationReportItem('CRDCDH-502 Data Commons', "${labelPrefix} multi-select options", "at least 2 options", "${names.size()} options: ${names}", multi ? "Pass" : "Fail")
			if (!multi) {
				KeywordUtil.markFailed("${labelPrefix} multi-select options: expected 'at least 2 options' but was '${names.size()} options: ${names}'")
			}
		} finally {
			try {
				new Actions(DriverFactory.getWebDriver()).sendKeys(Keys.ESCAPE).perform()
			} catch (Exception e) {
				KeywordUtil.logInfo("Escape to close Data Commons list: ${e.message}")
			}
		}
	}
}