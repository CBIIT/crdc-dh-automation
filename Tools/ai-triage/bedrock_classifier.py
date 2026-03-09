"""
Failure classifiers for the AI triage agent.

RuleBasedClassifier: pattern-matching against known flaky/environment error signatures.
BedrockClassifier:   sends unclassified failures to Amazon Bedrock for LLM analysis.
"""

import json
from typing import Any


class RuleBasedClassifier:
    """Classify failures by matching error messages against known patterns."""

    FLAKY = "FLAKY"
    ENVIRONMENT = "ENVIRONMENT"
    REAL = "REAL"
    UNKNOWN = "UNKNOWN"

    def __init__(
        self,
        flaky_patterns: list[str] | None = None,
        environment_patterns: list[str] | None = None,
    ):
        self.flaky_patterns = [p.lower() for p in (flaky_patterns or [])]
        self.environment_patterns = [p.lower() for p in (environment_patterns or [])]

    def classify(self, error_message: str) -> dict[str, str]:
        if not error_message:
            return {
                "classification": self.REAL,
                "reason": "No error message available",
                "confidence": "LOW",
                "suggestion": "Check test logs manually",
            }

        lower_msg = error_message.lower()

        for pattern in self.flaky_patterns:
            if pattern in lower_msg:
                return {
                    "classification": self.FLAKY,
                    "reason": f"Matched flaky pattern: '{pattern}'",
                    "confidence": "HIGH",
                    "suggestion": "Transient issue — should pass on retry.",
                }

        for pattern in self.environment_patterns:
            if pattern in lower_msg:
                return {
                    "classification": self.ENVIRONMENT,
                    "reason": f"Matched environment pattern: '{pattern}'",
                    "confidence": "HIGH",
                    "suggestion": "Fix the environment configuration.",
                }

        return {
            "classification": self.UNKNOWN,
            "reason": "No known pattern matched",
            "confidence": "LOW",
            "suggestion": "Investigate manually or escalate to LLM analysis.",
        }


class BedrockClassifier:
    """
    Send unclassified failures to Amazon Bedrock for LLM-powered analysis.
    Model-agnostic — works with any Bedrock-supported model by changing model_id.
    """

    SYSTEM_PROMPT = (
        "You are a test failure triage specialist for a Katalon/Selenium UI automation suite "
        "that tests the CRDC Data Hub application (Cancer Research Data Commons). "
        "The suite uses Groovy keywords, Selenium WebDriver, a CLI uploader tool, and "
        "ValidationReport HTML for assertions. "
        "Classify failures accurately and concisely."
    )

    def __init__(self, model_id: str, region: str, max_tokens: int = 500):
        import boto3

        self.client = boto3.client("bedrock-runtime", region_name=region)
        self.model_id = model_id
        self.max_tokens = max_tokens

    def classify_failure(
        self, test_name: str, error_message: str, log_excerpt: str
    ) -> dict[str, str]:
        prompt = self._build_prompt(test_name, error_message, log_excerpt)

        body = self._build_request_body(prompt)
        response = self.client.invoke_model(
            modelId=self.model_id,
            body=json.dumps(body),
        )

        response_body = json.loads(response["body"].read())
        return self._parse_response(response_body)

    def _build_prompt(
        self, test_name: str, error_message: str, log_excerpt: str
    ) -> str:
        return f"""Analyze this test failure and respond in EXACTLY this JSON format:
{{
  "classification": "<FLAKY|ENVIRONMENT|REAL_BUG|TEST_DATA|UNKNOWN>",
  "reason": "<1-2 sentence root cause explanation>",
  "suggestion": "<1-2 sentence fix suggestion>",
  "confidence": "<HIGH|MEDIUM|LOW>"
}}

Test case: {test_name}

Error message:
{error_message[:1500]}

Log excerpt (around failure):
{log_excerpt[:1500]}

Respond ONLY with the JSON object, no other text."""

    def _build_request_body(self, prompt: str) -> dict[str, Any]:
        """Build the request body. Supports Anthropic Claude and Amazon Titan models."""
        if "anthropic" in self.model_id.lower():
            return {
                "anthropic_version": "bedrock-2023-05-31",
                "system": self.SYSTEM_PROMPT,
                "messages": [{"role": "user", "content": prompt}],
                "max_tokens": self.max_tokens,
                "temperature": 0.1,
            }
        elif "titan" in self.model_id.lower():
            return {
                "inputText": f"{self.SYSTEM_PROMPT}\n\n{prompt}",
                "textGenerationConfig": {
                    "maxTokenCount": self.max_tokens,
                    "temperature": 0.1,
                },
            }
        elif "meta" in self.model_id.lower() or "llama" in self.model_id.lower():
            return {
                "prompt": f"<s>[INST] <<SYS>>\n{self.SYSTEM_PROMPT}\n<</SYS>>\n\n{prompt} [/INST]",
                "max_gen_len": self.max_tokens,
                "temperature": 0.1,
            }
        else:
            return {
                "anthropic_version": "bedrock-2023-05-31",
                "system": self.SYSTEM_PROMPT,
                "messages": [{"role": "user", "content": prompt}],
                "max_tokens": self.max_tokens,
                "temperature": 0.1,
            }

    def _parse_response(self, response_body: dict) -> dict[str, str]:
        """Extract classification from the LLM response, handling different model formats."""
        text = ""
        if "content" in response_body:
            # Anthropic Claude format
            for block in response_body["content"]:
                if block.get("type") == "text":
                    text = block["text"]
                    break
        elif "results" in response_body:
            # Amazon Titan format
            text = response_body["results"][0].get("outputText", "")
        elif "generation" in response_body:
            # Meta Llama format
            text = response_body["generation"]
        else:
            text = json.dumps(response_body)

        try:
            start = text.index("{")
            end = text.rindex("}") + 1
            parsed = json.loads(text[start:end])
            return {
                "classification": parsed.get("classification", "UNKNOWN"),
                "reason": parsed.get("reason", "LLM did not provide a reason"),
                "suggestion": parsed.get("suggestion", ""),
                "confidence": parsed.get("confidence", "MEDIUM"),
            }
        except (ValueError, json.JSONDecodeError):
            return {
                "classification": "UNKNOWN",
                "reason": f"Could not parse LLM response: {text[:200]}",
                "suggestion": "Investigate manually",
                "confidence": "LOW",
            }
