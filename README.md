# hybasOS

hybasOS is a browser-based operating system concept focused on minimalism and efficiency.

## Syntax Codex

Open `index.html` to use Syntax Codex, a responsive AI coding assistant with:

- Programming, debugging, explanation, and structured logic modes
- OpenAI, OpenRouter, Groq, and custom OpenAI-compatible endpoints
- Local chat history, file attachments, Markdown/code rendering, and export
- An offline demo engine that works without an API key
- Installable PWA support for desktop and Android browsers
- A native Android WebView package with file upload and Markdown export

API keys are stored only in `sessionStorage` and are removed when the browser tab closes. A backend proxy is recommended before publishing a production instance.

### Web and PWA

Serve the repository over HTTPS or localhost, then open `index.html`. On Android Chrome, use the **Cài app** action when it appears or choose **Add to Home screen** from the browser menu.

### Android APK

The Android project packages the current web assets during each build:

```bash
cd android
./gradlew assembleDebug
```

The debug APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`. Android SDK 35 and Java 17 are required.

## hybasOS

Open `hybasOS.html` to run the original virtual operating system interface.
