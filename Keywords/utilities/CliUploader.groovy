package utilities

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * CRDC Data Hub CLI Uploader integration for Katalon.
 * Self-contained: builds config, runs the uploader binary, parses submission ID from URL.
 * Does not modify any existing keyword files.
 *
 * All inputs (submission ID, data folder, manifest path, token, etc.) are read from the
 * Katalon execution profile. Update profile variables as needed; no parameters required.
 *
 * Profile variables (add/update in Katalon execution profile):
 *   Required:
 *   - CRDC_API_TOKEN
 *   - CRDC_API_URL
 *   - CRDC_SUBMISSION_ID
 *   - CRDC_DATA_FOLDER_PATH
 *   - CRDC_MANIFEST_PATH 
 */
class CliUploader {

	private static final String DEFAULT_CLI_SUBDIR = "Tools/CRDC-CLI"
	private static final long DEFAULT_TIMEOUT_MINUTES = 30L


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

	/** Get CLI root directory (contains uploader binary). Default: projectDir/Tools/CRDC-CLI */
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
	 * Executes the CRDC CLI uploader using the provided config file.
	 * Validates the uploader binary, runs the process, captures combined output,
	 * waits up to the specified timeout, and returns [exitCode, outputText].
	 * Fails the test if the binary is missing or the process times out.
	 */

	private static List<Object> runUploaderProcess(File configFile, File workingDir, long timeoutMinutes) {
		String osName = System.getProperty("os.name", "").toLowerCase()
		String exeName = osName.contains("windows") ? "uploader.exe" : "uploader"
		File exe = new File(workingDir, exeName)
		if (!exe.exists() || !exe.canExecute()) {
			KeywordUtil.markFailed("CrdcCliUpload: uploader binary not found or not executable: " + exe.getAbsolutePath())
			return [-1, ""]
		}

		List<String> command = [
			exe.getAbsolutePath(),
			"--config",
			configFile.getAbsolutePath()
		]
		ProcessBuilder pb = new ProcessBuilder(command)
		pb.directory(workingDir)
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
	 * Upload data files to CRDC Data Hub using the CLI.
	 * All values are read from the execution profile (no parameters).
	 * Set in profile: CRDC_API_TOKEN, CRDC_API_URL, CRDC_SUBMISSION_ID, CRDC_DATA_FOLDER_PATH, CRDC_MANIFEST_PATH.
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
			KeywordUtil.markFailed("CCliUploader: CRDC_MANIFEST_PATH is not set in getManifestPath().")
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
			WebUI.refresh()
			KeywordUtil.logInfo("CrdcCliUpload: data file upload completed successfully.\n" + output)
		} finally {
			configFile.delete()
		}
	}
}
