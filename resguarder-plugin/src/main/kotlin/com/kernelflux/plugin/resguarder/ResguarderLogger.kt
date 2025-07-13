package com.kernelflux.plugin.resguarder

import org.gradle.api.Project
import java.io.File

object ResguarderLogger {
    var enableLog = false
    var enableFileLog = false
    private var logFile: File? = null


    @JvmStatic
    fun init(project: Project) {
        if (enableLog) {
            logFile =
                project.layout.buildDirectory.file("outputs/resource_aop_log.txt").get().asFile
            logFile?.writeText("")
        }
    }

    @JvmStatic
    fun log(msg: String) {
        if (enableLog) {
            println("[Resguarder] $msg")
        }
        if (enableLog) {
            logFile?.appendText("[Resguarder] $msg\n")
        }
    }
}