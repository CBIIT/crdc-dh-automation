package utilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.JavascriptExecutor;
import org.apache.poi.xssf.usermodel.XSSFCell
import com.kms.katalon.core.annotation.Keyword;
import com.kms.katalon.core.configuration.RunConfiguration;
import com.kms.katalon.core.util.KeywordUtil;
import internal.GlobalVariable
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.openqa.selenium.Keys as Keys
import com.kms.katalon.core.testdata.TestDataFactory
import com.kms.katalon.core.testobject.ObjectRepository
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import java.util.regex.Pattern
import java.util.regex.Matcher
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.driver.DriverFactory
import org.openqa.selenium.By
import org.openqa.selenium.interactions.Actions
import com.kms.katalon.core.model.FailureHandling

public class Utils {

	// App key used in every profile
	public static String appKey = GlobalVariable.AppKey;
	public static String usrDir = RunConfiguration.getProjectDir();
	public static String RESULT_TAB_NAME;


	/**
	 * This function creates query file path based on the app key
	 */
	@Keyword
	public static Path getQueryFilePath(String input_file) {

		String usrDir = System.getProperty("user.dir");
		String inputFiles = "InputFiles";
		Path filePath;

		if(appKey.equals("Bento")) {
			filePath = Paths.get(usrDir, inputFiles, "Bento", input_file);
		}else if(appKey.equals("ICDC")) {
			filePath = Paths.get(usrDir, inputFiles, "ICDC", input_file);
		}else if(appKey.equals("CCDI")) {
			filePath = Paths.get(usrDir, inputFiles, "CCDI", input_file);
		}else if(appKey.equals("C3DC")) {
			filePath = Paths.get(usrDir, inputFiles, "C3DC", input_file);
		}else if(appKey.equals("INS")) {
			filePath = Paths.get(usrDir, inputFiles, "INS", input_file);
		}else if(appKey.equals("CDS")) {
			filePath = Paths.get(usrDir, inputFiles, "CDS", input_file);
		}else if(appKey.equals("CTDC")) {
			filePath = Paths.get(usrDir, inputFiles, "CTDC", input_file);
		}else if(appKey.equals("MTP")) {
			filePath = Paths.get(usrDir, inputFiles, "MTP", input_file);
		}else if(appKey.equals("CCDC")) {
			filePath = Paths.get(usrDir, inputFiles, "CCDC", input_file);
		}else if(appKey.equals("CRDC")) {
			filePath = Paths.get(usrDir, inputFiles, "CRDC", input_file);
		}else {
			KeywordUtil.markFailed("Invalid App Key: Check Profile or getQueryFilePath() function")
		}

		if (filePath !=null) {
			KeywordUtil.markPassed("This is query file path: "+filePath.toString())
			GlobalVariable.InputExcel=filePath.toString();
		}else{
			KeywordUtil.markFailed("Query file is not found. Check getQueryFilePath function")
		}

		return filePath;
	}



	/**
	 * This function assigns the tsv data files path based on the app key
	 */
	@Keyword
	public static Path getMetadataFilesPath() {

		Path tsvDataFilePath

		if(appKey.equals("CDS")) {
			tsvDataFilePath = Paths.get(usrDir, "InputFiles", "CDS")
		}else if(appKey.equals("ICDC")) {
			tsvDataFilePath = Paths.get(usrDir, "InputFiles", "ICDC")
		}else if(appKey.equals("CCDI")) {
			tsvDataFilePath = Paths.get(usrDir, "InputFiles", "CCDI")
		}else if(appKey.equals("C3DC")) {
			tsvDataFilePath = Paths.get(usrDir, "InputFiles", "C3DC")
		}else if(appKey.equals("INS")) {
			tsvDataFilePath = Paths.get(usrDir, "InputFiles", "INS")
		}else if(appKey.equals("Bento")) {
			tsvDataFilePath = Paths.get(usrDir, "InputFiles", "Bento")
		}else if(appKey.equals("CRDC")) {
			tsvDataFilePath = Paths.get(usrDir, "InputFiles", "CRDC")
		}else if(appKey.equals("CTDC")) {
			tsvDataFilePath = Paths.get(usrDir, "InputFiles", "CTDC")
		}else {
			KeywordUtil.markFailed("Invalid App Key or Node Path: Check getNodeFilesPath function")
		}
		return tsvDataFilePath
	}

	/**
	 * This function assigns Python files path of corresponding app
	 * @param PythonFileName
	 */
	public static Path getPythonFilePath(String pyFileName) {
		String pyPath;
		if(appKey.equals("CDS")) {
			pyPath = Paths.get(usrDir, "PythonFiles", "CDS", pyFileName)
		}else if(appKey.equals("ICDC")) {
			pyPath = Paths.get(usrDir, "PythonFiles", "ICDC", pyFileName)
		}else if(appKey.equals("CCDI")) {
			pyPath = Paths.get(usrDir, "PythonFiles", "CCDI", pyFileName)
		}else if(appKey.equals("C3DC")) {
			pyPath = Paths.get(usrDir, "PythonFiles", "C3DC", pyFileName)
		}else if(appKey.equals("INS")) {
			pyPath = Paths.get(usrDir, "PythonFiles", "INS", pyFileName)
		}else if(appKey.equals("Bento")) {
			pyPath = Paths.get(usrDir, "PythonFiles", "Bento", pyFileName)
		}else if(appKey.equals("Bento")) {
			pyPath = Paths.get(usrDir, "PythonFiles", "CRDC", pyFileName)
		}else if(appKey.equals("CTDC")) {
			pyPath = Paths.get(usrDir, "PythonFiles", "CTDC", pyFileName)
		}else {
			KeywordUtil.markFailed("Invalid App Key: Check getPythonScriptPath function")
		}
	}



	/**
	 * This function returns the Python executable path based on the JENKINS_HOME environment variable.
	 * @return String - Path to the Python executable
	 */
	public static String getPythonExecutablePath() {

		String jenkinsHome = System.getenv("JENKINS_HOME");
		String pyExecutablePath;

		if (jenkinsHome != null) {
			KeywordUtil.logInfo("Detected Jenkins environment. Using system Python executable...");
			pyExecutablePath = "python3";
		} else {
			KeywordUtil.logInfo("Running locally. Using local Python executable path...");
			pyExecutablePath = "/Library/Frameworks/Python.framework/Versions/3.12/bin/python3";
			//pyExecutablePath = "C:/Users/kallakuriv2/AppData/Local/Programs/Python/Python312/python.exe";
			//pyExecutablePath = "/Users/leungvw/vincent_testEnv/bin/python3";
			//pyExecutablePath = "C:/Users/epishinavv/AppData/Local/Programs/Python/Python312/python.exe";
		}

		return pyExecutablePath;
	}


	/**
	 * This function creates directory in the project path
	 * @param dirName
	 */
	public static void createDirctory(String dirName) {

		// Get the project directory path
		String projectDir = System.getProperty("user.dir");
		Path folderPath = Paths.get(projectDir, dirName);

		// Check if the dirName folder exists
		if (!Files.exists(folderPath)) {
			try {
				Files.createDirectory(folderPath);
				System.out.println(dirName + " folder has been created.");
			} catch (Exception e) {
				System.err.println("Failed to create "+dirName+" folder: " + e.getMessage());
			}
		} else {
			System.out.println(dirName +" folder already exists.");
		}
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
	 * This function clears text from a text field
	 * @return String
	 */
	public static String clearText() {
		return Keys.chord(Keys.CONTROL, "a") + Keys.DELETE;
	}

	//@@@@@@@@@@@@@@@@ Write web result to excel @@@@@@@@@@@@@@@@
	/**
	 * This function write webData to excel
	 * @param webSheetName
	 */
	public static void writeToExcel(String webSheetName){

		Utils.createDirctory("OutputFiles");

		try {
			String excelPath = GlobalVariable.G_WebExcel;
			File file1 = new File(excelPath);
			FileOutputStream fos = null;
			XSSFWorkbook workbook = null;
			XSSFSheet sheet;

			if( file1.exists()){
				System.out.println( "File exists, creating a new worksheet in the same file.")
				FileInputStream fileinp = new FileInputStream(excelPath);
				workbook = new XSSFWorkbook(fileinp);
				sheet = workbook.createSheet(webSheetName);
				fos = new FileOutputStream(excelPath);
			}
			else{
				fos = new FileOutputStream(new File(excelPath));
				System.out.println("File does not exist, creating a new file.")
				workbook = new XSSFWorkbook();           // Create Workbook instance holding .xls file
				sheet = workbook.createSheet(webSheetName);
			}

			List<String> writeData = new ArrayList<String>();
			writeData = GlobalVariable.G_CaseData
			for( int i = 0; i < writeData.size(); i++ ){
				Row row = sheet.createRow(i);
				int cellNo = 0
				ArrayList<String> cellData = writeData.get(i).split("\\|\\|");
				for( String cellD: cellData ){
					Cell cell = row.createCell(cellNo++);
					cell.setCellValue(cellD);
				}
			}//for loop of in
			workbook.write(fos);  //Write workbook into the excel
			fos.close(); //Close the workbook
			System.out.println("Web Data has been written to excel successfully");
			workbook.close();
		}catch (IOException ie) {
			ie.printStackTrace();
		}
	}//write to excel method ends here



	//@@@@@@@@@@@@@@@ SOHIL's Code @@@@@@@@@@@@@@@@@@@@
	/**
	 * This function compares two sheet. Other functions are called in this function
	 * To perform the entire action
	 * @param webSheetName
	 * @param neoSheetName
	 */
	@Keyword
	public static void compareSheets(String webSheetName, String tsvSheetName) {

		List<List<String>> UIData = new ArrayList<>();
		List<List<String>> tsvData = new ArrayList<>();

		// Initializing files path
		String UIfilename = GlobalVariable.G_WebExcel.toString();
		String tsvFilename = GlobalVariable.G_ResultPath.toString();

		System.out.println("This is the full UI   file path: " + UIfilename);
		System.out.println("This is the full TSV  file path: " + tsvFilename);

		// Read UI output excel
		UIData = ReadExcel.readOutputExcel(UIfilename, webSheetName);
		Collections.sort(UIData, new TestRunner());

		// Read TSV or DB output excel
		tsvData = ReadExcel.readOutputExcel(tsvFilename, tsvSheetName);
		Collections.sort(tsvData, new TestRunner());

		System.out.println("This is the row size of the UIWeb Output data: " + UIData.size());
		System.out.println("This is the row size of the TSV   Output data: " + tsvData.size());

		compareTwoLists(UIData, tsvData);
	}



	/**
	 * This function reads two lists and compares it's content
	 * @param l1 (specified for UI) a List that will have inner list
	 * @param l2 (specified for TSV) a List that will have inner list
	 */
	public static void compareTwoLists(List<List<String>> l1, List<List<String>> l2){

		System.out.println("============== Verification of the data ==============")

		int l2row=0;

		while( l2row < l2.size() ){

			for( int l1row = 0; l1row < l1.size(); l1row++ ){

				List<String> l1rowList = l1.get(l1row)
				List<String> l2rowList = l2.get(l2row)
				boolean l1NullFlag = false, l2NullFlag = false

				int l1rowCount =l1row+2;
				int l2rowCount =l2row+2;

				System.out.println("UI  Data Entire Row:  " + l1rowList)
				System.out.println("TSV Data Entire Row:  " + l2rowList)

				// Check if column counts do not match
				if (l1rowList.size() != l2rowList.size()) {
					System.err.println("*********** COLUMN COUNT MISMATCH ***********");
					System.err.println("UI  Data Row: " + l1rowCount + " has " + l1rowList.size() + " columns.");
					System.err.println("TSV Data Row: " + l2rowCount + " has " + l2rowList.size() + " columns.");
					KeywordUtil.markFailed("*********** COLUMN COUNT MISMATCH *************");
					return;  // Exit the function since the column counts do not match
				}

				for(int col = 0; col < l2rowList.size(); col++ ){

					String l1Col = l1rowList.get(col);
					String l2Col = l2rowList.get(col);

					String l1Value = l1rowList.get(col);
					String l2Value = l2rowList.get(col);

					//Check for empty cell in UI excel
					if(l1Col == null || l1Value.trim().isEmpty()){
						System.out.println("There is an empty cell in UI Data Row: " + l1rowCount + " Col: " + col );
						l1NullFlag = true
					}

					//Check for empty cell in DB excel
					if(l2Col == null || l2Value.trim().isEmpty()){
						System.out.println("There is an empty cellin TSV Data Row: " + l2rowCount + " Col: " + col );
						l2NullFlag = true
					}

					//When UI and DB empty cell don't match, warn user
					if (l1NullFlag != l2NullFlag) {
						System.err.println("********** EMPTY CELL MISMATCH **********")
						l1NullFlag = false
						l2NullFlag = false
					}

					//When there is data, compare UI value against DB value
					//					if(l1Value.equals(l2Value)){
					//						System.out.println("UI  data cell value is:  "+ l1Value + "\nTSV data cell value is:  "+ l2Value );
					//						System.out.println("Content matches for Row: " + l1rowCount + " Col: " + col +" \u2713");
					//					}else{
					//						System.err.println("*********** DATA MISMATCH ***********")
					//						System.err.println("UI  data cell value is:  "+ l1Value + "\nTSV data cell value is:  "+ l2Value );
					//						System.err.println("Content does not match for Row: " + l1rowCount + " Col: " + col +" \u2717")
					//						KeywordUtil.markFailed("*********** DATA MISMATCH in compareTwoLists *************");
					//					}

					// When there is data, compare UI value against DB value (with a fallback normalized compare)
					//					String uiNorm  = normalizeSemicolonList(l1Value);
					//					String tsvNorm = normalizeSemicolonList(l2Value);
					//
					//					if (l1Value.equals(l2Value) || uiNorm.equals(tsvNorm)) {
					//						System.out.println("UI  data cell value is:  " + l1Value + "\nTSV data cell value is:  " + l2Value);
					//						System.out.println("Content matches for Row: " + l1rowCount + " Col: " + col + " \u2713");
					//					} else {
					//						System.err.println("*********** DATA MISMATCH ***********");
					//						System.err.println("UI  data cell value is:  " + l1Value + "\nTSV data cell value is:  " + l2Value);
					//						System.err.println("UI  normalized: " + uiNorm + " | TSV normalized: " + tsvNorm);
					//						System.err.println("Content does not match for Row: " + l1rowCount + " Col: " + col + " \u2717");
					//						KeywordUtil.markFailed("*********** DATA MISMATCH in compareTwoLists *************");

					String l1ValueClean = stripCpiBadge(l1Value)
					String uiNorm  = normalizeSemicolonList(l1ValueClean)
					String tsvNorm = normalizeSemicolonList(l2Value)

					// And tweak the equality check to allow exact match after cleaning:
					if (l1Value.equals(l2Value) || l1ValueClean.equals(l2Value) || uiNorm.equals(tsvNorm)) {
						System.out.println("UI  data cell value is:  " + l1Value + "\nTSV data cell value is:  " + l2Value);
						System.out.println("Content matches for Row: " + l1rowCount + " Col: " + col + " \u2713");
					} else {
						System.err.println("*********** DATA MISMATCH ***********");
						System.err.println("UI  data cell value is:  " + l1Value + "\nTSV data cell value is:  " + l2Value);
						System.err.println("UI  normalized: " + uiNorm + " | TSV normalized: " + tsvNorm);
						System.err.println("Content does not match for Row: " + l1rowCount + " Col: " + col + " \u2717");
						KeywordUtil.markFailed("*********** DATA MISMATCH in compareTwoLists *************");
					}
				}
				l2row++
			}
		}
	}

	/**
	 * Extracts the first match group from a string using a given regex pattern.
	 * @param input - the string to search
	 * @param regex - the regex pattern with at least one capturing group
	 * @return the first group match or null if not found
	 */
	public static String extractFirstMatch(String input, String regex) {
		Pattern pattern = Pattern.compile(regex)
		Matcher matcher = pattern.matcher(input)
		if (matcher.find()) {
			return matcher.group(1)
		}
		return null
	}

	/**
	 * Get the xpath stored in the TestObject, handle for both Xpath and Attributes
	 * @param label - the path of the TestObject
	 */
	public static String getEffectiveXPath(TestObject to, String label) {
		//Get all defined selector types (e.g., XPATH, BASIC, CSS)
		def selectorMap = to.getSelectorCollection()
		if (selectorMap == null || selectorMap.isEmpty()) {
			KeywordUtil.markFailedAndStop("Selector map is missing or empty for: " + label)
		}
		//Look for either 'XPATH' or 'BASIC' entry in the selector map, where the value is a TestObjectProperty
		def xpathEntry = selectorMap.find { key, val ->
			key in ['XPATH', 'BASIC'] && val instanceof com.kms.katalon.core.testobject.TestObjectProperty
		}
		if (xpathEntry != null) {
			return xpathEntry.value.getValue()
		}
		//Handle older or manually constructed test objects where the selector is stored as a plain string
		def fallbackEntry = selectorMap.find { key, val -> val instanceof String }
		if (fallbackEntry != null) {
			println "Using fallback string selector for ${label}: ${fallbackEntry.key} => ${fallbackEntry.value}"
			return fallbackEntry.value
		}
		KeywordUtil.markFailedAndStop("No usable selector found for: " + label)
		return null
	}

	/**
	 * This function finds the filter using the search bar in the facet (available for dropdowns with a large number of filter options)
	 * @param filter (name of the filter)
	 */
	public static findFilterBySearch(String filter){
		if(appKey.equals("CDS")) {
			//PHS Accession dropdown
			if (filter.contains("/StudyFacet/PHS_Accession/") && !filter.contains("Ddn")) {
				String accession = extractFirstMatch(filter, "(phs\\d{6})")
				if (accession != null) {
					System.out.println("Searching for PHS Accession: " + accession)
					TestRunner.clickTab('Object Repository/CDS/Data_page/Filter/StudyFacet/PHS_Accession/phsAccession_Search')
					WebUI.setText(findTestObject('Object Repository/CDS/Data_page/Filter/StudyFacet/PHS_Accession/phsAccession_Search'), accession)
				} else {
					KeywordUtil.markFailedAndStop("Not able to get PHS Accession-- stopping test")
				}
			}
			//File Type dropdown
			else if (filter.contains("/FilesFacet/FileType/") && !filter.contains("Ddn")){
				TestObject to = ObjectRepository.findTestObject(filter)
				String xpath = getEffectiveXPath(to, filter)
				String fileType = extractFirstMatch(xpath, 'checkbox_File Type_([^"]+)')
				if (fileType != null) {
					System.out.println("Searching for File Type: " + fileType)
					TestRunner.clickTab('Object Repository/CDS/Data_page/Filter/FilesFacet/FileType/fileType_Search')
					WebUI.setText(findTestObject('Object Repository/CDS/Data_page/Filter/FilesFacet/FileType/fileType_Search'), fileType)
				} else {
					KeywordUtil.markFailedAndStop("Not able to get File Type-- stopping test")
				}
			}
			//Primary Diagnosis dropdown
			else if (filter.contains("/DiagnosisFacet/PrimaryDiagnosis/") && !filter.contains("Ddn")) {
				TestObject to = ObjectRepository.findTestObject(filter)
				String xpath = getEffectiveXPath(to, filter)
				String primaryDiagnosis = extractFirstMatch(xpath, '_([^"_]+)"\\]$')
				if (primaryDiagnosis != null) {
					System.out.println("Searching for Primary Diagnosis: " + primaryDiagnosis)
					TestRunner.clickTab('Object Repository/CDS/Data_page/Filter/DiagnosisFacet/PrimaryDiagnosis/primaryDiagnosis_Search')
					WebUI.setText(findTestObject('Object Repository/CDS/Data_page/Filter/DiagnosisFacet/PrimaryDiagnosis/primaryDiagnosis_Search'), primaryDiagnosis)
				} else {
					KeywordUtil.markFailedAndStop("Not able to get Primary Diagnosis-- stopping test")
				}
			}
		}
	}

	/**
	 * This function gets list of WebElements based on locator
	 */
	@Keyword
	public static List<WebElement> getListWebElements(String locator) {
		String xpath = TestRunner.givexpath(locator)
		WebDriver driver = DriverFactory.getWebDriver();
		return driver.findElements(By.xpath(xpath));
	}

	/**
	 * This function gets list of data from Katalon Data File
	 */
	@Keyword
	public static List<String> getListFromDataFile(String dataFile) {
		System.out.println("Getting the list from the Data File: " + dataFile)
		def data = TestDataFactory.findTestData(dataFile)
		List<String> allowedValues = []
		for (int i = 1; i <= data.getRowNumbers(); i++) {
			allowedValues.add(data.getValue(1, i))
		}
		return allowedValues;
	}

	/**
	 * This function waits for the element to disappear (ExpectedConditions was not working)
	 */
	@Keyword
	public static void waitForElementToDisappear(String locatorKey, int timeoutSeconds) {
		String xpath = TestRunner.givexpath(locatorKey);
		WebDriver driver = DriverFactory.getWebDriver();
		JavascriptExecutor js = (JavascriptExecutor) driver;
		long start = System.currentTimeMillis();

		KeywordUtil.logInfo("Polling for disappearance of: " + xpath);

		while ((System.currentTimeMillis() - start) / 1000 < timeoutSeconds) {
			String script = "return document.evaluate(\"" + xpath + "\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue != null;";
			boolean isPresent = (Boolean) js.executeScript(script);

			long elapsed = (System.currentTimeMillis() - start) / 1000;
			KeywordUtil.logInfo("Loop at " + elapsed + "s. Found: " + isPresent);

			if (!isPresent) {
				KeywordUtil.logInfo("Element disappeared after " + elapsed + " seconds.");
				return;
			}
			Thread.sleep(2000);
		}

		throw new AssertionError("Element did not disappear within " + timeoutSeconds + " seconds: " + xpath);
	}



	/**
	 * This function changes the pagination dropdown to 100 results per page
	 */
	@Keyword
	public static void changePaginationResultsPerPage100() {

		def driver = DriverFactory.getWebDriver()
		def js = (JavascriptExecutor) driver
		def actions = new Actions(driver)

		def dropdownTrigger = WebUI.findWebElement(findTestObject('CCDI/ExplorePage/CCDI_ResultsPerPage_Ddn'), 10)

		try {
			actions.moveToElement(dropdownTrigger).click().perform()
			WebUI.delay(0.5)
		} catch (Exception e) {
			WebUI.comment("Normal click failed: ${e.message}")
		}

		def choice = WebUI.findWebElement(findTestObject('CCDI/ExplorePage/CCDI_ResultsPerPage_100'), 10)
		try {
			WebUI.click(findTestObject('CCDI/ExplorePage/CCDI_ResultsPerPage_100'))
		} catch (Exception e) {
			WebUI.comment("WebUI click failed, retrying with JS: ${e.message}")
			js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", choice)
		}
	}

	/**
	 * Helper to normalize order for TSV/UI comparison
	 */
	private static String normalizeSemicolonList(String s) {
		if (s == null) return "";
		String trimmed = s.trim();
		if (trimmed.isEmpty()) return "";

		// If it's a multi-value cell, normalize order and spacing
		if (trimmed.indexOf(';') >= 0) {
			String[] parts = trimmed.split(";");
			List<String> items = new ArrayList<String>()
			for (String p : parts) {
				if (p != null) {
					String t = p.trim().replaceAll("\\s+", " "); // collapse internal spaces
					if (!t.isEmpty()) items.add(t);
				}
			}
			java.util.Collections.sort(items, String.CASE_INSENSITIVE_ORDER);
			return String.join(";", items); // join with ';' (no spaces)
		}
		// Single value: just trim & collapse inner spaces
		return trimmed.replaceAll("\\s+", " ");
	}

	/**
	 * Helper to strip the number badge for CPI mappings from the Participant ID column
	 */
	private static String stripCpiBadge(String s) {
		if (s == null) return null

		String out = s

		// Normalize line endings, collapse NBSP, trim
		out = out.replace('\r', '\n').replace('\u00A0', ' ').trim()

		// If UI renders the CPI badge on the next line (common case), keep only the first line
		int nl = out.indexOf('\n')
		if (nl >= 0) {
			out = out.substring(0, nl).trim()
		}

		// Now strip ONLY a trailing badge count that is separated by whitespace or brackets.
		// Important: these regexes require whitespace before the number or bracket,
		// so IDs like "...043775" (no space) remain intact.

		// e.g., " (2)" at end
		out = out.replaceAll('(?<=\\s)\\(\\d{1,3}\\)$', '')
		// e.g., " [2]" at end
		out = out.replaceAll('(?<=\\s)\\[\\d{1,3}\\]$', '')
		// e.g., " 2" at end
		out = out.replaceAll('(?<=\\s)\\d{1,3}$', '')

		// Clean up any trailing whitespace left by the removals
		return out.trim()
	}
}
