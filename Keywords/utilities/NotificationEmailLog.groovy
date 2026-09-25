package utilities

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.List

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.testdata.TestDataFactory
import com.kms.katalon.core.util.KeywordUtil

import internal.GlobalVariable

/**
 * Append-only Excel checklist for email-notification helper tests.
 * logEmails does not open inboxes. verifyNotificationEmails runs the Gmail judge.
 */
class NotificationEmailLog {

	private static final String RELATIVE_OUTPUT_PATH = "/OutputFiles/NotificationEmail_Checklist.xlsx"
	private static final String SHEET_NAME = "Checklist"
	private static final String[] HEADERS = [
		"Timestamp",
		"Actor",
		"Action",
		"Entity type",
		"ID",
		"Name",
		"Expected subject",
		"Expected recipient",
		"Notes",
		"Result",
		"Environment"
	] as String[]
	private static final String ENV_PREFIX_NOTE = "Subject is prefixed with [Environment] (QA, QA2, or STAGE)."
	private static final String CATALOG_DATA_FILE = "CRDC/Pbac/Permissions"

	/**
	 * Append one checklist row. Result is always left blank.
	 */
	@Keyword
	static void appendRow(String timestamp, String actor, String action, String entityType, String id, String name,
			String expectedSubject, String expectedRecipient, String notes) {
		Utils.createDirctory("OutputFiles")
		String outputPath = RunConfiguration.getProjectDir() + RELATIVE_OUTPUT_PATH
		File file = new File(outputPath)
		Workbook workbook
		if (file.exists()) {
			FileInputStream fis = new FileInputStream(file)
			workbook = new XSSFWorkbook(fis)
			fis.close()
		} else {
			workbook = new XSSFWorkbook()
		}
		try {
			Sheet sheet = workbook.getSheet(SHEET_NAME)
			if (sheet == null) {
				sheet = workbook.createSheet(SHEET_NAME)
				writeHeader(workbook, sheet)
			}
			int nextRow = Math.max(sheet.getLastRowNum() + 1, 1)
			Row row = sheet.createRow(nextRow)
			String[] values = [
				timestamp ?: "",
				actor ?: "",
				action ?: "",
				entityType ?: "",
				id ?: "",
				name ?: "",
				expectedSubject ?: "",
				expectedRecipient ?: "",
				notes ?: "",
				"",
				resolveEnvironment()
			] as String[]
			for (int i = 0; i < values.length; i++) {
				Cell cell = row.createCell(i)
				cell.setCellValue(values[i])
			}
			FileOutputStream fos = new FileOutputStream(file)
			workbook.write(fos)
			fos.close()
			KeywordUtil.logInfo("NotificationEmailLog: appended row " + nextRow + " to " + outputPath)
		} finally {
			workbook.close()
		}
	}

	/**
	 * After a UI action, write one To: checklist row for the matching Permissions Trigger.
	 * Actor is who clicked. toRole is who Gmail To: must contain (profile inbox).
	 */
	@Keyword
	static void logEmails(String trigger, String actor, String id, String name, String toRole) {
		if (!toRole?.toString()?.trim()) {
			KeywordUtil.markFailedAndStop("logEmails requires toRole")
			return
		}
		String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
		appendFromCatalog(trigger, timestamp, actor, id, name, toRole.trim())
	}

	/**
	 * Run VerifyNotificationEmails.py against today's checklist rows.
	 * Logs the script output. Stops the test if the process exits non-zero
	 * (any Fail, missing checklist, or script error).
	 */
	@Keyword
	static void verifyNotificationEmails() {
		String pythonExe = Utils.getPythonExecutablePath()
		String scriptPath = RunConfiguration.getProjectDir() + "/PythonFiles/CRDC/VerifyNotificationEmails.py"
		File script = new File(scriptPath)
		if (!script.exists()) {
			KeywordUtil.markFailedAndStop("VerifyNotificationEmails.py not found at " + scriptPath)
			return
		}
		KeywordUtil.logInfo("Running " + scriptPath + " with " + pythonExe)
		ProcessBuilder pb = new ProcessBuilder(pythonExe, scriptPath)
		pb.redirectErrorStream(true)
		pb.directory(new File(RunConfiguration.getProjectDir()))
		try {
			Process proc = pb.start()
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))
			StringBuilder output = new StringBuilder()
			String line
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n")
				KeywordUtil.logInfo(line)
			}
			int exitCode = proc.waitFor()
			if (exitCode != 0) {
				KeywordUtil.markFailedAndStop("VerifyNotificationEmails.py reported failures (exit " + exitCode + "):\n" + output.toString())
			} else {
				KeywordUtil.logInfo("VerifyNotificationEmails.py completed with no Fail rows")
			}
		} catch (Exception e) {
			KeywordUtil.markFailedAndStop("VerifyNotificationEmails.py failed to run: " + e.getMessage())
		}
	}

	/**
	 * Look up Trigger on Permissions email rows (pipe-separated tokens allowed) and
	 * append one checklist row. Blank Trigger cells are ignored. Entity type comes from Group
	 * (Submission Request Emails -> SR, Data Submission Emails -> DS).
	 */
	private static void appendFromCatalog(String trigger, String timestamp, String actor, String id, String name,
			String toRole) {
		def testData = TestDataFactory.findTestData(CATALOG_DATA_FILE)
		int rowCount = testData.getRowNumbers()
		boolean found = false
		for (int i = 1; i <= rowCount; i++) {
			String catalogTrigger = testData.getValue("Trigger", i)?.toString()?.trim() ?: ""
			if (!catalogTrigger) {
				continue
			}
			List<String> triggers = splitPacked(catalogTrigger)
			List<String> subjects = splitPacked(testData.getValue("Expected email subject", i)?.toString()?.trim() ?: "")
			if (triggers.size() != subjects.size()) {
				KeywordUtil.markFailedAndStop("Permissions catalog Trigger and Expected email subject token counts differ on row " + i)
				return
			}
			int tokenIndex = triggers.indexOf(trigger)
			if (tokenIndex < 0) {
				continue
			}
			found = true
			String entityType = entityTypeFromGroup(testData.getValue("Group", i)?.toString()?.trim() ?: "")
			appendRow(timestamp, actor, trigger, entityType, id, name, subjects[tokenIndex], emailForRole(toRole), ENV_PREFIX_NOTE)
		}
		if (!found) {
			KeywordUtil.markFailed("Permissions catalog has no email row for Trigger " + trigger)
		}
	}

	/**
	 * Split a catalog cell on |. No pipe means one token. Keeps empty tokens so a length mismatch fails.
	 */
	private static List<String> splitPacked(String value) {
		if (value == null || value.isEmpty()) {
			return []
		}
		return value.split("\\|", -1).collect { it.trim() }
	}

	/**
	 * SR vs DS for the checklist Entity type column, from the Permissions Group cell.
	 */
	private static String entityTypeFromGroup(String group) {
		String g = group?.toLowerCase() ?: ""
		if (g.contains("submission request")) {
			return "SR"
		}
		if (g.contains("data submission")) {
			return "DS"
		}
		return ""
	}

	/**
	 * Map logEmails toRole to the matching profile inbox
	 * (SubmitterEmail, FedleadEmail, DcpEmail, AdminEmail, or UserEmail).
	 */
	private static String emailForRole(String toRole) {
		String role = toRole?.toLowerCase() ?: ""
		if (role == "submitter") {
			return inbox(GlobalVariable.SubmitterEmail)
		}
		if (role == "fedlead") {
			return inbox(GlobalVariable.FedleadEmail)
		}
		if (role == "dcp") {
			return inbox(GlobalVariable.DcpEmail)
		}
		if (role == "admin") {
			return inbox(GlobalVariable.AdminEmail)
		}
		if (role == "user") {
			return inbox(GlobalVariable.UserEmail)
		}
		KeywordUtil.markFailedAndStop("Unknown toRole: " + toRole)
		return ""
	}

	/**
	 * Trim a GlobalVariable email to a string. Blank if the profile value is missing.
	 */
	private static String inbox(Object value) {
		return value?.toString()?.trim() ?: ""
	}

	/**
	 * Email subject prefix token: QA, QA2, or STAGE. hub-qa2 is checked before hub-qa.
	 */
	private static String resolveEnvironment() {
		String url = GlobalVariable.G_Urlname?.toString()?.toLowerCase() ?: ""
		if (url.contains("hub-qa2")) {
			return "QA2"
		}
		if (url.contains("hub-qa")) {
			return "QA"
		}
		if (url.contains("hub-stage")) {
			return "STAGE"
		}
		String profile = ""
		try {
			profile = RunConfiguration.getExecutionProfile()?.toString() ?: ""
		} catch (Exception ignored) {
			profile = ""
		}
		String lower = profile.toLowerCase()
		if (lower.contains("qa2")) {
			return "QA2"
		}
		if (lower.contains("qa")) {
			return "QA"
		}
		if (lower.contains("stage")) {
			return "STAGE"
		}
		return ""
	}

	/**
	 * Write the Checklist header row with bold grey styling. Used only when the sheet is created.
	 */
	private static void writeHeader(Workbook workbook, Sheet sheet) {
		Row header = sheet.createRow(0)
		CellStyle headerStyle = workbook.createCellStyle()
		Font headerFont = workbook.createFont()
		headerFont.setBold(true)
		headerStyle.setFont(headerFont)
		headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex())
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
		for (int i = 0; i < HEADERS.length; i++) {
			Cell cell = header.createCell(i)
			cell.setCellValue(HEADERS[i])
			cell.setCellStyle(headerStyle)
			sheet.setColumnWidth(i, 8000)
		}
	}
}
