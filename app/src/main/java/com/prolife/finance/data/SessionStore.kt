package com.prolife.finance.data

import android.content.Context
import com.prolife.finance.model.AppThemeStyle
import com.prolife.finance.model.ThemePreference

/**
 * 本地设置存储 — 仅保留主题、自动记账开关等偏好设置。
 * 已移除 Supabase 登录会话相关字段。
 */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("ledger_bloom_prefs", Context.MODE_PRIVATE)

    fun saveThemePreference(preference: ThemePreference) {
        prefs.edit()
            .putString(KEY_THEME_STYLE, preference.style.name)
            .putBoolean(KEY_THEME_DARK, preference.darkMode)
            .apply()
    }

    fun loadThemePreference(): ThemePreference {
        val styleName = prefs.getString(KEY_THEME_STYLE, AppThemeStyle.POP_ART.name)
        val style = runCatching { AppThemeStyle.valueOf(styleName.orEmpty()) }
            .getOrDefault(AppThemeStyle.POP_ART)
        return ThemePreference(
            style = style,
            darkMode = prefs.getBoolean(KEY_THEME_DARK, false)
        )
    }

    fun saveAutoBookkeepEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BOOKKEEP, enabled).apply()
    }

    fun loadAutoBookkeepEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_BOOKKEEP, false)
    }

    fun saveAASplitEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AA_SPLIT_ENABLED, enabled).apply()
    }

    fun loadAASplitEnabled(): Boolean {
        return prefs.getBoolean(KEY_AA_SPLIT_ENABLED, false)
    }

    fun saveAASplitThreshold(amount: Float) {
        prefs.edit().putFloat(KEY_AA_SPLIT_THRESHOLD, amount).apply()
    }

    fun loadAASplitThreshold(): Float {
        return prefs.getFloat(KEY_AA_SPLIT_THRESHOLD, 100f)
    }

    companion object {
        private const val KEY_THEME_STYLE = "theme_style"
        private const val KEY_THEME_DARK = "theme_dark"
        private const val KEY_AUTO_BOOKKEEP = "auto_bookkeep_enabled"
        private const val KEY_AA_SPLIT_ENABLED = "aa_split_enabled"
        private const val KEY_AA_SPLIT_THRESHOLD = "aa_split_threshold"
    }
}
