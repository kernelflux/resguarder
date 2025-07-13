package com.kernelflux.plugin.resguarder

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Locale

class ResguarderPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("resguarder", ResguarderExtension::class.java)
        ResguarderLogger.enableFileLog = ext.enableFileLog
        ResguarderLogger.enableLog = ext.enableLog
        ResguarderLogger.init(project)
        ResguarderLogger.log("ResguarderPlugin apply...")

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            ResguarderLogger.log("ResguarderPlugin apply2 variant:${variant.name}")

            val taskName = "generateResourceConstantsFor${
                variant.name.replaceFirstChar { it.titlecase(Locale.ROOT) }
            }"
            val taskProvider =
                project.tasks.register(taskName, GenerateResourceConstantsTask::class.java) {
                    variantName.set(variant.name)
                    generateClzName.set(ext.resguarderResGenerateClassName)
                    maxWidth.set(ext.maxWidth)
                    maxHeight.set(ext.maxHeight)
                    maxFileSize.set(ext.maxFileSize)
                    allBitmapUseImageLoader.set(ext.allBitmapUseImageLoader)
                    projectDir.set(project.projectDir)
                    buildDir.set(project.layout.buildDirectory.get().asFile)
                    outputDir.set(project.layout.buildDirectory.dir("generated/resguarder/com/kernelflux/resguarder"))
                }


            variant.sources.java?.addStaticSourceDirectory(
                project.layout.buildDirectory.dir("generated/resguarder/com/kernelflux/resguarder")
                    .get().asFile.absolutePath
            )
            project.tasks.matching { it.name == "process${variant.name.replaceFirstChar { it.uppercaseChar() }}Resources" }
                .configureEach { finalizedBy(taskProvider) }
            project.tasks.matching { it.name == "compile${variant.name.replaceFirstChar { it.uppercaseChar() }}JavaWithJavac" }
                .configureEach { dependsOn(taskProvider) }

            project.tasks.matching { it.name == "kaptGenerateStubs${variant.name.replaceFirstChar { it.uppercaseChar() }}Kotlin" }
                .configureEach { dependsOn(taskProvider) }

            project.tasks.matching { it.name == "compile${variant.name.replaceFirstChar { it.uppercaseChar() }}Kotlin" }
                .configureEach { dependsOn(taskProvider) }

            variant.instrumentation.transformClassesWith(
                ResguarderClassVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) {}
            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS)

        }
    }
}