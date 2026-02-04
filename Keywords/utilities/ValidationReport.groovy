package utilities

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.util.KeywordUtil

import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.List
import java.util.Map

/**
 * Collects validation results from SubmissionRequest (and others) and generates an HTML report.
 * Call addValidationReportItem() during tests; call generateValidationReport() at end of test to write Reports/ValidationReport_*.html.
 */
class ValidationReport {

	/** In-memory collector for validation report: section name -> list of [label, expected, actual, status] */
	private static Map<String, List<Map<String, String>>> validationReportCollector = new LinkedHashMap<>()

	static void addValidationReportItem(String section, String label, String expected, String actual, String status) {
		if (!validationReportCollector.containsKey(section)) validationReportCollector.put(section, new ArrayList<>())
		String s = (status == "Pass" || status == "Fail") ? status : "Fail"
		validationReportCollector.get(section).add([
			label: label ?: "",
			expected: expected ?: "",
			actual: actual ?: "",
			status: s
		])
		// Also log to console so original logging is visible alongside the HTML report
		String exp = expected != null ? (expected.length() > 120 ? expected.take(120) + "..." : expected) : ""
		String act = actual != null ? (actual.length() > 120 ? actual.take(120) + "..." : actual) : ""
		KeywordUtil.logInfo("[${section}] ${label} -> ${s} | expected: ${exp} | actual: ${act}")
	}

	/** Clear the report collector (e.g. before a new test run). */
	@Keyword
	static void clearValidationReportCollector() {
		validationReportCollector.clear()
	}

	private static String escapeHtml(String s) {
		if (s == null) return ""
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
	}

	private static String buildValidationReportHtml() {
		StringBuilder sb = new StringBuilder()
		sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Validation Report</title>")
		sb.append("<style>table{border-collapse:collapse;width:100%;} th,td{border:1px solid #ccc;padding:6px;text-align:left;} th{background:#f5f5f5;} .pass{background:#e8f5e9;} .fail{background:#ffebee;} details{margin:12px 0;} summary{cursor:pointer;font-weight:bold;padding:8px;background:#e3f2fd;}</style>")
		sb.append("</head><body>")
		sb.append("<h1>Validation Report</h1>")
		sb.append("<p>Generated: ").append(escapeHtml(new Date().format("yyyy-MM-dd HH:mm:ss"))).append("</p>")
		for (Map.Entry<String, List<Map<String, String>>> entry : validationReportCollector.entrySet()) {
			String sectionName = entry.getKey()
			List<Map<String, String>> items = entry.getValue()
			sb.append("<details open><summary>").append(escapeHtml(sectionName)).append(" (").append(items.size()).append(" items)</summary>")
			sb.append("<table><thead><tr><th>Item / Line</th><th>Expected</th><th>Actual</th><th>Status</th></tr></thead><tbody>")
			for (Map<String, String> item : items) {
				String status = item.get("status") ?: "Fail"
				String rowClass = status == "Pass" ? "pass" : "fail"
				sb.append("<tr class=\"").append(rowClass).append("\">")
				sb.append("<td>").append(escapeHtml(item.get("label"))).append("</td>")
				sb.append("<td>").append(escapeHtml(item.get("expected"))).append("</td>")
				sb.append("<td>").append(escapeHtml(item.get("actual"))).append("</td>")
				sb.append("<td>").append(escapeHtml(status)).append("</td></tr>")
			}
			sb.append("</tbody></table></details>")
		}
		sb.append("</body></html>")
		return sb.toString()
	}

	/**
	 * Build HTML report from collected validation results (all three tabs), write to Reports/ folder, then clear the collector.
	 * Call this at the end of the test. Report is collapsible by tab (&lt;details&gt;/&lt;summary&gt;).
	 */
	@Keyword
	static String generateValidationReport() {
		String projectDir = RunConfiguration.getProjectDir()
		File reportsDir = new File(projectDir, "Reports")
		if (!reportsDir.exists()) reportsDir.mkdirs()
		String filename = "ValidationReport_" + new Date().format("yyyyMMdd_HHmmss") + ".html"
		File outFile = new File(reportsDir, filename)
		String html = buildValidationReportHtml()
		outFile.setText(html, "UTF-8")
		KeywordUtil.logInfo("Validation report written to: " + outFile.absolutePath)
		validationReportCollector.clear()
		return outFile.absolutePath
	}
}
