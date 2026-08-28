package com.hub.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isTorchOn = false;
    
    private SharedPreferences prefs;
    private boolean isDark;

    static class Setting {
        String title, desc, icon, primary, fallback, colorHex;
        Setting(String t, String d, String i, String p, String f, String c) {
            this.title = t; 
            this.desc = d; 
            this.icon = i; 
            this.primary = p; 
            this.fallback = f; 
            this.colorHex = c;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme", 0);
        boolean isSysDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        isDark = (themeMode == 2) || (themeMode == 0 && isSysDark);

        setContentView(R.layout.activity_main);
        setupThemeColors();
        setupHardware();
        buildSettingsList();
        setupSearch();
    }

    private void setupThemeColors() {
        String bgColor = isDark ? "#121212" : "#F6F8FA";
        String cardColor = isDark ? "#1E1E1E" : "#FFFFFF";
        String textColor = isDark ? "#E3E3E3" : "#1F1F1F";
        String subTextColor = isDark ? "#9E9E9E" : "#636363";
        String searchColor = isDark ? "#2D2F33" : "#E9EEF6"; 
        String activeBtnColor = isDark ? "#A8C7FA" : "#0B57D0";
        String activeBtnText = isDark ? "#041E49" : "#FFFFFF";

        findViewById(R.id.root_view).setBackgroundColor(Color.parseColor(bgColor));

        EditText searchBar = findViewById(R.id.search_bar);
        searchBar.setBackground(createRoundedBg(searchColor, 100));
        searchBar.setTextColor(Color.parseColor(textColor));
        searchBar.setHintTextColor(Color.parseColor(subTextColor));

        String quickTileBg = isDark ? "#282A2D" : "#FFFFFF";
        styleQuickTile(findViewById(R.id.tile_torch), quickTileBg, textColor, subTextColor);
        styleQuickTile(findViewById(R.id.tile_rotate), quickTileBg, textColor, subTextColor);
        
        findViewById(R.id.list_container).setBackground(createRoundedBg(cardColor, 48));

        int themeMode = prefs.getInt("theme", 0);
        styleThemeBtn(findViewById(R.id.btn_theme_sys), themeMode == 0, cardColor, textColor, activeBtnColor, activeBtnText);
        styleThemeBtn(findViewById(R.id.btn_theme_light), themeMode == 1, cardColor, textColor, activeBtnColor, activeBtnText);
        styleThemeBtn(findViewById(R.id.btn_theme_dark), themeMode == 2, cardColor, textColor, activeBtnColor, activeBtnText);
    }

    private GradientDrawable createRoundedBg(String colorHex, int radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(radiusDp * getResources().getDisplayMetrics().density);
        shape.setColor(Color.parseColor(colorHex));
        return shape;
    }

    private void styleQuickTile(LinearLayout tile, String bg, String txt, String sub) {
        tile.setBackground(createRoundedBg(bg, 48));
        ((TextView) tile.getChildAt(1)).setTextColor(Color.parseColor(txt));
        ((TextView) tile.getChildAt(2)).setTextColor(Color.parseColor(sub));
    }

    private void styleThemeBtn(TextView btn, boolean isActive, String cardColor, String textColor, String activeBg, String activeTxt) {
        btn.setBackground(createRoundedBg(isActive ? activeBg : cardColor, 100));
        btn.setTextColor(Color.parseColor(isActive ? activeTxt : textColor));
        btn.setOnClickListener(v -> {
            int newTheme = btn.getId() == R.id.btn_theme_sys ? 0 : (btn.getId() == R.id.btn_theme_light ? 1 : 2);
            prefs.edit().putInt("theme", newTheme).apply();
            recreate(); 
        });
    }

    private void setupHardware() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager.getCameraIdList().length > 0) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }

        findViewById(R.id.tile_torch).setOnClickListener(v -> {
            try {
                if (cameraId != null) {
                    isTorchOn = !isTorchOn;
                    cameraManager.setTorchMode(cameraId, isTorchOn);
                    TextView status = findViewById(R.id.status_torch);
                    status.setText(isTorchOn ? "On" : "Off");
                    
                    String activeBg = isDark ? "#004A77" : "#D3E3FD"; 
                    String inactiveBg = isDark ? "#282A2D" : "#FFFFFF";
                    v.setBackground(createRoundedBg(isTorchOn ? activeBg : inactiveBg, 48));
                }
            } catch (Exception e) {
                Toast.makeText(this, "Flashlight unavailable", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.tile_rotate).setOnClickListener(v -> 
            openSetting("AUTO_ROTATE_SETTINGS", "DISPLAY_SETTINGS")
        );
    }

    private void buildSettingsList() {
        String netColor = isDark ? "#003355" : "#D3E3FD";   
        String devColor = isDark ? "#0D381E" : "#C4EED0";   
        String notifColor = isDark ? "#5C162E" : "#F8D8E5"; 
        String sysColor = isDark ? "#593000" : "#FEEFC3";   
        String dangerColor = isDark ? "#8C1D18" : "#F9DEDC";

        Setting[] allSettings = {
            new Setting("Wi-Fi & Networks", "Manage Wi-Fi connections", "🌐", "WIFI_SETTINGS", "", netColor),
            new Setting("Mobile Hotspot", "Share network via tethering", "🛜", "TETHER_SETTINGS", "WIRELESS_SETTINGS", netColor),
            new Setting("Mobile Data", "Data usage and toggle", "📶", "DATA_USAGE_SETTINGS", "NETWORK_OPERATOR_SETTINGS", netColor),
            new Setting("Connected devices", "Bluetooth, pairing", "🔵", "BLUETOOTH_SETTINGS", "", devColor),
            new Setting("Notifications", "Notification history, alerts", "🔔", "ALL_APPS_NOTIFICATION_SETTINGS", "NOTIFICATION_SETTINGS", notifColor),
            new Setting("Sound & vibration", "Volume and haptics", "🔊", "SOUND_SETTINGS", "", notifColor),
            new Setting("Display", "Brightness, dark theme", "☀️", "DISPLAY_SETTINGS", "", sysColor),
            new Setting("Battery", "Power saver and usage", "🔋", "BATTERY_SAVER_SETTINGS", "SETTINGS", devColor),
            new Setting("Security & Privacy", "Biometrics and screen lock", "🔒", "SECURITY_SETTINGS", "", sysColor),
            new Setting("System Updates", "Check for OS patches", "🔄", "SYSTEM_UPDATE_SETTINGS", "DEVICE_INFO_SETTINGS", netColor),
            new Setting("Factory Reset", "Erase all device data", "⚠️", "BACKUP_AND_RESET_SETTINGS", "SETTINGS", dangerColor)
        };

        LinearLayout listContainer = findViewById(R.id.list_container);
        String textColor = isDark ? "#E3E3E3" : "#1F1F1F";
        String subTextColor = isDark ? "#9E9E9E" : "#636363";

        for (Setting s : allSettings) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(48, 40, 48, 40);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setTag((s.title + " " + s.desc).toLowerCase());

            TextView icon = new TextView(this);
            icon.setText(s.icon);
            icon.setTextSize(20);
            icon.setGravity(Gravity.CENTER);
            
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(Color.parseColor(s.colorHex));
            icon.setBackground(circle);
            
            int size = (int) (44 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(size, size);
            iconParams.setMargins(0, 0, 48, 0);
            icon.setLayoutParams(iconParams);
            row.addView(icon);

            LinearLayout textBlock = new LinearLayout(this);
            textBlock.setOrientation(LinearLayout.VERTICAL);
            textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            
            TextView title = new TextView(this);
            title.setText(s.title);
            title.setTextSize(17);
            title.setTextColor(Color.parseColor(textColor));
            title.setTypeface(null, Typeface.BOLD);
            textBlock.addView(title);

            TextView desc = new TextView(this);
            desc.setText(s.desc);
            desc.setTextSize(13);
            desc.setTextColor(Color.parseColor(subTextColor));
            textBlock.addView(desc);
            
            row.addView(textBlock);
            row.setOnClickListener(v -> openSetting(s.primary, s.fallback));
            listContainer.addView(row);
        }
    }

    private void setupSearch() {
        EditText searchBar = findViewById(R.id.search_bar);
        LinearLayout quickPanel = findViewById(R.id.quick_panel);
        LinearLayout listContainer = findViewById(R.id.list_container);
        
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
