# CRDC Data Hub Automation

Automated test framework for the **Cancer Research Data Commons (CRDC) Data Hub**. This repository provides a Katalon Studio–based test suite for UI validation of CRDC workflows, including authentication, policy-based access control (PBAC), data submissions, submission requests, and validation reporting.

---

## Overview

The framework automates end-to-end validation of CRDC Data Hub functionality using **Katalon Studio**, **Groovy**, and **Selenium**. Test data is managed through Katalon data files and Excel/TSV inputs; optional Python utilities (e.g., OTP generation) are supported via the included Python scripts.

### Capabilities

- **Login** — Authentication flows, including OTP-based login via Python integration  
- **PBAC** — Policy-based access control scenarios and permission validation  
- **Data Submissions** — Data submission workflows and related UI validation  
- **Submission Request** — Submission request creation and validation (all fields and required-fields variants)  
- **Validation Reporting** — Validation results and mapping checks against reference data  


---

## Project Structure

| Path | Description |
| ---- | ----------- |
| `Commons_Automation.prj` | Katalon Studio project descriptor |
| `Data Files/` | Test data (CRDC: Login, PBAC, DataSubmissions, SubmissionRequest, ValidationResults, etc.) |
| `Include/config/` | Configuration (e.g., logging) |
| `InputFiles/` | Input assets (TSV, Excel, etc.) used by tests |
| `Keywords/utilities/` | Custom Groovy keywords (Login, Pbac, DataSubmissions, SubmissionRequest, ValidationReport, Utils, etc.) |
| `Object Repository/` | UI object definitions for Selenium/Katalon |
| `Plugins/` | Katalon plugin components |
| `PythonFiles/CRDC/` | Python utilities (e.g., OTP generation for login) |
| `Scripts/CRDC/` | Groovy scripts for CRDC test logic |
| `Test Cases/CRDC/` | Test case definitions (Login, PBAC, DataSubmissions, SubmissionRequest) |
| `Test Listeners/` | Test lifecycle hooks (e.g., API_Listener, ICDC_Listener) |
| `Test Suites/` | Suite definitions (`.ts` and `.groovy`) |
| `jenkins/` | Jenkins pipeline and test schedule |
| `requirements.txt` | Python dependencies for utilities |

---

## Prerequisites

- **Katalon Studio** (compatible with project version; see `Commons_Automation.prj`)  
- **Python 3.x** — Required if using Python utilities (e.g., OTP generation)  
- **Browser drivers** — As required by Katalon (Chrome, Firefox, etc.)  
- Access to CRDC environments and any shared configuration (e.g., profiles, credentials) as used by your organization  

---

## Getting Started

1. **Clone the repository** and open the project in Katalon Studio via `Commons_Automation.prj`.  
2. **Configure the environment** using the appropriate profile and `Include/config` (and any shared config) as needed.  
3. **Install Python dependencies** (if using Python utilities):
   ```bash
   pip install -r requirements.txt
   ```
4. **Select a test suite** (e.g., CRDC_Smoke, CRDC_Regression) and run from Katalon Studio or via the command line/Gradle.  
5. **CI/CD** — Use the Jenkins pipeline and `jenkins/Test-Schedule.csv` for scheduled runs; adjust branch, suite, profile, and job parameters to match your Jenkins setup.
