---
name: testing-syntax-codex
description: Run Syntax Codex locally and verify its core browser chat, attachment, provider, and export workflows end-to-end.
---

# Testing Syntax Codex

## Devin Secrets Needed

None for the Demo Engine flow. A provider-specific API key is only needed when explicitly testing a live OpenAI-compatible endpoint.

## Start the app

From the repository root:

```bash
python3 -m http.server 4173 --bind 127.0.0.1
```

Open `http://127.0.0.1:4173/` in Chrome. Clear `localStorage` and `sessionStorage` before a clean test run.

## Core browser flow

1. Use **Tạo mã** to verify prompt population, Code mode, formatted Markdown/code output, chat history, and the code-copy state.
2. Create a second chat and attach a small source fixture. Use a prompt containing both `debug` and a language name to verify debug intent takes precedence over generic language templates.
3. Open the model picker and verify at least one provider preset fills both endpoint and model.
4. Return to **Demo Engine**, export the active chat, and inspect the downloaded Markdown file from the shell.
5. Check the browser console after the complete flow and report any error.

## Evidence

- Record one focused browser session with structured annotations.
- Capture full-screen screenshots for generated code, attached-source debugging, provider values, and download completion.
- Verify exported headings and attached source text using `rg`.
- If any assertion fails, stop the recording, fix the issue outside test mode, clear browser storage, and restart the full procedure.

## Useful checks

```bash
node -e 'const fs=require("fs");const html=fs.readFileSync("index.html","utf8");const script=[...html.matchAll(/<script>([\s\S]*?)<\/script>/g)][0][1];new Function(script);console.log("JavaScript syntax OK")'
git diff --check
```
