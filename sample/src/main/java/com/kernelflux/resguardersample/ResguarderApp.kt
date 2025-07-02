package com.kernelflux.resguardersample

import android.app.Application
import com.kernelflux.resguarder.Resguarder

class ResguarderApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Resguarder.init(this)
    }


}