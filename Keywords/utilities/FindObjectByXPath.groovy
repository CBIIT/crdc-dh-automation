package utilities

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.ObjectRepository

/**
 * Find test object in Object Repository by XPath, or create dynamic TestObject from XPath.
 * @param xpath - XPath string
 * @return TestObject (from repository if found, else dynamic)
 */
class FindObjectByXPath {

	@Keyword
	static TestObject findObject(String xpath) {
		if (!xpath) return null

		try {
			List<TestObject> allObjects = ObjectRepository.getTestObjects()
			for (TestObject obj : allObjects) {
				for (TestObjectProperty prop : obj.getProperties()) {
					if (prop.getName().equalsIgnoreCase("xpath") &&
							prop.getCondition().equals(ConditionType.EQUALS) &&
							prop.getValue().equals(xpath)) {
						return obj
					}
				}
			}
		} catch (Exception e) {
			// Fall through to create dynamic object
		}

		TestObject testObj = new TestObject()
		testObj.addProperty(new TestObjectProperty("xpath", ConditionType.EQUALS, xpath))
		return testObj
	}
}
