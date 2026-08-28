package com.hub.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isTorchOn = false;

    // Inner class to define setting items cleanly
    static class Setting {
        String title, desc, icon, primary, fallback;
        Setting(String t, String d, String i, String p, String f) {
            this.title=t; this.desc=d; this.icon=i; this.primary=p; this.fallback=f;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Hardware Flashlight
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager.getCameraIdList().length > 0) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (Exception e) { e.printStackTrace(); }

        findViewById(R.id.tile_torch).setOnClickListener(v -> {
            try {
                if (cameraId != null) {
                    isTorchOn = !isTorchOn;
                    cameraManager.setTorchMode(cameraId, isTorchOn);
                    TextView status = findViewById(R.id.status_torch);
                    status.setText(isTorchOn ? "On" : "Off");
                    v.setBackgroundColor(isTorchOn ? Color.parseColor("#e8f0fe") : Color.TRANSPARENT);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Torch unavailable", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Initialize Auto-Rotate Intent (API 31+)
        findViewById(R.id.tile_rotate).setOnClickListener(v -> 
            openSetting("AUTO_ROTATE_SETTINGS", "DISPLAY_SETTINGS")
        );

        // 3. Define the Settings List Items
        Setting[] allSettings = {
            new Setting("Wi-Fi", "Manage Wi-Fi networks", "📶", "WIFI_SETTINGS", ""),
            new Setting("Mobile Data", "Cellular data and usage", "📊", "DATA_ROAMING_SETTINGS", "NETWORK_OPERATOR_SETTINGS"),
            new Setting("Wi-Fi Calling", "Call over Wi-Fi", "📞", "WIFI_CALLING_SETTINGS", "WIRELESS_SETTINGS"),
            new Setting("Bluetooth", "Pair connected devices", "🔵", "BLUETOOTH_SETTINGS", ""),
            new Setting("SIM Manager", "SIM and network preferences", "💳", "WIRELESS_SETTINGS", ""),
            new Setting("Mobile Hotspot", "Share network via tethering", "🛜", "TETHER_SETTINGS", "WIRELESS_SETTINGS"),
            new Setting("Sound & Vibration", "Volume and haptics", "🔊", "SOUND_SETTINGS", ""),
            new Setting("Notifications", "App alerts and Do Not Disturb", "🔔", "ALL_APPS_NOTIFICATION_SETTINGS", "NOTIFICATION_SETTINGS"),
            new Setting("Display", "Brightness and dark theme", "☀️", "DISPLAY_SETTINGS", ""),
            new Setting("Battery", "Power saver and battery health", "🔋", "BATTERY_SAVER_SETTINGS", "SETTINGS"),
            new Setting("Security & Privacy", "Biometrics and screen lock", "🔒", "SECURITY_SETTINGS", ""),
            new Setting("Account & Backup", "Cloud sync and saved accounts", "☁️", "SYNC_SETTINGS", "SETTINGS"),
            new Setting("Keyboard", "On-screen input tools", "⌨️", "INPUT_METHOD_SETTINGS", ""),
            new Setting("Software Update", "Check for OS patches", "🔄", "SYSTEM_UPDATE_SETTINGS", "DEVICE_INFO_SETTINGS")
        };

        // 4. Dynamically Render the Settings List
        LinearLayout listContainer = findViewById(R.id.list_container);
        for (Setting s : allSettings) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(32, 40, 32, 40);
            row.setClickable(true);
            row.setFocusable(true);
            row.setTag((s.title + " " + s.desc).toLowerCase()); // Tag used for search filtering

            // Emoji Icon
            TextView icon = new TextView(this);
            icon.setText(s.icon);
            icon.setTextSize(22);
            icon.setPadding(0, 0, 48, 0);
            row.addView(icon);

            // Text Layout (Title + Desc)
            LinearLayout textBlock = new LinearLayout(this);
            textBlock.setOrientation(LinearLayout.VERTICAL);
            textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            
            TextView title = new TextView(this);
            title.setText(s.title);
            title.setTextSize(16);
            title.setTextColor(Color.parseColor("#1f1f1f"));
            title.setTypeface(null, Typeface.BOLD);
            textBlock.addView(title);

            TextView desc = new TextView(this);
            desc.setText(s.desc);
            desc.setTextSize(12);
            desc.setTextColor(Color.parseColor("#636363"));
            textBlock.addView(desc);
            
            row.addView(textBlock);

            // Click listener for intents
            row.setOnClickListener(v -> openSetting(s.primary, s.fallback));
            listContainer.addView(row);
        }

        // 5. Setup Search Filtering
        EditText searchBar = findViewById(R.id.search_bar);
        LinearLayout quickPanel = findViewById(R.id.quick_panel);
        
        searchBar.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String query = s.toString().toLowerCase().trim();
                quickPanel.setVisibility(query.isEmpty() ? View.VISIBLE : View.GONE);
                
                for (int i = 0; i < listContainer.getChildCount(); i++) {
                    View row = listContainer.getChildAt(i);
                    String tags = (String) row.getTag();
                    row.setVisibility(tags.contains(query) ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    // 6. Safe Intent Launcher
    private void openSetting(String primary, String fallback) {
        try {
            Intent intent = new Intent("android.settings." + primary);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }
            if (!fallback.isEmpty()) {
                Intent fallbackIntent = new Intent("android.settings." + fallback);
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (fallbackIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(fallbackIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
