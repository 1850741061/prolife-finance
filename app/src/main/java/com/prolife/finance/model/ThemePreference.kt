package com.prolife.finance.model

enum class AppThemeStyle(val label: String) {
    POP_ART("波普"),
    INK_WASH("水墨"),
    SWISS("瑞士"),
    VAPORWAVE("蒸汽波")
}

data class ThemePreference(
    val style: AppThemeStyle = AppThemeStyle.POP_ART,
    val darkMode: Boolean = false
)
