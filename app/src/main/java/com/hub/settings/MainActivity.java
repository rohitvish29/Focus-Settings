package com.hub.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isTorchOn = false;
    
    private SharedPreferences prefs;
    private boolean isDark;

    static class Setting {
        String title, desc, icon, colorHex;
        String type; // "INTENT", "BRIGHTNESS", "TIMEOUT", "ADAPTIVE", "DARK_MODE"
        String[] intentActions;
        
        Setting(String t, String d, String i, String c, String type, String... actions) {
            this.title = t; 
            this.desc = d; 
            this.icon = i; 
            this.colorHex = c;
            this.type = type;
            this.intentActions = actions; 
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
        } catch (Exception e) {}

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
            openSettingIntent(new String[]{"AUTO_ROTATE_SETTINGS"})
        );
    }

    private void buildSettingsList() {
        String netColor = isDark ? "#003355" : "#D3E3FD";   
        String devColor = isDark ? "#0D381E" : "#C4EED0";   
        String notifColor = isDark ? "#5C162E" : "#F8D8E5"; 
        String sysColor = isDark ? "#593000" : "#FEEFC3";   

        Setting[] allSettings = {
            new Setting("Wi-Fi & Networks", "Manage Wi-Fi connections", "🌐", netColor, "INTENT", "WIFI_SETTINGS"),
            new Setting("Mobile Hotspot", "Share network via tethering", "🛜", netColor, "INTENT", "TETHER_SETTINGS"),
            
            // --- NEW: Network Intents added as separate list items ---
            new Setting("Data Usage", "View data activity and limits", "📊", netColor, "INTENT", "DATA_USAGE_SETTINGS"),
            new Setting("Data Roaming", "Manage roaming networks", "🌍", netColor, "INTENT", "DATA_ROAMING_SETTINGS"),
            new Setting("Network Operator", "Select network provider", "📡", netColor, "INTENT", "NETWORK_OPERATOR_SETTINGS"),
            new Setting("Wireless Settings", "Advanced wireless options", "📶", netColor, "INTENT", "WIRELESS_SETTINGS"),
            // ---------------------------------------------------------

            new Setting("Connected devices", "Bluetooth, pairing", "🔵", devColor, "INTENT", "BLUETOOTH_SETTINGS"),
            
            // --- IN-APP DISPLAY CONTROLS ---
            new Setting("Dark / Light Mode", "Change system theme", "🌗", sysColor, "DARK_MODE"),
            new Setting("Screen Brightness", "Manual brightness slider", "☀️", sysColor, "BRIGHTNESS"),
            new Setting("Adaptive Brightness", "Turn auto brightness ON/OFF", "🌤️", sysColor, "ADAPTIVE"),
            new Setting("Screen Timeout", "Change auto-lock time", "⏱️", sysColor, "TIMEOUT"),
            
            // --- SAFE DISPLAY INTENTS ---
            new Setting("Eye Comfort Shield", "Blue light filter", "👁️", sysColor, "INTENT", "NIGHT_DISPLAY_SETTINGS"),
            new Setting("Font Size & Style", "Adjust text appearance", "🔤", sysColor, "INTENT", "TEXT_READING_SETTINGS"),
            
            new Setting("Notifications", "Notification history, alerts", "🔔", notifColor, "INTENT", "ALL_APPS_NOTIFICATION_SETTINGS", "NOTIFICATION_SETTINGS"),
            new Setting("Sound & vibration", "Volume and haptics", "🔊", notifColor, "INTENT", "SOUND_SETTINGS"),
            new Setting("Battery", "Power saver and usage", "🔋", devColor, "INTENT", "BATTERY_SAVER_SETTINGS", "SETTINGS"),
            new Setting("Security & Privacy", "Biometrics and screen lock", "🔒", sysColor, "INTENT", "SECURITY_SETTINGS"),
            new Setting("System Updates", "Check for OS patches", "🔄", netColor, "INTENT", "SYSTEM_UPDATE_SETTINGS", "DEVICE_INFO_SETTINGS")
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
            row.setOnClickListener(v -> handleSettingClick(s));
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

    private void handleSettingClick(Setting s) {
        if ("BRIGHTNESS".equals(s.type)) {
            if (hasWritePermission()) showBrightnessDialog();
        } else if ("TIMEOUT".equals(s.type)) {
            if (hasWritePermission()) showTimeoutDialog();
        } else if ("ADAPTIVE".equals(s.type)) {
            if (hasWritePermission()) toggleAdaptiveBrightness();
        } else if ("DARK_MODE".equals(s.type)) {
            toggleSystemDarkMode();
        } else {
            openSettingIntent(s.intentActions);
        }
    }

    private void openSettingIntent(String[] intentActions) {
        for (String action : intentActions) {
            try {
                Intent intent = new Intent("android.settings." + action);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    return; 
                }
            } catch (Exception e) {}
        }
        Toast.makeText(this, "Direct setting not supported on this device", Toast.LENGTH_SHORT).show();
    }

    private boolean hasWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Toast.makeText(this, "Please allow permission to modify settings", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return false;
            }
        }
        return true;
    }

    private void toggleSystemDarkMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    UiModeManager uiManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
                    if (uiManager != null) {
                        int currentMode = uiManager.getNightMode();
                        int newMode = (currentMode == UiModeManager.MODE_NIGHT_YES) ? UiModeManager.MODE_NIGHT_NO : UiModeManager.MODE_NIGHT_YES;
                        uiManager.setNightMode(newMode);
                        Toast.makeText(this, "System Theme Changed", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to change theme", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "MDM Permission Required: WRITE_SECURE_SETTINGS", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showBrightnessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Screen Brightness");

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(60, 50, 60, 50);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(255);
        try {
            int currentBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            seekBar.setProgress(currentBrightness);
        } catch (Settings.SettingNotFoundException e) {}

        seekBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 10) progress = 10; 
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, progress);
                
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.screenBrightness = progress / 255.0f;
                getWindow().setAttributes(layoutParams);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        layout.addView(seekBar);
        builder.setView(layout);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void toggleAdaptiveBrightness() {
        try {
            int currentMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
            int newMode = (currentMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) 
                    ? Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL 
                    : Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, newMode);
            
            String status = (newMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) ? "ON" : "OFF";
            Toast.makeText(this, "Adaptive Brightness turned " + status, Toast.LENGTH_SHORT).show();
        } catch (Settings.SettingNotFoundException e) {
            Toast.makeText(this, "Feature not supported", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTimeoutDialog() {
        final String[] timeNames = {"15 Seconds", "30 Seconds", "1 Minute", "2 Minutes", "5 Minutes", "10 Minutes"};
        final int[] timeValues = {15000, 30000, 60000, 120000, 300000, 600000};

        int currentTimeout = 30000;
        try {
            currentTimeout = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Settings.SettingNotFoundException e) {}

        int selectedIndex = 1;
        for (int i = 0; i < timeValues.length; i++) {
            if (currentTimeout == timeValues[i]) {
                selectedIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Screen Timeout");
        builder.setSingleChoiceItems(timeNames, selectedIndex, (dialog, which) -> {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, timeValues[which]);
            Toast.makeText(this, "Timeout set to " + timeNames[which], Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.show();
    }
}
