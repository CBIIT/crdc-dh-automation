#!/usr/bin/env python3
"""Verify NotificationEmail_Checklist.xlsx rows against the QA Gmail inbox.

Katalon (or later Playwright) UI tests append expected-email rows to the Excel
checklist. This script is the inbox judge: it does not send mail and does not
drive the browser.

How a row is judged
1. Keep rows whose Timestamp starts with --date (default: today).
2. Search Gmail by Environment-prefixed subject, recency, and the unique Name
   when Permissions (InputFiles/CRDC/PBACPermissions.xlsx) says Name in body is
   TRUE. A subject-only fallback still classifies a missing-Name Fail.
3. For each candidate, require the Subject to start with [QA], [QA2], or [STAGE]
   from the Environment column, to contain the expected subject phrase, Name in
   the body when the catalog says TRUE, and Expected body contains when that
   checklist cell is non-blank. To rows also require Expected recipient in the
   To header only (not Cc or Bcc). Bcc rows search the mailbox whose token
   profile email matches Expected recipient, and require the recipient not in
   To or Cc. Cc rows search that same mailbox and require the recipient in Cc
   and not in To.
4. Write Pass or Fail into the Result column once. Run this after the
   UI tests when mail should already be in the inbox. A later re-run retries only
   blank and Fail rows; Pass and Skip are left unchanged. Rows with a blank
   Environment cell Fail without searching Gmail.

Auth files (gitignored) live in PythonFiles/CRDC/gmail/:
  token.json               refresh token for qa@essential-soft.com
  token-fedlead-bcc.json   refresh token for the Fedlead BCC Gmail inbox
  token-dcp-bcc.json       refresh token for the DCP BCC Gmail inbox
  token-admin-bcc.json     refresh token for the Admin BCC Gmail inbox
credentials.json is only for the one-time browser login that creates those tokens.
Plus-aliases (qa+crdc-automation-submitter@...) land in the To mailbox.
Bcc rows search the matching BCC inbox; the address must not appear in To or Cc.
Cc on FedleadCc/AdminCc searches the Fedlead or Admin BCC inbox. Cc on
SubmitterCc (DS Config Change) searches the qa@ To mailbox; the address must be
in Cc and not in To.
"""
from __future__ import annotations

import argparse
import base64
import os
import sys
import time
from datetime import date
from pathlib import Path

from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from openpyxl import load_workbook

import urllib.error

from email_git_parity import (
	check_trigger_parity,
	git_parity_skip_reason,
	load_email_constants,
	load_mapping,
)

# Read-only Gmail scope. If you change this, delete token.json and re-authorize.
SCOPES = ["https://www.googleapis.com/auth/gmail.readonly"]
SHEET_NAME = "Checklist"
# 1-based Excel columns written by NotificationEmailLog.groovy
COL_TIMESTAMP = 1
COL_NAME = 6
COL_SUBJECT = 7
COL_RECIPIENT = 8
COL_RESULT = 10
COL_ENV = 11
COL_BODY_CONTAINS = 12
COL_RECIPIENT_KIND = 13
COL_ACTION = 3
COL_ID = 5
COL_GIT_PARITY = 14
GIT_PARITY_HEADER = "Git parity"
CATALOG_SHEET = "Permissions"

# This file is PythonFiles/CRDC/VerifyNotificationEmails.py
# parents[0] = PythonFiles/CRDC, parents[1] = repo root
SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parents[1]
DEFAULT_MAPPING = REPO_ROOT / "InputFiles" / "CRDC" / "email_git_parity_triggers.yaml"
DEFAULT_EMAIL_BRANCH = os.environ.get("CRDC_EMAIL_CONTENT_BRANCH", "qa")
DEFAULT_EXCEL = REPO_ROOT / "OutputFiles" / "NotificationEmail_Checklist.xlsx"
DEFAULT_TOKEN = SCRIPT_DIR / "gmail" / "token.json"
DEFAULT_TOKEN_FEDLEAD_BCC = SCRIPT_DIR / "gmail" / "token-fedlead-bcc.json"
DEFAULT_TOKEN_DCP_BCC = SCRIPT_DIR / "gmail" / "token-dcp-bcc.json"
DEFAULT_TOKEN_ADMIN_BCC = SCRIPT_DIR / "gmail" / "token-admin-bcc.json"
DEFAULT_CATALOG = REPO_ROOT / "InputFiles" / "CRDC" / "PBACPermissions.xlsx"
RATE_LIMIT_SLEEP_SEC = 60
RATE_LIMIT_MAX_ATTEMPTS = 5


def body_text(payload):
	"""Return concatenated text/html from a Gmail message payload.

	Gmail payloads are a tree of MIME parts (multipart/alternative, nested
	multipart/mixed, etc.). Walk that tree and base64url-decode every part that
	has body.data. find_message searches this string for the Excel Name.
	"""
	texts = []
	stack = list(payload.get("parts") or [payload])
	while stack:
		part = stack.pop()
		stack.extend(part.get("parts") or [])
		data = part.get("body", {}).get("data")
		if data:
			texts.append(base64.urlsafe_b64decode(data).decode("utf-8", "replace"))
	return "\n".join(texts)


def cell_str(row, col):
	"""Read one openpyxl row tuple as a stripped string.

	row is the tuple from sheet.iter_rows. col is 1-based (Excel column A = 1).
	Missing or empty cells become "".
	"""
	value = row[col - 1].value if col <= len(row) else None
	if value is None:
		return ""
	return str(value).strip()


def load_creds(token_path):
	"""Load Gmail OAuth credentials from the user token JSON.

	token.json, token-fedlead-bcc.json, token-dcp-bcc.json, or token-admin-bcc.json already has
	client_id, client_secret, and refresh_token from the one-time browser login.
	If the access token is expired, refresh it and rewrite the token file. Does
	not open a browser and does not read credentials.json.
	"""
	creds = None
	if Path(token_path).is_file():
		creds = Credentials.from_authorized_user_file(token_path, SCOPES)
	if not creds or not creds.valid:
		if creds and creds.expired and creds.refresh_token:
			creds.refresh(Request())
			Path(token_path).write_text(creds.to_json())
		else:
			raise FileNotFoundError(
				"Missing or invalid Gmail token at %s. Authorize once and drop the token JSON in PythonFiles/CRDC/gmail/."
				% token_path
			)
	return creds


def env_tag(env):
	"""Return the subject prefix token, e.g. QA -> [QA]. Empty env yields empty string."""
	token = (env or "").strip()
	if not token:
		return ""
	return "[%s]" % token


def subject_matches(expected, actual, env):
	"""True if actual Subject has this env prefix and the expected phrase.

	The tag must be followed by a space so [QA] does not match [QA2].
	Example: env QA, expected New Data Submission Created for GC matches
	[QA] New Data Submission Created for GC and not [STAGE] ... or [QA2] ....
	"""
	if not expected or not env:
		return False
	actual_cf = (actual or "").casefold()
	tag = env_tag(env).casefold()
	if not actual_cf.startswith(tag + " "):
		return False
	return expected.casefold() in actual_cf


def to_matches(expected_recipient, actual_to):
	"""True if the expected To: address appears in the Gmail To header.

	Case-insensitive substring so display names still match, e.g.
	qa+crdc-automation-fedlead@essential-soft.com inside
	Federal Lead <qa+crdc-automation-fedlead@essential-soft.com>.
	Does not inspect Cc or Bcc.
	"""
	if not expected_recipient:
		return False
	return expected_recipient.casefold() in (actual_to or "").casefold()


def catalog_truthy(value):
	"""TRUE/YES/1 or Excel boolean True. Missing or FALSE means do not require Name in body."""
	if isinstance(value, bool):
		return value
	return str(value or "").strip().upper() in ("TRUE", "YES", "1")


def load_name_in_body_map(catalog_path):
	"""Map expected-subject (casefold) to Name in body from Permissions email rows."""
	path = Path(catalog_path)
	if not path.is_file():
		print("Permissions catalog not found:", path)
		return {}
	workbook = load_workbook(path, read_only=True, data_only=True)
	try:
		if CATALOG_SHEET not in workbook.sheetnames:
			print("Sheet %s not found in %s" % (CATALOG_SHEET, path))
			return {}
		sheet = workbook[CATALOG_SHEET]
		rows = list(sheet.iter_rows(values_only=True))
	finally:
		workbook.close()
	if not rows:
		return {}
	headers = [(h or "").strip() for h in rows[0]]
	try:
		subject_idx = headers.index("Expected email subject")
		name_idx = headers.index("Name in body")
	except ValueError:
		print("Permissions sheet is missing Expected email subject or Name in body")
		return {}
	mapping = {}
	for row in rows[1:]:
		if not row or subject_idx >= len(row):
			continue
		subject = str(row[subject_idx] or "").strip()
		if not subject:
			continue
		name_val = row[name_idx] if name_idx < len(row) else None
		require_name = catalog_truthy(name_val)
		for token in subject.split("|"):
			token = token.strip()
			if token:
				mapping[token.casefold()] = require_name
	return mapping


def requires_name_in_body(expected_subject, name_in_body_map):
	"""True when the catalog says this subject includes Name in the body.

	Unknown subjects default to True so a missing catalog row still checks Name.
	"""
	if not expected_subject:
		return False
	return name_in_body_map.get(expected_subject.casefold(), True)


def gmail_query(expected_subject, env, name=""):
	"""Build a Gmail search q for recent mail with this env-prefixed subject.

	Quotes [QA] phrase (or [QA2] / [STAGE]) as one subject string so a QA row
	does not match STAGE or QA2 mail. When name is non-blank, quote it too so
	Gmail matches that study or submission name in the body. Otherwise the
	newest 20 same-subject hits can be a later run and miss this row.
	"""
	escaped = expected_subject.replace('"', '\\"')
	prefixed = "%s %s" % (env_tag(env), escaped)
	q = 'newer_than:2d subject:"%s"' % prefixed
	if name:
		q += ' "%s"' % name.replace('"', '\\"')
	return q


def is_rate_limit(error):
	"""True for 403/429 Gmail quota errors (rateLimitExceeded or userRateLimitExceeded)."""
	status = int(getattr(getattr(error, "resp", None), "status", 0) or 0)
	if status not in (403, 429):
		return False
	text = str(error).casefold().replace("_", "").replace("-", "")
	return "ratelimitexceeded" in text or "userratelimitexceeded" in text


def gmail_execute(request):
	"""Run one Gmail API request. Sleep 60s and retry on per-user quota.

	list and get cost quota units. This project's per-user minute cap can be
	250 units; a full To+Bcc Verify can hit it. Do not treat the first 403 as
	missing mail.
	"""
	last_error = None
	for attempt in range(1, RATE_LIMIT_MAX_ATTEMPTS + 1):
		try:
			return request.execute()
		except HttpError as error:
			last_error = error
			if not is_rate_limit(error) or attempt == RATE_LIMIT_MAX_ATTEMPTS:
				raise
			print("Gmail rate limit (attempt %s/%s); waiting %ss" % (
				attempt, RATE_LIMIT_MAX_ATTEMPTS, RATE_LIMIT_SLEEP_SEC))
			time.sleep(RATE_LIMIT_SLEEP_SEC)
	raise last_error


def header_value(headers, name):
	"""Return a header value with a case-insensitive name lookup."""
	needle = (name or "").casefold()
	for key, value in headers.items():
		if (key or "").casefold() == needle:
			return value or ""
	return ""


def find_message(service, expected_subject, name, env, recipient, name_in_body_map, body_contains="",
		recipient_kind="To"):
	"""Return (actual Subject, None, body_text) if a match exists, else (None, reason, None).

	Lists up to 20 recent messages for the env-prefixed subject. When the catalog
	requires Name in the body, the Gmail q also quotes Name so a later same-subject
	mail does not hide this row. If that returns nothing, list subject-only so a
	real missing Name still fails as name rather than no_mail. Then load each full
	MIME payload and require subject_matches, Name in the body when the catalog
	says so, and body_contains when that checklist cell is non-blank. To rows
	require Expected recipient in the To header. Bcc rows require the message in
	this mailbox and the recipient not in To or Cc. Cc rows require the recipient
	in Cc and not in To.

	reason is no_mail, subject, name, body, to, bcc_in_headers, cc, or cc_in_to.
	After a subject match, later misses replace earlier ones so Fail text names
	the check that actually missed.
	"""
	need_name = requires_name_in_body(expected_subject, name_in_body_map) and bool(name)
	result = gmail_execute(service.users().messages().list(
		userId="me", q=gmail_query(expected_subject, env, name if need_name else ""), maxResults=20
	))
	kind = (recipient_kind or "To").casefold()
	messages = result.get("messages") or []
	if not messages and need_name:
		result = gmail_execute(service.users().messages().list(
			userId="me", q=gmail_query(expected_subject, env), maxResults=20
		))
		messages = result.get("messages") or []
	if not messages:
		return None, "no_mail", None
	best_reason = "subject"
	for m in messages:
		full = gmail_execute(service.users().messages().get(userId="me", id=m["id"], format="full"))
		headers = {h["name"]: h["value"] for h in full.get("payload", {}).get("headers", [])}
		actual_subject = header_value(headers, "Subject")
		if not subject_matches(expected_subject, actual_subject, env):
			continue
		text = body_text(full.get("payload") or {})
		if requires_name_in_body(expected_subject, name_in_body_map) and name and name not in text:
			best_reason = "name"
			continue
		if body_contains and body_contains not in text:
			best_reason = "body"
			continue
		actual_to = header_value(headers, "To")
		actual_cc = header_value(headers, "Cc")
		if kind == "bcc":
			if to_matches(recipient, actual_to) or to_matches(recipient, actual_cc):
				best_reason = "bcc_in_headers"
				continue
			return actual_subject, None, text
		if kind == "cc":
			if to_matches(recipient, actual_to):
				best_reason = "cc_in_to"
				continue
			if not to_matches(recipient, actual_cc):
				best_reason = "cc"
				continue
			return actual_subject, None, text
		if not to_matches(recipient, actual_to):
			best_reason = "to"
			continue
		return actual_subject, None, text
	return None, best_reason, None


def parse_args():
	"""CLI flags. Paths may also come from GMAIL_TOKEN_PATH / GMAIL_TOKEN_FEDLEAD_BCC_PATH / GMAIL_TOKEN_DCP_BCC_PATH / GMAIL_TOKEN_ADMIN_BCC_PATH."""
	parser = argparse.ArgumentParser(description="Verify today's notification checklist rows in Gmail.")
	parser.add_argument("--excel", default=str(DEFAULT_EXCEL), help="Checklist workbook path")
	parser.add_argument("--date", default=date.today().isoformat(), help="Row date prefix YYYY-MM-DD")
	parser.add_argument(
		"--token",
		default=os.environ.get("GMAIL_TOKEN_PATH", str(DEFAULT_TOKEN)),
		help="OAuth user token JSON for the To: mailbox (token.json)",
	)
	parser.add_argument(
		"--token-fedlead-bcc",
		default=os.environ.get("GMAIL_TOKEN_FEDLEAD_BCC_PATH", str(DEFAULT_TOKEN_FEDLEAD_BCC)),
		help="OAuth user token JSON for the Fedlead BCC inbox (token-fedlead-bcc.json)",
	)
	parser.add_argument(
		"--token-dcp-bcc",
		default=os.environ.get("GMAIL_TOKEN_DCP_BCC_PATH", str(DEFAULT_TOKEN_DCP_BCC)),
		help="OAuth user token JSON for the DCP BCC inbox (token-dcp-bcc.json)",
	)
	parser.add_argument(
		"--token-admin-bcc",
		default=os.environ.get("GMAIL_TOKEN_ADMIN_BCC_PATH", str(DEFAULT_TOKEN_ADMIN_BCC)),
		help="OAuth user token JSON for the Admin BCC inbox (token-admin-bcc.json)",
	)
	parser.add_argument(
		"--git-parity",
		action="store_true",
		help="After Gmail Pass, compare allowlisted To rows to Git notification_email_values.yaml",
	)
	parser.add_argument(
		"--email-content-branch",
		default=os.environ.get("CRDC_EMAIL_CONTENT_BRANCH", DEFAULT_EMAIL_BRANCH),
		help="Git branch for crdc-submission-portal-email-content (default qa)",
	)
	parser.add_argument(
		"--email-content-dir",
		default=os.environ.get("CRDC_EMAIL_CONTENT_DIR", ""),
		help="Local clone path; if set, skip GitHub raw fetch",
	)
	parser.add_argument(
		"--portal-url",
		default=os.environ.get("CRDC_PORTAL_URL", ""),
		help="Portal base URL for $url substitution (Katalon G_Urlname)",
	)
	parser.add_argument(
		"--git-parity-mapping",
		default=str(DEFAULT_MAPPING),
		help="Path to email_git_parity_triggers.yaml",
	)
	return parser.parse_args()


def load_bcc_services(token_paths):
	"""Authorize each BCC token once. Map profile email (casefold) to Gmail service.

	Skip missing files. Do not search one mailbox for another recipient; rows
	are grouped by Expected recipient and matched to the token whose profile
	email equals that address.
	"""
	services = {}
	for token_path in token_paths:
		path = Path(token_path)
		if not path.is_file():
			print("BCC token not found:", path)
			continue
		creds = load_creds(str(path))
		service = build("gmail", "v1", credentials=creds)
		email = (gmail_execute(service.users().getProfile(userId="me")).get("emailAddress") or "").strip()
		if not email:
			print("BCC token at %s has no profile email" % path)
			continue
		print("Authorized BCC Gmail as", email)
		services[email.casefold()] = service
	return services


def fail_detail(item, reason):
	"""One Fail clause for the check that missed. reason comes from find_message."""
	prefixed = "%s %s" % (env_tag(item["env"]), item["subject"])
	kind = (item.get("kind") or "To").casefold()
	if reason == "to":
		return "To does not contain %r" % item["recipient"]
	if reason == "cc":
		return "Cc does not contain %r" % item["recipient"]
	if reason == "cc_in_to":
		return "Cc address %r is in To" % item["recipient"]
	if reason == "name":
		if kind in ("bcc", "cc"):
			label = "Bcc" if kind == "bcc" else "Cc"
			return "missing Name %r in body of %s inbox %r" % (item["name"], label, item["recipient"])
		return "missing Name %r in body" % item["name"]
	if reason == "body":
		if kind in ("bcc", "cc"):
			label = "Bcc" if kind == "bcc" else "Cc"
			return "missing %r in body of %s inbox %r" % (item["body_contains"], label, item["recipient"])
		return "missing %r in body" % item["body_contains"]
	if reason == "bcc_in_headers":
		return "Bcc address %r is in To or Cc" % item["recipient"]
	if kind in ("bcc", "cc"):
		label = "Bcc" if kind == "bcc" else "Cc"
		return "no mail in %s inbox %r with subject containing %r" % (label, item["recipient"], prefixed)
	return "no mail with subject containing %r" % prefixed


def ensure_git_parity_header(sheet):
	"""Add Git parity column header on row 1 when missing."""
	header_row = sheet[1]
	existing = cell_str(header_row, COL_GIT_PARITY) if header_row else ""
	if existing == GIT_PARITY_HEADER:
		return
	sheet.cell(1, COL_GIT_PARITY).value = GIT_PARITY_HEADER


def resolve_email_branch(env):
	"""Map checklist Environment column to crdc-submission-portal-email-content branch (qa, qa2, stage)."""
	token = (env or "").strip().casefold()
	if token == "qa2":
		return "qa2"
	if token == "stage":
		return "stage"
	if token == "qa":
		return "qa"
	return DEFAULT_EMAIL_BRANCH


def run_git_parity_pass(args, sheet, rows, to_service, name_in_body_map):
	"""Second pass after Gmail judge: Git parity column for allowlisted To rows already Pass.

	Re-fetches each message body from the To Gmail service, loads Git YAML per row
	Environment, and calls email_git_parity.check_trigger_parity. Does not change Result.
	"""
	ensure_git_parity_header(sheet)
	mapping = load_mapping(Path(args.git_parity_mapping))
	content_dir = args.email_content_dir.strip() if args.email_content_dir else ""
	constants_by_branch = {}

	def constants_for_env(env):
		"""Cache notification_email_values.yaml per Git branch (one fetch per QA/QA2/STAGE)."""
		branch = resolve_email_branch(env)
		if branch not in constants_by_branch:
			try:
				constants_by_branch[branch] = load_email_constants(content_dir or None, branch, mapping)
			except (urllib.error.URLError, FileNotFoundError, OSError) as err:
				raise RuntimeError("Git parity: failed to load email content for branch %s: %s" % (branch, err))
		return constants_by_branch[branch]

	failures = 0
	for item in rows:
		result = sheet.cell(item["idx"], COL_RESULT).value
		if str(result or "").strip() != "Pass":
			continue
		skip = git_parity_skip_reason(item.get("action") or "", item.get("kind") or "To", mapping)
		if skip:
			sheet.cell(item["idx"], COL_GIT_PARITY).value = "Skip"
			print("Git parity Skip row %s | %s" % (item["idx"], skip))
			continue
		if not to_service:
			sheet.cell(item["idx"], COL_GIT_PARITY).value = "Fail"
			print("Git parity Fail row %s | no To Gmail service" % item["idx"])
			failures += 1
			continue
		try:
			actual, miss, body_text = find_message(
				to_service, item["subject"], item["name"], item["env"], item["recipient"],
				name_in_body_map, item["body_contains"], item.get("kind") or "To")
		except HttpError as error:
			sheet.cell(item["idx"], COL_GIT_PARITY).value = "Fail"
			print("Git parity Fail row %s | Gmail API %s" % (item["idx"], error))
			failures += 1
			continue
		if not actual or not body_text:
			sheet.cell(item["idx"], COL_GIT_PARITY).value = "Fail"
			print("Git parity Fail row %s | %s" % (item["idx"], fail_detail(item, miss)))
			failures += 1
			continue
		try:
			constants = constants_for_env(item["env"])
		except RuntimeError as err:
			sheet.cell(item["idx"], COL_GIT_PARITY).value = "Fail"
			print("Git parity Fail row %s | %s" % (item["idx"], err))
			failures += 1
			continue
		checklist = {"name": item["name"], "id": item.get("id") or ""}
		ok, detail = check_trigger_parity(
			item.get("action") or "",
			mapping,
			constants,
			body_text,
			checklist,
			item["env"],
			args.portal_url or "",
		)
		if ok:
			sheet.cell(item["idx"], COL_GIT_PARITY).value = "Pass"
			print("Git parity Pass row %s | %s" % (item["idx"], item.get("action")))
		else:
			sheet.cell(item["idx"], COL_GIT_PARITY).value = "Fail"
			print("Git parity Fail row %s | %s" % (item["idx"], detail))
			failures += 1
	return failures


def judge_search_rows(service, items, sheet, name_in_body_map):
	"""Search one Gmail service for checklist rows. Write Pass or Fail. Return Fail count.

	service is already authorized for the mailbox that should contain these rows
	(To: inbox for To rows, matching Fedlead/DCP/Admin inbox for Bcc and Cc rows).
	"""
	failures = 0
	for item in items:
		kind = item["kind"]
		rate_limited = False
		miss = None
		try:
			actual, miss, _body = find_message(
				service, item["subject"], item["name"], item["env"], item["recipient"],
				name_in_body_map, item["body_contains"], kind)
		except HttpError as error:
			print("Gmail API error for row %s: %s" % (item["idx"], error))
			actual = None
			miss = None
			rate_limited = is_rate_limit(error)
		if actual:
			sheet.cell(item["idx"], COL_RESULT).value = "Pass"
			kind_cf = kind.casefold()
			if kind_cf == "bcc":
				label = "Bcc inbox"
			elif kind_cf == "cc":
				label = "Cc inbox"
			else:
				label = "To"
			if item["body_contains"]:
				print("Pass row %s | %s | %s: %s | %s | body: %r" % (
					item["idx"], item["name"], label, item["recipient"], actual, item["body_contains"]))
			else:
				print("Pass row %s | %s | %s: %s | %s" % (
					item["idx"], item["name"], label, item["recipient"], actual))
		else:
			sheet.cell(item["idx"], COL_RESULT).value = "Fail"
			if rate_limited:
				print("Fail row %s | Gmail rate limit after retries" % item["idx"])
			else:
				print("Fail row %s | %s" % (item["idx"], fail_detail(item, miss)))
			failures += 1
	return failures


def main():
	"""Load today's checklist rows, search Gmail once, write Result, return exit code.

	Run after the UI tests when mail should already be in the inbox. Re-running
	retries blank and Fail rows only; Pass and Skip are left unchanged.
	Exit 0 if every row for the date is Pass or Skip (or there are no rows).
	Exit 1 if the workbook is missing or any row is still Fail.
	"""
	args = parse_args()
	excel_path = Path(args.excel)
	if not excel_path.is_file():
		print("Checklist not found:", excel_path)
		return 1

	workbook = load_workbook(excel_path)
	if SHEET_NAME not in workbook.sheetnames:
		print("Sheet %s not found in %s" % (SHEET_NAME, excel_path))
		return 1
	sheet = workbook[SHEET_NAME]

	# Collect only rows for --date. Older checklist history is left untouched.
	rows = []
	for idx, row in enumerate(sheet.iter_rows(min_row=2), start=2):
		timestamp = cell_str(row, COL_TIMESTAMP)
		if not timestamp.startswith(args.date):
			continue
		rows.append({
			"idx": idx,
			"timestamp": timestamp,
			"action": cell_str(row, COL_ACTION),
			"id": cell_str(row, COL_ID),
			"name": cell_str(row, COL_NAME),
			"subject": cell_str(row, COL_SUBJECT),
			"recipient": cell_str(row, COL_RECIPIENT),
			"manual": cell_str(row, COL_RESULT),
			"env": cell_str(row, COL_ENV),
			"body_contains": cell_str(row, COL_BODY_CONTAINS),
			"kind": cell_str(row, COL_RECIPIENT_KIND) or "To",
		})

	if not rows:
		print("No checklist rows for date", args.date)
		return 0

	name_in_body_map = load_name_in_body_map(DEFAULT_CATALOG)

	pending = []
	for item in rows:
		if item["manual"] in ("Pass", "Skip"):
			print("Leave row %s (%s)" % (item["idx"], item["manual"]))
			continue
		pending.append(item)

	failures = 0
	to_search = []
	bcc_search = []
	has_bcc_or_cc = any(
		item["env"] and item["recipient"] and item["kind"].casefold() in ("bcc", "cc")
		for item in pending
	)
	services_by_email = {}
	if has_bcc_or_cc:
		services_by_email = load_bcc_services([
			args.token_fedlead_bcc, args.token_dcp_bcc, args.token_admin_bcc])
	bcc_inbox_emails = set(services_by_email.keys())
	for item in pending:
		if not item["env"]:
			sheet.cell(item["idx"], COL_RESULT).value = "Fail"
			print("Fail row %s | missing Environment (QA, QA2, or STAGE)" % item["idx"])
			failures += 1
		elif not item["recipient"]:
			sheet.cell(item["idx"], COL_RESULT).value = "Fail"
			kind_cf = item["kind"].casefold()
			if kind_cf == "bcc":
				kind_label = "Bcc inbox"
			elif kind_cf == "cc":
				kind_label = "Cc inbox"
			else:
				kind_label = "To: address"
			print("Fail row %s | missing Expected recipient (%s)" % (item["idx"], kind_label))
			failures += 1
		else:
			kind_cf = item["kind"].casefold()
			if kind_cf == "bcc":
				bcc_search.append(item)
			elif kind_cf == "cc":
				if item["recipient"].casefold() in bcc_inbox_emails:
					bcc_search.append(item)
				else:
					to_search.append(item)
			else:
				to_search.append(item)

	to_service = None
	if to_search:
		creds = load_creds(args.token)
		to_service = build("gmail", "v1", credentials=creds)
		print("Authorized Gmail as", gmail_execute(to_service.users().getProfile(userId="me")).get("emailAddress"))
		failures += judge_search_rows(to_service, to_search, sheet, name_in_body_map)
	elif args.git_parity:
		try:
			creds = load_creds(args.token)
			to_service = build("gmail", "v1", credentials=creds)
			print("Authorized Gmail as", gmail_execute(to_service.users().getProfile(userId="me")).get("emailAddress"))
		except FileNotFoundError as err:
			print(err)
			failures += 1

	if bcc_search:
		by_recipient = {}
		for item in bcc_search:
			by_recipient.setdefault(item["recipient"].casefold(), []).append(item)
		for recipient_key, items in by_recipient.items():
			service_bcc = services_by_email.get(recipient_key)
			if not service_bcc:
				for item in items:
					sheet.cell(item["idx"], COL_RESULT).value = "Fail"
					print("Fail row %s | no BCC token for inbox %r" % (item["idx"], item["recipient"]))
					failures += 1
				continue
			failures += judge_search_rows(service_bcc, items, sheet, name_in_body_map)

	if args.git_parity:
		git_failures = run_git_parity_pass(args, sheet, rows, to_service, name_in_body_map)
		failures += git_failures

	workbook.save(excel_path)
	print("Updated", excel_path)
	return 1 if failures else 0


if __name__ == "__main__":
	sys.exit(main())
