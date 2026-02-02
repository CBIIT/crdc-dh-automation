package utilities

import com.kms.katalon.core.annotation.Keyword

/**
 * Handle validation results - check success and throw exception with details if failed.
 * ExportValidationReport is optional (if class exists, reports are exported on failure).
 */
class HandleValidationResult {

	/**
	 * Check result and optionally throw on failure.
	 * @param result - Map with 'success' boolean and 'mismatches' list
	 * @param validationName - Name of validation for error messages
	 * @param throwOnFailure - Whether to throw exception on failure (default: true)
	 * @param exportReport - Whether to export report if failed (default: true, optional if ExportValidationReport exists)
	 * @return boolean - true if passed, false if failed
	 */
	@Keyword
	static boolean checkAndThrow(Map<String, Object> result, String validationName, boolean throwOnFailure = true, boolean exportReport = true) {
		if (!result['success']) {
			List mismatches = result['mismatches'] ?: []
			println("\n❌ ${validationName} validation failed: ${mismatches.size()} mismatch(es)")
			mismatches.each { mismatch ->
				println("   - Row ${mismatch['row']}, Column '${mismatch['column']}': Expected '${mismatch['expected']}', Actual '${mismatch['actual']}'" +
					(mismatch['matchType'] ? " (matchType: ${mismatch['matchType']})" : ""))
			}
			if (exportReport) {
				try {
					Map<String, String> reportPaths = ExportValidationReport.export(result, validationName)
					println("📊 Detailed reports available: Excel: ${reportPaths['excelPath']}, HTML: ${reportPaths['htmlPath']}")
				} catch (Exception e) {
					println("⚠️  Report export skipped: ${e.message}")
				}
			}
			if (throwOnFailure) {
				throw new Exception("${validationName} validation failed: ${mismatches.size()} mismatches")
			}
			return false
		}
		println("✅ ${validationName} validation passed")
		return true
	}

	@Keyword
	static boolean checkAndLog(Map<String, Object> result, String validationName) {
		return checkAndThrow(result, validationName, false)
	}

	/**
	 * Handle Data View validation result with node-specific details.
	 */
	@Keyword
	static boolean checkDataViewResult(Map<String, Object> result, String validationName, boolean throwOnFailure = true, boolean exportReport = true) {
		if (!result['success']) {
			List failedNodes = result['nodeResults'].findAll { !it['success'] }.collect { it['nodeType'] }
			List allMismatches = result['mismatches'] ?: []
			println("❌ ${validationName} validation failed for node type(s): ${failedNodes.join(', ')}")
			println("   Total mismatches: ${allMismatches.size()}")
			allMismatches.take(5).each { mismatch ->
				println("   - Row ${mismatch['row']}, Column ${mismatch['column']}: Expected '${mismatch['expected']}', Actual '${mismatch['actual']}'")
			}
			if (exportReport) {
				try {
					Map<String, String> reportPaths = ExportValidationReport.export(result, validationName)
					println("📊 Detailed reports available: Excel: ${reportPaths['excelPath']}, HTML: ${reportPaths['htmlPath']}")
				} catch (Exception e) {
					println("⚠️  Report export skipped: ${e.message}")
				}
			}
			if (throwOnFailure) {
				throw new Exception("${validationName} validation failed for node type(s): ${failedNodes.join(', ')}")
			}
			return false
		}
		println("✅ ${validationName} validation passed for all node types")
		return true
	}
}
