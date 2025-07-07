package com.kernelflux.plugin.resguarder

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.tools.r8.internal.va
import com.android.tools.r8.internal.wh
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class ResguarderPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("resguarder", ResguarderExtension::class.java)
        ResguarderLogger.enableFileLog = ext.enableFileLog
        ResguarderLogger.init(project)


        ResguarderLogger.log("ResguarderPlugin apply...")

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        val config = ResguarderAsmConfig(project, ext)

        generateBigImagesConstantsClass(
            project = project,
            packageName = "com.kernelflux.resguarder",
            className = "ResguarderBigImages",
            bigImageIds = config.bigImageResIds,
            resIdTypeMap = config.resIdTypeMap
        )

        androidComponents.onVariants { variant ->
            ResguarderLogger.log("ResguarderPlugin apply2 variant:${variant.name}")
            variant.instrumentation.transformClassesWith(
                ResguarderClassVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) {
            }
            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS)
        }
    }

    private fun generateBigImagesConstantsClass(
        project: Project,
        packageName: String,
        className: String,
        bigImageIds: Set<Int>,
        resIdTypeMap: Map<Int, String>
    ) {
        val generatedDir = project.layout.buildDirectory.file(
            "generated/resguarder/${
                packageName.replace(
                    '.',
                    '/'
                )
            }"
        ).get().asFile
        generatedDir.mkdirs()
        val file = File(generatedDir, "$className.kt")

        val filteredBigImageIds = bigImageIds.filter { id ->
            val type = resIdTypeMap[id]
            type != null && type.equals("bitmap", ignoreCase = true)
        }.toSet()

        file.writeText(buildBigImagesKtContent(packageName, className, filteredBigImageIds))

        if (filteredBigImageIds.isEmpty()) {
            project.logger.warn("No big image ids found, generated $className.kt will be empty")
        }

        // 注册到 AGP SourceGen (最佳)
        val androidComponents =
            project.extensions.findByType(AndroidComponentsExtension::class.java)
        androidComponents?.onVariants { variant ->
            variant.sources.java?.addStaticSourceDirectory(generatedDir.absolutePath)
        }

        // 如果为纯 Java/Kotlin module
        project.extensions.findByName("sourceSets")?.let { sourceSets ->
            val main = sourceSets.javaClass.getMethod("getByName", String::class.java)
                .invoke(sourceSets, "main")
            main.javaClass.getMethod("srcDir", Any::class.java).invoke(main, generatedDir)
        }
    }


    /**
     * 生成 BigImages.kt 内容
     */
    private fun buildBigImagesKtContent(
        packageName: String,
        className: String,
        bigImageIds: Set<Int>
    ): String {
        return """
            package $packageName
            
            object $className {
                val ids = setOf(
                    ${bigImageIds.joinToString(separator = ",\n                    ")}
                )
            }
        """.trimIndent()
    }

}