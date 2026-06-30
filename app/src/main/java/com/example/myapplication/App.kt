package com.example.myapplication

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        MapKitFactory.setApiKey("03a1d3d6-e6b5-4d79-a950-8bc68b67fbd5")
        MapKitFactory.initialize(this)
    }
}