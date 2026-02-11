package utilities

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable
import java.io.FileFilter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
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


	private static String getApiToken() {
		return GlobalVariable.SubmitterApiToken
	}

	private static String getApiUrl() {
		return GlobalVariable.ApiUrl
	}

	private static String getSubmissionId() {
		return getSubmissionIdFromUploadActivityUrl();
	}

	private static String getDataFilesPath(String testName) {
		return RunConfiguration.getProjectDir() + "/InputFiles/CRDC/MetadataData/"+testName+"/DataFiles"
	}

	private static String getManifestPath(String manifestFolder) {
		return RunConfiguration.getProjectDir() + "/InputFiles/CRDC/MetadataData/"+manifestFolder+"/DataFiles/manifest.tsv"
	}

	private static String getArchiveManifestPath() {
		return safeGlobal("CRDC_ARCHIVE_MANIFEST_PATH", null)?.toString()?.trim()
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
	 * Builds and returns a formatted YAML configuration string used for
	 * data file submission via the CRDC submission CLI/API. 
	 * Constructs a fully structured YAML config block with submission settings
	 *
	 * @param submissionId         Unique submission identifier
	 * @param dataFolderPath       Path to the folder containing data files
	 * @param manifestPath         Path to the primary manifest file
	 * @param archiveManifestPath  (Optional) Path to archive manifest file
	 * @param idField              (Optional) Identifier field name used in submission
	 * @param retries              Number of retry attempts for submission
	 * @param overwrite            Whether existing files should be overwritten
	 * @param dryrun               Whether submission should run in dry-run mode
	 * @param apiUrl               API endpoint URL for submission
	 * @param token                Authentication token for API access
	 *
	 * @return String              Fully formatted YAML configuration content
	 */
	private static String buildConfigYaml(String submissionId, String dataFolderPath, String manifestPath,
			String archiveManifestPath, String idField, int retries, boolean overwrite, boolean dryrun,
			String apiUrl, String token) {
		String dataAbs = toAbsolutePath(dataFolderPath)
		String manifestAbs = toAbsolutePath(manifestPath)
		String archiveLine = (archiveManifestPath != null && !archiveManifestPath.isEmpty())
				? "    archive_manifest: " + toAbsolutePath(archiveManifestPath) + "\n"
				: ""
		String idFieldLine = (idField != null && !idField.isEmpty())
				? "    id-field: " + idField + "\n"
				: ""

		String safeToken = escapeYamlString(token)

		return """Config:
               api-url: ${apiUrl}
               token: ${safeToken}
               submission: ${submissionId}
               type: data file
               data: ${dataAbs}
               manifest: ${manifestAbs} ${archiveLine}${idFieldLine}    
               retries: ${retries}
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
			KeywordUtil.markFailed("CliUploader: CLI log verification failed: " + errors.join("; "))
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

	/**
	 * Extract submission ID from the current Data Hub upload-activity URL.
	 * Use this when you want to set CRDC_SUBMISSION_ID from the browser URL (e.g. in a script).
	 *
	 * @param url full URL (e.g. WebUI.getUrl())
	 * @return submission ID (UUID string) or null if not found
	 */
	@Keyword
	static String getSubmissionIdFromUploadActivityUrl() {
		String url = WebUI.getUrl()
		if (url == null || WebUI.getUrl().trim().isEmpty()) return null
		java.util.regex.Pattern p = ~/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/
		java.util.regex.Matcher m = p.matcher(url)
		return m.find() ? m.group(0) : null
	}

	/**
	 * Upload data files to CRDC Data Hub using the CLI (binary or Python source under Tools/crdc-datahub-cli-uploader-src).
	 * Uses GlobalVariable.ApiUrl and GlobalVariable.SubmitterApiToken; submission ID from current upload-activity URL;
	 * data and manifest paths derived from test case name. No extra profile variables required if defaults are used.
	 */
	@Keyword
	static void uploadDataFiles(String testCaseName) {
		String apiUrl = getApiUrl()
		String token = getApiToken()
		String submissionId = getSubmissionId()
		String dataFolderPath = getDataFilesPath(testCaseName)
		String manifestPath = getManifestPath(testCaseName)

		if (token == null || token.isEmpty()) {
			KeywordUtil.markFailed("CliUploader: CRDC_API_TOKEN is not set in profile.")
			return
		}
		if (apiUrl == null || apiUrl.isEmpty()) {
			KeywordUtil.markFailed("CliUploader: CRDC_API_URL is not set in profile.")
			return
		}
		if (submissionId == null || submissionId.isEmpty()) {
			KeywordUtil.markFailed("CliUploader: CRDC_SUBMISSION_ID is not set in getSubmissionId().")
			return
		}
		if (dataFolderPath == null || dataFolderPath.isEmpty()) {
			KeywordUtil.markFailed("CliUploader: CRDC_DATA_FILE_PATH is not set in getDataFilesPath().")
			return
		}
		if (manifestPath == null || manifestPath.isEmpty()) {
			KeywordUtil.markFailed("CliUploader: CRDC_MANIFEST_PATH is not set in getManifestPath().")
			return
		}

		File dataDir = new File(toAbsolutePath(dataFolderPath))
		if (!dataDir.exists() || !dataDir.isDirectory()) {
			KeywordUtil.markFailed("CliUploader: data folder does not exist or is not a directory: " + dataDir.getAbsolutePath())
			return
		}
		File manifestFile = new File(toAbsolutePath(manifestPath))
		if (!manifestFile.exists() || !manifestFile.isFile()) {
			KeywordUtil.markFailed("CliUpload: manifest file does not exist or is not a file: " + manifestFile.getAbsolutePath())
			return
		}

		File cliDir = getCliDir()
		if (!cliDir.exists() || !cliDir.isDirectory()) {
			KeywordUtil.markFailed("CliUploader: CLI directory does not exist: " + cliDir.getAbsolutePath())
			return
		}

		String archiveManifestPath = getArchiveManifestPath()
		if (archiveManifestPath != null) archiveManifestPath = archiveManifestPath.trim()
		if (archiveManifestPath != null && archiveManifestPath.isEmpty()) archiveManifestPath = null

		int retries = getRetries()
		boolean overwrite = getOverwrite()
		boolean dryrun = getDryrun()
		String idField = getIdField()
		if (idField != null) idField = idField.trim()
		if (idField != null && idField.isEmpty()) idField = null

		String yaml = buildConfigYaml(submissionId, dataFolderPath, manifestPath,
				archiveManifestPath, idField, retries, overwrite, dryrun, apiUrl, token)
		File configFile = writeConfigFile(yaml)
		try {
			List<Object> result = runUploaderProcess(configFile, cliDir, DEFAULT_TIMEOUT_MINUTES)
			int exitCode = ((Number) result.get(0)).intValue()
			String output = (String) result.get(1)
			if (exitCode != 0) {
				KeywordUtil.logInfo("CrdcCliUpload: uploader exit code=" + exitCode + "\nOutput:\n" + output)
				KeywordUtil.markFailed("CrdcCliUpload: data file upload failed (exit code " + exitCode + "). See log for output.")
				return
			}
			verifyCliUploadOutput(output)
			verifyCliUploadReport(cliDir, output)
			WebUI.refresh()
			KeywordUtil.logInfo("CrdcCliUpload: data file upload completed successfully.\n" + output)
		} finally {
			configFile.delete()
		}
	}
}
