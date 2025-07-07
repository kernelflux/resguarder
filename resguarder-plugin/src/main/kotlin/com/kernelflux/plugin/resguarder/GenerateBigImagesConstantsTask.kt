package com.kernelflux.plugin.resguarder

import com.android.tools.r8.internal.va
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.jar.JarFile
import javax.imageio.ImageIO

abstract class GenerateBigImagesConstantsTask : DefaultTask() {
    @Input
    val variantName: Property<String> = project.objects.property(String::class.java)

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

    @Input
    val enableFileLog: Property<Boolean> = project.objects.property(Boolean::class.java)

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val maxWidth = maxWidth.get()
        val maxHeight = maxHeight.get()
        val maxFileSize = maxFileSize.get()
        val allBitmapUseImageLoader = allBitmapUseImageLoader.get()
        val enableFileLog = enableFileLog.get()

        val projectDir = projectDir.get()
        val buildDir = buildDir.get()
        val variant = variantName.get()

        ResguarderLogger.enableFileLog = enableFileLog

        // 1. 资源扫描逻辑（和你原来的一样）
        val (nameTypeMap, usedDrawables, drawableDirs) = ResScanner.scan(projectDir)
        ResguarderLogger.log("ResScanner finished, usedDrawables: $usedDrawables")

        // 2. 解析 R.jar
        val idTypeMap = mutableMapOf<Int, String>()
        val bigIds = mutableSetOf<Int>()

        // 获取 Android 包名
        val androidExt = project.extensions.findByName("android")
        ResguarderLogger.log("fun scanResIdTypeMap 4.1, androidExt:${androidExt}")
        val packageName: String? = try {
            // AGP 7.0+ 推荐用 namespace
            val namespace =
                androidExt?.javaClass?.getMethod("getNamespace")?.invoke(androidExt) as? String
            ResguarderLogger.log("fun scanResIdTypeMap 4.2, namespace:${namespace}")
            if (!namespace.isNullOrBlank()) {
                namespace
            } else {
                // fallback: defaultConfig.applicationId
                val defaultConfig =
                    androidExt?.javaClass?.getMethod("getDefaultConfig")?.invoke(androidExt)
                val appId = defaultConfig?.javaClass?.getMethod("getApplicationId")
                    ?.invoke(defaultConfig) as? String
                ResguarderLogger.log("fun scanResIdTypeMap 4.3, defaultConfig:${defaultConfig},appId:${appId}")
                if (!appId.isNullOrBlank()) appId else null
            }
        } catch (e: Exception) {
            ResguarderLogger.log("fun scanResIdTypeMap 5, packageNameGetExp:${e}")
            null
        }

        val rJar = File(
            project.projectDir,
            "build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/$variant/processDebugResources/R.jar"
        )
        if (rJar.exists() && packageName != null) {
            ResguarderLogger.log("R.jar found: ${rJar.absolutePath}， packageName： $packageName")
            val jarFile = JarFile(rJar)
            val entry = jarFile.getEntry("${packageName.replace('.', '/')}/R\$drawable.class")
            if (entry != null) {
                jarFile.getInputStream(entry).use { input ->
                    val cr = ClassReader(input)
                    cr.accept(object : org.objectweb.asm.ClassVisitor(Opcodes.ASM9) {
                        override fun visitField(
                            access: Int,
                            name: String,
                            desc: String?,
                            signature: String?,
                            value: Any?
                        ): FieldVisitor? {
                            ResguarderLogger.log("visitField name:$name, access:$access, value:$value, isInt:${value is Int}, isLong:${value is Long}")
                            if ((access and Opcodes.ACC_STATIC) != 0 && (access and Opcodes.ACC_PUBLIC) != 0 && value is Int) {
                                if (usedDrawables.any { it.equals(name, ignoreCase = true) }) {
                                    val type = nameTypeMap[name] ?: "other"
                                    val id = value
                                    idTypeMap[id] = type
                                    ResguarderLogger.log("visitField2 type:$type, id:$value")
                                    // 是否需要ImageLoader
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
                            return null
                        }
                    }, 0)
                }
            } else {
                ResguarderLogger.log(
                    "R\$drawable.class not found in R.jar: ${
                        packageName.replace(
                            '.',
                            '/'
                        )
                    }/R\$drawable.class"
                )
            }
        } else {
            ResguarderLogger.log("R.jar not found: ${rJar.absolutePath} or packageName is null")
        }

        // 3. 生成常量类
        val generatedDir = File(buildDir, "generated/resguarder/com/kernelflux/resguarder")
        generatedDir.mkdirs()
        val file = File(generatedDir, "ResguarderBigImages.kt")
        val filteredBigImageIds =
            bigIds.filter { idTypeMap[it]?.equals("bitmap", ignoreCase = true) == true }.toSet()
        file.writeText(
            buildBigImagesKtContent(
                "com.kernelflux.resguarder",
                "ResguarderBigImages",
                filteredBigImageIds
            )
        )

        // 4. 流式日志输出
        ResguarderLogger.log("fun scanResIdTypeMap 5, idTypeMap:")
        idTypeMap.forEach { (id, type) ->
            ResguarderLogger.log("  id: 0x${id.toString(16)}, type: $type")
        }
        ResguarderLogger.log("fun scanResIdTypeMap 6, bigIds:")
        bigIds.forEach { id ->
            ResguarderLogger.log("  bigId: 0x${id.toString(16)}")
        }
    }


    private fun buildBigImagesKtContent(
        packageName: String,
        className: String,
        bigImageIds: Set<Int>
    ): String {
        val idsContent = if (bigImageIds.isEmpty()) {
            "emptySet<Int>()"
        } else {
            "setOf(\n" +
                    bigImageIds.joinToString(separator = ",\n") { "        $it" } + // 8空格缩进
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