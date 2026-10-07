package com.mbia.dataflow

import android.content.*
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val p=DataStore.prefs(context)
        if(p.getBoolean(DataStore.APP_ENABLED,true) && p.getBoolean(DataStore.SURVEILLANCE,true)) {
            ContextCompat.startForegroundService(context, Intent(context, MonitorService::class.java))
        }
    }
}
