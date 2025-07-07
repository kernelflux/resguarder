package com.kernelflux.resguardersample

import android.app.Application
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.kernelflux.resguarder.IResguarder
import com.kernelflux.resguarder.Resguarder

class ResguarderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Resguarder.setResguarderCallback(object : IResguarder {
            override fun loadImageResource(view: ImageView, resId: Int) {
                Log.i("xxx_tag","loadImageResource, view:$view, resId:$resId")
                GlideApp.with(view).load(resId).into(view)
            }

            override fun loadBackgroundResource(view: View, resId: Int) {
                Log.i("xxx_tag","loadBackgroundResource, view:$view, resId:$resId")
                GlideApp.with(view).load(resId).into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        view.setBackgroundResource(resId)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        //
                    }
                })
            }
        })
        Resguarder.init(this)
    }


}