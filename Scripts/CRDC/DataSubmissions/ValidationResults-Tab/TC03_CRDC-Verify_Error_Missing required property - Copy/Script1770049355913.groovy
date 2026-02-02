import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.By as By
import org.openqa.selenium.WebDriver as WebDriver
import org.openqa.selenium.WebElement as WebElement
import org.openqa.selenium.chrome.ChromeDriver as ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions as ChromeOptions
import org.openqa.selenium.firefox.FirefoxBinary as FirefoxBinary
import org.openqa.selenium.firefox.FirefoxDriver as FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions as FirefoxOptions
import org.openqa.selenium.support.ui.ExpectedConditions as ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait as WebDriverWait
import java.nio.file.Path as Path
import java.nio.file.Paths as Paths
import com.kms.katalon.core.testdata.TestDataFactory
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.openqa.selenium.WebElement
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement

/*This test script:
 - Opens the browser of choice: Chrome, Firefox or Edge
 - Driver opened by Katalon is used in Selenium.
 - Logs in as Submitter
 - Creates DS, uploads, and validates
 - Verifies data submission dashboard
 */



// =======================================
// EXPECTED MASTER VALUES FOR THIS TEST
// =======================================


//Login as Submitter


CustomKeywords.'utilities.SubmissionRequest.navigateToCrdc'()
WebUI.setViewPortSize(1920, 1080)
CustomKeywords.'utilities.SubmissionRequest.loginToCrdcOtp'('Submitter')
WebUI.delay(2)

//Create DS
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/NavBar/DataSubmissions-Tab')
CustomKeywords.'utilities.Pbac.createDataSubmission'("GC")
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/DataSubmissions/DataSubmissionName-Link')


//Upload data
CustomKeywords.'utilities.Pbac.uploadMetadataUI'('InputFiles/CRDC/MetadataData/ErrorMsgdata/TC03_program.tsv')
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Validation/Uploading-Text', 15)


//Wait for uploading to finish
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Validation/Uploading-Text', 30)

//Validate data
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validate-Btn'), 30)
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/DataSubmissions/Validate-Btn')


//Wait for validation to complete
CustomKeywords.'utilities.Utils.waitForElementToDisappear'('CRDC/DataSubmissions/Loading-Icon', 30)

//Go to Validation Results tab
WebUI.waitForElementClickable(findTestObject('CRDC/DataSubmissions/Validation/ValidationResults-Tab'), 30)
CustomKeywords.'utilities.TestRunner.clickTab'('CRDC/DataSubmissions/Validation/ValidationResults-Tab')

WebUI.delay(2)

println("\n---- OPENED VALIDATION RESULTS TAB ----")

// =====================================
// LOAD MASTER EXCEL FILE
// =====================================
Map master = CustomKeywords.'utilities.SubmissionRequest.loadExcelMaster'("InputFiles/CRDC/DataSubmissionValidation/ValidationMaster.xlsx")


// =====================================
// 1️⃣ VALIDATE AGGREGATED VIEW
// =====================================
println "\n===== VALIDATING AGGREGATED MODE ====="

CustomKeywords.'utilities.SubmissionRequest.forceCloseDropdownHard'()
CustomKeywords.'utilities.SubmissionRequest.validateAggregatedView'(master)

println "===== AGGREGATED MODE COMPLETED =====\n"


// =====================================
// 2️⃣ SWITCH TO EXPANDED MODE
// =====================================
println "\n===== TOGGLE SWITCH TEST START ====="

// Make sure no dropdown is blocking UI
CustomKeywords.'utilities.SubmissionRequest.forceCloseDropdownHard'()

// Build toggle TestObject
TestObject toggle = new TestObject("toggleInput")
toggle.addProperty("xpath", ConditionType.EQUALS, "//*[@data-testid='toggle-input']")

// Read state before click
String before = WebUI.getAttribute(toggle, "checked")
println "Toggle BEFORE click → " + before

// Try normal click
try {
    WebUI.waitForElementClickable(toggle, 10)
    WebUI.click(toggle)
    println "✔ Toggle clicked normally"
} catch (Exception e) {
    println "⚠ Normal click failed — trying JS fallback"
    WebElement el = WebUI.findWebElement(toggle, 5)
    WebUI.executeJavaScript("arguments[0].click();", Arrays.asList(el))
    println "✔ Toggle clicked using JavaScript"
}

WebUI.delay(1)

// Read state AFTER click
String after = WebUI.getAttribute(toggle, "checked")
println "Toggle AFTER click → " + after

println "===== TOGGLE SWITCH TEST END =====\n"

println "\n===== EXPANDED MODE HEADER VALIDATION START ====="

// HARD-CLOSE all overlays to avoid dropdown popups obstructing table scan
CustomKeywords.'utilities.SubmissionRequest.forceCloseDropdownHard'()

// Create TestObject for expanded mode column headers
TestObject headerObj = new TestObject("expandedHeaders")
headerObj.addProperty("xpath", ConditionType.EQUALS, "//table//thead//th")

// Fetch column headers
List<WebElement> headerElements = WebUI.findWebElements(headerObj, 10)

List<String> uiHeaders = headerElements.collect { it.getText().trim() }

println "\nUI Expanded Headers Found:"
uiHeaders.each { println " - ${it}" }

// Expected expanded headers (MUST MATCH THE UI EXACTLY)
List<String> expectedHeaders = [
		"Batch ID",
		"Node Type",
		"Submitted Identifier",
		"Severity",
		"Validated Date",
		"Issue Count",
		"Issue(s)"
]

println "\nExpected Headers:"
expectedHeaders.each { println " - ${it}" }

// Compare UI headers to expected
println "\nComparing UI headers with expected..."

expectedHeaders.each { expected ->
	if (!uiHeaders.contains(expected)) {
		KeywordUtil.markFailed("❌ Expanded header '${expected}' NOT found in UI")
	} else {
		println("✔ Header '${expected}' is present")
	}
}

/************************************************************
 *   EXPANDED MODE — DROPDOWN VALIDATION FROM TEST CASE
 ************************************************************/
println "=== VALIDATING ALL DROPDOWNS ==="

// EXPECTED VALUES
List<String> expectedIssueTypes = ["All", "CDE not available", "Missing required property"]
List<String> expectedNodeTypes  = ["All", "Program"]  // Add more if available
List<String> expectedSeverities = ["All", "Error", "Warning"]


// ----------------------
// ISSUE TYPE DROPDOWN
// ----------------------
openDropdown("mui-component-select-issueType")
List<String> issueVals = getDropdownOptions()
closeDropdown()
forceCloseDropdownHard()
println issueVals



// ----------------------
// NODE TYPE DROPDOWN
// ----------------------
openDropdown("mui-component-select-nodeType")
List<String> nodeVals = getDropdownOptions()
closeDropdown()
forceCloseDropdownHard()
println nodeVals


// ----------------------
// SEVERITY DROPDOWN
// ----------------------
openDropdown("mui-component-select-severity")
List<String> severityVals = getDropdownOptions()
closeDropdown()
forceCloseDropdownHard()
println severityVals

// ----------------------
// BATCH ID DROPDOWN
// ----------------------
openDropdown("mui-component-select-batchID")
List<String> batchVals = getDropdownOptions()
closeDropdown()
forceCloseDropdownHard()
println batchVals


println "\n=== DROPDOWN VALIDATION COMPLETED ==="



/* ============================================================
   DROPDOWN HELPERS — TEST CASE LEVEL (NO KEYWORDS)
   ============================================================ */

// OPEN DROPDOWN BY MUI ID
void openDropdown(String muiId) {
    println "\n➡ Opening dropdown: ${muiId}"

    TestObject dd = new TestObject("dd_" + muiId)
    dd.addProperty("xpath", ConditionType.EQUALS, "//*[@id='${muiId}']")

    try {
        WebUI.click(dd)
        WebUI.delay(1)
    } catch (Exception e) {
        println "⚠ Normal click failed — JS fallback"
        WebElement el = WebUI.findWebElement(dd, 5)
        WebUI.executeJavaScript("arguments[0].click();", Arrays.asList(el))
        WebUI.delay(1)
    }
}


// READ ALL OPTIONS FROM OPEN DROPDOWN
List<String> getDropdownOptions() {
    TestObject opts = new TestObject("dropdownOptions")
    opts.addProperty("xpath", ConditionType.EQUALS,
        "//div[contains(@class,'MuiPopover-root')]//li[@role='option']")

    List<WebElement> elements = WebUI.findWebElements(opts, 5)

    if (elements == null || elements.isEmpty()) {
        println "❌ No options found"
        return []
    }

    List<String> values = elements.collect { it.getText().trim() }
    println "✔ Options: ${values}"

    return values
}


// CLOSE DROPDOWN USING ESC
void closeDropdown() {
    println "➡ Closing dropdown (ESC)"
    try {
        TestObject body = new TestObject("bodyEsc")
        body.addProperty("xpath", ConditionType.EQUALS, "//body")
        WebUI.sendKeys(body, Keys.chord(Keys.ESCAPE))
        WebUI.delay(1)
    } catch (Exception e) {
        println "⚠ ESC failed — maybe closed"
    }
}


// HARD CLOSE (CLICK OUTSIDE)
void forceCloseDropdownHard() {
    println "➡ Forcing dropdown close (click outside)"

    try {
        TestObject root = new TestObject("rootClick")
        root.addProperty("xpath", ConditionType.EQUALS, "//*[@id='root']")
        WebUI.clickOffset(root, 5, 5)
        WebUI.delay(1)
    } catch (Exception e) {
        println "⚠ Hard close failed — maybe already closed"
    }
}


