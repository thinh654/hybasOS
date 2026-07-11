package ai.syntaxcodex.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 100;
    private static final int STORAGE_PERMISSION_REQUEST = 101;

    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private PendingDownload pendingDownload;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(true);

        webView.addJavascriptInterface(new AndroidBridge(), "SyntaxCodexAndroid");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return openExternalUrl(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openExternalUrl(Uri.parse(url));
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> callback,
                FileChooserParams params
            ) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;

                Intent intent = params.createIntent();
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                    "text/plain",
                    "text/markdown",
                    "text/html",
                    "text/css",
                    "text/javascript",
                    "application/json"
                });
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (RuntimeException error) {
                    fileChooserCallback = null;
                    Toast.makeText(MainActivity.this, R.string.file_picker_error, Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/index.html");
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private boolean openExternalUrl(Uri uri) {
        if ("file".equals(uri.getScheme())) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (RuntimeException error) {
            Toast.makeText(this, R.string.link_error, Toast.LENGTH_LONG).show();
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || fileChooserCallback == null) return;
        fileChooserCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
        fileChooserCallback = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != STORAGE_PERMISSION_REQUEST || pendingDownload == null) return;
        PendingDownload download = pendingDownload;
        pendingDownload = null;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveMarkdown(download.filename, download.content);
        } else {
            Toast.makeText(this, R.string.storage_permission_error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void saveMarkdown(String filename, String content) {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload = new PendingDownload(filename, content);
            requestPermissions(
                new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                STORAGE_PERMISSION_REQUEST
            );
            return;
        }

        try {
            writeMarkdown(filename, content);
            Toast.makeText(this, R.string.export_success, Toast.LENGTH_LONG).show();
        } catch (IOException error) {
            Toast.makeText(this, R.string.export_error, Toast.LENGTH_LONG).show();
        }
    }

    private void writeMarkdown(String filename, String content) throws IOException {
        String safeFilename = filename.replaceAll("[\\\\/:*?\"<>|]", "-");
        byte[] data = content.getBytes(StandardCharsets.UTF_8);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, safeFilename);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/markdown");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Unable to create download");
            try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
                if (stream == null) throw new IOException("Unable to open download");
                stream.write(data);
            }
            return;
        }

        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Unable to create Downloads");
        try (OutputStream stream = new FileOutputStream(new File(directory, safeFilename))) {
            stream.write(data);
        }
    }

    public final class AndroidBridge {
        @JavascriptInterface
        public void downloadMarkdown(String filename, String content) {
            runOnUiThread(() -> saveMarkdown(filename, content));
        }
    }

    private static final class PendingDownload {
        final String filename;
        final String content;

        PendingDownload(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }
    }
}
