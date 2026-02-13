package utilities

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.testobject.TestObject
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

/**
 * Generic, reusable web UI verification helpers (tooltips, element count, etc.).
 * Results are added to ValidationReport for generateValidationReport().
 */
class WebUIUtils {

	private static final String REPORT_SECTION = "Export Buttons"
	private static final int DEFAULT_TIMEOUT = 10

	/**
	 * Verify tooltip text when it is in the element's aria-label attribute.
	 * Use for tooltips that expose their text via aria-label (e.g. Data View export tooltip).
	 * @param to TestObject for the element with aria-label
	 * @param expectedText Expected tooltip text
	 */
	@Keyword
	static void verifyTooltipByAriaLabel(TestObject to, String expectedText) {
		String actualText = WebUI.getAttribute(to, 'aria-label')
		String status = expectedText.equals(actualText) ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Tooltip (aria-label)", expectedText, actualText != null ? actualText : "(empty)", status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyTooltipByAriaLabel: expected '${expectedText}', actual '${actualText}'")
		}
	}

	/**
	 * Verify tooltip text that appears on hover (e.g. MUI Tooltip).
	 * Hovers over the trigger element, finds the tooltip with role="tooltip", matches by substring, normalizes whitespace, and verifies.
	 * @param triggerTo TestObject for the element to hover (e.g. tooltip span wrapper)
	 * @param expectedText Full expected tooltip text (after whitespace normalization)
	 * @param substringToFind Substring to identify the correct tooltip when multiple exist
	 */
	@Keyword
	static void verifyTooltipOnHover(TestObject triggerTo, String expectedText, String substringToFind) {
		WebDriver driver = DriverFactory.getWebDriver()
		WebElement trigger = WebUI.findWebElement(triggerTo, DEFAULT_TIMEOUT)
		new Actions(driver).moveToElement(trigger).perform()
		WebUI.delay(1)
		List<WebElement> tooltips = driver.findElements(By.cssSelector("[role='tooltip']"))
		WebElement match = tooltips.find { it.getText().contains(substringToFind) }
		if (match == null) {
			ValidationReport.addValidationReportItem(REPORT_SECTION, "Tooltip (hover)", expectedText, "(not found)", "Fail")
			KeywordUtil.markFailed("Tooltip with expected text not found. Found: " + tooltips.collect { it.getText() })
		} else {
			String actualText = match.getText().replaceAll(/\s+/, ' ').trim()
			String status = actualText.equals(expectedText) ? "Pass" : "Fail"
			ValidationReport.addValidationReportItem(REPORT_SECTION, "Tooltip (hover)", expectedText, actualText, status)
			if (status == "Fail") {
				KeywordUtil.markFailed("verifyTooltipOnHover: expected '${expectedText}', actual '${actualText}'")
			}
		}
	}

	/**
	 * Verify that the count of elements matching the TestObject equals the expected value.
	 * @param to TestObject to match
	 * @param expectedCount Expected number of elements
	 */
	@Keyword
	static void verifyElementCount(TestObject to, int expectedCount) {
		int count = WebUI.findWebElements(to, DEFAULT_TIMEOUT).size()
		String status = (count == expectedCount) ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Element count", String.valueOf(expectedCount), String.valueOf(count), status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyElementCount: expected ${expectedCount}, actual ${count}")
		}
	}

	/**
	 * Wait for an element to be visible, then verify it is displayed.
	 * Results are logged to ValidationReport.
	 * @param to TestObject for the element
	 */
	@Keyword
	static void verifyElementDisplayed(TestObject to) {
		boolean visible = WebUI.waitForElementVisible(to, DEFAULT_TIMEOUT)
		String status = visible ? "Pass" : "Fail"
		String actualState = visible ? "displayed" : "not visible"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Element displayed", "displayed", actualState, status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyElementDisplayed: element not visible within ${DEFAULT_TIMEOUT}s")
		}
	}

	/**
	 * Verify that an element has the expected text.
	 * Waits for the element to be visible, gets its text, normalizes whitespace, then compares.
	 * Results are logged to ValidationReport.
	 * @param to TestObject for the element
	 * @param expectedText Expected element text (will be normalized for comparison)
	 */
	@Keyword
	static void verifyElementText(TestObject to, String expectedText) {
		WebElement element = WebUI.findWebElement(to, DEFAULT_TIMEOUT)
		String actualText = element.getText()?.replaceAll(/\s+/, ' ').trim() ?: ""
		String expectedNormalized = expectedText?.replaceAll(/\s+/, ' ').trim() ?: ""
		String status = expectedNormalized.equals(actualText) ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Element text", expectedText, actualText, status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyElementText: expected '${expectedText}', actual '${actualText}'")
		}
	}

	/**
	 * Verify that a button/element is enabled or disabled.
	 * Results are logged to ValidationReport.
	 * @param to TestObject for the button or element
	 * @param expectedEnabled true if element should be enabled, false if it should be disabled
	 */
	@Keyword
	static void verifyButtonEnabled(TestObject to, boolean expectedEnabled) {
		WebElement element = WebUI.findWebElement(to, DEFAULT_TIMEOUT)
		boolean actualEnabled = element.isEnabled()
		String expectedState = expectedEnabled ? "enabled" : "disabled"
		String actualState = actualEnabled ? "enabled" : "disabled"
		String status = (actualEnabled == expectedEnabled) ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Button enabled", expectedState, actualState, status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyButtonEnabled: expected ${expectedState}, actual ${actualState}")
		}
	}

	/**
	 * Verify that a radio button, checkbox, or toggle is selected (checked) or not.
	 * Results are logged to ValidationReport.
	 * @param to TestObject for the radio/checkbox/toggle element
	 * @param expectedSelected true if element should be selected, false if it should not be selected
	 */
	@Keyword
	static void verifyButtonSelected(TestObject to, boolean expectedSelected) {
		WebElement element = WebUI.findWebElement(to, DEFAULT_TIMEOUT)
		boolean actualSelected = element.isSelected()
		String expectedState = expectedSelected ? "selected" : "not selected"
		String actualState = actualSelected ? "selected" : "not selected"
		String status = (actualSelected == expectedSelected) ? "Pass" : "Fail"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Button selected", expectedState, actualState, status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyButtonSelected: expected ${expectedState}, actual ${actualState}")
		}
	}

	/**
	 * Click an element by its Object Repository path/name.
	 * Resolves XPath from the TestObject, scrolls into view, and clicks via JavaScript.
	 * Retries up to 3 times on StaleElementReferenceException.
	 * @param tabName Object Repository path (e.g. 'CRDC/DataSubmissions/Submit-Btn')
	 */
	@Keyword
	static void clickTab(String tabName) {
		WebDriver driver = DriverFactory.getWebDriver()
		WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT)
		JavascriptExecutor js = (JavascriptExecutor) driver
		TestObject obj = findTestObject(tabName)
		String tabXpath = obj.findPropertyValue('xpath')
		KeywordUtil.logInfo("Resolved XPath: " + tabXpath)
		for (int i = 0; i < 3; i++) {
			try {
				WebElement resultTab = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(tabXpath)))
				js.executeScript("arguments[0].scrollIntoView(true);", resultTab)
				js.executeScript("arguments[0].click();", resultTab)
				KeywordUtil.logInfo("Successfully clicked element")
				break
			} catch (StaleElementReferenceException e) {
				KeywordUtil.logInfo("StaleElementReferenceException. Retrying... Attempt: " + (i + 1))
				Thread.sleep(1000)
			} catch (Exception e) {
				KeywordUtil.logInfo("Unexpected error: " + e.getMessage())
				throw e
			}
		}
	}

	/**
	 * Navigate to a submission's upload-activity page.
	 * Builds URL from G_Urlname (profile) and the given submission ID.
	 * @param submissionId Submission UUID (e.g. from getSubmissionIdFromUploadActivityUrl)
	 */
	@Keyword
	static void navigateToSubmissionUploadActivity(String submissionId) {
		WebUI.waitForPageLoad(10)
		WebUI.delay(2)
		String baseUrl = GlobalVariable.G_Urlname?.toString()?.replaceAll(/\/$/, '') ?: ''
		String submissionUrl = "${baseUrl}/data-submission/${submissionId}/upload-activity"
		KeywordUtil.logInfo("Navigating to URL: " + submissionUrl)
		WebUI.navigateToUrl(submissionUrl)
		WebUI.waitForPageLoad(10)
		WebUI.delay(2)
	}
}
