package com.kernelflux.plugin.resguarder

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object ResScanner {

    data class Result(
        val nameTypeMap: Map<String, String>,
        val usedDrawables: Set<String>,
        val drawableDirs: List<File>
    )

    /**
     * 扫描 res 目录，返回资源名-类型映射、被引用的 drawable 名集合、所有 drawable 目录
     */
    fun scan(projectDir: File): Result {
        val resDirs = findAllResDirs(projectDir)
        ResguarderLogger.log("ResScanner: found resDirs:")
        resDirs.forEach { ResguarderLogger.log("  ${it.absolutePath}") }

        val drawableDirs = resDirs.flatMap { resDir ->
            val subDirs = resDir.listFiles()?.filter {
                it.isDirectory && (it.name.startsWith(
                    "drawable",
                    true
                ) || it.name.startsWith("mipmap", true))
            } ?: emptyList()
            ResguarderLogger.log("ResScanner: drawableDirs in ${resDir.absolutePath}:")
            subDirs.forEach { ResguarderLogger.log("  ${it.absolutePath}") }
            subDirs
        }
        val styleDirs = resDirs.flatMap { resDir ->
            val subDirs =
                resDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("values", true) }
                    ?: emptyList()
            ResguarderLogger.log("ResScanner: styleDirs in ${resDir.absolutePath}:")
            subDirs.forEach { ResguarderLogger.log("  ${it.absolutePath}") }
            subDirs
        }
        val layoutDirs = resDirs.flatMap { resDir ->
            val subDirs =
                resDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("layout", true) }
                    ?: emptyList()
            ResguarderLogger.log("ResScanner: layoutDirs in ${resDir.absolutePath}:")
            subDirs.forEach { ResguarderLogger.log("  ${it.absolutePath}") }
            subDirs
        }

        val nameTypeMap = mutableMapOf<String, String>()
        val usedDrawables = mutableSetOf<String>()
        val factory = DocumentBuilderFactory.newInstance()

        // 1. 扫描 styles/themes 里的图片引用
        for (styleDir in styleDirs) {
            styleDir.listFiles { f -> f.extension.equals("xml", ignoreCase = true) }
                ?.forEach { file ->
                    ResguarderLogger.log("ResScanner: parsing style xml: ${file.absolutePath}")
                    try {
                        val doc = factory.newDocumentBuilder().parse(file)
                        val items = doc.getElementsByTagName("item")
                        for (i in 0 until items.length) {
                            val item = items.item(i)
                            val nameAttr =
                                item.attributes?.getNamedItem("name")?.nodeValue ?: continue
                            if (nameAttr.contains(
                                    "background",
                                    ignoreCase = true
                                ) || nameAttr.contains("src", ignoreCase = true)
                            ) {
                                val value = item.textContent?.trim() ?: continue
                                ResguarderLogger.log("ResScanner: found style item value: $value")
                                if (value.startsWith("@drawable/", ignoreCase = true)) {
                                    val name = value.removePrefix("@drawable/")
                                    usedDrawables.add(name)
                                    ResguarderLogger.log("ResScanner: add used drawable: $name")
                                }
                                if (value.startsWith("@mipmap/", ignoreCase = true)) {
                                    val name = value.removePrefix("@mipmap/")
                                    usedDrawables.add(name)
                                    ResguarderLogger.log("ResScanner: add used mipmap: $name")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        ResguarderLogger.log("ResScanner: XML parse error: ${file.absolutePath}, $e")
                    }
                }
        }

        // 2. 扫描 layout 里的图片引用
        for (layoutDir in layoutDirs) {
            layoutDir.listFiles { f -> f.extension.equals("xml", ignoreCase = true) }
                ?.forEach { file ->
                    ResguarderLogger.log("ResScanner: parsing layout xml: ${file.absolutePath}")
                    try {
                        val doc = factory.newDocumentBuilder().parse(file)
                        val nodes = doc.getElementsByTagName("*")
                        for (i in 0 until nodes.length) {
                            val node = nodes.item(i)
                            val attrs = node.attributes ?: continue
                            for (j in 0 until attrs.length) {
                                val attr = attrs.item(j)
                                if ((attr.nodeName == "android:background" || attr.nodeName == "android:src")
                                    && attr.nodeValue.startsWith("@drawable/", ignoreCase = true)
                                ) {
                                    val drawableName = attr.nodeValue.removePrefix("@drawable/")
                                    usedDrawables.add(drawableName)
                                    ResguarderLogger.log("ResScanner: add used drawable: $drawableName")
                                }
                                if ((attr.nodeName == "android:background" || attr.nodeName == "android:src")
                                    && attr.nodeValue.startsWith("@mipmap/", ignoreCase = true)
                                ) {
                                    val drawableName = attr.nodeValue.removePrefix("@mipmap/")
                                    usedDrawables.add(drawableName)
                                    ResguarderLogger.log("ResScanner: add used mipmap: $drawableName")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        ResguarderLogger.log("ResScanner: XML parse error: ${file.absolutePath}, $e")
                    }
                }
        }

        // 3. 资源名 -> 类型
        for (drawableName in usedDrawables) {
            var type = "other"
            var file: File? = null

            // 检查所有 drawable 目录，兼容大小写后缀
            for (dir in drawableDirs) {
                dir.listFiles()?.forEach { f ->
                    val name = f.nameWithoutExtension
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
                if (file?.exists() == true) break
            }

            val sFile = file
            if (sFile == null || !sFile.exists()) {
                ResguarderLogger.log("ResScanner: file not found for drawable: $drawableName")
                continue
            }
            if (sFile.extension.equals("xml", ignoreCase = true)) {
                try {
                    val doc = factory.newDocumentBuilder().parse(sFile)
                    val root = doc.documentElement.nodeName
                    type = when (root) {
                        "selector", "shape", "layer-list", "ripple" -> "shape"
                        "vector", "animated-vector" -> "vector"
                        "color" -> "color"
                        else -> "xml"
                    }
                    ResguarderLogger.log("ResScanner: $drawableName is xml type: $type")
                } catch (e: Exception) {
                    ResguarderLogger.log("ResScanner: XML parse error: ${sFile.absolutePath}, $e")
                }
            } else if (sFile.extension.lowercase() in setOf(
                    "png",
                    "jpg",
                    "jpeg",
                    "webp"
                ) || sFile.name.lowercase().endsWith(".9.png")
            ) {
                type = "bitmap"
                ResguarderLogger.log("ResScanner: $drawableName is bitmap")
            }

            nameTypeMap[drawableName] = type
        }

        ResguarderLogger.log("ResScanner: nameTypeMap result:")
        nameTypeMap.forEach { (name, type) ->
            ResguarderLogger.log("  $name -> $type")
        }
        ResguarderLogger.log("ResScanner: usedDrawables result:")
        usedDrawables.forEach { name ->
            ResguarderLogger.log("  $name")
        }

        return Result(
            nameTypeMap = nameTypeMap,
            usedDrawables = usedDrawables,
            drawableDirs = drawableDirs
        )
    }

    /**
     * 递归查找所有 res 目录（支持多 module/多 sourceSet）
     */
    private fun findAllResDirs(projectDir: File): Set<File> {
        val resDirs = mutableSetOf<File>()
        fun scan(dir: File) {
            if (!dir.exists() || !dir.isDirectory) return
            if (dir.name == "res") {
                resDirs.add(dir)
            } else {
                dir.listFiles()?.forEach { scan(it) }
            }
        }
        scan(File(projectDir, "src"))
        // 兼容 buildSrc、其它自定义结构
        File(projectDir, "src").listFiles()?.forEach { scan(it) }
        return resDirs
    }
}