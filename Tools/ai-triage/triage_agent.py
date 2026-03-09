#!/usr/bin/env python3
"""
AI-powered post-run triage agent for crdc-dh-automation.

Collects Katalon test artifacts (JUnit XML, execution logs, ValidationReport HTML,
CLI uploader logs, retry-report.json), classifies failures using rule-based patterns
and optionally Amazon Bedrock LLM, and generates a markdown triage report.

Usage:
    python triage_agent.py --project-dir /path/to/crdc-dh-automation
    python triage_agent.py --project-dir . --no-llm
"""

import argparse
import os
import sys
import yaml

from artifact_collector import ArtifactCollector
from bedrock_classifier import BedrockClassifier, RuleBasedClassifier
from report_generator import ReportGenerator


def load_config(project_dir: str) -> dict:
    config_path = os.path.join(project_dir, "Tools", "ai-triage", "config.yaml")
    if not os.path.exists(config_path):
        print(f"[WARN] Config not found at {config_path}, using defaults")
        return {}
    with open(config_path, "r") as f:
        return yaml.safe_load(f) or {}


def main():
    parser = argparse.ArgumentParser(description="AI Test Triage Agent")
    parser.add_argument(
        "--project-dir",
        default=os.getcwd(),
        help="Root directory of the crdc-dh-automation project",
    )
    parser.add_argument(
        "--reports-dir",
        default=None,
        help="Override the Katalon reports directory (default: from config.yaml)",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="Override output directory for triage report",
    )
    parser.add_argument(
        "--no-llm",
        action="store_true",
        help="Skip LLM classification; use rule-based only",
    )
    args = parser.parse_args()

    project_dir = os.path.abspath(args.project_dir)
    config = load_config(project_dir)

    reports_cfg = config.get("reports", {})
    katalon_reports_dir = args.reports_dir or os.path.join(
        project_dir, reports_cfg.get("katalon_reports_dir", "Reports")
    )
    cli_logs_dir = os.path.join(
        project_dir, reports_cfg.get("cli_logs_dir", "Tools/crdc-datahub-cli-uploader-src/tmp")
    )
    output_dir = args.output or os.path.join(
        project_dir, reports_cfg.get("output_dir", "Reports/AI_Triage")
    )

    print(f"[triage] Project dir:  {project_dir}")
    print(f"[triage] Reports dir:  {katalon_reports_dir}")
    print(f"[triage] CLI logs dir: {cli_logs_dir}")
    print(f"[triage] Output dir:   {output_dir}")

    # Step 1: Collect artifacts
    collector = ArtifactCollector(katalon_reports_dir, cli_logs_dir)
    artifacts = collector.collect()

    if not artifacts.get("junit_results") and not artifacts.get("retry_report"):
        print("[triage] No test results found. Nothing to triage.")
        sys.exit(0)

    print(f"[triage] Found {len(artifacts.get('junit_results', []))} test results from JUnit XML")

    # Step 2: Classify failures
    classification_cfg = config.get("classification", {})
    bedrock_cfg = config.get("bedrock", {})
    use_llm = bedrock_cfg.get("enabled", True) and not args.no_llm

    rule_classifier = RuleBasedClassifier(
        flaky_patterns=classification_cfg.get("flaky_patterns", []),
        environment_patterns=classification_cfg.get("environment_patterns", []),
    )

    llm_classifier = None
    if use_llm and classification_cfg.get("use_llm_for_unknown", True):
        try:
            llm_classifier = BedrockClassifier(
                model_id=bedrock_cfg.get("model_id", "anthropic.claude-3-5-sonnet-20241022-v2:0"),
                region=bedrock_cfg.get("region", "us-east-1"),
                max_tokens=bedrock_cfg.get("max_tokens", 500),
            )
            print("[triage] Bedrock LLM classifier initialized")
        except Exception as e:
            print(f"[triage] Could not initialize Bedrock: {e}. Falling back to rule-based only.")
            llm_classifier = None

    classified_failures = []
    for test in artifacts.get("junit_results", []):
        if test["status"] != "FAILED":
            continue

        error_msg = test.get("error_message", "")
        log_excerpt = _find_log_excerpt(
            test["name"], artifacts.get("execution_log", "")
        )

        result = rule_classifier.classify(error_msg)

        if result["classification"] == "UNKNOWN" and llm_classifier:
            try:
                llm_result = llm_classifier.classify_failure(
                    test["name"], error_msg, log_excerpt
                )
                result = llm_result
                result["source"] = "LLM"
            except Exception as e:
                print(f"[triage] LLM classification failed for {test['name']}: {e}")
                result["source"] = "rule-based (LLM fallback failed)"
        else:
            result["source"] = "rule-based"

        classified_failures.append({
            "test_name": test["name"],
            "full_name": test.get("full_name", test["name"]),
            "duration": test.get("duration", 0),
            "error_message": error_msg,
            "log_excerpt": log_excerpt[:1000] if log_excerpt else "",
            **result,
        })

    # Step 3: Merge with retry report if available
    retry_data = artifacts.get("retry_report", {})
    if retry_data:
        _merge_retry_data(classified_failures, retry_data)

    # Step 4: Generate report
    generator = ReportGenerator(output_dir)
    report_path = generator.generate(
        suite_name=artifacts.get("suite_name", "Unknown Suite"),
        total_tests=len(artifacts.get("junit_results", [])),
        passed=sum(1 for t in artifacts.get("junit_results", []) if t["status"] == "PASSED"),
        failed=len(classified_failures),
        duration=artifacts.get("total_duration", "N/A"),
        classified_failures=classified_failures,
        history=artifacts.get("run_history", []),
    )

    print(f"[triage] Report written to: {report_path}")


def _find_log_excerpt(test_name: str, full_log: str, context_lines: int = 15) -> str:
    """Extract lines around the test name in the execution log."""
    if not full_log or not test_name:
        return ""
    lines = full_log.split("\n")
    for i, line in enumerate(lines):
        if test_name in line and ("FAILED" in line or "ERROR" in line):
            start = max(0, i - 3)
            end = min(len(lines), i + context_lines)
            return "\n".join(lines[start:end])
    return ""


def _merge_retry_data(classified_failures: list, retry_data: dict):
    """Enrich classified failures with retry information from retry-report.json."""
    retry_results = {
        r.get("testCaseId", ""): r for r in retry_data.get("testResults", [])
    }
    for failure in classified_failures:
        short_name = failure["test_name"]
        retry_info = retry_results.get(short_name, {})
        if retry_info:
            failure["retry_attempt"] = retry_info.get("retryAttempt", 0)
            failure["retried_status"] = retry_info.get("retriedStatus", "")
            if retry_info.get("classification"):
                failure["groovy_classification"] = retry_info["classification"]


if __name__ == "__main__":
    main()
