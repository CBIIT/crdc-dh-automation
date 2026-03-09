"""
Triage report generator.

Produces a structured markdown report with:
- Run summary (total, passed, failed, by classification)
- Per-failure analysis (category, root cause, suggestion, retry info)
- Trend analysis from historical runs
"""

import os
from collections import Counter
from datetime import datetime


class ReportGenerator:
    def __init__(self, output_dir: str):
        self.output_dir = output_dir

    def generate(
        self,
        suite_name: str,
        total_tests: int,
        passed: int,
        failed: int,
        duration: str,
        classified_failures: list[dict],
        history: list[dict] | None = None,
    ) -> str:
        os.makedirs(self.output_dir, exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"triage-{timestamp}.md"
        filepath = os.path.join(self.output_dir, filename)

        lines = []
        lines.append("# Test Suite Triage Report\n")
        lines.append(f"**Suite:** {suite_name}  ")
        lines.append(f"**Date:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  ")
        lines.append(f"**Duration:** {duration}\n")

        # Summary
        lines.append("## Summary\n")
        classification_counts = Counter(
            f.get("classification", "UNKNOWN") for f in classified_failures
        )
        retried_and_passed = sum(
            1 for f in classified_failures if f.get("retried_status") == "PASSED"
        )

        lines.append(f"| Metric | Count |")
        lines.append(f"|--------|-------|")
        lines.append(f"| Total tests | {total_tests} |")
        lines.append(f"| Passed | {passed} |")
        lines.append(f"| Failed | {failed} |")
        lines.append(f"| Flaky (retried and passed) | {retried_and_passed} |")
        for cls_name in ["FLAKY", "ENVIRONMENT", "REAL", "REAL_BUG", "TEST_DATA", "UNKNOWN"]:
            count = classification_counts.get(cls_name, 0)
            if count > 0:
                lines.append(f"| {cls_name} | {count} |")
        lines.append("")

        # Per-failure details
        if classified_failures:
            lines.append("## Failed Tests\n")
            for i, failure in enumerate(classified_failures, 1):
                lines.append(f"### {i}. {failure.get('test_name', 'Unknown')}\n")
                lines.append(f"- **Category:** {failure.get('classification', 'UNKNOWN')}")
                lines.append(f"- **Confidence:** {failure.get('confidence', 'N/A')}")
                lines.append(f"- **Source:** {failure.get('source', 'rule-based')}")
                lines.append(f"- **Root Cause:** {failure.get('reason', 'Unknown')}")
                lines.append(f"- **Suggestion:** {failure.get('suggestion', 'Investigate manually')}")

                retry_attempt = failure.get("retry_attempt", 0)
                retried_status = failure.get("retried_status", "")
                if retry_attempt > 0:
                    lines.append(
                        f"- **Retry:** Retried {retry_attempt}x, {retried_status or 'status unknown'}"
                    )
                else:
                    classification = failure.get("classification", "")
                    if classification == "REAL" or classification == "REAL_BUG":
                        lines.append("- **Retry:** Not retried (real failure)")
                    else:
                        lines.append("- **Retry:** No retry data available")

                error_msg = failure.get("error_message", "")
                if error_msg:
                    truncated = error_msg[:300] + ("..." if len(error_msg) > 300 else "")
                    lines.append(f"\n<details><summary>Error message</summary>\n")
                    lines.append(f"```\n{truncated}\n```\n")
                    lines.append(f"</details>\n")
                else:
                    lines.append("")
        else:
            lines.append("## No Failures\n")
            lines.append("All tests passed. No triage needed.\n")

        # Trend analysis
        if history and len(history) > 1:
            lines.append("## Trend Analysis (last {} runs)\n".format(len(history)))
            lines.append("| Run | Suite | Total | Failed | Flaky | Environment | Real |")
            lines.append("|-----|-------|-------|--------|-------|-------------|------|")
            for run in history[-10:]:
                summary = run.get("summary", {})
                classifications = summary.get("classifications", {})
                lines.append(
                    "| {} | {} | {} | {} | {} | {} | {} |".format(
                        run.get("timestamp", "N/A")[:16],
                        run.get("suiteId", ""),
                        summary.get("total", 0),
                        summary.get("failed", 0),
                        classifications.get("flaky", 0),
                        classifications.get("environment", 0),
                        classifications.get("real", 0),
                    )
                )
            lines.append("")

            # Identify frequently failing tests
            fail_counts: Counter = Counter()
            for run in history:
                for ft in run.get("failedTests", []):
                    fail_counts[ft.get("testCaseId", "")] += 1

            frequent = [(tc, count) for tc, count in fail_counts.most_common(5) if count > 1]
            if frequent:
                lines.append("### Frequently Failing Tests\n")
                for tc, count in frequent:
                    lines.append(f"- **{tc}** — failed in {count}/{len(history)} runs")
                lines.append("")

        # Write file
        report_content = "\n".join(lines)
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(report_content)

        # Also write a latest symlink / copy for easy access
        latest_path = os.path.join(self.output_dir, "triage-latest.md")
        with open(latest_path, "w", encoding="utf-8") as f:
            f.write(report_content)

        return filepath
