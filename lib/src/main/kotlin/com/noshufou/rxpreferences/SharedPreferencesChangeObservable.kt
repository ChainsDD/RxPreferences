package com.noshufou.rxpreferences

import android.content.SharedPreferences
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable

internal class SharedPreferencesChangeObservable(private val preferences: SharedPreferences,
                                                 private val scheduler: Scheduler)
    : Observable<Pair<String, Any>>() {

    /**
     * Called when someone subscribes to the observable
     */
    override fun subscribeActual(observer: Observer<in Pair<String, Any>>) {
        val listener = Listener(preferences, observer, scheduler)

        // attach the listener to the observer so it gets disposed on unsubscription
        observer.onSubscribe(listener)
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    class Listener(private val preferences: SharedPreferences,
                   private val observer: Observer<in Pair<String, Any>>,
                   private val scheduler: Scheduler)
        : Disposable, SharedPreferences.OnSharedPreferenceChangeListener {

        private var disposed = false

        override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
            if (!isDisposed) {
                // send out a Pair<String, Any> on the proper Scheduler, since
                // onSharedPreferenceChanged is always called from the main thread
                scheduler.scheduleDirect { observer.onNext(key to prefs.all[key]!!) }
            }
        }

        override fun isDisposed(): Boolean = disposed

        override fun dispose() {
            // stop listening for preference changes, and mark ourselves as disposed
            preferences.unregisterOnSharedPreferenceChangeListener(this)
            disposed = true
        }
    }

}