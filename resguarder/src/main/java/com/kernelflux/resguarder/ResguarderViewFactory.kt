package com.kernelflux.resguarder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView

class ResguarderViewFactory(private val delegate: LayoutInflater.Factory2?) : LayoutInflater.Factory2 {
    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        val view = delegate?.onCreateView(parent, name, context, attrs)
        if (view is ImageView) {
            // 标准属性
            val srcResId = attrs.getAttributeResourceValue(
                "http://schemas.android.com/apk/res/android",
                "src",
                0
            )
            if (srcResId != 0) Resguarder.loadImageResource(view, srcResId)
            val bgResId = attrs.getAttributeResourceValue(
                "http://schemas.android.com/apk/res/android",
                "background",
                0
            )
            if (bgResId != 0) Resguarder.loadBackgroundResource(view, bgResId)
        }
        return view
    }
    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return onCreateView(null, name, context, attrs)
    }
}