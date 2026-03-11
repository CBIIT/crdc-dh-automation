#!/usr/bin/env python3
"""
Compare a current screenshot to a baseline image using Claude on AWS Bedrock.
Prints to stdout: line 1 = PASS or FAIL, line 2 = reason.
Used by Katalon keyword WebUIUtils.verifyScreenshotMatchesBaseline().
"""
import argparse
import base64
import json
import sys

try:
    import boto3
except ImportError:
    print("FAIL", file=sys.stderr)
    print("boto3 not installed", file=sys.stderr)
    sys.exit(1)

DEFAULT_REGION = "us-east-1"
DEFAULT_MODEL = "anthropic.claude-3-sonnet-20240229-v1:0"


def encode_image(path: str) -> str:
    with open(path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")


def compare_screenshots(
    baseline_path: str, current_path: str, region: str, model_id: str, verbose: bool = False
) -> tuple[str, str, str]:
    """
    Send both images to Bedrock, get PASS/FAIL + reason.
    Returns (result, reason, raw_response) where result is "PASS" or "FAIL".
    raw_response is the full model text when verbose=True, else "".
    """
    baseline_b64 = encode_image(baseline_path)
    current_b64 = encode_image(current_path)

    prompt = """You are a QA visual verification assistant. You will see two screenshots.

Image 1 (first image): BASELINE - the expected reference.
Image 2 (second image): CURRENT - the screenshot to verify.

Task: Decide if the CURRENT screenshot shows the SAME page/screen as the BASELINE.

You MUST reply FAIL if:
- The current image is a different page, application, or website (e.g. login page, different app, unrelated content).
- The current image shows clearly different subject matter, layout, or structure from the baseline.
- The two images are not the same type of screen (e.g. baseline is a data submission page with charts but current is something else).
When in doubt, or if the two images show clearly different content or layout, reply FAIL.

Reply PASS only if both images are clearly the same screen/page: same application, same section, same type of charts and content. Minor differences in non-essential text (e.g. timestamps, counts that are expected to change) are acceptable for PASS.

Reply with exactly two lines:
Line 1: Either the word PASS or the word FAIL (all caps, nothing else on that line).
Line 2: One short reason (e.g. "Charts and layout match" or "Current shows different page than baseline").
"""

    content = [
        {"type": "text", "text": prompt},
        {
            "type": "image",
            "source": {
                "type": "base64",
                "media_type": "image/png",
                "data": baseline_b64,
            },
        },
        {
            "type": "image",
            "source": {
                "type": "base64",
                "media_type": "image/png",
                "data": current_b64,
            },
        },
    ]

    body = {
        "anthropic_version": "bedrock-2023-05-31",
        "messages": [
            {
                "role": "user",
                "content": content,
            }
        ],
        "max_tokens": 500,
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
    text = ""
    if "content" in parsed and isinstance(parsed["content"], list):
        for item in parsed["content"]:
            if item.get("type") == "text":
                text += item.get("text", "")

    lines = [line.strip() for line in text.strip().splitlines() if line.strip()]
    if not lines:
        return "FAIL", "No response from model", text if verbose else ""

    first = lines[0].upper()
    if first.startswith("PASS"):
        result = "PASS"
    elif first.startswith("FAIL"):
        result = "FAIL"
    else:
        result = "FAIL"

    reason = lines[1] if len(lines) > 1 else (lines[0] if result == "FAIL" else "Match")
    return result, reason, text if verbose else ""


def main():
    parser = argparse.ArgumentParser(description="Compare current screenshot to baseline via Claude on Bedrock")
    parser.add_argument("current_path", help="Path to current screenshot (PNG)")
    parser.add_argument("baseline_path", help="Path to baseline/reference screenshot (PNG)")
    parser.add_argument("--region", default=DEFAULT_REGION, help="AWS region for Bedrock")
    parser.add_argument("--model", default=DEFAULT_MODEL, help="Bedrock model ID")
    parser.add_argument("--verbose", action="store_true", help="Print LLM call info and full model response to stdout")
    args = parser.parse_args()

    try:
        result, reason, raw_response = compare_screenshots(
            args.baseline_path, args.current_path, args.region, args.model, verbose=args.verbose
        )
    except FileNotFoundError as e:
        print("FAIL")
        print(str(e))
        sys.exit(1)
    except Exception as e:
        print("FAIL")
        print(f"Error: {e}")
        sys.exit(1)

    if args.verbose:
        print("VERBOSE: Calling Bedrock (region={}, model={}) with baseline and current image.".format(args.region, args.model))
        print("VERBOSE: Raw model response:")
        print(raw_response)
    print(result)
    print(reason)
    sys.exit(0 if result == "PASS" else 1)


if __name__ == "__main__":
    main()
