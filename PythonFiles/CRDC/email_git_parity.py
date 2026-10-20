"""Load CRDC notification copy from Git and compare to Gmail body text (wave 1).

Used by VerifyNotificationEmails.py when --git-parity is set. Gmail already checks
subject, recipient, and catalog body snippets; this module checks that allowlisted To
rows also match fragments from notification_email_values.yaml (and static template
phrases for DS Create). Mapping lives in InputFiles/CRDC/email_git_parity_triggers.yaml.
"""
from __future__ import annotations

import html
import re
import urllib.error
import urllib.request
from pathlib import Path
from typing import List, Optional, Tuple

import yaml

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MAPPING = REPO_ROOT / "InputFiles" / "CRDC" / "email_git_parity_triggers.yaml"
DEFAULT_ENV_VARS = REPO_ROOT / "InputFiles" / "CRDC" / "email_git_parity_env.yaml"
YAML_REL = "yaml/notification_email_values.yaml"
# Reserved for future DS Create parity loaded from Git HTML; wave 1 uses html_static_phrases in triggers YAML.
SUBMISSION_TEMPLATE_REL = "email-templates/notification-template-submission.html"


def load_mapping(path: Optional[Path] = None) -> dict:
	"""Load trigger allowlist and per-Action rules from email_git_parity_triggers.yaml."""
	with open(path or DEFAULT_MAPPING, encoding="utf-8") as f:
		return yaml.safe_load(f)


def load_env_variables(env: str, path: Optional[Path] = None) -> dict:
	"""Return QA / QA2 / STAGE literals (helpDesk, contactEmail, etc.) from email_git_parity_env.yaml."""
	p = path or DEFAULT_ENV_VARS
	if not p.is_file():
		return {}
	with open(p, encoding="utf-8") as f:
		data = yaml.safe_load(f) or {}
	token = (env or "").strip()
	return dict(data.get(token) or {})


def fetch_raw_github(owner: str, repo: str, branch: str, rel_path: str) -> str:
	"""Download a single file from GitHub raw (no git clone)."""
	url = "https://raw.githubusercontent.com/%s/%s/%s/%s" % (owner, repo, branch, rel_path)
	req = urllib.request.Request(url, headers={"User-Agent": "crdc-dh-automation-email-parity"})
	with urllib.request.urlopen(req, timeout=60) as resp:
		return resp.read().decode("utf-8")


def load_email_constants(content_dir: Optional[str], branch: str, mapping: dict) -> dict:
	"""Load notification_email_values.yaml from a local clone or GitHub for the given branch."""
	if content_dir:
		yaml_path = Path(content_dir) / YAML_REL
		if not yaml_path.is_file():
			raise FileNotFoundError("Missing %s under CRDC_EMAIL_CONTENT_DIR" % YAML_REL)
		with open(yaml_path, encoding="utf-8") as f:
			return yaml.safe_load(f)
	gh = mapping.get("github") or {}
	owner = gh.get("owner", "CBIIT")
	repo = gh.get("repo", "crdc-submission-portal-email-content")
	text = fetch_raw_github(owner, repo, branch, YAML_REL)
	return yaml.safe_load(text)


def resolve_binding(source: str, checklist: dict, env_vars: dict, portal_url: str) -> str:
	"""Turn a triggers.yaml binding (checklist:Name, env:helpDesk, env:url) into a string value."""
	if source == "checklist:Name":
		return (checklist.get("name") or "").strip()
	if source == "checklist:Id":
		return (checklist.get("id") or "").strip()
	if source.startswith("env:"):
		key = source[4:]
		if key == "url" and portal_url:
			return portal_url.strip()
		return str(env_vars.get(key) or "").strip()
	return ""


def substitute_yaml(text: str, variables: dict, ignore_placeholders: Optional[List[str]] = None) -> str:
	"""Replace $placeholders in Git YAML strings; optionally leave named keys blank (e.g. $pi)."""
	if not text:
		return ""
	ignore = set(ignore_placeholders or [])
	out = text
	for key, value in variables.items():
		if key in ignore:
			continue
		out = out.replace("$%s" % key, value or "")
	for key in ignore:
		out = re.sub(r"\$%s\b" % re.escape(key), "", out)
	return out


def normalize_plain(text: str) -> str:
	"""Collapse Gmail/HTML body to a single line of plain text for substring checks."""
	if not text:
		return ""
	stripped = re.sub(r"<[^>]+>", " ", text)
	stripped = html.unescape(stripped)
	stripped = re.sub(r"\s+", " ", stripped).strip()
	return stripped


def html_to_plain_email_body(raw: str) -> str:
	"""Alias for normalize_plain; Gmail bodies are HTML."""
	return normalize_plain(raw)


def fragment_plain(constants: dict, body_key: str, variables: dict, ignore: Optional[List[str]] = None) -> str:
	"""Build one expected body substring from a YAML key after $ substitution and normalization."""
	raw = constants.get(body_key) or ""
	sub = substitute_yaml(raw, variables, ignore)
	return normalize_plain(sub)


def check_trigger_parity(
	action: str,
	mapping: dict,
	constants: dict,
	body_raw: str,
	checklist: dict,
	env: str,
	portal_url: str = "",
	env_path: Optional[Path] = None,
) -> Tuple[bool, str]:
	"""Compare Git copy rules for one checklist Action to the normalized Gmail body.

	Subject is not checked here (Gmail + PBAC catalog). Returns (True, "ok") or
	(False, short reason). Rules come from triggers[action]: body_keys, flexible keys,
	html_static_phrases, and body_variables.
	"""
	triggers = mapping.get("triggers") or {}
	cfg = triggers.get(action)
	if not cfg:
		return False, "no mapping for action %r" % action

	env_vars = load_env_variables(env, env_path)
	plain_body = html_to_plain_email_body(body_raw)
	var_bindings = cfg.get("variables") or {}
	variables = {}
	for yaml_key, binding in var_bindings.items():
		variables[yaml_key] = resolve_binding(binding, checklist, env_vars, portal_url)

	for body_key in cfg.get("body_keys") or []:
		frag = fragment_plain(constants, body_key, variables)
		if frag and frag not in plain_body:
			return False, "Git body missing fragment from %s: %r" % (body_key, frag[:80])

	for flex in cfg.get("body_keys_flexible") or []:
		bkey = flex.get("key")
		flex_vars = {}
		for yaml_key, binding in (flex.get("variables") or {}).items():
			flex_vars[yaml_key] = resolve_binding(binding, checklist, env_vars, portal_url)
		ignore = flex.get("ignore_placeholders") or []
		frag = fragment_plain(constants, bkey, flex_vars, ignore)
		if frag and frag not in plain_body:
			return False, "Git body missing flexible fragment from %s" % bkey

	for phrase in cfg.get("html_static_phrases") or []:
		if phrase and phrase not in plain_body:
			return False, "Git body missing static phrase %r" % phrase[:60]

	body_vars = cfg.get("body_variables") or {}
	for _label, binding in body_vars.items():
		val = resolve_binding(binding, checklist, env_vars, portal_url)
		if val and val not in plain_body:
			return False, "Git body missing checklist value %r" % val[:60]

	return True, "ok"


def is_git_parity_candidate(action: str, kind: str, mapping: dict) -> bool:
	"""True if this checklist row should run Git parity (allowlisted Action and To recipient)."""
	allow = set(mapping.get("allowlist") or [])
	if action not in allow:
		return False
	return (kind or "To").casefold() == "to"


def git_parity_skip_reason(action: str, kind: str, mapping: dict) -> Optional[str]:
	"""Human-readable reason to Skip Git parity, or None if the row is a candidate."""
	if (kind or "To").casefold() != "to":
		return "not To row"
	allow = set(mapping.get("allowlist") or [])
	if action not in allow:
		return "not in Git allowlist"
	return None
