package com.noshufou.rxpreferences

import android.content.Context
import android.content.SharedPreferences
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers

/**
 * Base class for shared preferences
 *
 * @constructor creates an instance for accessing SharedPreferences
 * @param context the context to get the SparedPreferences instance from
 * @param name desired preferences file, defaults to context.packageName
 * @param scheduler desired scheduler to read and write on, defaults to Schedulers.io()
 */
class RxPreferences(context: Context,
                    name: String = context.packageName,
                    private val scheduler: Scheduler = Schedulers.io()) {

    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val changeObservable by lazy { SharedPreferencesChangeObservable(prefs, scheduler).share() }

    /**
     * Observe a SharedPreference
     *
     * @param T the type of the shared preference
     * @param key the key you'd like to observe
     * @param defValue default value to be used if the key doesn't exist
     * @return an Observable of the shared preference
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: String, defValue: T): Observable<T> =
            Observable.fromCallable {
                when (defValue) {
                    is Boolean -> prefs.getBoolean(key, defValue) as T
                    is Float -> prefs.getFloat(key, defValue) as T
                    is Int -> prefs.getInt(key, defValue) as T
                    is Long -> prefs.getLong(key, defValue) as T
                    is String -> prefs.getString(key, defValue) as T
                    is Set<*> -> {
                        if (defValue.any { it !is String }) throw UnsupportedOperationException("Only Set<String> is supported")
                        prefs.getStringSet(key, defValue as Set<String>) as T
                    }
                    else -> throw UnsupportedOperationException("no accessor found for type ${defValue::class.java}")
                }
            }.subscribeOn(scheduler)
                    .concatWith(changeObservable.subscribeOn(scheduler)
                            .filter { it.first == key }
                            .map { it.second }
                            .map { it as T })

    /**
     * Obtain a Consumer for the given key
     *
     * This consumer does not check for the type of SharedPreference it is writing to. If you
     * supply a key that was originally a different type, it will be overwritten with a new key
     * of the given type. This is inline with how SharedPreferences works.
     *
     * @param T type of the Consumer, must be Boolean, Float, Int, Long, String or Set<String>
     * @param key key for the SharedPreference you want to update
     * @return a Consumer<T> that persists each value asynchronously
     */
    operator fun <T : Any> get(key: String): Consumer<T> {
        return Consumer {
            EditorImpl(prefs).apply {
                key to it
                apply()
            }
        }
    }

    /**
     * Edit the attached SharedPreferences asynchronously
     *
     * Similar to SharedPreferences.Editor, no checks are made regarding the type of existing keys.
     * If you assign a key to a different type, it is simply overwritten.
     *
     * @param func lambda with a receiver of the Editor type
     * @throws UnsupportedOperationException if a Set<> is used that contains anything other than Strings
     */
    fun edit(func: Editor.() -> Unit) {
        EditorImpl(prefs).apply {
            func()
            apply()
        }
    }

    /**
     * SharedPreferences.Editor
     */
    interface Editor : SharedPreferences.Editor {
        /**
         * assign a value to a given key
         *
         * @receiver the key of the preference you want to assign
         * @param value value to be assigned to the key, must be Boolean, Float, Int, Long, String, or Set<String>
         * @throws UnsupportedOperationException if a Set<> is used that contains anything other than Strings
         */
        infix fun String.to(value: Any)
    }

    private class EditorImpl(prefs: SharedPreferences)
        : Editor, SharedPreferences.Editor by prefs.edit() {

        override infix fun String.to(value: Any) {
            when (value) {
                is Boolean -> putBoolean(this, value)
                is Float -> putFloat(this, value)
                is Int -> putInt(this, value)
                is Long -> putLong(this, value)
                is String -> putString(this, value)
                is Set<*> -> {
                    if (value.any { it !is String }) throw UnsupportedOperationException("Sets must only contain Strings")
                    @Suppress("UNCHECKED_CAST")
                    if (value.isEmpty()) remove(this)
                    else putStringSet(this, value as Set<String>)
                }
                else -> throw UnsupportedOperationException("type ${value::class.java} not supported")
            }

        }
    }
}


