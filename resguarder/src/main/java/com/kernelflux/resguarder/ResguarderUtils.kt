package com.kernelflux.resguarder

import android.util.Log


object ResguarderUtils {
    private val bigImageIds: Set<Int> by lazy {
        try {
            val clazz = Class.forName("com.kernelflux.resguarder.ResguarderBigImages")
            val field = clazz.getDeclaredField("ids")
            @Suppress("UNCHECKED_CAST")
            Log.i("resguarder_tag", "bigImageIds success==========>")
            field.get(null) as? Set<Int> ?: emptySet()
        } catch (e: Exception) {
            Log.i("resguarder_tag", "bigImageIds fail==========> $e")
            e.printStackTrace()
            emptySet()
        }
    }

    @JvmStatic
    fun isBigImage(resId: Int): Boolean {
        Log.i("resguarder_tag", "isBigImage==========> $resId")
        for (id in bigImageIds) {
            Log.i("resguarder_tag", "id==========> $id")
        }
        return bigImageIds.contains(resId)
    }
}