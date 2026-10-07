package com.mbia.dataflow

import android.content.Context
import android.net.TrafficStats
import java.text.SimpleDateFormat
import java.util.*

object DataStore {
    const val PREFS = "mbia_dataflow_store"
    const val APP_ENABLED = "app_enabled"
    const val SURVEILLANCE = "surveillance"
    const val PROTECTION = "protection"
    const val LIMIT = "limit_bytes"
    const val PERIOD_MODE = "period_mode"
    const val CUSTOM_DATE = "custom_date"
    const val INSTALL_TOTAL = "install_total"
    const val PERIOD_TOTAL = "period_total"
    const val BASE_RX = "base_rx"
    const val BASE_TX = "base_tx"
    const val LAST_SUM = "last_sum"
    const val PERIOD_START = "period_start"
    const val WARN_LEVEL = "warn_level"
    const val HISTORY = "history"

    fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun sample(ctx: Context): Long {
        val p = prefs(ctx)
        val rx = TrafficStats.getMobileRxBytes()
        val tx = TrafficStats.getMobileTxBytes()
        if (rx < 0L || tx < 0L) return p.getLong(PERIOD_TOTAL, 0L)
        var br = p.getLong(BASE_RX, -1L)
        var bt = p.getLong(BASE_TX, -1L)
        var last = p.getLong(LAST_SUM, 0L)
        if (br < 0L || rx < br) br = rx
        if (bt < 0L || tx < bt) bt = tx
        val current = (rx - br).coerceAtLeast(0L) + (tx - bt).coerceAtLeast(0L)
        if (current < last) last = current
        val delta = (current - last).coerceAtLeast(0L)
        var install = p.getLong(INSTALL_TOTAL, 0L) + delta
        var period = p.getLong(PERIOD_TOTAL, 0L) + delta
        var start = p.getLong(PERIOD_START, System.currentTimeMillis())
        if (shouldReset(p, start)) {
            appendHistory(p, start, period, "auto-reset")
            period = 0L
            start = System.currentTimeMillis()
            p.edit().putInt(WARN_LEVEL, 0).apply()
        }
        p.edit().putLong(BASE_RX, br).putLong(BASE_TX, bt).putLong(LAST_SUM, current)
            .putLong(INSTALL_TOTAL, install).putLong(PERIOD_TOTAL, period).putLong(PERIOD_START, start).apply()
        return period
    }

    fun manualReset(ctx: Context) {
        val p = prefs(ctx)
        sample(ctx)
        val start = p.getLong(PERIOD_START, System.currentTimeMillis())
        val period = p.getLong(PERIOD_TOTAL, 0L)
        appendHistory(p, start, period, "manual-reset")
        val rx = TrafficStats.getMobileRxBytes().coerceAtLeast(0L)
        val tx = TrafficStats.getMobileTxBytes().coerceAtLeast(0L)
        val br = p.getLong(BASE_RX, rx)
        val bt = p.getLong(BASE_TX, tx)
        val current = (rx - br).coerceAtLeast(0L) + (tx - bt).coerceAtLeast(0L)
        p.edit().putLong(PERIOD_TOTAL, 0L).putLong(LAST_SUM, current).putLong(PERIOD_START, System.currentTimeMillis()).putInt(WARN_LEVEL, 0).apply()
    }

    fun setCustomDate(ctx: Context, millis: Long) { prefs(ctx).edit().putLong(CUSTOM_DATE, millis).putString(PERIOD_MODE, "custom").apply() }

    private fun shouldReset(p: android.content.SharedPreferences, start: Long): Boolean {
        return when (p.getString(PERIOD_MODE, "monthly") ?: "monthly") {
            "none" -> false
            "manual" -> false
            "daily" -> dayKey(start) != dayKey(System.currentTimeMillis())
            "weekly" -> weekKey(start) != weekKey(System.currentTimeMillis())
            "monthly" -> monthKey(start) != monthKey(System.currentTimeMillis())
            "custom" -> p.getLong(CUSTOM_DATE, Long.MAX_VALUE) <= System.currentTimeMillis()
            else -> false
        }
    }

    private fun dayKey(t: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(t))
    private fun weekKey(t: Long) = SimpleDateFormat("yyyy-ww", Locale.US).format(Date(t))
    private fun monthKey(t: Long) = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(t))

    private fun appendHistory(p: android.content.SharedPreferences, start: Long, bytes: Long, reason: String) {
        val old = p.getString(HISTORY, "") ?: ""
        val line = "${start}|${System.currentTimeMillis()}|$bytes|$reason"
        val lines = (old.lines().filter { it.isNotBlank() } + line).takeLast(500)
        p.edit().putString(HISTORY, lines.joinToString("\n")).apply()
    }
}
