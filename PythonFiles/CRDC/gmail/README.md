# CRDC email notification verification

This is a **two-step pipeline**. Katalon only drives the UI and writes expected rows. Python only judges Gmail later. Neither talks to the other at runtime.

## 1. Catalog (source of truth)

`InputFiles/CRDC/PBACPermissions.xlsx` → sheet **Permissions**, the email-notification rows (`Submission Request Emails`, `Data Submission Emails`). Katalon sees it as Data File `CRDC/Pbac/Permissions`.

Four extra columns on those rows drive the judge. Other permission rows leave them blank.

| Trigger | Who gets it (To role) | Name in body |
|---|---|---|
| SR Submit | Submitter, and Fedlead | FALSE on Received; TRUE on Available for Review |
| DS Create | Dcp | TRUE |
| DS Submit | Submitter | TRUE |

Subject, To role, and name-in-body live here. Tests do not hardcode those strings. Fill Trigger on a Permissions email row to include it in `logEmails`.

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

## 4. logEmails

One timestamp. Find every catalog row whose **Trigger** matches. For each:

- Action on the checklist = Trigger
- Actor = who clicked (script argument, always Submitter today)
- Expected recipient = profile email for To role (`SubmitterEmail` / `FedleadEmail` / `DcpEmail` / `AdminEmail`)
- Environment from `G_Urlname` (`QA` / `QA2` / `STAGE`; `hub-qa2` is checked before `hub-qa`)
- Name = study title or DS display name
- Result left blank

SR Submit writes **two** rows (Submitter + Fedlead). DS Create and DS Submit write one each.

Unknown Trigger → test fails. New mail after the same click = another catalog row with the same Trigger. New UI action = a new Trigger value plus one `logEmails` call.

`appendFromCatalog` does the lookup. `appendRow` writes one Excel row. Tests only call `logEmails`.

## 5. Checklist

Append-only `OutputFiles/NotificationEmail_Checklist.xlsx`. UI tests never open Gmail.

## 6. Verifier (run after mail should be in the inbox)

`PythonFiles/CRDC/VerifyNotificationEmails.py` against `qa@essential-soft.com` (plus-aliases like `qa+crdc-automation-submitter@…` land in the same mailbox).

For each of **today’s** checklist rows that are still blank or Fail it:

1. Searches Gmail: `newer_than:2d subject:"[QA] <expected subject>"` (or `[QA2]` / `[STAGE]`) so a QA run cannot Pass STAGE mail. The Name is not in the Gmail search string.
2. Walks up to 20 hits until one message has **all** of:
   - Subject starts with that env tag and contains the expected title
   - **To** contains the expected address (not Cc/Bcc)
   - Body contains **Name** only if the catalog says Name in body = TRUE

Writes **Pass** or **Fail**. Re-run retries Fail/blank only; Pass/Skip stay. Console on Pass includes the To: address. A blank Environment cell Fails without searching Gmail.

## 7. How you run it

1. Refresh Katalon Data File `CRDC/Pbac/Permissions` if you changed Trigger/subject/To role/Name in body.
2. Run TC01 and/or TC02 (profile `CRDC_QA` / `CRDC_QA2` / `CRDC_STAGE`).
3. When mail should already be there, from the project venv:

```bash
python PythonFiles/CRDC/VerifyNotificationEmails.py
```

Override with `--excel`, `--date`, `--credentials`, `--token`, or env `GMAIL_CREDENTIALS_PATH` / `GMAIL_TOKEN_PATH`.

That is the whole loop: catalog defines mail, UI tests only log after a real submit, Gmail judge scores the sheet.

## Auth files (not committed)

Drop copies of these files in this folder (same files used by the local Gmail API quickstart):

- `credentials.json` — OAuth client from Google Cloud (Desktop app)
- `token.json` — refresh token after one browser login as `qa@essential-soft.com`

Ask a teammate who already authorized the inbox for a copy. Do not commit either file. The repo ignores `PythonFiles/CRDC/gmail/*.json`.
