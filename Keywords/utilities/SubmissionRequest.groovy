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
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.StaleElementReferenceException
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
import java.io.File;
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
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Cookie as Cookie
import com.kms.katalon.core.configuration.RunConfiguration;
import java.nio.file.Files;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Sheet;
import org.openqa.selenium.JavascriptExecutor;




class SubmissionRequest extends TestRunner implements Comparator<List<XSSFCell>>{
	public int compare( List<XSSFCell> l1, List<XSSFCell> l2 ){
		return l1.get(0).getStringCellValue().compareTo( l2.get(0).getStringCellValue() )
	}

	public static WebDriver driver

	private static final int VERIFY_STATUS_BAR_DEFAULT_WAIT_SEC = 30

	/** Fixed future date for SR date pickers (Section B planned pub, Section D delivery/publication). Bump when it becomes past. */
	private static final String SR_QUESTIONNAIRE_FUTURE_DATE = '05/12/2036'

	//*************** Input functions start here ****************
	/**
	 * This function creates an instance of webdriver
	 * And navigates user to crdc application
	 * Delegates to utilities.Login; kept for backward compatibility.
	 */
	@Keyword
	public static void navigateToCrdc() {
		(new Login()).navigateToCrdc()
		driver = DriverFactory.getWebDriver()
	}

	/**
	 * This function gets the system's current date
	 * @param format i.e MM/DD/YYYY
	 * @return formated date
	 */
	public static String getCurrentDate(String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		Date currentDate = new Date();
		String formattedDate = dateFormat.format(currentDate);
		return formattedDate;
	}

	private static void enterSectionBPlannedPublicationExpectedDate(String objPath) {
		TestObject pubDateField = findTestObject(objPath + 'ExpectedPubDate-Clndr')
		WebUI.waitForElementPresent(pubDateField, 10)
		WebUI.click(pubDateField)
		WebUI.setText(pubDateField, clearText() + SR_QUESTIONNAIRE_FUTURE_DATE)
		String actual = WebUI.getAttribute(pubDateField, 'value')?.trim()
		if (!actual) {
			actual = WebUI.getText(pubDateField)?.trim()
		}
		KeywordUtil.logInfo(
			"Planned publication Expected Publication Date — intended: ${SR_QUESTIONNAIRE_FUTURE_DATE}, field value: '${actual}'")
		if (actual != SR_QUESTIONNAIRE_FUTURE_DATE) {
			KeywordUtil.markFailed(
				"Planned publication Expected Publication Date is '${actual}'; expected '${SR_QUESTIONNAIRE_FUTURE_DATE}'")
		}
	}

	/**
	 * This function enters text to any text box 
	 * @param eleObj
	 * @param fPath
	 * @param colNam
	 * @param rowNum
	 */
	@Keyword
	static void setText(TestObject eleObj, String fPath, String colNam, int rowNum) {
		TestData testData = TestDataFactory.findTestData(fPath)
		String data = testData.getValue(colNam, rowNum)
		WebUI.setText(eleObj, data)
		//WebUI.setText(findTestObject(ePath+'EstimDtaSiz-RowOne-Txtbx'), findTestData(fPath).getValue('estimtd-data-size', dataSizeRN));
		//setText(findTestObject(ePath+'AdditionalComnt-TxtBx'), fPath, 'addit-comnt',1)
	}


	/**
	 * This function validates CRDC Submission Request status bar
	 * @param expStatus Status to be validated e.g. NEW, IN PROGRESS, SUBMITTED
	 */
	@Keyword
	public static void verifyStatusBar(String expStatus) {
		verifyStatusBar(expStatus, VERIFY_STATUS_BAR_DEFAULT_WAIT_SEC)
	}

	/**
	 * @param expStatus Expected status label (case-insensitive)
	 * @param statusWaitSeconds Max time to wait for the status text to match after Save/Next etc.
	 */
	@Keyword
	public static void verifyStatusBar(String expStatus, int statusWaitSeconds) {
		expStatus = expStatus.trim().toUpperCase()
		WebUI.delay(5)
		TestObject statusTo = findTestObject('CRDC/SubmissionRequest/Status-Bar/StatusBar-Stutus')
		WebUI.waitForElementPresent(statusTo, 10)
		long deadline = System.currentTimeMillis() + (statusWaitSeconds * 1000L)
		while (System.currentTimeMillis() < deadline) {
			try {
				if (WebUI.getText(statusTo).trim().toUpperCase() == expStatus) {
					break
				}
			} catch (Exception e) {
				// element not ready or stale; keep polling
			}
			Thread.sleep(400)
		}
		String actualStatus = WebUI.getText(statusTo).trim().toUpperCase()
		System.out.println("Actual Status is: " + actualStatus + "\nExpected Status is: " + expStatus)
		if (actualStatus != expStatus) {
			KeywordUtil.markFailed("Status bar did not show '${expStatus}' within ${statusWaitSeconds}s (last seen: '${actualStatus}')")
			return
		}
		WebUI.verifyMatch(actualStatus, expStatus, false)
	}


	/**
	 * This function clears text from a text field
	 * @return String
	 */
	public static String clearText() {
		return Keys.chord(Keys.CONTROL, "a") + Keys.DELETE;
	}

	/** Add the path to the web element**/
	public static String ePath=null;

	/** Add the path to the data file**/
	public static String fPath=null;


	/**
	 * This function enters Principal Investigator information into submission request form
	 * @param dataFileRowNum Please add data file row number to be read
	 */
	@Keyword
	public static void enterPrincipalInvestigatorInfo(int dataRowNum) {
		String objPath = 'CRDC/SubmissionRequest/Section-A/';
		String dataFile = 'CRDC/SubmissionRequest/section-a';
		TestData testData = findTestData(dataFile);

		// Define mappings of field object names to their data keys
		Map<String, String> fields = [
			'PI_FirstName-TxtBx'       : 'pi-first-name',
			'PI_LastName-Txtbx'        : 'pi-last-name',
			'PI_Position-Txtbx'        : 'position',
			'PI_Email-Txtbx'           : 'pi-email',
			'PI_Institution-Dd'        : 'pi-institution',
			'PI_InstitAddress-Txtbx'   : 'pi-instit-address'
		]
		fields.each { testObjectId, columnName ->
			TestObject to = findTestObject(objPath + testObjectId)
			WebUI.waitForElementPresent(to, 20)

			// Get current value in the field
			String existingValue = WebUI.getAttribute(to, 'value')
			String newValue = testData.getValue(columnName, dataRowNum)

			// Profile auto-fill can pre-populate PI email; Review verification expects test data—always overwrite email.
			if (columnName == 'pi-email') {
				WebUI.scrollToElement(to, 20)
				WebUI.waitForElementClickable(to, 20)
				WebUI.click(to)
				WebUIUtils.clearTextfieldByOs(to)
				WebUI.sendKeys(to, newValue?.toString() ?: "")
				KeywordUtil.logInfo("PI email set from test data (overwrote pre-filled value if any: ${existingValue}): ${newValue}")
			} else if (existingValue?.trim()) {
				KeywordUtil.logInfo("Skipping field ${columnName} because it already has value: ${existingValue}")
			} else {
				WebUI.clearText(to)
				WebUI.setText(to, newValue)
				KeywordUtil.logInfo("Successfully entered: ${newValue} into ${columnName}")
			}
		}
	}

	/**
	 *  This function enters Primary Contact information into submission request form
	 * @param dataFileRowNum Please add data file row number to be read
	 */
	@Keyword
	public static void enterPrimaryContactInfo(int dataRowNum){

		String objPath = 'CRDC/SubmissionRequest/Section-A/';
		String dataFile = 'CRDC/SubmissionRequest/section-a';
		TestData testData = findTestData(dataFile);

		// Define mappings of field object names to their data keys
		Map<String, String> fields = [
			'PC_FirstName-Txtbx'       : 'pc-first-name',
			'PC_LastName-Txtbx'        : 'pc-last-name',
			'PC_Position-Txtbx'        : 'pc-position',
			'PC_Email-Txtbx'           : 'pc-email',
			'PC_Institution-Dd'        : 'pc-institution',
			'PC_Phone-Txtbx'           : 'pc-phone'
		]
		fields.each { testObjectId, columnName ->
			TestObject to = findTestObject(objPath + testObjectId)
			WebUI.waitForElementPresent(to, 20)
			String value = testData.getValue(columnName, dataRowNum)
			WebUI.setText(to, clearText() + value)
			KeywordUtil.logInfo("Successfully entered: " + value +" into "+ columnName);
		}
	}

	/**
	 *  This function enters Additional Contact information into submission request form
	 * @param dataFileRowNum Please add data file row number to be read
	 */
	@Keyword
	public static void enterAdditionalContactInfo(int dataRowNum) {

		String objPath = 'CRDC/SubmissionRequest/Section-A/';
		String dataFile = 'CRDC/SubmissionRequest/section-a';
		TestData testData = findTestData(dataFile);

		WebUI.click(findTestObject(objPath+'AddContact-Btn'))

		// Define mappings of field object names to their data keys
		Map<String, String> fields = [
			'AC_FirstName-Txtbx'       : 'ac-first-name',
			'AC_LastName-Txtbx'        : 'ac-last-name',
			'AC_Position-Txtbx'        : 'ac-position',
			'AC_Email-Txtbx'           : 'ac-email',
			'AC_Institution-Dd'        : 'ac-institution',
			'AC_Phone-Txtbx'           : 'ac-phone'
		]
		fields.each { testObjectId, columnName ->
			TestObject to = findTestObject(objPath + testObjectId)
			WebUI.waitForElementPresent(to, 20)
			String value = testData.getValue(columnName, dataRowNum)
			WebUI.setText(to, clearText() + value)
			KeywordUtil.logInfo("Successfully entered: " + value +" into "+ columnName+" field.");
		}
	}

	//SECTION B STARTS HERE
	/**
	 * This function validates program fields on Program and Study page (section B)
	 * It is intended to be used in the class. Don't directly call it in the test-case
	 * @param rowNumber
	 */
	public static void validateProgramFields(int rowNumber) {

		ePath = "CRDC/SubmissionRequest/Section-B/";
		fPath = "CRDC/SubmissionRequest/Section-B/program-study";
		String actual=null;
		String expctd=null;
		Thread.sleep(1000)
		actual = WebUI.getAttribute(findTestObject(ePath+'ProgramTitle-TxtBx'), 'value')
		expctd = findTestData(fPath).getValue('program-title', rowNumber);
		System.out.println("Actual Program Title is: " + actual +"\nExpected Program Title is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getAttribute(findTestObject(ePath+'ProgAbbre-Txtbx'), 'value')
		expctd = findTestData(fPath).getValue('prog-abbreviation', rowNumber);
		System.out.println("Actual Program Abbreviation is: " + actual +"\nExpected Program Abbreviation is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getAttribute(findTestObject(ePath+'ProgDescrptn-Txtbx'), 'value')
		expctd = findTestData(fPath).getValue('prog-description', rowNumber);
		System.out.println("Actual Program Description is: " + actual +"\nExpected Program Description is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)
	}

	public static String programTimeStamp;

	/**
	 * The function enters Program info into the submission request form
	 * @param dataFileRowNum Please add data file row number to be read
	 */
	@Keyword
	public static void enterProgramInfo(String ddValueToSelect, int dataRowNum) {

		programTimeStamp=getCurrentDate("M-d-yyyy-HH-mm");


		ePath = "CRDC/SubmissionRequest/Section-B/";
		fPath = "CRDC/SubmissionRequest/section-b";
		Thread.sleep(1000);
		WebUI.click(findTestObject(ePath+'Program-Dd'))
		Thread.sleep(500);
		GlobalVariable.CrdcUiElement=ddValueToSelect;
		WebUI.scrollToElement(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'), 10)
		WebUI.click(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'))

		if(ddValueToSelect.contains("Other")) {
			String programTitleBase = findTestData(fPath).getValue('program-title', dataRowNum)
			programTitleBase = (programTitleBase != null) ? programTitleBase.toString() : ""
			lastEnteredProgramName = programTitleBase + programTimeStamp
			WebUI.setText(findTestObject(ePath+'ProgramTitle-TxtBx'), lastEnteredProgramName);
			WebUI.setText(findTestObject(ePath+'ProgAbbre-Txtbx'), findTestData(fPath).getValue('prog-abbreviation', dataRowNum)+programTimeStamp);
			WebUI.setText(findTestObject(ePath+'ProgDescrptn-Txtbx'), findTestData(fPath).getValue('prog-description', dataRowNum));
		}else if(ddValueToSelect.contains("CCDI")) {
			lastEnteredProgramName = null

			validateProgramFields(2);
		}else if(ddValueToSelect.contains("CPTAC")) {
			lastEnteredProgramName = null
			validateProgramFields(3);
		}else if(ddValueToSelect.contains("DCCPS")) {
			lastEnteredProgramName = null
			validateProgramFields(4);
		}else if(ddValueToSelect.contains("HTAN")) {
			lastEnteredProgramName = null
			validateProgramFields(5);
		}else {
			KeywordUtil.markFailed("Invalid Drop-down Value! Check enterProgramInfo function")
		}

		System.out.println("Successfully entered program information");
	}





	/**These variables hold date value and reused in verification functions**/
	public static String studyTimeStamp

	/**
	 * Full study title last written by {@link #enterStudyInfo(int)} (base text + {@link #studyTimeStamp}).
	 * Use this when searching the Submission Requests list or calling {@link #rejectSubmissionRequest(String, String)} (second argument optional).
	 * If the table truncates long titles, pass a substring that still appears in the Study column.
	 */
	public static String lastEnteredStudyTitle

	/**
	 * Full study abbreviation last written by {@link #enterStudyInfo(int)} (base text + {@link #studyTimeStamp}).
	 * Pass with {@link #lastEnteredStudyTitle} into {@link #enterOldStudyInfo(String, String)} to replay a prior submission without rebuilding strings in the test.
	 */
	public static String lastEnteredStudyAbbreviation

	/** Section-b row last passed to {@link #enterStudyInfo(int)}; used by {@link #enterOldStudyInfo(String, String)} for description. */
	public static int lastEnteredStudyInfoDataRow

	/**
	 * Program title for an "Other" program last entered via {@link #enterProgramInfo(String, int)}:
	 * {@code program-title} from test data for that row + {@link #programTimeStamp}.
	 * Cleared when a preset program (CCDI, CPTAC, etc.) is selected instead of Other.
	 */
	public static String lastEnteredProgramName

	/**
	 * Re-sync {@link #lastEnteredProgramName} from test data + {@link #programTimeStamp} (e.g. Manage Programs after Other program).
	 * Not required when {@link #enterProgramInfo(String, int)} with Other already ran in the same test.
	 *
	 * @param programDataRowArg Row in {@code CRDC/SubmissionRequest/section-b} (accepts int, {@link Integer}, or numeric {@link String} from Katalon)
	 */
	@Keyword
	public static void recordLastEnteredProgramNameForOtherProgramEntry(Object programDataRowArg) {
		int programDataRow = parseTestDataRowIndex(programDataRowArg, 'recordLastEnteredProgramNameForOtherProgramEntry')
		TestData testData = findTestData('CRDC/SubmissionRequest/section-b')
		String base = testData.getValue('program-title', programDataRow)
		base = (base != null) ? base.toString() : ""
		String ts = (programTimeStamp != null) ? programTimeStamp : ""
		lastEnteredProgramName = base + ts
		KeywordUtil.logInfo("recordLastEnteredProgramNameForOtherProgramEntry: lastEnteredProgramName=" + lastEnteredProgramName)
	}

	/**
	 * This function enters study information into submission request form
	 * @param dataRowNum Row number from the test data file
	 */
	@Keyword
	public static void enterStudyInfo(int dataRowNum) {

		lastEnteredStudyInfoDataRow = dataRowNum
		String objPath = 'CRDC/SubmissionRequest/Section-B/';
		TestData testData = findTestData('CRDC/SubmissionRequest/section-b')

		// Timestamp used to make study title and abbreviation unique
		studyTimeStamp = Utils.getCurrentDate("M-d-yyyy-HH-mm")

		String studyTitleValue = testData.getValue('study-title', dataRowNum) + studyTimeStamp
		lastEnteredStudyTitle = studyTitleValue
		String abbrevBase = testData.getValue('study-abbreviation', dataRowNum)
		lastEnteredStudyAbbreviation = (abbrevBase != null ? abbrevBase.toString() : '') + studyTimeStamp

		// Define mappings of field object names to their data keys
		Map<String, String> fields = [
			'StudyTitle-Txtbx'       : studyTitleValue,
			'StudyAbbre-Txtbx'       : lastEnteredStudyAbbreviation,
			'StudyDescription-Txtbx' : testData.getValue('study-description', dataRowNum)
		]

		fields.each { testObjectId, value ->
			TestObject to = findTestObject(objPath + testObjectId)
			WebUI.waitForElementPresent(to, 20)
			WebUI.setText(to, clearText() + value)
			KeywordUtil.logInfo("Successfully entered: " + value +" into the field.");
		}
	}

	/**
	 * Re-enters Section B study title and abbreviation from a prior submission; study description is filled the same way as
	 * {@link #enterStudyInfo(int)} using {@code study-description} from {@code CRDC/SubmissionRequest/section-b} at
	 * {@link #lastEnteredStudyInfoDataRow} (set on the last {@code enterStudyInfo} call, typically the prior submission’s row;
	 * falls back to row {@code 1} if unset).
	 *
	 * @param priorStudyName Study title exactly as previously submitted (typically {@link #lastEnteredStudyTitle})
	 * @param priorStudyAbbreviation Study abbreviation exactly as previously submitted (typically {@link #lastEnteredStudyAbbreviation})
	 */
	@Keyword
	public static void enterOldStudyInfo(String priorStudyName, String priorStudyAbbreviation) {
		String objPath = 'CRDC/SubmissionRequest/Section-B/'
		TestData td = findTestData('CRDC/SubmissionRequest/section-b')
		int descRow = lastEnteredStudyInfoDataRow > 0 ? lastEnteredStudyInfoDataRow : 1
		String studyDescription = td.getValue('study-description', descRow)
		if (studyDescription == null) {
			studyDescription = ''
		}

		Map<String, String> fields = [
			'StudyTitle-Txtbx'       : priorStudyName != null ? priorStudyName : '',
			'StudyAbbre-Txtbx'       : priorStudyAbbreviation != null ? priorStudyAbbreviation : '',
			'StudyDescription-Txtbx' : studyDescription.toString()
		]
		fields.each { testObjectId, value ->
			TestObject to = findTestObject(objPath + testObjectId)
			WebUI.waitForElementPresent(to, 20)
			WebUI.setText(to, clearText() + value)
			KeywordUtil.logInfo("Successfully entered: " + value + " into " + testObjectId)
		}
	}

	/**
	 * This function enters funding agency information into the submission request form.
	 * @param dataRowNum Row number from the test data file
	 */
	@Keyword
	public static void enterFundingAgencyInfo(int dataRowNum) {

		String objPath = 'CRDC/SubmissionRequest/Section-B/';
		String dataFile = 'CRDC/SubmissionRequest/section-b';
		TestData testData = findTestData(dataFile);

		// Define mappings of TestObject IDs to column names
		Map<String, String> fields = [
			'FundingAgency-Dd'                     : 'funding-agency',
			'GrantContractNumber-Txtbx'            : 'grant-number',
			'NCIProgramOfficer-Txtbx'              : 'nci-prog-officer',
			// below has been removed from UI
			//	'NciGenProgAdministrator-Txtbx'        : 'nci-genomic-prog-admin'
		]

		fields.each { testObjectId, columnName ->
			TestObject to = findTestObject(objPath + testObjectId)
			WebUI.waitForElementPresent(to, 20)
			String value = testData.getValue(columnName, dataRowNum)
			WebUI.setText(to, clearText() + value)
			KeywordUtil.logInfo("Successfully entered: " + value +" into "+ columnName+" field.");
		}
	}


	/**
	 * This function clears text from a text field
	 * @return String
	 */
	public static void clearTxt(TestObject obj) {
		WebUI.sendKeys(obj,  Keys.chord(Keys.CONTROL, "a") + Keys.DELETE);
	}

	/**
	 * This function enters Existing and Planned Publications info into the submission request form.
	 * @param dataRowNum Row number to read from the test data file
	 */
	@Keyword
	public static void enterExistingAndPlannedPublicationsInfo(int dataRowNum) {

		String objPath = 'CRDC/SubmissionRequest/Section-B/';
		String dataFile = 'CRDC/SubmissionRequest/section-b';
		TestData testData = findTestData(dataFile);

		// Add Existing Publication
		WebUI.scrollToElement(findTestObject(objPath + 'AddExistingPublication-Btn'), 20)
		WebUI.click(findTestObject(objPath + 'AddExistingPublication-Btn'))
		Thread.sleep(1000)

		Map<String, String> existingPubFields = [
			'PublicationTitle-TxtBx' : 'publication-title',
			'PubMedID-Txtbx'         : 'pubmed-id',
			'DOI-Txtbx'              : 'doi'
		]

		existingPubFields.each { testObjectId, columnName ->
			TestObject to = findTestObject(objPath + testObjectId)
			WebUI.waitForElementPresent(to, 10)
			String value = testData.getValue(columnName, dataRowNum)
			WebUI.setText(to, clearText() + value)
			KeywordUtil.logInfo("Successfully entered: " + value +" into "+ columnName+" field.");
		}

		// Add Planned Publication
		WebUI.click(findTestObject(objPath + 'AddPlannedPublication-Btn'))
		Thread.sleep(500)
		WebUI.scrollToElement(findTestObject(objPath + 'PlannedPublicationTitle-Txtbx'), 3)

		WebUI.setText(
				findTestObject(objPath + 'PlannedPublicationTitle-Txtbx'),
				clearText() + testData.getValue('pland-publictn-title', dataRowNum)
				)

		enterSectionBPlannedPublicationExpectedDate(objPath)
		KeywordUtil.logInfo("Successfully entered planned publication")
	}

	/**
	 * Enters Section B repository row: name, study id; if other-data-types is non-empty,
	 * selects "Other" in Data Type(s) Submitted (MUI listbox) and fills Other Data Type(s).
	 * If that column is empty, selects dataTypeWhenNoOtherText (default Clinical) only.
	 * @param dataRowNum Row in CRDC/SubmissionRequest/section-b test data
	 */
	@Keyword
	public static void enterRepositoryInfo(int dataRowNum) {
		enterRepositoryInfo(dataRowNum, 'Clinical')
	}

	/**
	 * @param dataRowNum Row in test data
	 * @param dataTypeWhenNoOtherText Visible menu label when other-data-types is blank (e.g. Clinical)
	 * When other-data-types is non-empty, tries listbox option Other first; if absent (older UI), selects Clinical then still fills Other Data Type(s) text.
	 */
	@Keyword
	public static void enterRepositoryInfo(int dataRowNum, String dataTypeWhenNoOtherText) {
		String objPath = 'CRDC/SubmissionRequest/Section-B/'
		TestData testData = findTestData('CRDC/SubmissionRequest/section-b')
		final String OTHER_DATA_TYPE_SUBMITTED_LABEL = 'Other'
		final String FALLBACK_DATA_TYPE_SUBMITTED_LABEL = 'Clinical'

		WebUI.click(findTestObject(objPath + 'AddRepository-Btn'))
		Thread.sleep(1000)

		String otherDtRaw = testData.getValue('other-data-types', dataRowNum)
		String otherDt = (otherDtRaw != null) ? otherDtRaw.trim() : ''

		Map<String, String> fields = [
			'RepositoryName-Txtbx'   : 'repository-name',
			'StudyID-Txtbx'          : 'study-id',
		]

		fields.each { testObjectId, columnName ->
			TestObject to = findTestObject(objPath + testObjectId)
			WebUI.waitForElementPresent(to, 10)
			String value = testData.getValue(columnName, dataRowNum)
			WebUI.setText(to, clearText() + value)
			KeywordUtil.logInfo("Successfully entered: " + value +" into "+ columnName+" field.");
		}

		WebDriver driver = DriverFactory.getWebDriver()

		if (!otherDt.isEmpty()) {
			WebUI.click(findTestObject(objPath + 'DataTypesSubmitd-Dd'))
			Thread.sleep(500)
			TestObject dataTypesMenuOptionOther = new TestObject('SectionB_DataTypesSubmitted_Other')
			dataTypesMenuOptionOther.addProperty('xpath', ConditionType.EQUALS,
					"(//*[@role='listbox'])[last()]//li[normalize-space()='" + OTHER_DATA_TYPE_SUBMITTED_LABEL + "']", true)
			boolean otherOptionVisible = WebUI.waitForElementVisible(dataTypesMenuOptionOther, 10, FailureHandling.OPTIONAL)
			if (otherOptionVisible) {
				WebUI.click(dataTypesMenuOptionOther)
			} else {
				KeywordUtil.logInfo(
						"Data Type(s) Submitted: \"Other\" option not found; selecting \"" + FALLBACK_DATA_TYPE_SUBMITTED_LABEL + "\" for backward compatibility.")
				TestObject dataTypesMenuOptionFallback = new TestObject('SectionB_DataTypesSubmitted_Clinical')
				dataTypesMenuOptionFallback.addProperty('xpath', ConditionType.EQUALS,
						"(//*[@role='listbox'])[last()]//li[normalize-space()='" + FALLBACK_DATA_TYPE_SUBMITTED_LABEL + "']", true)
				WebUI.waitForElementVisible(dataTypesMenuOptionFallback, 10)
				WebUI.click(dataTypesMenuOptionFallback)
			}
			new Actions(driver).sendKeys(Keys.ESCAPE).perform()
			WebUI.delay(1)

			TestObject otherTo = findTestObject(objPath + 'OtherDataTypes-Txtbx')
			WebUI.waitForElementPresent(otherTo, 10)
			WebUI.waitForElementClickable(otherTo, 15)
			WebUI.scrollToElement(otherTo, 10)
			WebElement otherInput = WebUI.findWebElement(otherTo, 10)
			new Actions(driver).moveToElement(otherInput).click().perform()
			otherInput.clear()
			otherInput.sendKeys(otherDt)
			otherInput.sendKeys(Keys.TAB)
			WebUI.delay(1)
			KeywordUtil.logInfo("Successfully entered: " + otherDt +" into other-data-types field.");
		} else {
			WebUI.click(findTestObject(objPath + 'DataTypesSubmitd-Dd'))
			Thread.sleep(500)
			TestObject dataTypesMenuOption = new TestObject('SectionB_DataTypesSubmitted_' + dataTypeWhenNoOtherText)
			dataTypesMenuOption.addProperty('xpath', ConditionType.EQUALS,
					"(//*[@role='listbox'])[last()]//li[normalize-space()='" + dataTypeWhenNoOtherText + "']", true)
			WebUI.waitForElementVisible(dataTypesMenuOption, 10)
			WebUI.click(dataTypesMenuOption)
			new Actions(driver).sendKeys(Keys.ESCAPE).perform()
		}
	}


	/**
	 * This function enters Repository information into the submission request form.
	 * @param dropDownValue The value to select from the Data Types Submitted dropdown
	 * @param dataRowNum Row number to read from the test data file
	 */
	@Keyword
	public static void enterDataAccessTypeAndDbGapRegInfo(String accessType, String dbGapRegistered) {
		String objPath = 'CRDC/SubmissionRequest/Section-C/';
		TestData testData = findTestData('CRDC/SubmissionRequest/section-c');

		if(accessType.equalsIgnoreCase("open")) {
			WebUI.click(findTestObject(objPath+'OpenAccess-ChkBx'));
		}else if(accessType.equalsIgnoreCase("controlled")) {
			WebUI.click(findTestObject(objPath+'ControlledAccess-Chkbx'));
		}else if(accessType.equalsIgnoreCase("both")) {
			WebUI.click(findTestObject(objPath+'OpenAccess-ChkBx'));
			WebUI.click(findTestObject(objPath+'ControlledAccess-Chkbx'));
		}else {
			KeywordUtil.markFailed("Invalid Access Type entered. check enterDataAccessTypeAndDbGapRegInfo() function")
		}

		if(dbGapRegistered.equalsIgnoreCase("yes")) {
			WebUI.click(findTestObject(objPath+'dbGapRegistered-TogleBtn'));
			WebUI.setText(findTestObject(objPath+'dbGapPHSNumber-Txtbx'), "phs005555")
		}else if(dbGapRegistered.equalsIgnoreCase("no")) {
			KeywordUtil.logInfo("Skipping dbGaP PHS field as it is no regitered");
		}else {
			KeywordUtil.markFailed("Invalid response for dbGaP Registration. check enterDataAccessTypeAndDbGapRegInfo() function")
		}
	}


	/**
	 * This function enters Data Access Types and Cancer Types into submission request form
	 * @param dataFileRowNum Please add data file row number to be read
	 */
	@Keyword
	public static void enterCancerTypeAndSubjectsInfo(String cancerType, String speciesOfSub, int dataRowNum) {

		ePath = "CRDC/SubmissionRequest/Section-C/";
		TestData testData = findTestData('CRDC/SubmissionRequest/section-c');

		WebUI.click(findTestObject("CRDC/SubmissionRequest/Section-C/CancerTypes-Dd"))
		GlobalVariable.CrdcUiElement=cancerType;
		WebUI.click(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'))

		GlobalVariable.CrdcUiElement="Bone";
		WebUI.sendKeys(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'), Keys.chord(Keys.TAB))
		WebUI.setText(findTestObject('CRDC/SubmissionRequest/Section-C/PreCancerTypes-Txtbx'), testData.getValue('pre-cancer-type', dataRowNum));

		Thread.sleep(500);
		WebUI.click(findTestObject("CRDC/SubmissionRequest/Section-C/SpeciesOfSubjects-Dd"))
		GlobalVariable.CrdcUiElement=speciesOfSub;
		WebUI.click(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'))
		GlobalVariable.CrdcUiElement="Rattus";
		WebUI.sendKeys(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'), Keys.chord(Keys.TAB))

		WebUI.setText(findTestObject('CRDC/SubmissionRequest/Section-C/NumOfSubjectsIncludInSub-Txtbx'), testData.getValue('num-of-subjects-included', dataRowNum));
		System.out.println("Successfully entered Data Access Types and Cancer Types information");
		WebUI.takeScreenshot("./OutputFiles/BeforeEnteringData0.png")
	}



	/**
	 * Selects today's date for both "Targeted Data Submission Delivery Date" and
	 * "Expected Publication Date" fields in the Submission Request Section-D form.
	 */
	@Keyword
	public static void enterTargetDeliveryAndExpectedPublicationDate() {
		// Do not call TimeZone.setDefault — it mutates JVM-wide TZ and skews getCurrentDate() vs dates entered earlier in the test.

		//String currentDate = new SimpleDateFormat("mm/dd/yyyy").format(new Date())
		Thread.sleep(10000)
		WebUI.takeScreenshot("./OutputFiles/BeforeEnteringData1.png")

		//clearText(findTestObject(ePath+'ExpctdPubliDate-Clndr'))
		//WebUI.setText(findTestObject(ePath+'ExpctdPubliDate-Clndr'), clearText() + getCurrentDate("MM/dd/yyyy"));


		WebUI.takeScreenshot("./OutputFiles/BeforeEnteringData2.png")
		TestObject targetedDeliveryDate = findTestObject('CRDC/SubmissionRequest/Section-D/TragetSubmDelivryDate-TxtBx')
		TestObject expectedPublicationDate = findTestObject('CRDC/SubmissionRequest/Section-D/ExpctdPublicationDate-TxtBx')
		WebUI.clearText(targetedDeliveryDate)
		WebUI.setText(targetedDeliveryDate, SR_QUESTIONNAIRE_FUTURE_DATE)
		WebUI.clearText(expectedPublicationDate)
		WebUI.setText(expectedPublicationDate, SR_QUESTIONNAIRE_FUTURE_DATE)
		//TestObject targetSubDeDate = findTestObject('CRDC/SubmissionRequest/Section-D/TragetSubmDelivryDate-TxtBx');

		//		WebUI.scrollToElement(targetSubDeDate, 10)
		//		WebUI.waitForElementPresent(targetSubDeDate, 10)
		//		WebUI.setText(targetSubDeDate, currentDate, FailureHandling.STOP_ON_FAILURE)
		//		WebUI.takeScreenshot("./OutputFiles/BeforeEnteringData3.png")
		//		TestObject expectedPubData = findTestObject('CRDC/SubmissionRequest/Section-D/ExpctdPublicationDate-TxtBx');
		//		WebUI.scrollToElement(expectedPubData, 10)
		//		WebUI.waitForElementPresent(expectedPubData, 10)
		//		WebUI.setText(expectedPubData, currentDate, FailureHandling.STOP_ON_FAILURE)
	}


	static void selectTodayDate(TestObject calendarButton, TestObject todayDateLocator) {
		// Click on the calendar button to open the date picker
		WebUI.click(calendarButton)
		Thread.sleep(500)
		// Locate and click on today's date
		WebUI.click(todayDateLocator)
	}

	private static final String NOTISTACK_SNACKBAR_LOC = 'CRDC/AILocators/Global/Notistack-Snackbar'
	private static final int NOTISTACK_DISMISS_WAIT_SEC = 20

	/**
	 * Notistack toasts (e.g. after Save) can intercept clicks on Section D fields near the top of the viewport.
	 * Non-fatal if no snackbar was shown or Utils times out.
	 */
	private static void waitForSubmissionRequestNotistackDismissed() {
		try {
			Utils.waitForElementNotVisible(NOTISTACK_SNACKBAR_LOC, NOTISTACK_DISMISS_WAIT_SEC)
		} catch (Throwable t) {
			KeywordUtil.logInfo("waitForSubmissionRequestNotistackDismissed: ${t.message}")
		}
	}

	/**
	 * This function selects Data Types on the submission request form
	 * @param buttonLable Toggle button label to be clicked (enter only one word per toggle button)
	 */
	@Keyword
	public static void selectDataTypes(String... buttonLable) {

		ePath = "CRDC/SubmissionRequest/Section-D/";
		waitForSubmissionRequestNotistackDismissed()
		TestData testData = findTestData('CRDC/SubmissionRequest/section-d');
		Thread.sleep(1000)

		//Verify default is 'No' for all data types
		List elements = WebUI.findWebElements(findTestObject(ePath+'AllSlider-Btns'), 20)
		for (WebElement element : elements) {
			String value = element.getAttribute('class')
			WebUI.verifyMatch(value, 'textChecked', false)
		}

		//Select data type based on the user provided value
		for (String label : buttonLable) {
			label = label.toLowerCase();
			GlobalVariable.CrdcUiElement=label;

			if(label.contains("other")) {
				WebUI.setText(findTestObject(ePath+'OthrDtaTyp-Txtbx'), testData.getValue('other', 1));
			}else if(label.equals("other-clinical")){
				WebUI.setText(findTestObject(ePath+'OthrClinclDtaTyp-Txtbx'), testData.getValue('othr-clinicl', 1));
			}else if(label.contains("imaging")) {
				WebUI.click(findTestObject('CRDC/SubmissionRequest/Toggle-Btn'))
				WebUI.click(findTestObject(ePath+'ConfirmDataIdentified_yes-RdoBtn'))
			}else {
				WebUI.click(findTestObject('CRDC/SubmissionRequest/Toggle-Btn'))
			}

			System.out.println("Successfully selected '"+label+"' data type");
		}
	}



	/**
	 * This function selects File Types of the submission request form
	 * @param  Row Number of the data to be selected/entered
	 */
	@Keyword
	public static void selectFileTypes(int fileTypRN, int fileExtRN, int numOfFileRN, int dataSizeRN) {

		TestData testData = findTestData('CRDC/SubmissionRequest/section-d');
		waitForSubmissionRequestNotistackDismissed()

		WebUI.scrollToElement(findTestObject(ePath+'FileType-RowOne-Dd'), 20)
		WebUI.click(findTestObject(ePath+'FileType-RowOne-Dd'))
		String fileType = testData.getValue('file-type', fileTypRN)
		GlobalVariable.CrdcUiElement = fileType;
		Thread.sleep(500);
		WebUI.click(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'))

		WebUI.click(findTestObject(ePath+'FileExtension-RowOne-Dd'))
		String fileExten = testData.getValue('file-extension', fileExtRN);
		GlobalVariable.CrdcUiElement = fileExten;
		WebUI.click(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'))

		WebUI.setText(findTestObject(ePath+'NumOfFiles-RowOne-Txtbx'), testData.getValue('num-of-file', numOfFileRN));
		WebUI.setText(findTestObject(ePath+'EstimDtaSiz-RowOne-Txtbx'), testData.getValue('estimtd-data-size', dataSizeRN));
		findTestObject('CRDC/SubmissionRequest/Section-D/ConfirmDataIdentified_yes-RdoBtn')

		WebUI.click(findTestObject("CRDC/SubmissionRequest/Section-D/ConfirmDataIdentified_yes-RdoBtn"))

		WebUI.setText(findTestObject(ePath+'AdditionalComnt-TxtBx'), testData.getValue('addit-comnt', 1));
		System.out.println("Successfully selected file type '"+fileType+ "' with extension '"+fileExten+"'");
	}



	//*************** Verification functions start here ****************

	static String actual=null;
	static String expctd=null;

	/**
	 * This function selects File Types of the submission request form
	 * @param  Row Number of the data to be selected/entered
	 */
	@Keyword
	public static void verifyPrincipalInvestigatorInfo(int dataRowNum){
		Thread.sleep(1000)
		ePath = "CRDC/SubmissionRequest/ReviewSubmit/";
		TestData testData = findTestData('CRDC/SubmissionRequest/section-a');


		actual = WebUI.getText(findTestObject(ePath+'PI_Name-Txt'))
		expctd = testData.getValue('pi-last-name', dataRowNum)+", " +testData.getValue('pi-first-name', dataRowNum)
		KeywordUtil.logInfo("Actual PI Name is: " + actual +" Expected PI Name is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'PI_Position-Txt'))
		expctd = testData.getValue('position', dataRowNum)
		KeywordUtil.logInfo("Actual PI Position is: " + actual +" Expected PI Position is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'PI_Email-Txt'))
		expctd = testData.getValue('pi-email', dataRowNum)
		KeywordUtil.logInfo("Actual PI email is: " + actual +" Expected PI email is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'PI_InstitName-Txt'))
		expctd = testData.getValue('pi-institution', dataRowNum)
		KeywordUtil.logInfo("Actual PI institution is: " + actual +" Expected PI institution is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'PI_InstitAddress-Txt'))
		expctd = testData.getValue('pi-instit-address', dataRowNum)
		KeywordUtil.logInfo("Actual PI institution address is: " + actual +" Expected PI institution address is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)
	}

	/**
	 * Verifies Principal Investigator fields on the Section A edit form (not the Review summary) match the
	 * expected {@code CRDC/SubmissionRequest/section-a} test data for the given row—typically used after starting a new
	 * Submission Request when the portal auto-populates PI from the logged-in submitter profile.
	 * <p>
	 * Waits briefly for the first name field to become non-empty so asynchronous profile hydration does not race the check.
	 * Text inputs use {@code value}; institution dropdown uses {@code value} or visible text as returned by the control.
	 * </p>
	 *
	 * @param dataRowNum Row number in {@code section-a} test data (same column keys as {@link #enterPrincipalInvestigatorInfo(int)}).
	 * Accepts {@code int} or {@code Integer} (Katalon often passes boxed {@code Integer} from test scripts).
	 */
	@Keyword
	public static void verifyPrincipalInvestigatorInfoAutoPopulated(Object dataRowNum) {
		int rowNum = parseTestDataRowIndex(dataRowNum, 'verifyPrincipalInvestigatorInfoAutoPopulated')
		String objPath = 'CRDC/SubmissionRequest/Section-A/'
		TestData testData = findTestData('CRDC/SubmissionRequest/section-a')
		TestObject firstNameTo = findTestObject(objPath + 'PI_FirstName-TxtBx')
		WebUI.waitForElementPresent(firstNameTo, 25)
		waitForSectionAFieldPopulated(firstNameTo, 15)

		Map<String, String> fields = [
			'PI_FirstName-TxtBx'     : 'pi-first-name',
			'PI_LastName-Txtbx'      : 'pi-last-name',
			'PI_Position-Txtbx'      : 'position',
			'PI_Email-Txtbx'         : 'pi-email',
			'PI_Institution-Dd'      : 'pi-institution',
			'PI_InstitAddress-Txtbx' : 'pi-instit-address'
		]
		fields.each { String testObjectId, String columnName ->
			TestObject to = findTestObject(objPath + testObjectId)
			WebUI.waitForElementPresent(to, 15)
			String expected = testData.getValue(columnName, rowNum)?.trim()
			if (expected == null) {
				expected = ''
			}
			String actual = getSectionAFormDisplayedValue(to)
			KeywordUtil.logInfo("verifyPrincipalInvestigatorInfoAutoPopulatedInSectionA: ${columnName} — expected='${expected}', actual='${actual}'")
			WebUI.verifyMatch(actual, expected, false)
		}
		KeywordUtil.logInfo('verifyPrincipalInvestigatorInfoAutoPopulatedInSectionA: all PI fields matched expected test data.')
	}

	/**
	 * Same verification as {@link #verifyPrincipalInvestigatorInfoAutoPopulated(Object)}; alternate name for scripts.
	 */
	@Keyword
	public static void verifyPrincipalInvestigatorInfoAutoPopulatedInSectionA(Object dataRowNum) {
		verifyPrincipalInvestigatorInfoAutoPopulated(dataRowNum)
	}

	/**
	 * Coerces Katalon / Groovy row arguments ({@code Integer}, {@code int}, {@code String}) to a 1-based test data row index.
	 */
	private static int parseTestDataRowIndex(Object raw, String callerLabel) {
		if (raw == null) {
			KeywordUtil.markFailed(callerLabel + ": data row index is null.")
			return 1
		}
		if (raw instanceof Number) {
			return ((Number) raw).intValue()
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim())
		} catch (NumberFormatException e) {
			KeywordUtil.markFailed(callerLabel + ": invalid data row index: " + raw)
			return 1
		}
	}

	/**
	 * Polls until the given field has non-empty visible content or {@code maxSeconds} elapses (profile auto-fill).
	 */
	private static void waitForSectionAFieldPopulated(TestObject firstFieldTo, int maxSeconds) {
		for (int s = 0; s < maxSeconds; s++) {
			String v = getSectionAFormDisplayedValue(firstFieldTo)
			if (v != null && !v.trim().isEmpty()) {
				return
			}
			WebUI.delay(1)
		}
		KeywordUtil.logInfo('verifyPrincipalInvestigatorInfoAutoPopulatedInSectionA: first name still empty after wait; proceeding with assertions.')
	}

	/**
	 * Reads value for Section A inputs (text, textarea, MUI-style selects): {@code value} first, then visible text.
	 */
	private static String getSectionAFormDisplayedValue(TestObject to) {
		String v = WebUI.getAttribute(to, 'value')
		if (v != null && !v.trim().isEmpty()) {
			return v.trim()
		}
		String t = WebUI.getText(to)
		return t != null ? t.trim() : ''
	}

	/**
	 * This function verifies Primary contact info on Review Page
	 * @param dataFileRowNum Please add data file row number to be read
	 */
	@Keyword
	public static void verifyPrimaryContactInfo(int dataRowNum){

		fPath = "CRDC/SubmissionRequest/Section-A/primary-contact";
		TestData testData = findTestData('CRDC/SubmissionRequest/section-a');

		actual = WebUI.getText(findTestObject(ePath+'PC_Name-Txt'))
		expctd = testData.getValue('pc-last-name', dataRowNum)+", " +testData.getValue('pc-first-name', dataRowNum)
		KeywordUtil.logInfo("Actual Primary Contact Name is: " + actual +" Expected Primary Contact Name is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'PC_Position-Txt'))
		expctd = testData.getValue('pc-position', dataRowNum)
		KeywordUtil.logInfo("Actual Primary Contact Position is: " + actual +" Expected Primary Contact Position is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'PC_Email-Txt'))
		expctd = testData.getValue('pc-email', dataRowNum)
		KeywordUtil.logInfo("Actual Primary Contact email is: " + actual +" Expected Primary Contact email is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'PC_InstitName-Txt'))
		expctd = testData.getValue('pc-institution', dataRowNum)
		KeywordUtil.logInfo("Actual Primary Contact institution is: " + actual +" Expected Primary Contact institution is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'PC_InstitName-Txt'))
		expctd = testData.getValue('pc-institution', dataRowNum)
		KeywordUtil.logInfo("Actual Primary Contact phone is: " + actual +" Expected Primary Contact phone is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)
	}

	/**
	 * This function verifies additional contact info of the submission request form
	 * @param  Row Number of the data to be selected/entered
	 */
	@Keyword
	public static void verifyAdditionalContactInfo(int dataRowNum) {

		TestData testData = findTestData('CRDC/SubmissionRequest/section-a');

		actual = WebUI.getText(findTestObject(ePath+'AC_Name-Txt'))
		expctd = testData.getValue('ac-last-name', dataRowNum)+", " +testData.getValue('ac-first-name', dataRowNum)
		KeywordUtil.logInfo("Actual Aditional Contact Name is: " + actual +" Expected Aditional Contact Name is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'AC_Position-Txt'))
		expctd = testData.getValue('ac-position', dataRowNum)
		KeywordUtil.logInfo("Actual Aditional Contact Position is: " + actual +" Expected Aditional Contact Position is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'AC_Email-Txt'))
		expctd = testData.getValue('ac-email', dataRowNum)
		KeywordUtil.logInfo("Actual Aditional Contact email is: " + actual +" Expected Aditional Contact email is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'AC_InstitName-Txt'))
		expctd = testData.getValue('ac-institution', dataRowNum)
		KeywordUtil.logInfo("Actual Aditional Contact institution is: " + actual +" Expected Aditional Contact institution is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		//Write script to convert the phone number format
		//actual = WebUI.getText(findTestObject(ePath+'AC_Phone-Txt'))
		//expctd = findTestData(fPath).getValue('ac-phone', institRN)
		//System.out.println("Actual Aditional Contact phone is: " + actual +"\nExpected Aditional Contact phone is: "+expctd);
		//WebUI.verifyMatch(actual, expctd, false)
		System.out.println("Successfully verified additional contact information");
	}


	/**
	 * This function verifies program info on review page
	 * @param rowNumber
	 */
	@Keyword
	public static void verifyProgramInfo(int rowNumber) {

		ePath = "CRDC/SubmissionRequest/ReviewSubmit/";
		TestData testData = findTestData("CRDC/SubmissionRequest/section-b");

		actual = WebUI.getText(findTestObject(ePath+'ProgramTitle-Txt'))
		expctd = testData.getValue('program-title', rowNumber)+programTimeStamp;
		KeywordUtil.logInfo("Actual Program Title is: " + actual +" Expected Program Title is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'ProgAbbre-Txt'))
		expctd = testData.getValue('prog-abbreviation', rowNumber)+programTimeStamp;
		KeywordUtil.logInfo("Actual Program Abbreviation is: " + actual +" Expected Program Abbreviation is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'ProgDescrptn-Txt'))
		expctd = testData.getValue('prog-description', rowNumber);
		KeywordUtil.logInfo("Actual Program Description is: " + actual +" Expected Program Description is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)
	}

	/**
	 * This function verifies study information on the review page
	 * @param dataFileRowNum Please add data file row number to be read
	 */
	@Keyword
	public static void verifyStudyInfo(int dataRowNum) {

		TestData testData = findTestData("CRDC/SubmissionRequest/section-b");

		actual = WebUI.getText(findTestObject(ePath+'StudyTitle-Txt'))
		expctd = testData.getValue('study-title', dataRowNum)+ studyTimeStamp
		KeywordUtil.logInfo("Actual study title is: " + actual +" Expected study title is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'StudyAbbre-Txt'))
		expctd = testData.getValue('study-abbreviation', dataRowNum)+studyTimeStamp
		KeywordUtil.logInfo("Actual study abbreviation is: " + actual +" Expected study abbreviation is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'StudyDscrptin-Txt'))
		expctd = testData.getValue('study-description', dataRowNum)
		KeywordUtil.logInfo("Actual study description is: " + actual +" Expected study description is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)
		KeywordUtil.logInfo("Successfully verified Study information");
	}

	/**
	 *  This function verifies Funding Agency and DbGaP information on the review page
	 * @param dataFileRowNum Data file row number to be read
	 */
	@Keyword
	public static void verifyFundingAgencyAndDbGaPInfo(int dataRowNum) {

		TestData testData = findTestData("CRDC/SubmissionRequest/section-b");

		actual = WebUI.getText(findTestObject(ePath+'FndngAgncy-Txt'))
		expctd = testData.getValue('funding-agency', dataRowNum)
		KeywordUtil.logInfo("Actual Funding agency is: " + actual +" Expected Funding agency is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'GrantCntractNum-Txt'))
		expctd = testData.getValue('grant-number', dataRowNum)
		KeywordUtil.logInfo("Actual Grant number is: " + actual +" Expected Grant number is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'NCIProgOficer-Txt'))
		expctd = testData.getValue('nci-prog-officer', dataRowNum)
		KeywordUtil.logInfo("Actual NCI program officer is: " + actual +" Expected NCI program officer is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		//		actual = WebUI.getText(findTestObject(ePath+'NciGenProgAdmstr-Txt'))
		//		expctd = testData.getValue('nci-genomic-prog-admin', dataRowNum)
		//		KeywordUtil.logInfo("Actual NCI program Administrator is: " + actual +" Expected NCI program Administrator is: "+expctd);
		//		WebUI.verifyMatch(actual, expctd, false)

		//		//Add 'HAS YOUR STUDY BEEN REGISTERED IN dbGaP? Yes No verification here
		//		actual = WebUI.getText(findTestObject(ePath+'dbGapPHSNumber-Txt'))
		//		expctd = testData.getValue('dbgap-phs-num', dataRowNum)
		//		KeywordUtil.logInfo("Actual dbGaP PHS number is: " + actual +" Expected dbGaP PHS number is: "+expctd);
		//		WebUI.verifyMatch(actual, expctd, false)
		//		KeywordUtil.logInfo("Successfully verified Funding Agency and dbGaP information");
	}

	/**
	 *  This function verifies Publications information on the review page
	 * @param dataFileRowNum Please add data file row number to be read
	 */
	@Keyword
	public static void verifyPublicationsInfo(int dataRowNum) {

		ePath = "CRDC/SubmissionRequest/ReviewSubmit/"
		TestData testData = findTestData("CRDC/SubmissionRequest/section-b");

		//Need to add a method to verify if publication is added, then verify, otherwise don't verify
		actual = WebUI.getText(findTestObject(ePath+'PublictnTitle-Txt'))
		expctd = testData.getValue('publication-title', dataRowNum)
		KeywordUtil.logInfo("Actual Publications title is: " + actual +" Expected Publications title is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'PubMedID-Txt'))
		expctd = testData.getValue('pubmed-id', dataRowNum)
		KeywordUtil.logInfo("Actual PubMed ID is: " + actual +" Expected PubMed ID is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'DOI-Txt'))
		expctd =  testData.getValue('doi', dataRowNum)
		KeywordUtil.logInfo("Actual PubMed ID is: " + actual +" Expected PubMed ID is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		//Need to add a method to verify if planned publication is added, then verify, otherwise don't verify

		actual = WebUI.getText(findTestObject(ePath+'PlnedPublTitle-Txt'))
		expctd =  testData.getValue('pland-publictn-title', dataRowNum)
		KeywordUtil.logInfo("Actual Planned publication title is: " + actual +" Expected Planned publication title is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'ExpctdPubDate-Txt'))
		expctd = SR_QUESTIONNAIRE_FUTURE_DATE
		KeywordUtil.logInfo("Actual Planned publication date is: " + actual + " Expected Planned publication date is: " + expctd)
		WebUI.verifyMatch(actual, expctd, false)
		KeywordUtil.logInfo("Successfully verified Publications information");
	}

	/**
	 * Verifies Repository information on the review page (name, study id, other data types text).
	 * @param dataRowNum Row in CRDC/SubmissionRequest/section-b test data
	 */
	@Keyword
	public static void verifyRepositoryInfo(int dataRowNum) {

		ePath = "CRDC/SubmissionRequest/ReviewSubmit/";
		TestData testData = findTestData("CRDC/SubmissionRequest/section-b");

		actual = WebUI.getText(findTestObject(ePath+'RepoName-Txt'))
		expctd = testData.getValue('repository-name', dataRowNum)
		KeywordUtil.logInfo("Actual Repository name is: " + actual +" Expected Repository name is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'StudyID-Txt'))
		expctd = testData.getValue('study-id', dataRowNum)
		KeywordUtil.logInfo("Actual Study ID is: " + actual +" Expected Study ID is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'RepositoryOtherDataTypes-Txt'))
		expctd = testData.getValue('other-data-types', dataRowNum)
		KeywordUtil.logInfo("Actual Other data type is: " + actual +"\nExpected Other data type is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)
		System.out.println("Successfully verified Repository information");
	}

	/**
	 * This function verifies Data Access Types and Cancer Types on the review page
	 * @param dataFileRowNum Please add data file row number to be read
	 */
	@Keyword
	public static void verifyDataAccessAndDiseaseInfo(String cancerType, String preCancerType, String speciesOfSub, int dataRowNum) {

		TestData testData = findTestData("CRDC/SubmissionRequest/section-c");

		//WebUI.scrollToElement(findTestObject(ePath+'OtherCancerTypes-Txt'), 3)

		actual = WebUI.getText(findTestObject(ePath+'AccessTypes-Txt'))
		expctd = testData.getValue('access-types', dataRowNum)
		KeywordUtil.logInfo("Actual Access type is: " + actual +" Expected Access type is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'CancerTypes-Txt'))

		//Based on the user input, pull the corresponding value from excel for cancer type
		switch (cancerType.toLowerCase()) {

			case 'adrenocortical':
				expctd = testData.getValue('cancer-type', 1)
				break;
			case 'pheochromocytoma':
				expctd = testData.getValue('cancer-type', 2)
				break;
			case 'cholangiocarcinoma':
				expctd = testData.getValue('cancer-type', 3)
				break;
			case 'bladder':
				expctd = testData.getValue('cancer-type', 4)
				break;
			case 'lymphocytic':
				expctd = testData.getValue('cancer-type', 5)
				break;
			case 'myeloid':
				expctd = testData.getValue('cancer-type', 6)
				break;
			case 'sarcoma':
				expctd = testData.getValue('cancer-type', 7)
				break;
			default:
				KeywordUtil.markFailed("Invalid Cancer Type Drop-Down Value: "+cancerType)
		}

		WebUI.verifyMatch(actual, expctd, false)

		//		actual = WebUI.getText(findTestObject(ePath+'OtherCancerTypes-Txt'))
		//		expctd = findTestData(fPath).getValue('other-cancer-type', otherCancerTyRN)
		//		KeywordUtil.logInfo("Actual Other cancer type is: " + actual +"\nExpected Other cancer type is: "+expctd);
		//		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'PreCancerTypes-Txt'))

		//Based on the user input, pull the corresponding value from excel for pre-cancer type
		switch (preCancerType.toLowerCase()) {

			case 'lung':
				expctd = testData.getValue('pre-cancer-type', 1)
				break;
			case 'breast':
				expctd = testData.getValue('pre-cancer-type', 2)
				break;
			default:
				KeywordUtil.markFailed("Invalid Pre-Cancer Type Drop-Down Value: "+preCancerType)
		}

		WebUI.verifyMatch(actual, expctd, false)

		//		actual = WebUI.getText(findTestObject(ePath+'OtherPreCancerTypes-Txt'))
		//		expctd = findTestData(fPath).getValue('other-pre-cancer-type', otherPreCancerTyRN)
		//		KeywordUtil.logInfo("Actual Other pre-cancer type is: " + actual +"\nExpected Other pre-cancer type is: "+expctd);
		//		WebUI.verifyMatch(actual, expctd, false)

		//Add code for Species Of Subject
		//WebUI.click(findTestObject(ePath+"SpeciesOfSubjects-Dd"))
		//GlobalVariable.CrdcUiElement=speciesOfSub;
		//WebUI.click(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'))
		//GlobalVariable.CrdcUiElement="Rattus";
		//WebUI.sendKeys(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'), Keys.chord(Keys.TAB))

		actual = WebUI.getText(findTestObject(ePath+'SpeciesOfSubject-Txt'))

		//Based on the user input, pulling the corresponding value from excel
		switch (speciesOfSub.toLowerCase()) {

			case 'homo':
				expctd = testData.getValue('species-of-subject', 1)
				break;
			case 'mus':
				expctd = testData.getValue('species-of-subject', 2)
				break;
			case 'canis':
				expctd = testData.getValue('species-of-subject', 3)
				break;
			case 'rattus':
				expctd = testData.getValue('species-of-subject', 4)
				break;
			default:
				KeywordUtil.markFailed("Invalid Species of Subject Drop-Down Value: "+cancerType)
		}

		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'NumOfSubjInclud-Txt'))
		expctd = testData.getValue('num-of-subjects-included', dataRowNum)
		KeywordUtil.logInfo("Actual number of subject included in sub is: " + actual +" Expected number of subject included in sub is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'CellLines-Txt'))
		expctd = testData.getValue('cell-lines', dataRowNum)
		KeywordUtil.logInfo("Actual Cell lines is: " + actual +" Expected Cell lines is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'DataDeIdentified-Txt'))
		expctd = testData.getValue('data-de-identified', dataRowNum)
		KeywordUtil.logInfo("Actual Data De-Identified is: " + actual +" Expected Data De-Identified is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		System.out.println("Successfully verified Data Access Types and Cancer Types");
	}

	/**
	 * This function verifies Data Types of the submission request form review page
	 * @param buttonLable Toggle button label to be clicked (enter only one word per toggle button)
	 */
	@Keyword
	public static void verifyDataTypes(String... buttonLable) {

		TestData testData = findTestData("CRDC/SubmissionRequest/section-d");
		String currentDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date())

		actual = WebUI.getText(findTestObject(ePath+'TragetSubmDelivryDate-Txt'))
		expctd = SR_QUESTIONNAIRE_FUTURE_DATE
		KeywordUtil.logInfo("Actual target data submission delivery date is: " + actual +" Expected target data submission delivery date is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'ExpctdPubliDateSec-D-RevPage-Txt'))
		expctd = SR_QUESTIONNAIRE_FUTURE_DATE
		KeywordUtil.logInfo("Actual Publication date is: " + actual +"\nExpected Publication date is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)
		KeywordUtil.logInfo("Successfully verified Data Types delivery dates");

		//Verifying data type based on the user provided value
		for (String label : buttonLable) {
			label = label.toLowerCase();
			GlobalVariable.CrdcUiElement=label;

			switch (label) {
				case 'clinical':
					actual = WebUI.getText(findTestObject(ePath+'ClinicalTrial-Txt'))
					expctd = testData.getValue('clinical-trial', 2)
					break;

				case 'immunology':
					actual = WebUI.getText(findTestObject(ePath+'Imunology-Txt'))
					expctd = testData.getValue('immunology', 2)
					break;

				case 'genomics':
					actual = WebUI.getText(findTestObject(ePath+'Genomics-Txt'))
					expctd = testData.getValue('genomics', 2)
					break;

				case 'proteomics':
					actual = WebUI.getText(findTestObject(ePath+'Proteomics-Txt'))
					expctd = testData.getValue('proteomics', 2)
					break;

				case 'imaging':
					actual = WebUI.getText(findTestObject(ePath+'Imaging-Txt'))
					expctd = testData.getValue('imaging', 2)
					break;

				case 'epidemiologic':
					actual = WebUI.getText(findTestObject(ePath+'EpidemilogicCohort-Txt'))
					expctd = testData.getValue('epide-cohort', 2)
					break;

				//				case 'other':
				//					actual = WebUI.getText(findTestObject(ePath+'OtherDataTypes-Txt'))
				//					expctd = findTestData(fPath).getValue('other', 1)
				//					break;

				case 'demographic':
					actual = WebUI.getText(findTestObject(ePath+'Demographic-Txt'))
					expctd = testData.getValue('demog-dta', 2)
					break;

				case 'relapse':
					actual = WebUI.getText(findTestObject(ePath+'Relapse-Txt'))
					expctd = testData.getValue('relapse-dta', 2)
					break;

				case 'diagnosis':
					actual = WebUI.getText(findTestObject(ePath+'Diagnosis-Txt'))
					expctd = testData.getValue('diagno-dta', 2)
					break;

				case 'outcome':
					actual = WebUI.getText(findTestObject(ePath+'Outcome-Txt'))
					expctd = testData.getValue('outcome', 2)
					break;

				case 'treatment':
					actual = WebUI.getText(findTestObject(ePath+'Treatment-Txt'))
					expctd = testData.getValue('teatmnt-dta', 2)
					break;

				case 'other-clinical':
					actual = WebUI.getText(findTestObject(ePath+'OthrClinclDtaTyp-Txt'))
					expctd = testData.getValue('othr-clinicl', 1)
					break;

				default:
					WebUI.click(findTestObject('CRDC/SubmissionRequest/Toggle-Btn'))
			}

			KeywordUtil.logInfo("Actual "+label+" data type is: " + actual +" Expected "+label+" data type is: "+expctd);
			WebUI.verifyMatch(actual, expctd, false)
			KeywordUtil.logInfo("Successfully verified '"+label+"' data type");
		}
	}


	/**
	 * This function verifies File Types on review page
	 * @param  Row Number of the data to be verified
	 */
	@Keyword
	public static void verifyFileTypes(int fileTypRN, int fileExtRN, int numOfFileRN, int dataSizeRN) {

		TestData testData = findTestData("CRDC/SubmissionRequest/section-d");

		actual = WebUI.getText(findTestObject(ePath+'FileType-RowOne-Txt'))
		expctd = testData.getValue('file-type', fileTypRN)
		KeywordUtil.logInfo("Actual file type is: " + actual +" Expected file type is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'FileExtension-Txt'))
		expctd = testData.getValue('file-extension', fileExtRN)
		KeywordUtil.logInfo("Actual file extension is: " + actual +" Expected file extension is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'NumOfFiles-RowOne-Txt'))
		expctd = testData.getValue('num-of-file', numOfFileRN)
		KeywordUtil.logInfo("Actual number of files: " + actual +" Expected number of files: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'EstimDtaSiz-RowOne-Txt'))
		expctd =  testData.getValue('estimtd-data-size', dataSizeRN)
		KeywordUtil.logInfo("Actual data size is: " + actual +" Expected data size is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'AdditionalComnt-Txt'))
		expctd = testData.getValue('addit-comnt', 1)
		KeywordUtil.logInfo("Actual comment is: " + actual +" Expected comment is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)
		KeywordUtil.logInfo("Successfully verified File Types");
	}


	/**
	 * Extract the Submission Request application id from the current form URL.
	 * Call this after fillSubmissionRequestAllSections and before clickSubmitButton,
	 * while the browser is still on /submission-request/{appId}/...
	 * @return application UUID string, or null if not found
	 */
	@Keyword
	static String getApplicationIdFromUrl() {
		String url = WebUI.getUrl()
		if (url == null || url.trim().isEmpty()) return null
		java.util.regex.Pattern p = ~/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/
		java.util.regex.Matcher m = p.matcher(url)
		String applicationId = m.find() ? m.group(0) : null
		KeywordUtil.logInfo("Submission Request ID: ${applicationId}")
		return applicationId
	}

	/**
	 * Open the Submission Requests list, click Start a Submission Request, and accept the read/accept dialog
	 * so Section A of a new request is showing.
	 */
	@Keyword
	public static void startSubmissionRequest() {
		new SubmissionRequestList().navigateToSubmissionRequestsList()
		WebUI.waitForElementClickable(findTestObject('CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn'), 10)
		WebUI.click(findTestObject('CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn'))
		WebUI.waitForElementClickable(findTestObject('CRDC/SubmissionRequest/ReadAndAcceptPopUp-Btn'), 10)
		WebUI.click(findTestObject('CRDC/SubmissionRequest/ReadAndAcceptPopUp-Btn'))
	}

	/**
	 * This function clicks on submit and confirm submit buttons
	 */
	@Keyword
	public static void clickSubmitButton() {

		WebUI.click(findTestObject('CRDC/SubmissionRequest/Submit-Btn'))
		Thread.sleep(500)
		WebUI.click(findTestObject('CRDC/SubmissionRequest/PopUpConfirmToSubmit-Btn'))
		Thread.sleep(500)
		WebUI.verifyElementPresent(findTestObject('CRDC/SubmissionRequest/Start_a_SubmissionRequest-Btn'), 10)
	}

	/**
	 * This function clicks Cancel Request, enters a cancel reason, and confirms the cancel
	 */
	@Keyword
	public static void cancelSubmissionRequestButton() {
		WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelRequest-Btn'), 10)
		WebUI.click(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelRequest-Btn'))
		WebUI.waitForElementVisible(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequest-TxtBx'), 10)
		// setText on the MUI reason field does not update react-hook-form, so Confirm stays disabled. Native click then sendKeys fires onChange.
		WebUI.click(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequest-TxtBx'))
		WebUI.sendKeys(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequest-TxtBx'), "Auto Test Cancel")
		WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequestConfirm-Btn'), 10)
		WebUI.click(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequestConfirm-Btn'))
	}

	/**
	 * Restore a Canceled submission request from the Submission Requests list.
	 * After cancel, the form leaves the request; Restore is the list-row icon
	 * (data-testid cancel-restore-application-button). Default list statuses omit Canceled,
	 * so this filters study plus Canceled, then enters a restore reason and confirms.
	 *
	 * @param studyName Study title or search string for the Study filter (minimum 3 characters)
	 */
	@Keyword
	public static void restoreSubmissionRequest(String studyName) {
		if (studyName == null || studyName.trim().isEmpty()) {
			KeywordUtil.markFailed("restoreSubmissionRequest: studyName is required.")
			return
		}
		String token = studyName.trim()
		if (token.length() < 3) {
			KeywordUtil.markFailed("restoreSubmissionRequest: studyName must be at least 3 characters (Submission Requests Study filter).")
			return
		}
		KeywordUtil.logInfo("restoreSubmissionRequest: navigating to Submission Requests list; study token length=" + token.length())
		new SubmissionRequestList().navigateToSubmissionRequestsList()
		SubmissionRequestList.applySubmissionRequestsListFilters([study: token, status: 'Canceled'] as Map<String, String>)
		WebUIUtils.waitUntilEnabledOrStop(findTestObject('CRDC/AILocators/SubmissionRequests/List/Restore-Btn'), 30, 'SR Restore')
		WebUI.click(findTestObject('CRDC/AILocators/SubmissionRequests/List/Restore-Btn'))
		WebUI.waitForElementVisible(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequest-TxtBx'), 10)
		WebUI.click(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequest-TxtBx'))
		WebUI.sendKeys(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequest-TxtBx'), "Auto Test Restore")
		WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequestConfirm-Btn'), 10)
		WebUI.click(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequestConfirm-Btn'))
		KeywordUtil.logInfo("restoreSubmissionRequest: completed for study token.")
	}

	/**
	 * Reopen an Approved submission request from the Submission Requests list.
	 * Reopen is the list-row button (data-testid reopen-application-button). Fedlead default
	 * list statuses omit Approved, so this filters study plus Approved, then confirms the
	 * shared DeleteDialog without changing Owner.
	 *
	 * @param studyName Study title or search string for the Study filter (minimum 3 characters)
	 */
	private static void navigateToApprovedStudyOnList(String studyName) {
		String token = studyName.trim()
		new SubmissionRequestList().navigateToSubmissionRequestsList()
		SubmissionRequestList.applySubmissionRequestsListFilters([study: token, status: 'Approved'] as Map<String, String>)
	}

	/**
	 * Open the Reopen confirmation dialog for an Approved submission (list filter by study).
	 */
	@Keyword
	public static void openReopenDialogForStudy(String studyName) {
		if (studyName == null || studyName.trim().isEmpty()) {
			KeywordUtil.markFailed("openReopenDialogForStudy: studyName is required.")
			return
		}
		if (studyName.trim().length() < 3) {
			KeywordUtil.markFailed("openReopenDialogForStudy: studyName must be at least 3 characters.")
			return
		}
		KeywordUtil.logInfo("openReopenDialogForStudy: study token length=" + studyName.trim().length())
		navigateToApprovedStudyOnList(studyName)
		WebUIUtils.waitUntilEnabledOrStop(findTestObject('CRDC/AILocators/SubmissionRequests/List/Reopen-Btn'), 30, 'SR Reopen')
		WebUI.click(findTestObject('CRDC/AILocators/SubmissionRequests/List/Reopen-Btn'))
		WebUI.waitForElementVisible(findTestObject('CRDC/AILocators/DeleteDialog/Dialog'), 10)
	}

	/**
	 * Verify Reopen Submission Request dialog copy and study/program summary (CRDCDH-3670).
	 * Uses shared DeleteDialog test objects. Program/study abbreviations default to last entered Section B values.
	 */
	@Keyword
	public static void verifyReopenSubmissionRequestDialog(String studyAbbreviation = null, String programAbbreviation = null) {
		String studyTitle = (lastEnteredStudyTitle ?: '').trim()
		String studyAbbr = (studyAbbreviation != null && !studyAbbreviation.trim().isEmpty())
			? studyAbbreviation.trim() : (lastEnteredStudyAbbreviation ?: '')
		String programName = (programAbbreviation != null && !programAbbreviation.trim().isEmpty())
			? programAbbreviation.trim() : (lastEnteredProgramName ?: '')
		boolean ok = true
		String headerText = WebUI.getText(findTestObject('CRDC/AILocators/DeleteDialog/Header-Lbl')).trim()
		if (!headerText.contains('Reopen Submission Request')) {
			KeywordUtil.markFailed("verifyReopenSubmissionRequestDialog: expected header to contain 'Reopen Submission Request', actual '" + headerText + "'")
			ok = false
		}
		String description = WebUI.getText(findTestObject('CRDC/AILocators/DeleteDialog/Description-Lbl'))?.trim() ?: ''
		if (!description.contains('Reopen an Approved Submission Request for Editing')) {
			KeywordUtil.markFailed("verifyReopenSubmissionRequestDialog: expected description to contain 'Reopen an Approved Submission Request for Editing', actual: " + description)
			ok = false
		}
		String studyValue = WebUI.getAttribute(findTestObject('CRDC/AILocators/SubmissionRequests/ReopenDialog/Study-TxtBx'), 'value')?.trim() ?: ''
		if (studyTitle && !studyValue.contains(studyTitle)) {
			KeywordUtil.markFailed("verifyReopenSubmissionRequestDialog: study field value missing title '" + studyTitle + "', actual: " + studyValue)
			ok = false
		}
		if (studyAbbr && !studyValue.contains(studyAbbr)) {
			KeywordUtil.markFailed("verifyReopenSubmissionRequestDialog: study field value missing abbreviation '" + studyAbbr + "', actual: " + studyValue)
			ok = false
		}
		String programValue = WebUI.getAttribute(findTestObject('CRDC/AILocators/SubmissionRequests/ReopenDialog/Program-TxtBx'), 'value')?.trim() ?: ''
		if (programName && !programValue.contains(programName)) {
			KeywordUtil.markFailed("verifyReopenSubmissionRequestDialog: program field value missing '" + programName + "', actual: " + programValue)
			ok = false
		}
		if (ok) {
			ValidationReport.addValidationReportItem('Submission Request Reopen', 'Reopen dialog', 'title, message, study/program fields', 'verified', 'Pass')
		}
	}

	@Keyword
	public static void cancelReopenDialog() {
		WebUIUtils.waitUntilEnabledOrStop(findTestObject('CRDC/AILocators/DeleteDialog/Cancel-Btn'), 30, 'SR Reopen Cancel')
		WebUI.click(findTestObject('CRDC/AILocators/DeleteDialog/Cancel-Btn'))
		Utils.waitForElementToDisappear('CRDC/AILocators/DeleteDialog/Dialog', 30)
	}

	@Keyword
	public static void confirmReopenDialog() {
		WebUIUtils.waitUntilEnabledOrStop(findTestObject('CRDC/AILocators/DeleteDialog/Confirm-Btn'), 30, 'SR Reopen Confirm')
		WebUI.click(findTestObject('CRDC/AILocators/DeleteDialog/Confirm-Btn'))
		Utils.waitForElementToDisappear('CRDC/AILocators/DeleteDialog/Dialog', 30)
		WebUI.delay(2)
	}

	private static final String REOPEN_DIALOG_OWNER_AUTOCOMPLETE = 'CRDC/AILocators/SubmissionRequests/ReopenDialog/Owner-Autocomplete'
	private static final String REOPEN_DIALOG_OWNER_INPT = 'CRDC/AILocators/SubmissionRequests/ReopenDialog/Owner-Inpt'

	/**
	 * Assert the Reopen dialog Owner control is shown (Fed Lead/Admin) or hidden (Submitter owner).
	 * Call after openReopenDialogForStudy while the delete-dialog is open.
	 */
	@Keyword
	public static void verifyReopenOwnerControlVisible(boolean expectedVisible) {
		boolean present = WebUIUtils.fastElementPresent(REOPEN_DIALOG_OWNER_AUTOCOMPLETE)
		String label = 'Reopen dialog Owner field'
		if (present == expectedVisible) {
			ValidationReport.addValidationReportItem('Submission Request Reopen', label, String.valueOf(expectedVisible), String.valueOf(present), 'Pass')
		} else {
			ValidationReport.addValidationReportItem('Submission Request Reopen', label, String.valueOf(expectedVisible), String.valueOf(present), 'Fail')
			KeywordUtil.markFailed(label + ': expected visible=' + expectedVisible + ', actual=' + present)
		}
	}

	/**
	 * Select a new owner on the Reopen dialog (Admin/Fed Lead). Types a PBAC account prefix (e.g. pbac.user-automation), then clicks the matching MUI list option.
	 */
	@Keyword
	public static void selectReopenDialogOwner(String ownerMatchText) {
		if (ownerMatchText == null || ownerMatchText.trim().isEmpty()) {
			KeywordUtil.markFailed('selectReopenDialogOwner: ownerMatchText is required.')
			return
		}
		String token = ownerMatchText.trim()
		String escaped = token.replace("'", "''")
		if (!WebUI.waitForElementPresent(findTestObject(REOPEN_DIALOG_OWNER_INPT), 10)) {
			KeywordUtil.markFailed('selectReopenDialogOwner: Owner field not found in Reopen dialog.')
			return
		}
		WebUIUtils.clickTab(REOPEN_DIALOG_OWNER_INPT)
		WebUI.delay(0.5)
		WebUIUtils.clearTextfieldByOs(REOPEN_DIALOG_OWNER_INPT)
		WebUI.sendKeys(findTestObject(REOPEN_DIALOG_OWNER_INPT), token)
		WebUI.delay(2)
		String valueAfterType = WebUI.getAttribute(findTestObject(REOPEN_DIALOG_OWNER_INPT), 'value')
		KeywordUtil.logInfo('selectReopenDialogOwner: input value after search text: ' + (valueAfterType != null ? valueAfterType : '(null)'))
		TestObject ownerListOption = new TestObject('ReopenDialog_OwnerListOption')
		ownerListOption.addProperty('xpath', ConditionType.EQUALS,
				"//ul[@role='listbox']//li[contains(normalize-space(.), '" + escaped + "')]", true)
		boolean optionReady = WebUI.waitForElementClickable(ownerListOption, 10, FailureHandling.OPTIONAL)
		if (!optionReady) {
			ownerListOption = new TestObject('ReopenDialog_OwnerListOption_Fallback')
			ownerListOption.addProperty('xpath', ConditionType.EQUALS,
					"//li[contains(normalize-space(.), '" + escaped + "')]", true)
			optionReady = WebUI.waitForElementClickable(ownerListOption, 10, FailureHandling.OPTIONAL)
		}
		if (!optionReady) {
			KeywordUtil.markFailed('selectReopenDialogOwner: no list option contains "' + token + '"')
			return
		}
		WebUI.click(ownerListOption)
		WebUI.delay(1)
		KeywordUtil.logInfo('selectReopenDialogOwner: selected option matching ' + token)
	}

	/**
	 * After list filters are applied, assert whether the Reopen list action is visible (any matching row).
	 */
	@Keyword
	public static void assertReopenButtonVisibleForStudy(String studyName, boolean expectedVisible, String statusFilter = 'Approved') {
		if (studyName == null || studyName.trim().length() < 3) {
			KeywordUtil.markFailed("assertReopenButtonVisibleForStudy: studyName must be at least 3 characters.")
			return
		}
		SubmissionRequestList listHelper = new SubmissionRequestList()
		listHelper.navigateToSubmissionRequestsList()
		listHelper.resetSubmissionRequestsFilters()
		Map<String, String> criteria = [study: studyName.trim()] as Map<String, String>
		if (statusFilter != null && !statusFilter.trim().isEmpty()) {
			criteria.status = statusFilter.trim()
		}
		SubmissionRequestList.applySubmissionRequestsListFilters(criteria)
		boolean present = WebUIUtils.fastElementPresent('CRDC/AILocators/SubmissionRequests/List/Reopen-Btn')
		String label = "Reopen visible (study filter, status=" + (statusFilter ?: 'any') + ")"
		if (expectedVisible == present) {
			ValidationReport.addValidationReportItem('Submission Request Reopen', label, String.valueOf(expectedVisible), String.valueOf(present), 'Pass')
		} else {
			ValidationReport.addValidationReportItem('Submission Request Reopen', label, String.valueOf(expectedVisible), String.valueOf(present), 'Fail')
			KeywordUtil.markFailed("assertReopenButtonVisibleForStudy: expected Reopen present=" + expectedVisible + ", actual=" + present)
		}
	}

	/**
	 * After list filters are applied, assert whether Resume or View is visible on the filtered list (owner can open the SRF form).
	 * statusFilter must match Status dropdown labels exactly (e.g. Reopened, In Progress), not status-bar uppercase text.
	 */
	@Keyword
	public static void assertResumeOrViewVisibleForStudy(String studyName, boolean expectedVisible, String statusFilter = null) {
		if (studyName == null || studyName.trim().length() < 3) {
			KeywordUtil.markFailed("assertResumeOrViewVisibleForStudy: studyName must be at least 3 characters.")
			return
		}
		SubmissionRequestList listHelper = new SubmissionRequestList()
		listHelper.navigateToSubmissionRequestsList()
		listHelper.resetSubmissionRequestsFilters()
		Map<String, String> criteria = [study: studyName.trim()] as Map<String, String>
		if (statusFilter != null && !statusFilter.trim().isEmpty()) {
			criteria.status = statusFilter.trim()
		}
		SubmissionRequestList.applySubmissionRequestsListFilters(criteria)
		boolean resume = WebUIUtils.fastElementPresent('CRDC/AILocators/SubmissionRequests/List/Resume-Btn')
		boolean view = WebUIUtils.fastElementPresent('CRDC/AILocators/SubmissionRequests/List/View-Btn')
		boolean present = resume || view
		String label = "Resume or View visible (study filter, status=" + (statusFilter ?: 'any') + ")"
		if (expectedVisible == present) {
			ValidationReport.addValidationReportItem('Submission Request Reopen', label, String.valueOf(expectedVisible), String.valueOf(present), 'Pass')
		} else {
			ValidationReport.addValidationReportItem('Submission Request Reopen', label, String.valueOf(expectedVisible), String.valueOf(present), 'Fail')
			KeywordUtil.markFailed("assertResumeOrViewVisibleForStudy: expected Resume/View present=" + expectedVisible + ", actual=" + present)
		}
	}

	@Keyword
	public static void reopenApprovedSubmissionRequest(String studyName) {
		if (studyName == null || studyName.trim().isEmpty()) {
			KeywordUtil.markFailed("reopenApprovedSubmissionRequest: studyName is required.")
			return
		}
		String token = studyName.trim()
		if (token.length() < 3) {
			KeywordUtil.markFailed("reopenApprovedSubmissionRequest: studyName must be at least 3 characters (Submission Requests Study filter).")
			return
		}
		KeywordUtil.logInfo("reopenApprovedSubmissionRequest: navigating to Submission Requests list; study token length=" + token.length())
		openReopenDialogForStudy(token)
		confirmReopenDialog()
		KeywordUtil.logInfo("reopenApprovedSubmissionRequest: completed for study token.")
	}

	/**
	 * This function logs in to CRDC via Login.gov 2FA / OTP using a dynamic TOTP secret.
	 * Delegates to utilities.Login; kept for backward compatibility.
	 * @param pbacUser Admin, Fedlead, Dcp, Submitter, or User
	 */
	@Keyword
	public static void loginToCrdcOtp(String pbacUser) {
		(new Login()).loginToCrdcOtp(pbacUser)
	}

	/**
	 * This function logs out from CRDC 
	 */
	@Keyword
	public static void logoutFromCrdc() {
		clickTab('CRDC/Login/UserProfile-Dd')
		clickTab('CRDC/Login/Logout-Link')
	}

	/**
	 * This function clicks on the Home icon
	 */
	@Keyword
	public static void clickHome() {
		clickTab('CRDC/NavBar/Home-Btn')
	}

	/**
	 * This function clicks on the account profile dropdown
	 */
	@Keyword
	public static void clickAccountDropdown() {
		clickTab('CRDC/Login/UserProfile-Dd')
	}

	/**
	 * End-to-end completion of a Submission Request: fills required fields for Sections A through D using the
	 * same row-driven test data files as the granular keywords ({@code section-a} … {@code section-d}),
	 * saves and advances after each section, then submits (including the confirmation dialog).
	 * <p>
	 * Prerequisites: caller has already opened a new Submission Request and accepted the read/accept dialog
	 * so that Section A is visible and the status bar shows NEW.
	 * </p>
	 * <p>
	 * This orchestration matches the minimal required path used by {@code TC02_CRDC_Submission_Request-RequiredFields}
	 * (PI + Primary Contact only in A; Program, Study, Funding in B; access + disease/subjects in C;
	 * dates, data types, and file row in D). Optional Section A/B blocks such as additional contact,
	 * publications, or repository are not included—call those keywords separately if your scenario requires them.
	 * </p>
	 *
	 * @param sectionARow Test data row for {@link #enterPrincipalInvestigatorInfo(int)} and {@link #enterPrimaryContactInfo(int)}
	 * @param programDd Program dropdown value passed to {@link #enterProgramInfo(String, int)} (e.g. {@code Other})
	 * @param programDataRow Row index for program fields when {@code Other} is selected
	 * @param studyDataRow Row for {@link #enterStudyInfo(int)}
	 * @param fundingDataRow Row for {@link #enterFundingAgencyInfo(int)}
	 * @param dataAccessType {@code open}, {@code controlled}, or {@code both} for {@link #enterDataAccessTypeAndDbGapRegInfo(String, String)}
	 * @param dbGapRegistered {@code yes} or {@code no}
	 * @param cancerType Cancer type dropdown label for {@link #enterCancerTypeAndSubjectsInfo(String, String, int)}
	 * @param speciesOfSubjects Species dropdown label (e.g. {@code Homo})
	 * @param sectionCDataRow Row for Section C text fields in {@link #enterCancerTypeAndSubjectsInfo(String, String, int)}
	 * @param firstDataType First data-type toggle label for {@link #selectDataTypes(String...)}
	 * @param secondDataType Second data-type toggle label
	 * @param fileTypeRow Row index for file type in {@link #selectFileTypes(int, int, int, int)}
	 * @param fileExtensionRow Row index for file extension
	 * @param numOfFilesRow Row index for number of files
	 * @param estimatedDataSizeRow Row index for estimated data size
	 */
	@Keyword
	public static void fillSubmissionRequestAllSections(
			int sectionARow,
			String programDd,
			int programDataRow,
			int studyDataRow,
			int fundingDataRow,
			String dataAccessType,
			String dbGapRegistered,
			String cancerType,
			String speciesOfSubjects,
			int sectionCDataRow,
			String firstDataType,
			String secondDataType,
			int fileTypeRow,
			int fileExtensionRow,
			int numOfFilesRow,
			int estimatedDataSizeRow
	) {
		KeywordUtil.logInfo('fillCompleteSubmissionRequestAllSectionsAndSubmit: Section A (PI, Primary Contact)')
		enterPrincipalInvestigatorInfo(sectionARow)
		enterPrimaryContactInfo(sectionARow)
		WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
		WebUI.click(findTestObject('CRDC/SubmissionRequest/Next-Btn'))
		verifyStatusBar('IN PROGRESS')

		KeywordUtil.logInfo('fillCompleteSubmissionRequestAllSectionsAndSubmit: Section B (Program, Study, Funding)')
		enterProgramInfo(programDd, programDataRow)
		enterStudyInfo(studyDataRow)
		enterFundingAgencyInfo(fundingDataRow)
		WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
		WebUI.click(findTestObject('CRDC/SubmissionRequest/Next-Btn'))

		KeywordUtil.logInfo('fillCompleteSubmissionRequestAllSectionsAndSubmit: Section C (Access, cancer, subjects)')
		enterDataAccessTypeAndDbGapRegInfo(dataAccessType, dbGapRegistered)
		enterCancerTypeAndSubjectsInfo(cancerType, speciesOfSubjects, sectionCDataRow)
		WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
		WebUI.click(findTestObject('CRDC/SubmissionRequest/Next-Btn'))

		KeywordUtil.logInfo('fillCompleteSubmissionRequestAllSectionsAndSubmit: Section D (dates, data types, files)')
		enterTargetDeliveryAndExpectedPublicationDate()
		selectDataTypes(firstDataType, secondDataType)
		selectFileTypes(fileTypeRow, fileExtensionRow, numOfFilesRow, estimatedDataSizeRow)
		WebUI.click(findTestObject('CRDC/SubmissionRequest/Save-Btn'))
		WebUI.click(findTestObject('CRDC/SubmissionRequest/Next-Btn'))

		KeywordUtil.logInfo('fillCompleteSubmissionRequestAllSections: finished')
	}

	private static final String DEFAULT_REJECTION_COMMENT = "Automated test rejection - CRDC submission request."
	private static final String DEFAULT_APPROVAL_COMMENT = "Automated test approval - CRDC submission request."
	private static final String DEFAULT_INQUIRE_COMMENT = "Automated test inquire - CRDC submission request."
	private static final String SR_FORM_BASE = "CRDC/AILocators/SubmissionRequests/Form"
	/** ValidationReport section for submission-request UI checks (snackbar, etc.); aligns with list section naming. */
	private static final String SECTION_SUBMISSION_REQUEST_UI = "Submission Request"

	/** CRDCDH-3948: Save disabled tooltip when the form has no unsaved changes (full text). */
	public static final String SR_SAVE_DISABLED_TOOLTIP = 'No changes have been made. Please make edits to the form then click save.'
	/** CRDCDH-3948: Substring passed to verifyTooltipOnHover for the Save disabled tooltip. */
	public static final String SR_SAVE_DISABLED_TOOLTIP_SUBSTRING = 'No changes have been made'
	/** CRDCDH-3948 / QA: Import when re-imported Excel matches current form (e.g. TC01 New). */
	public static final String SR_IMPORT_NO_CHANGES_MESSAGE = 'No changes were applied. The imported file matches the current form.'
	/** ImportApplicationButton: parsed Excel has incomplete sections (TC02 uses InputFiles fixture with invalid/incomplete rows). */
	public static final String SR_IMPORT_VALIDATION_ERRORS_MESSAGE = 'Your data has been imported, but some pages contain validation errors. Please review each page and resolve before submitting.'
	/** CRDCDH-3948 TC02: copy of QA export CRDC_Submission_Request_KT (v3.5); incomplete C/D → validation-errors snackbar. */
	public static final String SR_IMPORT_VALIDATION_ERRORS_FIXTURE = 'InputFiles/CRDC/SubmissionRequest/CRDCDH-3948/CRDCDH-3948_TC02_import-with-validation-errors.xlsx'
	/** ImportApplicationButton / ImportDialog when parse or save fails (not used for TC02 happy validation-errors path). */
	public static final String SR_IMPORT_FAILED_MESSAGE = 'Import failed. Your data could not be imported. Please check the file format and template, then try again.'
	/** ImportApplicationButton: all parsed sections Completed after successful import (e.g. TC03/TC04 full form). */
	public static final String SR_IMPORT_ALL_PASSED_VALIDATION_MESSAGE = 'Your data has been imported and all passed validation. You may proceed to Review & Submit.'

	private static final String SR_SAVE_TOOLTIP_TRIGGER = 'CRDC/AILocators/SubmissionRequests/Form/Navigation/Save-Btn-TooltipTrigger'

	/**
	 * CRDCDH-3948 req 2: hover Save tooltip trigger and verify the disabled-save message.
	 */
	@Keyword
	public static void verifySubmissionRequestSaveDisabledTooltip() {
		WebUIUtils.verifyTooltipOnHover(SR_SAVE_TOOLTIP_TRIGGER, SR_SAVE_DISABLED_TOOLTIP, SR_SAVE_DISABLED_TOOLTIP_SUBSTRING)
	}

	/**
	 * Export the current Submission Request form to Excel (sidebar Export Form), wait for download under project Downloads.
	 *
	 * @param timeoutSeconds Max wait for the exported .xlsx file
	 * @return Absolute path to the downloaded file
	 */
	@Keyword
	public static String exportSubmissionRequestFormExcel(int timeoutSeconds = 60) {
		Utils.clearProjectDownloadsFolder()
		WebUIUtils.clickTab('CRDC/AILocators/SubmissionRequests/Form/ExportForm/ExportForm-Btn')
		String path = Utils.waitForFileInProjectDownloadsByPrefix('CRDC_Submission_Request_', '.xlsx', timeoutSeconds)
		if (path == null || path.trim().isEmpty()) {
			KeywordUtil.markFailed('exportSubmissionRequestFormExcel: no exported file under Downloads')
		}
		return path
	}

	/**
	 * Import a Submission Request Excel file via Import Form dialog (upload + confirm).
	 *
	 * @param absolutePath Path to .xlsx (typically from exportSubmissionRequestFormExcel)
	 */
	@Keyword
	public static void importSubmissionRequestExcel(String absolutePath) {
		if (absolutePath == null || absolutePath.trim().isEmpty()) {
			KeywordUtil.markFailed('importSubmissionRequestExcel: absolutePath is required.')
			return
		}
		File importFile = new File(absolutePath.trim())
		if (!importFile.isFile()) {
			KeywordUtil.markFailed("importSubmissionRequestExcel: file not found: ${importFile.getAbsolutePath()}")
			return
		}
		String filePath = importFile.getAbsolutePath()

		WebUIUtils.clickTab('CRDC/AILocators/SubmissionRequests/Form/ImportForm/ImportForm-Btn')
		WebUI.waitForElementPresent(findTestObject('CRDC/AILocators/SubmissionRequests/Form/ImportDialog/Upload-Input'), 15)
		WebUI.uploadFile(findTestObject('CRDC/AILocators/SubmissionRequests/Form/ImportDialog/Upload-Input'), filePath)
		WebUI.delay(2)
		WebUIUtils.clickTab('CRDC/AILocators/SubmissionRequests/Form/ImportDialog/Confirm-Btn')
	}

	/**
	 * Return to the Submission Request questionnaire as the owner via the list (Resume or View on the study row).
	 * Used when the test left the form and must reopen it in edit mode (e.g. CRDCDH-3948 dirty-state checks).
	 * Not used by notification-email helpers (they never reopen the form) or PBAC (list-only reopen visibility).
	 * Distinct from reopenApprovedSubmissionRequest (Fedlead Reopen on Approved rows, stays on list) and from
	 * approve/inquire/reject flows (Fedlead Review on the row).
	 *
	 * @param studyName Study title or search string for the Study filter (minimum 3 characters)
	 * @param listStatus Optional status filter label (e.g. Rejected, Approved). Required when the SR is not in the
	 *        list default status set (New, In Progress, Submitted, In Review, Inquired only).
	 */
	@Keyword
	public static void openSubmissionRequestFormForStudy(String studyName, String listStatus = null) {
		if (studyName == null || studyName.trim().isEmpty()) {
			KeywordUtil.markFailed("openSubmissionRequestFormForStudy: studyName is required.")
			return
		}
		String token = studyName.trim()
		if (token.length() < 3) {
			KeywordUtil.markFailed("openSubmissionRequestFormForStudy: studyName must be at least 3 characters (Submission Requests Study filter).")
			return
		}
		KeywordUtil.logInfo("openSubmissionRequestFormForStudy: list navigation for study token.")
		SubmissionRequestList listHelper = new SubmissionRequestList()
		listHelper.navigateToSubmissionRequestsList()
		listHelper.resetSubmissionRequestsFilters()
		Map<String, String> criteria = [study: token]
		if (listStatus != null && !listStatus.trim().isEmpty()) {
			criteria.status = listStatus.trim()
		}
		SubmissionRequestList.applySubmissionRequestsListFilters(criteria)
		listHelper.verifySubmissionRequestsFilterResults([expectAtLeastRows: 1] as Map<String, Object>)
		clickResumeOrViewForStudyRow(token)
		boolean formReady = WebUI.waitForElementPresent(findTestObject('CRDC/AILocators/SubmissionRequests/StatusBar/Status-Lbl'), 40)
		if (!formReady) {
			KeywordUtil.markFailed('openSubmissionRequestFormForStudy: submission request form did not load (status bar not visible).')
		}
	}

	/**
	 * Federal-lead (or reviewer) flow: open Submission Requests list, filter by study, open Review for the matching row,
	 * open the Review section in the form sidebar (progress bar), then Reject with a comment and confirm.
	 * The list row Review action lands on section A; Approve/Reject appear only after selecting the Review tab.
	 * <p>
	 * Prerequisites: logged in as a user who can review (e.g. Fed Lead); the request is in a reviewable status
	 * (e.g. Submitted). The study filter requires at least three characters—pass a value that appears in the
	 * Study column (use {@link #lastEnteredStudyTitle} after {@link #enterStudyInfo(int)}). If the list truncates the title,
	 * the row text may not include the full string; in that case, when the filter yields exactly one data row, Review is
	 * clicked on that row automatically.
	 * </p>
	 *
	 * @param studyName Study title or substring that matches the list row (must satisfy the Study filter minimum length)
	 * @param rejectionComment Optional reviewer comment; if omitted or blank, a default automated-test message is used
	 */
	@Keyword
	public static void rejectSubmissionRequest(String studyName, String rejectionComment = DEFAULT_REJECTION_COMMENT) {
		if (openSubmissionRequestReview(studyName, "rejectSubmissionRequest") == null) return
			String comment = (rejectionComment != null && !rejectionComment.trim().isEmpty()) ? rejectionComment.trim() : DEFAULT_REJECTION_COMMENT
		confirmReviewDecision(SR_FORM_BASE + "/Navigation/Reject-Btn",
				"CRDC/AILocators/SubmissionRequests/RejectDialog/ReviewComment-TxtBx",
				"CRDC/AILocators/SubmissionRequests/RejectDialog/ConfirmReject-Btn", comment)
		KeywordUtil.logInfo("rejectSubmissionRequest: completed for study token.")
	}

	/**
	 * Federal-lead (or reviewer) flow: same navigation as {@link #rejectSubmissionRequest(String, String)}—list filter by study,
	 * open the row Review action, select the Review section in the form sidebar, then Approve with a comment and confirm.
	 * <p>
	 * Prerequisites match {@link #rejectSubmissionRequest(String, String)} (Fed Lead, reviewable status, study filter rules,
	 * {@link #lastEnteredStudyTitle}, truncated Study column handling via {@link #clickSubmissionRequestsListReviewForStudyRow(String)}).
	 * </p>
	 *
	 * @param studyName Study title or search string for the Study filter (minimum 3 characters)
	 * @param approvalComment Optional reviewer comment for the approve dialog; if omitted or blank, a default automated-test message is used
	 */
	@Keyword
	public static void approveSubmissionRequest(String studyName, String approvalComment = DEFAULT_APPROVAL_COMMENT) {
		if (openSubmissionRequestReview(studyName, "approveSubmissionRequest") == null) return
			String comment = (approvalComment != null && !approvalComment.trim().isEmpty()) ? approvalComment.trim() : DEFAULT_APPROVAL_COMMENT
		confirmReviewDecision(SR_FORM_BASE + "/Navigation/Approve-Btn",
				"CRDC/AILocators/SubmissionRequests/ApproveDialog/ReviewComment-TxtBx",
				"CRDC/AILocators/SubmissionRequests/ApproveDialog/ConfirmApprove-Btn", comment)
		KeywordUtil.logInfo("approveSubmissionRequest: completed for study token.")
	}

	/**
	 * Same navigation as approveSubmissionRequest, then Approve with Require Data Model changes
	 * checked so the backend sends the conditionally-approved mail instead of a clean approve.
	 *
	 * @param studyName Study title or search string for the Study filter (minimum 3 characters)
	 * @param approvalComment Optional reviewer comment; if omitted or blank, the default automated-test message is used
	 */
	@Keyword
	public static void approveSubmissionRequestWithModelChangePending(String studyName, String approvalComment = DEFAULT_APPROVAL_COMMENT) {
		if (openSubmissionRequestReview(studyName, "approveSubmissionRequestWithModelChangePending") == null) return
			String comment = (approvalComment != null && !approvalComment.trim().isEmpty()) ? approvalComment.trim() : DEFAULT_APPROVAL_COMMENT
		String actionBtnPath = SR_FORM_BASE + "/Navigation/Approve-Btn"
		String commentPath = "CRDC/AILocators/SubmissionRequests/ApproveDialog/ReviewComment-TxtBx"
		String confirmPath = "CRDC/AILocators/SubmissionRequests/ApproveDialog/ConfirmApprove-Btn"
		WebUI.scrollToElement(findTestObject(actionBtnPath), 15)
		WebUI.waitForElementVisible(findTestObject(actionBtnPath), 20)
		WebUI.waitForElementClickable(findTestObject(actionBtnPath), 15)
		WebUI.click(findTestObject(actionBtnPath))
		// ApproveDialog/Dialog (approve-form-dialog) is not in the live ReviewFormDialog. Wait on the pending checkbox instead; confirmReviewDecision waits on the comment field.
		WebUI.waitForElementVisible(findTestObject('CRDC/AILocators/SubmissionRequests/ApproveDialog/PendingModelChange-Chkbx'), 10)
		WebUI.check(findTestObject('CRDC/AILocators/SubmissionRequests/ApproveDialog/PendingModelChange-Chkbx'))
		WebUI.waitForElementPresent(findTestObject(commentPath), 10)
		WebUI.waitForElementVisible(findTestObject(commentPath), 10)
		// setText on the MUI review-comment field does not update react-hook-form, so Confirm stays disabled. Native click then sendKeys fires onChange.
		WebUI.click(findTestObject(commentPath))
		WebUI.sendKeys(findTestObject(commentPath), comment)
		WebUI.waitForElementClickable(findTestObject(confirmPath), 10)
		WebUI.click(findTestObject(confirmPath))
		WebUI.delay(2)
		KeywordUtil.logInfo("approveSubmissionRequestWithModelChangePending: completed for study token.")
	}

	/**
	 * Same navigation as approveSubmissionRequest, then Approve with image de-identification
	 * pending checked so the backend sends the image pending-condition mail.
	 *
	 * @param studyName Study title or search string for the Study filter (minimum 3 characters)
	 * @param approvalComment Optional reviewer comment; if omitted or blank, the default automated-test message is used
	 */
	@Keyword
	public static void approveSubmissionRequestWithImageDeidPending(String studyName, String approvalComment = DEFAULT_APPROVAL_COMMENT) {
		if (openSubmissionRequestReview(studyName, "approveSubmissionRequestWithImageDeidPending") == null) return
			String comment = (approvalComment != null && !approvalComment.trim().isEmpty()) ? approvalComment.trim() : DEFAULT_APPROVAL_COMMENT
		String actionBtnPath = SR_FORM_BASE + "/Navigation/Approve-Btn"
		String commentPath = "CRDC/AILocators/SubmissionRequests/ApproveDialog/ReviewComment-TxtBx"
		String confirmPath = "CRDC/AILocators/SubmissionRequests/ApproveDialog/ConfirmApprove-Btn"
		WebUI.scrollToElement(findTestObject(actionBtnPath), 15)
		WebUI.waitForElementVisible(findTestObject(actionBtnPath), 20)
		WebUI.waitForElementClickable(findTestObject(actionBtnPath), 15)
		WebUI.click(findTestObject(actionBtnPath))
		WebUI.waitForElementVisible(findTestObject('CRDC/AILocators/SubmissionRequests/ApproveDialog/PendingImageDeid-Chkbx'), 10)
		WebUI.check(findTestObject('CRDC/AILocators/SubmissionRequests/ApproveDialog/PendingImageDeid-Chkbx'))
		WebUI.waitForElementPresent(findTestObject(commentPath), 10)
		WebUI.waitForElementVisible(findTestObject(commentPath), 10)
		WebUI.click(findTestObject(commentPath))
		WebUI.sendKeys(findTestObject(commentPath), comment)
		WebUI.waitForElementClickable(findTestObject(confirmPath), 10)
		WebUI.click(findTestObject(confirmPath))
		WebUI.delay(2)
		KeywordUtil.logInfo("approveSubmissionRequestWithImageDeidPending: completed for study token.")
	}

	/**
	 * Federal-lead (or reviewer) flow: same navigation as rejectSubmissionRequest — list filter by study,
	 * open the row Review action, select the Review section in the form sidebar, then Request Additional
	 * Information (Inquire) with a comment and confirm.
	 *
	 * @param studyName Study title or search string for the Study filter (minimum 3 characters)
	 * @param inquireComment Optional reviewer comment for the inquire dialog; if omitted or blank, a default automated-test message is used
	 */
	@Keyword
	public static void inquireSubmissionRequest(String studyName, String inquireComment = DEFAULT_INQUIRE_COMMENT) {
		if (openSubmissionRequestReview(studyName, "inquireSubmissionRequest") == null) return
			String comment = (inquireComment != null && !inquireComment.trim().isEmpty()) ? inquireComment.trim() : DEFAULT_INQUIRE_COMMENT
		confirmReviewDecision(SR_FORM_BASE + "/Navigation/Inquire-Btn",
				"CRDC/AILocators/SubmissionRequests/InquireDialog/ReviewComment-TxtBx",
				"CRDC/AILocators/SubmissionRequests/InquireDialog/ConfirmInquire-Btn", comment)
		KeywordUtil.logInfo("inquireSubmissionRequest: completed for study token.")
	}

	/**
	 * Filter the Submission Requests list by study, open Review on the matching row, and select the Review
	 * section in the form sidebar so the decision buttons are showing.
	 * Returns the trimmed study token, or null after markFailed when studyName is blank or shorter than 3 characters.
	 */
	private static String openSubmissionRequestReview(String studyName, String caller) {
		if (studyName == null || studyName.trim().isEmpty()) {
			KeywordUtil.markFailed(caller + ": studyName is required.")
			return null
		}
		String token = studyName.trim()
		if (token.length() < 3) {
			KeywordUtil.markFailed(caller + ": studyName must be at least 3 characters (Submission Requests Study filter).")
			return null
		}

		KeywordUtil.logInfo(caller + ": navigating to Submission Requests list; study token length=" + token.length())
		new SubmissionRequestList().navigateToSubmissionRequestsList()
		SubmissionRequestList.applySubmissionRequestsListFilters([study: token] as Map<String, String>)
		WebUI.delay(2)
		clickSubmissionRequestsListReviewForStudyRow(token)

		// List "Review" opens the request on the first section (e.g. PI); decision buttons exist only on the final Review step.
		WebUI.waitForElementPresent(findTestObject(SR_FORM_BASE + "/Review/ProgressBar-Review"), 45)
		WebUI.waitForElementClickable(findTestObject(SR_FORM_BASE + "/Review/ProgressBar-Review"), 15)
		WebUI.click(findTestObject(SR_FORM_BASE + "/Review/ProgressBar-Review"))
		WebUI.delay(2)
		return token
	}

	/**
	 * Click a review decision button (Reject, Approve, and so on), type the comment in its dialog, and confirm.
	 */
	private static void confirmReviewDecision(String actionBtnPath, String commentPath, String confirmPath, String comment) {
		WebUI.scrollToElement(findTestObject(actionBtnPath), 15)
		WebUI.waitForElementVisible(findTestObject(actionBtnPath), 20)
		WebUI.waitForElementClickable(findTestObject(actionBtnPath), 15)
		WebUI.click(findTestObject(actionBtnPath))
		WebUI.waitForElementPresent(findTestObject(commentPath), 10)
		WebUI.waitForElementVisible(findTestObject(commentPath), 10)
		// setText on the MUI review-comment field does not update react-hook-form, so Confirm stays disabled. Native click then sendKeys fires onChange.
		WebUI.click(findTestObject(commentPath))
		WebUI.sendKeys(findTestObject(commentPath), comment)
		WebUI.waitForElementClickable(findTestObject(confirmPath), 10)
		WebUI.click(findTestObject(confirmPath))
		WebUI.delay(2)
	}

	/**
	 * Verifies a Notistack toast on Submission Request flows using {@code CRDC/AILocators/Global/Notistack-Snackbar}
	 * ({@code id="notistack-snackbar"}). Waits for visibility, then polls until the message text matches.
	 * {@code snackbarVariant} is optional metadata for the ValidationReport label only (not used for DOM lookup).
	 *
	 * @param expectedMessageText Substring that must appear in the snackbar (default), or the full message when {@code exactMatch} is true
	 * @param timeoutSeconds Max seconds to wait for the snackbar and message
	 * @param exactMatch If true, normalized snackbar text must equal {@code expectedMessageText} (after trim/collapse whitespace)
	 * @param reportSection Section name in the HTML ValidationReport (call ValidationReport.generateValidationReport at end of test)
	 * @param snackbarVariant Optional label for the report (e.g. error, info, success); does not change the locator
	 */
	@Keyword
	public static void verifySubmissionRequestSnackbarError(String expectedMessageText, int timeoutSeconds = 20, boolean exactMatch = false, String reportSection = SECTION_SUBMISSION_REQUEST_UI, String snackbarVariant = 'error') {
		String section = (reportSection != null && !reportSection.trim().isEmpty()) ? reportSection.trim() : SECTION_SUBMISSION_REQUEST_UI
		if (expectedMessageText == null || expectedMessageText.trim().isEmpty()) {
			ValidationReport.addValidationReportItem(section, "Snackbar message", "(non-empty expected text)", "(missing)", "Fail")
			KeywordUtil.markFailed("verifySubmissionRequestSnackbarError: expectedMessageText is required.")
			return
		}
		String variantLabel = (snackbarVariant != null && !snackbarVariant.trim().isEmpty()) ? snackbarVariant.trim().toLowerCase() : 'error'
		String reportLabel = "Notistack snackbar" + (variantLabel ? " (" + variantLabel + ")" : "")
		TestObject snackbarTo = findTestObject(NOTISTACK_SNACKBAR_LOC)
		try {
			WebUI.waitForElementVisible(snackbarTo, timeoutSeconds)
		} catch (Exception e) {
			ValidationReport.addValidationReportItem(section, reportLabel, "visible within " + timeoutSeconds + "s", "not found: " + e.getMessage(), "Fail")
			KeywordUtil.markFailed("verifySubmissionRequestSnackbarError: snackbar did not appear within " + timeoutSeconds + "s. " + e.getMessage())
			return
		}

		String expected = expectedMessageText.trim()
		String normalizedExpected = expected.replaceAll("\\s+", " ").trim()
		String expectedForReport = exactMatch ? ("equals: " + expected) : ("contains: " + expected)
		long deadlineMs = System.currentTimeMillis() + (long) timeoutSeconds * 1000L
		String visibleText = ""
		String normalizedVisible = ""
		boolean matched = false
		while (System.currentTimeMillis() < deadlineMs) {
			try {
				visibleText = WebUI.getText(snackbarTo)
				if (visibleText == null) {
					visibleText = ""
				}
				normalizedVisible = visibleText.replaceAll("\\s+", " ").trim()
				if (exactMatch) {
					matched = normalizedVisible.equals(normalizedExpected)
				} else {
					matched = normalizedVisible.contains(expected)
				}
				if (matched) {
					break
				}
			} catch (StaleElementReferenceException e) {
				KeywordUtil.logInfo("verifySubmissionRequestSnackbarError: stale snackbar, retrying...")
			}
			Thread.sleep(500)
		}

		if (!matched) {
			ValidationReport.addValidationReportItem(section, reportLabel, expectedForReport, visibleText, "Fail")
			if (exactMatch) {
				KeywordUtil.markFailed("verifySubmissionRequestSnackbarError: expected exact message '" + expected + "' but got '" + visibleText + "'")
			} else {
				KeywordUtil.markFailed("verifySubmissionRequestSnackbarError: expected snackbar to contain '" + expected + "' but got '" + visibleText + "'")
			}
			return
		}
		ValidationReport.addValidationReportItem(section, reportLabel, expectedForReport, visibleText, "Pass")
		KeywordUtil.logInfo("verifySubmissionRequestSnackbarError: verified snackbar message.")
	}

	/**
	 * Clicks Review on the Submission Requests list: first tries a row whose text contains {@code studyToken}
	 * (matches full title when the table is not truncated). If none match, the Study column often shows only a short
	 * prefix plus ellipsis—then when exactly one tbody row has a Review control, that row is used (filter already scoped the result).
	 */
	private static void clickSubmissionRequestsListReviewForStudyRow(String studyToken) {
		WebDriver driver = DriverFactory.getWebDriver()
		String tableXpath = TestRunner.givexpath("CRDC/AILocators/GenericTable/Table")
		WebElement table = driver.findElement(By.xpath(tableXpath))
		List<WebElement> rows = table.findElements(By.xpath(".//tbody//tr"))
		if (rows.isEmpty()) {
			KeywordUtil.markFailed("Submission request list: table has no data rows.")
			return
		}

		for (WebElement row : rows) {
			String rowText = row.getText()
			if (rowText != null && rowText.contains(studyToken) && clickReviewControlInListRow(driver, row)) {
				KeywordUtil.logInfo("clickSubmissionRequestsListReviewForStudyRow: matched row by full study token.")
				return
			}
		}

		List<WebElement> rowsWithReview = new ArrayList<>()
		for (WebElement row : rows) {
			if (!findReviewControlsInListRow(row).isEmpty()) {
				rowsWithReview.add(row)
			}
		}
		if (rowsWithReview.size() == 1) {
			KeywordUtil.logInfo("clickSubmissionRequestsListReviewForStudyRow: using single filtered row (study text may be truncated in the grid).")
			if (clickReviewControlInListRow(driver, rowsWithReview.get(0))) {
				return
			}
		}

		if (studyToken.length() > 10) {
			String tailToken = studyToken.substring(studyToken.length() - 10)
			for (WebElement row : rows) {
				String rowText = row.getText()
				if (rowText != null && rowText.contains(tailToken) && clickReviewControlInListRow(driver, row)) {
					KeywordUtil.logInfo("clickSubmissionRequestsListReviewForStudyRow: matched row by suffix of study token.")
					return
				}
			}
		}

		KeywordUtil.markFailed("Submission request list: could not find a Review control (full title not in row text and " +
				(rowsWithReview.size() == 0 ? "no row has Review." : (rowsWithReview.size() + " rows have Review—ambiguous; narrow filters.")))
	}

	private static List<WebElement> findReviewControlsInListRow(WebElement row) {
		return row.findElements(By.xpath(".//button[contains(normalize-space(.), 'Review')] | .//a[contains(normalize-space(.), 'Review')]"))
	}

	private static boolean clickReviewControlInListRow(WebDriver driver, WebElement row) {
		List<WebElement> reviewControls = findReviewControlsInListRow(row)
		if (reviewControls.isEmpty()) {
			return false
		}
		WebElement btn = reviewControls.get(0)
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn)
		WebUI.delay(1)
		btn.click()
		return true
	}

	/**
	 * Same row-matching rules as {@link #clickSubmissionRequestsListReviewForStudyRow(String)}, but clicks the list **View**
	 * action (e.g. In Progress requests) instead of **Review**.
	 */
	private static void clickSubmissionRequestsListViewForStudyRow(String studyToken) {
		WebDriver driver = DriverFactory.getWebDriver()
		String tableXpath = TestRunner.givexpath("CRDC/AILocators/GenericTable/Table")
		WebElement table = driver.findElement(By.xpath(tableXpath))
		List<WebElement> rows = table.findElements(By.xpath(".//tbody//tr"))
		if (rows.isEmpty()) {
			KeywordUtil.markFailed("Submission request list: table has no data rows.")
			return
		}

		for (WebElement row : rows) {
			String rowText = row.getText()
			if (rowText != null && rowText.contains(studyToken) && clickViewControlInListRow(driver, row)) {
				KeywordUtil.logInfo("clickSubmissionRequestsListViewForStudyRow: matched row by full study token.")
				return
			}
		}

		List<WebElement> rowsWithView = new ArrayList<>()
		for (WebElement row : rows) {
			if (!findViewControlsInListRow(row).isEmpty()) {
				rowsWithView.add(row)
			}
		}
		if (rowsWithView.size() == 1) {
			KeywordUtil.logInfo("clickSubmissionRequestsListViewForStudyRow: using single filtered row (study text may be truncated in the grid).")
			if (clickViewControlInListRow(driver, rowsWithView.get(0))) {
				return
			}
		}

		if (studyToken.length() > 10) {
			String tailToken = studyToken.substring(studyToken.length() - 10)
			for (WebElement row : rows) {
				String rowText = row.getText()
				if (rowText != null && rowText.contains(tailToken) && clickViewControlInListRow(driver, row)) {
					KeywordUtil.logInfo("clickSubmissionRequestsListViewForStudyRow: matched row by suffix of study token.")
					return
				}
			}
		}

		KeywordUtil.markFailed("Submission request list: could not find a View control (full title not in row text and " +
				(rowsWithView.size() == 0 ? "no row has View." : (rowsWithView.size() + " rows have View—ambiguous; narrow filters.")))
	}

	private static List<WebElement> findViewControlsInListRow(WebElement row) {
		return row.findElements(By.xpath(".//button[contains(normalize-space(.), 'View')] | .//a[contains(normalize-space(.), 'View')]"))
	}

	private static boolean clickViewControlInListRow(WebDriver driver, WebElement row) {
		List<WebElement> viewControls = findViewControlsInListRow(row)
		if (viewControls.isEmpty()) {
			return false
		}
		WebElement btn = viewControls.get(0)
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn)
		WebUI.delay(1)
		btn.click()
		return true
	}

	/**
	 * On the Submission Requests list (caller applies filters first): find the row for {@code studyToken} and open the form.
	 * Submitter row actions are Resume or View, not Review/Reopen. Row matching follows the same rules as
	 * clickSubmissionRequestsListReviewForStudyRow and clickSubmissionRequestsListViewForStudyRow: full study text in the row,
	 * else exactly one visible row with Resume/View after filter, else a suffix of the study title when the grid truncates Study.
	 */
	private static void clickResumeOrViewForStudyRow(String studyToken) {
		WebDriver driver = DriverFactory.getWebDriver()
		String tableXpath = TestRunner.givexpath("CRDC/AILocators/GenericTable/Table")
		WebElement table = driver.findElement(By.xpath(tableXpath))
		List<WebElement> rows = table.findElements(By.xpath(".//tbody//tr"))
		if (rows.isEmpty()) {
			KeywordUtil.markFailed("Submission request list: table has no data rows.")
			return
		}

		for (WebElement row : rows) {
			String rowText = row.getText()
			if (rowText != null && rowText.contains(studyToken) && clickResumeOrViewControlInListRow(driver, row)) {
				KeywordUtil.logInfo("clickResumeOrViewForStudyRow: matched row by full study token.")
				return
			}
		}

		List<WebElement> rowsWithAction = new ArrayList<>()
		for (WebElement row : rows) {
			if (!findResumeOrViewControlsInListRow(row).isEmpty()) {
				rowsWithAction.add(row)
			}
		}
		if (rowsWithAction.size() == 1) {
			KeywordUtil.logInfo("clickResumeOrViewForStudyRow: using single filtered row (study text may be truncated in the grid).")
			if (clickResumeOrViewControlInListRow(driver, rowsWithAction.get(0))) {
				return
			}
		}

		if (studyToken.length() > 10) {
			String tailToken = studyToken.substring(studyToken.length() - 10)
			for (WebElement row : rows) {
				String rowText = row.getText()
				if (rowText != null && rowText.contains(tailToken) && clickResumeOrViewControlInListRow(driver, row)) {
					KeywordUtil.logInfo("clickResumeOrViewForStudyRow: matched row by suffix of study token.")
					return
				}
			}
		}

		KeywordUtil.markFailed("Submission request list: could not find Resume or View control (full title not in row text and " +
				(rowsWithAction.size() == 0 ? "no row has Resume/View." : (rowsWithAction.size() + " rows have Resume/View—ambiguous; narrow filters.")))
	}

	private static List<WebElement> findResumeControlsInListRow(WebElement row) {
		return row.findElements(By.xpath(".//button[contains(normalize-space(.), 'Resume')] | .//a[contains(normalize-space(.), 'Resume')]"))
	}

	/** True if the row exposes Resume or View (used to detect a single actionable row when Study text is truncated). */
	private static List<WebElement> findResumeOrViewControlsInListRow(WebElement row) {
		List<WebElement> resume = findResumeControlsInListRow(row)
		if (!resume.isEmpty()) {
			return resume
		}
		return findViewControlsInListRow(row)
	}

	/**
	 * Click the list-row control that opens the form for the submitter: Resume when present, otherwise View.
	 * List actions are located by visible label; Reopen uses a separate AILocator on Approved rows.
	 */
	private static boolean clickResumeOrViewControlInListRow(WebDriver driver, WebElement row) {
		List<WebElement> resumeControls = findResumeControlsInListRow(row)
		if (!resumeControls.isEmpty()) {
			WebElement btn = resumeControls.get(0)
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn)
			WebUI.delay(1)
			btn.click()
			return true
		}
		return clickViewControlInListRow(driver, row)
	}

	private static void fedLeadClickNextToNextSection() {
		TestObject nextBtn = findTestObject('CRDC/SubmissionRequest/Next-Btn')
		WebUI.waitForElementClickable(nextBtn, 20)
		WebUI.scrollToElement(nextBtn, 10)
		WebUI.click(nextBtn)
		WebUI.delay(1)
	}

	/**
	 * Federal Lead: reset list filters, filter by study + In Progress, open the row (View; Resume if shown), walk A→B→C→D using **Next** with one read-only
	 * spot-check per section, advance to the final step, then submit on behalf (same flow as {@link #clickSubmitButton()}).
	 * Section B study title is compared to {@link #lastEnteredStudyTitle} (set when the submitter ran {@link #enterStudyInfo(int)}).
	 *
	 * @param studyFilterToken Study filter string (same rules as reject/approve list navigation; use {@link #lastEnteredStudyTitle})
	 * @param sectionARow {@code section-a} row for expected PI first name
	 * @param sectionCRow {@code section-c} row for expected number of subjects
	 * @param sectionDRow {@code section-d} row index for expected estimated data size (file row)
	 */
	@Keyword
	public static void fedLeadSubmitSubmissionRequestOnBehalfWithReadOnlySpotChecks(
			Object studyFilterToken,
			Object sectionARow,
			Object sectionCRow,
			Object sectionDRow
	) {
		String token = studyFilterToken != null ? studyFilterToken.toString().trim() : ''
		if (token.length() < 3) {
			KeywordUtil.markFailed("fedLeadSubmitSubmissionRequestOnBehalfWithReadOnlySpotChecks: study filter token must be at least 3 characters.")
			return
		}
		if (lastEnteredStudyTitle == null || !lastEnteredStudyTitle.trim()) {
			KeywordUtil.markFailed("fedLeadSubmitSubmissionRequestOnBehalfWithReadOnlySpotChecks: lastEnteredStudyTitle is empty; submitter flow must call enterStudyInfo first.")
			return
		}
		int aRow = parseTestDataRowIndex(sectionARow, 'fedLeadSubmitSubmissionRequestOnBehalfWithReadOnlySpotChecks')
		int cRow = parseTestDataRowIndex(sectionCRow, 'fedLeadSubmitSubmissionRequestOnBehalfWithReadOnlySpotChecks')
		int dRow = parseTestDataRowIndex(sectionDRow, 'fedLeadSubmitSubmissionRequestOnBehalfWithReadOnlySpotChecks')

		KeywordUtil.logInfo("fedLeadSubmitSubmissionRequestOnBehalfWithReadOnlySpotChecks: list navigation for study token")
		SubmissionRequestList listHelper = new SubmissionRequestList()
		listHelper.navigateToSubmissionRequestsList()
		listHelper.resetSubmissionRequestsFilters()
		SubmissionRequestList.applySubmissionRequestsListFilters([
			study : token,
			status: 'In Progress'
		] as Map<String, String>)
		listHelper.verifySubmissionRequestsFilterResults([expectAtLeastRows: 1] as Map<String, Object>)
		clickResumeOrViewForStudyRow(token)

		TestData tdA = findTestData('CRDC/SubmissionRequest/section-a')
		TestData tdC = findTestData('CRDC/SubmissionRequest/section-c')
		TestData tdD = findTestData('CRDC/SubmissionRequest/section-d')

		WebUI.waitForElementPresent(findTestObject(SR_FORM_BASE + "/Review/ProgressBar-SectionA"), 40)

		// Section A — PI first name (landing page after View)
		TestObject piFirst = findTestObject('CRDC/SubmissionRequest/Section-A/PI_FirstName-TxtBx')
		WebUI.waitForElementPresent(piFirst, 15)
		assertFedLeadFieldReadOnly(piFirst)
		String expFirst = tdA.getValue('pi-first-name', aRow)?.trim()
		WebUI.verifyMatch(getSectionAFormDisplayedValue(piFirst), expFirst, false)

		fedLeadClickNextToNextSection()

		// Section B — study title (matches timestamped title from submitter)
		TestObject studyTitleTo = findTestObject('CRDC/SubmissionRequest/Section-B/StudyTitle-Txtbx')
		WebUI.waitForElementPresent(studyTitleTo, 15)
		assertFedLeadFieldReadOnly(studyTitleTo)
		WebUI.verifyMatch(getSectionAFormDisplayedValue(studyTitleTo), lastEnteredStudyTitle.trim(), false)

		fedLeadClickNextToNextSection()

		// Section C — number of subjects
		TestObject numSubjTo = findTestObject('CRDC/SubmissionRequest/Section-C/NumOfSubjectsIncludInSub-Txtbx')
		WebUI.waitForElementPresent(numSubjTo, 15)
		assertFedLeadFieldReadOnly(numSubjTo)
		String expNum = tdC.getValue('num-of-subjects-included', cRow)?.trim()
		WebUI.verifyMatch(getSectionAFormDisplayedValue(numSubjTo), expNum, false)

		fedLeadClickNextToNextSection()

		// Section D — estimated data size (first file row)
		TestObject estimTo = findTestObject('CRDC/SubmissionRequest/Section-D/EstimDtaSiz-RowOne-Txtbx')
		WebUI.waitForElementPresent(estimTo, 15)
		assertFedLeadFieldReadOnly(estimTo)
		String expEst = tdD.getValue('estimtd-data-size', dRow)?.trim()
		WebUI.verifyMatch(getSectionAFormDisplayedValue(estimTo), expEst, false)

		// Same as submitter flow: Next after section D opens the final step with Submit (submit on behalf)
		fedLeadClickNextToNextSection()

		WebUI.scrollToElement(findTestObject('CRDC/SubmissionRequest/Submit-Btn'), 15)
		clickSubmitButton()
		KeywordUtil.logInfo('fedLeadSubmitSubmissionRequestOnBehalfWithReadOnlySpotChecks: submit on behalf completed.')
	}

	private static void assertFedLeadFieldReadOnly(TestObject to) {
		WebElement el = WebUI.findWebElement(to, 10)
		if (!el.isEnabled()) {
			return
		}
		String ro = el.getAttribute('readonly')
		String roCamel = el.getAttribute('readOnly')
		boolean hasReadonlyAttr = (ro != null && !ro.isEmpty()) || (roCamel != null && !roCamel.isEmpty())
		boolean ariaDisabled = 'true'.equalsIgnoreCase(el.getAttribute('aria-disabled'))
		if (!hasReadonlyAttr && !ariaDisabled) {
			KeywordUtil.markFailed("fedLeadSubmitSubmissionRequestOnBehalfWithReadOnlySpotChecks: expected read-only control is still editable: " + to.getObjectId())
		}
	}

	private static final String SECTION_CRDCDH_2226 = 'CRDCDH-2226'
	private static final String REJECT_DIALOG_COMMENT = 'CRDC/AILocators/SubmissionRequests/RejectDialog/ReviewComment-TxtBx'
	private static final String REJECT_DIALOG_COMMENT_COUNT = 'CRDC/AILocators/SubmissionRequests/RejectDialog/ReviewComment-CharacterCount'
	private static final String REJECT_DIALOG_COMMENT_ERROR = 'CRDC/AILocators/SubmissionRequests/RejectDialog/ReviewComment-DialogError'
	private static final String REJECT_DIALOG_CONFIRM = 'CRDC/AILocators/SubmissionRequests/RejectDialog/ConfirmReject-Btn'
	private static final Map<String, String> STATUS_DESCRIPTIONS = [
		'New': 'The request form was created.',
		'In Progress': 'The request form was started but not submitted.',
		'Submitted': 'The request form was submitted for review.',
		'In Review': 'The request form is under evaluation by the Submission Review Committee.',
		'Approved': 'The request form was reviewed and approved.',
		'Reopened': 'The approved request form was reopened for editing.',
		'Rejected': 'The request form was reviewed and rejected.',
		'Inquired': 'Additional information or clarification was requested from the submitter.',
		'Canceled': 'The request form was manually canceled and is no longer active.',
		'Deleted': 'The request form was automatically deleted by the system due to inactivity and is no longer active.'
	]

	/**
	 * Click the list action on the first table row. Caller must already be on the Submission Requests list
	 * (typically after applySubmissionRequestsListFilters).
	 * @param action Optional. "view" (default) opens the form via Resume or View. "review" opens the Fed Lead Review flow.
	 */
	@Keyword
	public static void clickFirstSubmissionRequestListRow(String action = 'view') {
		WebDriver driver = DriverFactory.getWebDriver()
		String tableXpath = TestRunner.givexpath('CRDC/AILocators/GenericTable/Table')
		WebElement table = driver.findElement(By.xpath(tableXpath))
		List<WebElement> rows = table.findElements(By.xpath(".//tbody//tr"))
		if (rows.isEmpty()) {
			KeywordUtil.markFailed('clickFirstSubmissionRequestListRow: table has no data rows.')
			return
		}
		WebElement firstRow = rows.get(0)
		boolean clicked = false
		if (action != null && action.trim().equalsIgnoreCase('review')) {
			clicked = clickReviewControlInListRow(driver, firstRow)
		} else {
			clicked = clickResumeOrViewControlInListRow(driver, firstRow)
		}
		if (!clicked) {
			KeywordUtil.markFailed('clickFirstSubmissionRequestListRow: no ' + action + ' control on first row.')
		}
		WebUI.delay(2)
	}

	/**
	 * From an open Submission Request form, click Full History on the status bar and wait for the history dialog.
	 * Records a Pass entry in the validation report when the dialog appears.
	 */
	@Keyword
	public static void openFullHistoryDialog() {
		WebUIUtils.clickTab('CRDC/AILocators/SubmissionRequests/StatusBar/FullHistory-Btn')
		boolean dialogOpen = WebUI.waitForElementPresent(findTestObject('CRDC/AILocators/SubmissionRequests/StatusBar/History-Dialog'), 15)
		if (!dialogOpen) {
			ValidationReport.addValidationReportItem(SECTION_CRDCDH_2226, 'Full History dialog', 'open', 'not found', 'Fail')
			KeywordUtil.markFailed('openFullHistoryDialog: status-bar-history-dialog did not appear.')
			return
		}
		ValidationReport.addValidationReportItem(SECTION_CRDCDH_2226, 'Full History dialog', 'open', 'open', 'Pass')
	}

	/**
	 * With Full History open, hover each history row status label and verify the MUI tooltip text.
	 * Expected copy matches crdc-datahub-ui QuestionnaireTooltips STATUS_DESCRIPTIONS (built-in map when
	 * expectedByStatus is null or empty). History labels are uppercase in the UI; they are mapped to status keys
	 * before lookup. Unknown statuses are skipped with a log line. Each check is reported via ValidationReport.
	 * @param expectedByStatus Optional map of status key to tooltip text; omit to use the default product copy.
	 */
	@Keyword
	public static void verifyFullHistoryStatusTooltips(Map expectedByStatus = null) {
		Map descriptions = (expectedByStatus != null && !expectedByStatus.isEmpty()) ? expectedByStatus : STATUS_DESCRIPTIONS
		List<WebElement> statusEls = Utils.getListWebElements('CRDC/AILocators/SubmissionRequests/StatusBar/History-Item-Status')
		if (statusEls == null || statusEls.isEmpty()) {
			KeywordUtil.markFailed('verifyFullHistoryStatusTooltips: no history status elements found.')
			return
		}
		for (int i = 0; i < statusEls.size(); i++) {
			WebElement statusEl = statusEls.get(i)
			String display = statusEl.getText()?.trim()
			if (display == null || display.isEmpty()) {
				continue
			}
			String key = historyDisplayStatusToKey(display)
			if (key == null || !descriptions.containsKey(key)) {
				KeywordUtil.markFailed('verifyFullHistoryStatusTooltips: no expected tooltip configured for status label: ' + display)
				continue
			}
			String expected = descriptions.get(key)?.toString()
			TestObject trigger = new TestObject('history_status_' + i)
			trigger.addProperty('xpath', ConditionType.EQUALS, "//*[@data-testid='status-bar-history-dialog']//*[@data-testid='history-item-" + i + "-status']", true)
			WebUIUtils.verifyTooltipOnHover(trigger, expected, expected.take(Math.min(20, expected.length())))
		}
	}

	/** Maps a history row status label (e.g. APPROVED) to STATUS_DESCRIPTIONS key (e.g. Approved). */
	private static String historyDisplayStatusToKey(String displayUpper) {
		for (String key : STATUS_DESCRIPTIONS.keySet()) {
			if (key.equalsIgnoreCase(displayUpper?.trim())) {
				return key
			}
		}
		return null
	}

	/**
	 * Fed Lead flow: filter list by study, open Review, go to the Review section, then open a decision dialog
	 * without entering a comment or confirming. Used to exercise ReviewFormDialog UI before submit/cancel.
	 * @param studyName Study title or filter token (minimum 3 characters), same rules as rejectSubmissionRequest.
	 * @param decision Optional. "reject" (default), "approve", or "inquire".
	 */
	@Keyword
	public static void openReviewDecisionDialog(String studyName, String decision = 'reject') {
		if (openSubmissionRequestReview(studyName, 'openReviewDecisionDialog') == null) {
			return
		}
		String actionPath
		String d = decision != null ? decision.trim().toLowerCase() : 'reject'
		if (d == 'approve') {
			actionPath = SR_FORM_BASE + '/Navigation/Approve-Btn'
		} else if (d == 'inquire') {
			actionPath = SR_FORM_BASE + '/Navigation/Inquire-Btn'
		} else {
			actionPath = SR_FORM_BASE + '/Navigation/Reject-Btn'
		}
		WebUI.scrollToElement(findTestObject(actionPath), 15)
		WebUI.waitForElementClickable(findTestObject(actionPath), 15)
		WebUI.click(findTestObject(actionPath))
		WebUI.waitForElementPresent(findTestObject(REJECT_DIALOG_COMMENT), 10)
	}

	/**
	 * Click Cancel on the open ReviewFormDialog (Approve/Reject/Inquire) and dismiss without saving a decision.
	 */
	@Keyword
	public static void cancelReviewDecisionDialog() {
		WebUI.waitForElementClickable(findTestObject('CRDC/AILocators/SubmissionRequests/RejectDialog/Cancel-Btn'), 10)
		WebUI.click(findTestObject('CRDC/AILocators/SubmissionRequests/RejectDialog/Cancel-Btn'))
		WebUI.delay(1)
	}

	/**
	 * On an open Reject ReviewFormDialog: type hello and check counter 5 / limit; then set limit+1 characters,
	 * click Confirm, and check the max-length validation message. Does not complete reject.
	 * @param maxLimit Maximum allowed characters (default 10000).
	 */
	@Keyword
	public static void verifyReviewCommentFieldUi(int maxLimit = 10000) {
		String limitLabel = String.format('%,d', maxLimit)
		String overLimitError = 'Maximum of ' + limitLabel + ' characters allowed'

		WebUI.waitForElementPresent(findTestObject(REJECT_DIALOG_COMMENT_COUNT), 10)
		WebUI.click(findTestObject(REJECT_DIALOG_COMMENT))
		WebUI.sendKeys(findTestObject(REJECT_DIALOG_COMMENT), 'hello')
		WebUI.delay(1)
		String expectedFive = '5 / ' + limitLabel
		String actualFive = WebUI.getText(findTestObject(REJECT_DIALOG_COMMENT_COUNT))?.trim() ?: ''
		boolean fiveOk = expectedFive == actualFive
		ValidationReport.addValidationReportItem(SECTION_CRDCDH_2226, 'Review comment count after hello', expectedFive, actualFive ?: '(empty)', fiveOk ? 'Pass' : 'Fail')
		if (!fiveOk) {
			KeywordUtil.markFailed('verifyReviewCommentFieldUi: expected "' + expectedFive + '", got "' + actualFive + '"')
			return
		}

		WebUI.click(findTestObject(REJECT_DIALOG_COMMENT))
		WebUIUtils.clearTextfieldByOs(REJECT_DIALOG_COMMENT)
		WebUI.sendKeys(findTestObject(REJECT_DIALOG_COMMENT), 'z'.repeat(maxLimit + 1))
		WebUI.delay(2)

		WebUI.click(findTestObject(REJECT_DIALOG_CONFIRM))
		WebUI.delay(1)
		WebUI.waitForElementVisible(findTestObject(REJECT_DIALOG_COMMENT_ERROR), 10)
		String errorText = WebUI.getText(findTestObject(REJECT_DIALOG_COMMENT_ERROR))?.trim() ?: ''
		boolean errorOk = errorText.contains(overLimitError)
		ValidationReport.addValidationReportItem(SECTION_CRDCDH_2226, 'Review comment over-limit error', overLimitError, errorText ?: '(empty)', errorOk ? 'Pass' : 'Fail')
		if (!errorOk) {
			KeywordUtil.markFailed('verifyReviewCommentFieldUi: expected "' + overLimitError + '" after Confirm, got "' + errorText + '"')
		}
	}

	/**
	 * Assert whether the status bar View Comments button is present on the current Submission Request form.
	 * The button appears when history includes at least one review comment.
	 * @param expectedDisplayed true if the button should be visible (default).
	 */
	@Keyword
	public static void verifyViewCommentsButtonDisplayed(boolean expectedDisplayed = true) {
		boolean present = WebUIUtils.fastElementPresent('CRDC/AILocators/SubmissionRequests/StatusBar/ViewComments-Btn')
		String status = (present == expectedDisplayed) ? 'Pass' : 'Fail'
		ValidationReport.addValidationReportItem(SECTION_CRDCDH_2226, 'View Comments button', String.valueOf(expectedDisplayed), String.valueOf(present), status)
		if (status == 'Fail') {
			KeywordUtil.markFailed('verifyViewCommentsButtonDisplayed: expected ' + expectedDisplayed + ', actual ' + present)
		}
	}

	/**
	 * Click View Comments on the status bar and wait for the review-comments-dialog.
	 */
	@Keyword
	public static void openViewCommentsDialog() {
		WebUIUtils.clickTab('CRDC/AILocators/SubmissionRequests/StatusBar/ViewComments-Btn')
		// Status bar opens the dialog in one click; legacy two-step flow uses comments-item-0-view when present.
		if (WebUIUtils.fastElementPresent('CRDC/AILocators/SubmissionRequests/StatusBar/ReviewComments-View-Btn')) {
			WebUIUtils.clickTab('CRDC/AILocators/SubmissionRequests/StatusBar/ReviewComments-View-Btn')
		}
		WebUI.waitForElementPresent(findTestObject('CRDC/AILocators/SubmissionRequests/StatusBar/ReviewComments-Dialog'), 15)
	}

	/**
	 * With review-comments-dialog open, verify title contains "Comments", optionally verify comment body text,
	 * record results in ValidationReport, then click Close. UI shows the most recent history entry with a review comment.
	 * @param expectedSubstring If non-null and non-empty, dialog text must contain this substring; if null, only checks non-empty body.
	 * @param verifyTitleComments When true (default), fail if "Comments" is not in the dialog text.
	 * @param mustNotContainSubstring If non-null and non-empty, dialog text must not contain this substring (e.g. older inquire comment).
	 */
	@Keyword
	public static void verifyViewCommentsDialog(String expectedSubstring = null, boolean verifyTitleComments = true, String mustNotContainSubstring = null) {
		TestObject dialogTo = findTestObject('CRDC/AILocators/SubmissionRequests/StatusBar/ReviewComments-Dialog')
		WebUI.waitForElementVisible(dialogTo, 10)
		String dialogText = WebUI.getText(dialogTo)?.trim() ?: ''
		if (dialogText.isEmpty()) {
			KeywordUtil.markFailed('verifyViewCommentsDialog: dialog text is empty.')
			return
		}
		if (verifyTitleComments && !dialogText.contains('Comments')) {
			ValidationReport.addValidationReportItem(SECTION_CRDCDH_2226, 'View Comments title', 'Comments', dialogText.take(80), 'Fail')
			KeywordUtil.markFailed('verifyViewCommentsDialog: expected Comments in dialog.')
			return
		}
		ValidationReport.addValidationReportItem(SECTION_CRDCDH_2226, 'View Comments title', 'Comments', 'present', 'Pass')

		if (expectedSubstring != null && !expectedSubstring.trim().isEmpty()) {
			boolean contains = dialogText.contains(expectedSubstring.trim())
			ValidationReport.addValidationReportItem(SECTION_CRDCDH_2226, 'View Comments body', 'contains ' + expectedSubstring.take(40), contains ? 'found' : 'not found', contains ? 'Pass' : 'Fail')
			if (!contains) {
				KeywordUtil.markFailed('verifyViewCommentsDialog: comment body missing expected text.')
				return
			}
		} else {
			ValidationReport.addValidationReportItem(SECTION_CRDCDH_2226, 'View Comments body', 'non-empty', 'length ' + dialogText.length(), 'Pass')
		}

		if (mustNotContainSubstring != null && !mustNotContainSubstring.trim().isEmpty()) {
			String stale = mustNotContainSubstring.trim()
			boolean absent = !dialogText.contains(stale)
			ValidationReport.addValidationReportItem(SECTION_CRDCDH_2226, 'View Comments body stale text', 'must not contain ' + stale.take(40), absent ? 'absent' : 'present', absent ? 'Pass' : 'Fail')
			if (!absent) {
				KeywordUtil.markFailed('verifyViewCommentsDialog: dialog still shows older comment text.')
				return
			}
		}

		WebUI.click(findTestObject('CRDC/AILocators/SubmissionRequests/StatusBar/ReviewComments-Dialog-Close'))
		WebUI.delay(1)
	}
}