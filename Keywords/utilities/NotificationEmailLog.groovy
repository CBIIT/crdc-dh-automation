package utilities

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat

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
 * Does not open or verify inboxes. Result is left blank for the Gmail verifier.
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
	 * After a UI action, write To: rows whose Trigger matches Permissions email rows.
	 * Action on the checklist is the Trigger. Actor is who clicked, not who receives.
	 */
	@Keyword
	static void logEmails(String trigger, String actor, String id, String name) {
		String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
		appendFromCatalog(trigger, timestamp, actor, id, name)
	}

	/**
	 * Look up Trigger on Permissions email rows and append one checklist row per match.
	 * Rows with a blank Trigger are ignored. Entity type comes from Group
	 * (Submission Request Emails -> SR, Data Submission Emails -> DS).
	 */
	private static void appendFromCatalog(String trigger, String timestamp, String actor, String id, String name) {
		def testData = TestDataFactory.findTestData(CATALOG_DATA_FILE)
		int rowCount = testData.getRowNumbers()
		boolean found = false
		for (int i = 1; i <= rowCount; i++) {
			String catalogTrigger = testData.getValue("Trigger", i)?.toString()?.trim() ?: ""
			if (!catalogTrigger || catalogTrigger != trigger) {
				continue
			}
			found = true
			String entityType = entityTypeFromGroup(testData.getValue("Group", i)?.toString()?.trim() ?: "")
			String subject = testData.getValue("Expected email subject", i)?.toString()?.trim() ?: ""
			String toRole = testData.getValue("To role", i)?.toString()?.trim() ?: ""
			appendRow(timestamp, actor, trigger, entityType, id, name, subject, emailForRole(toRole), ENV_PREFIX_NOTE)
		}
		if (!found) {
			KeywordUtil.markFailed("Permissions catalog has no email row for Trigger " + trigger)
		}
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
	 * Map Permissions To role to the matching profile inbox
	 * (SubmitterEmail, FedleadEmail, DcpEmail, or AdminEmail).
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
		KeywordUtil.markFailed("Unknown To role in Permissions catalog: " + toRole)
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
