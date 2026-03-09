# AI-Powered Test Intelligence for crdc-dh-automation

This document describes the two AI-powered capabilities added to the CRDC Data Hub automation framework:

1. **Intelligent Flaky Test Retry** — A Groovy-based, in-Katalon system that classifies test failures in real time and works with Katalon's built-in rerun mechanism.
2. **AI-Powered Post-Run Triage Report** — A standalone Python agent that runs after a test suite, collects artifacts, applies rule-based and LLM classification, and generates a structured markdown triage report.

**Start with [How the two systems work together](#how-the-two-systems-work-together)** for a clear picture of Groovy vs Python, what classifies what, and how the retry report is used.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Katalon Test Suite                       │
│                                                             │
│  @BeforeTestSuite                                           │
│    └─ FailureClassifierListener initializes RetryReport     │
│                                                             │
│  Test Case 1 ──► @AfterTestCase                             │
│  Test Case 2 ──► @AfterTestCase ──► FailureClassifier       │
│  Test Case N ──► @AfterTestCase       ├─ FLAKY              │
│                                       ├─ ENVIRONMENT        │
│                                       └─ REAL               │
│                                                             │
│  @AfterTestSuite                                            │
│    ├─ RetryReport.writeReport()   → Reports/retry-report.json
│    └─ RetryReport.appendToHistory() → Reports/AI_Triage/run-history.json
│                                                             │
│  Katalon Built-in Rerun (numberOfRerun=1)                   │
│    └─ Reruns failed test cases immediately                  │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Python Post-Run Triage Agent                    │
│              (Tools/ai-triage/triage_agent.py)              │
│                                                             │
│  1. Collect artifacts                                       │
│     ├─ JUnit_Report.xml (test results)                      │
│     ├─ execution0.log (full console output)                 │
│     ├─ ValidationReport_*.html (pass/fail items)            │
│     ├─ CLI uploader logs (error lines)                      │
│     ├─ retry-report.json (Groovy classifications)           │
│     └─ run-history.json (trend data)                        │
│                                                             │
│  2. Classify failures                                       │
│     ├─ Rule-based pattern matching (first pass)             │
│     └─ Amazon Bedrock LLM (for unclassified failures)       │
│                                                             │
│  3. Generate triage report                                  │
│     └─ Reports/AI_Triage/triage-{timestamp}.md              │
└─────────────────────────────────────────────────────────────┘
```

---

## How the two systems work together

It helps to think of **two separate classifiers** and **one source of truth** for the triage report.

### Two classifiers, different roles

| | **Groovy (during Katalon run)** | **Python (post-run triage agent)** |
|---|----------------------------------|-------------------------------------|
| **When** | After every test execution (and again after each rerun) | Once, after the suite finishes |
| **Input** | The full error message from the test listener (each run) | The **final** failure message from **JUnit** only (one result per test case) |
| **Patterns** | Hardcoded in `FailureClassifier.groovy` | Read from `config.yaml` (`flaky_patterns`, `environment_patterns`) |
| **LLM** | Never — pattern matching only | Only when rule-based returns **UNKNOWN** (no pattern matched) |
| **Output** | `retry-report.json` (audit trail: every execution + classification) | Triage markdown report (one row per failed test, with category and Source) |

**The classification that appears in the triage report** (FLAKY / ENVIRONMENT / REAL and “Source: rule-based” vs “Source: LLM”) is **always from the Python agent**. It uses only the **error message from JUnit** (and optional LLM for UNKNOWN). The Groovy listener does not decide what goes into that report.

### What the retry report is used for (by Python)

The Python agent **does not** use `retry-report.json` to classify. It uses it only to **enrich** the report:

- **Source of failures:** JUnit XML only. The list of failed tests and their error messages comes from `JUnit_Report.xml`.
- **Classification:** Rule-based (using `config.yaml` patterns) on that JUnit error message → if UNKNOWN, then one LLM call per failure.
- **Merge step:** If `retry-report.json` exists, the agent looks up each failed test and adds:
  - `retry_attempt`, `retried_status` (e.g. “Retried 1x, PASSED”)
  - `groovy_classification` (what the Groovy listener thought: FLAKY/ENVIRONMENT/REAL)

So the retry report is an **audit trail** with its own Groovy classification; the “real” classification in the triage report is from Python (rules + optional LLM).

### Why you might see different classifications

- **Groovy** records **every execution**. If TC16 fails twice (first run + retry), you get two rows in `retry-report.json`: e.g. first run “Unable to click on object” → FLAKY, second run “Element did not appear within 30 seconds” → REAL.
- **Python** sees only the **final** JUnit result (one row per test). So it classifies that single error message. If that message matches a pattern → “Source: rule-based”. If it doesn’t → UNKNOWN → LLM → “Source: LLM”.

So the same test can be FLAKY in the retry report (first run) and REAL or ENVIRONMENT in the triage report (final run), because they are classifying different runs/messages.

### When the LLM is called

The LLM is used **only** when the Python rule-based classifier returns **UNKNOWN** (the error message did not match any `flaky_patterns` or `environment_patterns` in `config.yaml`). If any pattern matches, the category is set from the rules and **no** LLM call is made; the report shows **Source: rule-based**.

---

## Feature 1: Intelligent Flaky Test Retry (Groovy / Katalon)

### Components

| File | Purpose |
|------|---------|
| `Keywords/utilities/FailureClassifier.groovy` | Rule-based classifier that categorizes failures as `FLAKY`, `ENVIRONMENT`, or `REAL` |
| `Keywords/utilities/RetryReport.groovy` | Collects per-test results and writes `retry-report.json` and `run-history.json` |
| `Test Listeners/FailureClassifierListener.groovy` | Katalon Test Listener that hooks into `@BeforeTestSuite`, `@AfterTestCase`, `@AfterTestSuite` |
| `Include/config/retry.properties` | Configuration file to enable/disable the system and control behavior |

### How It Works

1. **Before the suite starts**, the listener reads `Include/config/retry.properties` and initializes the `RetryReport` collector.
2. **After each test case** (including after each rerun), the listener captures the test result. If the test failed, it passes the full error message to `FailureClassifier.classify()`, which matches it against known patterns. So a test that fails twice (e.g. TC16) produces **two entries** in the report — one per execution.
3. **After the suite ends**, the listener writes:
   - `Reports/retry-report.json` — One row per test **execution** (not per test case). So a test that ran twice has two rows. Each row includes classification and recommendation for that run.
   - `Reports/AI_Triage/run-history.json` — Appends a summary of the current run for trend analysis (keeps the last 20 runs).
4. **Katalon's built-in rerun** mechanism handles the actual retry. When a test suite has `isRerun=true` and `numberOfRerun=1`, Katalon will automatically rerun failed test cases once.

### Failure Classification Categories

| Category | Description | Retryable? | Examples |
|----------|-------------|-----------|----------|
| **FLAKY** | Transient timing or DOM interaction issue | Yes | `StaleElementReferenceException`, `ElementClickInterceptedException`, `TimeoutException`, OTP/Login race conditions, loading spinner timeouts |
| **ENVIRONMENT** | Infrastructure or configuration problem | Conditionally | `metadata_encoding` (wrong Python version), `connection refused`, `session not created` (ChromeDriver mismatch), DNS failures |
| **REAL** | Genuine test or application failure | No | No known pattern matched — requires human investigation |

### Configuration (`Include/config/retry.properties`)

```properties
# Master switch: set to false to disable the failure classifier listener entirely
retry.enabled=true

# Maximum retry attempts for flaky tests (used by Katalon's built-in rerun)
retry.maxRetries=1

# Only retry tests classified as FLAKY (true) or also ENVIRONMENT (false)
retry.retryFlakyOnly=false

# Write retry-report.json after suite completes
retry.writeReport=true

# Append to run history for trend analysis
retry.trackHistory=true
```

### Enabling Rerun on a Test Suite

To activate automatic retries, the test suite `.ts` file must have these XML elements:

```xml
<isRerun>true</isRerun>
<numberOfRerun>1</numberOfRerun>
<rerunFailedTestCasesOnly>true</rerunFailedTestCasesOnly>
<rerunImmediately>true</rerunImmediately>
```

This is already configured on `CRDC_DebugSuite.ts`. Apply the same settings to any suite that should retry flaky tests.

### Sample `retry-report.json` Output

`testResults` has **one entry per test execution** (so a test that ran twice due to rerun appears twice). Each entry has the classification from the Groovy listener for that run.

```json
{
  "suiteId": "CRDC_DebugSuite",
  "timestamp": "2026-02-13T10:30:00",
  "summary": {
    "total": 5,
    "passed": 4,
    "failed": 1,
    "classifications": {
      "flaky": 1,
      "environment": 0,
      "real": 0
    },
    "retriedAndPassed": 0
  },
  "testResults": [
    {
      "testCaseId": "TC01_SomeTest",
      "fullTestCaseId": "Test Cases/CRDC/DataSubmissions/GC/.../TC01_SomeTest",
      "status": "FAILED",
      "errorMessage": "StaleElementReferenceException: stale element reference...",
      "classification": "FLAKY",
      "reason": "DOM element became stale during interaction",
      "recommendation": "Transient issue — should pass on retry.",
      "retryAttempt": 0,
      "retriedStatus": ""
    }
  ]
}
```

### Pattern matching (Groovy)

The classifier checks patterns in **order** and returns on the **first** match. The full error string (including exception cause and stack trace) is used. So if the message contains both "ElementClickInterceptedException" and "Unable to click on object", the **reason** shown is from whichever pattern appears first in `FLAKY_PATTERNS`. Keep patterns ordered by specificity if you want the most precise reason.

### Adding New Patterns

To add new flaky or environment patterns, edit the pattern lists in `Keywords/utilities/FailureClassifier.groovy`:

```groovy
private static final List<Map<String, String>> FLAKY_PATTERNS = [
    [pattern: 'YourNewPattern', reason: 'Explanation of why this is flaky'],
    // ... existing patterns
]
```

Also update the corresponding list in `Tools/ai-triage/config.yaml` so the Python agent uses the same patterns:

```yaml
classification:
  flaky_patterns:
    - "YourNewPattern"
```

---

## Feature 2: AI-Powered Post-Run Triage Report (Python)

### Components

| File | Purpose |
|------|---------|
| `Tools/ai-triage/triage_agent.py` | Main orchestrator — collects, classifies, and generates the report |
| `Tools/ai-triage/artifact_collector.py` | Locates and parses Katalon run artifacts (JUnit XML, logs, HTML, JSON) |
| `Tools/ai-triage/bedrock_classifier.py` | Rule-based classifier + Amazon Bedrock LLM classifier for deeper analysis |
| `Tools/ai-triage/report_generator.py` | Generates the structured markdown triage report |
| `Tools/ai-triage/config.yaml` | Configuration for Bedrock model, report paths, and classification patterns |
| `Tools/ai-triage/requirements.txt` | Python dependencies |

### Prerequisites

- Python 3.9+
- AWS credentials configured (for Bedrock LLM — not needed in `--no-llm` mode)
- Virtual environment (recommended)

### Setup

```bash
cd Tools/ai-triage

# Create and activate a virtual environment
python3 -m venv venv
source venv/bin/activate    # macOS/Linux
# venv\Scripts\activate     # Windows

# Install dependencies
pip install -r requirements.txt
```

### Usage

**Rule-based only (no AWS/LLM required):**

```bash
cd Tools/ai-triage
source venv/bin/activate
python triage_agent.py --project-dir /path/to/crdc-dh-automation --no-llm
```

**With Amazon Bedrock LLM classification:**

```bash
cd Tools/ai-triage
source venv/bin/activate
python triage_agent.py --project-dir /path/to/crdc-dh-automation
```

**Override directories:**

```bash
python triage_agent.py \
  --project-dir /path/to/crdc-dh-automation \
  --reports-dir /custom/reports/path \
  --output /custom/output/path
```

### Command-Line Arguments

| Argument | Default | Description |
|----------|---------|-------------|
| `--project-dir` | Current directory | Root of the crdc-dh-automation project |
| `--reports-dir` | From `config.yaml` → `Reports` | Katalon reports directory |
| `--output` | From `config.yaml` → `Reports/AI_Triage` | Output directory for triage reports |
| `--no-llm` | Off | Skip Bedrock LLM calls; use rule-based classification only |

### Artifact Collection

The `ArtifactCollector` automatically finds the most recent Katalon run and collects:

| Artifact | Location | How Python uses it |
|----------|----------|---------------------|
| **JUnit XML** | `Reports/{timestamp}/{Suite}/{exec}/JUnit_Report.xml` | **Source of failed tests and error messages.** One result per test case (final result after reruns). Classification is done from this. |
| Execution log | `Reports/{timestamp}/{Suite}/{exec}/execution0.log` | Log excerpt around each failure is sent to the LLM as context (when LLM is called). |
| ValidationReport | `Reports/ValidationReport_*.html` | Parsed for pass/fail items; available for future use. |
| CLI uploader logs | `Tools/crdc-datahub-cli-uploader-src/tmp/Uploader Main-*.log` | Error lines; available for context. |
| **Retry report** | `Reports/retry-report.json` | **Enrichment only.** Not used to classify. Merges in `retry_attempt`, `retried_status`, and `groovy_classification` so the report can show “Retried 1x, PASSED” and what Groovy thought. |
| Run history | `Reports/AI_Triage/run-history.json` | Trend table (last N runs) and frequently failing tests. |

### Classification Pipeline

1. **Build list of failures from JUnit only.** The agent reads `JUnit_Report.xml` and takes every test with `status == "FAILED"`. Each has one error message (the final failure after any reruns). No list of failures is taken from the retry report.

2. **Rule-based (first pass):** For each failed test, the `RuleBasedClassifier` checks that error message against `flaky_patterns` and `environment_patterns` in `config.yaml`. If a pattern matches → FLAKY or ENVIRONMENT, and **Source: rule-based**. No LLM call.

3. **LLM (only when UNKNOWN):** If no pattern matched, the result is UNKNOWN. Then (if Bedrock is enabled and not `--no-llm`) the agent sends that failure to Amazon Bedrock. The LLM receives the test name, error message (truncated), and a log excerpt. It returns classification, reason, suggestion, confidence. **Source: LLM.**

4. **Merge with retry report:** If `retry-report.json` exists, the agent looks up each failed test by name and adds `retry_attempt`, `retried_status`, and optionally `groovy_classification` to the failure dict. This does not change the category or source; it only enriches the report.

### Supported Bedrock Models

The agent is model-agnostic and supports multiple Bedrock model families. Change `model_id` in `config.yaml`:

| Provider | Example Model ID | Notes |
|----------|-----------------|-------|
| Anthropic Claude | `anthropic.claude-3-5-sonnet-20241022-v2:0` | Default, best quality |
| Amazon Titan | `amazon.titan-text-express-v1` | Lower cost |
| Meta Llama | `meta.llama3-8b-instruct-v1:0` | Open-source option |

### Configuration (`config.yaml`)

```yaml
bedrock:
  model_id: "anthropic.claude-3-5-sonnet-20241022-v2:0"
  region: "us-east-1"
  max_tokens: 500
  enabled: true           # Set to false for rule-based only (same as --no-llm)

reports:
  katalon_reports_dir: "Reports"
  cli_logs_dir: "Tools/crdc-datahub-cli-uploader-src/tmp"
  output_dir: "Reports/AI_Triage"
  output_format: "markdown"

classification:
  flaky_patterns:
    - "StaleElementReferenceException"
    - "ElementClickInterceptedException"
    - "TimeoutException"
    # ... (see config.yaml for full list)
  environment_patterns:
    - "path does not exist"
    - "metadata_encoding"
    - "connection refused"
    # ... (see config.yaml for full list)
  use_llm_for_unknown: true   # Send UNKNOWN failures to Bedrock
```

### Output

Reports are written to `Reports/AI_Triage/`:

- `triage-{YYYYMMDD_HHMMSS}.md` — Timestamped report for each run
- `triage-latest.md` — Always overwritten with the most recent report

### Sample Report Structure

```markdown
# Test Suite Triage Report

**Suite:** CRDC_DataValidation
**Date:** 2026-02-13 10:30:00
**Duration:** 12m 34s

## Summary

| Metric | Count |
|--------|-------|
| Total tests | 15 |
| Passed | 13 |
| Failed | 2 |
| FLAKY | 1 |
| REAL | 1 |

## Failed Tests

### 1. TC04_VerifyValidationResults

- **Category:** FLAKY
- **Confidence:** HIGH
- **Source:** rule-based
- **Root Cause:** Matched flaky pattern: 'staleelementreferenceexception'
- **Suggestion:** Transient issue — should pass on retry.
- **Retry:** Retried 1x, PASSED

### 2. TC10_DataSubmissionsListAndFilter

- **Category:** REAL
- **Confidence:** LOW
- **Source:** rule-based
- **Root Cause:** No known pattern matched
- **Suggestion:** Investigate manually or escalate to LLM analysis.
- **Retry:** Not retried (real failure)

## Trend Analysis (last 5 runs)

| Run | Suite | Total | Failed | Flaky | Environment | Real |
|-----|-------|-------|--------|-------|-------------|------|
| 2026-02-13T10:3 | CRDC_DataValidation | 15 | 2 | 1 | 0 | 1 |
| 2026-02-12T14:0 | CRDC_DataValidation | 15 | 1 | 1 | 0 | 0 |

### Frequently Failing Tests

- **TC04_VerifyValidationResults** — failed in 3/5 runs
```

---

## File Layout

```
crdc-dh-automation/
├── Include/config/
│   └── retry.properties                  # Groovy-side configuration
├── Keywords/utilities/
│   ├── FailureClassifier.groovy          # Rule-based failure classifier
│   └── RetryReport.groovy                # Retry report collector and writer
├── Test Listeners/
│   └── FailureClassifierListener.groovy  # Katalon lifecycle hooks
├── Reports/
│   ├── retry-report.json                 # (Generated) Per-execution audit trail; Groovy classifications
│   └── AI_Triage/
│       ├── run-history.json              # (Generated) Trend data across runs
│       ├── triage-latest.md              # (Generated) Latest triage report
│       └── triage-{timestamp}.md         # (Generated) Timestamped reports
└── Tools/ai-triage/
    ├── README.md                         # This file
    ├── config.yaml                       # Python agent configuration
    ├── requirements.txt                  # Python dependencies
    ├── triage_agent.py                   # Main orchestrator
    ├── artifact_collector.py             # Artifact locator and parser
    ├── bedrock_classifier.py             # Rule-based + Bedrock LLM classifiers
    ├── report_generator.py               # Markdown report generator
    └── venv/                             # Python virtual environment (not committed)
```

---

## Workflow: Running Both Features Together

1. **Run a test suite** in Katalon Studio (or via CLI/Jenkins). The `FailureClassifierListener` fires automatically and writes `retry-report.json` and `run-history.json`.

2. **Katalon reruns failed tests** if the suite is configured with `isRerun=true`.

3. **After the suite finishes**, run the Python triage agent:

```bash
cd Tools/ai-triage
source venv/bin/activate
python triage_agent.py --project-dir ../.. --no-llm
```

4. **Review the triage report** at `Reports/AI_Triage/triage-latest.md`.

### Jenkins Integration

To add the triage agent as a post-build step in Jenkins:

```bash
cd ${WORKSPACE}/Tools/ai-triage
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python triage_agent.py --project-dir ${WORKSPACE} --no-llm
```

For Bedrock LLM support in Jenkins, ensure the build node has AWS credentials configured (via IAM role, environment variables, or `~/.aws/credentials`).

---

## Extending the System

### Adding New Flaky/Environment Patterns

1. Add the pattern string and a human-readable reason to `FLAKY_PATTERNS` or `ENVIRONMENT_PATTERNS` in `Keywords/utilities/FailureClassifier.groovy`.
2. Add the same pattern string to the corresponding list in `Tools/ai-triage/config.yaml`.

### Changing the Bedrock Model

Edit `model_id` in `Tools/ai-triage/config.yaml`. The agent auto-detects the model family (Anthropic, Titan, Llama) and formats the request accordingly.

### Disabling the System

- **Groovy side:** Set `retry.enabled=false` in `Include/config/retry.properties`. The listener will skip all processing.
- **Python side:** Simply don't run `triage_agent.py`, or pass `--no-llm` to skip LLM costs while keeping rule-based analysis.
- **Suite rerun:** Set `<isRerun>false</isRerun>` in the test suite `.ts` file.
