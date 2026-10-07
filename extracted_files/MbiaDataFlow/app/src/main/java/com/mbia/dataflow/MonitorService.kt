package com.mbia.dataflow

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo

class MonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val p by lazy { DataStore.prefs(this) }
    private val tick = object : Runnable { override fun run() { if (p.getBoolean(DataStore.APP_ENABLED,true) && p.getBoolean(DataStore.SURVEILLANCE,true)) { DataStore.sample(this@MonitorService); checkLimit() }; handler.postDelayed(this, 5000) } }
    override fun onCreate() { super.onCreate(); createChannel(); val n=notification(); ServiceCompat.startForeground(this, 7, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC); handler.post(tick) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onDestroy() { handler.removeCallbacks(tick); super.onDestroy() }
    override fun onBind(intent: Intent?) = null
    private fun checkLimit() {
        if (!p.getBoolean(DataStore.PROTECTION,true)) return
        val limit=p.getLong(DataStore.LIMIT,2L*1024*1024*1024); if(limit<=0)return
        val used=p.getLong(DataStore.PERIOD_TOTAL,0); val level=when { used>=limit->3; used*100>=limit*90->2; used*100>=limit*80->1; else->0 }; val old=p.getInt(DataStore.WARN_LEVEL,0)
        if(level>old){p.edit().putInt(DataStore.WARN_LEVEL,level).apply(); val nm=getSystemService(NotificationManager::class.java); val pct=(used*100/limit).toInt(); nm.notify(8,NotificationCompat.Builder(this,"dataflow").setSmallIcon(com.mbia.dataflow.R.drawable.ic_dataflow).setContentTitle("MbiaDataFlow • Alerte").setContentText(if(level==3)"Limite atteinte : $pct %" else "Utilisation : $pct % de la limite").setAutoCancel(true).build())}
    }
    private fun notification(): Notification = NotificationCompat.Builder(this,"dataflow").setSmallIcon(com.mbia.dataflow.R.drawable.ic_dataflow).setContentTitle("MbiaDataFlow").setContentText("Surveillance des données mobiles active").setOngoing(true).build()
    private fun createChannel(){getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("dataflow","MbiaDataFlow",NotificationManager.IMPORTANCE_LOW))}
}
