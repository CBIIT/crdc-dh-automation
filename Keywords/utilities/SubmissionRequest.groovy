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
import java.util.HashMap;
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
	 */
	@Keyword
	public static void navigateToCrdc() {
		driver = CustomBrowserDriver.createWebDriver();
		driver.manage().window().maximize();
		driver.get(GlobalVariable.G_Urlname);
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		WebUI.waitForPageLoad(30)
		WebUI.verifyMatch(GlobalVariable.G_Urlname, WebUI.getUrl(), false)
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
	public static String studyTimeStamp;


	/**
	 * This function enters study information into submission request form
	 * @param dataRowNum Row number from the test data file
	 */
	@Keyword
	public static void enterStudyInfo(int dataRowNum) {

		String objPath = 'CRDC/SubmissionRequest/Section-B/';
		TestData testData = findTestData('CRDC/SubmissionRequest/section-b');

		// Timestamp used to make study title and abbreviation unique
		studyTimeStamp = Utils.getCurrentDate("M-d-yyyy-HH-mm");

		// Define mappings of field object names to their data keys
		Map<String, String> fields = [
			'StudyTitle-Txtbx'       : testData.getValue('study-title', dataRowNum) + studyTimeStamp,
			'StudyAbbre-Txtbx'       : testData.getValue('study-abbreviation', dataRowNum) + studyTimeStamp,
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

		actual = WebUI.getText(findTestObject(ePath+'NciGenProgAdmstr-Txt'))
		expctd = testData.getValue('nci-genomic-prog-admin', dataRowNum)
		KeywordUtil.logInfo("Actual NCI program Administrator is: " + actual +" Expected NCI program Administrator is: "+expctd);
		WebUI.verifyMatch(actual, expctd, false)

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
	 * This function logs in to CRDC via Login.gov 2FA / OTP using a dynamic TOTP secret.
	 * @param Admin, Fedlead, Dcp, Submitter, User
	 */
	@Keyword
	public static void loginToCrdcOtp(String pbacUser) {
		clickTab('CRDC/NavBar/WarningBanner_Continue-Btn')

		clickTab('CRDC/Login/LoginPageLogin-Btn')
		clickTab('CRDC/Login/Login.gov-Btn')

		String userEmail = GlobalVariable."${pbacUser}Email"
		String userPassword = GlobalVariable."${pbacUser}Password"
		String userSecret = GlobalVariable."${pbacUser}Secret"

		WebUI.setText(findTestObject('CRDC/Login/Login.gov_UserEmail-TxtBx'), userEmail)
		WebUI.setText(findTestObject('CRDC/Login/Login.gov_UserPass-TxtBx'), userPassword)
		clickTab('CRDC/Login/Login.gov_SignIn-Btn')

		String pythonPath = Utils.getPythonExecutablePath();
		String scriptPath = "${System.getProperty('user.dir')}/PythonFiles/CRDC/GenerateOtp.py"

		// Generate OTP
		Closure<String> generateTotp = { String secretArg ->
			ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath, secretArg)
			pb.redirectErrorStream(true)
			Process proc = pb.start()
			proc.waitFor()
			return proc.inputStream.text.trim()
		}

		int otpAttempts = 0
		boolean loggedIn = false

		while (otpAttempts < 2 && !loggedIn) {
			String totpCode = generateTotp(userSecret)
			println("Attempt ${otpAttempts + 1} - TOTP Code: ${totpCode}")

			WebUI.setText(findTestObject('CRDC/Login/Login.gov_OneTimeCode-TxtBx'), totpCode)
			clickTab('CRDC/Login/Login.gov_OneTimeCode_Submit-Btn')

			// Wait for Grant button
			TestObject consentBtn = findTestObject('CRDC/Login/Login.gov_ConsentGrant-Btn')
			println("Waiting for 'Grant' button...")

			if (WebUI.waitForElementVisible(consentBtn, 5, FailureHandling.OPTIONAL)) {
				println("OTP accepted. Proceeding to consent...")
				clickTab('CRDC/Login/Login.gov_ConsentGrant-Btn')
				loggedIn = true
			} else {
				println("Grant button not found — likely OTP failed. Retrying in 30s...")
				WebUI.delay(30)
				otpAttempts++
			}
		}

		if (!loggedIn) {
			KeywordUtil.markFailed("OTP failed after 2 attempts.")
		}
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

	private static String tabToSectionName(String tab) {
		if (tab == "UploadActivities") return "Upload Activities"
		if (tab == "ValidationResults") return "Validation Results"
		if (tab == "DataView") return "Data View"
		return tab
	}

	/**
	 * This function verifies the headers of the data submission dashboard's tabs: Upload Activities, Validation Results, Data View
	 */
	@Keyword
	public void verifyDataSubmissionTableHeaders(String tab) {
		try {
			//Get list of expected headers
			clickTab('CRDC/DataSubmissions/Validation/' + tab + '-Tab')
			List<String> expectedHeaders = Utils.getListFromDataFile('Data Files/CRDC/DataSubmissions/Headers-' + tab)

			//Get list of actual headers
			List<WebElement> headerList = Utils.getListWebElements('CRDC/DataSubmissions/Validation/Table-Headers')
			List<String> actualHeaders = new ArrayList<>();
			for (WebElement header : headerList) {
				actualHeaders.add(header.getText().trim());
			}

			String sectionName = tabToSectionName(tab)
			boolean anyFail = false
			int maxLen = Math.max(expectedHeaders.size(), actualHeaders.size())
			for (int i = 0; i < maxLen; i++) {
				String exp = i < expectedHeaders.size() ? expectedHeaders.get(i) : "(missing)"
				String act = i < actualHeaders.size() ? actualHeaders.get(i) : "(missing)"
				String status = (i < expectedHeaders.size() && i < actualHeaders.size() && exp.equals(act)) ? "Pass" : "Fail"
				if (status == "Fail") anyFail = true
				ValidationReport.addValidationReportItem(sectionName, "Header " + (i + 1), exp, act, status)
			}
			if (anyFail) {
				KeywordUtil.markFailed("Header mismatch. Expected: " + expectedHeaders + ", Actual: " + actualHeaders);
			} else {
				KeywordUtil.markPassed("Headers match: " + actualHeaders);
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("Exception occurred: " + e.getMessage());
		}
	}

	/**
	 * This function verifies the rows of the Data View tab matches with the metadata file
	 */
	@Keyword
	public void verifyDataViewMatchesMetadata(String metadataFile, String node) {
		KeywordUtil.logInfo("--- Verifying Data View for node: " + node + " ---")
		try {
			WebDriver driver = DriverFactory.getWebDriver();

			// Step 1: Load and parse TSV
			Path filePath = Paths.get(RunConfiguration.getProjectDir(), metadataFile);
			List<String> lines = Files.readAllLines(filePath);
			if (lines.size() <= 1) {
				KeywordUtil.markFailed("TSV file does not contain data rows.");
			}

			// Step 2: Determine which columns to keep (skip 'type' and fields with '.')
			String[] headers = lines.get(0).split("\t", -1);
			List<Integer> includedIndexes = new ArrayList<>();
			for (int j = 1; j < headers.length; j++) {
				if (!headers[j].contains(".")) {
					includedIndexes.add(j);
				}
			}

			// Step 3: Load expected values from filtered columns
			List<List<String>> expectedRows = new ArrayList<>();
			for (int i = 1; i < lines.size(); i++) {
				String[] fields = lines.get(i).split("\t", -1);
				List<String> row = new ArrayList<>();
				for (int index : includedIndexes) {
					String val = fields[index].trim();
					if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
						val = val.substring(1, val.length() - 1);
					}
					row.add(val);
				}
				expectedRows.add(row);
			}


			// Step 4: Select node from dropdown and wait for table refresh
			WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validation/NodeType-Ddn'), 30);
			WebUI.click(findTestObject('CRDC/DataSubmissions/Validation/NodeType-Ddn'));
			WebUI.delay(1);
			WebElement option = driver.findElement(By.xpath("//li[normalize-space(text())='" + node + "']"));
			option.click();
			WebUI.delay(1)

			// Step 5: Get fresh list of actual UI rows
			List<WebElement> actualRowElements = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Row");
			if (actualRowElements.size() == 20) {
				KeywordUtil.logInfo("Full table");
			}
			else if (actualRowElements.size() != expectedRows.size()) {
				KeywordUtil.markFailed("Row count mismatch: UI has " + actualRowElements.size() + ", but file has " + expectedRows.size());
			}

			// Step 6: Compare row by row and collect for report
			String sectionName = "Data View"
			boolean anyFail = false
			for (int i = 0; i < actualRowElements.size(); i++) {
				WebElement row = actualRowElements.get(i);
				List<WebElement> cells = row.findElements(By.tagName("td"));

				List<String> actualValues = new ArrayList<>();
				List<String> uiValues = new ArrayList<>();
				int uiColIndex = 0;

				for (int j = 0; j < cells.size(); j++) {
					if (j == 0 || j == 2) continue; // skip checkbox and status

					if (uiColIndex >= includedIndexes.size()) break;

					WebElement cell = cells.get(j);
					String actual;
					String uiText = "";

					try {
						WebElement span = cell.findElement(By.tagName("span"));
						String ariaLabel = span.getAttribute("aria-label");
						uiText = span.getText().trim();
						actual = (ariaLabel != null && !ariaLabel.isEmpty()) ? ariaLabel.trim() : cell.getText().trim();
					} catch (Exception e) {
						actual = cell.getText().trim();
						uiText = actual;
					}

					actualValues.add(actual);
					uiValues.add(uiText);
					uiColIndex++;
				}

				List<String> expectedValues = i < expectedRows.size() ? expectedRows.get(i) : new ArrayList<>()
				if (actualValues.size() != expectedValues.size()) {
					anyFail = true
					ValidationReport.addValidationReportItem(sectionName, "Row " + (i + 1), "columns=" + expectedValues.size(), "columns=" + actualValues.size(), "Fail")
					continue
				}

				for (int j = 0; j < expectedValues.size(); j++) {
					String expected = expectedValues.get(j)
					String actual = actualValues.get(j)
					String actualUi = uiValues.get(j)
					String cleanedUi = actualUi.replace("...", "").trim()
					boolean match = actual.equals(expected) || expected.contains(cleanedUi)
					if (!match) anyFail = true
					ValidationReport.addValidationReportItem(sectionName, "Row " + (i + 1) + " Col " + (j + 1), expected, actual, match ? "Pass" : "Fail")
				}
			}

			if (anyFail) {
				KeywordUtil.markFailed("Data View: one or more row/column mismatches. See validation report.")
			} else {
				KeywordUtil.logInfo("Finished verifying-- Data View tab in UI matches the contents of the TSV (ignoring type and reference columns).");
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("Exception in verifyDataViewMatchesMetadata(): " + e.getMessage());
		}
	}

	/**
	 * Resolve the user's system Downloads folder (cross-platform).
	 */
	private static File getUserDownloadsDir() {
		return new File(System.getProperty("user.home"), "Downloads");
	}

	/**
	 * Extract node name from an expected TSV filename.
	 * e.g. GC_consent_group_v9.0.0_expected.tsv -> consent_group
	 */
	private static String nodeFromExpectedFilename(String filename) {
		if (!filename.endsWith("_expected.tsv")) return filename.replaceFirst(~/\.[^.]+$/, "").replaceFirst(/^[^_]+_/, "")
		String base = filename - ~/_expected\.tsv$/
		base = base.replaceFirst(/_v\d+\.\d+\.\d+$/, "")
		return base.contains("_") ? base.substring(base.indexOf("_") + 1) : base
	}

	/**
	 * List expected TSV files in InputFiles/CRDC/MetadataData/&lt;tcName&gt;/expected/
	 * Returns list of [node, expectedFile] (node = extracted from filename).
	 */
	private List<List<?>> listExpectedFilesForTC(String tcName) {
		String baseDir = RunConfiguration.getProjectDir()
		File expectedDir = new File(baseDir, "InputFiles/CRDC/MetadataData/${tcName}/expected")
		if (!expectedDir.exists() || !expectedDir.isDirectory()) {
			KeywordUtil.markFailed("downloadAndCompareExpectedForTC: expected folder not found: ${expectedDir.absolutePath}")
			return []
		}
		List<File> fileList = expectedDir.listFiles()?.findAll { it.isFile() && it.name.endsWith("_expected.tsv") } ?: []
		if (fileList.isEmpty()) {
			KeywordUtil.markFailed("downloadAndCompareExpectedForTC: no *_expected.tsv files in ${expectedDir.absolutePath}")
			return []
		}
		List<List<?>> out = []
		for (File f in fileList) {
			String node = nodeFromExpectedFilename(f.name)
			out.add([node, f.absolutePath])
		}
		return out
	}

	/**
	 * Find the most recently modified file in a directory.
	 */
	private static File findLatestFile(File dir) {
		List<File> files = dir.listFiles()?.findAll { it.isFile() } ?: []
		if (files.isEmpty()) return null
		return files.sort { -it.lastModified() }.get(0)
	}

	/**
	 * Compare two TSV files in full: headers and all data rows, cell-by-cell (trimmed).
	 * Collects all mismatches, logs them, then fails once at the end with the full output (no fail on first mismatch).
	 */
	private void compareTsvFullContent(String expectedPath, String actualPath) {
		Path expPath = Paths.get(expectedPath)
		Path actPath = Paths.get(actualPath)
		if (!Files.exists(expPath)) {
			KeywordUtil.markFailed("Expected file not found: " + expectedPath)
			return
		}
		if (!Files.exists(actPath)) {
			KeywordUtil.markFailed("Downloaded file not found: " + actualPath)
			return
		}
		List<String> expLines = Files.readAllLines(expPath)
		List<String> actLines = Files.readAllLines(actPath)
		List<String> errors = new ArrayList<>()
		String sectionName = "Data View (Expected vs Downloaded)"
		if (expLines.size() != actLines.size()) {
			errors.add("TSV row count mismatch: expected ${expLines.size()}, actual ${actLines.size()} (${actualPath})")
			ValidationReport.addValidationReportItem(sectionName, "Row count", String.valueOf(expLines.size()), String.valueOf(actLines.size()), "Fail")
		}
		int maxRows = Math.min(expLines.size(), actLines.size())
		for (int i = 0; i < maxRows; i++) {
			String expLine = expLines.get(i)
			String actLine = actLines.get(i)
			String[] expCells = expLine.split("\t", -1)
			String[] actCells = actLine.split("\t", -1)
			boolean lineOk = true
			if (expCells.length != actCells.length) {
				errors.add("Line ${i + 1}: column count expected ${expCells.length}, actual ${actCells.length}")
				lineOk = false
			}
			int maxCols = Math.min(expCells.length, actCells.length)
			for (int j = 0; j < maxCols; j++) {
				String expVal = expCells[j].trim()
				String actVal = actCells[j].trim()
				if (expVal != actVal) {
					errors.add("Line ${i + 1}, column ${j + 1}: expected '${expVal}', actual '${actVal}'")
					lineOk = false
				}
			}
			if (expCells.length != actCells.length) {
				for (int j = maxCols; j < Math.max(expCells.length, actCells.length); j++) {
					String expVal = j < expCells.length ? expCells[j].trim() : "(missing)"
					String actVal = j < actCells.length ? actCells[j].trim() : "(missing)"
					errors.add("Line ${i + 1}, column ${j + 1}: expected '${expVal}', actual '${actVal}'")
				}
				lineOk = false
			}
			String expShort = expLine.length() > 120 ? expLine.substring(0, 120) + "..." : expLine
			String actShort = actLine.length() > 120 ? actLine.substring(0, 120) + "..." : actLine
			ValidationReport.addValidationReportItem(sectionName, "Line " + (i + 1), expShort, actShort, lineOk ? "Pass" : "Fail")
		}
		if (!errors.isEmpty()) {
			KeywordUtil.logInfo("--- TSV comparison output for ${actualPath} (${errors.size()} issue(s)) ---")
			for (String err in errors) {
				KeywordUtil.logInfo(err)
			}
			KeywordUtil.logInfo("--- end comparison output ---")
			KeywordUtil.markFailed("TSV comparison failed with ${errors.size()} mismatch(es). See log output above for full details.")
		} else {
			KeywordUtil.logInfo("TSV full content match: ${actualPath}")
		}
	}

	/**
	 * For a given TC (e.g. TC01): for each node with an expected file, select node, click Download,
	 * move the latest file from user's Downloads to Downloads/&lt;tcName&gt;, then compare expected vs downloaded TSV (headers + full content).
	 */
	@Keyword
	void downloadAndCompareExpectedForTC(String tcName) {
		WebDriver driver = DriverFactory.getWebDriver()
		String projectDir = RunConfiguration.getProjectDir()
		File userDownloads = getUserDownloadsDir()
		File downloadsDir = new File(projectDir, "Downloads/${tcName}")
		if (!downloadsDir.exists()) {
			downloadsDir.mkdirs()
			KeywordUtil.logInfo("Created downloads folder: ${downloadsDir.absolutePath}")
		}
		List<List<?>> expectedList = listExpectedFilesForTC(tcName)
		if (expectedList.isEmpty()) return
			for (List<?> pair in expectedList) {
				String node = (String) pair.get(0)
				String expectedPath = (String) pair.get(1)
				WebUI.waitForElementClickable(findTestObject("CRDC/DataSubmissions/Validation/NodeType-Ddn"), 30)
				WebUI.click(findTestObject("CRDC/DataSubmissions/Validation/NodeType-Ddn"))
				WebUI.delay(1)
				WebElement option = driver.findElement(By.xpath("//li[normalize-space(text())='" + node + "']"))
				option.click()
				WebUI.delay(1)
				WebUI.waitForElementClickable(findTestObject("CRDC/DataSubmissions/Validation/Download-Btn"), 10)
				WebUI.click(findTestObject("CRDC/DataSubmissions/Validation/Download-Btn"))
				WebUI.delay(5)
				File latest = findLatestFile(userDownloads)
				if (latest == null) {
					KeywordUtil.markFailed("downloadAndCompareExpectedForTC: no file found in user Downloads (${userDownloads.absolutePath}) after downloading node ${node}")
					return
				}
				File destFile = new File(downloadsDir, latest.name)
				try {
					FileUtils.moveFile(latest, destFile)
				} catch (Exception e) {
					KeywordUtil.markFailed("downloadAndCompareExpectedForTC: failed to move file to ${destFile.absolutePath}: " + e.getMessage())
					return
				}
				compareTsvFullContent(expectedPath, destFile.absolutePath)
			}
	}

	// ------------------------------- Validation Results vs expected TSV (by node, severity, issue type) -------------------------------

	/**
	 * Extracts the node name from an expected Validation Results TSV filename.
	 * Example: "GC_consent_group_v9.0.0_expected-ValidationResults.tsv" -> "consent_group".
	 * Strips the "_expected-ValidationResults.tsv" suffix, then the version (_vX.Y.Z), then returns the part after the first underscore (prefix like "GC_" is dropped).
	 * Returns empty string if filename does not end with "_expected-ValidationResults.tsv".
	 */
	private static String nodeFromExpectedValidationResultsFilename(String filename) {
		if (!filename.endsWith("_expected-ValidationResults.tsv")) return ""
		String base = filename - ~/_expected-ValidationResults\.tsv$/
		base = base.replaceFirst(/_v\d+\.\d+\.\d+$/, "")
		return base.contains("_") ? base.substring(base.indexOf("_") + 1) : base
	}

	/** Load node -> submitted-identifier column name from Data File (Data Files/CRDC/DataSubmissions/ValidationResults-Mappings). */
	private Map<String, String> loadValidationResultsMappings() {
		Map<String, String> nodeToColumn = new HashMap<>()
		try {
			def data = findTestData("Data Files/CRDC/DataSubmissions/ValidationResults-Mappings")
			int rows = data.getRowNumbers()
			String[] colNames = data.getColumnNames()
			String nodeCol = (colNames != null && colNames.length >= 1) ? colNames[0] : null
			String columnCol = (colNames != null && colNames.length >= 2) ? colNames[1] : null
			for (int i = 1; i <= rows; i++) {
				String nodeVal = nodeCol ? data.getValue(nodeCol, i) : data.getValue(1, i)
				String colVal = columnCol ? data.getValue(columnCol, i) : data.getValue(2, i)
				if (nodeVal != null && colVal != null) nodeToColumn.put(nodeVal.trim(), colVal.trim())
			}
		} catch (Exception e) {
			KeywordUtil.logInfo("Loading ValidationResults-Mappings from Excel fallback: " + e.getMessage())
			String path = RunConfiguration.getProjectDir() + "/InputFiles/CRDC/DataSubmissionValidation/DSV_ValidationResults-mappings.xlsx"
			File f = new File(path)
			if (f.exists()) {
				Workbook wb = WorkbookFactory.create(f)
				Sheet sh = wb.getSheet("GC")
				if (sh != null) {
					for (Row r : sh) {
						if (r.getCell(0) != null && r.getCell(1) != null) {
							String nodeVal = r.getCell(0).toString().trim()
							String colVal = r.getCell(1).toString().trim()
							if (!nodeVal.isEmpty() && !colVal.isEmpty()) nodeToColumn.put(nodeVal, colVal)
						}
					}
				}
				wb.close()
			}
		}
		return nodeToColumn
	}

	/** Find expected TSV file path for the given node in InputFiles/CRDC/MetadataData/&lt;tcName&gt;/expected-ValidationResults/ */
	private String findExpectedValidationResultsTsv(String tcName, String node) {
		String baseDir = RunConfiguration.getProjectDir()
		File dir = new File(baseDir, "InputFiles/CRDC/MetadataData/${tcName}/expected-ValidationResults")
		if (!dir.exists() || !dir.isDirectory()) return null
		List<File> files = dir.listFiles()?.findAll { it.isFile() && it.name.endsWith("_expected-ValidationResults.tsv") } ?: []
		for (File f in files) {
			if (nodeFromExpectedValidationResultsFilename(f.name) == node) return f.absolutePath
		}
		return null
	}

	/** List unique node names from expected-ValidationResults folder for the given TC. Returns empty list if folder missing or no files. */
	private List<String> listNodesFromExpectedValidationResultsFolder(String tcName) {
		String baseDir = RunConfiguration.getProjectDir()
		File dir = new File(baseDir, "InputFiles/CRDC/MetadataData/${tcName}/expected-ValidationResults")
		KeywordUtil.logInfo("verifyValidationResultsVsExpectedForTC: looking in " + dir.absolutePath)
		if (!dir.exists() || !dir.isDirectory()) {
			KeywordUtil.logInfo("verifyValidationResultsVsExpectedForTC: folder does not exist or is not a directory.")
			return Collections.emptyList()
		}
		List<File> files = dir.listFiles()?.findAll { it.isFile() && it.name.endsWith("_expected-ValidationResults.tsv") } ?: []
		if (files.isEmpty()) KeywordUtil.logInfo("verifyValidationResultsVsExpectedForTC: no *_expected-ValidationResults.tsv files found.")
		Set<String> nodes = new LinkedHashSet<>()
		for (File f in files) {
			String node = nodeFromExpectedValidationResultsFilename(f.name)
			if (!node.isEmpty()) nodes.add(node)
		}
		return new ArrayList<>(nodes)
	}

	/** Get unique (severity, issueType) pairs from an expected Validation Results TSV. Returns list of [severity, issueType]. */
	private List<List<String>> getUniqueSeverityIssueTypePairsFromExpectedTsv(String tsvPath) {
		List<List<String>> out = new ArrayList<>()
		Path path = Paths.get(tsvPath)
		if (!Files.exists(path)) return out
		List<String> lines = Files.readAllLines(path)
		if (lines.size() < 2) return out
		String[] headers = lines.get(0).split("\t", -1)
		int idxIssue = -1, idxSev = -1
		for (int i = 0; i < headers.length; i++) {
			String h = headers[i].trim()
			if (h.equals("Issue_Type")) idxIssue = i
			if (h.equals("Expected_Severity")) idxSev = i
		}
		if (idxIssue < 0 || idxSev < 0) return out
		Set<String> seen = new LinkedHashSet<>()
		for (int i = 1; i < lines.size(); i++) {
			String[] cells = lines.get(i).split("\t", -1)
			String rowIssue = idxIssue < cells.length ? cells[idxIssue].trim() : ""
			String rowSev = idxSev < cells.length ? cells[idxSev].trim() : ""
			if (rowSev.isEmpty() || rowIssue.isEmpty()) continue
				String key = rowSev + "\t" + rowIssue
			if (seen.add(key)) out.add(Arrays.asList(rowSev, rowIssue))
		}
		return out
	}

	/** Load expected rows from TSV filtered by severity and issue type. Returns list of [submittedId, issueType, severity]. */
	private List<List<String>> loadExpectedValidationResultsTsv(String tsvPath, String submittedIdentifierColumn, String severity, String issueType) {
		List<List<String>> out = new ArrayList<>()
		Path path = Paths.get(tsvPath)
		if (!Files.exists(path)) return out
		List<String> lines = Files.readAllLines(path)
		if (lines.size() < 2) return out
		String[] headers = lines.get(0).split("\t", -1)
		int idxId = -1, idxIssue = -1, idxSev = -1
		for (int i = 0; i < headers.length; i++) {
			String h = headers[i].trim()
			if (h.equalsIgnoreCase(submittedIdentifierColumn)) idxId = i
			if (h.equals("Issue_Type")) idxIssue = i
			if (h.equals("Expected_Severity")) idxSev = i
		}
		if (idxId < 0 || idxIssue < 0 || idxSev < 0) return out
		for (int i = 1; i < lines.size(); i++) {
			String[] cells = lines.get(i).split("\t", -1)
			String rowIssue = idxIssue < cells.length ? cells[idxIssue].trim() : ""
			String rowSev = idxSev < cells.length ? cells[idxSev].trim() : ""
			if (!rowIssue.equals(issueType) || !rowSev.equalsIgnoreCase(severity)) continue
				String id = idxId < cells.length ? cells[idxId].trim() : ""
			out.add(Arrays.asList(id, rowIssue, rowSev))
		}
		return out
	}

	/** Apply Validation Results filters (node, severity, issue type) and read table rows: [submittedId, issueType, severity]. */
	private List<List<String>> readValidationResultsTableFromUI(String node, String severity, String issueType) {
		WebDriver driver = DriverFactory.getWebDriver()
		WebUI.delay(1)
		WebUI.waitForElementClickable(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_NodeType-Ddn"), 15)
		WebUI.click(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_NodeType-Ddn"))
		WebUI.delay(1)
		WebElement nodeOpt = driver.findElement(By.xpath("//li[normalize-space(text())='" + node + "']"))
		nodeOpt.click()
		WebUI.delay(1)
		WebUI.waitForElementClickable(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_Severity-Ddn"), 10)
		WebUI.click(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_Severity-Ddn"))
		WebUI.delay(1)
		WebElement sevOpt = driver.findElement(By.xpath("//li[normalize-space(text())='" + severity + "']"))
		sevOpt.click()
		WebUI.delay(1)
		WebUI.waitForElementClickable(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_IssueType-Ddn"), 10)
		WebUI.click(findTestObject("CRDC/DataSubmissions/Validation/ValidationResults_IssueType-Ddn"))
		WebUI.delay(1)
		WebElement issueOpt = driver.findElement(By.xpath("//li[normalize-space(text())='" + issueType + "']"))
		issueOpt.click()
		WebUI.delay(2)
		// Wait for table to finish loading after filter change; then a short delay so content can settle.
		Utils.waitForElementToDisappear('Object Repository/CRDC/DataSubmissions/Loading-Icon', 15)
		WebUI.delay(1)
		// Stabilize table: after changing node/severity/issueType the UI re-renders the table. If we read rows
		// before the DOM has settled, we can get StaleElementReferenceException when using row/cell references.
		// Wait until row count is unchanged across two samples 0.5s apart (up to 5 tries).
		int rowCount = 0
		for (int w = 0; w < 5; w++) {
			List<WebElement> waitRows = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Row")
			int n = waitRows != null ? waitRows.size() : 0
			WebUI.delay(0.5)
			waitRows = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Row")
			int n2 = waitRows != null ? waitRows.size() : 0
			if (n == n2) {
				rowCount = n
				break
			}
		}
		List<WebElement> headerEls = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Headers")
		List<String> headerNames = new ArrayList<>()
		for (WebElement h : headerEls) headerNames.add(h.getText().trim())
		int idxSubmitted = headerNames.findIndexOf { it.equalsIgnoreCase("Submitted Identifier") }
		int idxIssue = headerNames.findIndexOf { it.equalsIgnoreCase("Issue Type") }
		if (idxIssue < 0) idxIssue = headerNames.findIndexOf { it.equalsIgnoreCase("Issue(s)") }
		if (idxIssue < 0) idxIssue = headerNames.findIndexOf { it.toLowerCase().contains("issue") }
		int idxSev = headerNames.findIndexOf { it.equalsIgnoreCase("Severity") }
		if (idxSubmitted < 0 || idxSev < 0) {
			KeywordUtil.markFailed("Validation Results table missing required columns (Submitted Identifier, Severity). Headers: " + headerNames)
			return Collections.emptyList()
		}
		if (idxIssue < 0) idxIssue = -1
		List<WebElement> rows = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Row")
		// If stabilization loop never saw a stable count (e.g. table kept changing), use current row count
		if (rowCount <= 0 && rows != null) rowCount = rows.size()
		List<List<String>> out = new ArrayList<>()
		// Iterate by index and re-fetch the row list each time. Holding a single list of WebElement rows and
		// iterating over it can cause stale refs if the table re-renders (e.g. virtualization or delayed update).
		// Re-querying rows for each index gives us a fresh reference for that row.
		for (int i = 0; i < rowCount; i++) {
			boolean done = false
			// Retry up to 3 times on StaleElementReferenceException: the DOM may update while we read this row.
			for (int attempt = 1; attempt <= 3 && !done; attempt++) {
				try {
					List<WebElement> currentRows = Utils.getListWebElements("CRDC/DataSubmissions/Validation/Table-Row")
					if (i >= (currentRows != null ? currentRows.size() : 0)) break
					WebElement row = currentRows.get(i)
					List<WebElement> cells = row.findElements(By.tagName("td"))
					String submitted = getCellText(cells, idxSubmitted, true)
					String issue = getCellText(cells, idxIssue, false)
					String sev = getCellText(cells, idxSev, false)
					out.add(Arrays.asList(submitted, issue, sev))
					done = true
				} catch (StaleElementReferenceException e) {
					if (attempt == 3) throw e
					WebUI.delay(0.5)
				}
			}
		}
		return out
	}

	/** If the cell's visible text is this many characters or less, we use it as-is and skip DOM lookups for full value (truncated-text-wrapper / span). */
	private static final int TRUNCATED_CHECK_THRESHOLD = 15

	/**
	 * Returns the text to use for a table cell at the given column index. Used when reading Validation Results (and similar) tables from the UI.
	 * The UI may show truncated values (e.g. "auto_CDS2016_DN..."); the full value is in a [data-testid='truncated-text-wrapper'] element's aria-label, or in a span's aria-label/text.
	 * For short values (length <= TRUNCATED_CHECK_THRESHOLD), returns the cell's getText() to avoid extra findElements. For longer values, tries truncated-text-wrapper aria-label, then span aria-label/text, then cell text.
	 * Uses findElements (not findElement) so we never block on implicit wait when the element is absent.
	 * @param cells list of td WebElements for one row
	 * @param index column index (0-based)
	 * @param debug if true, log which path was used to the console
	 * @return trimmed string for the cell, or "" if index invalid or on exception
	 */
	private static String getCellText(List<WebElement> cells, int index, boolean debug) {
		if (index < 0 || index >= cells.size()) return ""
		try {
			WebElement cell = cells.get(index)
			String cellRawText = cell.getText().trim()
			// If value is short enough, no need to look for truncated wrapper or span—use table text as-is
			if (cellRawText != null && cellRawText.length() <= TRUNCATED_CHECK_THRESHOLD) {
				if (debug) KeywordUtil.logInfo("[getCellText] cell index=" + index + ", short value (" + cellRawText.length() + " chars), using as-is='" + cellRawText + "'")
				return cellRawText
			}
			List<WebElement> truncatedWrappers = cell.findElements(By.cssSelector("[data-testid='truncated-text-wrapper']"))
			if (debug) {
				KeywordUtil.logInfo("[getCellText] cell index=" + index + ", raw cell.getText()='" + cellRawText + "', truncated-text-wrapper count=" + truncatedWrappers.size())
			}
			if (truncatedWrappers != null && !truncatedWrappers.isEmpty()) {
				WebElement first = truncatedWrappers.get(0)
				String aria = first.getAttribute("aria-label")
				if (debug) KeywordUtil.logInfo("[getCellText] using truncated-text-wrapper aria-label='" + (aria != null ? aria : "null") + "'")
				if (aria != null && !aria.isEmpty()) return aria.trim()
			}
			// Use findElements (not findElement) so we never block for implicit wait when cell has no span
			List<WebElement> spans = cell.findElements(By.tagName("span"))
			if (spans != null && !spans.isEmpty()) {
				WebElement span = spans.get(0)
				String aria = span.getAttribute("aria-label")
				if (debug) KeywordUtil.logInfo("[getCellText] fallback: span aria-label='" + (aria != null ? aria : "null") + "', span.getText()='" + span.getText().trim() + "'")
				if (aria != null && !aria.isEmpty()) return aria.trim()
				return span.getText().trim()
			}
			if (debug) KeywordUtil.logInfo("[getCellText] fallback: no span, using cell text='" + cellRawText + "'")
			return cellRawText
		} catch (Exception e) {
			if (debug) KeywordUtil.logInfo("[getCellText] exception: " + e.getMessage())
			return ""
		}
	}

	/**
	 * Compare Validation Results UI (filtered by node, severity, issue type) to expected TSV for that node.
	 * Expected TSV: InputFiles/CRDC/MetadataData/&lt;tcName&gt;/expected-ValidationResults/GC_*_node_*_expected-ValidationResults.tsv.
	 * Matching by Submitted Identifier (column per node from Data Files/CRDC/DataSubmissions/ValidationResults-Mappings).
	 */
	@Keyword
	void verifyValidationResultsVsExpected(String tcName, String node, String severity, String issueType) {
		KeywordUtil.logInfo("--- Verifying Validation Results for node: " + node + ", severity: " + severity + ", issueType: " + issueType + " ---")
		Map<String, String> mappings = loadValidationResultsMappings()
		String submittedIdColumn = mappings.get(node)
		if (submittedIdColumn == null || submittedIdColumn.isEmpty()) {
			KeywordUtil.markFailed("verifyValidationResultsVsExpected: no mapping for node '" + node + "' in ValidationResults-Mappings")
			return
		}
		String tsvPath = findExpectedValidationResultsTsv(tcName, node)
		if (tsvPath == null) {
			KeywordUtil.markFailed("verifyValidationResultsVsExpected: no expected TSV for node '" + node + "' in expected-ValidationResults folder")
			return
		}
		List<List<String>> expectedRows = loadExpectedValidationResultsTsv(tsvPath, submittedIdColumn, severity, issueType)
		List<List<String>> actualRows = readValidationResultsTableFromUI(node, severity, issueType)
		Map<String, List<String>> actualBySubmittedId = new LinkedHashMap<>()
		for (List<String> row : actualRows) {
			if (row.size() >= 1 && !row.get(0).isEmpty()) actualBySubmittedId.put(row.get(0).trim(), row)
		}
		List<String> errors = new ArrayList<>()
		String sectionName = "Validation Results"
		
		for (List<String> exp : expectedRows) {
			String submittedId = exp.get(0)
			List<String> act = actualBySubmittedId.get(submittedId)
			if (act == null) {
				errors.add("Submitted Identifier '" + submittedId + "': expected in UI but not found")
				ValidationReport.addValidationReportItem(sectionName, "Submitted ID " + submittedId, "Issue=" + exp.get(1) + ", Severity=" + exp.get(2), "(not found in UI)", "Fail")
				continue
			}
			String expIssue = exp.get(1), expSev = exp.get(2)
			String actIssue = act.size() >= 2 ? act.get(1) : ""
			String actSev = act.size() >= 3 ? act.get(2) : ""
			boolean issueOk = actIssue.equals(expIssue)
			boolean sevOk = actSev.equalsIgnoreCase(expSev)
			if (!issueOk) errors.add("Submitted Identifier '" + submittedId + "': Issue Type expected '" + expIssue + "', actual '" + actIssue + "'")
			if (!sevOk) errors.add("Submitted Identifier '" + submittedId + "': Severity expected '" + expSev + "', actual '" + actSev + "'")
			String status = (issueOk && sevOk) ? "Pass" : "Fail"
			ValidationReport.addValidationReportItem(sectionName, "Submitted ID " + submittedId, "Issue=" + expIssue + ", Severity=" + expSev, "Issue=" + actIssue + ", Severity=" + actSev, status)
		}
		for (String submittedId : actualBySubmittedId.keySet()) {
			boolean found = expectedRows.any { it.get(0).equals(submittedId) }
			if (!found) {
				errors.add("Submitted Identifier '" + submittedId + "': present in UI but not in expected TSV")
				List<String> act = actualBySubmittedId.get(submittedId)
				String actIssue = act.size() >= 2 ? act.get(1) : ""
				String actSev = act.size() >= 3 ? act.get(2) : ""
				ValidationReport.addValidationReportItem(sectionName, "Submitted ID " + submittedId, "(not in expected)", "Issue=" + actIssue + ", Severity=" + actSev, "Fail")
			}
		}
		if (!errors.isEmpty()) {
			KeywordUtil.logInfo("--- Validation Results vs Expected (" + node + ", " + severity + ", " + issueType + ") - " + errors.size() + " issue(s) ---")
			for (String err : errors) KeywordUtil.logInfo(err)
			KeywordUtil.logInfo("--- end ---")
			KeywordUtil.markFailed("verifyValidationResultsVsExpected: " + errors.size() + " mismatch(es). See log above.")
		} else {
			KeywordUtil.logInfo("Validation Results match expected for node=" + node + ", severity=" + severity + ", issueType=" + issueType)
		}
	}

	/**
	 * One-call Validation Results verification for a test case: discovers all expected-ValidationResults TSV files
	 * under InputFiles/CRDC/MetadataData/&lt;tcName&gt;/expected-ValidationResults/, derives nodes and (severity, issueType)
	 * pairs from those files, and runs verifyValidationResultsVsExpected for each (node, severity, issueType).
	 * Use this in the test case instead of multiple 4-arg calls.
	 */
	@Keyword
	void verifyValidationResultsVsExpectedForTC(String tcName) {

		clickTab("CRDC/DataSubmissions/Validation/ValidationResults-Tab")
		clickTab("Object Repository/CRDC/DataSubmissions/Validation/ValidationResults_AggregatedExpanded-Btn")

		List<String> nodes = listNodesFromExpectedValidationResultsFolder(tcName)
		if (nodes.isEmpty()) {
			KeywordUtil.logInfo("verifyValidationResultsVsExpectedForTC: No expected Validation Results files for TC '" + tcName + "'; skipping.")
			return
		}
		for (String node : nodes) {
			String tsvPath = findExpectedValidationResultsTsv(tcName, node)
			if (tsvPath == null) {
				KeywordUtil.logInfo("verifyValidationResultsVsExpectedForTC: No expected TSV for node '" + node + "'; skipping.")
				continue
			}
			List<List<String>> pairs = getUniqueSeverityIssueTypePairsFromExpectedTsv(tsvPath)
			for (List<String> pair : pairs) {
				String severity = pair.get(0)
				String issueType = pair.get(1)
				verifyValidationResultsVsExpected(tcName, node, severity, issueType)
			}
		}
	}

	// ===============================
	//   FULL MEGA-WRAPPER TEMPLATE
	//   Paste into your CrdcDH.groovy
	// ===============================
	// ======================================
	// 1. Dynamic TestObject Builder
	//	// ======================================
	//
	TestObject makeTO(String xpath) {
		TestObject to = new TestObject(xpath)
		to.addProperty("xpath", ConditionType.EQUALS, xpath)
		return to
	}

	// ======================================
	// 2. Excel Loader (FINAL PRODUCTION VERSION)
	// ======================================
	@Keyword
	Map loadExcelMaster(String excelPath) {

		Map master = [:]
		Workbook wb = WorkbookFactory.create(new File(excelPath))

		// -------------------------
		// Load Aggregated Headers
		// -------------------------
		master.aggregatedHeaders = []
		wb.getSheet("aggregated_headers").each { Row r ->
			if (r.getCell(0)) {
				master.aggregatedHeaders << r.getCell(0).toString().trim()
			}
		}

		// -------------------------
		// Load Expanded Headers
		// -------------------------
		master.expandedHeaders = []
		wb.getSheet("expanded_headers").each { Row r ->
			if (r.getCell(0)) {
				master.expandedHeaders << r.getCell(0).toString().trim()
			}
		}

		// -------------------------
		// Load IssueType dropdown
		// -------------------------
		master.issueTypes = []
		wb.getSheet("dropdown_issueType").each { Row r ->
			if (r.getCell(0)) {
				master.issueTypes << r.getCell(0).toString().trim()
			}
		}

		// -------------------------
		// Load NodeType dropdown
		// -------------------------
		master.nodeTypes = []
		wb.getSheet("dropdown_nodeType").each { Row r ->
			if (r.getCell(0)) {
				master.nodeTypes << r.getCell(0).toString().trim()
			}
		}

		// -------------------------
		// Load Severities dropdown
		// -------------------------
		master.severities = []
		wb.getSheet("dropdown_severity").each { Row r ->
			if (r.getCell(0)) {
				master.severities << r.getCell(0).toString().trim()
			}
		}

		// -------------------------
		// Load IssueType → ExpectedSeverity Mapping
		// -------------------------
		master.issueTypeSeverityMap = [:]   // <IssueType : ExpectedSeverity>

		Sheet mapSheet = wb.getSheet("issueTypeSeverityMap")
		if (!mapSheet) {
			KeywordUtil.markFailed("❌ Excel Sheet 'issueTypeSeverityMap' NOT FOUND!")
		}

		mapSheet.each { Row r ->
			Cell c1 = r.getCell(0)
			Cell c2 = r.getCell(1)

			if (c1 && c2) {
				String issue = c1.toString().trim()
				String sev   = c2.toString().trim()
				master.issueTypeSeverityMap[issue] = sev
			}
		}

		println "\n=== Loaded Master Excel Configuration ==="
		println "Aggregated Headers: ${master.aggregatedHeaders}"
		println "Expanded Headers:   ${master.expandedHeaders}"
		println "Issue Types:        ${master.issueTypes}"
		println "Node Types:         ${master.nodeTypes}"
		println "Severities:         ${master.severities}"
		println "Mapping:            ${master.issueTypeSeverityMap}"

		return master
	}
	//	// ==========================================================================
	//	// 3. EXPANDED MODE SUPPORT HELPERS (REQUIRED FOR EXPANDED VALIDATION)
	//	// ==========================================================================
	//
	//	// -------------------------------------------------------
	//	// 3.1 Safe Cell Reader (prevents null failures)
	//	// -------------------------------------------------------
	//	String safeCell(WebElement row, int index) {
	//		try {
	//			return row.findElement(By.xpath("./td[" + index + "]")).getText().trim()
	//		} catch (Exception e) {
	//			return ""
	//		}
	//	}
	//
	//
	//	// -------------------------------------------------------
	//	// 3.2 Toggle to Expanded Mode (this integrates YOUR WORKING VERSION)
	//	// -------------------------------------------------------
	//	@Keyword 	void switchToExpandedMode() {
	//
	//		println "\n>>> Switching to EXPANDED mode..."
	//
	//		TestObject toggle = makeTO("//*[@data-testid='toggle-input']")
	//
	//		try {
	//			WebUI.waitForElementClickable(toggle, 10)
	//			WebUI.click(toggle)
	//			println("✔ Toggle clicked normally")
	//		}
	//		catch (Exception e) {
	//			println("⚠ Normal toggle click failed — trying JS fallback...")
	//			try {
	//				WebElement el = WebUI.findWebElement(toggle, 10)
	//				WebUI.executeJavaScript("arguments[0].click();", Arrays.asList(el))
	//				println("✔ Toggle clicked with JavaScript")
	//			}
	//			catch (Exception ex) {
	//				KeywordUtil.markFailed("❌ Toggle NOT clickable even with JS fallback")
	//			}
	//		}
	//
	//		WebUI.delay(2)
	//	}
	//
	//
	//	// -------------------------------------------------------
	//	// 3.3 Generic Dropdown Validator (OPTION A LOGIC)
	//	// -------------------------------------------------------
	//	@Keyword 	void validateDropdown(String ddName, String ddXpath, List<String> rowValues) {
	//
	//		println "\nChecking '${ddName}' dropdown..."
	//
	//		TestObject dropdown = makeTO(ddXpath)
	//
	//		// Try normal click first
	//		try {
	//			WebUI.waitForElementClickable(dropdown, 8)
	//			WebUI.click(dropdown)
	//		} catch (Exception e) {
	//			println "⚠ Normal click failed — using JS fallback"
	//			try {
	//				WebElement el = WebUI.findWebElement(dropdown, 5)
	//				WebUI.executeJavaScript("arguments[0].click();", Arrays.asList(el))
	//			} catch (Exception ex) {
	//				KeywordUtil.markFailed("❌ Unable to click dropdown '${ddName}'")
	//			}
	//		}
	//
	//		WebUI.delay(1)
	//
	//		// Read UI dropdown options
	//		List<WebElement> opts = WebUI.findWebElements(makeTO("//*[@role='option']"), 10)
	//		List<String> uiOptions = opts.collect { it.getText().trim() }
	//
	//		// Expected = UNIQUE row values + "All"
	//		Set<String> expected = rowValues.toSet()
	//		expected.add("All")
	//
	//		expected.each { exp ->
	//			if (!uiOptions.contains(exp)) {
	//				KeywordUtil.markFailed("❌ '${exp}' NOT found in '${ddName}' dropdown")
	//			} else {
	//				println "✔ '${exp}' validated in '${ddName}' dropdown"
	//			}
	//		}
	//
	//		// Close dropdown
	//		try {
	//			WebUI.sendKeys(makeTO("//body"), Keys.chord(Keys.ESCAPE))
	//			WebUI.delay(1)
	//			println "✔ Dropdown '${ddName}' closed"
	//		}
	//		catch (Exception e) {
	//			println "⚠ Could not close dropdown '${ddName}' — ESC ignored"
	//		}
	//	}
	//
	//
	//	@Keyword
	//	void validateExpandedView(Map master) {
	//
	//		println "\n===== START EXPANDED MODE VALIDATION ====="
	//
	//		//----------------------------------------------------------------------
	//		// 1. VALIDATE HEADERS
	//		//----------------------------------------------------------------------
	//		List<WebElement> headerEls = WebUI.findWebElements(
	//				makeTO("//*[@data-testid='generic-table-header-cell']"),
	//				10
	//				)
	//
	//		List<String> uiHeaders = headerEls.collect { it.getText().trim() }
	//		println "UI Headers: ${uiHeaders}"
	//
	//		master.expandedHeaders.each { expected ->
	//			if (!uiHeaders.contains(expected)) {
	//				KeywordUtil.markFailed("❌ Expanded header '${expected}' NOT found")
	//			} else {
	//				println "✔ Header '${expected}' present"
	//			}
	//		}
	//
	//		// Build dynamic row column index map
	//		Map<String, Integer> colIndex = [:]
	//		uiHeaders.eachWithIndex { h, i -> colIndex[h] = i + 1 }
	//
	//		println "Expanded Column Index Map = ${colIndex}"
	//
	//
	//		//----------------------------------------------------------------------
	//		// 2. GET ALL ROW DATA (Used for dropdown validation)
	//		//----------------------------------------------------------------------
	//		List<WebElement> rows = WebUI.findWebElements(
	//				makeTO("//*[@data-testid='generic-table-row']"),
	//				10
	//				)
	//
	//		if (rows.isEmpty()) {
	//			println "⚠ No expanded rows available — dropdown validation only"
	//		}
	//
	//		List<String> issueTypeValues = rows.collect { safeCell(it, colIndex["Issue Type"]) }
	//		List<String> batchIdValues   = rows.collect { safeCell(it, colIndex["Batch ID"]) }
	//		List<String> nodeTypeValues  = rows.collect { safeCell(it, colIndex["Node Type"]) }
	//		List<String> severityValues  = rows.collect { safeCell(it, colIndex["Severity"]) }
	//
	//
	//		//----------------------------------------------------------------------
	//		// 3. VALIDATE DROPDOWNS
	//		//    ✔ IssueType → validate against Excel
	//		//    ✔ NodeType → validate against Excel
	//		//    ✔ Severity → validate against Excel
	//		//    ❗ Batch ID → ONLY LOG VALUES (no Excel validation)
	//		//----------------------------------------------------------------------
	//
	//		println "\n===== VALIDATING EXPANDED MODE DROPDOWNS ====="
	//
	//		// ISSUE TYPE
	//		validateDropdownAgainstExcel(
	//				"issueType",
	//				"//*[@id='mui-component-select-issueType']",
	//				master.issueTypes
	//				)
	//
	//		// BATCH ID (SKIP EXCEL)
	//		validateDropdownOnlyLog(
	//				"batchID",
	//				"//*[@id='mui-component-select-batchID']"
	//				)
	//
	//		// NODE TYPE
	//		validateDropdownAgainstExcel(
	//				"nodeType",
	//				"//*[@id='mui-component-select-nodeType']",
	//				master.nodeTypes
	//				)
	//
	//		// SEVERITY
	//		validateDropdownAgainstExcel(
	//				"severity",
	//				"//*[@id='mui-component-select-severity']",
	//				master.severities
	//				)
	//
	//
	//		//----------------------------------------------------------------------
	//		// 4. VALIDATE EACH EXPANDED ROW
	//		//----------------------------------------------------------------------
	//		if (!rows.isEmpty()) {
	//
	//			println "\n===== VALIDATING EXPANDED MODE ROWS ====="
	//			println "✔ Found ${rows.size()} rows"
	//
	//			rows.eachWithIndex { row, idx ->
	//
	//				println "\nRow ${idx + 1}:"
	//				String issueType = safeCell(row, colIndex["Issue Type"])
	//				String severity  = safeCell(row, colIndex["Severity"])
	//				String issues    = safeCell(row, colIndex["Issue(s)"])
	//
	//				println "  Issue Type : ${issueType}"
	//				println "  Severity   : ${severity}"
	//				println "  Issue(s)   : ${issues}"
	//
	//				// Mapping check
	//				String expected = master.issueTypeSeverityMap[issueType]
	//
	//				if (!expected) {
	//					KeywordUtil.markFailed("❌ No Excel mapping found for IssueType '${issueType}'")
	//				}
	//
	//				if (!severity.equalsIgnoreCase(expected)) {
	//					KeywordUtil.markFailed(
	//							"❌ Row ${idx+1} severity mismatch: UI='${severity}', Expected='${expected}'"
	//							)
	//				} else {
	//					println "✔ Row ${idx+1} severity validated"
	//				}
	//			}
	//		}
	//
	//		println "\n===== EXPANDED MODE VALIDATION COMPLETED SUCCESSFULLY =====\n"
	//	}


	// ======================================
	// 3. DROPDOWN HELPERS (FINAL VERSION)
	// ======================================
	@Keyword
	def openDropdown(String name) {

		// 🔥 MUST RUN FIRST → removes ALL leftover dropdown overlays
		forceCloseDropdownHard()
		WebUI.delay(0.2)

		println("Opening dropdown '${name}'...")

		TestObject dd = new TestObject("dd")
		dd.addProperty("id", ConditionType.EQUALS, "mui-component-select-${name}")

		try {
			WebUI.click(dd)
			WebUI.delay(0.5)
		}
		catch (Exception e) {
			println("⚠ Normal click failed → using JS click fallback")

			WebElement el = WebUI.findWebElement(dd, 3)
			WebUI.executeJavaScript("arguments[0].click();", Arrays.asList(el))
			WebUI.delay(0.5)
		}

		println("✔ '${name}' dropdown opened")
	}


	//	// -------------------------
	//	// OPEN DROPDOWN
	//	// -------------------------
	//	@Keyword  	void openDropdown(String name) {
	//
	//		String xpath
	//
	//		switch(name) {
	//			case "severity":
	//				xpath = "//*[@id='mui-component-select-severity']"
	//				break
	//			case "issueType":
	//				xpath = "//*[@id='mui-component-select-issueType']"
	//				break
	//			case "nodeType":
	//				xpath = "//*[@id='mui-component-select-nodeType']"
	//				break
	//			case "batchID":
	//				xpath = "//*[@id='mui-component-select-batchID']"
	//				break
	//			default:
	//				KeywordUtil.markFailed("❌ Unknown dropdown name: ${name}")
	//				return
	//		}
	//
	//		TestObject to = makeTO(xpath)
	//
	//		println "\n➡ Opening '${name}' dropdown..."
	//
	//		try {
	//			WebUI.waitForElementClickable(to, 10)
	//			WebUI.click(to)
	//			WebUI.delay(1)
	//		}
	//		catch (Exception e) {
	//			println("⚠ Normal dropdown click failed — using JS fallback")
	//			try {
	//				WebElement el = WebUI.findWebElement(to, 5)
	//				WebUI.executeJavaScript("arguments[0].click();", Arrays.asList(el))
	//				WebUI.delay(1)
	//			}
	//			catch (Exception ex) {
	//				KeywordUtil.markFailed("❌ Unable to open dropdown '${name}'")
	//			}
	//		}
	//
	//		waitForDropdownOpen()
	//	}


	// -------------------------
	// WAIT FOR DROPDOWN PANEL
	// -------------------------
	@Keyword 	void waitForDropdownOpen() {

		TestObject panel = makeTO(
				"//*[contains(@class,'MuiPopover-paper') or contains(@class,'MuiMenu-paper') or @role='listbox']"
				)

		int timeout = 10
		int counter = 0

		while (counter < timeout) {

			boolean opened = WebUI.verifyElementPresent(
					panel, 1, FailureHandling.OPTIONAL
					)

			if (opened) {
				println "✔ Dropdown panel opened"
				return
			}

			counter++
			WebUI.delay(1)
		}

		KeywordUtil.markFailed("❌ Dropdown did NOT open within timeout")
	}


	// -------------------------
	// READ OPTIONS FROM DROPDOWN
	// -------------------------
	//	List<String> getDropdownOptions() {
	//
	//		List<WebElement> els = WebUI.findWebElements(
	//				makeTO("//*[@role='option']"),
	//				10
	//				)
	//
	//		if (!els || els.isEmpty()) {
	//			KeywordUtil.markFailed("❌ No dropdown options found")
	//		}
	//
	//		return els.collect { it.getText().trim() }
	//	}
	@Keyword
	List<String> getDropdownOptions() {

		println("Fetching dropdown options...")

		TestObject optionList = new TestObject("dropdownOpenList")
		optionList.addProperty("xpath", ConditionType.EQUALS,
				"//div[contains(@class,'MuiPopover-root')]//li[@role='option']")

		List<WebElement> options = WebUI.findWebElements(optionList, 5)

		if (options == null || options.isEmpty()) {
			KeywordUtil.markFailed("❌ No dropdown options found in open dropdown panel!")
			return []
		}

		List<String> values = options.collect { it.getText().trim() }
		println("✔ Options found: $values")
		return values
	}


	// -------------------------
	// CLOSE DROPDOWN (FINAL VERSION)
	// -------------------------
	@Keyword  void closeDropdown() {

		println "➡ Attempting to close dropdown..."

		try {
			WebUI.sendKeys(
					makeTO("//body"),
					Keys.chord(Keys.ESCAPE),
					FailureHandling.OPTIONAL
					)

			WebUI.delay(1)

			println "✔ Dropdown closed successfully"
		}
		catch (Exception e) {
			println "⚠ Dropdown already closed or ESC ignored"
		}
	}

	//	// -------------------------
	//	// FORCE CLOSE DROPDOWN (FINAL VERSION)
	//	// -------------------------
	//@Keyword
	//void forceCloseDropdown() {
	//
	//    println("🔎 Checking for any open dropdown...")
	//
	//    try {
	//        TestObject dropdownPanel = new TestObject("dropdownPanel")
	//        dropdownPanel.addProperty("xpath", ConditionType.EQUALS,
	//                "//*[@role='presentation' or contains(@class,'MuiPopover-root')]")
	//
	//        // First check if dropdown exists
	//        List<WebElement> panels = WebUI.findWebElements(dropdownPanel, 1)
	//        if (panels.size() == 0) {
	//            println("ℹ No open dropdown detected")
	//            return
	//        }
	//
	//        println("⚠ Dropdown detected → closing using ESC + outside click")
	//
	//        // Send ESCAPE key
	//        WebUI.sendKeys(findTestObject(null), Keys.chord(Keys.ESCAPE))
	//        WebUI.delay(0.5)
	//
	//        // Click outside root (backup)
	//        TestObject root = new TestObject("root")
	//        root.addProperty("xpath", ConditionType.EQUALS, "//*[@id='root']")
	//        WebUI.clickOffset(root, 5, 5)
	//        WebUI.delay(0.5)
	//
	//        // FINAL check
	//        panels = WebUI.findWebElements(dropdownPanel, 1)
	//        if (panels.size() == 0) {
	//            println("✔ Dropdown successfully closed")
	//        } else {
	//            println("❌ WARNING: Dropdown still open → forcing JS close")
	//
	//            WebUI.executeJavaScript(
	//                    "document.querySelectorAll('[role=\"presentation\"]').forEach(e=>e.remove());",
	//                    null
	//            )
	//
	//            WebUI.delay(0.5)
	//            println("✔ Forced dropdown removed")
	//        }
	//    }
	//    catch (Exception e) {
	//        println("ℹ Error in dropdown close logic: ${e.message}")
	//    }
	//}

	@Keyword
	void forceCloseDropdownHard() {

		println("🔎 HARD-CLOSE: Checking all dropdown overlays...")

		try {
			String script = """
            document.querySelectorAll('[role="presentation"], .MuiPopover-root, .MuiModal-root')
                    .forEach(el => el.remove());
        """
			WebUI.executeJavaScript(script, null)
			WebUI.delay(0.3)

			println("✔ HARD-CLOSE: All dropdown overlays removed")
		}
		catch (Exception e) {
			println("⚠ HARD-CLOSE error: " + e.message)
		}
	}

	//==============================
	//Helper: Dropdown Validation (With Close Message)
	//=======================================
	@Keyword
	void validateExpandedDropdown(String dropdownName, List<String> expectedValues) {

		println "\n===== VALIDATING '${dropdownName}' DROPDOWN ====="

		// Normalize Excel values (lowercase + trim)
		List<String> normalizedExpected = expectedValues.collect { it.trim().toLowerCase() }

		// Open dropdown
		openDropdown(dropdownName)

		// Read UI values
		List<String> uiOptions = getDropdownOptions()
				.collect { it.trim().toLowerCase() } // normalize UI text
				.unique()

		// Remove the universal "All" option — NEVER validate it
		uiOptions.remove("all")

		// Close safely
		forceCloseDropdownHard()
		println "✔ '${dropdownName}' dropdown closed"

		// Compare
		normalizedExpected.each { expected ->
			if (!uiOptions.contains(expected)) {
				KeywordUtil.markFailed("❌ '${expected}' NOT found in '${dropdownName}' dropdown")
			} else {
				println "✔ '${expected}' validated in '${dropdownName}' dropdown"
			}
		}

		println "✔ '${dropdownName}' dropdown validation completed."
	}



	//	//======================================================
	//	//Helper: Safe Cell Reader (Prevents null failures)
	//	//=======================================================

	String safeCell(WebElement row, Integer index) {
		if (!index) return ""
		try {
			return row.findElement(By.xpath("./td[${index}]")).getText().trim()
		} catch (Exception e) {
			return ""
		}
	}



	// =====================================================
	// Helper for detecting the IssueType–Severity sheet
	// Accepts sheet name:
	//   "issueTypeSeverityMap"   (Exact match)
	//   or any name starting with prefix:
	//   "issueTypeSeverity"
	// =====================================================

	Sheet loadIssueTypeSeveritySheet(Workbook wb) {

		String exact = "issueTypeSeverityMap"
		Sheet sheet = wb.getSheet(exact)

		if (sheet != null) {
			println("✔ Found sheet: ${exact}")
			return sheet
		}

		// fallback for truncated names (Excel cuts long ones)
		String prefix = "issueTypeSeverity"

		for (Sheet s : wb) {
			if (s.getSheetName().toLowerCase().startsWith(prefix.toLowerCase())) {
				println("✔ Auto-detected mapping sheet: ${s.getSheetName()}")
				return s
			}
		}

		KeywordUtil.markFailed(
				"❌ Could NOT find IssueType–Severity sheet. Expected '${exact}' or prefix '${prefix}'"
				)
		return null
	}

	//	==============================================================================
	//	Supporting Function: Dropdown Subset Validation
	//	=================================================================================


	@Keyword void validateDropdownSubset(String dropdownName, List<String> expectedList) {

		println "\nChecking '${dropdownName}' dropdown..."

		openDropdown(dropdownName)
		List<String> uiVals = getDropdownOptions()
		closeDropdown()

		println "UI Dropdown Values: ${uiVals}"

		List<String> normalizedExpected =
				expectedList.collect { it.toLowerCase().trim() }

		println "Excel Allowed Values (normalized): ${normalizedExpected}"

		uiVals.each { val ->
			if (val.equalsIgnoreCase("All")) {
				println "✔ 'All' (skip)"
				return
			}
			if (!normalizedExpected.contains(val.toLowerCase().trim())) {
				KeywordUtil.markFailed("❌ UI value '${val}' NOT found in Excel Master list.")
			} else {
				println "✔ '${val}' validated"
			}
		}
	}

	//	===========================================
	//	HELPER METHODS USED IN SECTION 7
	//	============================================

	//7A. Parse Issue Text
	List<String> parseIssues(String issueText) {

		if (!issueText) return []

		if (issueText.contains(" and other ")) {
			String mainIssue = issueText.split(" and other ")[0].trim()
			return [mainIssue]
		}

		return [issueText.trim()]
	}

	//7B. Modal Validator

	@Keyword		void validateModalForRow(int rowIndex, Map master) {

		println "\n---- Opening modal for row ${rowIndex} ----"

		String detailsBtnXpath =
				"(//*[@data-testid='generic-table-row'])[${rowIndex}]//button[contains(., 'details')]"

		TestObject detailsBtn = makeTO(detailsBtnXpath)

		try {
			WebUI.click(detailsBtn)
		} catch (Exception e) {
			WebElement el = WebUI.findWebElement(detailsBtn, 5)
			WebUI.executeJavaScript("arguments[0].click();", Arrays.asList(el))
		}

		WebUI.delay(1)

		// Validate modal content
		println "Modal opened — validating contents..."

		WebUI.verifyElementPresent(makeTO("//p[@data-testid='issue-message']"), 5)
		WebUI.verifyElementPresent(makeTO("//p[@data-testid='issue-severity']"), 5)

		// CLOSE MODAL
		WebUI.click(makeTO("//button[contains(.,'Close')]"))
		WebUI.delay(1)

		println "✔ Modal validated & closed"
	}

	//7C. Dropdown Validation Against Excel
	@Keyword
	void validateDropdownAgainstExcelExact(String dropdownName, List<String> excelList) {

		println "\nValidating '${dropdownName}' dropdown..."

		openDropdown(dropdownName)

		List<String> uiOptions = getDropdownOptions()

		closeDropdown()

		List<String> excelNorm = excelList.collect { it.trim().toLowerCase() }

		uiOptions.each { uiVal ->

			if (uiVal.equalsIgnoreCase("All")) {
				println "✔ 'All' detected — skip"
				return
			}

			if (!excelNorm.contains(uiVal.trim().toLowerCase())) {
				KeywordUtil.markFailed(
						"❌ UI dropdown '${dropdownName}' contains '${uiVal}' NOT found in Excel sheet"
						)
			} else {
				println "✔ '${uiVal}' validated"
			}
		}
	}

	//7D. Batch ID Dropdown Logging
	@Keyword
	void validateBatchIdDropdown() {

		println "\nReading Batch ID dropdown values..."

		openDropdown("batchID")
		List<String> options = getDropdownOptions()
		closeDropdown()

		println "Batch ID values: ${options}"
	}


	// ======================================
	// 4. Table Header Validator
	// ======================================

	@Keyword 	void validateTableHeaders(List<String> expectedHeaders) {

		expectedHeaders.each { headerName ->

			String xpath = """
		//table//th[
			.//span[normalize-space(text())='${headerName}']
			or normalize-space(.)='${headerName}'
			or @data-testid='generic-table-header-${headerName.replace(" ", "_")}'
		]
		"""

			TestObject to = makeTO(xpath)

			boolean exists = WebUI.verifyElementPresent(
					to,
					10,
					FailureHandling.CONTINUE_ON_FAILURE
					)

			if (!exists) {
				KeywordUtil.markFailed("❌ Header '${headerName}' NOT found in UI using xpath: ${xpath}")
			} else {
				println("✔ Header '${headerName}' present")
			}
		}
	}


	// ======================================
	// 5. Pagination Utils (Silent)
	// ======================================

	boolean hasNextPage() {
		try {
			TestObject to = makeTO("//button[@aria-label='Go to next page' and not(@disabled)]")
			return WebUI.findWebElements(to, 1).size() > 0
		} catch (Exception e) {
			return false
		}
	}

	void goNextPage() {
		WebUI.click(makeTO("//button[@aria-label='Go to next page' and not(@disabled)]"))
		WebUI.delay(1)
	}

	// ======================================
	// 5.1 SMART Validation Wait (Chip-Based)
	// ======================================

	@Keyword 	void waitForValidationComplete() {

		println "\n⏳ Waiting for validation to complete..."

		int timeout = 180
		int interval = 3
		int elapsed = 0

		TestObject validatingBtn = makeTO("//button[contains(., 'Validating')]")
		TestObject completedChip = makeTO("//*[@data-testid='validation-status-chip' and contains(., 'COMPLETED')]")

		while (elapsed < timeout) {

			boolean chipVisible = WebUI.verifyElementPresent(completedChip, 1, FailureHandling.OPTIONAL)
			if (chipVisible) {
				println "✔ Validation completed (chip detected)."
				WebUI.delay(1)
				return
			}

			boolean validatingVisible = WebUI.verifyElementPresent(validatingBtn, 1, FailureHandling.OPTIONAL)
			if (validatingVisible) {
				println "...still validating (${elapsed}s)"
				WebUI.delay(interval)
				elapsed += interval
				continue
			}

			println "✔ Validation completed (no validating button)."
			WebUI.delay(1)
			return
		}

		KeywordUtil.markFailed("❌ Validation did NOT complete within timeout (${timeout}s)")
	}

	// ===========================================================================
	// 5.2 CLICK TOGGLE USING CORRECT LOCATOR data-testid="toggle-input"
	// ===========================================================================
	@Keyword
	void clickToggleInput() {

		// The REAL clickable switch (NOT the input)
		TestObject toggle = new TestObject("expandedToggle")
		toggle.addProperty("xpath", ConditionType.EQUALS, "//*[@data-testid='table-view-switch']")

		println("Attempting to toggle Expanded/Aggregated mode...")

		try {
			WebUI.waitForElementClickable(toggle, 10)
			WebUI.click(toggle)
			println("✔ Toggle clicked successfully (normal click).")
		}
		catch (Exception e1) {
			println("⚠ Normal click failed — trying JS fallback...")

			try {
				WebElement el = WebUI.findWebElement(toggle, 10)
				WebUI.executeJavaScript("arguments[0].click();", Arrays.asList(el))
				println("✔ Toggle clicked with JavaScript.")
			}
			catch (Exception e2) {
				KeywordUtil.markFailed("❌ Toggle NOT clickable even with JS fallback!")
			}
		}

		WebUI.delay(1)
	}

	// ===========================================================================
	// 5.3 RELIABLE TOGGLE CLICK (Handles normal + JS click)
	// ===========================================================================

	@Keyword void clickExpandedToggle() {
		println("\n---- CLICKING EXPANDED MODE TOGGLE ----")

		TestObject toggleObj = new TestObject("toggleObj")
		toggleObj.addProperty("xpath", ConditionType.EQUALS, "//*[@data-testid='toggle-input']")

		try {
			WebUI.waitForElementClickable(toggleObj, 5)
			WebUI.click(toggleObj)
			println("✔ Toggle clicked normally")
			return
		}
		catch (Exception e1) {
			println("⚠ Normal click failed → using JS fallback...")
		}

		try {
			WebElement el = WebUI.findWebElement(toggleObj, 5)
			WebUI.executeJavaScript("arguments[0].click();", Arrays.asList(el))
			println("✔ Toggle clicked via JavaScript")
		}
		catch (Exception e2) {
			KeywordUtil.markFailed("❌ Toggle NOT clickable even with JS: " + e2.message)
		}

		WebUI.delay(1)
	}



	/* ==========================================================
	 * 6. VALIDATE AGGREGATED MODE (FINAL VERSION)
	 * ========================================================== 
	 * */

	@Keyword void validateAggregatedView(Map master) {

		println "\n===== VALIDATING AGGREGATED MODE ====="

		// ======================================================
		// 1️⃣ VALIDATE HEADERS (best selector for your UI)
		// ======================================================

		List<WebElement> headerEls = WebUI.findWebElements(
				makeTO("//th[starts-with(@data-testid,'generic-table-header-')]"),
				10
				)

		List<String> uiHeaders = headerEls.collect { it.getText().trim() }

		println "UI Headers Found: ${uiHeaders}"

		master.aggregatedHeaders.each { expected ->
			if (!uiHeaders.contains(expected)) {
				KeywordUtil.markFailed("❌ Aggregated header '${expected}' NOT found in UI")
			} else {
				println "✔ Header '${expected}' present"
			}
		}

		// Build header index map dynamically
		Map<String, Integer> colIndex = [:]

		uiHeaders.eachWithIndex { h, i ->
			colIndex[h] = i + 1
		}

		if (!colIndex["Issue Type"] || !colIndex["Severity"]) {
			KeywordUtil.markFailed("❌ Required columns (Issue Type, Severity) NOT found in table header.")
		}


		// ======================================================
		// 2️⃣ VALIDATE SEVERITY DROPDOWN
		// ======================================================

		println "\nChecking Severity dropdown (Aggregated mode)..."

		println "▶ Opening 'severity' dropdown..."
		openDropdown("severity")

		List<String> uiOptions = getDropdownOptions()

		println "▶ Closing dropdown..."
		closeDropdown()

		println "UI Severity Options = ${uiOptions}"

		master.severities.each { expected ->
			if (!uiOptions.contains(expected)) {
				KeywordUtil.markFailed("❌ Severity '${expected}' NOT found in dropdown")
			} else {
				println "✔ Severity '${expected}' validated"
			}
		}

		// ======================================================
		// 3️⃣ VALIDATE TABLE ROWS (dynamic column indexes)
		// ======================================================

		println "\nValidating aggregated rows..."

		List<WebElement> rows = WebUI.findWebElements(
				makeTO("//*[@data-testid='generic-table-row']"),
				20
				)

		println "✔ Found ${rows.size()} aggregated rows"

		// ======================================================
		// 4️⃣ READ & VALIDATE EACH ROW
		// ======================================================

		rows.eachWithIndex { WebElement row, int idx ->

			// Dynamic column extraction
			String issueType = row.findElement(
					By.xpath("./td[" + colIndex["Issue Type"] + "]")
					).getText().trim()

			String severity = row.findElement(
					By.xpath("./td[" + colIndex["Severity"] + "]")
					).getText().trim()

			// Row details in console
			println "\nRow ${idx+1} Details:"
			println "   Issue Type : ${issueType}"
			println "   Severity   : ${severity}"

			// --------------------------------------------------
			// 5️⃣ VERIFY ISSUE TYPE EXISTS IN EXCEL MAPPING
			// --------------------------------------------------

			String expectedSeverity = master.issueTypeSeverityMap[issueType]

			if (!expectedSeverity) {
				KeywordUtil.markFailed(
						"❌ Excel mapping missing IssueType '${issueType}'"
						)
			}

			// --------------------------------------------------
			// 6️⃣ VERIFY SEVERITY MATCHES EXPECTED
			// --------------------------------------------------

			if (!severity.equalsIgnoreCase(expectedSeverity)) {
				KeywordUtil.markFailed(
						"❌ Row ${idx+1}: Severity '${severity}' does NOT match expected '${expectedSeverity}'"
						)
			}

			println "✔ Row ${idx+1} validated successfully"
		}

		println "\n✔ Aggregated Mode Validation Completed Successfully"
	}



	/* ======================================================================================
	 *  7. EXPANDED MODE VALIDATION  (FINAL, PRODUCTION–READY VERSION)
	 * ====================================================================================== */

	@Keyword
	void validateExpandedModeFull(Map master) {

		println "\n===== VALIDATING EXPANDED MODE ====="

		// -------------------------------------------------------------------
		// 1️⃣ Switch to Expanded Mode
		// -------------------------------------------------------------------
		forceCloseDropdownHard()
		clickExpandedToggle()
		WebUI.delay(1)
		forceCloseDropdownHard()


		// -------------------------------------------------------------------
		// 2️⃣ Validate Expanded Table Headers
		// -------------------------------------------------------------------
		println "\nValidating Expanded Headers..."

		List<WebElement> headerEls = WebUI.findWebElements(
				makeTO("//th[starts-with(@data-testid,'generic-table-header-')]"),
				10
				)

		List<String> uiHeaders = headerEls.collect { it.getText().trim() }
		println "UI Headers Found: ${uiHeaders}"

		master.expandedHeaders.each { expected ->
			if (!uiHeaders.contains(expected)) {
				KeywordUtil.markFailed("❌ Expanded header '${expected}' NOT found in UI")
			} else {
				println "✔ Header '${expected}' present"
			}
		}

		Map<String, Integer> colIndex = [:]
		uiHeaders.eachWithIndex { h, i -> colIndex[h] = i + 1 }


		// -------------------------------------------------------------------
		// 3️⃣ Validate All Dropdowns (against Excel dropdown sheets)
		// -------------------------------------------------------------------
		println "\n===== VALIDATING EXPANDED DROPDOWNS ====="

		println "\n▶ Issue Type Dropdown"
		validateDropdownAgainstExcelExact("issueType", master.issueTypes)

		println "\n▶ Node Type Dropdown"
		validateDropdownAgainstExcelExact("nodeType", master.nodeTypes)

		println "\n▶ Severity Dropdown"
		validateDropdownAgainstExcelExact("severity", master.severities)

		println "\n▶ Batch ID Dropdown (Logging Only)"
		validateBatchIdDropdown()



		// -------------------------------------------------------------------
		// 4️⃣ Validate Table Rows
		// -------------------------------------------------------------------
		println "\n===== VALIDATING EXPANDED ROWS ====="

		List<WebElement> rows = WebUI.findWebElements(
				makeTO("//*[@data-testid='generic-table-row']"),
				20
				)

		println "✔ Found ${rows.size()} expanded rows"


		rows.eachWithIndex { WebElement row, int idx ->

			println "\n------------------------------"
			println "Row ${idx+1} Details"
			println "------------------------------"

			String nodeType  = safeCell(row, colIndex["Node Type"])
			String severity  = safeCell(row, colIndex["Severity"])
			String issueText = safeCell(row, colIndex["Issue(s)"])

			println "Node Type  : ${nodeType}"
			println "Severity   : ${severity}"
			println "Issue(s)   : ${issueText}"


			// -------------------------------------------------------------------
			// 4A — VALIDATE NODE TYPE
			// -------------------------------------------------------------------
			if (!master.nodeTypes.any { it.equalsIgnoreCase(nodeType) }) {
				KeywordUtil.markFailed("❌ Row ${idx+1}: NodeType '${nodeType}' NOT found in Excel dropdown")
			} else {
				println "✔ Node Type validated"
			}


			// -------------------------------------------------------------------
			// 4B — HANDLE MULTIPLE ISSUES ("... and other X")
			// -------------------------------------------------------------------
			List<String> parsedIssues = parseIssues(issueText)

			println "Parsed Issues = ${parsedIssues}"

			parsedIssues.each { issueType ->

				// -------------------------------------------------------------------
				// 4B.1 — Validate IssueType Exists in Excel Mapping Sheet
				// -------------------------------------------------------------------
				String expectedSeverity = master.issueTypeSeverityMap.find {
					it.key.equalsIgnoreCase(issueType)
				}?.value

				if (!expectedSeverity) {
					KeywordUtil.markFailed("❌ Excel mapping missing IssueType '${issueType}'")
				} else {
					println "✔ Issue '${issueType}' exists in Excel mapping"
				}

				// -------------------------------------------------------------------
				// 4B.2 — Validate Severity Matches Expected
				// -------------------------------------------------------------------
				if (!severity.equalsIgnoreCase(expectedSeverity)) {
					KeywordUtil.markFailed(
							"❌ Row ${idx+1}: Severity '${severity}' does NOT match expected '${expectedSeverity}' for Issue '${issueType}'"
							)
				} else {
					println "✔ Severity validated for Issue '${issueType}'"
				}
			}


			// -------------------------------------------------------------------
			// 4C — OPEN DETAILS MODAL & VALIDATE ISSUES INSIDE
			// -------------------------------------------------------------------
			validateModalForRow(idx + 1, master)

			println "✔ Row ${idx+1} fully validated"
		}

		println "\n✔ Expanded Mode Validation Completed Successfully"
	}


	// ======================================
	// 8. Expanded Row Validation (FINAL - PAGINATION DISABLED)
	// ======================================

	@Keyword void validateExpandedRows(Map master) {

		println("\n===== VALIDATING EXPANDED ROWS =====")

		List<WebElement> rows = WebUI.findWebElements(
				makeTO("//tr[@data-testid='generic-table-row']"),
				10
				)

		println("✔ Found ${rows.size()} expanded rows")

		int index = 1

		rows.each { row ->

			println("\n--- Row ${index} ---")

			// Extract Issue(s)
			String issueText = row.findElement(By.xpath(".//td[7]")).getText().trim()

			// Extract Severity
			String severityText = row.findElement(By.xpath(".//td[4]")).getText().trim()

			// Extract NodeType
			String nodeType = row.findElement(By.xpath(".//td[2]")).getText().trim()

			//---------------------------------------------------
			// 8.1 Validate NodeType
			//---------------------------------------------------
			if (!master.nodeTypes.contains(nodeType)) {
				KeywordUtil.markFailed("❌ NodeType '${nodeType}' not found in Excel master list")
			} else {
				println("✔ NodeType '${nodeType}' is valid")
			}

			//---------------------------------------------------
			// 8.2 Validate IssueType + Severity mapping
			//---------------------------------------------------

			// Find the matching IssueType key from Excel using substring match
			String matchedIssue = master.issueTypeMap.keySet().find { key ->
				issueText.toLowerCase().contains(key.toLowerCase())
			}

			if (!matchedIssue) {
				KeywordUtil.markFailed("❌ No Excel IssueType mapping found for UI Issue(s): '${issueText}'")
			}

			String expectedSeverity = master.issueTypeMap[matchedIssue]

			//---------------------------------------------------
			// 8.3 Compare Severity
			//---------------------------------------------------
			if (!severityText.equalsIgnoreCase(expectedSeverity)) {
				KeywordUtil.markFailed(
						"❌ Severity mismatch for Issue '${matchedIssue}'. " +
						"UI='${severityText}', Expected='${expectedSeverity}'"
						)
			} else {
				println("✔ Severity '${severityText}' validated for '${matchedIssue}'")
			}

			//---------------------------------------------------
			// 8.4 Click 'See details' and validate modal
			//---------------------------------------------------

			String detailsBtnXpath = "(//tr[@data-testid='generic-table-row'])[${index}]//button[contains(., 'See details')]"

			try {
				WebUI.click(makeTO(detailsBtnXpath))
			} catch (Exception e) {
				WebElement el = WebUI.findWebElement(makeTO(detailsBtnXpath), 5)
				WebUI.executeJavaScript("arguments[0].click();", Arrays.asList(el))
			}

			WebUI.delay(1)

			validateModal(master)
			index++
		}
	}



	// ======================================
	// 9. Modal Validator
	// ======================================

	void validateModal(Map master) {

		println("\n==== Modal Validation ====")

		// Modal container
		WebUI.verifyElementPresent(makeTO("//div[contains(@class,'MuiDialog-paper')]"), 10)

		// Title
		WebUI.verifyElementPresent(makeTO("//h1[@data-testid='error-details-title']"), 5)

		// Node Info
		WebUI.verifyElementPresent(makeTO("//span[@data-testid='error-details-node-info']"), 5)

		// Issue Count
		WebUI.verifyElementPresent(makeTO("//p[@data-testid='error-details-error-count']"), 5)

		// Severity
		WebUI.verifyElementPresent(makeTO("//p[@data-testid='issue-severity']"), 5)

		// Issue Message
		WebUI.verifyElementPresent(makeTO("//p[@data-testid='issue-message']"), 5)

		// Scroll through modal
		WebUI.executeJavaScript("document.querySelector('.MuiDialog-paper').scrollTop = 500;", null)

		// Close modal
		WebUI.click(findTestObject('CRDC/DataSubmissions/error-details-close-button'))
		WebUI.delay(1)

		println("✔ Modal closed")
	}



	// ======================================
	// 10. MASTER WRAPPER (Call This in Test Case)
	// ======================================

	@Keyword
	def validateValidationResultsUI(String excelPath) {

		println("\n==========================")
		println(" STARTING FULL VALIDATION ")
		println("==========================\n")

		Map master = loadExcelMaster(excelPath)

		// 1️⃣ Wait for validation to finish
		waitForValidationComplete()

		// 2️⃣ Aggregated mode checks
		validateAggregatedView(master)

		// 3️⃣ Expanded mode + row-level checks
		validateExpandedView(master)
		validateExpandedRows(master)

		println("\n==========================")
		println(" VALIDATION COMPLETE ✔")
		println("==========================\n")
	}
}