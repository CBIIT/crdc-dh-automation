package utilities

import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.util.KeywordUtil
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

/**
 * Collects per-test-case failure classification results during a suite run
 * and writes a retry-report.json at the end. This file is consumed by the
 * post-run AI triage agent (Tools/ai-triage/) for deeper analysis.
 */
class RetryReport {

	private static List<Map<String, Object>> testResults = []
	private static String suiteId = ""
	private static String suiteTimestamp = ""

	static void beginSuite(String suiteName) {
		suiteId = suiteName ?: "unknown"
		suiteTimestamp = new Date().format("yyyy-MM-dd'T'HH:mm:ss")
		testResults = []
		KeywordUtil.logInfo("[RetryReport] Suite started: ${suiteId}")
	}

	/**
	 * Record the outcome of a single test case.
	 * Called from the test listener after classification.
	 */
	static void recordTestResult(Map<String, Object> result) {
		testResults.add(result)
	}

	/**
	 * Write the collected results to Reports/retry-report.json.
	 * If a previous report exists, it is overwritten (one report per suite run).
	 */
	static String writeReport() {
		String projectDir = RunConfiguration.getProjectDir()
		File reportsDir = new File(projectDir, "Reports")
		if (!reportsDir.exists()) reportsDir.mkdirs()

		Map<String, Object> report = [
			suiteId: suiteId,
			timestamp: suiteTimestamp,
			summary: buildSummary(),
			testResults: testResults
		]

		File outFile = new File(reportsDir, "retry-report.json")
		String json = new JsonBuilder(report).toPrettyString()
		outFile.setText(json, "UTF-8")
		KeywordUtil.logInfo("[RetryReport] Written to: ${outFile.absolutePath}")
		KeywordUtil.logInfo("[RetryReport] Summary — total: ${testResults.size()}, " +
				"passed: ${testResults.count { it.status == 'PASSED' }}, " +
				"failed: ${testResults.count { it.status == 'FAILED' }}, " +
				"flaky: ${testResults.count { it.classification == 'FLAKY' }}, " +
				"environment: ${testResults.count { it.classification == 'ENVIRONMENT' }}, " +
				"real: ${testResults.count { it.classification == 'REAL' }}")
		return outFile.absolutePath
	}

	private static Map<String, Object> buildSummary() {
		int total = testResults.size()
		int passed = testResults.count { it.status == 'PASSED' }
		int failed = testResults.count { it.status == 'FAILED' }
		int flaky = testResults.count { it.classification == 'FLAKY' }
		int environment = testResults.count { it.classification == 'ENVIRONMENT' }
		int real = testResults.count { it.classification == 'REAL' }
		int retriedAndPassed = testResults.count { it.retriedStatus == 'PASSED' }

		return [
			total: total,
			passed: passed,
			failed: failed,
			classifications: [
				flaky: flaky,
				environment: environment,
				real: real
			],
			retriedAndPassed: retriedAndPassed
		]
	}

	/**
	 * Append historical trend data. Reads previous triage history file (if exists),
	 * adds current run summary, and writes back.
	 */
	static void appendToHistory() {
		String projectDir = RunConfiguration.getProjectDir()
		File historyFile = new File(projectDir, "Reports/AI_Triage/run-history.json")

		List<Map<String, Object>> history = []
		if (historyFile.exists()) {
			try {
				history = new JsonSlurper().parseText(historyFile.getText("UTF-8")) as List
			} catch (Exception e) {
				KeywordUtil.logInfo("[RetryReport] Could not parse history file, starting fresh: ${e.message}")
			}
		}

		history.add([
			suiteId: suiteId,
			timestamp: suiteTimestamp,
			summary: buildSummary(),
			failedTests: testResults.findAll { it.status == 'FAILED' }.collect {
				[
					testCaseId: it.testCaseId,
					classification: it.classification
				]
			}
		])

		// Keep last 20 runs
		if (history.size() > 20) {
			history = history.subList(history.size() - 20, history.size())
		}

		if (!historyFile.parentFile.exists()) historyFile.parentFile.mkdirs()
		historyFile.setText(new JsonBuilder(history).toPrettyString(), "UTF-8")
		KeywordUtil.logInfo("[RetryReport] History updated: ${historyFile.absolutePath} (${history.size()} runs)")
	}
}
