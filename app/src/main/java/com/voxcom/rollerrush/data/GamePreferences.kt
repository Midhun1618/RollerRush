package com.voxcom.rollerrush.data

import android.content.Context
import android.content.SharedPreferences


class GamePreferences(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("roller_rush_prefs", Context.MODE_PRIVATE)

    fun getInt(key: String, def: Int): Int = sp.getInt(key, def)
    fun putInt(key: String, value: Int) = sp.edit().putInt(key, value).apply()

    fun getString(key: String, def: String): String = sp.getString(key, def) ?: def
    fun putString(key: String, value: String) = sp.edit().putString(key, value).apply()

    fun getBoolean(key: String, def: Boolean): Boolean = sp.getBoolean(key, def)
    fun putBoolean(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()

    fun getStringSet(key: String): Set<String> = sp.getStringSet(key, null)?.toSet() ?: emptySet()
    fun putStringSet(key: String, value: Set<String>) = sp.edit().putStringSet(key, value).apply()

    fun clearAll() = sp.edit().clear().apply()

    // ---- Settings -------------------------------------------------------
    var soundEnabled: Boolean
        get() = getBoolean("settings_sound", true)
        set(v) = putBoolean("settings_sound", v)

    /** Debug option: draws hitboxes on the game screen. */
    var debugHitboxes: Boolean
        get() = getBoolean("settings_debug_hitboxes", false)
        set(v) = putBoolean("settings_debug_hitboxes", v)


    var tutorialCompleted: Boolean
        get() = getBoolean("tutorial_completed", false)
        set(value) = putBoolean("tutorial_completed", value)



}
