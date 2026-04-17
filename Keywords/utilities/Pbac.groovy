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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
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
import java.time.Duration
import utilities.Utils


class Pbac extends TestRunner {

	public static WebDriver driver

	//*************** Input functions start here ****************

	/**
	 * Find a user in the Manage Users table by name
	 * @param User role Fedlead, Dcp, Admin, Submitter, User
	 */
	@Keyword
	public static void findAndEditUserByName(String userRole) {
		String userNameAutomation = "pbac." + userRole + "-automation"
		boolean userFound = false
		WebUI.delay(2)

		while (!userFound) {
			driver = DriverFactory.getWebDriver()
			WebDriverWait wait = new WebDriverWait(driver, 10)
			wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//table//tr[td]")))

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
	 * Write the expected PBAC permissions for the role to Output Excel sheet
	 * @param User role (Fedlead, Dcp, Admin, Submitter, User), path of Katalon Data File
	 */
	@Keyword
	def static void writePbacExpectedPermissionsForRole(String userRole, String dataFilePath) {
		def testData = TestDataFactory.findTestData(dataFilePath)
		int rowCount = testData.getRowNumbers()

		String outputPath = RunConfiguration.getProjectDir() + "/OutputFiles/PBAC_Defaults_Results.xlsx"
		File file = new File(outputPath)
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
		// Map displayed roles in dropdown to internal names
		Map<String, String> roleDisplayMap = [
			'User'     : 'User',
			'Submitter': 'Submitter',
			'Dcp'      : 'Data Commons Personnel',
			'Fedlead'  : 'Federal Lead',
			'Admin'    : 'Admin'
		]

		String roleDisplay = roleDisplayMap[role]
		if (!roleDisplay) {
			KeywordUtil.markFailed("Invalid role passed to resetPermissions: ${role}")
			return
		}

		// Click role dropdown
		WebUI.click(findTestObject('CRDC/ManageUsers/Role-Ddn'))
		KeywordUtil.logInfo("Clicked role dropdown")

		// Find an alternate role to toggle to
		String alternateRole = roleDisplayMap.values().find { it != roleDisplay }
		if (!alternateRole) {
			KeywordUtil.markFailed("Could not find alternate role to switch to.")
			return
		}

		// Select alternate role
		WebUI.click(findTestObject('CRDC/ManageUsers/RoleDdn-Option', [('role') : alternateRole]))
		KeywordUtil.logInfo("Selected alternate role: ${alternateRole}")
		WebUI.delay(2)

		// Open dropdown again
		WebUI.click(findTestObject('CRDC/ManageUsers/Role-Ddn'))
		KeywordUtil.logInfo("Clicked role dropdown again")
		WebUI.delay(2)

		// Re-select target role
		WebUI.click(findTestObject('CRDC/ManageUsers/RoleDdn-Option', [('role') : roleDisplay]))
		KeywordUtil.logInfo("Selected target role: ${roleDisplay}")
		WebUI.delay(2)

		// Click Save
		WebUI.click(findTestObject('CRDC/ManageUsers/Save-Btn'))
		KeywordUtil.logInfo("Clicked Save")

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
		List<WebElement> checkboxes = WebUI.findWebElements(findTestObject('CRDC/ManageUsers/PbacOptions-Chkbx'), 10)
		for (WebElement cb : checkboxes) {
			if (cb.isEnabled() && !cb.isSelected()) {
				cb.click()
				KeywordUtil.logInfo("Checked permission checkbox")
			}
		}
	}

	/**
	 * Uncheck all enabled permission checkboxes on the Edit User page
	 */
	@Keyword
	public static void uncheckAllEnabledPermissionCheckboxes() {
		List<WebElement> checkboxes = WebUI.findWebElements(findTestObject('CRDC/ManageUsers/PbacOptions-Chkbx'), 10)
		for (WebElement cb : checkboxes) {
			if (cb.isEnabled() && cb.isSelected()) {
				cb.click()
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
		findAndEditUserByName(userRole)

		//Expand the Permissions panel and Email Notifications panel
		clickTab('CRDC/ManageUsers/PermissionsPanel-Ddn')
		clickTab('CRDC/ManageUsers/NotificationsPanel-Ddn')

		if (scenario.equals("positive")) {
			checkAllEnabledPermissionCheckboxes()
			WebUI.delay(2)
			WebUI.click(findTestObject('CRDC/ManageUsers/Save-Btn'))
		}
		if (scenario.equals("negative")) {
			uncheckAllEnabledPermissionCheckboxes()
			uncheckAllEnabledPermissionCheckboxes() //do it again to uncheck DCP's SR - View permission because it is disabled until higher permissions are unchecked first
			WebUI.delay(2)
			WebUI.click(findTestObject('CRDC/ManageUsers/Save-Btn'))
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
		WebUI.click(findTestObject('CRDC/DataSubmissions/Create/MetadataOnly-Btn'))
		WebUI.click(findTestObject('CRDC/DataSubmissions/Create/Study-Ddn'))
		WebUI.delay(1)

		//Select the study for automation purposes
		String automationStudy = 'ATS - AutoTest-Study'

		WebDriver driver = DriverFactory.getWebDriver()
		List<WebElement> studyDropdownOptions = driver.findElements(By.xpath("//li[contains(@data-testid,'study-option')]"))
		for (WebElement option : studyDropdownOptions) {
			String studyName = option.getAttribute("innerText")?.trim()
			KeywordUtil.logInfo("Dropdown option is: " + studyName)
		}
		KeywordUtil.logInfo("Number of study options in dropdown: " + studyDropdownOptions.size())

		WebElement match = studyDropdownOptions.find {it.getAttribute("innerText")?.trim() == automationStudy}
		if (match != null) {
			try {
				new Actions(driver).moveToElement(match).click().perform()
			} catch (Exception e) {
				KeywordUtil.logInfo("Click blocked, retrying after overlay removal...")
				WebUI.executeJavaScript("""document.querySelectorAll('.MuiBackdrop-root, .MuiDialog-container').forEach(e => e.remove())""", null)
				WebUI.delay(1)
				new Actions(driver).moveToElement(match).click().perform()
			}
		} else {
			KeywordUtil.markFailed("Study option '${automationStudy}' not found in dropdown.")
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
		WebUI.delay(5)
	}

	/**
	 * Upload a metadata file in the data submission via the UI
	 */
	@Keyword
	public static void uploadMetadataUI(String relativePath) {
		String absolutePath = Paths.get(relativePath).toAbsolutePath().toString()
		WebUI.delay(2)
		WebUI.uploadFile(findTestObject('CRDC/DataSubmissions/ChooseFiles-Btn'), absolutePath)
		WebUI.delay(5)
		TestRunner.clickTab('CRDC/DataSubmissions/Upload-Btn')
		WebUI.delay(10)
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

		// Upload the modified file
		WebUI.delay(2)
		uploadMetadataUI(tempFile.absolutePath)

		// Clean up
		if (tempFile.exists()) {
			tempFile.delete()
			KeywordUtil.logInfo("Temporary metadata file deleted: ${tempFile.getAbsolutePath()}")
		}
	}


	/**
	 * Create mappings between permissions and verification actions
	 */
	private static final Map<String, Closure> permissionToCheck = [:]
	static {
		permissionToCheck["submission_request:view"] = {
			KeywordUtil.logInfo("Verifying SR View...")
			try {
				Utils.waitForElementToDisappear('CRDC/DataSubmissions/Loading-Icon', 30)
				WebUI.delay(1) //Race condition upon login
				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/SubmissionRequest-Tab')
				WebUI.delay(2)
				List<WebElement> rows = WebUI.findWebElements(findTestObject("CRDC/SubmissionRequest/SubmReqListTable-Row"), 20)
				if (rows.size() == 1 && rows[0].getText().contains("do not have the appropriate permissions")) {
					KeywordUtil.logInfo("The empty table message is displayed as expected")
					return false
				}
				KeywordUtil.logInfo("Number of rows in SR list found: " + rows.size())
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
				TestRunner.clickTab('CRDC/NavBar/SubmissionRequest-Tab')
				//only run the create SR flow if the Create button is available -- verify the Resume button is displayed in SR list and the Submit button during SR creation
				if (WebUI.verifyElementPresent(findTestObject("CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn"), 5, FailureHandling.OPTIONAL)) {
					boolean srSubmitExists = false, srListResume = false;
					//SR resume flow
					TestRunner.clickTab('CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn')
					WebUI.delay(1)
					TestRunner.clickTab('CRDC/SubmissionRequest/ReadAndAcceptPopUp-Btn')
					WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
					TestRunner.clickTab('CRDC/NavBar/SubmissionRequest-Tab')
					String loggedInUserName = WebUIUtils.getProfileName()
					SubmissionRequestList.applySubmissionRequestsListFilters([
						submitterName: loggedInUserName,
						program: '',
						study: '',
						status: 'In Progress'
					])
					srListResume = WebUI.verifyElementPresent(findTestObject("CRDC/AILocators/SubmissionRequests/List/Resume-Btn"), 5, FailureHandling.OPTIONAL)

					//SR create/submit flow
					TestRunner.clickTab('CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn')
					WebUI.delay(1)
					TestRunner.clickTab('CRDC/SubmissionRequest/ReadAndAcceptPopUp-Btn')

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
				return WebUIUtils.fastElementPresent('CRDC/SubmissionRequest/Cancel-Btn')
			} catch (Exception e) {
				KeywordUtil.logInfo("Error verifying permission - submission_request:cancel -> ${e.message}")
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
				WebUI.delay(3)
				List<WebElement> rows = WebUI.findWebElements(findTestObject("CRDC/DataSubmissions/DataSubmListTable-Row"), 20)
				if (rows.size() == 1 && rows[0].getText().contains("do not have the appropriate permissions")) {
					KeywordUtil.logInfo("The Data Submissions empty table message is displayed")
					dataSubmissionsExist = false
				}
				KeywordUtil.logInfo("Number of rows in Data Submission list found: " + rows.size())

				SubmissionRequest.clickHome()
				TestRunner.clickTab('CRDC/NavBar/DataExplorer-Tab')
				WebUI.delay(3)
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
					return WebUI.verifyElementPresent(findTestObject("CRDC/DataSubmissions/AdminSubmit-Btn"), 5, FailureHandling.OPTIONAL)
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
				KeywordUtil.logInfo("[FUTURE DEVELOPMENT] No verification method mapped yet for '${permission}' — skipping.")
			}
		}
	}
}