package com.hub.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebViewClient;
import android.webkit.PermissionRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {
    private WebView webView;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isTorchOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Setup Camera Hardware
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager.getCameraIdList().length > 0) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Setup WebView & Force Full Screen Size
        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);

        // 3. Error Catcher (Will show text instead of a white screen if it fails)
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                String errorHtml = "<html><body style='padding:20px; font-family:sans-serif;'>" +
                        "<h2 style='color:red;'>File Not Found!</h2>" +
                        "<p>The app could not find your index.html file.</p>" +
                        "<p><b>Please verify your exact GitHub folder structure is:</b><br/>" +
                        "app / src / main / assets / index.html</p>" +
                        "</body></html>";
                view.loadData(errorHtml, "text/html", "UTF-8");
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        // 4. Native Android Bridge
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void openSetting(String action, String fallbackAction) {
                try {
                    Intent intent = new Intent("android.settings." + action);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                        return; 
                    }
                    
                    if (fallbackAction != null && !fallbackAction.isEmpty()) {
                        Intent fallback = new Intent("android.settings." + fallbackAction);
                        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (fallback.resolveActivity(getPackageManager()) != null) {
                            startActivity(fallback);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @JavascriptInterface
            public boolean toggleNativeTorch() {
                try {
                    if (cameraId != null) {
                        isTorchOn = !isTorchOn;
                        cameraManager.setTorchMode(cameraId, isTorchOn);
                        return isTorchOn;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        }, "AndroidNative");

        setContentView(webView);
        
        // Ensure this URL is exactly this:
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
