# CRDC email notification verification

Katalon UI tests live under `Test Cases/CRDC/NotificationEmails/`: **SR/** (submit, cancel, reject, approve), **DS/** (create/submit through complete), **Miscellaneous/** (Request Access and similar), and **VerifyNotificationEmails** in the parent folder.

This is a **two-step pipeline**. Katalon only drives the UI and writes expected rows. Python only judges Gmail later. Neither talks to the other at runtime.

## 1. Catalog (source of truth)

`InputFiles/CRDC/PBACPermissions.xlsx` → sheet **Permissions**, the email-notification rows (`Submission Request Emails`, `Data Submission Emails`, and Miscellaneous Request Access). Katalon sees it as Data File `CRDC/Pbac/Permissions`.

Catalog columns on those rows: **Trigger**, **Expected email subject**, **Name in body**. Other permission rows leave them blank. Recipient is not on this sheet; each `logEmails` call passes `toRole`.

One PBAC checkbox that sends more than one template packs Triggers and subjects with ` | ` (same token count). `logEmails` matches one token and uses the subject at the same index.

| Trigger | Name in body | Typical `toRole` on the test |
|---|---|---|
| SR Submit | FALSE | Submitter (or User if that account owns the SR) |
| SR Available for Review | TRUE | Fedlead |
| SR Reject / SR Approve / SR Inquire (packed on **When review decision made**) | TRUE | Submitter (or User) |
| SR Cancel / SR Restore (packed on **When canceled/restored**) | TRUE | Submitter (or User) |
| DS Create | TRUE | Dcp |
| DS Submit | TRUE | Submitter |
| DS Cancel | TRUE | Submitter |
| DS Withdraw | TRUE | Dcp |
| DS Release | TRUE | Dcp |
| DS Reject | TRUE | Submitter |
| DS Complete | TRUE | Submitter |
| Request Access | TRUE | Admin |

Tests do not hardcode expected subjects. Fill Trigger on a Permissions email row to include it in `logEmails`. Leave Trigger blank on the `access:request` permission row (the Request Access button). Do not add extra Permissions rows for extra templates; pack tokens on the existing checkbox row.

## 2. TC01 — Submitter submits an SR

Each SR script runs the steps itself and keeps the values for `logEmails` in variables:

1. `SubmissionRequest.startSubmissionRequest()`, then `fillSubmissionRequestAllSections(...)`.
2. `srId = SubmissionRequest.getApplicationIdFromUrl()` (application UUID) and `studyName = SubmissionRequest.lastEnteredStudyTitle` (unique study title), read while still on the form URL.
3. `WebUIUtils.waitUntilEnabledOrStop(Submit-Btn, 30, 'SR Submit')`, then `SubmissionRequest.clickSubmitButton()`.

Only then:

`logEmails('SR Submit', 'Submitter', srId, studyName, 'Submitter')`

`logEmails('SR Available for Review', 'Submitter', srId, studyName, 'Fedlead')`

If Submit never enables, the case **stops** and **no checklist rows** are written. Submit sends two To: mails, so this case calls `logEmails` twice.

## 3. TC02 — Submitter creates and submits a DS

Each DS script runs the steps itself and keeps the values for `logEmails` in variables:

1. `clickTab` Data Submissions, `DataSubmissions.createDataSubmission('New', 'Metadata Only', 'GC', 'ATS - AutoTest-Study')`, open the DS, wait for the loading icon to go.
2. `dsId = DataSubmissions.getSubmissionIdFromUploadActivityUrl()` (submission UUID) and `dsName = DataSubmissions.expectedSubmissionName` (DS display name).
3. `Pbac.prepareUniqueMetadataAndUpload(program.tsv)`, `DataSubmissions.clickValidateButton()`, then `DataSubmissions.submitDataSubmission()` (Submit only after it is enabled or stop, Yes, wait until status is Submitted or stop).

Only then:

`logEmails('DS Create', 'Submitter', dsId, dsName, 'Dcp')` then `logEmails('DS Submit', 'Submitter', dsId, dsName, 'Submitter')`

Same rule: fail before log.

## 4. TC03 — Submitter cancels a DS

TC02 steps 1–2, then `DataSubmissions.cancelDataSubmission()` (Cancel only after it is enabled or stop, Yes, wait until status is Canceled or stop). No upload or submit.

Only then:

`logEmails('DS Cancel', 'Submitter', dsId, dsName, 'Submitter')`

To is the Submitter. Fedlead/DCP/Admin with the cancelled notification on are BCC and are **not** judged. Creating still sends the DCP create mail; this case does not log `DS Create`.

## 5. TC04 — Submitter withdraws a DS

TC02 steps 1–3, then `DataSubmissions.withdrawDataSubmission()` (Withdraw only after it is enabled or stop, Yes, wait until status is Withdrawn or stop).

Only then:

`logEmails('DS Withdraw', 'Submitter', dsId, dsName, 'Dcp')`

To is DCP. Fedlead/Submitter/Admin with the withdrawn notification on are BCC and are **not** judged. Submit still sends create/submit mail; this case does not log `DS Create` or `DS Submit`.

## 6. TC05 — DCP releases a DS

TC02 steps 1–3 as Submitter → `Login.switchUser('Dcp')` → `DataSubmissions.releaseDataSubmission(dsId)` (open the DS, wait until status is Submitted, `clickCrossValidateButton`, `clickTab` Release, `clickTab` Yes, wait until status is Released).

Only then:

`logEmails('DS Release', 'Dcp', dsId, dsName, 'Dcp')`

To is DCP. Fedlead/Submitter/Admin with the released notification on are BCC and are **not** judged. Submit still sends create/submit mail; this case does not log `DS Create` or `DS Submit`.

## 7. TC06 — DCP rejects a DS

TC02 steps 1–3 as Submitter → `Login.switchUser('Dcp')` → `DataSubmissions.rejectDataSubmission(dsId)` (open the DS, wait until status is Submitted, `clickTab` Reject, comment `Rejecting`, Confirm to Reject, wait until status is Rejected).

Only then:

`logEmails('DS Reject', 'Dcp', dsId, dsName, 'Submitter')`

To is the Submitter. Fedlead/DCP/Admin with the rejected notification on are BCC and are **not** judged. Submit still sends create/submit mail; this case does not log `DS Create` or `DS Submit`.

## 8. TC07 — DCP completes a DS

TC02 steps 1–3 as Submitter → `Login.switchUser('Dcp')` → `DataSubmissions.releaseDataSubmission(dsId)` → `DataSubmissions.completeDataSubmission()` (`clickTab` Complete, `clickTab` Yes, wait until status is Completed).

Only then:

`logEmails('DS Complete', 'Dcp', dsId, dsName, 'Submitter')`

To is the Submitter. Fedlead/DCP/Admin with the completed notification on are BCC and are **not** judged. Submit and Release still send mail; this case does not log `DS Create`, `DS Submit`, or `DS Release`.

## 9. TC08 — User requests access

Login User → `Pbac.submitAccessRequest()` (Profile → Request Access, Institution `AutoTest-Institution`, study `AutoTest-Study`, unique Additional Info, native Submit). Stops if the dialog does not close.

Only then:

`logEmails('Request Access', 'User', created.id, created.name, 'Admin')`

To is Admin. Entity type is blank (Miscellaneous). `created.id` and `created.name` are the Additional Info token. Admin’s Request Access email checkbox is already on by default; this case does not turn permissions on.

## 10. TC09 — Submitter cancels an In Progress SR

TC01 steps 1–2, then `SubmissionRequest.cancelSubmissionRequestButton()`. Does **not** Submit.

Only then:

`logEmails('SR Cancel', 'Submitter', srId, studyName, 'Submitter')`

To is the Submitter (`toRole`). Fedlead/DCP/Admin with the canceled notification on are BCC and are **not** judged. Restore uses the same PBAC checkbox (packed `SR Restore` on that row); this case does not log a restore row.

## 11. TC10 — Fedlead rejects an SR

TC01 steps 1–3 as Submitter → `Login.switchUser('Fedlead')` → `SubmissionRequest.rejectSubmissionRequest(studyName)`.

Only then:

`logEmails('SR Reject', 'Fedlead', srId, studyName, 'Submitter')`

To is the Submitter. Fedlead/DCP/Admin with the reviewed notification on are BCC and are **not** judged. Submit still sends TC01 mail; this case does not log `SR Submit`.

## 12. TC11 — Fedlead approves an SR

TC01 steps 1–3 as Submitter → `Login.switchUser('Fedlead')` → `SubmissionRequest.approveSubmissionRequest(studyName)` (no pending-condition checkboxes).

Only then:

`logEmails('SR Approve', 'Fedlead', srId, studyName, 'Submitter')`

To is the Submitter. Same BCC rule as TC10. Submit mail is not logged here.

## 13. VerifyNotificationEmails

No browser. `NotificationEmailLog.verifyNotificationEmails()` runs `PythonFiles/CRDC/VerifyNotificationEmails.py` with `Utils.getPythonExecutablePath()`. The case **fails** if the process exits non-zero (any Fail for today, or the checklist is missing). Pass/Skip rows are left unchanged. Last case in `CRDC_EmailNotifications`.

## 14. logEmails

One timestamp. Find the catalog row whose **Trigger** cell contains that token (plain value or ` | `-separated). Write one checklist row:

- Action on the checklist = Trigger argument
- Actor = who clicked (script argument; Submitter except TC05/TC06/TC07 Dcp, TC08 User, and TC10/TC11 Fedlead)
- Expected recipient = profile email for the `toRole` argument (`SubmitterEmail` / `FedleadEmail` / `DcpEmail` / `AdminEmail` / `UserEmail`)
- Environment from `G_Urlname` (`QA` / `QA2` / `STAGE`; `hub-qa2` is checked before `hub-qa`)
- Name = study title, DS display name, or the Request Access Additional Info token
- Result left blank

Blank `toRole` or unknown Trigger → test fails. Packed Trigger and Expected email subject must have the same token count. One UI action that sends two different mails (TC01) uses two `logEmails` calls with two Triggers. One checkbox with several templates (reject / approve / inquire) stays one Permissions row with packed tokens; the test passes the Trigger for that click.

`appendFromCatalog` does the lookup. `appendRow` writes one Excel row. Tests only call `logEmails`.

## 15. Checklist

Append-only `OutputFiles/NotificationEmail_Checklist.xlsx`. UI tests never open Gmail.

## 16. Verifier (run after mail should be in the inbox)

`PythonFiles/CRDC/VerifyNotificationEmails.py` against `qa@essential-soft.com` (plus-aliases like `qa+crdc-automation-submitter@…` land in the same mailbox).

For each of **today’s** checklist rows that are still blank or Fail it:

1. Searches Gmail: `newer_than:2d subject:"[QA] <expected subject>"` (or `[QA2]` / `[STAGE]`) so a QA run cannot Pass STAGE mail. The Name is not in the Gmail search string.
2. Walks up to 20 hits until one message has **all** of:
   - Subject starts with that env tag and contains the expected title
   - **To** contains the expected address (not Cc/Bcc)
   - Body contains **Name** only if the catalog says Name in body = TRUE (packed Expected email subject cells are split on `|`)

Writes **Pass** or **Fail**. Re-run retries Fail/blank only; Pass/Skip stay. Console on Pass includes the To: address. A blank Environment cell Fails without searching Gmail.

## 17. How you run it

1. Refresh Katalon Data File `CRDC/Pbac/Permissions` if you changed Trigger/Expected email subject/Name in body (or after removing a column).
2. Run TC01 through TC11 as needed (profile `CRDC_QA` / `CRDC_QA2` / `CRDC_STAGE`).
3. When mail should already be there, run **VerifyNotificationEmails** (same profile). It runs `PythonFiles/CRDC/VerifyNotificationEmails.py`. The case **fails** if the script exits non-zero (any Fail row, or the checklist file is missing). You can also run the script from the project venv:

```bash
python PythonFiles/CRDC/VerifyNotificationEmails.py
```

Override with `--excel`, `--date`, `--credentials`, `--token`, or env `GMAIL_CREDENTIALS_PATH` / `GMAIL_TOKEN_PATH`.

That is the whole loop: catalog defines mail, UI tests only log after a real submit, cancel, withdraw, release, reject, complete, request access, or SR approve, Gmail judge scores the sheet.

## Auth files (not committed)

Drop copies of these files in this folder (same files used by the local Gmail API quickstart):

- `credentials.json` — OAuth client from Google Cloud (Desktop app)
- `token.json` — refresh token after one browser login as `qa@essential-soft.com`

Ask a teammate who already authorized the inbox for a copy. Do not commit either file. The repo ignores `PythonFiles/CRDC/gmail/*.json`.
