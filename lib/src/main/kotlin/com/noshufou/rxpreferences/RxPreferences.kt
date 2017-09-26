package com.noshufou.rxpreferences

import android.content.Context
import android.content.SharedPreferences
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.io.IOException

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
            Observable.fromCallable { prefs[key, defValue] }.subscribeOn(scheduler)
                    .concatWith(changeObservable.subscribeOn(scheduler)
                            .filter { it.first == key }
                            .map { it.second }
                            .map { it as T })

    fun apply(func: Editor.() -> Unit) {
        Editor(prefs).apply {
            func()
            apply()
        }
    }

    fun commit(func: Editor.() -> Unit): Completable {
        val editor = Editor(prefs)
        try {
            editor.func()
        } catch (e: Throwable) {
            return Completable.error(e)
        }
        return Completable.fromAction {
            if (!editor.commit()) throw IOException("failed to write preferences")
        }.subscribeOn(scheduler)
    }

    fun <T : Any> put(key: String): Consumer<T> {
        return Consumer {
            Editor(prefs).apply {
                key to it
                apply()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private operator fun <T : Any> SharedPreferences.get(key: String, defValue: T): T =
            when (defValue) {
                is Boolean -> getBoolean(key, defValue) as T
                is Float -> getFloat(key, defValue) as T
                is Int -> getInt(key, defValue) as T
                is Long -> getLong(key, defValue) as T
                is String -> getString(key, defValue) as T
                is Set<*> -> {
                    if (defValue.any { it !is String }) throw UnsupportedOperationException("Only Set<String> is supported")
                    getStringSet(key, defValue as Set<String>) as T
                }
                else -> throw UnsupportedOperationException("no accessor found for type ${defValue::class.java}")
            }

    class Editor(prefs: SharedPreferences)
        : SharedPreferences.Editor by prefs.edit() {

        infix fun String.to(value: Any) {
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


