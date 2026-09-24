# CRDC email notification verification

This is a **two-step pipeline**. Katalon only drives the UI and writes expected rows. Python only judges Gmail later. Neither talks to the other at runtime.

## 1. Catalog (source of truth)

`InputFiles/CRDC/PBACPermissions.xlsx` → sheet **Permissions**, the email-notification rows (`Submission Request Emails`, `Data Submission Emails`, and Miscellaneous Request Access). Katalon sees it as Data File `CRDC/Pbac/Permissions`.

Four extra columns on those rows drive the judge. Other permission rows leave them blank.

| Trigger | Who gets it (To role) | Name in body |
|---|---|---|
| SR Submit | Submitter, and Fedlead | FALSE on Received; TRUE on Available for Review |
| DS Create | Dcp | TRUE |
| DS Submit | Submitter | TRUE |
| DS Cancel | Submitter | TRUE |
| DS Withdraw | Dcp | TRUE |
| DS Release | Dcp | TRUE |
| DS Reject | Submitter | TRUE |
| DS Complete | Submitter | TRUE |
| Request Access | Admin | TRUE |

Expected email subject, To role, and name-in-body live here. Tests do not hardcode those strings. Fill Trigger on a Permissions email row to include it in `logEmails`. Leave Trigger blank on the `access:request` permission row (the Request Access button).

## 2. TC01 — Submitter submits an SR

Login → `Pbac.createAndSubmitSubmissionRequest()` (fill the questionnaire, wait until Submit is clickable with STOP_ON_FAILURE, click Submit).

Only then:

`logEmails('SR Submit', 'Submitter', created.id, created.name)`

If Submit never enables, the case **stops** and **no checklist rows** are written.

## 3. TC02 — Submitter creates and submits a DS

Login → `Pbac.createUploadValidateAndSubmitDataSubmission()` (create DS, upload unique `program.tsv`, wait until Validate is clickable, native click, wait until Submit is clickable, native click, Yes).

Only then:

`logEmails('DS Create', …)` then `logEmails('DS Submit', …)`

Same rule: fail before log. `created.id` is the submission UUID; `created.name` is the DS display name.

## 4. TC03 — Submitter cancels a DS

Login → `Pbac.createAndCancelDataSubmission()` (create a New metadata-only DS, wait until Cancel is clickable, native click, Yes). No upload or submit.

Only then:

`logEmails('DS Cancel', 'Submitter', created.id, created.name)`

To is the Submitter. Fedlead/DCP/Admin with the cancelled notification on are BCC and are **not** judged. Creating still sends the DCP create mail; this case does not log `DS Create`.

## 5. TC04 — Submitter withdraws a DS

Login → `Pbac.createSubmitAndWithdrawDataSubmission()` (reuses `createUploadValidateAndSubmitDataSubmission`, wait until Withdraw is clickable, native click, Yes).

Only then:

`logEmails('DS Withdraw', 'Submitter', created.id, created.name)`

To is DCP. Fedlead/Submitter/Admin with the withdrawn notification on are BCC and are **not** judged. Submit still sends create/submit mail; this case does not log `DS Create` or `DS Submit`.

## 6. TC05 — DCP releases a DS

Login Submitter → `Pbac.createSubmitAndReleaseDataSubmission()` (reuses `createUploadValidateAndSubmitDataSubmission`, logout, login DCP, wait until status is Submitted, `clickCrossValidateButton`, `clickTab` Release, `clickTab` Yes — same waits as TC06 history).

Only then:

`logEmails('DS Release', 'Dcp', created.id, created.name)`

To is DCP. Fedlead/Submitter/Admin with the released notification on are BCC and are **not** judged. Submit still sends create/submit mail; this case does not log `DS Create` or `DS Submit`.

## 7. TC06 — DCP rejects a DS

Login Submitter → `Pbac.createSubmitAndRejectDataSubmission()` (reuses `createUploadValidateAndSubmitDataSubmission`, logout, login DCP, wait until status is Submitted, `clickTab` Reject, comment `Rejecting`, Confirm to Reject — same waits as TC06 history).

Only then:

`logEmails('DS Reject', 'Dcp', created.id, created.name)`

To is the Submitter. Fedlead/DCP/Admin with the rejected notification on are BCC and are **not** judged. Submit still sends create/submit mail; this case does not log `DS Create` or `DS Submit`.

## 8. TC07 — DCP completes a DS

Login Submitter → `Pbac.createSubmitReleaseAndCompleteDataSubmission()` (reuses `createSubmitAndReleaseDataSubmission`, then `clickTab` Complete, `clickTab` Yes — same waits as TC06 history).

Only then:

`logEmails('DS Complete', 'Dcp', created.id, created.name)`

To is the Submitter. Fedlead/DCP/Admin with the completed notification on are BCC and are **not** judged. Submit and Release still send mail; this case does not log `DS Create`, `DS Submit`, or `DS Release`.

## 9. TC08 — User requests access

Login User → `Pbac.submitAccessRequest()` (Profile → Request Access, Institution `AutoTest-Institution`, study `AutoTest-Study`, unique Additional Info, native Submit). Stops if the dialog does not close.

Only then:

`logEmails('Request Access', 'User', created.id, created.name)`

To is Admin. Entity type is blank (Miscellaneous). `created.id` and `created.name` are the Additional Info token. Admin’s Request Access email checkbox is already on by default; this case does not turn permissions on.

## 10. logEmails

One timestamp. Find every catalog row whose **Trigger** matches. For each:

- Action on the checklist = Trigger
- Actor = who clicked (script argument; Submitter except TC05/TC06/TC07 Dcp and TC08 User)
- Expected recipient = profile email for To role (`SubmitterEmail` / `FedleadEmail` / `DcpEmail` / `AdminEmail`)
- Environment from `G_Urlname` (`QA` / `QA2` / `STAGE`; `hub-qa2` is checked before `hub-qa`)
- Name = study title, DS display name, or the Request Access Additional Info token
- Result left blank

SR Submit writes **two** rows (Submitter + Fedlead). DS Create, DS Submit, DS Cancel, DS Withdraw, DS Release, DS Reject, DS Complete, and Request Access write one each.

Unknown Trigger → test fails. New mail after the same click = another catalog row with the same Trigger. New UI action = a new Trigger value plus one `logEmails` call.

`appendFromCatalog` does the lookup. `appendRow` writes one Excel row. Tests only call `logEmails`.

## 11. Checklist

Append-only `OutputFiles/NotificationEmail_Checklist.xlsx`. UI tests never open Gmail.

## 12. Verifier (run after mail should be in the inbox)

`PythonFiles/CRDC/VerifyNotificationEmails.py` against `qa@essential-soft.com` (plus-aliases like `qa+crdc-automation-submitter@…` land in the same mailbox).

For each of **today’s** checklist rows that are still blank or Fail it:

1. Searches Gmail: `newer_than:2d subject:"[QA] <expected subject>"` (or `[QA2]` / `[STAGE]`) so a QA run cannot Pass STAGE mail. The Name is not in the Gmail search string.
2. Walks up to 20 hits until one message has **all** of:
   - Subject starts with that env tag and contains the expected title
   - **To** contains the expected address (not Cc/Bcc)
   - Body contains **Name** only if the catalog says Name in body = TRUE

Writes **Pass** or **Fail**. Re-run retries Fail/blank only; Pass/Skip stay. Console on Pass includes the To: address. A blank Environment cell Fails without searching Gmail.

## 13. How you run it

1. Refresh Katalon Data File `CRDC/Pbac/Permissions` if you changed Trigger/Expected email subject/To role/Name in body.
2. Run TC01 through TC08 as needed (profile `CRDC_QA` / `CRDC_QA2` / `CRDC_STAGE`).
3. When mail should already be there, from the project venv:

```bash
python PythonFiles/CRDC/VerifyNotificationEmails.py
```

Override with `--excel`, `--date`, `--credentials`, `--token`, or env `GMAIL_CREDENTIALS_PATH` / `GMAIL_TOKEN_PATH`.

That is the whole loop: catalog defines mail, UI tests only log after a real submit, cancel, withdraw, release, reject, complete, or request access, Gmail judge scores the sheet.

## Auth files (not committed)

Drop copies of these files in this folder (same files used by the local Gmail API quickstart):

- `credentials.json` — OAuth client from Google Cloud (Desktop app)
- `token.json` — refresh token after one browser login as `qa@essential-soft.com`

Ask a teammate who already authorized the inbox for a copy. Do not commit either file. The repo ignores `PythonFiles/CRDC/gmail/*.json`.
