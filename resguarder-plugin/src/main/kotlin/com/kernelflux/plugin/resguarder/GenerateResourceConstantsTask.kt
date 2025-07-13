package com.kernelflux.plugin.resguarder

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.imageio.ImageIO

abstract class GenerateResourceConstantsTask : DefaultTask() {
    @Input
    val variantName: Property<String> = project.objects.property(String::class.java)


    @Input
    val generateClzName: Property<String> = project.objects.property(String::class.java)

    @Input
    val projectDir: Property<File> = project.objects.property(File::class.java)

    @Input
    val buildDir: Property<File> = project.objects.property(File::class.java)

    @Input
    val maxWidth: Property<Int> = project.objects.property(Int::class.java)

    @Input
    val maxHeight: Property<Int> = project.objects.property(Int::class.java)

    @Input
    val maxFileSize: Property<Long> = project.objects.property(Long::class.java)

    @Input
    val allBitmapUseImageLoader: Property<Boolean> = project.objects.property(Boolean::class.java)

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val maxWidth = maxWidth.get()
        val maxHeight = maxHeight.get()
        val maxFileSize = maxFileSize.get()
        val allBitmapUseImageLoader = allBitmapUseImageLoader.get()
        val generateClassName = generateClzName.get()

        val projectDir = projectDir.get()
        val buildDir = buildDir.get()
        val variant = variantName.get()

        // 1. resource scan
        val (nameTypeMap, usedDrawables, drawableDirs) = ResScanner.scan(projectDir)
        ResguarderLogger.log("ResScanner finished, usedDrawables: $usedDrawables")

        // 2. parse R.jar
        val idTypeMap = mutableMapOf<Int, String>()
        val bigIds = mutableSetOf<Int>()

        // 3. get pkg name
        val androidExt = project.extensions.findByName("android")
        val packageName: String? = try {
            // AGP 7.0+ use  namespace
            val namespace =
                androidExt?.javaClass?.getMethod("getNamespace")?.invoke(androidExt) as? String
            if (!namespace.isNullOrBlank()) {
                namespace
            } else {
                // fallback: defaultConfig.applicationId
                val defaultConfig =
                    androidExt?.javaClass?.getMethod("getDefaultConfig")?.invoke(androidExt)
                val appId = defaultConfig?.javaClass?.getMethod("getApplicationId")
                    ?.invoke(defaultConfig) as? String
                if (!appId.isNullOrBlank()) appId else null
            }
        } catch (_: Exception) {
            null
        }

        //4. parse resource files
        val rJar = File(
            project.projectDir,
            "build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/$variant/processDebugResources/R.jar"
        )
        ResguarderLogger.log("R.jar exist: ${rJar.exists()}， packageName： $packageName")
        if (rJar.exists() && packageName != null) {
            ResguarderLogger.log("R.jar found: ${rJar.absolutePath}， packageName： $packageName")
            val allResMaps =
                RClassParser.parseAllTypes(rJar, packageName) { ResguarderLogger.log(it) }
            val drawableNameIdMap = allResMaps["drawable"] ?: emptyMap()
            for ((name, id) in drawableNameIdMap) {
                if (usedDrawables.any { it.equals(name, ignoreCase = true) }) {
                    val type = nameTypeMap[name] ?: "other"
                    idTypeMap[id] = type
                    if (type == "bitmap") {
                        if (allBitmapUseImageLoader) {
                            bigIds.add(id)
                        } else {
                            // 判断阈值
                            val file = drawableDirs.firstNotNullOfOrNull { d ->
                                d.listFiles()?.find { f ->
                                    f.nameWithoutExtension.equals(
                                        name,
                                        ignoreCase = true
                                    )
                                }
                            }
                            var isBig = false
                            if (file != null && (file.extension.lowercase() in setOf(
                                    "png",
                                    "jpg",
                                    "jpeg",
                                    "webp"
                                ) || file.name.lowercase().endsWith(".9.png"))
                            ) {
                                try {
                                    val img = ImageIO.read(file)
                                    if (img != null) {
                                        if (img.width > maxWidth ||
                                            img.height > maxHeight ||
                                            file.length() > maxFileSize
                                        ) {
                                            isBig = true
                                        }
                                    } else {
                                        if (file.length() > maxFileSize) {
                                            isBig = true
                                        }
                                    }
                                } catch (_: Exception) {
                                    if (file.length() > maxFileSize) {
                                        isBig = true
                                    }
                                }
                            }
                            if (isBig) {
                                bigIds.add(id)
                            }
                        }
                    }
                }
            }

            // 5. generate constant class
            val generatedDir = File(buildDir, "generated/resguarder/com/kernelflux/resguarder")
            generatedDir.mkdirs()
            val file = File(generatedDir, "${generateClassName}.kt")
            val filteredBigImageIds =
                bigIds.filter { idTypeMap[it]?.equals("bitmap", ignoreCase = true) == true }.toSet()
            file.writeText(
                buildBigImagesKtContent(
                    className = generateClassName,
                    bigImageIds = filteredBigImageIds
                )
            )

            // 5. print log
            ResguarderLogger.log("fun scanResIdTypeMap 5, idTypeMap:")
            idTypeMap.forEach { (id, type) ->
                ResguarderLogger.log("  id: 0x${id.toString(16)}, type: $type")
            }
            ResguarderLogger.log("fun scanResIdTypeMap 6, bigIds:")
            bigIds.forEach { id ->
                ResguarderLogger.log("  bigId: 0x${id.toString(16)}")
            }
        } else {
            ResguarderLogger.log("R.jar not found: ${rJar.absolutePath} or packageName is null")
        }
    }


    private fun buildBigImagesKtContent(
        packageName: String = "com.kernelflux.resguarder",
        className: String,
        bigImageIds: Set<Int>
    ): String {
        val idsContent = if (bigImageIds.isEmpty()) {
            "emptySet<Int>()"
        } else {
            "setOf(\n" +
                    bigImageIds.joinToString(separator = ",\n") { "        $it" } +
                    "\n    )"
        }

        return """
        |package $packageName
        |
        |object $className {
        |    @JvmField
        |    val ids = $idsContent
        |}
    """.trimMargin()
    }


}