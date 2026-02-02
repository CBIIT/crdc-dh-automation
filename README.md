# Commons\_Automation

This repository contains automated test scripts for **Data Commons** applications. It provides a cross-language automation framework designed to validate data accuracy and consistency between UI and backend reference files using **Groovy**, **Java**, **Python**, and **Selenium**.

---

## Overview

The framework automates the comparison of:

* **UI data** extracted via Katalon Studio and Selenium.
* **Reference data** loaded from TSV files using Python’s pandas library.

### Execution Flow

1. **Query File Input**

   * Test execution begins by reading an Excel file containing query definitions.

2. **UI Interaction**

   * Filters are applied using Selenium-based actions.
   * Resulting UI data is captured and saved to an output Excel file.

3. **Reference Data Load**

   * TSV source files are read into pandas DataFrames.
   * Data is processed and transformed according to the query schema.
   * Output is saved to a second Excel file.

4. **Data Comparison**

   * The two result sets are compared.
   * A test passes if all data matches; discrepancies result in test failure.

---

## 📁 Project Structure

| Path                     | Description                                    |
| ------------------------ | ---------------------------------------------- |
| `.settings/`             | IDE and project configuration settings         |
| `Drivers/`               | Web drivers for browser automation             |
| `Include/config/`        | Environment and test configuration files       |
| `InputFiles/`            | Source files (Query, TSV, CSV, Excel)                 |
| `Keywords/utilities/`    | Custom Groovy keywords for automation tasks    |
| `Libs/internal/`         | Internal support libraries                     |
| `Object Repository/`     | UI object definitions for Selenium             |
| `Plugins/`               | Katalon plugin components                      |
| `Profiles/`              | Environment profiles (e.g., QA, Stage)         |
| `PythonFiles/`           | Python scripts for data processing and merging |
| `Scripts/`               | Core automation logic                          |
| `Test Cases/`            | Test implementations using Groovy/Java         |
| `Test Listeners/`        | Hooks for test lifecycle events                |
| `Test Suites/`           | Collections of related test cases              |
| `jenkins/`               | Jenkins integration scripts                    |
| `requirements.txt`       | Python dependency list                         |
| `Commons_Automation.prj` | Katalon Studio project descriptor              |

---

## Getting Started

### Prerequisites

* [Katalon Studio](https://www.katalon.com/)
* [Python 3.x](https://www.python.org/)
* Access to shared configuration profiles (via SharePoint)

