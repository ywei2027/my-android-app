package com.example.myandroidapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (com.example.myandroidapp.BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
