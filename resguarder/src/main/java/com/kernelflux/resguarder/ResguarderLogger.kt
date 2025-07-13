package com.kernelflux.resguarder

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Professional logger for Resguarder SDK
 * 
 * Features:
 * - Multiple log levels (VERBOSE, DEBUG, INFO, WARN, ERROR)
 * - Performance optimized with lazy initialization
 * - Thread-safe implementation
 * - Configurable log output
 * - Tag-based filtering
 * - Memory efficient
 * 
 * Usage:
 * ```kotlin
 * ResguarderLogger.d("Custom message")
 * ResguarderLogger.i("User action", "user_click")
 * ResguarderLogger.e("Error occurred", exception)
 * ```
 */
object ResguarderLogger {
    
    private const val DEFAULT_TAG = "Resguarder"
    private const val MAX_TAG_LENGTH = 23 // Android log tag limit
    
    // Log levels
    enum class Level(val priority: Int) {
        VERBOSE(Log.VERBOSE),
        DEBUG(Log.DEBUG),
        INFO(Log.INFO),
        WARN(Log.WARN),
        ERROR(Log.ERROR)
    }
    
    // Configuration
    private var isEnabled = AtomicBoolean(true)
    private var minLevel = Level.DEBUG
    private var customTag: String? = null
    private val tagCache = ConcurrentHashMap<String, String>()
    
    // Performance optimization: lazy initialization
    private val isDebugMode by lazy {
        try {
            // Check if running in debug mode
            val applicationInfo = android.content.Context::class.java
                .getMethod("getApplicationInfo")
                .invoke(null)
            val flags = applicationInfo.javaClass.getField("flags").getInt(applicationInfo)
            (flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Exception) {
            false
        }
    }
    
    /**
     * Check if running in debug mode
     */
    @JvmStatic
    fun isDebugMode(): Boolean = isDebugMode
    
    /**
     * Initialize logger with custom configuration
     */
    @JvmStatic
    fun init(
        enabled: Boolean = true,
        level: Level = Level.DEBUG,
        tag: String? = null
    ) {
        isEnabled.set(enabled)
        minLevel = level
        customTag = tag?.take(MAX_TAG_LENGTH)
    }
    
    /**
     * Enable or disable logging
     */
    @JvmStatic
    fun setEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
    }
    
    /**
     * Set minimum log level
     */
    @JvmStatic
    fun setLevel(level: Level) {
        minLevel = level
    }
    
    /**
     * Set custom tag for all logs
     */
    @JvmStatic
    fun setTag(tag: String) {
        customTag = tag.take(MAX_TAG_LENGTH)
    }
    
    // Log methods with different levels
    
    @JvmStatic
    fun v(message: String, tag: String? = null) {
        log(Level.VERBOSE, message, tag)
    }
    
    @JvmStatic
    fun d(message: String, tag: String? = null) {
        log(Level.DEBUG, message, tag)
    }
    
    @JvmStatic
    fun i(message: String, tag: String? = null) {
        log(Level.INFO, message, tag)
    }
    
    @JvmStatic
    fun w(message: String, tag: String? = null) {
        log(Level.WARN, message, tag)
    }
    
    @JvmStatic
    fun e(message: String, tag: String? = null) {
        log(Level.ERROR, message, tag)
    }
    
    @JvmStatic
    fun e(message: String, throwable: Throwable?, tag: String? = null) {
        log(Level.ERROR, message, tag, throwable)
    }
    
    // Performance logging methods
    
    @JvmStatic
    fun logPerformance(operation: String, startTime: Long, tag: String? = null) {
        val duration = System.currentTimeMillis() - startTime
        i("Performance: $operation took ${duration}ms", tag)
    }
    
    @JvmStatic
    fun logMemoryUsage(operation: String, tag: String? = null) {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsage = (usedMemory * 100 / maxMemory).toInt()
        i("Memory: $operation - ${memoryUsage}% used (${usedMemory / 1024 / 1024}MB/${maxMemory / 1024 / 1024}MB)", tag)
    }
    
    // Resource-specific logging methods
    
    @JvmStatic
    fun logResourceObfuscation(resourceName: String, newName: String, tag: String? = null) {
        d("Resource obfuscated: $resourceName -> $newName", tag)
    }
    
    @JvmStatic
    fun logResourcePreserved(resourceName: String, reason: String, tag: String? = null) {
        i("Resource preserved: $resourceName (reason: $reason)", tag)
    }
    
    @JvmStatic
    fun logBigImageDetected(resourceId: Int, size: Long, tag: String? = null) {
        w("Big image detected: R.id.$resourceId (${size / 1024}KB)", tag)
    }
    
    // Private implementation
    
    private fun log(level: Level, message: String, tag: String? = null, throwable: Throwable? = null) {
        // Early return if logging is disabled or level is too low
        if (!isEnabled.get() || level.priority < minLevel.priority) {
            return
        }
        
        val finalTag = getTag(tag)
        val finalMessage = formatMessage(message, level)
        
        when (level) {
            Level.VERBOSE -> Log.v(finalTag, finalMessage, throwable)
            Level.DEBUG -> Log.d(finalTag, finalMessage, throwable)
            Level.INFO -> Log.i(finalTag, finalMessage, throwable)
            Level.WARN -> Log.w(finalTag, finalMessage, throwable)
            Level.ERROR -> Log.e(finalTag, finalMessage, throwable)
        }
    }
    
    private fun getTag(customTag: String?): String {
        val tag = customTag ?: this.customTag ?: DEFAULT_TAG
        return tagCache.getOrPut(tag) { 
            if (tag.length > MAX_TAG_LENGTH) tag.substring(0, MAX_TAG_LENGTH) else tag 
        }
    }
    
    private fun formatMessage(message: String, level: Level): String {
        val timestamp = System.currentTimeMillis()
        val threadName = Thread.currentThread().name
        return "[${level.name}] [$threadName] $message"
    }
    
    // Utility methods for common scenarios
    
    @JvmStatic
    fun logSdkInitialization(version: String, config: String) {
        i("Resguarder SDK initialized - Version: $version, Config: $config")
    }
    
    @JvmStatic
    fun logPluginIntegration(pluginVersion: String) {
        i("Resguarder Plugin integrated - Version: $pluginVersion")
    }
    
    @JvmStatic
    fun logBuildProcess(stage: String, details: String? = null) {
        val message = "Build process: $stage${details?.let { " - $it" } ?: ""}"
        i(message)
    }
    
    @JvmStatic
    fun logErrorWithContext(error: String, context: String, throwable: Throwable? = null) {
        e("Error in $context: $error", throwable)
    }
} 