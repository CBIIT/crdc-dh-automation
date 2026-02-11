/**
 * MongoDB query and verification - uses query from data file with dynamic parameters.
 * Connection details come from the selected Katalon profile (G_MongoUri, G_MongoDatabase).
 * Query template is in InputFiles/CRDC/DBQueries/MongoQueries.xlsx, key "released".
 */
CustomKeywords.'utilities.DatabaseUtils.connectFromGlobalVariable'()

// POC test

// Substitute with actual values for your test -- below values are for QA environment
Map params = [
	status       : "Deleted",
	submissionID : "79cca77d-f313-4ab5-9f02-eb259f12d43e",
	nodeID       : "GC-TREATMENT-20705900"
]

def doc = CustomKeywords.'utilities.DatabaseUtils.findRecordFromDataFile'(
	"Data Files/CRDC/DataSubmissions/DBQueries", "released", params)

CustomKeywords.'utilities.DatabaseUtils.verifyRecordReturned'(doc)

CustomKeywords.'utilities.DatabaseUtils.close'()

// After create, upload, validate, release & complete, then validate in Mongo, for example:
// CustomKeywords.'utilities.DataSubmissions.verifyRecordsInMongo'('TC09', 'released', 'Deleted')
