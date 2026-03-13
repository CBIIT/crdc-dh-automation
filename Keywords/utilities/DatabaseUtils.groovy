package utilities

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.testdata.TestDataFactory
import com.kms.katalon.core.util.KeywordUtil
import java.io.File
import java.util.ArrayList
import groovy.json.JsonSlurper
import internal.GlobalVariable
import org.bson.Document
import org.bson.conversions.Bson
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.MongoCursor

/**
 * MongoDB connection and query utilities for CRDC Data Hub automation.
 * Use connectFromGlobalVariable() (requires G_MongoUri, G_MongoDatabase in profile),
 * then find/findOne/count/findRecordFromDataFile, and close() when done.
 */
public class DatabaseUtils {

	private static MongoClient mongoClient
	private static MongoDatabase database
	/** Collection name for data records in MongoDB. */
	private static final String DATA_RECORDS_COLLECTION = "dataRecords"

	/**
	 * Connect using connection URI and database name from GlobalVariable.
	 * Set GlobalVariable.G_MongoUri and GlobalVariable.G_MongoDatabase (e.g. in Katalon profile or execution settings).
	 */
	@Keyword
	public static void connectFromGlobalVariable() {
		String uri = GlobalVariable.G_MongoUri
		String dbName = GlobalVariable.G_MongoDatabase
		if (uri == null || uri.isEmpty() || dbName == null || dbName.isEmpty()) {
			KeywordUtil.markFailed("G_MongoUri and G_MongoDatabase must be set in GlobalVariable.")
			return
		}
		try {
			if (mongoClient != null) {
				KeywordUtil.logInfo("MongoDB client already connected; closing existing connection.")
				close()
			}
			mongoClient = MongoClients.create(uri)
			database = mongoClient.getDatabase(dbName)
			KeywordUtil.logInfo("Connected to MongoDB database: ${dbName}")
		} catch (Exception e) {
			KeywordUtil.markFailed("MongoDB connect failed: ${e.message}")
			throw e
		}
	}

	/**
	 * Convert a Groovy Map into a BSON Document for MongoDB queries.
	 * Recursively handles nested Maps (e.g. for $regex, $options, $in, etc.).
	 * MongoDB expects Bson/Document; Groovy Maps from JSON or literals need this conversion.
	 * Example: [name: [$regex: 'auto-test', $options: 'i']] becomes a valid BSON filter.
	 */
	private static Document mapToDocument(Map map) {
		if (map == null) return new Document()
		Document doc = new Document()
		map.each { key, value ->
			if (value instanceof Map && !(value instanceof Document)) {
				doc.put(key.toString(), mapToDocument(value))
			} else {
				doc.put(key.toString(), value)
			}
		}
		return doc
	}

	/**
	 * Find multiple documents in a collection matching the filter.
	 * @param collectionName Collection name (e.g. "submissions")
	 * @param filter Map representing the query (e.g. [name: [$regex: '^auto-test', $options: 'i']])
	 * @return List of Document (empty list if none or not connected)
	 */
	@Keyword
	public static List<Document> find(String collectionName, Map filter = null) {
		if (database == null) {
			KeywordUtil.markFailed("Not connected to MongoDB. Call connectFromGlobalVariable() first.")
			return []
		}
		MongoCollection<Document> collection = database.getCollection(collectionName)
		Bson bsonFilter = filter != null ? mapToDocument(filter) : new Document()
		List<Document> results = []
		MongoCursor<Document> cursor = collection.find(bsonFilter).iterator()
		try {
			while (cursor.hasNext()) {
				results.add(cursor.next())
			}
		} finally {
			cursor.close()
		}
		KeywordUtil.logInfo("find(${collectionName}) returned ${results.size()} document(s)")
		return results
	}

	/**
	 * Find one document in a collection matching the filter, or null if none.
	 */
	@Keyword
	public static Document findOne(String collectionName, Map filter = null) {
		if (database == null) {
			KeywordUtil.markFailed("Not connected to MongoDB. Call connectFromGlobalVariable() first.")
			return null
		}
		MongoCollection<Document> collection = database.getCollection(collectionName)
		Bson bsonFilter = filter != null ? mapToDocument(filter) : new Document()
		Document doc = collection.find(bsonFilter).first()
		KeywordUtil.logInfo("findOne(${collectionName}) returned " + (doc != null ? "1 document" : "null"))
		return doc
	}

	/**
	 * Find one document using a query loaded from the data file by key, with dynamic parameter substitution.
	 * @param dataFilePath Data file path (e.g. "Data Files/CRDC/DataSubmissions/DBQueries")
	 * @param queryKey Key to look up in the data file (column "key")
	 * @param params Map of parameter names to values for {{paramName}} substitution in the query template
	 * @return Document or null if not found
	 */
	@Keyword
	public static Document findRecordFromDataFile(String dataFilePath, String queryKey, Map params) {
		if (database == null) {
			KeywordUtil.markFailed("Not connected to MongoDB. Call connectFromGlobalVariable() first.")
			return null
		}

		def testData = TestDataFactory.findTestData(dataFilePath)
		int rows = testData.getRowNumbers()

		String[] colNames = testData.getColumnNames()
		String keyCol = (colNames != null && colNames.length >= 1) ? colNames[0] : null
		String collectionCol = (colNames != null && colNames.length >= 2) ? colNames[1] : null
		String queryCol = (colNames != null && colNames.length >= 3) ? colNames[2] : null
		// Ensure column count
		if (keyCol == null || collectionCol == null || queryCol == null) {
			KeywordUtil.markFailed("DBQueries data file must have at least 3 columns: key, collection, query.")
			return null
		}
		// Find row where key matches queryKey
		String collectionName = null
		String queryTemplate = null
		for (int i = 1; i <= rows; i++) {
			String keyVal = testData.getValue(keyCol, i)?.toString()?.trim()
			if (queryKey != null && queryKey == keyVal) {
				collectionName = testData.getValue(collectionCol, i)?.toString()?.trim()
				queryTemplate = testData.getValue(queryCol, i)?.toString()
				break
			}
		}
		// Ensure key is found
		if (collectionName == null || queryTemplate == null || queryTemplate.isEmpty()) {
			KeywordUtil.markFailed("Query key '${queryKey}' not found in data file ${dataFilePath}.")
			return null
		}
		// Replace {{paramName}} placeholders with params
		String queryStr = queryTemplate
		if (params != null) {
			params.each { paramName, paramValue ->
				queryStr = queryStr.replace('{{' + paramName + '}}', paramValue != null ? paramValue.toString() : '')
			}
		}
		// Ensure unresolved placeholders
		if (queryStr.contains('{{') && queryStr.contains('}}')) {
			KeywordUtil.markFailed("Unresolved placeholder(s) in query template for key '${queryKey}'. Check params.")
			return null
		}
		// Parse JSON string to Map
		Map filterMap
		try {
			filterMap = new JsonSlurper().parseText(queryStr) as Map
		} catch (Exception e) {
			KeywordUtil.markFailed("Invalid JSON in query for key '${queryKey}': ${e.message}")
			return null
		}
		// Call findOne and return
		return findOne(collectionName, filterMap)
	}

	/**
	 * Verify that a document has a field with the expected value. Fails the test if missing or different.
	 * @param doc Document from findOne or findRecordFromDataFile
	 * @param fieldName Field name (e.g. "status")
	 * @param expectedValue Expected value (e.g. "Deleted")
	 */
	@Keyword
	public static void verifyRecordFieldValue(Document doc, String fieldName, Object expectedValue) {
		if (doc == null) {
			KeywordUtil.markFailed("Cannot verify field '${fieldName}': document is null.")
			return
		}
		Object actual = doc.get(fieldName)
		boolean actualNull = (actual == null)
		boolean expectedNull = (expectedValue == null)
		if (actualNull && !expectedNull) {
			KeywordUtil.markFailed("Field '${fieldName}' missing or null. Expected: ${expectedValue}")
		} else if (!actualNull && expectedNull) {
			KeywordUtil.markFailed("Field '${fieldName}' expected null but got: ${actual}")
		} else if (!actualNull && !expectedNull && !actual.toString().equals(expectedValue.toString())) {
			KeywordUtil.markFailed("Field '${fieldName}' mismatch. Expected: ${expectedValue}, Actual: ${actual}")
		} else {
			KeywordUtil.markPassed("Field '${fieldName}' verified: ${expectedValue}")
		}
	}

	/**
	 * Verify that at least one record was returned. Fails the test if null or empty.
	 * @param doc Document from findOne (or findRecordFromDataFile)
	 */
	@Keyword
	public static void verifyRecordReturned(Document doc) {
		if (doc == null) {
			KeywordUtil.markFailed("Expected at least one record to be returned, but got null.")
		} else {
			KeywordUtil.markPassed("Record verified: 1 document returned.")
		}
	}

	/**
	 * Verify that at least one record was returned. Fails the test if null or empty.
	 * @param results List of Document from find (or findFromDataFile)
	 */
	@Keyword
	public static void verifyRecordReturned(List<Document> results) {
		if (results == null || results.isEmpty()) {
			KeywordUtil.markFailed("Expected at least one record to be returned, but got " + (results == null ? "null" : "empty list") + ".")
		} else {
			KeywordUtil.markPassed("Record verified: ${results.size()} document(s) returned.")
		}
	}

	/**
	 * Count documents in a collection matching the filter.
	 * @param collectionName Collection name
	 * @param filter Optional filter Map; use null or [:] for all documents
	 * @return Count (0 if none or not connected)
	 */
	@Keyword
	public static long count(String collectionName, Map filter = null) {
		if (database == null) {
			KeywordUtil.markFailed("Not connected to MongoDB. Call connectFromGlobalVariable() first.")
			return 0
		}
		MongoCollection<Document> collection = database.getCollection(collectionName)
		Bson bsonFilter = filter != null ? mapToDocument(filter) : new Document()
		long n = collection.countDocuments(bsonFilter)
		KeywordUtil.logInfo("count(${collectionName}) = ${n}")
		return n
	}

	/**
	 * Run an aggregation pipeline on a collection.
	 * Each element of pipeline is a Map representing one stage (e.g. [$match: [...], $sort: [...]]).
	 * Converts each Map to BSON via mapToDocument and returns all result documents.
	 *
	 * @param collectionName Collection name (e.g. "release")
	 * @param pipeline List of Map, each a pipeline stage (e.g. [$match: [nodeType: "study"]], [$sort: [updatedAt: 1]])
	 * @return List of Document (empty list if none or not connected)
	 */
	@Keyword
	public static List<Document> aggregate(String collectionName, List<Map> pipeline) {
		if (database == null) {
			KeywordUtil.markFailed("Not connected to MongoDB. Call connectFromGlobalVariable() first.")
			return []
		}
		if (pipeline == null || pipeline.isEmpty()) {
			KeywordUtil.markFailed("aggregate: pipeline must not be null or empty.")
			return []
		}
		MongoCollection<Document> collection = database.getCollection(collectionName)
		List<Bson> stages = pipeline.collect { Map stage -> mapToDocument(stage) }
		List<Document> results = collection.aggregate(stages).into(new ArrayList<>())
		KeywordUtil.logInfo("aggregate(${collectionName}) returned ${results.size()} document(s)")
		return results
	}

	/**
	 * Run an aggregation pipeline loaded from a data file (e.g. Pipelines sheet) by key.
	 * Data file must have columns: key, collection, pipeline. The pipeline column is a JSON array string
	 * of pipeline stages (e.g. [{"$match":{...}},{"$sort":{...}}]).
	 *
	 * @param dataFilePath Data file path (e.g. "Data Files/CRDC/DataSubmissions/DBPipelines")
	 * @param pipelineKey Key to look up (e.g. "ReleaseStudies")
	 * @return List of Document, or null if data file / key not found or pipeline invalid (caller may fall back to built-in pipeline)
	 */
	@Keyword
	public static List<Document> aggregateFromDataFile(String dataFilePath, String pipelineKey) {
		if (database == null) {
			KeywordUtil.markFailed("Not connected to MongoDB. Call connectFromGlobalVariable() first.")
			return null
		}
		def testData
		try {
			testData = TestDataFactory.findTestData(dataFilePath)
		} catch (Exception e) {
			KeywordUtil.logInfo("aggregateFromDataFile: data file not found or invalid: ${dataFilePath}. ${e.message}")
			return null
		}
		int rows = testData.getRowNumbers()
		String[] colNames = testData.getColumnNames()
		String keyCol = (colNames != null && colNames.length >= 1) ? colNames[0] : null
		String collectionCol = (colNames != null && colNames.length >= 2) ? colNames[1] : null
		String pipelineCol = (colNames != null && colNames.length >= 3) ? colNames[2] : null
		if (keyCol == null || collectionCol == null || pipelineCol == null) {
			KeywordUtil.logInfo("aggregateFromDataFile: data file must have at least 3 columns: key, collection, pipeline.")
			return null
		}
		String collectionName = null
		String pipelineStr = null
		for (int i = 1; i <= rows; i++) {
			String keyVal = testData.getValue(keyCol, i)?.toString()?.trim()
			if (pipelineKey != null && pipelineKey == keyVal) {
				collectionName = testData.getValue(collectionCol, i)?.toString()?.trim()
				pipelineStr = testData.getValue(pipelineCol, i)?.toString()?.trim()
				break
			}
		}
		if (collectionName == null || pipelineStr == null || pipelineStr.isEmpty()) {
			KeywordUtil.logInfo("aggregateFromDataFile: pipeline key '${pipelineKey}' not found or pipeline empty in ${dataFilePath}.")
			return null
		}
		List<Map> pipeline
		try {
			def parsed = new JsonSlurper().parseText(pipelineStr)
			if (!(parsed instanceof List)) {
				KeywordUtil.logInfo("aggregateFromDataFile: pipeline for key '${pipelineKey}' must be a JSON array.")
				return null
			}
			pipeline = parsed.collect { it instanceof Map ? it : [:] }
		} catch (Exception e) {
			KeywordUtil.logInfo("aggregateFromDataFile: invalid JSON pipeline for key '${pipelineKey}': ${e.message}")
			return null
		}
		if (pipeline.isEmpty()) {
			KeywordUtil.logInfo("aggregateFromDataFile: pipeline for key '${pipelineKey}' is empty.")
			return null
		}
		return aggregate(collectionName, pipeline)
	}

	/**
	 * Verifies that the table total displayed in the UI (from pagination "X–Y of Z") matches the
	 * document count in a MongoDB collection. Reusable for Manage Users (users), Manage Programs
	 * (organization), Manage Studies (approvedStudies), or any other list page with pagination.
	 * Manages connection lifecycle (connect, verify, close).
	 *
	 * @param uiCount Total entry count from the UI (e.g. WebUIUtils.getTableTotalEntryCount()). If &lt; 0, the keyword fails and does not connect to MongoDB.
	 * @param collectionName MongoDB collection name (e.g. "users", "organization", "approvedStudies").
	 * @param filter Optional filter Map for the count query; use null or [:] for all documents.
	 * @param reportSection Optional section label for ValidationReport (default "MongoDB").
	 * @param reportLabel Optional label for the report item (default "Table vs &lt;collectionName&gt; collection").
	 */
	@Keyword
	public static void verifyTableCountMatchesMongoCollection(int uiCount, String collectionName, Map filter = null, String reportSection = "MongoDB", String reportLabel = null) {
		if (uiCount < 0) {
			KeywordUtil.markFailed("verifyTableCountMatchesMongoCollection: UI count is invalid (${uiCount}). Cannot compare to MongoDB.")
			return
		}
		if (collectionName == null || collectionName.isEmpty()) {
			KeywordUtil.markFailed("verifyTableCountMatchesMongoCollection: collectionName must not be null or empty.")
			return
		}
		String label = (reportLabel != null && !reportLabel.isEmpty()) ? reportLabel : "Table vs ${collectionName} collection"
		connectFromGlobalVariable()
		try {
			long mongoCount = count(collectionName, filter)
			String expectedStr = String.valueOf(mongoCount)
			String actualStr = String.valueOf(uiCount)
			boolean match = (uiCount == mongoCount)
			String status = match ? "Pass" : "Fail"
			ValidationReport.addValidationReportItem(reportSection, label, expectedStr, actualStr, status)
			if (!match) {
				KeywordUtil.markFailed("verifyTableCountMatchesMongoCollection: count mismatch for ${collectionName}. UI=${uiCount}, MongoDB=${mongoCount}")
			} else {
				KeywordUtil.logInfo("verifyTableCountMatchesMongoCollection: ${collectionName} count verified. UI and MongoDB both ${uiCount}")
			}
		} finally {
			close()
		}
	}

	/**
	 * Verifies that no records exist in the dataRecords collection for the given submission ID.
	 * Used for negative scenarios (e.g. failed essential validation, no data loaded).
	 * Manages connection lifecycle (connect, verify, close).
	 * @param submissionId Submission UUID (e.g. from DataSubmissions.getSubmissionIdFromUploadActivityUrl)
	 */
	@Keyword
	public static void verifyNoRecordsInMongoForSubmission() {
		String submissionId = DataSubmissions.getSubmissionIdFromUploadActivityUrl()
		if (submissionId == null || submissionId.isEmpty()) {
			KeywordUtil.markFailed("verifyNoRecordsInMongoForSubmission: submissionId must not be null or empty.")
			return
		}
		connectFromGlobalVariable()
		try {
			Map filter = [submissionID: submissionId]
			long count = count(DATA_RECORDS_COLLECTION, filter)
			String status = (count == 0) ? "Pass" : "Fail"
			ValidationReport.addValidationReportItem("MongoDB", "dataRecords count for submission " + submissionId, "0", String.valueOf(count), status)
			if (count > 0) {
				KeywordUtil.markFailed("verifyNoRecordsInMongoForSubmission: Expected 0 records in dataRecords for submission " + submissionId + ", but found " + count + ".")
			} else {
				KeywordUtil.logInfo("verifyNoRecordsInMongoForSubmission: No records in dataRecords for submission " + submissionId + " (as expected).")
			}
		} finally {
			close()
		}
	}

	/**
	 * Verifies that at least one record exists in the dataRecords collection for the given submission ID.
	 * Used for positive scenarios (e.g. successful upload, data loaded).
	 * Manages connection lifecycle (connect, verify, close).
	 * @param submissionId Submission UUID (e.g. from DataSubmissions.getSubmissionIdFromUploadActivityUrl)
	 */
	@Keyword
	public static void verifyRecordsExistInMongoForSubmission() {
		String submissionId = DataSubmissions.getSubmissionIdFromUploadActivityUrl()
		if (submissionId == null || submissionId.isEmpty()) {
			KeywordUtil.markFailed("verifyRecordsExistInMongoForSubmission: submissionId must not be null or empty.")
			return
		}
		connectFromGlobalVariable()
		try {
			Map filter = [submissionID: submissionId]
			long count = count(DATA_RECORDS_COLLECTION, filter)
			String status = (count > 0) ? "Pass" : "Fail"
			ValidationReport.addValidationReportItem("MongoDB", "dataRecords count for submission " + submissionId, "> 0", String.valueOf(count), status)
			if (count == 0) {
				KeywordUtil.markFailed("verifyRecordsExistInMongoForSubmission: Expected at least 1 record in dataRecords for submission " + submissionId + ", but found 0.")
			} else {
				KeywordUtil.logInfo("verifyRecordsExistInMongoForSubmission: Found " + count + " record(s) in dataRecords for submission " + submissionId + " (as expected).")
			}
		} finally {
			close()
		}
	}

	/**
	 * Verify records in MongoDB for each node from the given test case.
	 * Gets submissionID from DataSubmissions.getSubmissionIdFromUploadActivityUrl(), derives nodes from
	 * DataSubmissions.listTsvFilesAndNodesForTC(tcName), uses loadValidationResultsMappings to get the submitted-identifier
	 * column per node, reads metadata TSV to extract submitted IDs, then queries MongoDB for each
	 * (submissionID, nodeID) where nodeID is the Submitted Identifier value.
	 * @param tcName Test case name (e.g. TC09) - used to derive nodes and metadata paths from InputFiles/CRDC/MetadataData/&lt;tcName&gt;/
	 * @param queryKey Query key in DBQueries data file (e.g. "released")
	 * @param expectedStatus Expected value for the "status" field (e.g. "Deleted")
	 */
	@Keyword
	public static void verifyRecordsInMongo(String tcName, String queryKey, String expectedStatus) {
		String submissionID = DataSubmissions.getSubmissionIdFromUploadActivityUrl()
		if (submissionID == null || submissionID.isEmpty()) {
			KeywordUtil.markFailed("verifyRecordsInMongo: Could not get submissionID from Upload Activity URL.")
			return
		}
		List<List<String>> listResult = DataSubmissions.listTsvFilesAndNodesForTC(tcName)
		List<String> relativePaths = listResult.get(0)
		List<String> nodes = listResult.get(1)
		if (nodes.isEmpty()) {
			KeywordUtil.markFailed("verifyRecordsInMongo: No nodes found for tcName '${tcName}'.")
			return
		}
		Map<String, String> mappings = DataSubmissions.loadValidationResultsMappings()
		String baseDir = RunConfiguration.getProjectDir()
		connectFromGlobalVariable()
		List<String> errors = new ArrayList<>()
		String sectionName = "MongoDB"
		try {
			for (int i = 0; i < nodes.size(); i++) {
				String node = nodes.get(i)
				String relativePath = relativePaths.get(i)
				String submittedIdColumn = mappings.get(node)
				if (submittedIdColumn == null || submittedIdColumn.isEmpty()) {
					KeywordUtil.logInfo("[WARNING] verifyRecordsInMongo: no mapping for node '${node}' in ValidationResults-Mappings; skipping.")
					continue
				}
				String tsvPath = new File(baseDir, relativePath).absolutePath
				List<String> submittedIds = DataSubmissions.loadSubmittedIdentifiersFromTsv(tsvPath, submittedIdColumn)
				if (submittedIds.isEmpty()) {
					KeywordUtil.logInfo("[WARNING] verifyRecordsInMongo: no submitted identifiers for node '${node}' in ${relativePath}; skipping.")
					continue
				}
				for (String submittedId : submittedIds) {
					Map params = [status: expectedStatus, submissionID: submissionID, nodeID: submittedId]
					Document doc = findRecordFromDataFile("Data Files/CRDC/DataSubmissions/DBQueries", queryKey, params)
					String actualStatus = doc != null ? doc.get("status")?.toString() : null
					boolean recordOk = doc != null
					boolean statusOk = expectedStatus.equals(actualStatus)
					String status = (recordOk && statusOk) ? "Pass" : "Fail"
					String actualDisplay = actualStatus != null ? actualStatus : "(not found)"
					ValidationReport.addValidationReportItem(sectionName, "Submitted ID " + submittedId, expectedStatus, actualDisplay, status)
					if (status == "Fail") {
						errors.add("Submitted ID '${submittedId}': expected status '${expectedStatus}', actual '${actualDisplay}'")
					}
				}
			}
		} finally {
			close()
		}
		if (!errors.isEmpty()) {
			KeywordUtil.logInfo("--- MongoDB verification - " + errors.size() + " mismatch(es) ---")
			for (String err : errors) KeywordUtil.logInfo(err)
			KeywordUtil.markFailed("verifyRecordsInMongo: " + errors.size() + " record(s) failed. See ValidationReport and log above.")
		}
	}

	/**
	 * Close the MongoDB client and release resources.
	 */
	@Keyword
	public static void close() {
		if (mongoClient != null) {
			try {
				mongoClient.close()
				KeywordUtil.logInfo("MongoDB connection closed.")
			} catch (Exception e) {
				KeywordUtil.logInfo("Error closing MongoDB client: ${e.message}")
			}
			mongoClient = null
			database = null
		}
	}
}
