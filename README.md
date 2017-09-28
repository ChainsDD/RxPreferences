# RxPreferences
Android SharedPreferences made reactive

Usage of this library is simple, create an RxPreferences instance, and use that to obtain observables of your preferences.

    val rxPrefs = RxPreferences(context)
    rxPrefs["key", "default"]
        .subscribe { /* do whatever with your preference */ }

Events are delivered on via an optional scheduler in the RxPreferences constructor (which defaults to Schedulers.io())

RxPreferences also provides Consumers for updating preferences.

    Observable.just("foo").subscribe(rxPrefs["key"])
