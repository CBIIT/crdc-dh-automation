#!/usr/bin/env python3
"""Verify NotificationEmail_Checklist.xlsx rows against the QA Gmail inbox.

Katalon (or later Playwright) UI tests append expected-email rows to the Excel
checklist. This script is the inbox judge: it does not send mail and does not
drive the browser.

How a row is judged
1. Keep rows whose Timestamp starts with --date (default: today).
2. Search Gmail by Environment-prefixed subject and recency. Do not put the Name
   in the Gmail q string. Name-in-body is required when Permissions
   (InputFiles/CRDC/PBACPermissions.xlsx) says TRUE for that expected subject.
3. For each candidate, require the Subject to start with [QA], [QA2], or [STAGE]
   from the Environment column, to contain the expected subject phrase, Name in
   the body when the catalog says TRUE, and Expected body contains when that
   checklist cell is non-blank. To rows also require Expected recipient in the
   To header only (not Cc or Bcc). Bcc rows search the Fedlead BCC mailbox and
   require the recipient not in To or Cc.
4. Write Pass or Fail into the Result column once. Run this after the
   UI tests when mail should already be in the inbox. A later re-run retries only
   blank and Fail rows; Pass and Skip are left unchanged. Rows with a blank
   Environment cell Fail without searching Gmail.

Auth files (gitignored) live in PythonFiles/CRDC/gmail/:
  token.json               refresh token for qa@essential-soft.com
  token-fedlead-bcc.json   refresh token for the Fedlead BCC Gmail inbox
credentials.json is only for the one-time browser login that creates those tokens.
Plus-aliases (qa+crdc-automation-submitter@...) land in the To mailbox.
Bcc rows search the Fedlead BCC inbox; the address must not appear in To or Cc.
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
CATALOG_SHEET = "Permissions"

# This file is PythonFiles/CRDC/VerifyNotificationEmails.py
# parents[0] = PythonFiles/CRDC, parents[1] = repo root
SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parents[1]
DEFAULT_EXCEL = REPO_ROOT / "OutputFiles" / "NotificationEmail_Checklist.xlsx"
DEFAULT_TOKEN = SCRIPT_DIR / "gmail" / "token.json"
DEFAULT_TOKEN_BCC = SCRIPT_DIR / "gmail" / "token-fedlead-bcc.json"
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

	token.json (or token-fedlead-bcc.json) already has client_id, client_secret,
	and refresh_token from the one-time browser login. If the access token is
	expired, refresh it and rewrite the token file. Does not open a browser and
	does not read credentials.json.
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


def gmail_query(expected_subject, env):
	"""Build a Gmail search q for recent mail with this env-prefixed subject.

	Quotes [QA] phrase (or [QA2] / [STAGE]) as one subject string so a QA row
	does not match STAGE or QA2 mail. Does not include submission names.
	"""
	escaped = expected_subject.replace('"', '\\"')
	prefixed = "%s %s" % (env_tag(env), escaped)
	return 'newer_than:2d subject:"%s"' % prefixed


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
	"""Return the actual Subject if a matching message exists, else None.

	Lists up to 20 recent messages for the env-prefixed subject, loads each full
	MIME payload, then requires subject_matches, Name in the body when the
	Permissions catalog says so, and body_contains when that checklist cell is
	non-blank. To rows require Expected recipient in the To header. Bcc rows
	require the message in this mailbox and the recipient not in To or Cc.
	"""
	result = gmail_execute(service.users().messages().list(
		userId="me", q=gmail_query(expected_subject, env), maxResults=20
	))
	kind = (recipient_kind or "To").casefold()
	for m in result.get("messages") or []:
		full = gmail_execute(service.users().messages().get(userId="me", id=m["id"], format="full"))
		headers = {h["name"]: h["value"] for h in full.get("payload", {}).get("headers", [])}
		actual_subject = header_value(headers, "Subject")
		if not subject_matches(expected_subject, actual_subject, env):
			continue
		text = body_text(full.get("payload") or {})
		if requires_name_in_body(expected_subject, name_in_body_map) and name and name not in text:
			continue
		if body_contains and body_contains not in text:
			continue
		actual_to = header_value(headers, "To")
		actual_cc = header_value(headers, "Cc")
		if kind == "bcc":
			if to_matches(recipient, actual_to) or to_matches(recipient, actual_cc):
				continue
			return actual_subject
		if not to_matches(recipient, actual_to):
			continue
		return actual_subject
	return None


def parse_args():
	"""CLI flags. Paths may also come from GMAIL_TOKEN_PATH / GMAIL_TOKEN_BCC_PATH."""
	parser = argparse.ArgumentParser(description="Verify today's notification checklist rows in Gmail.")
	parser.add_argument("--excel", default=str(DEFAULT_EXCEL), help="Checklist workbook path")
	parser.add_argument("--date", default=date.today().isoformat(), help="Row date prefix YYYY-MM-DD")
	parser.add_argument(
		"--token",
		default=os.environ.get("GMAIL_TOKEN_PATH", str(DEFAULT_TOKEN)),
		help="OAuth user token JSON for the To: mailbox (token.json)",
	)
	parser.add_argument(
		"--token-bcc",
		default=os.environ.get("GMAIL_TOKEN_BCC_PATH", str(DEFAULT_TOKEN_BCC)),
		help="OAuth user token JSON for the Fedlead BCC inbox (token-fedlead-bcc.json)",
	)
	return parser.parse_args()


def judge_search_rows(service, items, sheet, name_in_body_map):
	"""Search one Gmail service for checklist rows. Write Pass or Fail. Return Fail count.

	service is already authorized for the mailbox that should contain these rows
	(To: inbox for To rows, Fedlead BCC inbox for Bcc rows).
	"""
	failures = 0
	for item in items:
		kind = item["kind"]
		rate_limited = False
		try:
			actual = find_message(
				service, item["subject"], item["name"], item["env"], item["recipient"],
				name_in_body_map, item["body_contains"], kind)
		except HttpError as error:
			print("Gmail API error for row %s: %s" % (item["idx"], error))
			actual = None
			rate_limited = is_rate_limit(error)
		if actual:
			sheet.cell(item["idx"], COL_RESULT).value = "Pass"
			label = "Bcc inbox" if kind.casefold() == "bcc" else "To"
			if item["body_contains"]:
				print("Pass row %s | %s | %s: %s | %s | body: %r" % (
					item["idx"], item["name"], label, item["recipient"], actual, item["body_contains"]))
			else:
				print("Pass row %s | %s | %s: %s | %s" % (
					item["idx"], item["name"], label, item["recipient"], actual))
		else:
			sheet.cell(item["idx"], COL_RESULT).value = "Fail"
			prefixed = "%s %s" % (env_tag(item["env"]), item["subject"])
			if rate_limited:
				print("Fail row %s | Gmail rate limit after retries" % item["idx"])
			elif kind.casefold() == "bcc":
				if item["body_contains"]:
					print("Fail row %s | missing %r in body of Bcc inbox %r for subject containing %r" % (
						item["idx"], item["body_contains"], item["recipient"], prefixed))
				elif requires_name_in_body(item["subject"], name_in_body_map):
					print("Fail row %s | missing %r in body of Bcc inbox %r for subject containing %r" % (
						item["idx"], item["name"], item["recipient"], prefixed))
				else:
					print("Fail row %s | no mail in Bcc inbox %r with subject containing %r" % (
						item["idx"], item["recipient"], prefixed))
			elif item["body_contains"]:
				print("Fail row %s | missing %r in body or To %r for subject containing %r" % (
					item["idx"], item["body_contains"], item["recipient"], prefixed))
			elif requires_name_in_body(item["subject"], name_in_body_map):
				print("Fail row %s | missing %r in body or To %r for subject containing %r" % (
					item["idx"], item["name"], item["recipient"], prefixed))
			else:
				print("Fail row %s | no mail with To %r and subject containing %r" % (
					item["idx"], item["recipient"], prefixed))
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
	for item in pending:
		if not item["env"]:
			sheet.cell(item["idx"], COL_RESULT).value = "Fail"
			print("Fail row %s | missing Environment (QA, QA2, or STAGE)" % item["idx"])
			failures += 1
		elif not item["recipient"]:
			sheet.cell(item["idx"], COL_RESULT).value = "Fail"
			kind_label = "Bcc inbox" if item["kind"].casefold() == "bcc" else "To: address"
			print("Fail row %s | missing Expected recipient (%s)" % (item["idx"], kind_label))
			failures += 1
		elif item["kind"].casefold() == "bcc":
			bcc_search.append(item)
		else:
			to_search.append(item)

	if to_search:
		creds = load_creds(args.token)
		service = build("gmail", "v1", credentials=creds)
		print("Authorized Gmail as", gmail_execute(service.users().getProfile(userId="me")).get("emailAddress"))
		failures += judge_search_rows(service, to_search, sheet, name_in_body_map)

	if bcc_search:
		token_bcc = Path(args.token_bcc)
		if not token_bcc.is_file():
			for item in bcc_search:
				sheet.cell(item["idx"], COL_RESULT).value = "Fail"
				print("Fail row %s | missing Fedlead BCC token at %s" % (item["idx"], token_bcc))
				failures += 1
		else:
			creds_bcc = load_creds(str(token_bcc))
			service_bcc = build("gmail", "v1", credentials=creds_bcc)
			bcc_email = gmail_execute(service_bcc.users().getProfile(userId="me")).get("emailAddress", "")
			print("Authorized Fedlead BCC Gmail as", bcc_email)
			expected_bcc = {item["recipient"].casefold() for item in bcc_search}
			if bcc_email.casefold() not in expected_bcc:
				for item in bcc_search:
					sheet.cell(item["idx"], COL_RESULT).value = "Fail"
					print("Fail row %s | BCC token authorizes %r, expected %r" % (
						item["idx"], bcc_email, item["recipient"]))
					failures += 1
			else:
				failures += judge_search_rows(service_bcc, bcc_search, sheet, name_in_body_map)

	workbook.save(excel_path)
	print("Updated", excel_path)
	return 1 if failures else 0


if __name__ == "__main__":
	sys.exit(main())
