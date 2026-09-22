# Gmail API files (not committed)

Drop copies of these files here (same files used by the local Gmail API quickstart):

- `credentials.json` — OAuth client from Google Cloud (Desktop app)
- `token.json` — refresh token after one browser login as `qa@essential-soft.com`

Ask a teammate who already authorized the inbox for a copy. Do not commit either file.

The verifier defaults to this folder:

```bash
python PythonFiles/CRDC/VerifyNotificationEmails.py
```

Override with `--credentials`, `--token`, or env `GMAIL_CREDENTIALS_PATH` / `GMAIL_TOKEN_PATH`.
