"""
Artifact collector for the AI triage agent.

Locates and parses the most recent Katalon run artifacts:
- JUnit_Report.xml  (test names, pass/fail, duration, error messages)
- execution0.log    (full console output)
- ValidationReport_*.html (pass/fail items by section)
- CLI uploader logs  (error messages)
- retry-report.json  (failure classifications from Groovy listener)
- run-history.json   (trend data across runs)
"""

import glob
import json
import os
import xml.etree.ElementTree as ET
from typing import Any

from bs4 import BeautifulSoup


class ArtifactCollector:
    def __init__(self, katalon_reports_dir: str, cli_logs_dir: str):
        self.katalon_reports_dir = katalon_reports_dir
        self.cli_logs_dir = cli_logs_dir

    def collect(self) -> dict[str, Any]:
        """Collect all available artifacts and return a unified dict."""
        result: dict[str, Any] = {}

        # Find the most recent Katalon run directory
        run_dir = self._find_latest_run_dir()
        if run_dir:
            print(f"[collector] Using run directory: {run_dir}")
            result["junit_results"] = self._parse_junit(run_dir)
            result["execution_log"] = self._read_execution_log(run_dir)
            result["suite_name"] = self._extract_suite_name(run_dir)
            result["total_duration"] = self._extract_total_duration(result["junit_results"])
        else:
            print("[collector] No Katalon run directory found")
            result["junit_results"] = []
            result["execution_log"] = ""
            result["suite_name"] = "Unknown"
            result["total_duration"] = "N/A"

        result["validation_report"] = self._parse_validation_reports()
        result["cli_logs"] = self._read_cli_logs()
        result["retry_report"] = self._read_retry_report()
        result["run_history"] = self._read_run_history()

        return result

    def _find_latest_run_dir(self) -> str | None:
        """
        Katalon writes to Reports/{timestamp}/{SuiteName}/{execTimestamp}/.
        Find the most recently modified directory that contains a JUnit_Report.xml.
        """
        pattern = os.path.join(self.katalon_reports_dir, "*", "*", "*", "JUnit_Report.xml")
        matches = glob.glob(pattern)
        if not matches:
            pattern = os.path.join(self.katalon_reports_dir, "*", "*", "JUnit_Report.xml")
            matches = glob.glob(pattern)
        if not matches:
            return None
        latest = max(matches, key=os.path.getmtime)
        return os.path.dirname(latest)

    def _parse_junit(self, run_dir: str) -> list[dict]:
        """Parse JUnit_Report.xml and extract test case results."""
        junit_path = os.path.join(run_dir, "JUnit_Report.xml")
        if not os.path.exists(junit_path):
            return []

        results = []
        try:
            tree = ET.parse(junit_path)
            root = tree.getroot()

            for testsuite in root.iter("testsuite"):
                for testcase in testsuite.findall("testcase"):
                    name = testcase.get("name", "unknown")
                    classname = testcase.get("classname", "")
                    duration = float(testcase.get("time", "0"))

                    failure = testcase.find("failure")
                    error = testcase.find("error")
                    skipped = testcase.find("skipped")

                    if failure is not None:
                        status = "FAILED"
                        error_message = failure.get("message", "")
                        error_detail = failure.text or ""
                    elif error is not None:
                        status = "FAILED"
                        error_message = error.get("message", "")
                        error_detail = error.text or ""
                    elif skipped is not None:
                        status = "SKIPPED"
                        error_message = ""
                        error_detail = ""
                    else:
                        status = "PASSED"
                        error_message = ""
                        error_detail = ""

                    results.append({
                        "name": name,
                        "full_name": classname,
                        "status": status,
                        "duration": duration,
                        "error_message": error_message,
                        "error_detail": error_detail[:2000],
                    })
        except ET.ParseError as e:
            print(f"[collector] JUnit XML parse error: {e}")

        return results

    def _read_execution_log(self, run_dir: str) -> str:
        """Read the execution log from the run directory."""
        log_path = os.path.join(run_dir, "execution0.log")
        if not os.path.exists(log_path):
            return ""
        try:
            with open(log_path, "r", encoding="utf-8", errors="replace") as f:
                return f.read()
        except Exception as e:
            print(f"[collector] Could not read execution log: {e}")
            return ""

    def _extract_suite_name(self, run_dir: str) -> str:
        """Infer suite name from the run directory path."""
        parts = run_dir.replace("\\", "/").split("/")
        for i, part in enumerate(parts):
            if part == "Reports" and i + 2 < len(parts):
                return parts[i + 2]
        return os.path.basename(os.path.dirname(run_dir))

    @staticmethod
    def _extract_total_duration(junit_results: list[dict]) -> str:
        total_seconds = sum(t.get("duration", 0) for t in junit_results)
        minutes = int(total_seconds // 60)
        seconds = int(total_seconds % 60)
        return f"{minutes}m {seconds}s"

    def _parse_validation_reports(self) -> list[dict]:
        """Parse the most recent ValidationReport HTML for pass/fail items."""
        pattern = os.path.join(self.katalon_reports_dir, "ValidationReport_*.html")
        matches = glob.glob(pattern)
        if not matches:
            return []

        latest = max(matches, key=os.path.getmtime)
        try:
            with open(latest, "r", encoding="utf-8") as f:
                soup = BeautifulSoup(f.read(), "lxml")

            items = []
            for details in soup.find_all("details"):
                summary = details.find("summary")
                section = summary.get_text(strip=True) if summary else "Unknown"
                for row in details.find_all("tr"):
                    cells = row.find_all("td")
                    if len(cells) >= 4:
                        items.append({
                            "section": section,
                            "label": cells[0].get_text(strip=True),
                            "expected": cells[1].get_text(strip=True),
                            "actual": cells[2].get_text(strip=True),
                            "status": cells[3].get_text(strip=True),
                        })
            return items
        except Exception as e:
            print(f"[collector] Could not parse ValidationReport: {e}")
            return []

    def _read_cli_logs(self) -> list[dict]:
        """Read CLI uploader logs and report TSVs."""
        logs = []
        if not os.path.isdir(self.cli_logs_dir):
            return logs

        for log_file in sorted(glob.glob(os.path.join(self.cli_logs_dir, "Uploader Main-*.log"))):
            try:
                with open(log_file, "r", encoding="utf-8", errors="replace") as f:
                    content = f.read()
                error_lines = [
                    line.strip()
                    for line in content.split("\n")
                    if any(kw in line.lower() for kw in ["error", "fail", "invalid", "not found"])
                ]
                if error_lines:
                    logs.append({
                        "file": os.path.basename(log_file),
                        "errors": error_lines[:20],
                    })
            except Exception:
                pass

        return logs

    def _read_retry_report(self) -> dict:
        """Read the retry-report.json produced by the Groovy listener."""
        path = os.path.join(self.katalon_reports_dir, "retry-report.json")
        if not os.path.exists(path):
            return {}
        try:
            with open(path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"[collector] Could not read retry-report.json: {e}")
            return {}

    def _read_run_history(self) -> list[dict]:
        """Read the run-history.json for trend analysis."""
        path = os.path.join(self.katalon_reports_dir, "AI_Triage", "run-history.json")
        if not os.path.exists(path):
            return []
        try:
            with open(path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"[collector] Could not read run-history.json: {e}")
            return []
