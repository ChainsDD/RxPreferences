package com.noshufou.rxpreferences

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    private lateinit var rxPrefs: RxPreferences

    @Before
    fun init() {
        rxPrefs = RxPreferences(InstrumentationRegistry.getTargetContext())
        rxPrefs.commit { clear() }.test().await()
    }

    @Test
    fun basic() {
        Log.d("TestThread", "Start of test: ${Thread.currentThread().name}")
        val testObserver = rxPrefs["int", 0].test()
        testObserver.awaitCount(1) // Wait here for the default value

        rxPrefs.commit { "int" to 1 }.test().await()

        testObserver.awaitCount(2)
        testObserver.assertValues(0, 1)
        Log.d("TestThread", "End of test: ${Thread.currentThread().name}")
    }
}
