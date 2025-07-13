package com.kernelflux.resguarder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View

class ProxyFactory2(
    private val delegates: List<LayoutInflater.Factory2>,
    private val customHandler: ((View?, String, Context, AttributeSet) -> View?)? = null
) : LayoutInflater.Factory2 {

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        var view: View? = null
        for (delegate in delegates) {
            view = delegate.onCreateView(parent, name, context, attrs)
            if (view != null) break
        }
        val handledView = customHandler?.invoke(view, name, context, attrs)
        return handledView ?: view
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return onCreateView(null, name, context, attrs)
    }
}