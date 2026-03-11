# Data Explorer Verification – Final Plan

## Goal

- Add **Keywords/utilities/DataExplorer.groovy** (class name `DataExplorer`) with a function that accepts **one or more node types** and verifies the Data Explorer study metadata view for each.
- **Do not use TestRunner.** DataExplorer is a standalone class using WebUIUtils, findTestObject, DriverFactory, and ValidationReport directly.
- Invoke the function from **TC201_CRDCDH-3026-VerifyDataExplorerMetadataView** and keep using the existing **report generator** (ValidationReport).

---

## Implementation constraints

- **Do not change existing functions or test cases.** Reuse existing keywords (WebUIUtils, ValidationReport, DataSubmissions patterns) **without modifying them**. Only add new code in the new file and in the TC201 script; no edits to other Groovy keywords or other test scripts.
- **Do not create unnecessary helper functions.** Add helpers only when clearly needed (e.g. repeated logic that would otherwise be duplicated). When a helper is added, implement it professionally with clear comments (purpose, parameters, return value, usage).
- **No impact on existing behavior.** The new DataExplorer class and TC201 changes must not affect any other test cases or existing keyword behavior.

---

## References for implementation

When implementing, use these for deeper understanding of the UI and backend:

- **[.cursor/rules/crdc-datahub-ui-reference.mdc](.cursor/rules/crdc-datahub-ui-reference.mdc)** — CRDC Data Hub UI (crdc-datahub-ui): routes (`/data-explorer`, `/data-explorer/:studyId?`), tech stack (React, MUI, Apollo), DOM strategy (`data-testid` preferred), and AILocators layout (`DataExplorer/` under Object Repository). Use for exact component structure, testids, and navbar/filter locators.
- **[.cursor/rules/crdc-datahub-backend-reference.mdc](.cursor/rules/crdc-datahub-backend-reference.mdc)** — CRDC Data Hub backend: Data Explorer GraphQL queries (`listReleasedStudies`, `listReleasedDataRecords` with `studyID!`, `nodeType!`, `dataCommonsDisplayName!`), submission lifecycle (Released status), and how released data is served. Use to align expected TSV data with what the backend exposes after release.

---

## Flow (from screenshots)

1. **List page** (`/data-explorer?perPage=10`): Study filter, dbGaP ID filter, Data Commons dropdown, table (Study Abbreviation, Study Name, Data Commons, dbGaP ID). User clicks a study (e.g. "autoQA") to open the study view.
2. **Study view** (`/data-explorer/{studyId}`): "Node Type" dropdown (program, study, file, consent group, participant, pdx, sample); metadata table with columns that depend on the selected node type. For each requested node type: select it, then verify the metadata table (structure and content vs. expected TSVs).

---

## Existing pieces to reuse

- **Reuse only; do not update.** Call or mirror behavior of existing keywords and patterns without changing their code.

| Piece | Use |
|-------|-----|
| **WebUIUtils** | `clickTab()` for Nav; `verifyElementDisplayed`, `verifyElementText` for container/table. Table: use DriverFactory + By.cssSelector for `generic-table-header-*` and `generic-table-row` (same pattern as WebUIUtils; no need to extend it). |
| **ValidationReport** | `addValidationReportItem(section, label, expected, actual, status)` for every check; test case calls `generateValidationReport()` at the end. |
| **DataSubmissions** | Click dropdown then `driver.findElement(By.xpath("//li[normalize-space(text())='" + node + "']")).click()` for Node Type. **verifyDataViewMatchesMetadata**: reuse same TSV parse and table-vs-TSV compare pattern for Data Explorer. |

---

## Existing locators (Data Explorer)

- **List:** CRDC/NavBar/DataExplorer-Tab, CRDC/AILocators/DataExplorer/List/Filters/StudyName-TxtBx, DbGaPID-TxtBx, DataCommons-Ddn, Filters-Container, ResetFilters-Btn.
- **Study view:** CRDC/AILocators/DataExplorer/StudyView/Container (`data-testid='study-view-container'`), ColumnVisibility-Btn.
- **Generic table:** `data-testid='generic-table'`, `generic-table-header-*`, `generic-table-row`.

**Locators you may need to provide:** Node Type dropdown on the Data Explorer study page (e.g. CRDC/AILocators/DataExplorer/StudyView/NodeType-Ddn if different from Data View). Optionally study link from list (or we use first generic-table-row).

---

## Recommended design

### 1. New file: Keywords/utilities/DataExplorer.groovy (class DataExplorer)

- **Do not extend TestRunner.** Standalone class; use `WebUIUtils.clickTab()`, `findTestObject()`, `DriverFactory.getWebDriver()`, and `ValidationReport` directly.
- **Report section:** Use a fixed section name (e.g. `"Data Explorer"`) and include the node type in the label (e.g. `"Node type 'program' – row count"`) so the HTML report stays clear.

### 2. Main keyword: verifyDataExplorerMetadataView(String... nodeTypes)

- **Parameters:** Varargs `nodeTypes` (e.g. `'program'`, `'study'`, `'file'`) so TC201 can call e.g. `verifyDataExplorerMetadataView('program', 'study')` or a single `'program'`.
- **Assumption:** Caller has already navigated to Data Explorer and opened a study (current page is the study view with Node Type dropdown and metadata table). Optionally a helper can do "list → open first study" (see below).
- **Per node type:**
  1. Select the node type from the Node Type dropdown (click dropdown, then click `//li[normalize-space(text())=...]`).
  2. Wait for the metadata table to load (e.g. wait for study-view-container and generic-table or table body).
  3. Verify: container/table visible, table has headers (`generic-table-header-*`), and at least one data row (`generic-table-row`) or "no data" message for empty node types.
  4. **Read table from UI** (headers + rows, cell text per column; same pattern as WebUIUtils and DataSubmissions.verifyDataViewMatchesMetadata).
  5. **Load expected from TSV** (uploaded/released input files — see below) and compare row count and cell values; report each check via `ValidationReport.addValidationReportItem`.
- **WebUIUtils:** Use `WebUIUtils.clickTab()` for navigation and `WebUIUtils.verifyElementDisplayed(findTestObject(...))` where it fits. For table structure use DriverFactory + By.cssSelector for generic-table, without extending any class.

### 3. How we verify the table (content vs. expected)

- After each node type is selected we **read the table from the UI**: headers via `generic-table-header-*`, rows via `generic-table-row`, cell text per column (same pattern as WebUIUtils and DataSubmissions.verifyDataViewMatchesMetadata).
- **Expected data:** Use the **input files that were uploaded and released**. Expected rows/columns for each node type come from the **same metadata TSVs** used in the submission that was uploaded and released (e.g. `InputFiles/CRDC/MetadataData/<tcName>/Metadata/program.tsv`, `study.tsv`, or TSVs under `InputFiles/CRDC/MetadataData/<tcName>/`). The test assumes a submission was created, metadata from that folder was uploaded, validated, and **released**, so Data Explorer shows that study; we then compare the Data Explorer table to those TSVs.
- **Comparison:** Reuse the approach from DataSubmissions.verifyDataViewMatchesMetadata: load expected from TSV (parse headers, skip `type` and columns with `'.'` if needed), read actual headers and rows from the UI, compare row count and cell values (with optional column mapping if UI header labels differ from TSV). Report each check via `ValidationReport.addValidationReportItem`.
- **Expected TSV location:** Prefer the same folder as submission metadata (e.g. `InputFiles/CRDC/MetadataData/TC201_CRDCDH-3026/` or `.../Metadata/` with program.tsv, study.tsv). Optionally a subfolder like `expected-DataExplorer/` if you want separate files. Recommendation: **use the same uploaded/released TSVs** so one set of files drives both upload and Data Explorer verification.

### 4. Optional helper: "ensure on study view"

- **Add only if needed.** Do not introduce this helper unless the test flow clearly requires "list → open first study" and that logic would otherwise be duplicated or hard to read in the script. Prefer having TC201 navigate and open the study explicitly (using existing locators) so the main keyword only verifies the study view.
- **If added:** Implement professionally with a short Javadoc-style comment (purpose, when to use, preconditions). No unnecessary helpers; only one that is clearly justified.

### 5. TC201 script changes (high level)

- After login and viewport:
  - Call `ValidationReport.clearValidationReportCollector()`.
  - Navigate to Data Explorer: `WebUIUtils.clickTab('CRDC/NavBar/DataExplorer-Tab')`.
  - Either: call the new "open first study" helper, or add steps (with your locators) to open a specific study.
  - Call `CustomKeywords.'utilities.DataExplorer.verifyDataExplorerMetadataView'('program', 'study')` (or whichever node types you want). The keyword will need the **tcName** (or expected TSV base path) to load expected TSVs — either pass it as an additional parameter (e.g. `verifyDataExplorerMetadataView('TC201_CRDCDH-3026', 'program', 'study')`) or derive from test case name in RunConfiguration.
  - Call `CustomKeywords.'utilities.ValidationReport.generateValidationReport'()`.
  - Then `WebUI.closeBrowser()`.

---

## Implementation summary

| Item | Action |
|------|--------|
| **New file** | `Keywords/utilities/DataExplorer.groovy` (class `DataExplorer`, **standalone — do not extend TestRunner**). |
| **Main API** | `verifyDataExplorerMetadataView(String... nodeTypes)` – for each node type: select in dropdown, wait for table, verify container + headers + row count; **compare table content to expected TSVs** (uploaded/released input files), log all checks to ValidationReport. Overload or add param for expected-data path/tcName if needed. |
| **Reuse** | WebUIUtils.clickTab, WebUIUtils.verifyElementDisplayed; ValidationReport.addValidationReportItem; DriverFactory + By for generic-table; option selection pattern from DataSubmissions (dropdown + li by text); TSV load/compare from DataSubmissions.verifyDataViewMatchesMetadata. **Reuse without changing existing code.** |
| **No changes to existing code** | **Do not modify** any existing keyword classes or test scripts other than TC201. Only add the new DataExplorer.groovy and update the TC201 script. Helpers only when necessary, with professional comments. |
| **Report** | All checks under section `"Data Explorer"`; TC201 calls `generateValidationReport()` at end. |
| **TC201** | Add clearValidationReportCollector, (optional) open-study step, call to `verifyDataExplorerMetadataView(...)`, then `generateValidationReport()`. |
| **Locators** | Use existing Data Explorer list and StudyView container; you provide Node Type dropdown for study view (and optionally study link from list) if different from existing objects. |

---

## Parameter shape

- **Varargs** `verifyDataExplorerMetadataView(String... nodeTypes)` for the node types to verify.
- If expected TSVs are keyed by test case name, add a first parameter (e.g. `tcName`) or derive from `RunConfiguration.getExecutionSourceName()` / test case name so the keyword knows which `InputFiles/CRDC/MetadataData/<tcName>/` folder to use.

Once you confirm and provide any missing locators (Node Type dropdown and, if needed, study link), implementation can follow this plan.
