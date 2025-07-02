package com.kernelflux.plugin.resguarder

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.tools.r8.internal.va
import org.gradle.api.Plugin
import org.gradle.api.Project

class ResguarderPlugin:Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("resguarder", ResguarderExtension::class.java)
        ResguarderLogger.enableFileLog = ext.enableFileLog
        ResguarderLogger.init(project)


        ResguarderLogger.log("ResguarderPlugin apply...")

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        val config = ResguarderAsmConfig(project, ext)
        androidComponents.onVariants { variant ->
            ResguarderLogger.log("ResguarderPlugin apply2 variant:${variant.name}")
            variant.instrumentation.transformClassesWith(
                ResguarderClassVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) { params ->
                params.bigImageResIds.set(config.bigImageResIds)
                params.resIdTypeMap.set(config.resIdTypeMap)
            }
            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS)
        }
    }

}