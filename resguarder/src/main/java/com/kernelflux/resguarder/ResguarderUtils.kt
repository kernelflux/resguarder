package com.kernelflux.resguarder


object ResguarderUtils {
    private val bigImageIds: Set<Int> by lazy {
        try {
            val clazz = Class.forName("com.kernelflux.resguarder.ResguarderBigImages")
            val field = clazz.getDeclaredField("ids")
            @Suppress("UNCHECKED_CAST")
            field.get(null) as? Set<Int> ?: emptySet()
        } catch (e: Exception) {
            e.printStackTrace()
            emptySet()
        }
    }

    @JvmStatic
    fun isBigImage(resId: Int): Boolean {
        return bigImageIds.contains(resId)
    }
}