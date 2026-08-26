package com.example.selectivesettings

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AppMonitorService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
