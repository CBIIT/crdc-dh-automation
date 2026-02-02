package utilities

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.configuration.RunConfiguration
import internal.GlobalVariable
import java.io.File

/**
 * Download table data by clicking export button and parsing the downloaded TSV.
 * Handles: click export button, detect downloaded file, parse TSV into headers + rows.
 */
class DownloadTableData {

	/**
	 * Click the export button, wait for the downloaded TSV file to appear, and return its path.
	 * @param buttonXPath - XPath to the export button (e.g. export-node-data-button)
	 * @param timeoutSeconds - Timeout in seconds to wait for the file
	 * @return Map: success (boolean), filePath (String), error (String)
	 */
	@Keyword
	static Map<String, Object> downloadAndRead(String buttonXPath, int timeoutSeconds) {
		String projectDir = RunConfiguration.getProjectDir()
		String downloadFolderName = 'Downloads/'
		try {
			if (GlobalVariable.hasProperty('downloadFolder') && GlobalVariable.downloadFolder) {
				downloadFolderName = GlobalVariable.downloadFolder
			}
		} catch (Exception e) { /* use default */ }
		if (!downloadFolderName.endsWith('/') && !downloadFolderName.endsWith(File.separator)) {
			downloadFolderName += '/'
		}
		File downloadDir = new File(projectDir, downloadFolderName)
		if (!downloadDir.exists()) {
			downloadDir.mkdirs()
		}

		long beforeClick = System.currentTimeMillis()
		List<File> existingTsv = listTsvFiles(downloadDir)
		Map<String, Long> existingPaths = [:]
		existingTsv.each { existingPaths[it.absolutePath] = it.lastModified() }

		TestObject exportBtn = FindObjectByXPath.findObject(buttonXPath)
		WebUI.waitForElementPresent(exportBtn, 10)
		WebUI.click(exportBtn)

		String foundPath = null
		int pollIntervalMs = 500
		int elapsed = 0
		while (elapsed < timeoutSeconds * 1000) {
			WebUI.delay(pollIntervalMs / 1000)
			elapsed += pollIntervalMs

			List<File> currentTsv = listTsvFiles(downloadDir)
			for (File f : currentTsv) {
				String path = f.absolutePath
				long mod = f.lastModified()
				if (!existingPaths.containsKey(path) || mod >= beforeClick) {
					if (mod >= beforeClick) {
						foundPath = path
						break
					}
					if (foundPath == null) foundPath = path
				}
			}
			if (foundPath != null) break
		}

		if (foundPath == null) {
			return [
				success: false,
				filePath: null,
				error: "No TSV file appeared in ${downloadDir.absolutePath} within ${timeoutSeconds}s"
			]
		}

		return [ success: true, filePath: foundPath, error: null ]
	}

	/**
	 * Parse a TSV file into headers and rows (same structure as ValidateDataView table).
	 * @param filePath - Absolute path to the TSV file
	 * @return Map with headers (List<String>) and rows (List<List<String>>)
	 */
	@Keyword
	static Map<String, Object> readTSVFile(String filePath) {
		File file = new File(filePath)
		if (!file.exists()) {
			throw new Exception("TSV file not found: ${filePath}")
		}

		List<String> headers = []
		List<List<String>> rows = []

		file.eachLine { line, lineNum ->
			String[] parts = line.split('\t', -1)
			if (lineNum == 1) {
				headers = parts.collect { it != null ? it.trim() : '' }
			} else {
				rows.add(parts.collect { it != null ? it.trim() : '' })
			}
		}

		return [ headers: headers, rows: rows ]
	}

	private static List<File> listTsvFiles(File dir) {
		if (!dir.exists() || !dir.isDirectory()) return []
		File[] list = dir.listFiles()
		if (list == null) return []
		return list.findAll { it.isFile() && it.name.toLowerCase().endsWith('.tsv') }
	}
}
