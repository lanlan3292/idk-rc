package com.lanlan3292.resolutionchanger

import android.app.Application
import com.google.android.material.color.DynamicColors

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Material You 动态取色（Android 12+）
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
