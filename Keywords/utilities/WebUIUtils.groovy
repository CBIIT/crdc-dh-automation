package utilities

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.testobject.TestObject
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions

/**
 * Generic, reusable web UI verification helpers (tooltips, element count, etc.).
 * Results are added to ValidationReport for generateValidationReport().
 */
class WebUIUtils {

	private static final String REPORT_SECTION = "Export Buttons"

	/**
	 * Verify tooltip text when it is in the element's aria-label attribute.
	 * Use for tooltips that expose their text via aria-label (e.g. Data View export tooltip).
	 * @param to TestObject for the element with aria-label
	 * @param expectedText Expected tooltip text
	 * @param timeout Wait timeout in seconds
	 */
	@Keyword
	static void verifyTooltipByAriaLabel(TestObject to, String expectedText, int timeout) {
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
	 * @param timeout Wait timeout in seconds for trigger element
	 */
	@Keyword
	static void verifyTooltipOnHover(TestObject triggerTo, String expectedText, String substringToFind, int timeout) {
		WebDriver driver = DriverFactory.getWebDriver()
		WebElement trigger = WebUI.findWebElement(triggerTo, timeout)
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
	 * @param timeout Wait timeout in seconds
	 */
	@Keyword
	static void verifyElementCount(TestObject to, int expectedCount, int timeout) {
		int count = WebUI.findWebElements(to, timeout).size()
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
	 * @param timeout Wait timeout in seconds
	 */
	@Keyword
	static void verifyElementDisplayed(TestObject to, int timeout) {
		boolean visible = WebUI.waitForElementVisible(to, timeout)
		String status = visible ? "Pass" : "Fail"
		String actualState = visible ? "displayed" : "not visible"
		ValidationReport.addValidationReportItem(REPORT_SECTION, "Element displayed", "displayed", actualState, status)
		if (status == "Fail") {
			KeywordUtil.markFailed("verifyElementDisplayed: element not visible within ${timeout}s")
		}
	}
}
