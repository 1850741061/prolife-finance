package com.prolife.finance.data

import com.prolife.finance.model.FinanceSnapshot
import com.prolife.finance.model.ThemePreference

/**
 * 纯本地数据仓库 — 所有数据只保存在设备本地缓存中。
 * 无需登录、无需网络。
 */
class FinanceRepository(
    private val sessionStore: SessionStore,
    private val financeCache: FinanceCache
) {
    fun loadThemePreference(): ThemePreference = sessionStore.loadThemePreference()

    fun saveThemePreference(preference: ThemePreference) {
        sessionStore.saveThemePreference(preference)
    }

    fun loadCachedSnapshot(): FinanceSnapshot = financeCache.readSnapshot()

    fun cacheSnapshot(snapshot: FinanceSnapshot) {
        financeCache.writeSnapshot(snapshot)
    }

    fun clearAllData() {
        financeCache.clear()
    }

    fun saveAutoBookkeepEnabled(enabled: Boolean) = sessionStore.saveAutoBookkeepEnabled(enabled)
    fun loadAutoBookkeepEnabled(): Boolean = sessionStore.loadAutoBookkeepEnabled()

    fun saveAASplitEnabled(enabled: Boolean) = sessionStore.saveAASplitEnabled(enabled)
    fun loadAASplitEnabled(): Boolean = sessionStore.loadAASplitEnabled()
    fun saveAASplitThreshold(amount: Float) = sessionStore.saveAASplitThreshold(amount)
    fun loadAASplitThreshold(): Float = sessionStore.loadAASplitThreshold()
}
