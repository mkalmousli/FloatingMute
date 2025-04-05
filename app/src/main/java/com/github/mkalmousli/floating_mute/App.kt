package com.github.mkalmousli.floating_mute

import android.app.Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
}