package com.hub.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
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

        // 1. Setup Camera Hardware for Native Flashlight
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0]; // Get primary rear camera
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Setup WebView
        webView = new WebView(this);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        // 3. Connect JavaScript to Native Android Actions
        webView.addJavascriptInterface(new Object() {
            
            // Safely open settings without double-jumping
            @JavascriptInterface
            public void openSetting(String action, String fallbackAction) {
                try {
                    Intent intent = new Intent("android.settings." + action);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    // Check if the specific setting exists on this phone
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                        return; // Stop here! Do not run fallback.
                    }
                    
                    // Only run fallback if the primary setting is completely missing
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

            // Native hardware flashlight toggle
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
