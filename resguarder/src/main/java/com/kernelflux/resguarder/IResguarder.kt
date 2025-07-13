package com.kernelflux.resguarder

import android.view.View
import android.widget.ImageView


interface IResguarder {
    fun loadImageResource(view: ImageView, resId: Int)
    fun loadBackgroundResource(view: View, resId: Int)
}