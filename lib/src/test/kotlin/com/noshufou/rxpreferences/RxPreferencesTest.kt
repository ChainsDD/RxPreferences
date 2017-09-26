package com.noshufou.rxpreferences

import android.content.Context
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Test

class RxPreferencesTest {

    private lateinit var rxPrefs: RxPreferences

    @Before
    fun init() {
        val context = mock<Context> {
            on { getSharedPreferences(any(), any()) } doReturn MockSharedPreferences()
            on { packageName } doReturn "foo"
        }
        rxPrefs = RxPreferences(context, scheduler = Schedulers.trampoline())
    }

    @Test fun observable() {
        val testObserver: TestObserver<Int> = rxPrefs["int", 0].test()
        testObserver.assertValue(0)

        rxPrefs.apply {
            "int" to 1
            "str" to "bar"
        }

        testObserver.assertValues(0, 1)
        testObserver.dispose()
    }

    @Test fun multipleObservables() {
        val strObserver = rxPrefs["str", "foo"].test()
        val intObserver = rxPrefs["int", 0].test()
        val disposables = CompositeDisposable()
        disposables.addAll(strObserver, intObserver)

        strObserver.assertValue("foo")
        intObserver.assertValue(0)

        rxPrefs.apply {
            "int" to 1
            "str" to "bar"
        }

        strObserver.assertValues("foo", "bar")
        intObserver.assertValues(0, 1)
        disposables.dispose()
    }

    @Test fun wrongType() {
        rxPrefs.apply { "int" to 1 }
        val strObserver = rxPrefs["int", ""].test()
        strObserver.assertError(ClassCastException::class.java)
    }

    @Test fun stringSet() {
        rxPrefs.apply { "strs" to setOf("foo", "bar") }
        val setObserver = rxPrefs["strs", emptySet<String>()].test()
        setObserver.assertValue(setOf("foo", "bar"))
        setObserver.dispose()
    }

    @Test fun setNotStrings() {
        val shouldFail = rxPrefs.commit { "ints" to setOf(0, 1) }.test()
        shouldFail.assertError(UnsupportedOperationException::class.java)
    }

    @Test fun put() {
        val intObserver = rxPrefs["int", 0].test()
        Observable.just(1, 2, 3).subscribe(rxPrefs.put("int"))
        intObserver.assertValues(0, 1, 2, 3)
    }
}