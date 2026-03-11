#!/usr/bin/env python3
"""
Compare a current screenshot to a baseline image using SSIM (structural similarity).
Pass/fail is deterministic; no LLM is used for the decision.
Optionally, on FAIL, call Claude on Bedrock to get a short explanation of the difference.

Prints to stdout: line 1 = PASS or FAIL, line 2 = reason.
Used by Katalon keyword WebUIUtils.verifyScreenshotMatchesBaseline().
"""
import argparse
import base64
import io
import json
import sys
from typing import Optional, Tuple

import numpy as np
from PIL import Image

try:
    from skimage.metrics import structural_similarity as ssim
except ImportError:
    ssim = None  # type: ignore

# Optional: only needed when --explain-with-llm is used
try:
    import boto3
except ImportError:
    boto3 = None  # type: ignore

DEFAULT_THRESHOLD = 0.99
DEFAULT_REGION = "us-east-1"
DEFAULT_MODEL = "anthropic.claude-3-sonnet-20240229-v1:0"


def load_grayscale_array(path: str) -> np.ndarray:
    """Load image from path, convert to grayscale, return numpy array (uint8)."""
    with open(path, "rb") as f:
        data = f.read()
    img = Image.open(io.BytesIO(data))
    if img.mode not in ("L", "RGB", "RGBA"):
        img = img.convert("RGB")
    if img.mode != "L":
        img = img.convert("L")
    return np.array(img, dtype=np.uint8)


def compute_ssim(baseline_path: str, current_path: str) -> Tuple[float, Optional[np.ndarray]]:
    """
    Load baseline and current, align sizes (resize current to baseline), compute SSIM.
    Returns (score, diff_image). diff_image is the absolute difference (0-255); can be used to save a diff PNG.
    """
    gray_baseline = load_grayscale_array(baseline_path)
    gray_current = load_grayscale_array(current_path)

    if gray_baseline.shape != gray_current.shape:
        # Resize current to baseline dimensions
        pil_current = Image.fromarray(gray_current)
        pil_baseline = Image.fromarray(gray_baseline)
        try:
            resample = Image.Resampling.LANCZOS
        except AttributeError:
            resample = getattr(Image, "LANCZOS", Image.LINEAR)
        pil_current = pil_current.resize((pil_baseline.width, pil_baseline.height), resample)
        gray_current = np.array(pil_current, dtype=np.uint8)

    if ssim is None:
        raise RuntimeError("scikit-image is required; install with: pip install scikit-image")

    score, ssim_map = ssim(
        gray_baseline,
        gray_current,
        data_range=255,
        full=True,
    )
    # ssim_map is similarity per pixel (1 = same). For a "diff" image we use 1 - similarity so differences are bright.
    diff_uint8 = (np.clip(1.0 - ssim_map, 0, 1) * 255).astype(np.uint8) if ssim_map is not None else None
    return float(score), diff_uint8


def explain_with_llm(baseline_path: str, current_path: str, region: str, model_id: str) -> str:
    """Call Bedrock with both images; ask for one short sentence describing the difference. Returns that text or a fallback."""
    if boto3 is None:
        return "boto3 not installed; cannot get LLM explanation."
    try:
        with open(baseline_path, "rb") as f:
            baseline_b64 = base64.b64encode(f.read()).decode("utf-8")
        with open(current_path, "rb") as f:
            current_b64 = base64.b64encode(f.read()).decode("utf-8")
    except Exception as e:
        return str(e)

    prompt = "These two screenshots differ. In one short sentence, describe the main visual difference (e.g. layout change, different content, missing element). Reply with only that sentence, no preamble."

    content = [
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
        "max_tokens": 200,
    }
    try:
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
        return text.strip() or "LLM returned no text"
    except Exception as e:
        return f"LLM explanation failed: {e}"


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Compare current screenshot to baseline using SSIM. Pass/fail is local; optional LLM explains on fail."
    )
    parser.add_argument("current_path", help="Path to current screenshot (PNG)")
    parser.add_argument("baseline_path", help="Path to baseline/reference screenshot (PNG)")
    parser.add_argument(
        "--threshold",
        type=float,
        default=DEFAULT_THRESHOLD,
        help=f"SSIM threshold for PASS (default {DEFAULT_THRESHOLD}). Score >= threshold -> PASS.",
    )
    parser.add_argument(
        "--explain-with-llm",
        action="store_true",
        help="On FAIL, call Bedrock to get a one-sentence description of the difference (requires AWS creds).",
    )
    parser.add_argument("--region", default=DEFAULT_REGION, help="AWS region for Bedrock (only if --explain-with-llm)")
    parser.add_argument("--model", default=DEFAULT_MODEL, help="Bedrock model ID (only if --explain-with-llm)")
    parser.add_argument(
        "--save-diff",
        metavar="PATH",
        default=None,
        help="On FAIL, save diff image to this path (e.g. Reports/vision_diff.png).",
    )
    parser.add_argument("--verbose", action="store_true", help="Print SSIM score and dimensions to stderr")
    args = parser.parse_args()

    try:
        score, diff_image = compute_ssim(args.baseline_path, args.current_path)
    except FileNotFoundError as e:
        print("FAIL")
        print(str(e))
        sys.exit(1)
    except Exception as e:
        print("FAIL")
        print(f"Error: {e}")
        sys.exit(1)

    if args.verbose:
        print(f"VERBOSE: SSIM score = {score:.4f}, threshold = {args.threshold}", file=sys.stderr)

    passed = score >= args.threshold
    if passed:
        result = "PASS"
        reason = f"SSIM {score:.3f}"
    else:
        result = "FAIL"
        if args.explain_with_llm and boto3:
            reason = explain_with_llm(
                args.baseline_path, args.current_path, args.region, args.model
            )
        else:
            reason = f"SSIM {score:.3f} below threshold {args.threshold}"

        if args.save_diff and diff_image is not None:
            try:
                Image.fromarray(diff_image).save(args.save_diff)
                if args.verbose:
                    print(f"VERBOSE: Diff image saved to {args.save_diff}", file=sys.stderr)
            except Exception as e:
                if args.verbose:
                    print(f"VERBOSE: Could not save diff: {e}", file=sys.stderr)

    print(result)
    print(reason)
    sys.exit(0 if passed else 1)


if __name__ == "__main__":
    main()
