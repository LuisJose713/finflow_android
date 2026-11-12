package com.softdevcode.finflow_android


import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FinFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.softdevcode.finflow_android.data.updates.scheduleUpdateChecks(this)
    }
}
