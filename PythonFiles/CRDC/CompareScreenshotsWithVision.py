#!/usr/bin/env python3
"""
Compare baseline and current screenshots using Claude vision on AWS Bedrock.
Returns structured JSON diff (report-only POC; no pixel/SSIM gate).

Prints to stdout: line 1 = OK or ERROR, line 2 = path to JSON file or error message.
Verbose logs go to stderr.
Used by Katalon keyword WebUIUtils.compareScreenshotsWithVisionAi().
"""
import argparse
import base64
import json
import re
import sys
from typing import Any, Dict, List

try:
    import boto3
except ImportError:
    boto3 = None  # type: ignore

DEFAULT_REGION = "us-east-1"
DEFAULT_MODEL = "us.anthropic.claude-sonnet-4-6"
DEFAULT_MAX_TOKENS = 2000

CHANGE_TYPES = "unchanged | cosmetic | structural | content | missing | added"

JSON_SCHEMA_EXAMPLE = {
    "summary": "One sentence overall assessment",
    "elements": [
        {
            "name": "Region or UI element name",
            "baseline": ["observation in baseline image"],
            "current": ["observation in current image"],
            "changeType": "unchanged",
        }
    ],
}


def load_image_b64(path: str) -> str:
    with open(path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")


def build_prompt(context: str) -> str:
    context_line = f"\nContext: {context}\n" if context.strip() else "\n"
    schema = json.dumps(JSON_SCHEMA_EXAMPLE, indent=2)
    return (
        "You are a QA engineer comparing two UI screenshots for visual regression.\n"
        "Image 1 is the BASELINE (expected). Image 2 is the CURRENT (actual).\n"
        f"{context_line}\n"
        "Compare them and return ONLY valid JSON (no markdown fences, no preamble) matching this schema:\n"
        f"{schema}\n\n"
        "Rules:\n"
        "- List meaningful UI regions in elements (charts, legends, titles, empty states, overlays).\n"
        "- Ignore subpixel antialiasing, font smoothing, and minor color shifts — classify those as changeType unchanged or cosmetic.\n"
        "- Use changeType structural or missing only for real layout breaks, missing widgets, or wrong screen state.\n"
        "- baseline and current are arrays of short observation strings per element.\n"
        f"- changeType must be one of: {CHANGE_TYPES}.\n"
        "- summary is one sentence describing the overall comparison.\n"
    )


def extract_text_from_bedrock_response(parsed: Dict[str, Any]) -> str:
    text = ""
    content = parsed.get("content")
    if isinstance(content, list):
        for item in content:
            if isinstance(item, dict) and item.get("type") == "text":
                text += item.get("text", "")
    return text.strip()


def strip_json_fences(text: str) -> str:
    cleaned = text.strip()
    cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned, flags=re.IGNORECASE)
    cleaned = re.sub(r"\s*```$", "", cleaned)
    return cleaned.strip()


def parse_diff_json(text: str) -> Dict[str, Any]:
    cleaned = strip_json_fences(text)
    data = json.loads(cleaned)
    if not isinstance(data, dict):
        raise ValueError("Response JSON must be an object")
    if "elements" not in data or not isinstance(data["elements"], list):
        raise ValueError("Response JSON must include elements array")
    return data


def invoke_vision_diff(
    baseline_path: str,
    current_path: str,
    context: str,
    region: str,
    model_id: str,
    max_tokens: int,
    verbose: bool,
) -> str:
    if boto3 is None:
        raise RuntimeError("boto3 is required; install with: pip install boto3")

    baseline_b64 = load_image_b64(baseline_path)
    current_b64 = load_image_b64(current_path)
    prompt = build_prompt(context)

    content: List[Dict[str, Any]] = [
        {
            "type": "image",
            "source": {"type": "base64", "media_type": "image/png", "data": baseline_b64},
        },
        {
            "type": "image",
            "source": {"type": "base64", "media_type": "image/png", "data": current_b64},
        },
        {"type": "text", "text": prompt},
    ]
    body = {
        "anthropic_version": "bedrock-2023-05-31",
        "messages": [{"role": "user", "content": content}],
        "max_tokens": max_tokens,
    }

    if verbose:
        print(f"VERBOSE: Bedrock model={model_id}, region={region}", file=sys.stderr)

    client = boto3.client("bedrock-runtime", region_name=region)
    response = client.invoke_model(
        body=json.dumps(body).encode("utf-8"),
        modelId=model_id,
        accept="application/json",
        contentType="application/json",
    )
    raw = response["body"].read().decode("utf-8")
    parsed = json.loads(raw)
    return extract_text_from_bedrock_response(parsed)


def get_diff_with_retry(
    baseline_path: str,
    current_path: str,
    context: str,
    region: str,
    model_id: str,
    max_tokens: int,
    verbose: bool,
) -> Dict[str, Any]:
    text = invoke_vision_diff(
        baseline_path, current_path, context, region, model_id, max_tokens, verbose
    )
    try:
        return parse_diff_json(text)
    except (json.JSONDecodeError, ValueError) as first_err:
        if verbose:
            print(f"VERBOSE: JSON parse failed ({first_err}), retrying with stricter prompt", file=sys.stderr)
        retry_prompt = (
            build_prompt(context)
            + "\nYour previous response was not valid JSON. Reply with ONLY the JSON object, no markdown.\n"
        )
        if boto3 is None:
            raise RuntimeError("boto3 is required") from first_err

        baseline_b64 = load_image_b64(baseline_path)
        current_b64 = load_image_b64(current_path)
        content: List[Dict[str, Any]] = [
            {
                "type": "image",
                "source": {"type": "base64", "media_type": "image/png", "data": baseline_b64},
            },
            {
                "type": "image",
                "source": {"type": "base64", "media_type": "image/png", "data": current_b64},
            },
            {"type": "text", "text": retry_prompt},
        ]
        body = {
            "anthropic_version": "bedrock-2023-05-31",
            "messages": [{"role": "user", "content": content}],
            "max_tokens": max_tokens,
        }
        client = boto3.client("bedrock-runtime", region_name=region)
        response = client.invoke_model(
            body=json.dumps(body).encode("utf-8"),
            modelId=model_id,
            accept="application/json",
            contentType="application/json",
        )
        raw = response["body"].read().decode("utf-8")
        parsed = json.loads(raw)
        text = extract_text_from_bedrock_response(parsed)
        try:
            return parse_diff_json(text)
        except (json.JSONDecodeError, ValueError) as second_err:
            raise ValueError(f"Could not parse Bedrock JSON after retry: {second_err}") from second_err


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Compare screenshots via Bedrock vision; output structured JSON diff."
    )
    parser.add_argument("baseline_path", help="Path to baseline/reference screenshot (PNG)")
    parser.add_argument("current_path", help="Path to current screenshot (PNG)")
    parser.add_argument("--context", default="", help="Optional context for the comparison prompt")
    parser.add_argument("--region", default=DEFAULT_REGION, help="AWS region for Bedrock")
    parser.add_argument("--model", default=DEFAULT_MODEL, help="Bedrock model ID or inference profile")
    parser.add_argument(
        "--output-json",
        metavar="PATH",
        required=True,
        help="Path to write structured diff JSON (e.g. Reports/vision_diff.json)",
    )
    parser.add_argument("--verbose", action="store_true", help="Print diagnostics to stderr")
    args = parser.parse_args()

    try:
        diff_data = get_diff_with_retry(
            args.baseline_path,
            args.current_path,
            args.context,
            args.region,
            args.model,
            DEFAULT_MAX_TOKENS,
            args.verbose,
        )
        with open(args.output_json, "w", encoding="utf-8") as f:
            json.dump(diff_data, f, indent=2)
        if args.verbose:
            print(f"VERBOSE: Wrote diff JSON to {args.output_json}", file=sys.stderr)
        print("OK")
        print(args.output_json)
        sys.exit(0)
    except FileNotFoundError as e:
        print("ERROR")
        print(str(e))
        sys.exit(1)
    except Exception as e:
        print("ERROR")
        print(str(e))
        sys.exit(1)


if __name__ == "__main__":
    main()
