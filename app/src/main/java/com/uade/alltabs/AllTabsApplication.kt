package com.uade.alltabs

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AllTabsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Application initialization code here
    }
}
