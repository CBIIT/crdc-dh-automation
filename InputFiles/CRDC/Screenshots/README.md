# Baseline screenshots for visual comparison

This folder stores **baseline (reference) screenshots** for visual regression tests.

## Vision AI comparison (TC17 — recommended POC)

`WebUIUtils.compareScreenshotsWithVisionAi()` sends baseline and current PNGs to **Claude on AWS Bedrock** (default: `us.anthropic.claude-sonnet-4-6`) and returns a **structured JSON diff** — not a pixel/SSIM pass/fail.

- **Output:** `Reports/vision_diff.json` plus ValidationReport entries with status **Info** (report-only; test does not fail on diff content in Phase 1)
- **Note:** If report rows showed **Fail** despite a passing test, ensure `ValidationReport` accepts `Info` status (fixed in framework; re-run after refresh)
- **Override model:** set optional Katalon global `BedrockVisionModelId` (e.g. `us.anthropic.claude-opus-4-8`)
- **AWS:** `AwsAccessKeyId`, `AwsSecretAccessKey`, optional `AwsRegion` in profile, or `~/.aws/credentials`
- **Python:** `boto3` (see `requirements.txt`)

## SSIM comparison (legacy)

`WebUIUtils.verifyScreenshotMatchesBaseline()` uses **SSIM (structural similarity)** locally. Optional Bedrock one-sentence explanation on FAIL only.

- Threshold default **1.0** in script (README historically suggested 0.99 for tolerance)
- **Python:** `Pillow`, `numpy`, `scikit-image`

## Convention

- **Path pattern:** `InputFiles/CRDC/Screenshots/<pageOrTestId>_baseline.png`
- **Examples:**
  - `DataSubmission_Summary_baseline-TC05.png` – statistics charts after TC05 validation (TC17)
  - `DataSubmission_ValidationResults_baseline.png` – validation results tab

## Creating a baseline

1. Run the test or navigate manually to the page you want to baseline (use a known-good build).
2. Set a fixed viewport (e.g. 1920×1080) so future runs match.
3. Take a screenshot and save it here with the `_baseline.png` suffix.
4. Pass this path as the baseline argument in your test script.

## Requirements

- Profile/AppKey set to **CRDC** so `Utils.getPythonFilePath()` resolves to `PythonFiles/CRDC/`.
