package com.kernelflux.plugin.resguarder

import org.gradle.api.Project
import java.io.File

object ResguarderLogger {
    var enableFileLog = false
    private var logFile: File? = null


    @JvmStatic
    fun init(project: Project) {
        if (enableFileLog) {
            logFile = project.layout.buildDirectory.file("outputs/resource_aop_log.txt").get().asFile
            logFile?.writeText("")
        }
    }

    @JvmStatic
    fun log(msg: String) {
        println("[Resguarder] $msg")
        if (enableFileLog) {
            logFile?.appendText("[Resguarder] $msg\n")
        }
    }
}