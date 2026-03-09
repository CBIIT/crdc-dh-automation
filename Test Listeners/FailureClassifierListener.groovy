import com.kms.katalon.core.annotation.AfterTestCase
import com.kms.katalon.core.annotation.AfterTestSuite
import com.kms.katalon.core.annotation.BeforeTestSuite
import com.kms.katalon.core.context.TestCaseContext
import com.kms.katalon.core.context.TestSuiteContext
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.util.KeywordUtil

import utilities.FailureClassifier
import utilities.RetryReport

/**
 * Katalon Test Listener that intercepts test case outcomes, classifies failures
 * using FailureClassifier, and writes a retry-report.json via RetryReport.
 *
 * Enable/disable via Include/config/retry.properties (retry.enabled=true|false).
 */
class FailureClassifierListener {

	private static boolean enabled = true

	private static boolean loadConfig() {
		try {
			String projectDir = RunConfiguration.getProjectDir()
			File configFile = new File(projectDir, "Include/config/retry.properties")
			if (configFile.exists()) {
				Properties props = new Properties()
				configFile.withInputStream { props.load(it) }
				return props.getProperty("retry.enabled", "true").trim().equalsIgnoreCase("true")
			}
		} catch (Exception e) {
			KeywordUtil.logInfo("[FailureClassifierListener] Could not load config: ${e.message}")
		}
		return true
	}

	@BeforeTestSuite
	def beforeSuite(TestSuiteContext suiteContext) {
		enabled = loadConfig()
		if (!enabled) {
			KeywordUtil.logInfo("[FailureClassifierListener] Disabled via retry.properties")
			return
		}
		String suiteName = suiteContext.getTestSuiteId()
		if (suiteName?.contains("/")) {
			suiteName = suiteName.substring(suiteName.lastIndexOf("/") + 1)
		}
		RetryReport.beginSuite(suiteName)
		KeywordUtil.logInfo("[FailureClassifierListener] Initialized for suite: ${suiteName}")
	}

	@AfterTestCase
	def afterTestCase(TestCaseContext testCaseContext) {
		if (!enabled) return

		String testCaseId = testCaseContext.getTestCaseId() ?: "unknown"
		if (testCaseId.contains("/")) {
			testCaseId = testCaseId.substring(testCaseId.lastIndexOf("/") + 1)
		}
		String status = testCaseContext.getTestCaseStatus()
		String errorMessage = testCaseContext.getMessage() ?: ""

		Map<String, Object> result = [
			testCaseId: testCaseId,
			fullTestCaseId: testCaseContext.getTestCaseId(),
			status: status?.toUpperCase() ?: "UNKNOWN",
			errorMessage: "",
			classification: "N/A",
			reason: "",
			recommendation: "",
			retryAttempt: 0,
			retriedStatus: ""
		]

		if (status?.equalsIgnoreCase("FAILED") || status?.equalsIgnoreCase("ERROR")) {
			result.errorMessage = truncate(errorMessage, 2000)

			Map<String, String> classification = FailureClassifier.classify(errorMessage)
			result.classification = classification.classification
			result.reason = classification.reason
			result.recommendation = FailureClassifier.recommend(classification.classification, classification.reason)

			boolean retryable = FailureClassifier.isRetryable(classification.classification)

			KeywordUtil.logInfo("[FailureClassifierListener] ${testCaseId} -> ${status} | " +
				"Classification: ${classification.classification} | " +
				"Reason: ${classification.reason} | " +
				"Retryable: ${retryable}")
		} else {
			KeywordUtil.logInfo("[FailureClassifierListener] ${testCaseId} -> ${status}")
		}

		RetryReport.recordTestResult(result)
	}

	@AfterTestSuite
	def afterSuite(TestSuiteContext suiteContext) {
		if (!enabled) return
		try {
			RetryReport.writeReport()
			RetryReport.appendToHistory()
		} catch (Exception e) {
			KeywordUtil.logInfo("[FailureClassifierListener] Error writing report: ${e.message}")
		}
	}

	private static String truncate(String s, int maxLen) {
		if (s == null) return ""
		return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s
	}
}
