# Baseline screenshots for visual comparison

This folder stores **baseline (reference) screenshots** used by `WebUIUtils.verifyScreenshotMatchesBaseline()`.

## How comparison works

Comparison uses **SSIM (structural similarity)** locally — no LLM is used for pass/fail. The script loads baseline and current images, aligns their dimensions if needed, and computes an SSIM score in [0, 1]. If score ≥ threshold (default **0.99**), the test passes.

- **1.0** – identical
- **0.99+** – nearly identical (recommended default; allows minor rendering/timestamp differences)
- **0.95–0.99** – minor differences
- **&lt; 0.95** – major change or different screen

Optional: when the comparison **fails**, you can pass `explainWithLlm: true` so the keyword calls Bedrock once to get a one-sentence description of the difference (e.g. for the report). AWS credentials are only required in that case.

## Convention

- **Path pattern:** `InputFiles/CRDC/Screenshots/<pageOrTestId>_baseline.png`
- **Examples:**
  - `DataSubmission_Summary_baseline.png` – data submission summary/charts view
  - `DataSubmission_ValidationResults_baseline.png` – validation results tab

## Creating a baseline

1. Run the test or navigate manually to the page you want to baseline (use a known-good build).
2. Set a fixed viewport (e.g. 1920×1080) so future runs match.
3. Take a screenshot and save it here with the `_baseline.png` suffix.
4. In your test script, pass this path as the baseline argument to `verifyScreenshotMatchesBaseline(currentPath, baselinePath, reportSection)`.

## Requirements

- **Python:** `Pillow`, `numpy`, `scikit-image` (see project `requirements.txt`). No AWS dependency for basic comparison.
- **Optional (for LLM explanation on fail):** AWS credentials in Katalon profile (`AwsAccessKeyId`, `AwsSecretAccessKey`, optional `AwsRegion`) or environment / `~/.aws/credentials`, and `boto3`.
- Profile/AppKey set to CRDC so `Utils.getPythonFilePath()` resolves to `PythonFiles/CRDC/`.
