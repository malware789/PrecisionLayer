package com.example.precisionlayertesting

import android.app.Application
import com.example.precisionlayertesting.core.di.ManualDI

class PrecisionLayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ManualDI.init(this)
    }
}
