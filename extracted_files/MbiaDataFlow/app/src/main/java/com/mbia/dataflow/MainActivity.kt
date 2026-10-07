package com.mbia.dataflow

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val p by lazy { DataStore.prefs(this) }
    private lateinit var total: TextView
    private lateinit var period: TextView
    private lateinit var status: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable { override fun run() { refresh(); handler.postDelayed(this, 1000) } }
    private val df = DecimalFormat("0.00")

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); buildUi(); requestNotifications(); refresh() }
    override fun onResume() { super.onResume(); handler.post(tick) }
    override fun onPause() { handler.removeCallbacks(tick); super.onPause() }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 20, 24, 20); setBackgroundColor(0xFFF5F7FA.toInt()) }
        val title = TextView(this).apply { text = "MbiaDataFlow"; textSize = 30f; setTypeface(null, 1); setTextColor(0xFF101828.toInt()) }
        root.addView(title)
        root.addView(TextView(this).apply { text = "Données mobiles • Surveillance et protection"; textSize = 15f; setTextColor(0xFF667085.toInt()) })
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 20, 20, 20); setBackgroundColor(0xFFFFFFFF.toInt()) }
        root.addView(card, LinearLayout.LayoutParams(-1, 0, 1f).apply { setMargins(0, 22, 0, 14) })
        status = TextView(this).apply { textSize = 15f }; card.addView(status)
        total = TextView(this).apply { textSize = 38f; setTypeface(null, 1); gravity = Gravity.CENTER }; card.addView(total, LinearLayout.LayoutParams(-1, 0, 1f))
        period = TextView(this).apply { textSize = 16f; gravity = Gravity.CENTER; setTextColor(0xFF667085.toInt()) }; card.addView(period)

        val controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(controls)
        addSwitch(controls, "Application active", DataStore.APP_ENABLED, true)
        addSwitch(controls, "Surveillance", DataStore.SURVEILLANCE, true)
        addSwitch(controls, "Protection par limite", DataStore.PROTECTION, true)
        button(controls, "Définir la limite", ::limitDialog)
        button(controls, "Choisir la réinitialisation", ::resetModeDialog)
        button(controls, "Réinitialiser maintenant (historique conservé)", ::resetNow)
        button(controls, "Voir l'historique", ::historyDialog)
        setContentView(root)
    }

    private fun addSwitch(parent: LinearLayout, label: String, key: String, default: Boolean) {
        val s = Switch(this).apply { text = label; textSize = 16f; isChecked = p.getBoolean(key, default) }
        s.setOnCheckedChangeListener { _, value -> p.edit().putBoolean(key, value).apply(); updateService(); refresh() }
        parent.addView(s)
    }
    private fun button(parent: LinearLayout, label: String, action: () -> Unit) { parent.addView(Button(this).apply { text = label; setOnClickListener { action() } }) }

    private fun updateService() {
        if (p.getBoolean(DataStore.APP_ENABLED, true) && p.getBoolean(DataStore.SURVEILLANCE, true)) ContextCompat.startForegroundService(this, Intent(this, MonitorService::class.java))
        else stopService(Intent(this, MonitorService::class.java))
    }

    private fun refresh() {
        if (!p.getBoolean(DataStore.APP_ENABLED, true)) { status.text = "● Application désactivée"; total.text = "Surveillance arrêtée"; period.text = "Historique conservé"; stopService(Intent(this, MonitorService::class.java)); return }
        DataStore.sample(this)
        val current = p.getLong(DataStore.PERIOD_TOTAL, 0L)
        val all = p.getLong(DataStore.INSTALL_TOTAL, 0L)
        total.text = format(current)
        period.text = "Période • ${format(current)}\nDepuis l'installation • ${format(all)}"
        status.text = "● Surveillance " + if (p.getBoolean(DataStore.SURVEILLANCE, true)) "active" else "désactivée"
        if (p.getBoolean(DataStore.SURVEILLANCE, true)) updateService()
    }

    private fun limitDialog() {
        val input = EditText(this).apply { hint = "Ex. 2"; inputType = 8194; setText((p.getLong(DataStore.LIMIT, 2L*1024*1024*1024) / 1024.0 / 1024 / 1024 / 1024).toString()) }
        AlertDialog.Builder(this).setTitle("Limite en GB").setView(input).setPositiveButton("Enregistrer") { _, _ ->
            val gb = input.text.toString().toDoubleOrNull()?.coerceAtLeast(0.01) ?: 2.0
            p.edit().putLong(DataStore.LIMIT, (gb*1024*1024*1024).toLong()).putInt(DataStore.WARN_LEVEL, 0).apply(); refresh()
        }.setNegativeButton("Annuler", null).show()
    }

    private fun resetModeDialog() {
        val labels = arrayOf("Aucune réinitialisation", "Manuelle", "Quotidienne", "Hebdomadaire", "Mensuelle", "Date personnalisée")
        val values = arrayOf("none","manual","daily","weekly","monthly","custom")
        val current = values.indexOf(p.getString(DataStore.PERIOD_MODE, "monthly")).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle("Réinitialisation de la période").setSingleChoiceItems(labels, current) { d, which ->
            p.edit().putString(DataStore.PERIOD_MODE, values[which]).apply(); d.dismiss(); if (values[which] == "custom") customDateDialog(); else refresh()
        }.show()
    }
    private fun customDateDialog() {
        val now = Calendar.getInstance(); val date = DatePickerDialog(this, { _, y, m, day ->
            val c = Calendar.getInstance().apply { set(y,m,day,0,0,0); set(Calendar.MILLISECOND,0) }; DataStore.setCustomDate(this, c.timeInMillis); refresh()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)); date.show()
    }
    private fun resetNow() { DataStore.manualReset(this); Toast.makeText(this, "Période réinitialisée. Historique conservé.", Toast.LENGTH_SHORT).show(); refresh() }
    private fun historyDialog() {
        val h = p.getString(DataStore.HISTORY, "") ?: ""
        val lines = h.lines().filter { it.isNotBlank() }.takeLast(20).reversed()
        val text = if (lines.isEmpty()) "Aucun historique de réinitialisation." else lines.joinToString("\n\n") { it }
        AlertDialog.Builder(this).setTitle("Historique").setMessage(text).setPositiveButton("Fermer", null).show()
    }
    private fun requestNotifications() { if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 50) }
    private fun format(bytes: Long): String { val gb=1024.0*1024*1024; val mb=1024.0*1024; return when { bytes>=gb -> df.format(bytes/gb)+" GB"; bytes>=mb -> df.format(bytes/mb)+" MB"; else -> df.format(bytes/1024.0)+" KB" } }
}
