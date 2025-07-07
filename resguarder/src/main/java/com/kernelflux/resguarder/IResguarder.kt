package com.kernelflux.resguarder

import android.view.View
import android.widget.ImageView

/**
 * @author: QT
 * @date: 2025/7/7
 */
interface IResguarder {
    fun loadImageResource(view: ImageView, resId: Int)
    fun loadBackgroundResource(view: View, resId: Int)
}