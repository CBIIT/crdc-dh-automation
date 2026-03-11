# Baseline screenshots for vision comparison

This folder stores **baseline (reference) screenshots** used by the vision comparison keyword `WebUIUtils.verifyScreenshotMatchesBaseline()`.

## Convention (Option A)

- **Path pattern:** `InputFiles/CRDC/Screenshots/<pageOrTestId>_baseline.png`
- **Examples:**
  - `DataSubmission_Summary_baseline.png` – data submission summary/charts view
  - `DataSubmission_ValidationResults_baseline.png` – validation results tab

## Creating a baseline

1. Run the test or navigate manually to the page you want to baseline (use a known-good build).
2. Set a fixed viewport (e.g. 1920x1080) so future runs match.
3. Take a screenshot and save it here with the `_baseline.png` suffix.
4. In your test script, pass this path as the second argument to `verifyScreenshotMatchesBaseline(currentPath, baselinePath, reportSection)`.

## Requirements

- **AWS credentials for Bedrock:** Either set in the Katalon profile (recommended, same as other credentials) or via environment / `~/.aws/credentials`.
  - **Profile (recommended):** In your Katalon profile (e.g. CRDC), add variables: `AwsAccessKeyId`, `AwsSecretAccessKey`. Optional: `AwsRegion` (e.g. `us-east-1`). The vision keyword passes these into the Python process so it works locally and on Jenkins without extra setup.
  - **Otherwise:** Use `~/.aws/credentials` or environment variables `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (and optionally `AWS_DEFAULT_REGION`).
- Python with `boto3` (see project `requirements.txt`).
- Profile/AppKey set to CRDC so `Utils.getPythonFilePath()` resolves to `PythonFiles/CRDC/`.
