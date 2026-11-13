# CRDC Data Hub — environment seed checklist

Use this after a **DB wipe / migration** when Hub QA / QA2 / Stage is empty and Katalon (and Playwright) tests start failing until fixtures are recreated.

Tests **create most data submissions themselves**. They do **not** create Hub users, institutions, programs, or approved studies. Those must exist first.

Do **not** put passwords, TOTP secrets, API tokens, AWS keys, or Mongo URIs in this file. Those live in Katalon profiles (`Profiles/CRDC_QA.glbl`, `CRDC_QA2.glbl`, `CRDC_STAGE.glbl`) and GHA secrets.

---

## How to use

Work top to bottom. Check each box in Hub UI (or Admin APIs) for the target env (`G_Urlname`).

1. Institutions
2. Programs
3. Studies (Manage Studies by default; approved SR only when a test requires it)
4. Automation users, roles, study access, display names
5. Role permissions (`EnableAllPermissions`)
6. Pre-existing DS / Data Explorer seed
7. Email / Gmail / Login.gov (accounts exist; secrets in profile)
8. Env-specific IDs that must be updated in code after a wipe

Then run `Test Cases/CRDC/SuiteSetup/EnableAllPermissions` once, then smoke.

---

## 1. Institutions

Hub may load NIH institution names from a catalog. After a **full** wipe, confirm the dropdown is populated. Tests fail if these exact strings are missing.

| Institution | Why |
|---|---|
| **21MEDTECH LLC** | SR `InputFiles/CRDC/SubmissionRequest.xlsx` sheet `section-a` **row 2** — TC01 AllFields (`enterPrincipalInvestigatorInfo(2)` / PC / AC). If the Submitter profile already has a PI institution, AllFields **skips** that field, so the profile value must match this row (or leave PI institution empty so the test fills it). |
| **2WITECH SOLUTIONS LLC** | `section-a` row 3 (other SR cases that use that row) |
| **3-C INSTITUTE FOR SOCIAL DEVELOPMENT** | `section-a` row 4 |
| **AutoTest-Institution** | Custom fixture. PBAC / email Request Access (`Pbac.submitAccessRequest`). **Must be created** if it is not in the NIH catalog. |

Request Access needs **at least two** institutions in the dropdown.

---

## 2. Programs (Manage Programs)

Hub programs (not the program *node* inside uploaded TSVs).

| Program name | Abbreviation / notes | Why |
|---|---|---|
| **AutoTest-Program** | DS list / Manage Studies filters use this **name**. Default DS header Program is **`AUTOPROGRAM`** (abbreviation on the study used by TC01 headers). For CRDCDH-3839 the **approved SR snapshot** must have acronym **ATP**, name **AutoTest-Program**, full description **`test program description`** (auto-pop does not use the current Hub abbreviation if it differs from the SR). | TC08/TC09/TC10, 3839, Playwright TC08 |
| **auto_program** | Exact dropdown value in `ManageStudies.addNewStudy()`. | TC13 Create New Study |
| **auto_program-api** | Abbreviation **`AUTOPROGRAMAPI`**. Hub program for the CDS API fixture study. Matches `InputFiles/CRDC/API/program-stage.tsv`. | CRDC_API (CRDCDH-1526 / 1551) |
| **NCI National Clinical Trials Network** | Short name / abbreviation **NCTN**. Needed on the **CTDC 3839 SR** (and as a Hub program if the SR program field is a Hub dropdown). | CRDCDH-3839 TC03 |
| **auto_program-cv-CRDCDH-3138** | Optional grouping; TC27 comments refer to this program. The **study** picker is `autoQA - AutomationQA_CV-CRDCDH-3138`. | Cross-validation leftovers |

Data Concierge on programs should be the DCP user display name: **`pbac.dcp-automation Please Do Not Edit`**.

Uploaded metadata often uses TSV program acronym **AUTOPROGRAM** / name **auto_program**. That is **inside the DS**, not a second Hub program unless you want them to match.

---

## 3. Studies

Create Data Submission picker label is **`{acronym} - {name}`** (exact match, whitespace-normalized).

**Submitter (and any role that creates a DS) must have access** to every study they select.

### 3.1 Core studies (Manage Studies unless noted)

**Default: Admin → Manage Studies → Add Study.** Fill acronym, name, program, commons, access, and dbGaP from the table. Keep the study **Active** and grant Submitter/DCP access. This is enough for Create DS, validation, CV, email, PBAC, smoke, and Data Explorer seed.

**Exception — CRDCDH-3839 only:** auto-pop reads the **approved SR snapshot**, not the Manage Studies row. For those two studies: new SR → fill the fields in §3.3 → Submit → Fed Lead approve. Do **not** edit program/study fields after approve (edits do not backfill auto-pop). Do **not** attach a later SR to a hand-built study with the same picker label.

| Picker label | Acronym | Name | Commons | Access / dbGaP | How to create | Used by |
|---|---|---|---|---|---|---|
| `autoQA - AutomationQA_Study` | autoQA | AutomationQA_Study | **GC, ICDC, CTDC** (same study must be selectable for all three models) | Controlled; Hub program abbreviation shown on DS header must be **`AUTOPROGRAM`**. TSV metadata often uses `phs674534` / study_id `phs769076`. | **Manage Studies** | Default DS tests, smoke, DataValidation, ICDC/CTDC suites, Playwright `DS_AUTOMATION_STUDY_CREATE` |
| `ATS - AutoTest-Study` | ATS | AutoTest-Study | **GC** | Open or Controlled as in env | **Manage Studies** | `Pbac.createDataSubmission()`, email DS tests, CRDCDH-1163 TC17, Playwright `AUTOMATION_STUDY_DS` |
| `autoQAdbGaPID - AutomationQA_dbGaPID` | autoQAdbGaPID | AutomationQA_dbGaPID | **GC** | **Registered dbGaP ID = `phs123456`** (no version) | **Manage Studies** (set dbGaP on the study) | CRDCDH-3140 TC01/TC02/TC03 |
| `AUTOQA - AutomationQA_CRDCDH-3839` | AUTOQA | AutomationQA_CRDCDH-3839 | **GC and ICDC** | Open; **phs on SR = literal `N/A`**. Description `CRDCDH-3839`. Program **AutoTest-Program / ATP / `test program description` on the SR at approval.** | **Approved SR only** (§3.3) | CRDCDH-3839 TC01 (GC), TC02 (ICDC) |
| `AUTOQA - AutomationQA_CRDCDH-3839-CTDC` | AUTOQA | AutomationQA_CRDCDH-3839-CTDC | **CTDC** | dbGaP **`phs003839`**. Program short name **NCTN**, program name **NCI National Clinical Trials Network**, full description **`test program description`**. | **Approved SR only** (§3.3) | CRDCDH-3839 TC03 |
| `autoQA - AutomationQA_CV` | autoQA | AutomationQA_CV | **GC** | Dedicated CV study | **Manage Studies** | CRDCDH-1051 TC25/TC26, CRDCDH-1390 TC40 |
| `autoQA - AutomationQA_CV-CRDCDH-3138` | autoQA | AutomationQA_CV-CRDCDH-3138 | **GC** | Dedicated CV study. **No leftover Submitted/Released DS** on this study when TC27 starts. | **Manage Studies** | CRDCDH-3138 TC27 |
| `autoQA - AutomationQA_CV_ICDC` | autoQA | AutomationQA_CV_ICDC | **ICDC** | Dedicated CV | **Manage Studies** | ICDC CRDCDH-1051 TC39 |
| `autoQA - AutomationQA_CV_CTDC` | autoQA | AutomationQA_CV_CTDC | **CTDC** | Dedicated CV | **Manage Studies** | CTDC CRDCDH-1051 TC38 |
| `automation_CompletedStudy - AutomationCompletedStudy` | automation_CompletedStudy | AutomationCompletedStudy | **GC** | See §5.3 Data Explorer | **Manage Studies**, then a **Released** (Explorer-visible) DS | CRDCDH-3026 TC201 |
| `autoQA - AutomationQA_API_Tests` | autoQA | AutomationQA_API_Tests | **CDS** | Program **auto_program-api** / **AUTOPROGRAMAPI**. dbGaP **`phs0098688`**. After create, set profile **`ApiStudyId`** to the Hub study UUID (see §7). Grant Submitter access. | **Manage Studies** | CRDCDH-1526 TC01, CRDCDH-1551 TC01/TC02 |

### 3.2 3140 dbGaP rules (same study, three tests)

Registered Hub dbGaP must be **`phs123456`**.

| Test | Uploaded `phs_accession` | Expected |
|---|---|---|
| TC01 | `phs123456` | 0 validation errors |
| TC02 | `phs123456.v1.p1` | 0 errors (major portion match) |
| TC03 | `phs001234` | Error: dbGaPID mismatch |

### 3.3 3839 SR field checklist (copy onto the form before Submit)

**GC/ICDC study `AUTOQA - AutomationQA_CRDCDH-3839`**

- Program name: `AutoTest-Program`
- Program acronym: `ATP`
- Program full description: `test program description`
- Study name: `AutomationQA_CRDCDH-3839`
- Study acronym: `AUTOQA`
- Study description: `CRDCDH-3839`
- dbGaP / phs: `N/A`
- Data Commons: GC (ICDC tests reuse the same approved study)

**CTDC study `AUTOQA - AutomationQA_CRDCDH-3839-CTDC`**

- Program short name: `NCTN`
- Program name: `NCI National Clinical Trials Network`
- Program full description: `test program description`
- Study name / acronym / description: `AutomationQA_CRDCDH-3839-CTDC` / `AUTOQA` / `CRDCDH-3839`
- dbGaP: `phs003839`

A hand-built Manage Studies row with the same picker label will let Create DS succeed and then fail validate (missing required auto-pop fields). **Do not “fix” it with a later SR.** Inactive/rename the manual study, then approve a new SR.

---

## 4. Automation users and roles

Katalon `loginToCrdcOtp('<Role>')` reads `{Role}Email` / `{Role}Password` / `{Role}Secret` from the execution profile.

| Profile key (role) | Manage Users search token | Display name / notes | Login email (same on QA / QA2 / Stage / Dev profiles) | Hub role |
|---|---|---|---|---|
| **Master** | (must be able to open Manage Users and edit **every** role matrix) | Super-admin for `EnableAllPermissions` | `qa+crdc-automation-master@essential-soft.com` | Admin |
| **Admin** | `pbac.Admin-automation` | Admin automation user | `qa+crdc-automation-admin@essential-soft.com` | Admin |
| **Fedlead** | `pbac.Fedlead-automation` | Federal Lead | `qa+crdc-automation-fedlead@essential-soft.com` | Federal Lead |
| **Dcp** | `pbac.Dcp-automation` | Display name must be **`pbac.dcp-automation Please Do Not Edit`**. Assign Data Commons (**GC**, plus **ICDC** / **CTDC** if those suites run). CRDCDH-502 expects the Data Commons list to have **at least 2** options. | `qa+crdc-automation-dcp@essential-soft.com` | Data Commons Personnel |
| **Submitter** | `pbac.Submitter-automation` | Display name must be **`pbac.submitter-automation Please Do Not Edit`** (DS list filter TC10). | `qa+crdc-automation-submitter@essential-soft.com` | Submitter |
| **User** | `pbac.User-automation` | User role (email SR tests, PBAC) | `qa+crdc-automation-user@essential-soft.com` | User |
| **AlternateSubmitter** | (second submitter; not in `pbac.*-automation` search) | Second Submitter. **Must have access to `ATS - AutoTest-Study`**. DS Config Change email To: | `qa+sohil-sub@essential-soft.com` | Submitter |

Login.gov accounts **survive** a Hub DB wipe. After a wipe, re-create the **Hub** user records, map each email to the role above, keep TOTP in the Katalon profile, and re-grant study access. Do not put passwords or TOTP secrets in this document.

### 4.1 Study access (minimum)

| User | Must be able to see / create DS for |
|---|---|
| Submitter | All studies in §3.1 (GC/ICDC/CTDC as applicable) |
| Dcp | Same studies (validate, submit/release/reject, cross-validate, list/sort/paginate) |
| Fedlead | Approve/reject/inquire SRs; email To on SR Available for Review |
| AlternateSubmitter | `ATS - AutoTest-Study` |
| User | Can Request Access to **AutoTest-Study** / **AutoTest-Institution** (email + PBAC Request Access) |
| Admin / Master | Manage Users, Studies, Programs; PBAC |

### 4.2 Permissions

After a wipe (or after a PBAC “unchecked” suite), run:

`Test Cases/CRDC/SuiteSetup/EnableAllPermissions`

That logs in as **Master**, opens Manage Users, and sets **positive** permissions for Admin, Dcp, Fedlead, Submitter, User from `InputFiles/CRDC/PBACPermissions.xlsx`.

DSFlow, DataFileUpload, DataValidation, EmailNotifications, and PBAC include this case. **Smoke does not** — run it once before Smoke after a wipe.

Source of truth for checkboxes and email templates: **`InputFiles/CRDC/PBACPermissions.xlsx`**.

---

## 5. Data submissions — what must already exist vs what tests create

### 5.1 Tests create their own DS (no pre-seed required)

Almost all DataValidation / DSFlow / DataFileUpload / email DS cases:

1. Create DS on a fixture study
2. Upload / validate / submit / release / complete as the test needs
3. Leave leftovers (Canceled / Rejected / In Progress)

These studies must **not** already have **Submitted or Released** DS when the listed tests start (`failIfSubmittedOrReleasedExistForStudy`). After a failed run, cancel/reject/delete those leftovers:

| Study name filter | Test |
|---|---|
| `AutomationQA_CV-CRDCDH-3138` | CRDCDH-3138 TC27 |
| `AutomationQA_Study` | CRDCDH-1042 TC23 (delete). Also keep CV studies (`AutomationQA_CV`, `AutomationQA_CV_ICDC`, `AutomationQA_CV_CTDC`) free of Submitted/Released before 1051 / 1390. |

### 5.2 Must pre-seed (will fail immediately if missing)

| What | Status | Why |
|---|---|---|
| At least **one** DS whose name contains **`auto`**, program **AutoTest-Program**, submitter **`pbac.submitter-automation Please Do Not Edit`**, commons **GC**, status **Submitted or In Progress** | Submitted and/or In Progress | Smoke **TC10** Data Submissions list filter (`expectAtLeastRows: 1`). **Smoke currently starts with TC10** (the suite description mentions EnableAllPermissions first, but it is not the first case in `CRDC_Smoke.ts`). |
| Enough DS **visible to DCP** for pagination | any | Smoke **TC04** — DCP list, default **20** rows/page (`verifyDefaultRowsPerPage(20)`). After a wipe the DCP list may be empty; create dummy GC DS or run DS-create tests as DCP first. |
| At least one SR for Submitter in **New or In Progress** | New / In Progress | Smoke **TC12** SR list filter |
| At least one Manage Studies row matching filter **autoQA** + program **AutoTest-Program** | Active | Smoke **TC08**, CRDCDH-3913 export |
| At least one Manage Programs row matching **AutoTest** / study **autoQA** | Active | Smoke **TC09** |
| **Data Explorer seed** — see next section | Released (Explorer) | CRDCDH-3026 TC201 |

### 5.3 Data Explorer / Completed study (CRDCDH-3026)

Manual seed (test does **not** create the DS):

1. Study picker: `automation_CompletedStudy - AutomationCompletedStudy`
2. Create a GC metadata DS as Submitter
3. Upload `InputFiles/CRDC/MetadataData/CRDCDH-3026/`
4. Validate, Submit, DCP Cross-validate / Release so it appears in **Data Explorer**
5. Filter Explorer by study acronym `automation_CompletedStudy`

Known: this test has been a Hub bug in some envs; still reseed the study/DS so it is not a false “missing fixture” failure.

### 5.4 Header assertions (TC01 VerifyDataSubmissionHeaders)

Hardcoded expectations after create on `autoQA - AutomationQA_Study`:

| Header | Expected |
|---|---|
| Study | `autoQA` |
| Program | `AUTOPROGRAM` |
| Data Concierge | `pbac.dcp-automation Please Do Not Edit` |
| Submitter | `{profile name} Please Do Not Edit` |

If the Hub program shown on the DS is `AutoTest-Program` instead of `AUTOPROGRAM`, this test fails even when the study exists. Match whatever the env showed when the test last passed, or update the keyword — do not guess.

---

## 6. Email notification extras

Suite: `CRDC_EmailNotifications`. Catalog: PBAC Permissions sheet (see `PythonFiles/CRDC/gmail/README.md`).

BCC/CC Gmail inboxes (must exist and have SR/DS notification checkboxes on; tests do **not** EnableAllPermissions on these):

| Profile / use | Email |
|---|---|
| FedleadBcc | `pbac.fedlead@gmail.com` |
| DcpBcc | `vincentnihtestfour@gmail.com` |
| AdminBcc | `pbacmaster@gmail.com` |

Gmail OAuth tokens: `PythonFiles/CRDC/gmail/` (not Hub seed).

---

## 7. After wipe — code / profile values that go stale

These are **not** recreated in Hub UI; they break until updated:

| Item | Where | Notes |
|---|---|---|
| **API `ApiStudyId` UUID** | Profile `ApiStudyId` (QA: `bfe2cd9f-c703-4869-8712-f48f41880242`, QA2: `9d603c67-637a-403b-a6dd-4f75ddd5c99f`). Scripts read this for `createSubmission`; `dataCommons` stays `CDS`. | New env / wipe = new UUID for `autoQA - AutomationQA_API_Tests`. Paste it into the **profile**, not the scripts. Name match alone is not enough. Stage/Dev start empty until seeded. |
| **SubmitterApiToken** | Profile `SubmitterApiToken` | Mint a new JWT after user recreation. |
| **G_MongoUri** / database | Profile | DocDB host/creds per env. Used by Mongo verify keywords. |
| **ApiUrl** | Profile | `{G_Urlname}api/graphql` |
| CLI **get_temp_credential** | Hub/AWS | Not seed data. 504 here fails CRDCDH-1405 / CLI tests even when studies are correct. |

---

## 8. Suite map (what each suite needs)

| Suite | Extra seed beyond §1–4 |
|---|---|
| **CRDC_Smoke** | **Run EnableAllPermissions before Smoke** (it is not the first case in the suite XML). TC10/TC08/TC09/TC12 need existing list rows (§5.2). GC studies. CLI temp credentials healthy. |
| **CRDC_DataValidation** | All GC validation studies including **3140 dbGaP** and **3839 approved SR**. 2554 uses default autoQA. |
| **CRDC_DSFlow** | EnableAllPermissions. CV studies clean of Submitted/Released. |
| **CRDC_DataFileUpload** | EnableAllPermissions. `autoQA` GC. CLI + temp credentials. |
| **CRDC_ICDCSuite** / **CRDC_CTDCSuite** | Same `autoQA - AutomationQA_Study` selectable for ICDC/CTDC. CV + 3839 studies per commons. |
| **CRDC_SubmissionRequests** | Institutions + Submitter/Fedlead. Tests create SRs. |
| **CRDC_PBAC** | All six users. Master can edit role matrices. Unchecked suite **strips** permissions — run EnableAllPermissions after. |
| **CRDC_EmailNotifications** | ATS study, AlternateSubmitter access, Gmail BCC mailboxes. |
| **CRDC_API** | CDS study `autoQA - AutomationQA_API_Tests` on program **auto_program-api**. Set profile **`ApiStudyId`** to that study’s UUID. Refresh SubmitterApiToken. Not in Smoke. |
| **CRDC_Auxiliary** | Filters/pagination/create study (`auto_program`). |
| **CCDI-Studies Testing** | **Not Hub.** CCDI portal studies (`phs000466`, etc.). Ignore for a Data Hub DB wipe. |

Playwright (`crdc-dh-automation-pw`) reuses the same Hub fixtures: `autoQA - AutomationQA_Study`, `ATS - AutoTest-Study`, program `AutoTest-Program`.

---

## 9. Manual Hub order (copy/paste)

After migration:

- [ ] Institutions: 21MEDTECH LLC, 2WITECH SOLUTIONS LLC, 3-C INSTITUTE FOR SOCIAL DEVELOPMENT, **AutoTest-Institution**
- [ ] Programs: **AutoTest-Program** (filters + 3839 name), **auto_program** (TC13), **auto_program-api / AUTOPROGRAMAPI** (API), **NCTN / NCI National Clinical Trials Network** (CTDC 3839)
- [ ] Recreate Hub users for Master, Admin, Fedlead, Dcp, Submitter, User, AlternateSubmitter; search tokens `pbac.{Role}-automation`; DCP/Submitter display names include **` Please Do Not Edit`**
- [ ] DCP has Data Commons GC (plus ICDC/CTDC if those suites run); Data Concierge on AutoTest-Program = DCP display name
- [ ] Submitter profile PI institution empty **or** exactly **21MEDTECH LLC** (AllFields row 2)
- [ ] **Manage Studies:** add every study in §3.1 except the two 3839 rows (3140 dbGaP `phs123456`; default study program abbreviation `AUTOPROGRAM`)
- [ ] **Approved SR only for 3839:** fill §3.3 exactly, submit, Fed Lead approve (do not seed those two via Manage Studies)
- [ ] Grant Submitter / DCP / AlternateSubmitter study access
- [ ] Run **EnableAllPermissions**
- [ ] Seed one GC DS named with `auto` (Submitted or In Progress) for TC10
- [ ] Seed one Submitter SR in New/In Progress for TC12
- [ ] Seed CRDCDH-3026 Released DS for Data Explorer
- [ ] Confirm no Submitted/Released leftover on CV studies
- [ ] If running **CRDC_API:** Manage Studies CDS study `autoQA - AutomationQA_API_Tests` (program auto_program-api, dbGaP `phs0098688`); set profile **`ApiStudyId`** to the new UUID; mint SubmitterApiToken; grant Submitter access
- [ ] Spot-check Create DS picker: every label in §3.1 appears for Submitter

---

## 10. Failure shortcuts

| Symptom | Likely missing seed |
|---|---|
| `Study option '…' not found in dropdown` | Study missing, wrong commons, or user has no access |
| 3839: 2 Error rows; program/study name empty after validate | Study was **not** created from an approved SR with fields at approval |
| 3140 TC03 expected mismatch missing / TC01 has dbGaP errors | Hub dbGaP is not `phs123456` |
| TC10 filter `expectAtLeastRows: 1` | No DS named `auto` / wrong program / wrong submitter display name |
| TC08/TC09 filter empty | AutoTest-Program / autoQA study not Active |
| Headers: Program actual ≠ `AUTOPROGRAM` | Hub program acronym on that study |
| Headers: Data Concierge mismatch | DCP display name |
| `CliUploader: upload failed` + `get_temp_credential` 504 | Env/AWS, not missing study |
| PBAC / missing nav items after PBAC suite | Re-run EnableAllPermissions |
| API createSubmission 404/400 on studyID | Profile `ApiStudyId` stale after wipe, or blank on Stage/Dev |

---

## 11. Source files (if this checklist drifts)

- Create DS study strings: `Scripts/CRDC/**/*.groovy` `createDataSubmission(...)`
- PBAC DS study: `Keywords/utilities/Pbac.groovy` (`ATS - AutoTest-Study`, `AutoTest-Institution`)
- Login roles: `Keywords/utilities/Login.groovy`
- EnableAllPermissions: `Scripts/CRDC/SuiteSetup/EnableAllPermissions/`
- List filters: `Scripts/CRDC/AuxiliaryTests/TC08`, `TC09`, `TC10`, `TC12`
- 3839 / 3140 / 3026 script headers
- SR institutions: `InputFiles/CRDC/SubmissionRequest.xlsx` sheet `section-a`
- Email catalog: `PythonFiles/CRDC/gmail/README.md`
- Playwright: `src/crdc_pw/pages/data_submission_details.py`, `src/crdc_pw/utils/pbac_helpers.py`
