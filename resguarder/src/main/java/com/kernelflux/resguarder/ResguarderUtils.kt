package com.kernelflux.resguarder

object ResguarderUtils {
    private val bigImageIds: Set<Int> by lazy {
        try {
            val clazz = Class.forName("com.kernelflux.resguarder.ResguarderBigImages")
            val field = clazz.getDeclaredField("ids")
            ResguarderLogger.d("Big image IDs loaded successfully")
            val result = field.get(null)
            if (result is Set<*>) {
                @Suppress("UNCHECKED_CAST")
                val ids = result as Set<Int>
                ResguarderLogger.i("Loaded ${ids.size} big image IDs")
                ids
            } else {
                ResguarderLogger.w("Big image IDs field is not a Set, using empty set")
                emptySet()
            }
        } catch (e: Exception) {
            ResguarderLogger.e("Failed to load big image IDs", e, "big_image_loader")
            emptySet()
        }
    }

    @JvmStatic
    fun isBigImage(resId: Int): Boolean {
        ResguarderLogger.d("Checking if resource $resId is a big image")
        val isBig = bigImageIds.contains(resId)
        if (isBig) {
            ResguarderLogger.logBigImageDetected(resId, 0L, "big_image_check")
        }
        return isBig
    }
}