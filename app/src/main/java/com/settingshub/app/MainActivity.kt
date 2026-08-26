package com.settingshub.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupButton(R.id.btnWifi, Settings.ACTION_WIFI_SETTINGS)
        setupButton(R.id.btnData, Settings.ACTION_DATA_USAGE_SETTINGS)
        setupButton(R.id.btnWifiCalling, Settings.ACTION_WIRELESS_SETTINGS)
        setupButton(R.id.btnBluetooth, Settings.ACTION_BLUETOOTH_SETTINGS)
        setupButton(R.id.btnSimManager, Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
        
        findViewById<Button>(R.id.btnHotspot).setOnClickListener {
            try {
                val intent = Intent()
                intent.setClassName("com.android.settings", "com.android.settings.TetherSettings")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Hotspot unavailable on this brand", Toast.LENGTH_SHORT).show()
            }
        }

        setupButton(R.id.btnSound, Settings.ACTION_SOUND_SETTINGS)
        setupButton(R.id.btnNotifications, "android.settings.NOTIFICATION_SETTINGS") 
        setupButton(R.id.btnDisplay, Settings.ACTION_DISPLAY_SETTINGS)
        setupButton(R.id.btnBattery, Intent.ACTION_POWER_USAGE_SUMMARY)
        setupButton(R.id.btnSecurity, Settings.ACTION_SECURITY_SETTINGS)
        setupButton(R.id.btnAccount, Settings.ACTION_SYNC_SETTINGS)
        setupButton(R.id.btnKeyboard, Settings.ACTION_INPUT_METHOD_SETTINGS)
        setupButton(R.id.btnSoftwareUpdate, "android.settings.SYSTEM_UPDATE_SETTINGS")
    }

    private fun setupButton(buttonId: Int, action: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            try {
                startActivity(Intent(action))
            } catch (e: Exception) {
                Toast.makeText(this, "Setting unavailable on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
