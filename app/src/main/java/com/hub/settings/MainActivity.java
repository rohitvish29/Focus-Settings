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
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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

    // Theme color variables
    private String bgColor, cardColor, textColor, subTextColor, searchColor, activeBtnColor, activeBtnText, activeTileBg, inactiveTileBg;

    static class Setting {
        String title, desc, icon, primary, fallback, colorHex;
        Setting(String t, String d, String i, String p, String f, String c) {
            this.title = t; this.desc = d; this.icon = i; this.primary = p; this.fallback = f; this.colorHex = c;
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
        buildQuickTiles();
        buildSettingsList();
        setupSearch();
    }

    private void setupThemeColors() {
        bgColor = isDark ? "#121212" : "#F6F8FA";
        cardColor = isDark ? "#1E1E1E" : "#FFFFFF";
        textColor = isDark ? "#E3E3E3" : "#1F1F1F";
        subTextColor = isDark ? "#9E9E9E" : "#636363";
        searchColor = isDark ? "#2D2F33" : "#E9EEF6"; 
        activeBtnColor = isDark ? "#A8C7FA" : "#0B57D0";
        activeBtnText = isDark ? "#041E49" : "#FFFFFF";
        
        // Colors matching your uploaded screenshot (Cream/Yellowish active state)
        activeTileBg = isDark ? "#32302A" : "#F8EBC6"; 
        inactiveTileBg = isDark ? "#282A2D" : "#FFFFFF";

        findViewById(R.id.root_view).setBackgroundColor(Color.parseColor(bgColor));

        EditText searchBar = findViewById(R.id.search_bar);
        searchBar.setBackground(createRoundedBg(searchColor, 100));
        searchBar.setTextColor(Color.parseColor(textColor));
        searchBar.setHintTextColor(Color.parseColor(subTextColor));
        
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

    private void styleThemeBtn(TextView btn, boolean isActive, String card, String txt, String actBg, String actTxt) {
        btn.setBackground(createRoundedBg(isActive ? actBg : card, 100));
        btn.setTextColor(Color.parseColor(isActive ? actTxt : txt));
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
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Dynamic 2x3 Grid Builder for Quick Access
    private void buildQuickTiles() {
        LinearLayout container = findViewById(R.id.quick_panel_container);
        container.removeAllViews();

        // Row 1
        LinearLayout row1 = createTileRow();
        row1.addView(createTile("Wi-Fi", "Panel", "🌐", v -> openSetting("android.settings.panel.action.INTERNET_CONNECTIVITY", "android.settings.WIFI_SETTINGS")));
        row1.addView(createTile("Bluetooth", "Pop-up", "🔵", v -> openSetting("android.bluetooth.adapter.action.REQUEST_ENABLE", "android.settings.BLUETOOTH_SETTINGS")));
        container.addView(row1);

        // Row 2
        LinearLayout row2 = createTileRow();
        row2.addView(createTile("Mobile Data", "Panel", "📶", v -> openSetting("android.settings.panel.action.INTERNET_CONNECTIVITY", "android.settings.DATA_USAGE_SETTINGS")));
        row2.addView(createTile("Battery Saver", "Settings", "🔋", v -> openSetting("android.settings.BATTERY_SAVER_SETTINGS", "android.settings.SETTINGS")));
        container.addView(row2);

        // Row 3
        LinearLayout row3 = createTileRow();
        View torchTile = createTile("Flashlight", "Off", "🔦", null);
        torchTile.setOnClickListener(v -> toggleTorch(torchTile));
        row3.addView(torchTile);

        View rotateTile = createTile("Auto Rotate", getRotationState(), "🔄", null);
        rotateTile.setOnClickListener(v -> toggleRotation(rotateTile));
        row3.addView(rotateTile);
        
        container.addView(row3);
    }

    private LinearLayout createTileRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 0, 0, 16);
        return row;
    }

    private View createTile(String titleText, String subText, String iconText, View.OnClickListener listener) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 180, 1);
        params.setMargins(8, 0, 8, 0);
        tile.setLayoutParams(params);
        tile.setBackground(createRoundedBg(inactiveTileBg, 32));
        tile.setPadding(32, 24, 32, 24);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        
        if (listener != null) tile.setOnClickListener(listener);

        TextView icon = new TextView(this);
        icon.setText(iconText);
        icon.setTextSize(24);
        icon.setPadding(0, 0, 24, 0);
        tile.addView(icon);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        
        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextSize(14);
        title.setTextColor(Color.parseColor(textColor));
        title.setTypeface(null, Typeface.BOLD);
        
        TextView sub = new TextView(this);
        sub.setTag("status"); // Tag to easily find and update it later
        sub.setText(subText);
        sub.setTextSize(12);
        sub.setTextColor(Color.parseColor(subTextColor));
        
        textLayout.addView(title);
        textLayout.addView(sub);
        tile.addView(textLayout);

        return tile;
    }

    private void toggleTorch(View tile) {
        try {
            if (cameraId != null) {
                isTorchOn = !isTorchOn;
                cameraManager.setTorchMode(cameraId, isTorchOn);
                TextView status = tile.findViewWithTag("status");
                status.setText(isTorchOn ? "On" : "Off");
                tile.setBackground(createRoundedBg(isTorchOn ? activeTileBg : inactiveTileBg, 32));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Flashlight unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private String getRotationState() {
        try {
            int state = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
            return state == 1 ? "Auto" : "Portrait";
        } catch (Exception e) { return "Settings"; }
    }

    private void toggleRotation(View tile) {
        // Must request permission from Android first to toggle settings natively
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "Please grant permission to toggle rotation", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }

        try {
            int currentState = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
            int newState = currentState == 1 ? 0 : 1;
            Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, newState);
            
            TextView status = tile.findViewWithTag("status");
            status.setText(newState == 1 ? "Auto" : "Portrait");
            tile.setBackground(createRoundedBg(newState == 1 ? activeTileBg : inactiveTileBg, 32));
        } catch (Exception e) {
            openSetting("android.settings.AUTO_ROTATE_SETTINGS", "android.settings.DISPLAY_SETTINGS");
        }
    }

    private void buildSettingsList() {
        String netColor = isDark ? "#003355" : "#D3E3FD";   
        String devColor = isDark ? "#0D381E" : "#C4EED0";   
        String notifColor = isDark ? "#5C162E" : "#F8D8E5"; 
        String sysColor = isDark ? "#593000" : "#FEEFC3";   
        String dangerColor = isDark ? "#8C1D18" : "#F9DEDC";

        Setting[] allSettings = {
            new Setting("Wi-Fi & Networks", "Manage Wi-Fi connections", "🌐", "android.settings.WIFI_SETTINGS", "", netColor),
            new Setting("Mobile Hotspot", "Share network via tethering", "🛜", "android.settings.TETHER_SETTINGS", "android.settings.WIRELESS_SETTINGS", netColor),
            new Setting("Connected devices", "Bluetooth, pairing", "🔵", "android.settings.BLUETOOTH_SETTINGS", "", devColor),
            new Setting("Notifications", "Notification history, alerts", "🔔", "android.settings.ALL_APPS_NOTIFICATION_SETTINGS", "android.settings.NOTIFICATION_SETTINGS", notifColor),
            new Setting("Sound & vibration", "Volume and haptics", "🔊", "android.settings.panel.action.VOLUME", "android.settings.SOUND_SETTINGS", notifColor),
            new Setting("Display", "Brightness, dark theme", "☀️", "android.settings.DISPLAY_SETTINGS", "", sysColor),
            new Setting("Battery", "Power saver and usage", "🔋", "android.settings.BATTERY_SAVER_SETTINGS", "android.settings.SETTINGS", devColor),
            new Setting("Security & Privacy", "Biometrics and screen lock", "🔒", "android.settings.SECURITY_SETTINGS", "", sysColor),
            new Setting("System Updates", "Check for OS patches", "🔄", "android.settings.SYSTEM_UPDATE_SETTINGS", "android.settings.DEVICE_INFO_SETTINGS", netColor),
            new Setting("Factory Reset", "Erase all device data", "⚠️", "android.settings.PRIVACY_SETTINGS", "android.settings.SYNC_SETTINGS", dangerColor)
        };

        LinearLayout listContainer = findViewById(R.id.list_container);

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
        LinearLayout quickPanel = findViewById(R.id.quick_panel_container);
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
            Intent intent = new Intent(primary);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }
            if (!fallback.isEmpty()) {
                Intent fallbackIntent = new Intent(fallback);
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (fallbackIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(fallbackIntent);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
