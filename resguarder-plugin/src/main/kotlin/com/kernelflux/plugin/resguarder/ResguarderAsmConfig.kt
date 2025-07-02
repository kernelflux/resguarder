package com.kernelflux.plugin.resguarder

import org.gradle.api.Project
import java.io.File
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory

class ResguarderAsmConfig(
    project: Project,
    ext: ResguarderExtension
) {
    val resIdTypeMap: Map<Int, String>
    val bigImageResIds: Set<Int>

    init {
        val (idTypeMap, bigIds) = scanResIdTypeMap(project, ext)
        resIdTypeMap = idTypeMap
        bigImageResIds = bigIds
    }

    private fun getAllResDirs(project: Project): Set<File> {
        val resDirs = mutableSetOf<File>()
        val androidExtension = project.extensions.findByName("android")
        try {
            val baseExtClass = Class.forName("com.android.build.gradle.BaseExtension")
            if (baseExtClass.isInstance(androidExtension)) {
                val sourceSets = baseExtClass.getMethod("getSourceSets").invoke(androidExtension) as Iterable<*>
                for (sourceSet in sourceSets) {
                    val resField = sourceSet?.javaClass?.getMethod("getRes")?.invoke(sourceSet)
                    val srcDirs = resField?.javaClass?.getMethod("getSrcDirs")?.invoke(resField) as? Iterable<*>
                    srcDirs?.forEach { dir ->
                        if (dir is File && dir.exists()) resDirs.add(dir)
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略异常，兼容性处理
            ResguarderLogger.log("ResguarderPlugin getAllResDirs exp：${e}")
        }
        return resDirs
    }

    private fun scanResIdTypeMap(project: Project, ext: ResguarderExtension): Pair<Map<Int, String>, Set<Int>> {
        val resDirs = getAllResDirs(project)
        ResguarderLogger.log("ResguarderPlugin scanResIdTypeMap resDirs：${resDirs.size}")

        val drawableDirs = resDirs.flatMap { resDir ->
            resDir.listFiles()?.filter { it.isDirectory && (it.name.startsWith("drawable", true) || it.name.startsWith("mipmap", true)) } ?: emptyList()
        }
        val styleDirs = resDirs.flatMap { resDir ->
            resDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("values", true) } ?: emptyList()
        }
        val layoutDirs = resDirs.flatMap { resDir ->
            resDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("layout", true) } ?: emptyList()
        }

        val nameTypeMap = mutableMapOf<String, String>()
        val usedDrawables = mutableSetOf<String>()
        val factory = DocumentBuilderFactory.newInstance()

        // 1. 扫描 styles/themes 里的图片引用
        for (styleDir in styleDirs) {
            ResguarderLogger.log("fun scanResIdTypeMap 1, styleDir:${styleDir.absolutePath}")
            styleDir.listFiles { f -> f.extension.equals("xml", ignoreCase = true) }?.forEach { file ->
                val doc = factory.newDocumentBuilder().parse(file)
                val items = doc.getElementsByTagName("item")
                for (i in 0 until items.length) {
                    val item = items.item(i)
                    val nameAttr = item.attributes?.getNamedItem("name")?.nodeValue ?: continue
                    if (nameAttr.contains("background", ignoreCase = true) || nameAttr.contains("src", ignoreCase = true)) {
                        val value = item.textContent?.trim() ?: continue
                        ResguarderLogger.log("fun scanResIdTypeMap : $value")
                        if (value.startsWith("@drawable/", ignoreCase = true)) {
                            usedDrawables.add(value.removePrefix("@drawable/"))
                        }
                        if (value.startsWith("@mipmap/", ignoreCase = true)) {
                            usedDrawables.add(value.removePrefix("@mipmap/"))
                        }
                    }
                }
            }
        }

        // 2. 扫描 layout 里的图片引用
        for (layoutDir in layoutDirs) {
            ResguarderLogger.log("fun scanResIdTypeMap 2, layoutDir:${layoutDir.absolutePath}")
            layoutDir.listFiles { f -> f.extension.equals("xml", ignoreCase = true) }?.forEach { file ->
                val doc = factory.newDocumentBuilder().parse(file)
                val nodes = doc.getElementsByTagName("*")
                for (i in 0 until nodes.length) {
                    val node = nodes.item(i)
                    val attrs = node.attributes ?: continue
                    for (j in 0 until attrs.length) {
                        val attr = attrs.item(j)
                        if ((attr.nodeName == "android:background" || attr.nodeName == "android:src")
                            && attr.nodeValue.startsWith("@drawable/", ignoreCase = true)) {
                            val drawableName = attr.nodeValue.removePrefix("@drawable/")
                            usedDrawables.add(drawableName)
                        }
                        if ((attr.nodeName == "android:background" || attr.nodeName == "android:src")
                            && attr.nodeValue.startsWith("@mipmap/", ignoreCase = true)) {
                            val drawableName = attr.nodeValue.removePrefix("@mipmap/")
                            usedDrawables.add(drawableName)
                        }
                    }
                }
            }
        }

        // 3. 资源名 -> 类型
        for (drawableName in usedDrawables) {
            ResguarderLogger.log("fun scanResIdTypeMap 3, drawableName:${drawableName}")

            var type = "other"
            var file: File? = null

            // 检查所有 drawable 目录，兼容大小写后缀
            for (dir in drawableDirs) {
                dir.listFiles()?.forEach { f ->
                    val name = f.nameWithoutExtension
                    ResguarderLogger.log("ResguarderPlugin scan drawableDirs: $name")
                    if (name.equals(drawableName, ignoreCase = true)) {
                        when (f.extension.lowercase()) {
                            "png", "jpg", "jpeg", "webp" -> file = f
                            "xml" -> file = f
                        }
                    }
                    // 9.png 特殊处理
                    if (f.name.equals("$drawableName.9.png", ignoreCase = true)) {
                        file = f
                    }
                }
                if (file?.exists()==true) break
            }

            val sFile= file
            if (sFile == null || !sFile.exists()) continue
            if (sFile.extension.equals("xml", ignoreCase = true)) {
                val doc = factory.newDocumentBuilder().parse(sFile)
                val root = doc.documentElement.nodeName
                type = when (root) {
                    "selector", "shape", "layer-list", "ripple" -> "shape"
                    "vector", "animated-vector" -> "vector"
                    "color" -> "color"
                    else -> "xml"
                }
            } else if (sFile.extension.lowercase() in setOf("png", "jpg", "jpeg", "webp") || sFile.name.lowercase().endsWith(".9.png")) {
                type = "bitmap"
            }

            nameTypeMap[drawableName] = type
        }

        // 4. 资源名 -> id
        val rDrawableClass = try {
            Class.forName("${project.group}.R\$drawable")
        } catch (_: Exception) {
            null
        }
        val idTypeMap = mutableMapOf<Int, String>()
        val bigIds = mutableSetOf<Int>()
        if (rDrawableClass != null) {
            for (field in rDrawableClass.declaredFields) {
                if (!usedDrawables.any { it.equals(field.name, ignoreCase = true) }) continue // 只处理被引用的
                val type = nameTypeMap[field.name] ?: "other"
                val id = field.getInt(null)
                idTypeMap[id] = type
                // 是否需要ImageLoader
                if (type == "bitmap") {
                    if (ext.allBitmapUseImageLoader) {
                        bigIds.add(id)
                    } else {
                        // 判断阈值
                        val file = drawableDirs.firstNotNullOfOrNull { d ->
                            d.listFiles()?.find { f -> f.nameWithoutExtension.equals(field.name, ignoreCase = true) }
                        }
                        var isBig = false
                        if (file != null && (file.extension.lowercase() in setOf("png", "jpg", "jpeg", "webp") || file.name.lowercase().endsWith(".9.png"))) {
                            try {
                                val img = ImageIO.read(file)
                                if (img != null) {
                                    if (img.width > ext.maxWidth || img.height > ext.maxHeight || file.length() > ext.maxFileSize) {
                                        isBig = true
                                    }
                                } else {
                                    if (file.length() > ext.maxFileSize) {
                                        isBig = true
                                    }
                                }
                            } catch (_: Exception) {
                                if (file.length() > ext.maxFileSize) {
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
        return idTypeMap to bigIds
    }
}