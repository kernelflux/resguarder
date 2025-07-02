package com.kernelflux.resguarder

import android.util.Log
import android.view.View

object ResguarderImageLoader {

    @JvmStatic
    fun load(view: View, resId: Int){
        Log.i("xxx_tag","view:$view, resId: $resId")
    }

}