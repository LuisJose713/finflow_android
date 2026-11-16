package com.softdevcode.finflow_android.domain.model

enum class AppTheme(val label: String) {
    LIGHT("Claro"), DARK("Oscuro"), OLED("Oscuro OLED");

    val isDark: Boolean get() = this != LIGHT

    companion object {
        /** Preserve the existing boolean preference on upgrades. */
        fun resolve(saved: String?, legacyDarkMode: Boolean?): AppTheme =
            entries.firstOrNull { it.name == saved }
                ?: if (legacyDarkMode == false) LIGHT else DARK
    }
}
