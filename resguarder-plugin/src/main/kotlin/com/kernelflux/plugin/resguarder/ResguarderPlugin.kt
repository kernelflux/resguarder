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
        ResguarderLogger.init(project)
        ResguarderLogger.log("ResguarderPlugin apply...")

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            ResguarderLogger.log("ResguarderPlugin apply2 variant:${variant.name}")

            val taskName = "generateBigImagesConstantsFor${
                variant.name.replaceFirstChar { it.titlecase(Locale.ROOT) }
            }"
            val taskProvider =
                project.tasks.register(taskName, GenerateBigImagesConstantsTask::class.java) {
                    variantName.set(variant.name)
                    maxWidth.set(ext.maxWidth)
                    maxHeight.set(ext.maxHeight)
                    maxFileSize.set(ext.maxFileSize)
                    allBitmapUseImageLoader.set(ext.allBitmapUseImageLoader)
                    enableFileLog.set(ext.enableFileLog)
                    projectDir.set(project.projectDir)
                    buildDir.set(project.layout.buildDirectory.get().asFile)
                    outputDir.set(project.layout.buildDirectory.dir("generated/resguarder/com/kernelflux/resguarder"))
                }

            // 让 Java 源集包含生成目录
            variant.sources.java?.addStaticSourceDirectory(
                project.layout.buildDirectory.dir("generated/resguarder/com/kernelflux/resguarder")
                    .get().asFile.absolutePath
            )

            // 让 Task 在 Java 编译后执行（确保 R.class 已生成）
            project.tasks.matching { it.name == "compile${variant.name.replaceFirstChar { it.uppercaseChar() }}JavaWithJavac" }
                .configureEach { finalizedBy(taskProvider) }
            // 如果是 Kotlin 项目，也加上
            project.tasks.matching { it.name == "compile${variant.name.replaceFirstChar { it.uppercaseChar() }}Kotlin" }
                .configureEach { finalizedBy(taskProvider) }

            variant.instrumentation.transformClassesWith(
                ResguarderClassVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) {}
            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS)

        }
    }
}