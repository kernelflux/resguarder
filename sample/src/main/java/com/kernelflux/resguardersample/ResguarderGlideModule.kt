package com.kernelflux.resguardersample

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule


@GlideModule
class ResguarderGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)

        // 设置内存缓存大小20MB
        val memoryCacheSizeBytes = 20 * 1024 * 1024L
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes))

        // 设置磁盘缓存大小100 MB
        val diskCacheSizeBytes = 100 * 1024 * 1024L
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes))
    }

    override fun isManifestParsingEnabled(): Boolean = false


}