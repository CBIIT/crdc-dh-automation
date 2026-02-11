# Plan: Add CLI Source Folder for Jenkins Compatibility

## 1. Why the current CLI doesn’t work in Jenkins

- **Current setup**: `CliUploader.groovy` runs a **pre-built binary** from `Tools/CRDC-CLI/`:
  - `uploader` on macOS/Linux
  - `uploader.exe` on Windows  
  See `runUploaderProcess()` in `Keywords/utilities/CliUploader.groovy` (lines 196–235).

- **Jenkins environment**: The pipeline uses a Docker agent `docker-katalon-ch` (see `jenkins/Jenkinsfile_Test_Scheduler`). The image is almost certainly **Linux**.

- **Result**: The binary in `Tools/CRDC-CLI/` is almost certainly built for **Mac or Windows**. A Mac/Windows binary does not run on Linux, so the CLI fails in Jenkins. There is no Python-based fallback today.

---

## 2. Existing code analysis

### 2.1 Automation project (crdc-dh-automation)

| Component | Role |
|-----------|------|
| **Keywords/utilities/CliUploader.groovy** | Builds YAML config, runs `uploader`/`uploader.exe`, checks exit code and stdout, verifies `tmp/upload-report-*.tsv`. |
| **Tools/CRDC-CLI/** | Contains `uploader` (binary) and README. No source. |
| **Profile variables** | `CRDC_CLI_DIR`, `CRDC_API_TOKEN`, `CRDC_API_URL`, submission ID (from URL), data path, manifest path, optional archive manifest, retries, overwrite, dryrun, id-field. |
| **Test** | `TC11_CRDC-DataSubmissions-CliUpload` calls `CustomKeywords.'utilities.CliUploader.uploadDataFiles'('TC11')`. |

Config built by Groovy matches the CLI’s expected format (see below).

### 2.2 CLI source (crdc-datahub-cli-uploader-src 2)

| Item | Details |
|------|--------|
| **Entry point** | `src/uploader.py` with `--config <path>` (same YAML as binary). |
| **Config format** | YAML under top-level key `Config:` with `api-url`, `token`, `submission`, `type`, `data`, `manifest`, optional `archive_manifest`, `id-field`, `retries`, `overwrite`, `dryrun`. Matches what `buildConfigYaml()` in CliUploader.groovy produces. |
| **Working directory** | Must be the **CLI repo root** so that `./tmp/upload-report-<timestamp>.tsv` is created under that root (see `src/uploader.py` ~line 154). |
| **Dependencies** | Root `requirements.txt`: pyyaml, boto3, requests, requests_aws4auth, rich, pandas, pytest. Submodule `src/bento` (bento-common) with its own requirements. |
| **Python** | README states Python 3.13+. |

So: the same temp YAML file and the same success/report checks can be used for both binary and source; the only difference is **how** the process is started (binary vs `python3 src/uploader.py`).

---

## 3. Required changes (plan)

### 3.1 Add CLI source into the repo

- **Option A (recommended)**  
  - Copy or clone the CLI source (including `src/bento` submodule) into the project, e.g. **`Tools/CRDC-CLI-Source/`** (or replace/merge under `Tools/CRDC-CLI/` if you want a single CLI directory).  
  - If you use a separate folder like `Tools/CRDC-CLI-Source/`, keep the existing `Tools/CRDC-CLI/` for local runs that still use the binary (optional).

- **Option B**  
  - Point `CRDC_CLI_DIR` in the execution profile to an existing copy of the source (e.g. on the Jenkins agent). Less portable; not ideal for version control.

- **Submodule**  
  - The CLI repo has `.gitmodules` for `src/bento`. When adding the source, either:
  - Clone with `--recurse-submodules`, or  
  - Copy the repo including an already-initialized `src/bento` so that `from bento.common.utils` etc. resolve.

### 3.2 Support “source mode” in CliUploader.groovy

- **Detection**  
  - Prefer **explicit** control: e.g. new profile variable **`CRDC_CLI_USE_SOURCE`** (default `false`).  
  - Optional: if `CRDC_CLI_USE_SOURCE` is true **or** the binary is missing/not executable, use source mode.

- **Behavior when using source**  
  - **CLI directory** = directory that contains `src/uploader.py` (e.g. `Tools/CRDC-CLI-Source` or `Tools/CRDC-CLI` if source is there).  
  - **Command**: run Python with the same config file and CWD:
    - `python3 src/uploader.py --config <absolute-path-to-temp-config.yml>`
  - **Working directory** = that CLI root (so `./tmp/` is created there and `verifyCliUploadReport(cliDir, output)` still finds `tmp/upload-report-*.tsv`).
  - **Python executable**: Reuse existing logic from `Utils.getPythonExecutablePath()` so that in Jenkins (when `JENKINS_HOME` is set) it uses `python3`; locally you can keep using a path or `python3` as configured.

- **No change** to:
  - Config building (`buildConfigYaml`),
  - Temp file creation,
  - Exit code and stdout checks,
  - `verifyCliUploadOutput` / `verifyCliUploadReport`.

Only the **command** and **working directory** in `runUploaderProcess()` (or a small helper that builds the command list) need to branch on “binary vs source”.

### 3.3 Jenkins / Docker environment

- **Python 3**  
  - The image `docker-katalon-ch` must have **Python 3.13+** (or at least 3.9+) and `pip` available so that `python3 src/uploader.py` runs.

- **Dependencies**  
  - Install CLI dependencies before (or when) running the CLI in Jenkins:
    - From CLI root: `pip3 install -r requirements.txt`
    - If `src/bento` is a real submodule with its own `requirements.txt`, install that too (e.g. `pip3 install -r src/bento/requirements.txt`) unless the image already includes those packages.

- **Where to install**  
  - **Option 1**: Add a pipeline stage (e.g. “Prepare CLI”) that runs once per job when using source mode: e.g. `pip3 install -r Tools/CRDC-CLI-Source/requirements.txt`.  
  - **Option 2**: Bake Python + these dependencies into the `docker-katalon-ch` image so no per-job install is needed.

- **Profile**  
  - In the execution profile used by Jenkins, set:
    - `CRDC_CLI_USE_SOURCE = true`
    - `CRDC_CLI_DIR` = path to the CLI source root (e.g. `Tools/CRDC-CLI-Source` relative to project dir).

### 3.4 Optional: timeout and paths

- **Timeout**: Already 30 minutes in Groovy; keep as-is unless you need more for large uploads.
- **Paths**: Config uses absolute paths for `data`, `manifest`, and optional `archive_manifest`; Groovy already resolves these. No change needed for source mode.

---

## 4. Summary checklist

| # | Task |
|---|------|
| 1 | Add CLI source (and bento) under the repo, e.g. `Tools/CRDC-CLI-Source/`. |
| 2 | In `CliUploader.groovy`: add `CRDC_CLI_USE_SOURCE` (and optionally “no binary” fallback); when source mode, run `python3 src/uploader.py --config <config>` with CWD = CLI root; reuse `Utils.getPythonExecutablePath()` for the Python executable. |
| 3 | Ensure Jenkins Docker image has Python 3.13+ (or 3.9+) and install CLI (and bento) requirements there or in a “Prepare CLI” stage. |
| 4 | Set Jenkins execution profile: `CRDC_CLI_USE_SOURCE = true`, `CRDC_CLI_DIR = Tools/CRDC-CLI-Source` (or equivalent). |
| 5 | Run TC11 (and any other CLI tests) in Jenkins to confirm upload and report verification pass. |

---

## 5. File-level change summary

- **Keywords/utilities/CliUploader.groovy**
  - Read `CRDC_CLI_USE_SOURCE` (and optionally check for binary existence).
  - In `runUploaderProcess()` (or equivalent): if source mode, set `command = [ pythonExe, 'src/uploader.py', '--config', configFile.getAbsolutePath() ]`, `workingDir = cliDir` (CLI source root), and run that instead of the binary.
  - Use `Utils.getPythonExecutablePath()` for `pythonExe` when in source mode.

- **New/updated under Tools/**
  - Add `Tools/CRDC-CLI-Source/` (or merge into `Tools/CRDC-CLI/`) with full CLI source + bento.

- **Jenkins**
  - Either add a stage to install Python dependencies when `CRDC_CLI_USE_SOURCE` is true, or document/update the Docker image so Python + requirements are preinstalled.

- **Execution profile (Jenkins)**
  - `CRDC_CLI_USE_SOURCE = true`, `CRDC_CLI_DIR = Tools/CRDC-CLI-Source` (or the path you chose).

No changes are required to the Python CLI itself or to the YAML config format; the same config and the same success/report checks apply for both binary and source.
