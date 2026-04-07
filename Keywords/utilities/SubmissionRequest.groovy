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
import org.openqa.selenium.JavascriptExecutor;
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.kms.katalon.core.configuration.RunConfiguration;
import java.nio.file.Files;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Sheet;





class SubmissionRequest extends TestRunner implements Comparator<List<XSSFCell>>{
	public int compare( List<XSSFCell> l1, List<XSSFCell> l2 ){
		return l1.get(0).getStringCellValue().compareTo( l2.get(0).getStringCellValue() )
	}

	public static WebDriver driver

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
	 * @param Status to be validated i.e New, Submitted
	 */
	@Keyword
	public static void verifyStatusBar(String expStatus) {
		expStatus = expStatus.toUpperCase();
		String actualStatus = WebUI.getText(findTestObject('CRDC/SubmissionRequest/Status-Bar/StatusBar-Stutus'))
		String actualDate = WebUI.getText(findTestObject('CRDC/SubmissionRequest/Status-Bar/LastUpdated-Date'))
		System.out.println("Actual Status is: " + actualStatus +"\nExpected Status is: "+expStatus);
		System.out.println("Actual Date is: " + actualDate +"\nExpected Date is: "+getCurrentDate("M/d/yyyy"));
		WebUI.verifyMatch(actualStatus, expStatus, false)
		WebUI.verifyMatch(actualDate, getCurrentDate("M/d/yyyy"), false)
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

			if (existingValue?.trim()) {
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
			WebUI.setText(findTestObject(ePath+'ProgramTitle-TxtBx'), findTestData(fPath).getValue('program-title', dataRowNum)+programTimeStamp);
			WebUI.setText(findTestObject(ePath+'ProgAbbre-Txtbx'), findTestData(fPath).getValue('prog-abbreviation', dataRowNum)+programTimeStamp);
			WebUI.setText(findTestObject(ePath+'ProgDescrptn-Txtbx'), findTestData(fPath).getValue('prog-description', dataRowNum));
		}else if(ddValueToSelect.contains("CCDI")) {

			validateProgramFields(2);
		}else if(ddValueToSelect.contains("CPTAC")) {

			validateProgramFields(3);
		}else if(ddValueToSelect.contains("DCCPS")) {

			validateProgramFields(4);
		}else if(ddValueToSelect.contains("HTAN")) {

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

		TestObject pubDateField = findTestObject(objPath + 'ExpectedPubDate-Clndr')
		WebUI.clearText(pubDateField)
		WebUI.setText(pubDateField, Utils.getCurrentDate("MM/dd/yyyy"))
		KeywordUtil.logInfo("Successfully entered planned publication");
	}

	/**
	 * This function enters Repository information into the submission request form.
	 * @param dropDownValue The value to select from the Data Types Submitted dropdown
	 * @param dataRowNum Row number to read from the test data file
	 */
	@Keyword
	public static void enterRepositoryInfo(String dropDownValue, int dataRowNum) {
		String objPath = 'CRDC/SubmissionRequest/Section-B/';
		TestData testData = findTestData('CRDC/SubmissionRequest/section-b');

		WebUI.click(findTestObject(objPath + 'AddRepository-Btn'))
		Thread.sleep(1000)

		Map<String, String> fields = [
			'RepositoryName-Txtbx'   : 'repository-name',
			'StudyID-Txtbx'          : 'study-id',
			'OtherDataTypes-Txtbx'   : 'other-data-types'
		]

		fields.each { testObjectId, columnName ->
			TestObject to = findTestObject(objPath + testObjectId)
			WebUI.waitForElementPresent(to, 10)
			String value = testData.getValue(columnName, dataRowNum)
			WebUI.setText(to, clearText() + value)
			KeywordUtil.logInfo("Successfully entered: " + value +" into "+ columnName+" field.");
		}

		// Handle dropdown selection
		WebUI.click(findTestObject(objPath + 'DataTypesSubmitd-Dd'))
		Thread.sleep(500)
		GlobalVariable.CrdcUiElement = dropDownValue
		WebUI.click(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'))
		GlobalVariable.CrdcUiElement="Proteomics";
		WebUI.sendKeys(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'), Keys.chord(Keys.TAB))
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
		TimeZone.setDefault(TimeZone.getTimeZone("EDT"))

		//String currentDate = new SimpleDateFormat("mm/dd/yyyy").format(new Date())
		Thread.sleep(10000)
		WebUI.takeScreenshot("./OutputFiles/BeforeEnteringData1.png")

		//clearText(findTestObject(ePath+'ExpctdPubliDate-Clndr'))
		//WebUI.setText(findTestObject(ePath+'ExpctdPubliDate-Clndr'), clearText() + getCurrentDate("MM/dd/yyyy"));


		WebUI.takeScreenshot("./OutputFiles/BeforeEnteringData2.png")
		WebUI.setText(findTestObject('CRDC/SubmissionRequest/Section-D/TragetSubmDelivryDate-TxtBx'), clearText() + "05/12/2026");

		WebUI.setText(findTestObject('CRDC/SubmissionRequest/Section-D/ExpctdPublicationDate-TxtBx'), clearText() + "05/12/2026");
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

	/**
	 * This function selects Data Types on the submission request form
	 * @param buttonLable Toggle button label to be clicked (enter only one word per toggle button)
	 */
	@Keyword
	public static void selectDataTypes(String... buttonLable) {

		ePath = "CRDC/SubmissionRequest/Section-D/";
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
		expctd =  getCurrentDate("MM/dd/yyyy")
		KeywordUtil.logInfo("Actual Publication date is: " + actual +" Expected Publication date is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)
		KeywordUtil.logInfo("Successfully verified Publications information");
	}

	/**
	 *  This function verifies Repository information on the review page
	 * @param dataFileRowNum Please add data file row number to be read
	 */
	@Keyword
	public static void verifyRepositoryInfo(String dropDownVlue, int dataRowNum) {

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


		//Add code to verify DATA TYPE(S) SUBMITTED. Add if condition. i.e. if is clinical then do.... by checking current ui value
		//See below code to come up with a logic
		//WebUI.click(findTestObject(ePath+'DataTypesSubmitd-Dd'));
		//Thread.sleep(500);
		//GlobalVariable.CrdcUiElement=dropDownVlue;
		//WebUI.click(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'));
		//GlobalVariable.CrdcUiElement="Proteomics";
		//WebUI.sendKeys(findTestObject('CRDC/SubmissionRequest/CrdcDdValue'), Keys.chord(Keys.TAB))

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
		expctd = "05/12/2026";
		KeywordUtil.logInfo("Actual target data submission delivery date is: " + actual +" Expected target data submission delivery date is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

		actual = WebUI.getText(findTestObject(ePath+'ExpctdPubliDateSec-D-RevPage-Txt'))
		expctd =  "05/12/2026"
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
	 * This function clicks on submit and confirm submit buttons
	 */
	@Keyword
	public static void cancelSubmissionRequestButton() {
		findTestObject('Object Repository/CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequest-TxtBx')
		WebUI.click(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelRequest-Btn'))
		Thread.sleep(1000)
		WebUI.setText(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequest-TxtBx'), "Auto Test Cancel")
		Thread.sleep(1000)
		WebUI.click(findTestObject('CRDC/AILocators/SubmissionRequests/Form/Review/CancelSubmissionRequestConfirm-Btn'))
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
	private static final String SR_FORM_BASE = "CRDC/AILocators/SubmissionRequests/Form"
	/** ValidationReport section for submission-request UI checks (snackbar, etc.); aligns with list section naming. */
	private static final String SECTION_SUBMISSION_REQUEST_UI = "Submission Request"

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
		if (studyName == null || studyName.trim().isEmpty()) {
			KeywordUtil.markFailed("rejectSubmissionRequest: studyName is required.")
			return
		}
		String token = studyName.trim()
		if (token.length() < 3) {
			KeywordUtil.markFailed("rejectSubmissionRequest: studyName must be at least 3 characters (Submission Requests Study filter).")
			return
		}
		String comment = (rejectionComment != null && !rejectionComment.trim().isEmpty()) ? rejectionComment.trim() : DEFAULT_REJECTION_COMMENT

		KeywordUtil.logInfo("rejectSubmissionRequest: navigating to Submission Requests list; study token length=" + token.length())
		new SubmissionRequestList().navigateToSubmissionRequestsList()
		SubmissionRequestList.applySubmissionRequestsListFilters([study: token] as Map<String, String>)
		WebUI.delay(2)
		clickSubmissionRequestsListReviewForStudyRow(token)

		// List "Review" opens the request on the first section (e.g. PI); Approve/Reject exist only on the final Review step.
		WebUI.waitForElementPresent(findTestObject(SR_FORM_BASE + "/Review/ProgressBar-Review"), 45)
		WebUI.waitForElementClickable(findTestObject(SR_FORM_BASE + "/Review/ProgressBar-Review"), 15)
		WebUI.click(findTestObject(SR_FORM_BASE + "/Review/ProgressBar-Review"))
		WebUI.delay(2)
		WebUI.scrollToElement(findTestObject(SR_FORM_BASE + "/Navigation/Reject-Btn"), 15)
		WebUI.waitForElementVisible(findTestObject(SR_FORM_BASE + "/Navigation/Reject-Btn"), 20)
		WebUI.waitForElementClickable(findTestObject(SR_FORM_BASE + "/Navigation/Reject-Btn"), 15)
		WebUI.click(findTestObject(SR_FORM_BASE + "/Navigation/Reject-Btn"))
		TestObject rejectCommentTo = findTestObject("CRDC/AILocators/SubmissionRequests/RejectDialog/ReviewComment-TxtBx")
		WebUI.waitForElementPresent(rejectCommentTo, 10)
		WebUI.waitForElementVisible(rejectCommentTo, 10)
		WebUI.setText(rejectCommentTo, comment)
		WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/SubmissionRequests/RejectDialog/ConfirmReject-Btn"), 10)
		WebUI.click(findTestObject("CRDC/AILocators/SubmissionRequests/RejectDialog/ConfirmReject-Btn"))
		WebUI.delay(2)
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
		if (studyName == null || studyName.trim().isEmpty()) {
			KeywordUtil.markFailed("approveSubmissionRequest: studyName is required.")
			return
		}
		String token = studyName.trim()
		if (token.length() < 3) {
			KeywordUtil.markFailed("approveSubmissionRequest: studyName must be at least 3 characters (Submission Requests Study filter).")
			return
		}
		String comment = (approvalComment != null && !approvalComment.trim().isEmpty()) ? approvalComment.trim() : DEFAULT_APPROVAL_COMMENT

		KeywordUtil.logInfo("approveSubmissionRequest: navigating to Submission Requests list; study token length=" + token.length())
		new SubmissionRequestList().navigateToSubmissionRequestsList()
		SubmissionRequestList.applySubmissionRequestsListFilters([study: token] as Map<String, String>)
		WebUI.delay(2)
		clickSubmissionRequestsListReviewForStudyRow(token)

		WebUI.waitForElementPresent(findTestObject(SR_FORM_BASE + "/Review/ProgressBar-Review"), 45)
		WebUI.waitForElementClickable(findTestObject(SR_FORM_BASE + "/Review/ProgressBar-Review"), 15)
		WebUI.click(findTestObject(SR_FORM_BASE + "/Review/ProgressBar-Review"))
		WebUI.delay(2)
		WebUI.scrollToElement(findTestObject(SR_FORM_BASE + "/Navigation/Approve-Btn"), 15)
		WebUI.waitForElementVisible(findTestObject(SR_FORM_BASE + "/Navigation/Approve-Btn"), 20)
		WebUI.waitForElementClickable(findTestObject(SR_FORM_BASE + "/Navigation/Approve-Btn"), 15)
		WebUI.click(findTestObject(SR_FORM_BASE + "/Navigation/Approve-Btn"))
		WebUI.delay(2)
		TestObject approveCommentTo = findTestObject("CRDC/AILocators/SubmissionRequests/ApproveDialog/ReviewComment-TxtBx")
		WebUI.waitForElementPresent(approveCommentTo, 10)
		WebUI.waitForElementVisible(approveCommentTo, 10)
		WebUI.setText(approveCommentTo, comment)
		WebUI.delay(1)
		WebUI.waitForElementClickable(findTestObject("CRDC/AILocators/SubmissionRequests/ApproveDialog/ConfirmApprove-Btn"), 10)
		WebUI.click(findTestObject("CRDC/AILocators/SubmissionRequests/ApproveDialog/ConfirmApprove-Btn"))
		WebUI.delay(2)
		KeywordUtil.logInfo("approveSubmissionRequest: completed for study token.")
	}

	/**
	 * Verifies the Notistack/MUI error snackbar shown on Submission Request flows (e.g. after Approve when validation fails).
	 * Matches the hub DOM: {@code div.notistack-MuiContent-error[role='alert']}; message text is read from {@code #notistack-snackbar} when present, otherwise from the alert container.
	 * <p>
	 * Reusable for any submission-request error toast by passing the expected text (substring or full message).
	 * </p>
	 *
	 * @param expectedMessageText Substring that must appear in the snackbar (default), or the full message when {@code exactMatch} is true
	 * @param timeoutSeconds Max seconds to wait for the error snackbar to become visible
	 * @param exactMatch If true, normalized snackbar text must equal {@code expectedMessageText} (after trim/collapse whitespace)
	 * @param reportSection Section name in the HTML ValidationReport (call {@link ValidationReport#generateValidationReport()} at end of test)
	 */
	@Keyword
	public static void verifySubmissionRequestSnackbarError(String expectedMessageText, int timeoutSeconds = 20, boolean exactMatch = false, String reportSection = SECTION_SUBMISSION_REQUEST_UI) {
		String section = (reportSection != null && !reportSection.trim().isEmpty()) ? reportSection.trim() : SECTION_SUBMISSION_REQUEST_UI
		if (expectedMessageText == null || expectedMessageText.trim().isEmpty()) {
			ValidationReport.addValidationReportItem(section, "Error snackbar message", "(non-empty expected text)", "(missing)", "Fail")
			KeywordUtil.markFailed("verifySubmissionRequestSnackbarError: expectedMessageText is required.")
			return
		}
		WebDriver driver = DriverFactory.getWebDriver()
		WebDriverWait wait = new WebDriverWait(driver, timeoutSeconds)
		By snackbarLocator = By.cssSelector("div.notistack-MuiContent-error[role='alert']")
		WebElement snackbar
		try {
			snackbar = wait.until(ExpectedConditions.visibilityOfElementLocated(snackbarLocator))
		} catch (Exception e) {
			ValidationReport.addValidationReportItem(section, "Error snackbar (Notistack)", "visible within " + timeoutSeconds + "s", "not found: " + e.getMessage(), "Fail")
			KeywordUtil.markFailed("verifySubmissionRequestSnackbarError: error snackbar did not appear within " + timeoutSeconds + "s. " + e.getMessage())
			return
		}

		String visibleText = readNotistackErrorSnackbarText(wait, snackbarLocator, snackbar)
		if (visibleText == null) {
			visibleText = ""
		}
		String normalizedVisible = visibleText.replaceAll("\\s+", " ").trim()
		String expected = expectedMessageText.trim()
		String expectedForReport = exactMatch ? ("equals: " + expected) : ("contains: " + expected)

		if (exactMatch) {
			String normalizedExpected = expected.replaceAll("\\s+", " ").trim()
			if (!normalizedVisible.equals(normalizedExpected)) {
				ValidationReport.addValidationReportItem(section, "Error snackbar (Notistack)", expectedForReport, visibleText, "Fail")
				KeywordUtil.markFailed("verifySubmissionRequestSnackbarError: expected exact message '" + expected + "' but got '" + visibleText + "'")
				return
			}
		} else {
			if (!normalizedVisible.contains(expected)) {
				ValidationReport.addValidationReportItem(section, "Error snackbar (Notistack)", expectedForReport, visibleText, "Fail")
				KeywordUtil.markFailed("verifySubmissionRequestSnackbarError: expected snackbar to contain '" + expected + "' but got '" + visibleText + "'")
				return
			}
		}
		ValidationReport.addValidationReportItem(section, "Error snackbar (Notistack)", expectedForReport, visibleText, "Pass")
		KeywordUtil.logInfo("verifySubmissionRequestSnackbarError: verified snackbar message.")
	}

	private static String readNotistackErrorSnackbarText(WebDriverWait wait, By snackbarLocator, WebElement snackbar) {
		try {
			return extractSnackbarVisibleText(snackbar)
		} catch (StaleElementReferenceException e) {
			WebElement refreshed = wait.until(ExpectedConditions.visibilityOfElementLocated(snackbarLocator))
			return extractSnackbarVisibleText(refreshed)
		}
	}

	private static String extractSnackbarVisibleText(WebElement snackbar) {
		List<WebElement> inner = snackbar.findElements(By.id("notistack-snackbar"))
		if (!inner.isEmpty()) {
			String t = inner.get(0).getText()
			if (t != null && !t.trim().isEmpty()) {
				return t
			}
		}
		return snackbar.getText()
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

	private static void fedLeadClickNextToNextSection() {
		TestObject nextBtn = findTestObject('CRDC/SubmissionRequest/Next-Btn')
		WebUI.waitForElementClickable(nextBtn, 20)
		WebUI.scrollToElement(nextBtn, 10)
		WebUI.click(nextBtn)
		WebUI.delay(1)
	}

	/**
	 * Federal Lead: filter the list, open the row via **View** (In Progress), walk A→B→C→D using **Next** with one read-only
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
		new SubmissionRequestList().navigateToSubmissionRequestsList()
		SubmissionRequestList.applySubmissionRequestsListFilters([study: token] as Map<String, String>)
		WebUI.delay(2)
		clickSubmissionRequestsListViewForStudyRow(token)

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
}