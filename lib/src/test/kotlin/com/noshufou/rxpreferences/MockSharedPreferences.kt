package com.noshufou.rxpreferences

import android.content.SharedPreferences

class MockSharedPreferences : SharedPreferences {
    private val preferences = mutableMapOf<String, Any>()
    
    override fun contains(key: String): Boolean = preferences.contains(key)

    override fun getAll(): Map<String, Any> = preferences.toMap()

    override fun getBoolean(key: String, value: Boolean): Boolean {
        if (!preferences.contains(key)) return value
        return preferences[key] as? Boolean ?: throw ClassCastException("$key is not a Boolean")
    }

    override fun getFloat(key: String, value: Float): Float {
        if (!preferences.contains(key)) return value
        return preferences[key] as? Float ?: throw ClassCastException("$key is not a Float")
    }

    override fun getInt(key: String, value: Int): Int {
        if (!preferences.contains(key)) return value
        return preferences[key] as? Int ?: throw ClassCastException("$key is not an Int")
    }

    override fun getLong(key: String, value: Long): Long {
        if (!preferences.containsKey(key)) return value
        return preferences[key] as? Long ?: throw ClassCastException("$key is not a Long")
    }

    override fun getString(key: String, value: String): String {
        if (!preferences.containsKey(key)) return value
        return preferences[key] as? String ?: throw ClassCastException("$key is not a String")
    }

    override fun getStringSet(key: String, value: Set<String>): Set<String> {
        if (!preferences.containsKey(key)) return value
        @Suppress("UNCHECKED_CAST") // Inserts into preferences are checked down in the editor
        return preferences[key] as? Set<String> ?: throw ClassCastException("$key is not a Set<String>")
    }

    override fun edit(): SharedPreferences.Editor = Editor()

    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners += listener
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners -= listener
    }

    private inner class Editor : SharedPreferences.Editor {
        private var shouldClear: Boolean = false
        private val pendingChanges = mutableMapOf<String, Any>()
        private val pendingRemovals = mutableSetOf<String>()

        override fun clear(): SharedPreferences.Editor {
            shouldClear = true
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            pendingRemovals += key
            return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            pendingChanges[key] = value
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            pendingChanges[key] = value
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            pendingChanges[key] = value
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            pendingChanges[key] = value
            return this
        }

        override fun putString(key: String, value: String): SharedPreferences.Editor {
            pendingChanges[key] = value
            return this
        }

        override fun putStringSet(key: String, value: Set<String>): SharedPreferences.Editor {
            pendingChanges[key] = value
            return this
        }

        override fun commit(): Boolean {
            if (shouldClear) preferences.clear()
            pendingRemovals.forEach { preferences.remove(it) }

            preferences.putAll(pendingChanges)

            pendingChanges.forEach { key, _ ->
                listeners.forEach { it.onSharedPreferenceChanged(this@MockSharedPreferences, key) }
            }

            return true
        }

        override fun apply() {
            commit()
        }
    }
}