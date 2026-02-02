package utilities

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import javax.print.DocFlavor.STRING
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable
import utilities.ReadExcel
import org.openqa.selenium.Keys as Keys
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.TimeoutException
import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import com.kms.katalon.core.checkpoint.Checkpoint
import com.kms.katalon.core.checkpoint.CheckpointFactory
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testcase.TestCase
import com.kms.katalon.core.testcase.TestCaseFactory
import com.kms.katalon.core.testdata.TestData
import com.kms.katalon.core.testdata.TestDataFactory
import com.kms.katalon.core.testobject.ObjectRepository
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords
import com.kms.katalon.core.mobile.keyword.internal.MobileDriverFactory
import com.kms.katalon.core.testobject.RequestObject
import com.kms.katalon.core.testobject.ResponseObject
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.mobile.helper.MobileElementCommonHelper
import com.kms.katalon.core.webui.exception.WebElementNotFoundException
import java.awt.AWTException;
import java.io.IOException;
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
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Cookie as Cookie
import com.kms.katalon.core.configuration.RunConfiguration
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import java.util.regex.Pattern
import java.util.regex.Matcher



public class TestRunner implements Comparator<List<XSSFCell>>{
	public int compare( List<XSSFCell> l1, List<XSSFCell> l2 ){
		return l1.get(0).getStringCellValue().compareTo( l2.get(0).getStringCellValue() )
	}


	public static WebDriver driver
	public static WebElement nxtBtn
	public static String appKey = GlobalVariable.AppKey;


	/**
	 * This function reads the new excel file name from InputFiles
	 * @param input_file
	 */
	@Keyword
	public  void RunKatalon(String input_file) {

		Path filePath = Utils.getQueryFilePath(input_file);

		KeywordUtil.logInfo("Global variable set for InputFiles is: " + GlobalVariable.InputExcel )
		Thread.sleep(2000)
		List<List<XSSFCell>> sheetData_K = new ArrayList<>();
		FileInputStream fis = new FileInputStream(GlobalVariable.InputExcel);
		XSSFWorkbook workbook = new XSSFWorkbook(fis); // Create an excel workbook from the file system.
		int numberOfSheets = workbook.getNumberOfSheets();// Get the  sheets on the workbook
		int countrow = 0
		int countcol= 0
		Thread.sleep(2000)
		XSSFSheet sheet = workbook.getSheetAt(0);  //reading input query
		countrow = sheet.lastRowNum- sheet.firstRowNum;
		System.out.println ("Row count of input excel is: " + countrow);
		countcol = sheet.getRow(0).getLastCellNum();
		System.out.println("Colm count of input excel is: " + countcol);

		//This loops through the rows of the table till there is next row
		Iterator rows = sheet.rowIterator();
		while (rows.hasNext()) {
			XSSFRow row = (XSSFRow) rows.next();
			Iterator cells = row.cellIterator();
			List<XSSFCell> data = new ArrayList<>();
			while (cells.hasNext()) {
				XSSFCell cell = (XSSFCell) cells.next();
				data.add(cell);
			}
			sheetData_K.add(data);
		}

		KeywordUtil.markPassed("Data loaded from input file for the test case." )
		driver = CustomBrowserDriver.createWebDriver();
		System.out.println("This is the driver: " + driver)
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		excelparsingKatalon(sheetData_K, driver);
	}


	/**
	 * This function converts any given string value into integer
	 * @param stringVal Add the value to be converted to Int
	 * @return
	 */
	public static int convStringtoInt (String stringVal)	{
		int i =0;
		try {
			System.out.println("string value is = " + stringVal);
			i = Integer.parseInt(stringVal.trim());
			System.out.println("integer value is = " + i);
		}
		catch (NumberFormatException nfe) {
			System.out.println("NumberFormatException: " + nfe.getMessage());
		}
		return i;
	}


	/**
	 * This function reads input excels and assigns global variables to each query...
	 * @param sheetData
	 * @param dr
	 */
	private static void excelparsingKatalon(List<List<XSSFCell>> sheetData, WebDriver dr) {

		System.out.println("This is URL: "+GlobalVariable.G_Urlname)
		driver.get(GlobalVariable.G_Urlname)
		driver.manage().window().maximize()
		System.out.println("The window is maximized")
		Thread.sleep(3000)
		int countrow = 0
		countrow = sheetData.size();
		System.out.println( "Row count from initializing function: " + countrow )
		System.out.println( "Sheet  data size: " + sheetData.get(0).size())

		//Loop through rows
		for (int i = 1; i < countrow; i++){
			List<XSSFCell> datarow = sheetData.get(i);
			System.out.println("Columns count from initializing function: " + datarow.size())
			String str = "";
			//loop through columns
			for (int j = 0; j < datarow.size(); j++){
				//				System.out.println ("Value of  i: "  + i + "  Value of j  : " + j )
				XSSFCell cell = datarow.get(j);
				//Look for specific column names to perform action
				switch(sheetData.get(0).get(j).getStringCellValue().trim() ) {
					case("TabName"):
						GlobalVariable.G_inputTabName = sheetData.get(i).get(j).getStringCellValue()
						System.out.println("This is the tabname from input excel : "+GlobalVariable.G_inputTabName)
						break;
					case("query"):
						if(GlobalVariable.G_inputTabName=="CasesTab"){
							GlobalVariable.G_QueryCasesTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of cases tab query from switch case : "+GlobalVariable.G_QueryCasesTab)
						}else if(GlobalVariable.G_inputTabName=="SamplesTab"){
							GlobalVariable.G_QuerySamplesTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of samples tab query from switch case : "+GlobalVariable.G_QuerySamplesTab)
						}else if(GlobalVariable.G_inputTabName=="FilesTab"){
							GlobalVariable.G_QueryFilesTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of files tab query from switch case : "+GlobalVariable.G_QueryFilesTab)
						}else if(GlobalVariable.G_inputTabName=="ProgramsTab"){
							GlobalVariable.G_QueryProgramsTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of files tab query from switch case : "+GlobalVariable.G_QueryProgramsTab)
						}else if(GlobalVariable.G_inputTabName=="ParticipantsTab"){
							GlobalVariable.G_QueryParticipantsTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of Participants tab query from switch case : "+GlobalVariable.G_QueryParticipantsTab)
						}else if(GlobalVariable.G_inputTabName=="StudyFilesTab"){
							GlobalVariable.G_QueryStudyFilesTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of Study Files tab query from switch case : "+GlobalVariable.G_QueryStudyFilesTab)
						}else if(GlobalVariable.G_inputTabName=="SubjectsTab"){
							GlobalVariable.G_GQuerySubjectsTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of Study Files tab query from switch case : "+GlobalVariable.G_GQuerySubjectsTab)
						}else if(GlobalVariable.G_inputTabName=="GrantsTab"){
							GlobalVariable.G_QueryGrantsTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of Projects tab query from switch case : "+GlobalVariable.G_QueryGrantsTab)
						}else if(GlobalVariable.G_inputTabName=="PublicationsTab"){
							GlobalVariable.G_QueryPublicationsTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of Publications tab query from switch case : "+GlobalVariable.G_QueryPublicationsTab)
						}else if(GlobalVariable.G_inputTabName=="DatasetsTab"){
							System.out.println("This is the value of Datasets tab query from switch case : "+GlobalVariable.G_QueryDatasetsTab)
						}else if(GlobalVariable.G_inputTabName=="ClinicalTrialsTab"){
							GlobalVariable.G_QueryClinTrialsTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of Clinical Trials tab query from switch case : "+GlobalVariable.G_QueryClinTrialsTab)
						}else if(GlobalVariable.G_inputTabName=="DiagnosisTab"){
							GlobalVariable.G_QueryDiagnosisTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of Diagnosis tab query from switch case : "+GlobalVariable.G_QueryDiagnosisTab)
						}else if(GlobalVariable.G_inputTabName=="StudiesTab"){
							GlobalVariable.G_QueryStudiesTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of Studies tab query from switch case : "+GlobalVariable.G_QueryStudiesTab)
						}else if(GlobalVariable.G_inputTabName=="PatentsTab"){
							GlobalVariable.G_QueryPatentsTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of Patents tab query from switch case : "+GlobalVariable.G_QueryPatentsTab)
						}else if(GlobalVariable.G_inputTabName=="SurvivalTab"){
							GlobalVariable.G_QuerySurvivalTab = sheetData.get(i).get(j).getStringCellValue()
							System.out.println("This is the value of Survival tab query from switch case : "+GlobalVariable.G_QuerySurvivalTab)
						}
						break;

					case ("StatQuery"):  //query for stat bar only
						GlobalVariable.G_StatQuery= sheetData.get(i).get(j).getStringCellValue()
						break;
					case ("cartQuery"):  //query for My cart table only
						GlobalVariable.G_cartQuery= sheetData.get(i).get(j).getStringCellValue()
						System.out.println("This is the value of cart query from switch case : "+GlobalVariable.G_cartQuery)
						break;
					case("WebExcel"):
						GlobalVariable.G_WebExcel = sheetData.get(i).get(j).getStringCellValue()
						GlobalVariable.G_OutputFileName = GlobalVariable.G_WebExcel
						System.out.println("This is the value of gwebexcel before appending with directory :"+GlobalVariable.G_WebExcel)
						System.out.println("This is the value of output filename stored in a global var :"+GlobalVariable.G_OutputFileName)

						Path outputDir = Paths.get(System.getProperty("user.dir"), "OutputFiles")
						GlobalVariable.G_OutputDir =outputDir.toString()
						System.out.println("This is the path till the output directory : "+GlobalVariable.G_OutputDir)

						Path filepath = Paths.get(System.getProperty("user.dir"), "OutputFiles", GlobalVariable.G_WebExcel)
						GlobalVariable.G_WebExcel=filepath.toString()
						System.out.println("This is the full path stored in global variable gwebexcel: "+GlobalVariable.G_WebExcel)
						break;
					case("dbExcel"):
						GlobalVariable.G_dbexcel = sheetData.get(i).get(j).getStringCellValue()
						Path dbfilepath = Paths.get(System.getProperty("user.dir"), "OutputFiles", GlobalVariable.G_dbexcel)
						GlobalVariable.G_ResultPath=dbfilepath.toString()
						break;
					case("TsvExcel"):
						GlobalVariable.G_dbexcel = sheetData.get(i).get(j).getStringCellValue()
						Path dbfilepath = Paths.get(System.getProperty("user.dir"), "OutputFiles", GlobalVariable.G_dbexcel)
						GlobalVariable.G_ResultPath=dbfilepath.toString()
						break;
					default :
						System.out.println("Error in initializing")
						break;
				}// Switch case ends here
				str =str+ cell.getStringCellValue() + "||"
			}//for loop j ends (column read)
		}//for loop i ends (row read)
	} //excelparsingKatalon function ends here


	/**This function is used for case detail level automation
	 * @return
	 */
	@Keyword
	public static String getPageSwitch() {
		System.out.println("Inside pageswitch function")
		String switchStr;
		String pgUrl = driver.getCurrentUrl()

		if(appKey.equals("CCDI")) {
			System.out.println("This is CCDI. the url does not contain #")
			switchStr = "/explore"
		}else if(appKey.equals("C3DC")){
			System.out.println("This is C3DC. the url does not contain #")
			switchStr = "/explore"
		}else {
			String[] arrOfStr = pgUrl.split("#", 2);
			System.out.println ("This is the value of the array of strings after splitting url : "+arrOfStr)
			switchStr=getSwitchStr(arrOfStr[1])
		}

		return switchStr
	}

	/**
	 * This function returns a string for a particular application"s page for further processing
	 * and is called above in getPageSwitch function
	 * @param mainStr
	 * @return
	 */
	public static String getSwitchStr(String mainStr) {
		String retnSwStr
		if (mainStr.contains("/cases")){
			retnSwStr = "/cases"
		}else if(mainStr.contains("/case/")){
			retnSwStr = "/case/"
		}else if(mainStr.contains("/study/")){
			retnSwStr = "/study/"
		}else if(mainStr.contains("/explore")){
			retnSwStr = "/explore"
		}else if(mainStr.contains("/subjects")){
			retnSwStr = "/subjects"
		}else if(mainStr.contains("/data")){
			retnSwStr = "/data"
		}else if(mainStr.contains("/fileCentricCart")){
			retnSwStr = "/fileCentricCart"
		}else if(mainStr.contains("/projects")){
			retnSwStr = "/projects"
		}else if(mainStr.contains("/studies")){
			retnSwStr = "/studies"
		}else if(mainStr.contains("/programs")){
			retnSwStr = "/programs"
		}
		System.out.println("This is the value returned for switch case: "+retnSwStr)
		return retnSwStr
	}



	/**
	 * This function reads the results table and writes the web and database data to excel
	 * This function also verifies the stat-bar counts and compares the web and database excels
	 */
	@Keyword
	public static void multiFunction(String appName, String statVal, String tbl, String tblHdr, String nxtBtn, String webdataSheetName, String dbdataSheetName, String tabQuery) throws IOException {

		Utils.RESULT_TAB_NAME = dbdataSheetName;
		System.out.println("This is the value of stat (string) obtained from multifunction: " + statVal);

		int statValue = convStringtoInt(statVal);

		System.out.println("This is the value of stat (integer) obtained from multifunction: " + statValue);

		if (statValue !=0) {
			ReadCasesTableKatalon(statVal, tbl,tblHdr, nxtBtn, webdataSheetName)

			//ReadExcel.Neo4j(dbdataSheetName,tabQuery)
			PythonReader.readFile('ResultTabs.py')
			PythonReader.readFile('Statbar.py')

			Utils.compareSheets(webdataSheetName, dbdataSheetName)

			System.out.println("Control is before validate stat bar function from multifunction")
			validateStatBar(appName)
		}else {
			System.out.println("Skipping data collection from neo4j and compare lists of web and db as the stat value is 0")
		}
	}

	/**
	 * This function reads cases table
	 * @param statVal1
	 * @param tbl1
	 * @param hdr1
	 * @param nxtb1
	 * @param webSheetName
	 * @throws IOException
	 */
	@Keyword
	public static void ReadCasesTableKatalon(String statVal1, String tbl1, String hdr1, String nxtb1, String webSheetName) throws IOException {
		String switchCanine
		//String switchTrials
		String switchCTDC
		String switchBento
		String switchGMB
		String switchCDS
		String switchCCDI
		String switchC3DC
		String switchINS
		String switchString
		WebElement nextButton
		WebElement nxtBtn
		WebElement resultTab
		String trim

		WebDriverWait wait = new WebDriverWait(driver,30);
		System.out.println("This is the stat value of cases/total (in case of cart) before converting to int: "+statVal1)
		int statValue = convStringtoInt(statVal1);
		System.out.println("This is the passed value of stat for this run: "+statValue)


		//List<String> webData = new ArrayList<String>();  //this is not used
		List<String> wTableHdrData = new ArrayList<String>(); //to capture the table header data
		List<String> wTableBodyData = new ArrayList<String>(); //to capture the table body data
		String tbl_bdy;
		String tbl_main= givexpath(tbl1)
		System.out.println("This is the value of tbl main : "+tbl_main)

		tbl_bdy= tbl_main+"//tbody"
		GlobalVariable.G_cannine_caseTblBdy=tbl_bdy  //correct his variables name typo and also rename it to G_commons_casetblbdy
		System.out.println("This is the value of table body: "+GlobalVariable.G_cannine_caseTblBdy)

		//	driver.manage().window().maximize()  commenting to check the error in INS
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(tbl_bdy)));
		scrolltoViewjs(driver.findElement(By.xpath(tbl_bdy)))
		System.out.println("Scrolled into view and ready to click again")
		clickElement(driver.findElement(By.xpath(tbl_bdy)));
		System.out.println("using jscriptexec, clicked again")

		WebElement TableBdy =driver.findElement(By.xpath(GlobalVariable.G_cannine_caseTblBdy))
		List<WebElement> rows_table = TableBdy.findElements(By.tagName("tr"))
		System.out.println("This is the value of weblement rows table: "+rows_table);

		int rows_count = rows_table.size()
		System.out.println("This is the size of the rows in the results table in first page: "+(rows_count))
		String nxt_str= givexpath(nxtb1)
		System.out.println("This is the value of the xpath of nextbtn : "+nxt_str)
		//nextButton = driver.findElement(By.xpath(nxt_str));
		System.out.println("This is the value of the webelem next button from readcasestablekatalon method : "+nextButton)
		System.out.println("This is the value of the hdr object: "+hdr1)
		String hdr_str= givexpath(hdr1)
		System.out.println("This is the value of the hdr string - xpath : "+hdr_str)
		WebElement tableHdr = driver.findElement(By.xpath(hdr_str))

		List<WebElement> colHeader = tableHdr.findElements(By.tagName("th"));

		int columns_count
		String hdrdata = ""

		String crntUrl=driver.getCurrentUrl();

		//Read ICDC table header from result table for a specific tab
		if(appKey.equals("ICDC") && ((driver.getCurrentUrl()).contains("/case/"))){
			switchCanine = getPageSwitch();
			switchString = "Canine";
			System.out.println ("This is the value of CANINE switch string returned by getcurrentpage function: "+switchCanine)
			nxtBtn =  driver.findElement(By.xpath(givexpath('Object Repository/Canine/Canine_File_NextBtn'))); //remove these references of nxtbtn from all 4 ifs
			columns_count = (colHeader.size())
			for(int c=0;c<columns_count;c++){
				hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
			}
		}else if (appKey.equals("ICDC")){
			switchCanine = getPageSwitch();
			switchString = "Canine";
			nxtBtn =  driver.findElement(By.xpath(givexpath(nxtb1)));
			System.out.println("This is the value of next button from canine cases switch: "+nxtBtn)
			if(statValue==0){
				System.out.println ("No records in the table as stat value is 0")
			}else{
				columns_count = (colHeader.size())
				for(int c=0;c<columns_count;c++){
					//valentina
					if((colHeader.get(c).getAttribute("innerText"))!="Access"){
						//if column header = 'Access' ignore adding it to the hdrdata string
						System.out.println ("This is the value of col header index : "+c)
						hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
					}
				}
			}
		}else if (appKey.equals("ICDC") && ((driver.getCurrentUrl()).contains("/study"))){
			switchCanine = getPageSwitch();
			switchString = "Canine";
			System.out.println ("This is the value of CANINE switch string: " + switchCanine)
			nxtBtn =  driver.findElement(By.xpath(givexpath(nxtb1)));
			System.out.println("This is the value of next button from canine cases switch: "+nxtBtn)
			if(statValue==0){
				System.out.println ("No records in the table as stat value is 0")
			}else{
				columns_count = (colHeader.size())-1
				for(int c=1;c<=columns_count;c++){
					if((colHeader.get(c).getAttribute("innerText"))!="Access"){
						//if column header = 'Access' ignore adding it to the hdrdata string
						System.out.println ("This is the value of col header index : "+c)
						hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
					}
				}
			}
		}
		else if (appKey.equals("ICDC") && ((driver.getCurrentUrl()).contains("/fileCentricCart"))){
			switchCanine = getPageSwitch();
			switchString = "Canine";
			System.out.println ("This is the value of CANINE switch string: " + switchCanine)
			nxtBtn =  driver.findElement(By.xpath(givexpath(nxtb1)));
			System.out.println("This is the value of next button from canine mycart switch: "+nxtBtn)
			if(statValue==0){
				System.out.println ("No files in the cart")
			}else{
				columns_count = (colHeader.size())-1
				for(int c=1;c<=columns_count;c++){
					hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
				}
			}
			//******** INS function starts here ********
		}else if (appKey.equals("INS")){
			switchINS = getPageSwitch();
			switchString = "INS";
			System.out.println ("This is the value of INS switch string: " + switchINS)
			nxtBtn =  driver.findElement(By.xpath(givexpath(nxtb1)));
			System.out.println("This is the value of next button from INS projects switch: "+nxtBtn)
			if(statValue==0){
				System.out.println ("No records in the table as stat value is 0")
			}else{
				columns_count = colHeader.size()
				for(int c=0;c<columns_count;c++){
					System.out.println ("This is the value of col header index : "+c)
					//Added below if statement to skip Programs(s) and Project(s) cols as it have multiple elements - maybe fix later
					if((colHeader.get(c).getAttribute("innerText") != "Project(s)") && colHeader.get(c).getAttribute("innerText") != "Program(s)") {
						hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
					}
				}
			}
			//******** CDS function starts here ********
		}else if(appKey.equals("CDS")){
			switchCDS = getPageSwitch();
			switchString = "CDS";
			columns_count = (colHeader.size())
			columns_count=columns_count-1;
			System.out.println("Inside CDS switch case for header data::  " +columns_count)
			for(int c=1;c<=columns_count;c++){
				hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
			}

			//			//******** CCDI function starts here ********
			//		}else if(appKey.equals("CCDI")){
			//			System.out.println ("Control is about to go to case switch ")
			//			switchCCDI = getPageSwitch();
			//			System.out.println ("Control is about to go to the switch for CCDI ")
			//			switchString = "CCDI";
			//			System.out.println ("This is the value of CCDI switch string returned by getcurrentpage function: "+switchCCDI)
			//
			//			columns_count = (colHeader.size())
			//			columns_count=columns_count-1;
			//			System.out.println("Inside CCDI switch case for header data::  " +columns_count)
			//
			//			//If the column header is Study Status or Manifest, ignore it from data collection and ignore from writing it to output file
			//
			//			for(int c=1;c<=columns_count;c++){
			//				if (colHeader.get(c).getAttribute("innerText")!=("Manifest")){
			//					hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
			//				}
			//			}

			//******** CCDI function starts here ******** edited 12/04/25 - VL
		} else if (appKey.equals("CCDI")) {
			System.out.println("Control is about to go to case switch ")
			switchCCDI = getPageSwitch()
			System.out.println("Control is about to go to the switch for CCDI ")
			switchString = "CCDI"
			System.out.println("This is the value of CCDI switch string returned by getcurrentpage function: " + switchCCDI)

			if (isGuidEligibleStudy()) {
				WebUI.comment("GUID-enabled study detected → enabling GUID column")
				WebUI.click(findTestObject('Object Repository/CCDI/ExplorePage/DisplayedColumns_Ddn'))
				WebUI.click(findTestObject('Object Repository/CCDI/ExplorePage/DisplayedColumns_GUID'))
				WebUI.waitForElementVisible(findTestObject('Object Repository/CCDI/ExplorePage/Files_ResultsTab_GUIDCol'), 10)
			}

			// Re-fetch colHeader here after enabling GUID
			colHeader = WebUI.findWebElements(findTestObject('Object Repository/CCDI/ExplorePage/TableHeaders'), 10)
			int totalHeaders = colHeader.size()   // e.g. 11 when GUID is enabled
			System.out.println("Total header elements on page AFTER GUID toggle: " + totalHeaders)

			// Checkbox is index 0 → skip it always
			int startIndex = 1

			// Number of REAL data columns = total minus the checkbox column
			columns_count = totalHeaders - 1
			System.out.println("columns_count (real data columns): " + columns_count)

			// Build header string for ONLY real columns
			hdrdata = ""

			for (int c = startIndex; c < totalHeaders; c++) {
				String headerText = colHeader.get(c).getAttribute("innerText").trim()

				if (!"Manifest".equals(headerText)) {
					hdrdata += headerText + "||"
				}
			}

			System.out.println("Final header list: " + hdrdata)


			//******** C3DC function starts below ********
		}else if(appKey.equals("C3DC")){
			switchC3DC = getPageSwitch();
			switchString = "C3DC";
			System.out.println ("This is the value of C3DC switch string: "+switchC3DC)
			columns_count = (colHeader.size())
			columns_count = columns_count;
			System.out.println("Inside C3DC switch case for header data:  " + columns_count)

			if((tbl_main).equals("//*[@id='study_tab_table']")){
				for(int c=0;c<columns_count;c++){
					hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
				}
			}else {
				for(int c=1;c<columns_count;c++){
					hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
				}
			}

			//******** CTDC function starts below ********
		}else if(appKey.equals("CTDC")){
			switchCTDC = getPageSwitch();
			switchString = "CTDC";
			System.out.println ("Value of CTDC switch string: "+switchCTDC)
			nxtBtn =  driver.findElement(By.xpath(givexpath('CTDC/ResultTabs/All_Tabs_Next-Btn')));
			columns_count = (colHeader.size())
			columns_count=columns_count;
			System.out.println ("Total number of columns in CTDC result tab in explore page: "+ columns_count)
			for(int c=1;c<columns_count;c++){
				if (colHeader.get(c).getAttribute("innerText")!=("Access")){
					hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
				}
			}
			//********** Bento Function Start here *********
		}else if(appKey.equals("Bento")){
			switchBento = getPageSwitch();
			switchString = "Bento";
			System.out.println ("Value of Bento switch string: " + switchBento)
			columns_count = (colHeader.size())-1
			hdrdata = ""
			for(int c=1;c<=columns_count;c++){
				if((colHeader.get(c).getAttribute("innerText"))!="Access"){
					System.out.println ("This is the value of col header index: "+c)
					hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
					System.out.println ("This is the value of header data from the else condition: "+hdrdata)
				}
			}
		}else if (appKey.equals("Bento") && (crntUrl.contains("/fileCentricCart"))){
			switchBento = getPageSwitch();
			switchString = "Bento";
			System.out.println("Value of BENTO switch string returned by getcurrentpage function: "+switchBento)
			nxtBtn =  driver.findElement(By.xpath(givexpath(nxtb1)));
			System.out.println("This is the value of next button from Bento mycart switch: "+nxtBtn)
			if(statValue==0){
				System.out.println ("No files in the cart")
			}else{
				columns_count = (colHeader.size())-1
				for(int c=0;c<columns_count;c++){
					hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
				}
			}
		}

		else if (appKey.equals("Bento") && (crntUrl.contains("/programs"))){
			switchBento = getPageSwitch();
			switchString = "Bento";
			System.out.println ("Value of BENTO switch string: " + switchBento)
			nxtBtn =  driver.findElement(By.xpath(givexpath(nxtb1)));
			System.out.println("Value of next button from Bento pgm page switch: "+nxtBtn)
			if(statValue==0){
				System.out.println ("No records for pgms")
			}else{
				columns_count = (colHeader.size())
				for(int c=0;c<columns_count;c++){
					hdrdata = hdrdata + (colHeader.get(c).getAttribute("innerText")) + "||"
				}
			}
		}
		// Write header data
		wTableHdrData.add(hdrdata);
		System.out.println("Number of columns in the current result tab is:  "+ columns_count)
		System.out.println("Complete list of column headers in current tab:  "+ wTableHdrData)
		System.out.println("Total number of rows to read:  "+statValue);



		//@@@@@@@@@@@@@@@@@@  COLLECTING THE TABLE BODY DATA @@@@@@@@@@@@@@@
		int counter=1;

		if(appKey.equals("CCDI")) {
			Utils.changePaginationResultsPerPage100();

			// For Files tab validation for applicable studies, click on GUID column to sort
			if (isGuidEligibleStudy()) {
				WebUI.comment("GUID-enabled study detected → sorting table by GUID")
				WebUI.click(findTestObject('Object Repository/CCDI/ExplorePage/Files_ResultsTab_GUIDCol'))
				WebUI.delay(1)
			} else {
				WebUI.comment("GUID sorting is not needed for this study → no action taken")
			}
			counter=10;
		}

		if (statValue !=0) {

			while (counter <= 10) {
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(GlobalVariable.G_cannine_caseTblBdy)));   //the name is misleading but it is only a placeholder for all the applications
				scrolltoViewjs(driver.findElement(By.xpath(GlobalVariable.G_cannine_caseTblBdy)))
				TableBdy =driver.findElement(By.xpath(GlobalVariable.G_cannine_caseTblBdy))
				Thread.sleep(2000) //Check first and then delete
				rows_table = TableBdy.findElements(By.tagName("tr"))
				rows_count = rows_table.size()
				System.out.println("Number  of rows per  page is:  "+(rows_count))

				int i;

				for(i = 1; i <= rows_count; i++) {

					String data = ""

					//@@@@@@@@@@@@@@@@ CDS table data collection starts here  @@@@@@@@@@@@@@@@
					if(switchString == "CDS"){
						switch(switchCDS){
							case("/data"):

								int tblcol=GlobalVariable.G_rowcountFiles

								if((tbl_main).equals('//div[@id="case_tab_table"]')){
									tblcol=tblcol-3;
									for (int j = 1; j <=tblcol; j = j +1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + (j+1) +"]/*[1]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}else if((tbl_main).equals('//*[@id="sample_tab_table"]')){
									tblcol=tblcol-4;
									for (int j = 1; j <=tblcol; j = j +1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + (j+1) +"]/*[1]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}else if((tbl_main).equals('//*[@id="file_tab_table"]')){
									tblcol=tblcol-1;
									for (int j = 1; j <=tblcol; j = j +1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + (j+1) +"]/*[1]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}
								break;
							default:
								System.err.println("Check CDS switch statment for this error")
								break;
						}
					}

					// @@@@@@@@@@@@@@@@  INS table data collection starts here @@@@@@@@@@@@@@@@
					if(switchString == "INS"){
						System.out.println("Inside INS Switch Structure")

						switch(switchINS){
							case("/programs"):
								int tblcol=GlobalVariable.G_rowcount_Katalon; //13
								if((tbl_main).equals("//*[@id='program_tab_table']")){
									tblcol=tblcol-9;
									for (int j = 0; j <=tblcol; j = j +1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]//td[" + (j+1) +"]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}else if((tbl_main).equals("//*[@id='project_tab_table']")){
									tblcol=tblcol-8;
									for (int j = 0; j <=tblcol; j = j +1) {
										//Added below if statement to skip Programs(s) col as it has multiple ids - maybe fix later
										if((colHeader.get(j).getAttribute("innerText") != "Program(s)")) {
											System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
											String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]//td[" + (j+1) +"]")).getAttribute("innerText")))
											data = data + value + "||"
											System.out.println("This is the value of  table  cell:  "+value)
										}
									}
								}else if((tbl_main).equals("//*[@id='grant_tab_table']")){
									tblcol=tblcol-7;
									for (int j = 0; j <=tblcol; j = j +1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]//td[" + (j+1) +"]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}else if((tbl_main).equals("//*[@id='publication_tab_table']")){
									tblcol=tblcol-7;
									for (int j = 0; j <=tblcol; j = j +1) {
										//Added below line to skip Projects col as it has multiple link - maybe fix later
										if((colHeader.get(j).getAttribute("innerText") != "Project(s)")) {
											System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
											String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]//td[" + (j+1) +"]//p")).getAttribute("innerText")))
											data = data + value + "||"
											System.out.println("This is the value of  table  cell:  "+value)
										}
									}
								}
								break;
							default:
								System.err.println("Check INS switch statment for this error")
								break;
						}
					}

					//@@@@@@@@@@@@@@@@ C3DC table data collection starts here  @@@@@@@@@@@@@@@@
					if(switchString == "C3DC"){
						switch(switchC3DC){

							case("/explore"):

								int tblcol=GlobalVariable.G_rowcountFiles

								if((tbl_main).equals("//*[@id='study_tab_table']")){
									tblcol=tblcol-6;
									for (int j = 0; j <tblcol; j = j +1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}else if((tbl_main).equals("//*[@id='participant_tab_table']")){
									tblcol=tblcol-3;
									for (int j = 1; j <tblcol; j =j+1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}else if((tbl_main).equals("//*[@id='diagnosis_tab_table']")){
									tblcol=tblcol+2;
									for (int j = 1; j <tblcol; j = j +1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}else if((tbl_main).equals("//*[@id='treatment_tab_table']")){
									tblcol=tblcol;
									for (int j = 1; j <tblcol; j = j +1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}else if((tbl_main).equals("//*[@id='treatment_response_tab_table']")){
									tblcol=tblcol-2;
									for (int j = 1; j <tblcol; j = j +1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}else if((tbl_main).equals("//*[@id='survival_tab_table']")){
									tblcol=tblcol;
									for (int j = 1; j <tblcol; j = j +1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}
								break;
							default:
								System.err.println("Check C3DC switch statment for this error")
								break;
						}
					}

					//					//@@@@@@@@@@@@@@@@ CCDI table data collection starts here  @@@@@@@@@@@@@@@@  added on 8th Sep 2023
					//					if(switchString == "CCDI"){
					//						switch(switchCCDI){
					//							case("/explore"):
					//								System.out.println("Inside CCDI switch case for body data")
					//								int tblcol=GlobalVariable.G_rowcountFiles
					//								System.out.println ("This is the value of tblcol from CCDI body data :"+tblcol)
					//							//*[@id="tableContainer"]
					//								if((tbl_main).equals('//*[@id="participant_tab_table"]')){
					//									System.out.println("Inside CCDI participants switch")
					//									tblcol=tblcol+2;    //8-3=5 leaves out alternate id col   change to 8-2
					//									for (int j = 1; j <=tblcol; j = j +1) {
					//										System.out.println("Value of i is: "+ i +"\nValue of j is: " + j)
					//										System.out.println("This is the name of column header : "+colHeader.get(j).getAttribute("innerText"))
					//										//*[@id="participant_tab_table"]/div[2]/table/tbody/tr[1]/td[3]/p
					//										data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + (j+1) +"]/p")).getAttribute("innerText")) +"||")
					//										System.out.println("This is the value of data : "+data)
					//									}
					//								}else if((tbl_main).equals("//*[@id='study_tab_table']")){
					//									System.out.println("Inside CCDI studies switch. This is the value of the tblbody: "+tbl_bdy) //*[@id='study_tab_table']//tbody
					//									tblcol=tblcol+5;
					//									System.out.println("Value of tblcol from the studies section is: "+tblcol)
					//									for (int j = 1; j <=tblcol; j = j +1) {
					//										System.out.println("Value of i is: "+ i +"\nValue of j is: " + j)
					//										// only for two cols with (top 5) the xpath will not have the /p tag
					//
					//										if(((tbl_main).equals("//*[@id='study_tab_table']")) && (colHeader.get(j).getAttribute("innerText")=="Diagnosis (Top 5)")) {
					//											System.out.println("This is the name of column header : "+colHeader.get(j).getAttribute("innerText"))
					//											data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + (j+1) +"]")).getAttribute("innerText")) +"||")
					//										}else if(((tbl_main).equals("//*[@id='study_tab_table']")) && (colHeader.get(j).getAttribute("innerText")=="Diagnosis Anatomic Site (Top 5)")) {
					//											System.out.println("This is the name of column header : "+colHeader.get(j).getAttribute("innerText"))
					//											data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + (j+1) +"]")).getAttribute("innerText")) +"||")
					//										}else if(((tbl_main).equals("//*[@id='study_tab_table']")) && (colHeader.get(j).getAttribute("innerText")=="File Type (Top 5)")) {
					//											System.out.println("This is the name of column header : "+colHeader.get(j).getAttribute("innerText"))
					//											data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + (j+1) +"]")).getAttribute("innerText")) +"||")
					//										}else if (!colHeader.get(j).getAttribute("innerText").equals("Manifest")){
					//
					//											System.out.println("This is the name of column header : "+colHeader.get(j).getAttribute("innerText"))
					//											//*[@id="study_tab_table"]//tbody/tr[1]/td[2]/p  -- this is the updated one
					//
					//											data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")) +"||")
					//										}
					//										System.out.println("This is the value of data : "+data)
					//									}
					//								}else if((tbl_main).equals("//*[@id='sample_tab_table']")){
					//									System.out.println("Inside CCDI samples tab switch")
					//									tblcol=tblcol-1;
					//									System.out.println("Value of tblcol from the samples section is: "+tblcol)
					//									for (int j = 1; j <=tblcol; j = j +1) {
					//										System.out.println("Value of i is: "+ i +"\nValue of j is: " + j)
					//										// only for one col the xpath will not have the /p tag
					//										//*[@id="sample_tab_table"]/div[2]/table/tbody/tr[1]/td[6]
					//										if(((tbl_main).equals("//*[@id='sample_tab_table']")) && (colHeader.get(j).getAttribute("innerText")=="Age at Sample Collection (days)")) {
					//											System.out.println("This is the name of column header : "+colHeader.get(j).getAttribute("innerText"))
					//											data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + (j+1) +"]")).getAttribute("innerText")) +"||")
					//										}else {
					//											System.out.println("This is the name of column header : "+colHeader.get(j).getAttribute("innerText"))
					//											//*[@id="sample_tab_table"]/div[2]/table/tbody/tr[1]/td[2]/p
					//											data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + (j+1) +"]/p")).getAttribute("innerText")) +"||")
					//										}
					//										System.out.println("This is the value of data : "+data)
					//									}
					//								}else if((tbl_main).equals('//*[@id="file_tab_table"]')){
					//									System.out.println("Inside CCDI files tab switch")
					//									tblcol=tblcol+1;
					//									for (int j = 1; j <=tblcol; j = j +1) {
					//										System.out.println("Value of i is: "+ i +"\nValue of j is: " + j)
					//										//*[@id="file_tab_table"]//tbody/tr[1]/*[2]/*[2]
					//										System.out.println("This is the name of column header : "+colHeader.get(j).getAttribute("innerText"))
					//										//data = data + ((driver.findElement(By.xpath(tbl_bdy +"//tr[" + i + "]//td[" + (j+1) +"]/p")).getAttribute("innerText")) +"||")
					//										data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + (j+1) +"]")).getAttribute("innerText")) +"||")
					//										System.out.println("This is the value of data : "+data)
					//									}
					//								}
					//								break;
					//							default:
					//								System.err.println("Check CCDI switch statment for this error")
					//								break;
					//						}
					//					}

					//@@@@@@@@@@@@@@@@ CCDI table data collection starts here  @@@@@@@@@@@@@@@@  edited 12/04/25 - VL
					if (switchString == "CCDI") {
						switch (switchCCDI) {
							case ("/explore"):
								System.out.println("Inside CCDI switch case for body data")

							// IMPORTANT: default tblcol still comes from global, but we will
							// override it for specific tabs (especially file_tab_table).
								int tblcol = GlobalVariable.G_rowcountFiles
								System.out.println("This is the value of tblcol from CCDI body data :" + tblcol)

								if ((tbl_main).equals('//*[@id="participant_tab_table"]')) {
									System.out.println("Inside CCDI participants switch")
									tblcol = tblcol + 2   // existing logic
									for (int j = 1; j <= tblcol; j++) {
										System.out.println("Value of i is: " + i + "\nValue of j is: " + j)
										System.out.println("This is the name of column header : " + colHeader.get(j).getAttribute("innerText"))

										data = data + (
												driver.findElement(
												By.xpath(tbl_bdy + "/tr[" + i + "]/*[" + (j + 1) + "]/p")
												).getAttribute("innerText")
												) + "||"
										System.out.println("This is the value of data : " + data)
									}
								} else if ((tbl_main).equals("//*[@id='study_tab_table']")) {
									System.out.println("Inside CCDI studies switch. This is the value of the tblbody: " + tbl_bdy)
									tblcol = tblcol + 5
									System.out.println("Value of tblcol from the studies section is: " + tblcol)

									for (int j = 1; j <= tblcol; j++) {
										System.out.println("Value of i is: " + i + "\nValue of j is: " + j)

										String headerText = colHeader.get(j).getAttribute("innerText")
										if (((tbl_main).equals("//*[@id='study_tab_table']")) && headerText == "Diagnosis (Top 5)") {
											System.out.println("This is the name of column header : " + headerText)
											data = data + (
													driver.findElement(
													By.xpath(tbl_bdy + "/tr[" + i + "]/*[" + (j + 1) + "]")
													).getAttribute("innerText")
													) + "||"
										} else if (((tbl_main).equals("//*[@id='study_tab_table']")) && headerText == "Diagnosis Anatomic Site (Top 5)") {
											System.out.println("This is the name of column header : " + headerText)
											data = data + (
													driver.findElement(
													By.xpath(tbl_bdy + "/tr[" + i + "]/*[" + (j + 1) + "]")
													).getAttribute("innerText")
													) + "||"
										} else if (((tbl_main).equals("//*[@id='study_tab_table']")) && headerText == "File Type (Top 5)") {
											System.out.println("This is the name of column header : " + headerText)
											data = data + (
													driver.findElement(
													By.xpath(tbl_bdy + "/tr[" + i + "]/*[" + (j + 1) + "]")
													).getAttribute("innerText")
													) + "||"
										} else if (!"Manifest".equals(headerText)) {
											System.out.println("This is the name of column header : " + headerText)
											data = data + (
													driver.findElement(
													By.xpath(tbl_bdy + "/tr[" + i + "]/td[" + (j + 1) + "]")
													).getAttribute("innerText")
													) + "||"
										}
										System.out.println("This is the value of data : " + data)
									}
								} else if ((tbl_main).equals("//*[@id='sample_tab_table']")) {
									System.out.println("Inside CCDI samples tab switch")
									tblcol = tblcol - 1
									System.out.println("Value of tblcol from the samples section is: " + tblcol)

									for (int j = 1; j <= tblcol; j++) {
										System.out.println("Value of i is: " + i + "\nValue of j is: " + j)
										String headerText = colHeader.get(j).getAttribute("innerText")

										if (((tbl_main).equals("//*[@id='sample_tab_table']")) && headerText == "Age at Sample Collection (days)") {
											System.out.println("This is the name of column header : " + headerText)
											data = data + (
													driver.findElement(
													By.xpath(tbl_bdy + "/tr[" + i + "]/*[" + (j + 1) + "]")
													).getAttribute("innerText")
													) + "||"
										} else {
											System.out.println("This is the name of column header : " + headerText)
											data = data + (
													driver.findElement(
													By.xpath(tbl_bdy + "/tr[" + i + "]/*[" + (j + 1) + "]/p")
													).getAttribute("innerText")
													) + "||"
										}
										System.out.println("This is the value of data : " + data)
									}
								} else if ((tbl_main).equals('//*[@id="file_tab_table"]')) {
									System.out.println("Inside CCDI files tab switch")

									// 🔴 KEY FIX: derive tblcol from actual header count (checkbox + N data cols)
									// colHeader[0] = checkbox
									int totalHeaders = colHeader.size()  // e.g. 11 when GUID is visible
									tblcol = totalHeaders - 1            // 10 real data columns: File Name..GUID
									System.out.println("Value of tblcol from the files section is: " + tblcol)

									// j = 1..tblcol → File Name..GUID (because header index 0 is checkbox)
									for (int j = 1; j <= tblcol; j++) {
										System.out.println("Value of i is: " + i + "\nValue of j is: " + j)
										String headerText = colHeader.get(j).getAttribute("innerText")
										System.out.println("This is the name of column header : " + headerText)

										// td[1] = checkbox, so shift by +1 for data
										data = data + (
												driver.findElement(
												By.xpath(tbl_bdy + "/tr[" + i + "]/*[" + (j + 1) + "]")
												).getAttribute("innerText")
												) + "||"

										System.out.println("This is the value of data : " + data)
									}
								}
								break

							default:
								System.err.println("Check CCDI switch statement for this error")
								break
						}
					}


					// @@@@@@@@@@@@@@@@  Canine table data collection starts here @@@@@@@@@@@@@@@@
					if(switchString == "Canine"){
						System.out.println("Inside Canine Switch Structure")
						switch(switchCanine){

							case("/explore"):
								int tblcol=GlobalVariable.G_rowcount_Katalon;

								if((tbl_main).equals("//*[@id='case_tab_table']")){
									//This is for cases tab
									data = ""
									System.out.println("This is the val of tblcol: "+tblcol+"\nThis is the output of data: "+ data)
									for (int j = 2; j<= tblcol; j = j + 1) {
										//?
										System.out.println("Value of i is: "+i+"\nValue of j is: "+j)

										if( ((tbl_main).equals("//*[@id='case_tab_table']")) && (colHeader.get(j-2).getAttribute("innerText")=="Case ID")){
											//j-1
											System.out.println("Inside the dog filter control structure")
											data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + j +"]/div/a")).getAttribute("innerText").trim()) +"||")
											System.out.println("This is the data after filtering for dog icon :"+data)
										}else {
											data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + j + "]")).getAttribute("innerText")) +"||")
											System.out.println("This is the value of data : "+data)
										}
									}
								}else if((tbl_main).equals("//*[@id='sample_tab_table']")){
									//This is for samples tab
									data = ""
									System.out.println("This is the val of tblcol: "+tblcol+"\nThis is the output of data: "+ data)
									for (int j = 2; j<= tblcol; j = j + 1) {
										System.out.println("Value of i is: "+i+"\nValue of j is: "+j)
										data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + j + "]")).getAttribute("innerText")) +"||")
										System.out.println("This is the value of data : "+data)
									}
								}else if((tbl_main).equals("//*[@id='file_tab_table']")){
									//This is for case file tab
									tblcol=tblcol-2  // this is needed when files tab has 11 cols
									System.out.println("This is the count of tblcol when files tab is selected: "+tblcol)
									for (int j = 0; j< tblcol; j = j + 1) {
										System.out.println("Value of i is: "+i+"\nValue of j is: "+j)

										if((colHeader.get(j).getAttribute("innerText"))!="Access") {
											System.out.println("This is the name of column header: "+colHeader.get(j).getAttribute("innerText"))
											data = data + ( (driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+2) +"]")).getAttribute("innerText").trim()) +"||")//(j+1)
											System.out.println("This is the data after filtering for dog icon :"+data)
										}
									}
								}else if((tbl_main).equals("//table")){
									//This is for case file tab
									tblcol=tblcol-5  // this is needed when study files has 8 cols
									System.out.println("This is the count of tblcol when study files tab is selected: "+tblcol)
									for (int j = 0; j< tblcol; j = j + 1) {
										System.out.println("Value of i is: "+i+"\nValue of j is: "+j)

										if((colHeader.get(j).getAttribute("innerText"))!="Access") {
											System.out.println("This is the name of column header  :"+colHeader.get(j).getAttribute("innerText"))
											data = data + ((driver.findElement(By.xpath(tbl_bdy +"//tr" + "[" + i + "]/td[" + (j+2) +"]")).getAttribute("innerText")) +"||")
											System.out.println("This is the value of data :"+data)
										}
									}
								}else{
									System.err.println("Invalid Tab! Could not read the tab")
								}
								break;

							case("/fileCentricCart"):
								System.out.println("Inside filecentric cart case of ICDC - for 10 cols after excluding Access and Remove");
								int tblcol=GlobalVariable.G_rowcount_Katalon;
								System.out.println("This is the val of tblcol: "+tblcol)
							//i=i-1; // to start from 0 and include the first column
								System.out.println("**************** "+ data)
								data = ""
								for (int j = 2; j<= tblcol-2; j = j + 1) {
									System.out.println("Value of i is: "+i+"\nValue of j is: "+j)
									//*[@id='table_selected_files']//tbody/tr[1]/td[2]/div[2]   where div[2] is a constant
									data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/td[" + j + "]/div[2]")).getAttribute("innerText")) +"||")
									System.out.println("This is the value of data :"+data)
								}
								break;
							default:
								System.out.println("Canine Case did not match")
								break;
						} //canine switch ends here
					}//canine if ends here

					//@@@@@@@@@@@@@@@@ CTDC table data collection starts here  @@@@@@@@@@@@@@@@
					if(switchString == "CTDC"){
						switch(switchCTDC){

							case("/explore"):

								int tblcol=GlobalVariable.G_rowcountFiles

								if((tbl_main).equals('//*[@id="participants_tab_table"]')){
									tblcol=tblcol+1;
									for (int j = 1; j <tblcol; j = j +1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}else if((tbl_main).equals('//*[@id="biospecimens_tab_table"]')){
									tblcol=tblcol;
									for (int j = 1; j <tblcol; j = j +1) {
										System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
										String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")))
										data = data + value + "||"
										System.out.println("This is the value of  table  cell:  "+value)
									}
								}else if((tbl_main).equals('//*[@id="file_tab_table"]')){
									tblcol=tblcol+2;
									for (int j = 1; j <tblcol; j = j +1) {
										if((colHeader.get(j).getAttribute("innerText"))!="Access") {
											System.out.println("This is the name of column header:  "+colHeader.get(j).getAttribute("innerText"))
											String value = ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")))
											data = data + value + "||"
											System.out.println("This is the value of  table  cell:  "+value)
										}
									}
								}
								break;
							default:
								System.err.println("Check CTDC switch statment for this error")
								break;
						}
					}
					//@@@@@@@@@@@@@@@@ Bento table data collection starts here  @@@@@@@@@@@@@@@
					if(switchString == "Bento"){
						System.out.println("inside Bento switch structure");

						switch(switchBento){

							case("/explore"):
								System.out.println("Inside Bento switch for all cases")
								int tblcol=GlobalVariable.G_rowcount_Katalon;
								data = ""
								System.out.println("Value of columns_count variable : "+columns_count+"\nValue of tblcol variable : "+tblcol)

								if((tbl_main).equals('//*[@id="case_tab_table"]')) {
									for (int j = 1; j <columns_count+1; j = j + 1) {
										System.out.println("Value of i is: "+ i +"\nValue of j is: " + j)
										data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")) +"||")
										System.out.println("This is the value of data : "+data)
									}
								}else if((tbl_main).equals('//*[@id="sample_tab_table"]')){
									for (int j = 1; j <columns_count+1; j = j + 1) {
										System.out.println("Value of i is: "+ i +"\nValue of j is: " + j)
										data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")) +"||")
										System.out.println("This is the value of data : "+data)
									}
								}else if((tbl_main).equals('//*[@id="file_tab_table"]')){
									for (int j = 1; j <columns_count+1; j = j + 1) {
										if((colHeader.get(j).getAttribute("innerText"))!="Access") {
											System.out.println("Value of i is: "+ i +"\nValue of j is: " + j)
											data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr[" + i + "]/td[" + (j+1) +"]")).getAttribute("innerText")) +"||")
											System.out.println("This is the value of data : "+data)
										}
									}
								}else {
									KeywordUtil.markFailed("Invalid Tab name! Check Bento table data collection function")
								}

								break;
							case("/fileCentricCart"):
								int tblcol=GlobalVariable.G_rowcount_Katalon;
								for (int j = 1; j < columns_count+tblcol; j = j + 1) {
									System.out.println("Value of i is: "+i+"\nValue of j is: "+j)
									data = data+((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + j + "]")).getAttribute("innerText")) +"||")
									System.out.println("This is the value of data : "+data)
								}
								break;
							case("/programs"):
								System.out.println("Inside Bento switch for all Programs cases")
								int tblcol=GlobalVariable.G_rowcount_Katalon;
								System.out.println("Value of columns_count variable : "+columns_count + "\nValue of tblcol variable : "+tblcol)
								for (int j = 1; j < columns_count+1; j = j + 1) {
									System.out.println("Value of i is: "+ i +"\nValue of j is: "+j)

									//if((colHeader.get(j).getAttribute("innerText"))!="PubMed ID") {
									//System.out.println("This is the name of Pgm table column header  :"+colHeader.get(j).getAttribute("innerText"))
									//System.out.println("This is the value of data before calculating the index for innertext of the td: "+data)

									//data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + (j+1) + "]/*[2]")).getAttribute("innerText")) +"||")
									data = data + ((driver.findElement(By.xpath(tbl_bdy +"/tr" + "[" + i + "]/*[" + j + "]")).getAttribute("innerText").trim()) +"||")
									System.out.println("This is the value of data :"+data)
									//}
								}
								break;

							default:
								KeywordUtil.markFailed("Bento Case switch did not match")
								break;
						}
					}
					wTableBodyData.add(data)
				}//for loop ends


				//System.out.println("Size of table body list in current result tab is: "+wTableBodyData.size())
				for(int index = 0; index < wTableBodyData.size(); index++) {
					System.out.println("Table body data from current page is: " + wTableBodyData.get(index))
				}
				GlobalVariable.G_CaseData= wTableHdrData + wTableBodyData;

				nextButton = driver.findElement(By.xpath(nxt_str));
				scrolltoViewjs(nextButton)   //added to address the unable to scroll into view issue/ another element obscures next button issue

				if (nextButton.getAttribute("disabled")){
					break;
				} else {
					System.out.println("COLLECTED DATA FROM PAGE - " +counter);
					clickElement(nextButton); //uses jsexecutor to click
					System.out.println("Clicked next button");
					counter++;
				}
			}//while loop ends
		} //if loop for body data collection ends
		else {
			System.out.println("Not collecting the table data as the stat value is 0")
		}

		Utils.writeToExcel(webSheetName);
	}//ReadCasesTableKatalon function ends


	/**
	 * This function reads Bento Statbar
	 * @param bProgs
	 * @param bArms
	 * @param bCases
	 * @param bSamples
	 * @param bAssays
	 * @param bFiles
	 */
	@Keyword
	public void readStatBarBento(String bProgs, String bArms, String bCases, String bSamples, String bAssays, String bFiles) {
		Thread.sleep(8000);

		String xbProgs = givexpath(bProgs)
		String xbArms = givexpath(bArms)
		String xbCases = givexpath(bCases)
		String xbSamples = givexpath(bSamples)
		String xbAssays = givexpath(bAssays)
		String xbFiles = givexpath(bFiles)

		GlobalVariable.G_StatBar_Programs = driver.findElement(By.xpath(xbProgs)).getAttribute('innerHTML');
		System.out.println("This is the value of Programs count from Stat bar :"+GlobalVariable.G_StatBar_Programs)
		GlobalVariable.G_StatBar_Arms = driver.findElement(By.xpath(xbArms)).getAttribute('innerHTML');
		System.out.println("This is the value of Arms count from Stat bar :"+GlobalVariable.G_StatBar_Arms)
		GlobalVariable.G_StatBar_Cases = driver.findElement(By.xpath(xbCases)).getText();
		System.out.println("This is the value of Cases count from Stat bar :"+GlobalVariable.G_StatBar_Cases)
		GlobalVariable.G_StatBar_Samples = driver.findElement(By.xpath(xbSamples)).getText();
		System.out.println("This is the value of Samples count from Stat bar :"+GlobalVariable.G_StatBar_Samples)
		GlobalVariable.G_StatBar_Assays = driver.findElement(By.xpath(xbAssays)).getText();
		System.out.println("This is the value of Assays count from Stat bar :"+GlobalVariable.G_StatBar_Assays)
		GlobalVariable.G_StatBar_Files = driver.findElement(By.xpath(xbFiles)).getText();
		System.out.println("This is the value of Files count from Stat bar :"+GlobalVariable.G_StatBar_Files)
	}

	/**
	 * This function reads the count displayed near the cart icon in ICDC
	 * @param cmyCartCount
	 */
	@Keyword
	public void readMyCartCount(String cmyCartCount) {
		Thread.sleep(5000);
		String xcmyCartCount = givexpath(cmyCartCount)
		Thread.sleep(2000)
		GlobalVariable.G_myCartTotal = driver.findElement(By.xpath(xcmyCartCount)).getAttribute("innerText");
		System.out.println("This is the value of count from cart icon :"+GlobalVariable.G_myCartTotal)
	}

	/**
	 * This function reads Canine Statbar
	 * @param cProgs
	 * @param cStuds
	 * @param cCases
	 * @param cSamples
	 * @param cFiles
	 * @param cStudyFiles
	 */
	@Keyword
	public void readStatBarCanine(String cProgs, String cStuds, String cCases, String cSamples, String cFiles, String cStudyFiles) {
		Thread.sleep(5000);

		String xcProgs = givexpath(cProgs)
		String xcStuds = givexpath(cStuds)
		String xcCases = givexpath(cCases)
		String xcSamples = givexpath(cSamples)
		String xcFiles = givexpath(cFiles)
		String xcStudyFiles = givexpath(cStudyFiles)

		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Programs = driver.findElement(By.xpath(xcProgs)).getAttribute("innerText");
		System.out.println("This is the value of Programs count from Stat bar: "+GlobalVariable.G_StatBar_Programs)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Studies = driver.findElement(By.xpath(xcStuds)).getAttribute("innerText");
		System.out.println("This is the value of Studies count from Stat bar: "+GlobalVariable.G_StatBar_Studies)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Cases = driver.findElement(By.xpath(xcCases)).getAttribute("innerText");
		System.out.println("This is the value of Cases count from Stat bar: "+GlobalVariable.G_StatBar_Cases)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Samples = driver.findElement(By.xpath(xcSamples)).getAttribute("innerText");
		System.out.println("This is the value of Samples count from Stat bar: "+GlobalVariable.G_StatBar_Samples)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Files = driver.findElement(By.xpath(xcFiles)).getAttribute("innerText");
		System.out.println("This is the value of Case Files count from Stat bar: "+GlobalVariable.G_StatBar_Files)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_StudyFiles = driver.findElement(By.xpath(xcStudyFiles)).getAttribute("innerText");
		System.out.println("This is the value of Study Files count from Stat bar: "+GlobalVariable.G_StatBar_StudyFiles)
	}

	/**
	 * This function reads CCDI Hub Statbar
	 * @param cStuds
	 * @param cParticip
	 * @param cSamples
	 * @param cFiles
	 //@param cDiag  - this will be used later when diag is available in stat bar
	 */
	@Keyword
	public void readStatBarCCDIhub (String cStuds, String cParticip, String cSamples, String cFiles) {
		Thread.sleep(5000);

		String xcStuds = givexpath(cStuds)
		String xcParticip = givexpath(cParticip)
		String xcSamples = givexpath(cSamples)
		String xcFiles = givexpath(cFiles)

		System.out.println ("Inside read stat bar function for CCDI");

		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Studies = driver.findElement(By.xpath(xcStuds)).getAttribute("innerText").replace(',','');
		System.out.println("This is the value of Studies count from Stat bar: "+GlobalVariable.G_StatBar_Studies)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Participants = driver.findElement(By.xpath(xcParticip)).getAttribute("innerText").replace(',','');
		System.out.println("This is the value of Participants count from Stat bar: "+GlobalVariable.G_StatBar_Participants)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Samples = driver.findElement(By.xpath(xcSamples)).getAttribute("innerText").replace(',','');
		System.out.println("This is the value of Samples count from Stat bar: "+GlobalVariable.G_StatBar_Samples)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Files = driver.findElement(By.xpath(xcFiles)).getAttribute("innerText").replace(',','');
		System.out.println("This is the value of Case Files count from Stat bar: "+GlobalVariable.G_StatBar_Files)
		Thread.sleep(2000)
	}


	/**
	 * This function reads C3DC Hub Statbar
	 * @param cDiag
	 * @param cParticip
	 * @param cStudies
	 //@param cDiag  - this will be used later when diag is available in stat bar
	 */
	@Keyword
	public void readStatBarC3DC(String cDiag, String cParticip, String cStudies) {
		Thread.sleep(5000);

		String xcDiag = givexpath(cDiag)
		String xcParticip = givexpath(cParticip)
		String xcSamples = givexpath(cStudies)

		System.out.println ("Inside read stat bar function for C3DC");

		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Diagnosis = driver.findElement(By.xpath(xcDiag)).getAttribute("innerText");
		System.out.println("This is the value of Diagnosis count from Stat bar: "+GlobalVariable.G_StatBar_Diagnosis)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Participants = driver.findElement(By.xpath(xcParticip)).getAttribute("innerText").replace(',','');
		System.out.println("This is the value of Participants count from Stat bar: "+GlobalVariable.G_StatBar_Participants)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Studies = driver.findElement(By.xpath(xcSamples)).getAttribute("innerText");
		System.out.println("This is the value of Studies count from Stat bar: "+GlobalVariable.G_StatBar_Studies)
		Thread.sleep(2000)
	}


	/**
	 * This function reads CTDC Stat-bar
	 * @param stdCount
	 * @param partCount
	 * @param diagCount
	 * @param trgtTherapCount
	 * @param biospeciCount
	 * @param filesCount
	 */
	@Keyword
	public void readStatBarCTDC(String stdCount, String partCount, String diagCount, String trgtTherapCount, String biospeciCount, String filesCount){
		String studies = givexpath(stdCount)
		String participants = givexpath(partCount)
		String diagnoses = givexpath(diagCount)
		String targetedTherapies = givexpath(trgtTherapCount)
		String biospeciens = givexpath(biospeciCount)
		String files = givexpath(filesCount)

		Thread.sleep(1000)
		GlobalVariable.G_StatBar_Studies = driver.findElement(By.xpath(studies)).getAttribute("innerText");
		System.out.println("Value of Studies count from Stat bar: "+GlobalVariable.G_StatBar_Studies)
		Thread.sleep(1000)
		GlobalVariable.G_StatBar_Participants = driver.findElement(By.xpath(participants)).getAttribute("innerText");
		System.out.println("Value of Participants count from Stat bar: "+GlobalVariable.G_StatBar_Participants)
		Thread.sleep(1000)
		GlobalVariable.G_StatBar_Diagnosis = driver.findElement(By.xpath(diagnoses)).getAttribute("innerText");
		System.out.println("Value of Diagnoses count from Stat bar: "+GlobalVariable.G_StatBar_Diagnosis)
		Thread.sleep(1000)
		GlobalVariable.G_StatBar_Projects = driver.findElement(By.xpath(targetedTherapies)).getText();
		System.out.println("Value of Targeted Therapies count from Stat bar: "+GlobalVariable.G_StatBar_Projects)
		Thread.sleep(1000)
		GlobalVariable.G_StatBar_Grants = driver.findElement(By.xpath(biospeciens)).getText();
		System.out.println("Value of Biospecimens count from Stat bar: "+GlobalVariable.G_StatBar_Grants)
		Thread.sleep(1000)
		GlobalVariable.G_StatBar_Files = driver.findElement(By.xpath(files)).getAttribute("innerText");
		System.out.println("Value of Files count from Stat bar: "+GlobalVariable.G_StatBar_Files)
	}

	@Keyword
	public void readStatBarINS(String tProgs, String tProjs, String tGrants, String tPubs){
		String xpProgs = givexpath(tProgs)
		String xpProjs = givexpath(tProjs)
		String xpGrants = givexpath(tGrants)
		String xpPubs = givexpath(tPubs)

		GlobalVariable.G_StatBar_Programs = driver.findElement(By.xpath(xpProgs)).getText();
		System.out.println("This is the value of Programs count from Stat bar: "+GlobalVariable.G_StatBar_Programs)
		GlobalVariable.G_StatBar_Projects = driver.findElement(By.xpath(xpProjs)).getText();
		System.out.println("This is the value of Projects count from Stat bar: "+GlobalVariable.G_StatBar_Projects)
		GlobalVariable.G_StatBar_Grants = driver.findElement(By.xpath(xpGrants)).getText();
		System.out.println("This is the value of Grants count from Stat bar: "+GlobalVariable.G_StatBar_Grants)
		GlobalVariable.G_StatBar_Publications = driver.findElement(By.xpath(xpPubs)).getText();
		System.out.println("This is the value of Publications count from Stat bar: "+GlobalVariable.G_StatBar_Publications)
	}


	/**
	 * This function reads CDS Statbar
	 * @param cdsStuds
	 * @param cdsDisesSite
	 * @param cdsParticipants
	 * @param cdsSamples
	 * @param cdsFiles
	 */
	@Keyword
	public void readStatBarCDS(String cdsStuds, String cdsParticipants, String cdsSamples, String cdsFiles) {
		Thread.sleep(5000);

		String cStuds = givexpath(cdsStuds)
		//String cdisSite = givexpath(cdsDisesSite)
		String cParticipants = givexpath(cdsParticipants)
		String cSamples = givexpath(cdsSamples)
		String cFiles = givexpath(cdsFiles)

		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Studies = driver.findElement(By.xpath(cStuds)).getAttribute('innerHTML');
		System.out.println("This is the value of Studies count from Stat bar:  "+GlobalVariable.G_StatBar_Studies)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Participants = driver.findElement(By.xpath(cParticipants)).getAttribute('innerHTML');
		System.out.println("This is the value of Particp count from Stat bar:  "+GlobalVariable.G_StatBar_Participants)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Samples = driver.findElement(By.xpath(cSamples)).getAttribute('innerHTML');
		System.out.println("This is the value of Samples count from Stat bar:  "+GlobalVariable.G_StatBar_Samples)
		Thread.sleep(2000)
		GlobalVariable.G_StatBar_Files = driver.findElement(By.xpath(cFiles)).getAttribute('innerHTML');
		System.out.println("This is the value  of Files  count from Stat bar:  "+GlobalVariable.G_StatBar_Files)
	}


	/**
	 * This function returns the xpath of a given string (from the obj stored in katalons object repository)
	 * @param objname
	 * @return xpath of the object
	 */
	@Keyword
	public static String givexpath(String objname) {
		TestObject obj = findTestObject(objname)
		String xpathOfObj = obj.findPropertyValue('xpath')
		System.out.println(xpathOfObj)
		return xpathOfObj;
	}


	/**
	 * This function validates stat-bar values
	 * @param getAppName Application name i.e 'CDS'
	 */
	@Keyword
	public static void validateStatBar(getAppName) {

		List<List<XSSFCell>> statData = new ArrayList<>()
		String neo4jfilename=  GlobalVariable.G_ResultPath.toString()
		statData = ReadExcel.readOutputExcel(neo4jfilename, GlobalVariable.G_StatTabname)

		if(getAppName=='Bento'){

			System.out.println("This is the value of Programs Count from TSV result: "+statData.get(0).get(0).getStringCellValue())
			System.out.println("This is the value of ARMS Count from TSV result: "+statData.get(0).get(1).getStringCellValue())
			System.out.println("This is the value of Cases Count from TSV result: "+statData.get(0).get(2).getStringCellValue())
			System.out.println("This is the value of Samples Count from TSV result: "+statData.get(0).get(3).getStringCellValue())
			System.out.println("This is the value of Assays Count from TSV result: "+statData.get(0).get(4).getStringCellValue())
			System.out.println("This is the value of Files Count from TSV result: "+statData.get(0).get(5).getStringCellValue())

			(statData.get(0).get(0).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Programs)) ? KeywordUtil.markPassed("Statbar Programs count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Programs count")
			(statData.get(0).get(1).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Arms)) ? KeywordUtil.markPassed("Statbar ARMS count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar ARMS count")
			(statData.get(0).get(2).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Cases)) ? KeywordUtil.markPassed("Statbar Cases count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Cases count")
			(statData.get(0).get(3).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Samples)) ? KeywordUtil.markPassed("Statbar Samples count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Samples count")
			(statData.get(0).get(4).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Assays)) ? KeywordUtil.markPassed("Statbar Assays count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Assays count")
			(statData.get(0).get(5).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Files)) ? KeywordUtil.markPassed("Statbar Files count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Files count")
		}else if (getAppName=='INS') {

			System.out.println("This is the value of Programs Count from TSV result "+statData.get(0).get(0).getStringCellValue())
			System.out.println("This is the value of Projects Count from TSV result "+statData.get(0).get(1).getStringCellValue())
			System.out.println("This is the value of Grants Count from TSV result "+statData.get(0).get(2).getStringCellValue())
			System.out.println("This is the value of Publications Count from TSV result "+statData.get(0).get(3).getStringCellValue())


			(statData.get(0).get(0).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Programs)) ? KeywordUtil.markPassed("Statbar Programs count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Programs count")
			(statData.get(0).get(1).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Projects)) ? KeywordUtil.markPassed("Statbar Projects count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Projects count")
			(statData.get(0).get(2).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Grants)) ? KeywordUtil.markPassed("Statbar Grants count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Grants count")
			(statData.get(0).get(3).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Publications)) ? KeywordUtil.markPassed("Statbar Publications count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Publications count")
		}else if (getAppName=='ICDC'){

			System.out.println ("control is in line 1842");
			System.out.println("This is the value of Programs Count from TSV result: "+statData.get(0).get(0).getStringCellValue())
			System.out.println("This is the value of Studies Count from TSV result: "+statData.get(0).get(1).getStringCellValue())
			System.out.println("This is the value of Cases Count from TSV result: "+statData.get(0).get(2).getStringCellValue())
			System.out.println("This is the value of Samples Count from TSV result: "+statData.get(0).get(3).getStringCellValue())
			System.out.println("This is the value of CaseFiles Count from TSV result: "+statData.get(0).get(4).getStringCellValue())
			System.out.println("This is the value of StudyFiles Count from TSV result: "+statData.get(0).get(5).getStringCellValue())

			(statData.get(0).get(0).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Programs)) ? KeywordUtil.markPassed("Statbar Programs count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Programs count")
			(statData.get(0).get(1).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Studies)) ? KeywordUtil.markPassed("Statbar Studies count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Studies count")
			(statData.get(0).get(2).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Cases)) ? KeywordUtil.markPassed("Statbar Cases count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Cases count")
			(statData.get(0).get(3).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Samples)) ? KeywordUtil.markPassed("Statbar Samples count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Samples count")
			(statData.get(0).get(4).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Files)) ? KeywordUtil.markPassed("Statbar Case Files count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Case Files count")
			(statData.get(0).get(5).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_StudyFiles)) ? KeywordUtil.markPassed("Statbar Study Files count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Study Files count")
		}else if (getAppName=='CTDC') {

			System.out.println("This is the value of Studies Count from TSV result "+statData.get(0).get(0).getStringCellValue())
			System.out.println("This is the value of Participants Count from TSV result "+statData.get(0).get(1).getStringCellValue())
			System.out.println("This is the value of Diagnoses Count from TSV result "+statData.get(0).get(2).getStringCellValue())
			System.out.println("This is the value of Targeted Therapies Count from TSV result "+statData.get(0).get(3).getStringCellValue())
			System.out.println("This is the value of Biospecimens Count from TSV result "+statData.get(0).get(4).getStringCellValue())
			System.out.println("This is the value of Files Count from TSV result "+statData.get(0).get(5).getStringCellValue())

			(statData.get(0).get(0).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Studies)) ? KeywordUtil.markPassed("Statbar Studies count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Studies count")
			(statData.get(0).get(1).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Participants)) ? KeywordUtil.markPassed("Statbar Participants count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Participant count")
			(statData.get(0).get(2).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Diagnosis)) ? KeywordUtil.markPassed("Statbar Diagnoses count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Diagnoses count")
			(statData.get(0).get(3).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Projects)) ? KeywordUtil.markPassed("Statbar Targeted Therapies count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Targeted Therapies count")
			(statData.get(0).get(4).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Grants)) ? KeywordUtil.markPassed("Statbar Biospicimens count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Biospicimens count")
			(statData.get(0).get(5).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Files)) ? KeywordUtil.markPassed("Statbar Files count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Files count")
		}else if (getAppName=='CDS'){

			System.out.println("This is the value of Studies Count from TSV result:  "+statData.get(0).get(0).getStringCellValue())  //add in the query in input file later
			System.out.println("This is the value of Partici Count from TSV result:  "+statData.get(0).get(1).getStringCellValue())
			System.out.println("This is the value of Samples Count from TSV result:  "+statData.get(0).get(2).getStringCellValue())
			System.out.println("This is the value of Files   Count from TSV result:  "+statData.get(0).get(3).getStringCellValue())

			(statData.get(0).get(0).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Studies)) ? KeywordUtil.markPassed("Statbar Studies count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Studies count")
			(statData.get(0).get(1).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Participants)) ? KeywordUtil.markPassed("Statbar Participants count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Participants count")
			(statData.get(0).get(2).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Samples)) ? KeywordUtil.markPassed("Statbar Samples count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Samples count")
			(statData.get(0).get(3).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Files)) ? KeywordUtil.markPassed("Statbar Files count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Files count")
		}else if (getAppName=='CCDI'){

			System.out.println("This is the value of Studies Count from TSV result: "+statData.get(0).get(0).getStringCellValue())  //add in the query in input file later
			System.out.println("This is the value of Participants Count from TSV result: "+statData.get(0).get(1).getStringCellValue())
			System.out.println("This is the value of Samples Count from TSV result: "+statData.get(0).get(2).getStringCellValue())
			System.out.println("This is the value of Files Count from TSV result: "+statData.get(0).get(3).getStringCellValue())

			(statData.get(0).get(0).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Studies)) ? KeywordUtil.markPassed("Statbar Studies count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Studies count")
			(statData.get(0).get(1).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Participants)) ? KeywordUtil.markPassed("Statbar Participants count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Participants count")
			(statData.get(0).get(2).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Samples)) ? KeywordUtil.markPassed("Statbar Samples count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Samples count")
			(statData.get(0).get(3).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Files)) ? KeywordUtil.markPassed("Statbar Files count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Files count")
		}else if (getAppName=='C3DC'){

			System.out.println("This is the value of Diagnoses Count from TSV result: "+statData.get(0).get(0).getStringCellValue())  //add in the query in input file later
			System.out.println("This is the value of Participants Count from TSV result: "+statData.get(0).get(1).getStringCellValue())
			System.out.println("This is the value of Studies Count from TSV result: "+statData.get(0).get(2).getStringCellValue())

			(statData.get(0).get(0).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Diagnosis)) ? KeywordUtil.markPassed("Statbar Diagnoses count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Diagnosis count")
			(statData.get(0).get(1).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Participants)) ? KeywordUtil.markPassed("Statbar Participants count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Participants count")
			(statData.get(0).get(2).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Studies)) ? KeywordUtil.markPassed("Statbar Studies count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Studies count")
		}
	}


	@Keyword
	public void validateTrialsStatBar() {
		List<List<XSSFCell>> statData = new ArrayList<>()
		String neo4jfilename=  GlobalVariable.G_ResultPath.toString()
		statData = ReadExcel.readExceltoWeblist(neo4jfilename,GlobalVariable.G_StatTabname)  //change the function name Test in parent class and here
		System.out.println("This is the first row - stat data read from neo4j stat sheet : "+statData[0])
		System.out.println("This is the value of Files Count from Neo4j result "+statData.get(0).get(0).getStringCellValue())
		System.out.println("This is the value of Cases Count from Neo4j result "+statData.get(0).get(1).getStringCellValue())
		System.out.println("This is the value of Trials Count from Neo4j result"+statData.get(0).get(2).getStringCellValue())
		(statData.get(0).get(0).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Datasets)) ? KeywordUtil.markPassed("Statbar Files count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Files count")
		(statData.get(0).get(1).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_ClinTrials)) ? KeywordUtil.markPassed("Statbar Cases count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Cases count")
		(statData.get(0).get(2).getStringCellValue().contentEquals(GlobalVariable.G_StatBar_Grants)) ? KeywordUtil.markPassed("Statbar Trials count matches"): KeywordUtil.markFailed("Mismatch in Stat Bar Trials count")
	}


	/**
	 * This function is used to click on any element
	 * @param TabName
	 * @return
	 */
	@Keyword
	public static void clickTab(String tabName) {
		WebDriver driver = DriverFactory.getWebDriver()
		WebDriverWait wait = new WebDriverWait(driver, 10)
		JavascriptExecutor js = (JavascriptExecutor) driver

		String tabXpath = givexpath(tabName)
		println("Resolved XPath: " + tabXpath)

		for (int i = 0; i < 3; i++) {
			try {
				WebElement resultTab = wait.until(
						ExpectedConditions.presenceOfElementLocated(By.xpath(tabXpath))
						)
				js.executeScript("arguments[0].scrollIntoView(true);", resultTab)
				js.executeScript("arguments[0].click();", resultTab)
				println("Successfully clicked element")
				break
			} catch (StaleElementReferenceException e) {
				println("️StaleElementReferenceException. Retrying... Attempt: " + (i + 1))
				Thread.sleep(1000)
			} catch (Exception e) {
				println("Unexpected error: " + e.getMessage())
				throw e
			}
		}
	}


	/**
	 * This function clicks on Canine tabs
	 * @param TbName
	 * @return
	 */
	@Keyword
	public static clickTabCanineStat(String TbName){

		JavascriptExecutor js = (JavascriptExecutor)driver;
		String rawTabName = TbName
		String tabxpath = givexpath(TbName)
		System.out.println("This is the value of xpath of the element: "+tabxpath);
		WebElement resultTab = driver.findElement(By.xpath(tabxpath));
		js.executeScript("arguments[0].scrollIntoView(true);", resultTab);
		js.executeScript("arguments[0].click();", resultTab);
		System.out.println("Successfully clicked desired element")

		String xcCases = givexpath('Object Repository/Canine/StatBar/Canine_StatBar-Cases')
		GlobalVariable.G_StatBar_Cases = driver.findElement(By.xpath(xcCases)).getAttribute("innerText");
		System.out.println("This is the value of Cases count from Stat bar :"+GlobalVariable.G_StatBar_Cases)
		Thread.sleep(1000)
	}


	@Keyword
	public static clickTabINSStat(String TbName){

		JavascriptExecutor js = (JavascriptExecutor)driver;
		String rawTabName = TbName
		String tabxpath = givexpath(TbName)
		System.out.println("This is the value of xpath of the element: "+tabxpath);
		WebElement resultTab = driver.findElement(By.xpath(tabxpath));
		js.executeScript("arguments[0].scrollIntoView(true);", resultTab);
		js.executeScript("arguments[0].click();", resultTab);
		System.out.println("Successfully clicked desired element")
		Thread.sleep(1000)
	}


	@Keyword
	public static clickTabCDSStat(String TbName){
		//Handle new dropdown behavior for large number of options-- search for the filter before clicking
		Utils.findFilterBySearch(TbName)
		TestObject to = ObjectRepository.findTestObject(TbName)
		String tabxpath = Utils.getEffectiveXPath(to, TbName)

		//Some TestObjects for above scenario had to be changed from Attributes to XPath, so need to fallback to original givexpath() for non-affected objects
		if (tabxpath == null || tabxpath.trim().isEmpty()) {
			println("getEffectiveXPath failed, falling back to TestObject's built-in XPath")
			try {
				tabxpath = givexpath(TbName)
			} catch (Exception e) {
				KeywordUtil.markFailedAndStop("Could not resolve xpath from TestObject: " + TbName)
			}
		}
		if (tabxpath == null || tabxpath.trim().isEmpty()) {
			KeywordUtil.markFailedAndStop("Final resolved XPath is empty for: " + TbName)
		}

		//String tabxpath = givexpath(TbName)
		System.out.println("This is the value of xpath of the element: "+tabxpath);
		JavascriptExecutor js = (JavascriptExecutor)driver;
		WebElement resultTab = driver.findElement(By.xpath(tabxpath));
		js.executeScript("arguments[0].scrollIntoView(true);", resultTab);
		js.executeScript("arguments[0].click();", resultTab);
		System.out.println("Successfully clicked desired element")
		Thread.sleep(3000)
	}


	/**
	 * This method scrolls to a particular element
	 * @param elem
	 */
	@Keyword
	public static void scrolltoViewjs(WebElement elem){
		JavascriptExecutor js = (JavascriptExecutor)driver;
		js.executeScript("arguments[0].scrollIntoView(true);", elem);
	}

	/**
	 * This function clicks on the element using javascript
	 * @param el
	 */
	@Keyword
	public static void clickElement(WebElement el){
		JavascriptExecutor js = (JavascriptExecutor)driver;
		js.executeScript("arguments[0].click();", el);
	}


	/**
	 * This function is used for Bento login
	 */
	@Keyword
	public void Login(String signinButton, String emailID, String emailNxtBtn, String Passwd, String PasswdNxtBtn){
		String xsigninButton = givexpath(signinButton)
		String xemailNxtBtn = givexpath(emailNxtBtn)
		String xPasswd = givexpath(Passwd)
		String xPasswdNxtBtn = givexpath(PasswdNxtBtn)

		JavascriptExecutor js = (JavascriptExecutor)driver;

		Set<String> allHandlesb4signin = driver.getWindowHandles();
		System.out.println("Count of windows BEFORE sign in with google :"+allHandlesb4signin.size());
		System.out.println(allHandlesb4signin);
		String currentWindowHandleB4 = allHandlesb4signin.iterator().next();
		System.out.println("currentWindow Handle - default handle before signin : "+currentWindowHandleB4);

		//removed the if loop


		System.out.println("Waiting to log in")

		js.executeScript("arguments[0].click();", driver.findElement(By.xpath(xsigninButton)));
		Set<String> allHandlesAftersignin = driver.getWindowHandles();
		System.out.println("Count of windows AFTER sign in with google :"+allHandlesAftersignin.size());
		System.out.println(allHandlesAftersignin);
		String parent=driver.getWindowHandle();
		for(String curWindow : allHandlesAftersignin){
			System.out.println ("This is the id of the curr window :"+curWindow)
			driver.switchTo().window(curWindow);   //switching to the child window
		}
		String currentWindowHandleAFTER = allHandlesAftersignin.iterator().next();
		System.out.println("currentWindow Handle -default after signin : "+currentWindowHandleAFTER);
		//store parent window & child window

		//switch to child window
		WebUI.switchToWindowIndex(1)
		String FirstWndUrl = driver.getCurrentUrl();
		System.out.println("First Popup window's url: " + FirstWndUrl)
		driver.manage().window().maximize();


		//Entering the email id or username
		Thread.sleep(2000)
		driver.findElement(By.xpath(xemailID)).sendKeys(GlobalVariable.G_AppUserName);
		System.out.println("Reading the text typed in email field: "+driver.findElement(By.xpath(xemailID)).getAttribute("value"));
		Thread.sleep(2000)


		//Clicking the next button after email id
		js.executeScript("arguments[0].click();", driver.findElement(By.xpath(xemailNxtBtn)));
		Thread.sleep(10000);

		//Entering the password
		driver.findElement(By.xpath(xPasswd)).sendKeys(GlobalVariable.G_AppPassword);
		System.out.println("Getting password: "+driver.findElement(By.xpath(xPasswd)).getAttribute("value"));
		Thread.sleep(2000)

		//Clicking the next button after password
		js.executeScript("arguments[0].click();", driver.findElement(By.xpath(xPasswdNxtBtn)));
		Thread.sleep(3000)

		System.out.println("Typed password and clicked the next button in password window. Moving back to the parent window handle")
		driver.switchTo().window(parent);
		driver.manage().window().maximize();
		Thread.sleep(3000)

		System.out.println("This is the current url post login & moving back to parent window handle : "+ driver.getCurrentUrl() );

		Set<String> allHandlesAfterLogin = driver.getWindowHandles();
		System.out.println(allHandlesAfterLogin);
		System.out.println("Count of windows after successful login :"+allHandlesAfterLogin.size());
		String curWindowHandlePostLogin = allHandlesAfterLogin.iterator().next();
		System.out.println("currentWindow Handle -default after successful login : "+curWindowHandlePostLogin);

		System.out.println("After successful login, the landing page's url: " + driver.getCurrentUrl())
	}//login function ends here


	/** vleung
	 * This function verifies the static data text
	 * @param page - Name of the page where the static data is -- this should match the sub-folder where the Test Object is located
	 * 				 in the Object Repository and also the name of the sheet in the Excel
	 */
	@Keyword
	public static void verifyStaticData(String page) throws IOException {

		if(page == null || page.isEmpty()) {
			KeywordUtil.markFailed("Invalid page: Check verifyStaticText() method")
		}

		// Get the file path for Excel data
		String usrDir = System.getProperty("user.dir");
		String inputFiles = "InputFiles";
		Path filePath;

		if(appKey.equals("Bento")) {
			filePath = Utils.getQueryFilePath("Bento_StaticData.xlsx");
		}else if(appKey.equals("ICDC")) {
			filePath = Utils.getQueryFilePath("ICDC_StaticData.xlsx");
		}else if(appKey.equals("CCDI")) {
			filePath = Utils.getQueryFilePath("CCDI_StaticData.xlsx");
		}else if(appKey.equals("C3DC")) {
			filePath = Utils.getQueryFilePath("C3DC_StaticData.xlsx");
		}else if(appKey.equals("INS")) {
			filePath = Utils.getQueryFilePath("INS_StaticData.xlsx");
		}else if(appKey.equals("CDS")) {
			filePath = Utils.getQueryFilePath("CDS_StaticData.xlsx");
		}else if(appKey.equals("CTDC")) {
			filePath = Utils.getQueryFilePath("CTDC_StaticData.xlsx");
		}else if(appKey.equals("CCDC")) {
			filePath = Utils.getQueryFilePath("CCDC_StaticData.xlsx");
		}else {
			KeywordUtil.markFailed("Invalid App Key: Check Profile or verifyStaticText() function")
		}

		if (filePath !=null) {
			KeywordUtil.markPassed("This is the full file path: " + filePath.toString())
		}else{
			KeywordUtil.markFailed("File is not found")
		}

		// Get the data from Excel
		List<List<String>> excelData = ReadExcel.readOutputExcel(filePath.toString(), page)

		if (excelData == null || excelData.isEmpty()) {
			KeywordUtil.markFailed("Excel is empty")
		} else {

			// Default values
			boolean overallResult = true
			List<List<String>> outputData = new ArrayList<>()

			for (List<String> row : excelData) {
				if (row.size() < 3) {
					row.add("") // Account for TestObjects that do not have a URL to verify
				}
				String testObjectId = row[0]
				String expectedText = row[1]
				String expectedUrl= row[2]

				// Text and link verification default values
				boolean verifyTextPass = false
				boolean verifyUrlPass = false

				// Verify text data matches with element
				try {
					TestObject testObject;

					if (appKey=="ICDC") {
						testObject = findTestObject("Canine" + "/" + "Static" + "/" + page + "/" + testObjectId) // for ICDC only
					} else if (appKey=="CTDC") {
						testObject = findTestObject("Trials" + "/" + "Static" + "/" + page + "/" + testObjectId) // for CTDC only
					} else {
						testObject = findTestObject(appKey + "/" + "Static" + "/" + page + "/" + testObjectId) // path of Test Object in Object Repository
					}

					// Make sure TestObject has correct name in the sheet
					if (testObject == null) {
						System.err.println("TestObject does not exist in Object Repository: " + testObjectId)
						row.add("false")
						outputData.add(row)
						overallResult = false
						continue
					}

					// Make sure TestObject has the correct xpath in Katalon
					WebUI.waitForElementVisible(testObject, 10)
					boolean elementExists = WebUI.waitForElementPresent(testObject, 10)
					if (!elementExists) {
						System.err.println("TestObject has wrong xpath: " + testObjectId)
						row.add("false")
						outputData.add(row)
						overallResult = false
						continue
					}

					String actualText = WebUI.getText(testObject).replaceAll("\\r?\\n", " ").trim()
					expectedText = expectedText.replaceAll("\\r?\\n", " ").trim()
					System.out.println("Verifying text: " + testObjectId + " - Expected: " + expectedText + ", Actual: " + actualText);
					if (actualText.equals(expectedText)) {
						verifyTextPass = true;
					} else {
						System.err.println("Text verification failed: " + testObjectId)
						overallResult = false;
					}

					// If element is a link, also verify URL
					if (testObjectId.contains("Link")) {
						String env;
						if (GlobalVariable.G_Urlname.toString().contains("qa")) {
							env = "qa";
						} else if (GlobalVariable.G_Urlname.toString().contains("stage")) {
							env = "stage";
						} else {
							env = ""; //prod
						}
						String baseExpectedUrl;
						if (env.isEmpty()) {
							baseExpectedUrl = expectedUrl.replace("-qa", ""); //prod
						} else {
							baseExpectedUrl = expectedUrl.replace("-qa", "-" + env); //qa or stage
						}
						String actualUrl = WebUI.getAttribute(testObject, 'href').replaceAll("%20", " ")
						System.out.println("Verifying URL: " + testObjectId + " - Expected: " + baseExpectedUrl + ", Actual: " + actualUrl);
						if (actualUrl.equals(baseExpectedUrl.replaceAll("%20", " "))) {
							verifyUrlPass = true;
							row.add((verifyTextPass && verifyUrlPass).toString())
						} else {
							System.err.println("URL verification failed: " + testObjectId)
							row.add(verifyUrlPass.toString())
							overallResult = false;
						}
					} else {
						row.add(verifyTextPass.toString())
					}

					outputData.add(row)
				} catch (Exception e) {
					WebUI.comment("Error verifying " + testObjectId + ": " + e.message)
					overallResult = false;
				}
			}
			String outputFilePath = Paths.get(usrDir, "OutputFiles").resolve("Output_" + filePath.getFileName().toString())
			ReadExcel.writeOutputExcelStaticData(outputFilePath, page, outputData, 3)
			if (overallResult == false) {
				KeywordUtil.markFailed("There is a FAILURE -- verify in output Excel: "+ outputFilePath)
			}
		}
	}

	/** vleung
	 * This function clears the text field with backspace based on operating system
	 * @param textfield - Text field to be cleared
	 */
	@Keyword
	public static clearTextfieldByOs(String textfield) {
		// Get OS name to detect platform
		String os = System.getProperty("os.name").toLowerCase()
		if (os.contains("mac")) {
			WebUI.sendKeys(findTestObject(textfield), Keys.chord(Keys.COMMAND, "a", Keys.BACK_SPACE))
		} else {
			WebUI.sendKeys(findTestObject(textfield), Keys.chord(Keys.CONTROL, "a", Keys.BACK_SPACE))
		}
	}


	/**
	 * This function verifies INS data set page
	 * Compares UI with TSV datasets
	 */
	@Keyword
	public static void verifyUiAgainstTsvForInsDatasetsTab() {

		PythonReader.readFile('DataSets.py')

		List<List<String>> uiDataRows = new ArrayList<>();
		driver = DriverFactory.getWebDriver();
		Thread.sleep(1000);
		WebUI.click(findTestObject('INS/DataSets/RowsPerPage-Dd'))
		WebUI.click(findTestObject('INS/DataSets/RowPerPaDd-Value'))
		Thread.sleep(1500);
		uiDataRows.add(Arrays.asList("Title", "Source ID", "Primary Disease", "Sample Count"));

		while (true) {
			List<WebElement> cards = driver.findElements(By.xpath("//*[@class='container']"));

			for (WebElement card : cards) {
				String title = safeText(card, ".//a[starts-with(@href, '#/dataset/')]");
				String sourceId = safeText(card, ".//a[starts-with(@href, 'https://cedcd') or starts-with(@href, 'https://www.ncbi.nlm')]");
				String primaryDisease = safeText(card, ".//*[@class='itemSpan']");
				//String participantCount = safeText(card, ".//*[@class='textSpan caseCountHighlight']");
				String sampleCount = safeText(card, ".//*[@class='textSpan sampleCountHighlight']");
				//String description = safeText(card, ".//*[@class='textSpan']");

				System.out.println("Title is: "+ title + "\nSource ID is: "+ sourceId + "\nPrimary Disease: "+ primaryDisease);
				System.out.println("Sample Count: "+ sampleCount);
				uiDataRows.add(Arrays.asList(title, sourceId, primaryDisease, sampleCount));
			}

			// Try clicking "Next"
			try {
				//
				TestObject nextBtn= findTestObject('INS/DataSets/Next-Btn')
				if (WebUI.getAttribute(nextBtn,'class') != null && WebUI.getAttribute(nextBtn,'class').contains("disabled")) {
					break;
				}
				WebUI.waitForElementPresent(nextBtn, 5)
				WebUI.scrollToElement(nextBtn, 5)
				Thread.sleep(2000);
				WebUI.click(nextBtn);
				Thread.sleep(1000); // wait for content to load
			} catch (NoSuchElementException e) {
				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		writeDataToExcel(uiDataRows);
		Utils.compareSheets("WebDatasets","TsvDatasets");
	}


	/**
	 * This is a Utils function that extracts text from UI
	 * @param parent
	 * @param xpath
	 * @return
	 */
	private static String safeText(WebElement parent, String xpath) {
		try {
			return parent.findElement(By.xpath(xpath)).getText();
		} catch (NoSuchElementException e) {
			return "";
		}
	}



	/**
	 * This function write INS datasets page to ouput excel.
	 * @param data
	 */
	private static void writeDataToExcel(List<List<String>> data) {

		Utils.createDirctory("OutputFiles");
		String excelPath = GlobalVariable.G_WebExcel;

		Workbook workbook = new XSSFWorkbook()
		Sheet sheet = workbook.createSheet("WebDatasets")

		for (int i = 0; i < data.size(); i++) {
			Row row = sheet.createRow(i)
			List<String> rowData = data[i]
			for (int j = 0; j < rowData.size(); j++) {
				row.createCell(j).setCellValue(rowData[j])
			}
		}

		FileOutputStream out = null
		try {
			out = new FileOutputStream(excelPath)
			workbook.write(out)
		} catch (Exception e) {
			e.printStackTrace()
		} finally {
			if (out != null) out.close()
			if (workbook != null) workbook.close()
		}
	}

	/**
	 * Determines whether the current CCDI page should enable/sort by GUID.
	 * Returns true when:
	 *   - The Files tab is selected
	 *   - The active filters contain any known GUID-enabled phs accession
	 */
	public static boolean isGuidEligibleStudy() {

		// Read tab state
		String ariaValue = WebUI.getAttribute(
				findTestObject('Object Repository/CCDI/ExplorePage/Files_ResultsTab'),
				"aria-selected"
				)

		// Read active filters
		String filterHeaderText = WebUI.getText(
				findTestObject('Object Repository/CCDI/ExplorePage/Filters/FilterHeader')
				)

		// List of studies where GUID column behavior is required
		List<String> guidStudies = [
			'phs001327',
			'phs001228',
			'phs001714',
			'phs001738',
			'phs001846',
			'phs001878',
			'phs002187',
			'phs002322',
			'phs002276',
			'phs002620',
			'phs00311',
			'phs002883'
		]

		boolean filesTabSelected = "true".equalsIgnoreCase(ariaValue)

		boolean isGuidStudy = guidStudies.any { studyId ->
			filterHeaderText.contains(studyId)
		}

		return filesTabSelected && isGuidStudy
	}
}  //class ends here
