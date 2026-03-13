package io.github.darrindeyoung791.habitpulse

import android.app.Application
import com.google.android.material.color.DynamicColors

class HabitPulseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply Material Dynamic Colors (Monet) to activities when available (Android 12+)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
