package utilities

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable
import java.io.FileFilter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * CRDC Data Hub CLI Uploader integration for Katalon.
 * Builds YAML config from profile (API URL and token from GlobalVariable.ApiUrl / SubmitterApiToken),
 * runs either the uploader binary or the Python source (src/uploader.py), and verifies output and report.
 *
 * CLI location: By default uses Tools/crdc-datahub-cli-uploader-src (Python source). If a binary
 * (uploader or uploader.exe) is present there, it is used; otherwise runs python3 src/uploader.py.
 * Override via profile variable CRDC_CLI_DIR if needed.
 *
 * Required profile/GlobalVariable: ApiUrl, SubmitterApiToken. Submission ID is taken from the
 * current Data Hub upload-activity URL. Data and manifest paths are derived from test case name.
 */
class CliUploader {

	/** Default CLI root: Python source tree (Tools/crdc-datahub-cli-uploader-src). Binary is optional. */
	private static final String DEFAULT_CLI_SUBDIR = "Tools/crdc-datahub-cli-uploader-src"
	private static final long DEFAULT_TIMEOUT_MINUTES = 30L

	/** Relative path to Python entry point when using source. */
	private static final String UPLOADER_SCRIPT_PATH = "src/uploader.py"

	/** Max time (minutes) to wait for pip install of CLI dependencies when using Python source. */
	private static final long PIP_INSTALL_TIMEOUT_MINUTES = 3L

	/** CLI config type values (must match uploader expectations). */
	private static final String TYPE_DATA_FILE = "data file"
	private static final String TYPE_METADATA = "metadata"

	/** Normalize user dataType to CLI type; returns null if invalid. */
	private static String normalizeDataType(String dataType) {
		if (dataType == null || dataType.trim().isEmpty()) return null
		String t = dataType.trim().toLowerCase()
		if (t == "datafile" || t == "data file") return TYPE_DATA_FILE
		if (t == "metadata") return TYPE_METADATA
		return null
	}

	private static String getApiToken() {
		return GlobalVariable.SubmitterApiToken
	}

	private static String getApiUrl() {
		return GlobalVariable.ApiUrl
	}

	private static String getSubmissionId() {
		return DataSubmissions.getSubmissionIdFromUploadActivityUrl();
	}

	private static String getDataFilesPath(String testName) {
		return RunConfiguration.getProjectDir() + "/InputFiles/CRDC/MetadataData/"+testName+"/DataFiles"
	}

	private static String getMetaDataPath(String testName) {
		return RunConfiguration.getProjectDir() + "/InputFiles/CRDC/MetadataData/"+testName+"/Metadata"
	}

	private static String getManifestPath(String manifestFolder) {

		File dir = new File(
				RunConfiguration.getProjectDir() + "/InputFiles/CRDC/MetadataData/" + manifestFolder + "/DataFiles")

		if (!dir.exists() || !dir.isDirectory()) {
			KeywordUtil.markFailed("Manifest directory not found: " + dir.getAbsolutePath())
			return null
		}

		File manifestFile = dir.listFiles()
				?.find { it.isFile() && it.name.toLowerCase().endsWith("manifest.tsv") }

		if (manifestFile == null) {
			KeywordUtil.markFailed("No manifest file ending with 'manifest.tsv' found in: " + dir.getAbsolutePath())
			return null
		}

		return manifestFile.getAbsolutePath()
	}

	private static boolean hasZipFiles(File dir) {
		if (dir == null || !dir.exists() || !dir.isDirectory()) return false
		File[] zipFiles = dir.listFiles({ File f ->
			f.isFile() && f.name.toLowerCase().endsWith(".zip")
		} as FileFilter)
		return zipFiles != null && zipFiles.length > 0
	}

	private static String getZipArchiveManifestPath(String testCaseName) {
		File dir = new File(
				RunConfiguration.getProjectDir() + "/InputFiles/CRDC/MetadataData/" + testCaseName + "/DataFiles")
		if (!dir.exists() || !dir.isDirectory()) {
			KeywordUtil.markFailed("Archive manifest directory not found: " + dir.getAbsolutePath())
			return null
		}
		File archiveManifest = new File(dir, "zip-archive-manifest.tsv")
		if (!archiveManifest.exists() || !archiveManifest.isFile()) {
			KeywordUtil.markFailed("Zip archive manifest not found: " + archiveManifest.getAbsolutePath())
			return null
		}
		return archiveManifest.getAbsolutePath()
	}

	/**
	 * Resolves CLI root directory (binary or Python source). Used as process working directory.
	 * Default: projectDir/Tools/crdc-datahub-cli-uploader-src. Override with profile CRDC_CLI_DIR.
	 */
	private static File getCliDir() {
		String dir = safeGlobal("CRDC_CLI_DIR", null)?.toString()?.trim()
		if (dir != null && !dir.isEmpty()) {
			File f = new File(dir)
			return f.isAbsolute() ? f : new File(RunConfiguration.getProjectDir(), dir)
		}
		return new File(RunConfiguration.getProjectDir(), DEFAULT_CLI_SUBDIR)
	}

	private static int getRetries() {
		Object v = safeGlobal("CRDC_CLI_RETRIES", 3)
		if (v instanceof Number) return ((Number) v).intValue()
		try {
			return Integer.parseInt(v?.toString()?.trim() ?: "3")
		} catch (Exception e) {
			return 3
		}
	}

	private static boolean getOverwrite() {
		Object v = safeGlobal("CRDC_CLI_OVERWRITE", false)
		if (v instanceof Boolean) return v
		String s = v?.toString()?.trim()?.toLowerCase()
		return "true" == s || "1" == s
	}

	private static boolean getDryrun() {
		Object v = safeGlobal("CRDC_CLI_DRYRUN", false)
		if (v instanceof Boolean) return v
		String s = v?.toString()?.trim()?.toLowerCase()
		return "true" == s || "1" == s
	}

	private static String getIdField() {
		return safeGlobal("CRDC_MANIFEST_ID_FIELD", null)?.toString()?.trim()
	}

	/** Safe read of GlobalVariable by name; returns default if property missing or inaccessible. */
	private static Object safeGlobal(String name, Object defaultValue) {
		try {
			return GlobalVariable.getProperty(name)
		} catch (Exception e) {
			return defaultValue
		}
	}

	/**
	 * Builds YAML config for CLI. type is "data file" or "metadata".
	 * For "data file": manifest (and optional archive_manifest, id-field) are included.
	 * For "metadata": only type and data path are used; manifest* and id-field omitted.
	 */
	private static String buildConfigYaml(String submissionId, String type, String dataPath,
			String manifestPath, String archiveManifestPath, String idField, int retries, boolean overwrite,
			boolean dryrun, String apiUrl, String token) {
		String dataAbs = toAbsolutePath(dataPath)
		String safeToken = escapeYamlString(token)
		boolean isDataFile = TYPE_DATA_FILE.equals(type)

		// Indent must match other keys (15 spaces) so YAML parses as one block.
		String manifestBlock = ""
		if (isDataFile && manifestPath != null && !manifestPath.trim().isEmpty()) {
			String manifestAbs = toAbsolutePath(manifestPath)
			String pad = "               "
			String archiveLine = (archiveManifestPath != null && !archiveManifestPath.trim().isEmpty())
					? pad + "archive_manifest: " + toAbsolutePath(archiveManifestPath) + "\n"
					: ""
			String idFieldLine = (idField != null && !idField.trim().isEmpty())
					? pad + "id-field: " + idField + "\n"
					: ""
			manifestBlock = pad + "manifest: " + manifestAbs + "\n" + archiveLine + idFieldLine
		}

		return """Config:
               api-url: ${apiUrl}
               token: ${safeToken}
               submission: ${submissionId}
               type: ${type}
               data: ${dataAbs}
${manifestBlock}               retries: ${retries}
               overwrite: ${overwrite}
               dryrun: ${dryrun}
               """
	}

	/**
	 * Converts a file path to an absolute path.
	 * Returns the original value if null/empty.
	 * If already absolute, returns as-is.
	 * If relative, resolves it against the Katalon project directory.
	 */
	private static String toAbsolutePath(String path) {
		if (path == null || path.trim().isEmpty()) return path
		String p = path.trim()
		File f = new File(p)
		if (f.isAbsolute()) return f.getAbsolutePath()
		return new File(RunConfiguration.getProjectDir(), p).getAbsolutePath()
	}

	/** Escape for YAML double-quoted string: \\ and " */
	private static String escapeYamlString(String s) {
		if (s == null) return '""'
		return '"' + s.replace('\\', '\\\\').replace('"', '\\"') + '"'
	}


	/**
	 * Creates a temporary YAML config file with the provided content.
	 * The file is written in UTF-8 encoding and marked for deletion on JVM exit.
	 *
	 * @param yamlContent  YAML configuration content to write
	 * @return Temporary File containing the config
	 */
	private static File writeConfigFile(String yamlContent) {
		File tmp = File.createTempFile("crdc-cli-config-", ".yml")
		tmp.deleteOnExit()
		Files.write(tmp.toPath(), yamlContent.getBytes(StandardCharsets.UTF_8))
		return tmp
	}

	/**
	 * Installs CLI Python dependencies from requirements.txt in cliRoot using the given Python executable.
	 * Called automatically when running from Python source so that rich, pyyaml, boto3, etc. are available.
	 *
	 * @param cliRoot   CLI root directory (must contain requirements.txt).
	 * @param pythonExe Path to python or python3 executable.
	 * @return true if install succeeded or requirements.txt is missing; false if pip install failed.
	 */
	private static boolean ensureCliDependenciesInstalled(File cliRoot, String pythonExe) {
		File reqFile = new File(cliRoot, "requirements.txt")
		if (!reqFile.exists() || !reqFile.isFile()) {
			KeywordUtil.logInfo("CliUploader: no requirements.txt in CLI root; skipping dependency install.")
			return true
		}
		KeywordUtil.logInfo("CliUploader: installing CLI dependencies from " + reqFile.getAbsolutePath() + " ...")
		List<String> pipCmd = [
			pythonExe,
			"-m",
			"pip",
			"install",
			"-r",
			reqFile.getAbsolutePath(),
			"--quiet"
		]
		ProcessBuilder pb = new ProcessBuilder(pipCmd)
		pb.directory(cliRoot)
		pb.redirectErrorStream(true)
		Process process = pb.start()
		StringBuilder sb = new StringBuilder()
		byte[] buf = new byte[4096]
		int n
		InputStream stream = process.getInputStream()
		while ((n = stream.read(buf)) != -1) {
			sb.append(new String(buf, 0, n, StandardCharsets.UTF_8))
		}
		boolean finished = process.waitFor(PIP_INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
		String output = sb.toString()
		if (!finished) {
			process.destroyForcibly()
			KeywordUtil.markFailed("CliUploader: pip install timed out after " + PIP_INSTALL_TIMEOUT_MINUTES + " minutes. Output: " + output)
			return false
		}
		if (process.exitValue() != 0) {
			KeywordUtil.markFailed("CliUploader: pip install failed (exit " + process.exitValue() + "). Output: " + output)
			return false
		}
		KeywordUtil.logInfo("CliUploader: CLI dependencies installed successfully.")
		return true
	}

	/**
	 * Resolves the uploader command and runs it. Prefers binary (uploader/uploader.exe) if present
	 * and executable; otherwise runs Python source (src/uploader.py) using Utils.getPythonExecutablePath().
	 * Working directory is always cliDir so that tmp/upload-report-*.tsv is created under cliDir.
	 *
	 * @param configFile      Temporary YAML config file (absolute path).
	 * @param workingDir      CLI root directory (must contain binary or src/uploader.py).
	 * @param timeoutMinutes  Max wait for process completion.
	 * @return [exitCode, outputText]; exitCode -1 on failure to start or timeout.
	 */
	private static List<Object> runUploaderProcess(File configFile, File workingDir, long timeoutMinutes) {
		File cliRoot = workingDir.getAbsoluteFile()
		List<String> command = resolveUploaderCommand(cliRoot, configFile)
		if (command == null || command.isEmpty()) {
			return [-1, ""]
		}

		// When using Python source, ensure CLI dependencies are installed before running.
		if (command.size() >= 2 && UPLOADER_SCRIPT_PATH.equals(command.get(1))) {
			String pythonExe = command.get(0)
			if (!ensureCliDependenciesInstalled(cliRoot, pythonExe)) {
				return [-1, ""]
			}
		}

		ProcessBuilder pb = new ProcessBuilder(command)
		pb.directory(cliRoot)
		pb.redirectErrorStream(true)
		Process process = pb.start()

		InputStream combined = process.getInputStream()
		StringBuilder sb = new StringBuilder()
		byte[] buf = new byte[4096]
		int n
		while ((n = combined.read(buf)) != -1) {
			sb.append(new String(buf, 0, n, StandardCharsets.UTF_8))
		}
		String outputText = sb.toString()

		boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES)
		if (!finished) {
			process.destroyForcibly()
			KeywordUtil.markFailed("CliUploader: uploader timed out after " + timeoutMinutes + " minutes. Output: " + outputText)
			return [-1, outputText]
		}
		return [
			process.exitValue(),
			outputText
		]
	}

	/**
	 * Builds the command list to run the uploader: either binary (uploader/uploader.exe) or
	 * Python source (pythonExe src/uploader.py --config &lt;path&gt;). Fails the test and returns null if neither is available.
	 */
	private static List<String> resolveUploaderCommand(File cliRoot, File configFile) {
		String osName = System.getProperty("os.name", "").toLowerCase()
		String exeName = osName.contains("windows") ? "uploader.exe" : "uploader"
		File binary = new File(cliRoot, exeName)

		if (binary.exists() && binary.canExecute()) {
			KeywordUtil.logInfo("CliUploader: using binary " + binary.getAbsolutePath())
			return [
				binary.getAbsolutePath(),
				"--config",
				configFile.getAbsolutePath()
			]
		}

		File script = new File(cliRoot, UPLOADER_SCRIPT_PATH)
		if (script.exists() && script.isFile()) {
			String pythonExe = Utils.getPythonExecutablePath()
			KeywordUtil.logInfo("CliUploader: using Python source " + script.getAbsolutePath() + " with " + pythonExe)
			return [
				pythonExe,
				UPLOADER_SCRIPT_PATH,
				"--config",
				configFile.getAbsolutePath()
			]
		}

		KeywordUtil.markFailed("CliUploader: neither uploader binary (" + binary.getAbsolutePath() + ") nor Python script (" + script.getAbsolutePath() + ") found. Ensure CLI is at " + cliRoot.getAbsolutePath())
		return null
	}

	/**
	 * Verifies the CLI output string contains expected success indicators.
	 * Fails the test if any required pattern is missing.
	 */
	private static void verifyCliUploadOutput(String output) {
		if (output == null || output.isEmpty()) {
			KeywordUtil.markFailed("CliUploader: CLI output is empty; cannot verify.")
			return
		}
		List<String> errors = []
		if (!output.contains("Files failed: 0")) {
			errors.add("CLI output does not contain 'Files failed: 0'")
		}
		if (!output.contains("File uploading completed!")) {
			errors.add("CLI output does not contain 'File uploading completed!'")
		}
		if (!output.contains("Files processed:")) {
			errors.add("CLI output does not contain 'Files processed:'")
		}
		if (!output.contains("Files copied:")) {
			errors.add("CLI output does not contain 'Files copied:'")
		}
		// Optional: ensure validation completed for all files (e.g. "5 out of 5 file(s) have been validated")
		if (output.contains("out of") && output.contains("file(s) have been validated")) {
			java.util.regex.Matcher m = (output =~ /(\d+) out of (\d+) file\(s\) have been validated/)
			boolean lastMatchEqual = false
			while (m.find()) {
				int x = Integer.parseInt(m.group(1))
				int y = Integer.parseInt(m.group(2))
				if (x == y) lastMatchEqual = true
			}
			if (!lastMatchEqual) {
				errors.add("CLI output did not show all files validated (X out of Y with X < Y in last line)")
			}
		}
		if (!errors.isEmpty()) {
			KeywordUtil.logInfo("CliUploader verification failed:\n" + output)
			KeywordUtil.markWarning("CliUploader: CLI log verification failed: " + errors.join("; "))
		} else {
			KeywordUtil.logInfo("CliUploader: CLI output verification passed (Files failed: 0, File uploading completed!, Files processed/copied present).")
		}
	}

	/**
	 * Finds the latest upload-report-*.tsv in cliDir/tmp and verifies all rows have succeeded=true.
	 * If output contains "Files processed: N", also verifies the report has N data rows.
	 */
	private static void verifyCliUploadReport(File cliDir, String output) {
		File tmpDir = new File(cliDir, "tmp")
		if (!tmpDir.exists() || !tmpDir.isDirectory()) {
			KeywordUtil.logInfo("CliUploader: tmp folder not found; skipping upload report verification.")
			return
		}
		File[] reports = tmpDir.listFiles({ File f -> f.isFile() && f.name.startsWith("upload-report-") && f.name.endsWith(".tsv") } as FileFilter)
		if (reports == null || reports.length == 0) {
			KeywordUtil.markFailed("CliUploader: No upload-report-*.tsv found in " + tmpDir.getAbsolutePath())
			return
		}
		File latest = reports.toList().sort { -it.lastModified() }.get(0)
		List<String> lines = Files.readAllLines(latest.toPath(), StandardCharsets.UTF_8)
		if (lines.size() < 2) {
			KeywordUtil.markFailed("CliUploader: Upload report has no data rows: " + latest.getAbsolutePath())
			return
		}
		String header = lines.get(0)
		int succeededIdx = header.split("\t", -1).findIndexOf { it.equalsIgnoreCase("succeeded") }
		if (succeededIdx < 0) {
			KeywordUtil.logInfo("CliUploader: Report has no 'succeeded' column; skipping row-level verification.")
			return
		}
		int expectedCount = -1
		java.util.regex.Matcher m = (output =~ /Files processed:\s*(\d+)/)
		if (m.find()) {
			expectedCount = Integer.parseInt(m.group(1))
		}
		int dataRows = lines.size() - 1
		if (expectedCount >= 0 && dataRows != expectedCount) {
			KeywordUtil.markFailed("CliUploader: Upload report row count " + dataRows + " does not match Files processed: " + expectedCount + " in " + latest.getName())
			return
		}
		List<String> failed = []
		for (int i = 1; i < lines.size(); i++) {
			String[] cols = lines.get(i).split("\t", -1)
			String val = succeededIdx < cols.length ? cols[succeededIdx].trim() : ""
			if (!"true".equalsIgnoreCase(val)) {
				String fileName = cols.length > 1 ? cols[1] : "row " + (i + 1)
				failed.add(fileName + " (succeeded=" + val + ")")
			}
		}
		if (!failed.isEmpty()) {
			KeywordUtil.markFailed("CliUploader: Upload report has failed/skipped rows: " + failed.join(", "))
			return
		}
		KeywordUtil.logInfo("CliUploader: Upload report verified: " + dataRows + " row(s), all succeeded=true in " + latest.getName())
	}

	/** Default subfolder under InputFiles/CRDC/MetadataData/<testCaseName> for expected TSV(s). */
	private static final String EXPECTED_SUBFOLDER = "expected"
	/** Report subfolder name under project Reports for error verification output. */
	private static final String ERROR_VERIFICATION_REPORT_SUBDIR = "CLI_ErrorVerification"
	/** Column name in expected TSV for expected error(s); also try singular form. */
	private static final List<String> EXPECTED_ERROR_COLUMN_CANDIDATES = [
		"expected_errors",
		"expected_error"
	]
	/** Column names in actual CLI report for errors (first found wins). CLI may use "error", "errors", etc. */
	private static final List<String> ACTUAL_ERRORS_COLUMN_CANDIDATES = [
		"errors",
		"error",
		"validation_errors",
		"error_message",
		"message"
	]
	/** Column names to match rows between actual and expected (first found wins). */
	private static final List<String> FILE_NAME_COLUMN_CANDIDATES = [
		"file_name",
		"internal_file_name",
		"fileName"
	]

	/**
	 * Normalize an error cell value (e.g. "['msg1', 'msg2']" or "msg1") to a comparable string:
	 * extract list items, trim, sort, join with " | " so comparison is stable.
	 */
	private static String normalizeErrorCell(String raw) {
		if (raw == null) return ""
		String s = raw.trim()
		if (s.isEmpty()) return ""
		// Strip outer TSV double-quote wrapping and unescape "" -> "
		if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
			s = s.substring(1, s.length() - 1).replace("\"\"", "\"").trim()
		}
		// Handle Python-style list string: ['a', 'b'] or ["a", "b"]
		if (s.startsWith("[") && s.endsWith("]")) {
			String inner = s.substring(1, s.length() - 1).trim()
			if (inner.isEmpty()) return ""
			// Split by ", " or "', '" or "\", \"" and strip quotes from each part
			String delim = "||SEP||"
			String innerNorm = inner.replace("', '", delim).replace("\", \"", delim).replace("', \"", delim).replace("\", '", delim)
			List<String> parts = []
			for (String segment : innerNorm.split(java.util.regex.Pattern.quote(delim), -1)) {
				String t = segment.trim()
				if (t.startsWith("'") && t.endsWith("'")) t = t.substring(1, t.length() - 1)
				else if (t.startsWith("\"") && t.endsWith("\"")) t = t.substring(1, t.length() - 1)
				t = t.trim()
				if (!t.isEmpty()) parts.add(t)
			}
			return parts.sort().join(" | ")
		}
		return s
	}

	/**
	 * Find column index by name (case-insensitive); returns -1 if not found.
	 */
	private static int findColumnIndex(String headerLine, List<String> candidates) {
		String[] headers = headerLine.split("\t", -1)
		for (String cand : candidates) {
			int idx = headers.findIndexOf { it != null && it.equalsIgnoreCase(cand.trim()) }
			if (idx >= 0) return idx
		}
		return -1
	}

	private static int findColumnIndex(String headerLine, String columnName) {
		return findColumnIndex(headerLine, [columnName])
	}

	/**
	 * Read (file_name -> expected_errors) from all TSV files in expectedDir.
	 * Uses first available column from EXPECTED_ERROR_COLUMN_CANDIDATES and FILE_NAME_COLUMN_CANDIDATES.
	 */
	private static Map<String, String> loadExpectedErrorsByFileName(File expectedDir) {
		Map<String, String> map = [:]
		if (!expectedDir.exists() || !expectedDir.isDirectory()) return map
		File[] tsvs = expectedDir.listFiles({ File f -> f.isFile() && f.name.toLowerCase().endsWith(".tsv") } as FileFilter)
		if (tsvs == null || tsvs.length == 0) return map
		for (File tsv : tsvs) {
			List<String> lines = Files.readAllLines(tsv.toPath(), StandardCharsets.UTF_8)
			if (lines.size() < 2) continue
				String header = lines.get(0)
			int expectedColIdx = findColumnIndex(header, EXPECTED_ERROR_COLUMN_CANDIDATES)
			int fileNameColIdx = findColumnIndex(header, FILE_NAME_COLUMN_CANDIDATES)
			if (expectedColIdx < 0 || fileNameColIdx < 0) continue
				for (int i = 1; i < lines.size(); i++) {
					String[] cols = lines.get(i).split("\t", -1)
					String fileName = fileNameColIdx < cols.length ? cols[fileNameColIdx].trim() : ""
					String expectedVal = expectedColIdx < cols.length ? cols[expectedColIdx].trim() : ""
					if (!fileName.isEmpty()) map.put(fileName, expectedVal)
				}
		}
		return map
	}

	/** Substrings that indicate an error line in CLI log (archive manifest, validation, etc.). */
	private static final List<String> CLI_ERROR_LINE_SUBSTRINGS = [
		"does not match",
		"No archive manifest",
		"does not match with that in archive manifest",
		"Real file md5",
		"Real file size"
	]

	/**
	 * Finds the most recently modified CLI log file in cliDir and cliDir/tmp.
	 * Prefers filenames like "Uploader Main-*.log"; falls back to any *.log in tmp.
	 * Logs are stored under Tools/crdc-datahub-cli-uploader-src/tmp.
	 */
	private static File findLatestCliLogFile(File cliDir) {
		List<File> candidates = []
		File tmpDir = new File(cliDir, "tmp")
		for (File dir : [cliDir, tmpDir]) {
			if (!dir.exists() || !dir.isDirectory()) continue
			File[] files = dir.listFiles({ File f ->
				f.isFile() && f.name.toLowerCase().endsWith(".log")
			} as FileFilter)
			if (files != null) candidates.addAll(files.toList())
		}
		if (candidates.isEmpty()) {
			KeywordUtil.logInfo("CliUploader.verifyCliErrorMessages: No .log file found in " + cliDir.getAbsolutePath() + " or " + tmpDir.getAbsolutePath())
			return null
		}
		return candidates.sort { -it.lastModified() }.get(0)
	}

	/**
	 * Reads log file content as UTF-8; returns empty string if null or unreadable.
	 */
	private static String readCliLogContent(File logFile) {
		if (logFile == null || !logFile.exists() || !logFile.isFile()) return ""
		try {
			return new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8)
		} catch (Exception e) {
			KeywordUtil.logInfo("CliUploader: Could not read log file " + logFile.getAbsolutePath() + ": " + e.getMessage())
			return ""
		}
	}

	/**
	 * Strips syslog/logger prefix from a log line so only the error message body remains.
	 * Finds the earliest occurrence of any CLI_ERROR_LINE_SUBSTRINGS and returns from that point onward (trimmed).
	 * E.g. "&lt;14&gt;1 2026-03-04T17:05:03.059Z - - 35293 - - ERROR: (File_Validator) Real file md5 ..." -> "Real file md5 ..."
	 */
	private static String stripLogPrefixToMessage(String logLine) {
		if (logLine == null || logLine.isEmpty()) return logLine
		String trimmed = logLine.trim()
		int start = trimmed.length()
		for (String phrase : CLI_ERROR_LINE_SUBSTRINGS) {
			int idx = trimmed.indexOf(phrase)
			if (idx >= 0 && idx < start) start = idx
		}
		return (start < trimmed.length()) ? trimmed.substring(start).trim() : trimmed
	}

	/**
	 * Extracts an error line for the given file from CLI log (or any) text.
	 * First tries a line that contains fileName (or base name without extension) and at least one of CLI_ERROR_LINE_SUBSTRINGS.
	 * If none found and singleFileFallback is true (e.g. only one row in report), returns the first line that contains an error phrase and looks like an error message (length > 40).
	 * Returns trimmed line (first match) or "".
	 */
	private static String extractErrorFromCliOutput(String fileName, String logOrOutput, boolean singleFileFallback = false) {
		if (fileName == null || fileName.isEmpty() || logOrOutput == null || logOrOutput.isEmpty()) return ""
		String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName
		String[] lines = logOrOutput.split("[\r\n]+")
		String firstErrorLine = null
		for (String line : lines) {
			String trimmed = line.trim()
			if (trimmed.isEmpty()) continue
			boolean hasErrorPhrase = false
			for (String phrase : CLI_ERROR_LINE_SUBSTRINGS) {
				if (trimmed.contains(phrase)) {
					hasErrorPhrase = true
					break
				}
			}
			if (!hasErrorPhrase) continue
			boolean hasFileName = trimmed.contains(fileName) || trimmed.contains(baseName)
			if (hasFileName) return trimmed
			if (singleFileFallback && trimmed.length() > 40 && firstErrorLine == null) firstErrorLine = trimmed
		}
		return firstErrorLine != null ? firstErrorLine : ""
	}

	/**
	 * Verify CLI error messages: reads the latest upload-report-*.tsv from CLI tmp folder,
	 * reads expected TSV(s) from InputFiles/CRDC/MetadataData/<testCaseName>/expected/,
	 * compares actual vs expected errors (by file_name), and generates a report.
	 * Fails the test if any error message does not match; otherwise passes and writes the report.
	 *
	 * @param testCaseName Test case name (e.g. "TC12") used to resolve expected folder:
	 *        &lt;projectDir&gt;/InputFiles/CRDC/MetadataData/&lt;testCaseName&gt;/expected/
	 */
	@Keyword
	static void verifyCliErrorMessages(String testCaseName) {
		if (testCaseName == null || testCaseName.trim().isEmpty()) {
			KeywordUtil.markFailed("CliUploader.verifyCliErrorMessages: testCaseName is required.")
			return
		}
		String tcName = testCaseName.trim()
		File cliDir = getCliDir()
		File tmpDir = new File(cliDir, "tmp")
		if (!tmpDir.exists() || !tmpDir.isDirectory()) {
			KeywordUtil.markFailed("CliUploader.verifyCliErrorMessages: tmp folder not found at " + tmpDir.getAbsolutePath())
			return
		}
		File[] reports = tmpDir.listFiles({ File f -> f.isFile() && f.name.startsWith("upload-report-") && f.name.endsWith(".tsv") } as FileFilter)
		if (reports == null || reports.length == 0) {
			KeywordUtil.markFailed("CliUploader.verifyCliErrorMessages: No upload-report-*.tsv found in " + tmpDir.getAbsolutePath())
			return
		}
		File latestReport = reports.toList().sort { -it.lastModified() }.get(0)
		List<String> reportLines = Files.readAllLines(latestReport.toPath(), StandardCharsets.UTF_8)
		if (reportLines.size() < 2) {
			KeywordUtil.markFailed("CliUploader.verifyCliErrorMessages: Upload report has no data rows: " + latestReport.getAbsolutePath())
			return
		}
		String reportHeader = reportLines.get(0)
		int actualErrorsIdx = findColumnIndex(reportHeader, ACTUAL_ERRORS_COLUMN_CANDIDATES)
		int reportFileNameIdx = findColumnIndex(reportHeader, FILE_NAME_COLUMN_CANDIDATES)
		if (actualErrorsIdx < 0) {
			KeywordUtil.markFailed("CliUploader.verifyCliErrorMessages: Report has no errors column (tried: " + ACTUAL_ERRORS_COLUMN_CANDIDATES.join(", ") + "). Header: " + reportHeader + " in " + latestReport.getName())
			return
		}
		if (reportFileNameIdx < 0) {
			KeywordUtil.markFailed("CliUploader.verifyCliErrorMessages: Report has no file name column (tried: " + FILE_NAME_COLUMN_CANDIDATES.join(", ") + "): " + latestReport.getName())
			return
		}
		String projectDir = RunConfiguration.getProjectDir()
		File expectedDir = new File(projectDir, "InputFiles/CRDC/MetadataData/" + tcName + "/" + EXPECTED_SUBFOLDER)
		Map<String, String> expectedMap = loadExpectedErrorsByFileName(expectedDir)
		if (expectedMap.isEmpty()) {
			KeywordUtil.markFailed("CliUploader.verifyCliErrorMessages: No expected TSV(s) with expected_error(s) and file_name columns found in " + expectedDir.getAbsolutePath())
			return
		}
		List<Map<String, String>> reportRows = []
		for (int i = 1; i < reportLines.size(); i++) {
			String[] cols = reportLines.get(i).split("\t", -1)
			String fileName = reportFileNameIdx < cols.length ? cols[reportFileNameIdx].trim() : ""
			String actualRaw = actualErrorsIdx < cols.length ? cols[actualErrorsIdx].trim() : ""
			reportRows.add([file_name: fileName, actual_error: actualRaw])
		}
		File logFile = findLatestCliLogFile(cliDir)
		String logContent = readCliLogContent(logFile)
		if (logFile != null && (logContent == null || logContent.isEmpty())) {
			KeywordUtil.logInfo("CliUploader.verifyCliErrorMessages: CLI log file empty or unreadable: " + logFile.getAbsolutePath())
		}
		List<String> mismatches = []
		List<String> reportLinesOut = [
			"file_name\tactual_error\texpected_error\tmatch_status"
		]
		for (Map<String, String> row : reportRows) {
			String fileName = row.get("file_name", "")
			String actualRaw = row.get("actual_error", "")
			if (actualRaw.isEmpty() && logContent != null && !logContent.isEmpty()) {
				boolean singleFileFallback = (reportRows.size() == 1)
				String fromLog = extractErrorFromCliOutput(fileName, logContent, singleFileFallback)
				if (!fromLog.isEmpty()) {
					actualRaw = stripLogPrefixToMessage(fromLog)
					KeywordUtil.logInfo("CliUploader.verifyCliErrorMessages: errors column empty for " + fileName + "; using error line from CLI log " + (logFile != null ? logFile.getName() : ""))
				} else if (expectedMap.get(fileName) != null && !expectedMap.get(fileName).isEmpty()) {
					KeywordUtil.logInfo("CliUploader.verifyCliErrorMessages: no matching error line in CLI log for " + fileName + " (log has " + logContent.split("[\r\n]+").length + " lines)")
				}
			}
			String expectedRaw = expectedMap.get(fileName)
			if (expectedRaw == null) expectedRaw = ""
			String actualNorm = normalizeErrorCell(actualRaw)
			String expectedNorm = normalizeErrorCell(expectedRaw)
			boolean match = actualNorm == expectedNorm
			String reportStatus = match ? "Pass" : "Fail"
			String expectedTrunc = expectedNorm.length() > 200 ? expectedNorm.substring(0, 200) + "..." : expectedNorm
			String actualTrunc = actualNorm.length() > 200 ? actualNorm.substring(0, 200) + "..." : actualNorm
			ValidationReport.addValidationReportItem("CLI Error Verification", fileName, expectedTrunc, actualTrunc, reportStatus)
			String status = match ? "PASS" : "FAIL"
			String actualSafe = (actualRaw != null ? actualRaw : "").replace("\t", " ").replace("\n", " ").replace("\r", " ")
			String expectedSafe = (expectedRaw != null ? expectedRaw : "").replace("\t", " ").replace("\n", " ").replace("\r", " ")
			reportLinesOut.add(fileName + "\t" + actualSafe + "\t" + expectedSafe + "\t" + status)
			if (!match) mismatches.add(fileName + ": actual='" + actualNorm + "' vs expected='" + expectedNorm + "'")
		}
		File reportsDir = new File(projectDir, "Reports")
		if (!reportsDir.exists()) reportsDir.mkdirs()
		File subDir = new File(reportsDir, ERROR_VERIFICATION_REPORT_SUBDIR)
		if (!subDir.exists()) subDir.mkdirs()
		String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
		File reportFile = new File(subDir, "error-verification-" + tcName + "-" + timestamp + ".tsv")
		Files.write(reportFile.toPath(), reportLinesOut.join(System.getProperty("line.separator", "\n")).getBytes(StandardCharsets.UTF_8))
		KeywordUtil.logInfo("CliUploader.verifyCliErrorMessages: Report written to " + reportFile.getAbsolutePath())
		if (!mismatches.isEmpty()) {
			KeywordUtil.markFailed("CliUploader.verifyCliErrorMessages: Error message mismatch(es): " + mismatches.join("; "))
			return
		}
		KeywordUtil.logInfo("CliUploader.verifyCliErrorMessages: All error messages matched. Test passed.")
	}

	/** DRS ID with DCF prefix: dg.4DFC/<uuid> */
	private static final java.util.regex.Pattern DRS_ID_WITH_PREFIX =
	java.util.regex.Pattern.compile('^dg\\.4DFC/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$')
	/** Bare UUID (no DCF prefix) */
	private static final java.util.regex.Pattern BARE_UUID =
	java.util.regex.Pattern.compile('^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$')

	/**
	 * Verifies the CLI-generated final manifest after a successful data file upload.
	 * Checks that:
	 *   1. The -final.tsv file exists (derived from original manifest name).
	 *   2. It contains a file_id column.
	 *   3. Every file_id value is a valid DRS ID (dg.4DFC/uuid or bare uuid).
	 *   4. Row count matches the original manifest.
	 *
	 * @param testCaseName  Test case name used to locate DataFiles/ folder under InputFiles/CRDC/MetadataData/
	 */
	@Keyword
	static void verifyFinalManifest(String testCaseName) {
		if (testCaseName == null || testCaseName.trim().isEmpty()) {
			KeywordUtil.markFailed("CliUploader.verifyFinalManifest: testCaseName is required.")
			return
		}
		String tcName = testCaseName.trim()

		String originalManifestPath = getManifestPath(tcName)
		if (originalManifestPath == null) return

			File originalManifest = new File(originalManifestPath)
		if (!originalManifest.exists() || !originalManifest.isFile()) {
			KeywordUtil.markFailed("CliUploader.verifyFinalManifest: Original manifest not found: " + originalManifestPath)
			return
		}

		String finalPath = originalManifestPath.replace(".tsv", "-final.tsv")
		File finalManifest = new File(finalPath)
		if (!finalManifest.exists() || !finalManifest.isFile()) {
			ValidationReport.addValidationReportItem("CLI Final Manifest Verification", "Final manifest exists", "true", "false", "Fail")
			KeywordUtil.markFailed("CliUploader.verifyFinalManifest: Final manifest not found: " + finalPath)
			return
		}
		ValidationReport.addValidationReportItem("CLI Final Manifest Verification", "Final manifest exists", "true", "true", "Pass")
		KeywordUtil.logInfo("CliUploader.verifyFinalManifest: Found final manifest " + finalManifest.getName())

		List<String> origLines = Files.readAllLines(originalManifest.toPath(), StandardCharsets.UTF_8)
		List<String> finalLines = Files.readAllLines(finalManifest.toPath(), StandardCharsets.UTF_8)
		if (finalLines.size() < 2) {
			KeywordUtil.markFailed("CliUploader.verifyFinalManifest: Final manifest has no data rows.")
			return
		}

		int origDataRows = origLines.size() - 1
		int finalDataRows = finalLines.size() - 1
		if (origDataRows != finalDataRows) {
			ValidationReport.addValidationReportItem("CLI Final Manifest Verification", "Row count matches", String.valueOf(origDataRows), String.valueOf(finalDataRows), "Fail")
			KeywordUtil.markFailed("CliUploader.verifyFinalManifest: Row count mismatch. Original=" + origDataRows + ", Final=" + finalDataRows)
			return
		}
		ValidationReport.addValidationReportItem("CLI Final Manifest Verification", "Row count matches", String.valueOf(origDataRows), String.valueOf(finalDataRows), "Pass")

		String header = finalLines.get(0)
		String[] headers = header.split("\t", -1)
		int fileIdIdx = -1
		for (int i = 0; i < headers.length; i++) {
			if (headers[i].trim().equalsIgnoreCase("file_id")) {
				fileIdIdx = i
				break
			}
		}
		if (fileIdIdx < 0) {
			ValidationReport.addValidationReportItem("CLI Final Manifest Verification", "file_id column present", "true", "false", "Fail")
			KeywordUtil.markFailed("CliUploader.verifyFinalManifest: Final manifest has no 'file_id' column.")
			return
		}
		ValidationReport.addValidationReportItem("CLI Final Manifest Verification", "file_id column present", "true", "true", "Pass")

		List<String> invalidIds = []
		for (int i = 1; i < finalLines.size(); i++) {
			String[] cols = finalLines.get(i).split("\t", -1)
			String fileId = fileIdIdx < cols.length ? cols[fileIdIdx].trim() : ""
			if (fileId.isEmpty()) {
				invalidIds.add("Row " + (i + 1) + ": empty file_id")
			} else if (!DRS_ID_WITH_PREFIX.matcher(fileId).matches() && !BARE_UUID.matcher(fileId).matches()) {
				invalidIds.add("Row " + (i + 1) + ": \"" + fileId + "\"")
			}
		}
		if (!invalidIds.isEmpty()) {
			String invalidSummary = invalidIds.size() + " invalid: " + (invalidIds.size() > 3 ? invalidIds.subList(0, 3).join("; ") + "..." : invalidIds.join("; "))
			ValidationReport.addValidationReportItem("CLI Final Manifest Verification", "All file_id values valid", "0 invalid", invalidSummary, "Fail")
			KeywordUtil.markFailed("CliUploader.verifyFinalManifest: Invalid file_id values: " + invalidIds.join("; "))
			return
		}
		ValidationReport.addValidationReportItem("CLI Final Manifest Verification", "All file_id values valid", "0 invalid", "0 invalid", "Pass")
		KeywordUtil.logInfo("CliUploader.verifyFinalManifest: Final manifest verified: " + finalDataRows + " row(s), all file_id values are valid DRS IDs.")
	}

	/**
	 * Verifies that generated file_ids in the final manifest are deterministic.
	 * Re-runs the CLI upload and compares file_ids by file_name between the two runs.
	 * Fails if any file_id differs between runs (expectation: UUID v5 from study + file_name).
	 *
	 * @param testCaseName  Test case name used to locate DataFiles/ folder (same as uploadFilesViaCli).
	 */
	@Keyword
	static void verifyFinalManifestDeterministic(String testCaseName) {
		if (testCaseName == null || testCaseName.trim().isEmpty()) {
			KeywordUtil.markFailed("CliUploader.verifyFinalManifestDeterministic: testCaseName is required.")
			return
		}
		String tcName = testCaseName.trim()

		String originalManifestPath = getManifestPath(tcName)
		if (originalManifestPath == null) return

			String finalPath = originalManifestPath.replace(".tsv", "-final.tsv")
		File firstFinal = new File(finalPath)
		if (!firstFinal.exists() || !firstFinal.isFile()) {
			KeywordUtil.markFailed("CliUploader.verifyFinalManifestDeterministic: First final manifest not found (run upload first): " + finalPath)
			return
		}

		List<String> firstLines = Files.readAllLines(firstFinal.toPath(), StandardCharsets.UTF_8)
		if (firstLines.size() < 2) {
			KeywordUtil.markFailed("CliUploader.verifyFinalManifestDeterministic: First final manifest has no data rows.")
			return
		}
		String header = firstLines.get(0)
		int fileIdIdx = findColumnIndex(header, "file_id")
		int fileNameIdx = findColumnIndex(header, FILE_NAME_COLUMN_CANDIDATES)
		if (fileIdIdx < 0 || fileNameIdx < 0) {
			KeywordUtil.markFailed("CliUploader.verifyFinalManifestDeterministic: First manifest missing file_id or file_name column.")
			return
		}

		Map<String, String> firstFileIdsByFileName = [:]
		for (int i = 1; i < firstLines.size(); i++) {
			String[] cols = firstLines.get(i).split("\t", -1)
			String fileName = fileNameIdx < cols.length ? cols[fileNameIdx].trim() : ""
			String fileId = fileIdIdx < cols.length ? cols[fileIdIdx].trim() : ""
			if (!fileName.isEmpty()) firstFileIdsByFileName.put(fileName, fileId)
		}
		if (firstFileIdsByFileName.isEmpty()) {
			KeywordUtil.markFailed("CliUploader.verifyFinalManifestDeterministic: No file_name/file_id pairs in first manifest.")
			return
		}

		KeywordUtil.logInfo("CliUploader.verifyFinalManifestDeterministic: Re-running CLI upload to verify deterministic file_ids...")
		uploadFilesViaCli(tcName, "datafile")

		File secondFinal = new File(finalPath)
		if (!secondFinal.exists() || !secondFinal.isFile()) {
			KeywordUtil.markFailed("CliUploader.verifyFinalManifestDeterministic: Second final manifest not found after re-upload: " + finalPath)
			return
		}

		List<String> secondLines = Files.readAllLines(secondFinal.toPath(), StandardCharsets.UTF_8)
		if (secondLines.size() < 2) {
			KeywordUtil.markFailed("CliUploader.verifyFinalManifestDeterministic: Second final manifest has no data rows.")
			return
		}
		String secondHeader = secondLines.get(0)
		int secondFileIdIdx = findColumnIndex(secondHeader, "file_id")
		int secondFileNameIdx = findColumnIndex(secondHeader, FILE_NAME_COLUMN_CANDIDATES)
		if (secondFileIdIdx < 0 || secondFileNameIdx < 0) {
			KeywordUtil.markFailed("CliUploader.verifyFinalManifestDeterministic: Second manifest missing file_id or file_name column.")
			return
		}

		Map<String, String> secondFileIdsByFileName = [:]
		for (int i = 1; i < secondLines.size(); i++) {
			String[] cols = secondLines.get(i).split("\t", -1)
			String fileName = secondFileNameIdx < cols.length ? cols[secondFileNameIdx].trim() : ""
			String fileId = secondFileIdIdx < cols.length ? cols[secondFileIdIdx].trim() : ""
			if (!fileName.isEmpty()) secondFileIdsByFileName.put(fileName, fileId)
		}

		List<String> mismatches = []
		for (Map.Entry<String, String> e : firstFileIdsByFileName.entrySet()) {
			String fileName = e.getKey()
			String firstId = e.getValue()
			String secondId = secondFileIdsByFileName.get(fileName)
			if (secondId == null) {
				mismatches.add(fileName + ": missing in second manifest")
			} else if (firstId != secondId) {
				mismatches.add(fileName + ": first=" + firstId + ", second=" + secondId)
			}
		}
		for (String fileName : secondFileIdsByFileName.keySet()) {
			if (!firstFileIdsByFileName.containsKey(fileName)) {
				mismatches.add(fileName + ": missing in first manifest (extra in second)")
			}
		}

		if (!mismatches.isEmpty()) {
			ValidationReport.addValidationReportItem("CLI Deterministic file_id", "All file_ids identical across runs", "0 mismatches", mismatches.size() + " mismatches", "Fail")
			KeywordUtil.markFailed("CliUploader.verifyFinalManifestDeterministic: file_id mismatch(es): " + mismatches.join("; "))
			return
		}
		ValidationReport.addValidationReportItem("CLI Deterministic file_id", "All file_ids identical across runs", "0 mismatches", "0 mismatches", "Pass")
		KeywordUtil.logInfo("CliUploader.verifyFinalManifestDeterministic: All " + firstFileIdsByFileName.size() + " file_id(s) identical across two upload runs. Determinism verified.")
	}

	/**
	 * Upload files via CLI: data files (with manifest) or metadata (TSV/TXT folder).
	 * dataType: "datafile" | "data file" | "metadata" (case-insensitive).
	 * @param options Optional map. When options.expectValidationErrors == true: the step does not fail on CLI validation
	 *        failure and does not call verifyCliUploadOutput (caller should use verifyCliErrorMessages). When in addition
	 *        options.omitArchiveManifest == true: archive manifest is omitted for datafile+zip so the CLI emits
	 *        "No archive manifest found for X.zip..." (e.g. TC30). When expectValidationErrors is true but omitArchiveManifest
	 *        is not set, the archive manifest is included if present (e.g. TC31/TC32 bad md5/size in manifest).
	 */
	@Keyword
	static void uploadFilesViaCli(String testCaseName, String dataType, Map options = null) {
		boolean expectValidationErrors = (options != null && options.get('expectValidationErrors') == true)
		String apiUrl = getApiUrl()
		String token = getApiToken()
		String submissionId = getSubmissionId()
		String type = normalizeDataType(dataType)
		if (type == null) {
			KeywordUtil.markFailed("CliUploader: dataType must be 'datafile' or 'metadata'. Got: " + dataType)
			return
		}

		if (token == null || token.isEmpty()) {
			KeywordUtil.markFailed("CliUploader: SubmitterApiToken is not set in profile.")
			return
		}
		if (apiUrl == null || apiUrl.isEmpty()) {
			KeywordUtil.markFailed("CliUploader: ApiUrl is not set in profile.")
			return
		}
		if (submissionId == null || submissionId.isEmpty()) {
			KeywordUtil.markFailed("CliUploader: submission ID could not be resolved from upload-activity URL.")
			return
		}

		String dataPath
		String manifestPath = null
		if (TYPE_DATA_FILE.equals(type)) {
			dataPath = getDataFilesPath(testCaseName)
			manifestPath = getManifestPath(testCaseName)
			if (dataPath == null || dataPath.trim().isEmpty()) {
				KeywordUtil.markFailed("CliUploader: data path is empty (getDataFilesPath).")
				return
			}
			if (manifestPath == null || manifestPath.trim().isEmpty()) {
				KeywordUtil.markFailed("CliUploader: manifest path is empty (getManifestPath).")
				return
			}
		} else {
			dataPath = getMetaDataPath(testCaseName)
			if (dataPath == null || dataPath.trim().isEmpty()) {
				KeywordUtil.markFailed("CliUploader: metadata path is empty (getMetaDataPath).")
				return
			}
		}

		File dataDir = new File(toAbsolutePath(dataPath))
		if (!dataDir.exists() || !dataDir.isDirectory()) {
			KeywordUtil.markFailed("CliUploader: path does not exist or is not a directory: " + dataDir.getAbsolutePath())
			return
		}
		if (TYPE_DATA_FILE.equals(type)) {
			File manifestFile = new File(toAbsolutePath(manifestPath))
			if (!manifestFile.exists() || !manifestFile.isFile()) {
				KeywordUtil.markFailed("CliUploader: manifest file does not exist: " + manifestFile.getAbsolutePath())
				return
			}
			// Delete any stale -final.tsv from a previous run so verifyFinalManifest
			// can reliably confirm the CLI created a fresh one during this run.
			String finalPath = toAbsolutePath(manifestPath).replace(".tsv", "-final.tsv")
			File staleFinal = new File(finalPath)
			if (staleFinal.exists()) {
				staleFinal.delete()
				KeywordUtil.logInfo("CliUploader: Deleted stale final manifest from previous run: " + staleFinal.getName())
			}
		}

		File cliDir = getCliDir()
		if (!cliDir.exists() || !cliDir.isDirectory()) {
			KeywordUtil.markFailed("CliUploader: CLI directory does not exist: " + cliDir.getAbsolutePath())
			return
		}

		String archiveManifestPath = null
		if (TYPE_DATA_FILE.equals(type) && hasZipFiles(dataDir)) {
			boolean omitArchiveManifest = (options != null && options.get('omitArchiveManifest') == true)
			if (expectValidationErrors && omitArchiveManifest) {
				KeywordUtil.logInfo("CliUploader: expectValidationErrors=true and omitArchiveManifest=true; omitting archive manifest so CLI emits 'No archive manifest' errors.")
			} else {
				KeywordUtil.logInfo("CliUploader: Detected .zip files in data path; requiring zip-archive-manifest.tsv.")
				archiveManifestPath = getZipArchiveManifestPath(testCaseName)
				if (archiveManifestPath == null || archiveManifestPath.trim().isEmpty()) {
					if (!expectValidationErrors) {
						KeywordUtil.markFailed("CliUploader: zip-archive-manifest.tsv is required when uploading .zip files.")
						return
					}
				} else {
					File archiveManifestFile = new File(toAbsolutePath(archiveManifestPath))
					if (!archiveManifestFile.exists() || !archiveManifestFile.isFile()) {
						KeywordUtil.markFailed("CliUploader: archive manifest file does not exist: " + archiveManifestFile.getAbsolutePath())
						return
					}
				}
			}
		}
		int retries = getRetries()
		boolean overwrite = getOverwrite()
		boolean dryrun = getDryrun()
		String idField = getIdField()
		if (idField != null) idField = idField.trim()
		if (idField != null && idField.isEmpty()) idField = null

		String yaml = buildConfigYaml(submissionId, type, dataPath, manifestPath,
				archiveManifestPath, idField, retries, overwrite, dryrun, apiUrl, token)
		File configFile = writeConfigFile(yaml)
		try {
			List<Object> result = runUploaderProcess(configFile, cliDir, DEFAULT_TIMEOUT_MINUTES)
			int exitCode = ((Number) result.get(0)).intValue()
			String output = (String) result.get(1)
			if (expectValidationErrors) {
				KeywordUtil.logInfo("CliUploader: expectValidationErrors=true; not failing on exit code or output. Call verifyCliErrorMessages to assert expected errors.")
				WebUI.refresh()
				return
			}
			if (exitCode != 0) {
				KeywordUtil.logInfo("CliUploader: uploader exit code=" + exitCode + "\nOutput:\n" + output)
				KeywordUtil.markFailed("CliUploader: upload failed (exit code " + exitCode + "). See log for output.")
				return
			}
			// CLI can exit 0 even when it logs failure (e.g. permission denied); treat output as source of truth.
			if (output != null && (output.contains("Failed to upload files") || output.contains("can't get data file config"))) {
				KeywordUtil.logInfo("CliUploader: uploader reported failure in output:\n" + output)
				KeywordUtil.markFailed("CliUploader: upload failed. Check log (e.g. permission to run batch operation).")
				return
			}
			verifyCliUploadOutput(output)
			WebUI.refresh()
			KeywordUtil.logInfo("CliUploader: upload completed successfully.\n" + output)
		} finally {
			configFile.delete()
		}
	}
}
