package utilities

import com.kms.katalon.core.util.KeywordUtil

/**
 * Rule-based failure classifier for test execution errors.
 * Categorizes failures as FLAKY, ENVIRONMENT, or REAL based on pattern matching
 * against exception messages and log output.
 */
class FailureClassifier {

	static final String FLAKY = "FLAKY"
	static final String ENVIRONMENT = "ENVIRONMENT"
	static final String REAL = "REAL"

	private static final List<Map<String, String>> FLAKY_PATTERNS = [
		[pattern: 'StaleElementReferenceException', reason: 'DOM element became stale during interaction'],
		[pattern: 'ElementClickInterceptedException', reason: 'Click intercepted by overlay or loading element'],
		[pattern: 'ElementNotInteractableException', reason: 'Element not interactable — timing/visibility issue'],
		[pattern: 'element is not attached to the page document', reason: 'DOM element detached during interaction'],
		[pattern: 'element click intercepted', reason: 'Click intercepted by overlay'],
		[pattern: 'waitForElementToDisappear', reason: 'Loading indicator did not disappear in time'],
		[pattern: 'Loading-Icon', reason: 'Loading icon timeout'],
		[pattern: 'Grant button not found', reason: 'OTP/Login flow race condition'],
		[pattern: 'OTP failed', reason: 'OTP generation or submission timing issue'],
		[pattern: 'likely OTP failed', reason: 'Login.gov OTP flow failed — transient'],
		[pattern: 'Unable to click on object', reason: 'Katalon click failed — possible timing issue'],
		[pattern: 'Object is not clickable at point', reason: 'Element obscured or not yet interactive'],
		[pattern: 'TimeoutException', reason: 'Operation timed out — possible slow page load'],
		[pattern: 'NoSuchSessionException', reason: 'Browser session lost — transient WebDriver issue'],
		[pattern: 'WebDriverException: disconnected', reason: 'Browser disconnected unexpectedly'],
		[pattern: 'WebDriverException: unknown error: net::ERR_', reason: 'Network error during page load'],
		[pattern: 'javascript error: Cannot read propert', reason: 'JS error from timing — page not fully loaded'],
	]

	private static final List<Map<String, String>> ENVIRONMENT_PATTERNS = [
		[pattern: 'path does not exist or is not a directory', reason: 'File path case sensitivity (Linux vs macOS)'],
		[pattern: 'metadata_encoding', reason: 'Python version < 3.11 missing metadata_encoding support'],
		[pattern: 'pip install', reason: 'Python dependency installation issue'],
		[pattern: 'connection refused', reason: 'Service or network connection refused'],
		[pattern: 'Connection reset', reason: 'Network connection reset'],
		[pattern: 'ERR_CONNECTION_REFUSED', reason: 'Target application not reachable'],
		[pattern: 'ERR_NAME_NOT_RESOLVED', reason: 'DNS resolution failure'],
		[pattern: 'unable to connect to renderer', reason: 'Browser process failed to start'],
		[pattern: 'session not created', reason: 'ChromeDriver/browser version mismatch'],
		[pattern: 'cannot find Chrome binary', reason: 'Chrome not installed or path incorrect'],
	]

	/**
	 * Classify a failure based on its error message.
	 * @return Map with keys: classification (FLAKY|ENVIRONMENT|REAL), reason (String)
	 */
	static Map<String, String> classify(String errorMessage) {
		if (errorMessage == null || errorMessage.trim().isEmpty()) {
			return [classification: REAL, reason: 'No error message available']
		}

		String lowerMsg = errorMessage.toLowerCase()

		for (Map<String, String> entry : FLAKY_PATTERNS) {
			if (lowerMsg.contains(entry.pattern.toLowerCase())) {
				return [classification: FLAKY, reason: entry.reason]
			}
		}

		for (Map<String, String> entry : ENVIRONMENT_PATTERNS) {
			if (lowerMsg.contains(entry.pattern.toLowerCase())) {
				return [classification: ENVIRONMENT, reason: entry.reason]
			}
		}

		return [classification: REAL, reason: 'No flaky or environment pattern matched']
	}

	/**
	 * Convenience check: is this failure retryable?
	 */
	static boolean isRetryable(String classification) {
		return classification == FLAKY || classification == ENVIRONMENT
	}

	/**
	 * Build a recommendation string based on classification.
	 */
	static String recommend(String classification, String reason) {
		switch (classification) {
			case FLAKY:
				return "Transient issue — should pass on retry. If recurring, consider increasing timeouts or adding explicit waits."
			case ENVIRONMENT:
				return "Environment issue: ${reason}. Fix the environment configuration; retrying may not help."
			case REAL:
				return "Real test failure — investigate the root cause in test logic or application behavior."
			default:
				return "Unknown classification."
		}
	}
}
