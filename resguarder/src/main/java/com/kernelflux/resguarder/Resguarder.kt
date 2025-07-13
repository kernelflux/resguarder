package com.kernelflux.resguarder

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView

object Resguarder {
    const val TAG = "resguarder_tag"

    private val isInHook = object : ThreadLocal<Boolean>() {
        override fun initialValue(): Boolean = false
    }
    private var mIResguarderCallback: IResguarder? = null


    @JvmStatic
    fun setResguarderCallback(callback: IResguarder) {
        mIResguarderCallback = callback
    }

    private inline fun <T> safeHook(block: () -> T): T {
        return if (isInHook.get() == true) {
            block()
        } else {
            isInHook.set(true)
            try {
                block()
            } finally {
                isInHook.set(false)
            }
        }
    }

    @JvmStatic
    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            @SuppressLint("PrivateApi")
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                val inflater = LayoutInflater.from(activity)

                val oldFactory2 = try {
                    val field = LayoutInflater::class.java.getDeclaredField("mFactory2")
                    field.isAccessible = true
                    field.get(inflater) as? LayoutInflater.Factory2
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                val delegates = mutableListOf<LayoutInflater.Factory2>()
                if (oldFactory2 != null) delegates.add(oldFactory2)
                val myHandler: (View?, String, Context, AttributeSet) -> View? =
                    { view, name, context, attrs -> handle(view, name, context, attrs) }
                forceSetFactory2(inflater, ProxyFactory2(delegates, myHandler))
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }


    @JvmStatic
    private fun handle(view: View?, name: String, context: Context, attrs: AttributeSet): View? {
        if (view is ImageView) {
            val srcResId = attrs.getAttributeResourceValue(
                "http://schemas.android.com/apk/res/android",
                "src",
                0
            )
            val bgResId = attrs.getAttributeResourceValue(
                "http://schemas.android.com/apk/res/android",
                "background",
                0
            )

            val srcResId2 = attrs.getAttributeResourceValue(
                "http://schemas.android.com/apk/res-auto",
                "src",
                0
            )
            val bgResId2 = attrs.getAttributeResourceValue(
                "http://schemas.android.com/apk/res-auto",
                "background",
                0
            )

            if (srcResId != 0) loadImageResource(view, srcResId)
            if (bgResId != 0) loadBackgroundResource(view, bgResId)

            if (srcResId2 != 0) loadImageResource(view, srcResId2)
            if (bgResId2 != 0) loadBackgroundResource(view, bgResId2)
        }

        return view
    }

    @SuppressLint("PrivateApi")
    @JvmStatic
    private fun forceSetFactory2(inflater: LayoutInflater, factory2: LayoutInflater.Factory2) {
        try {
            val field = LayoutInflater::class.java.getDeclaredField("mFactory2")
            field.isAccessible = true
            field.set(inflater, factory2)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    @JvmStatic
    fun loadImageResource(view: ImageView, resId: Int) {
        safeHook {
            if (ResguarderUtils.isBigImage(resId)) {
                ResguarderLogger.i(TAG, "loadImageResource isBigImage resId:$resId")
                mIResguarderCallback?.loadImageResource(view, resId) ?: view.setImageResource(resId)
            } else {
                ResguarderLogger.i(TAG, "loadImageResource isNotBigImage resId:$resId")
                view.setImageResource(resId)
            }
        }
    }

    @JvmStatic
    fun loadBackgroundResource(view: View, resId: Int) {
        safeHook {
            if (ResguarderUtils.isBigImage(resId)) {
                ResguarderLogger.i(TAG, "loadBackgroundResource isBigImage resId:$resId")
                mIResguarderCallback?.loadBackgroundResource(view, resId)
                    ?: view.setBackgroundResource(resId)
            } else {
                ResguarderLogger.i(TAG, "loadBackgroundResource isNotBigImage resId:$resId")
                view.setBackgroundResource(resId)
            }
        }
    }


}