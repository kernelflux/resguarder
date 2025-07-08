package com.kernelflux.plugin.resguarder

import org.objectweb.asm.ClassReader
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

object RClassParser {

    // Cache structure: rJar absolute path + packageName -> type -> name->id
    private val cache = ConcurrentHashMap<String, Map<String, Map<String, Int>>>()

    /**
     * Parse R.jar and return Map<type, Map<name, id>>
     * type such as drawable, layout, string, id, etc.
     */
    @JvmStatic
    fun parseAllTypes(
        rJarFile: File,
        packageName: String,
        logger: ((String) -> Unit)? = null
    ): Map<String, Map<String, Int>> {
        val cacheKey = "${rJarFile.absolutePath}#$packageName"
        cache[cacheKey]?.let {
            logger?.invoke("RClassParser: cache hit for $cacheKey")
            return it
        }

        val result = mutableMapOf<String, MutableMap<String, Int>>()
        if (!rJarFile.exists()) {
            logger?.invoke("RClassParser: R.jar not found: ${rJarFile.absolutePath}")
            return result
        }

        try {
            JarFile(rJarFile).use { jar ->
                jar.entries().asSequence()
                    .filter {
                        it.name.startsWith(
                            packageName.replace(
                                '.',
                                '/'
                            ) + "/R$"
                        ) && it.name.endsWith(".class")
                    }
                    .forEach { entry ->
                        val type = entry.name
                            .substringAfter("/R$")
                            .substringBefore(".class")
                        val typeMap = result.getOrPut(type) { mutableMapOf() }
                        jar.getInputStream(entry).use { input ->
                            val cr = ClassReader(input)
                            cr.accept(object : org.objectweb.asm.ClassVisitor(Opcodes.ASM9) {
                                override fun visitField(
                                    access: Int,
                                    name: String,
                                    desc: String?,
                                    signature: String?,
                                    value: Any?
                                ): FieldVisitor? {
                                    // Only static int fields
                                    if ((access and Opcodes.ACC_STATIC) != 0 && value is Int) {
                                        typeMap[name] = value
                                    }
                                    return null
                                }
                            }, 0)
                        }
                        logger?.invoke("RClassParser: parsed type $type, size=${typeMap.size}")
                    }
            }
            cache[cacheKey] = result
        } catch (e: Exception) {
            logger?.invoke("RClassParser: error parsing R.jar: ${e.message}")
        }
        return result
    }

    /**
     * Parse name->id mapping for the specified type
     */
    @JvmStatic
    fun parseResNameIdMap(
        rJarFile: File,
        packageName: String,
        resType: String,
        logger: ((String) -> Unit)? = null
    ): Map<String, Int> {
        return parseAllTypes(rJarFile, packageName, logger)[resType] ?: emptyMap()
    }

    /**
     * Parse id->name mapping for the specified type
     */
    @JvmStatic
    fun parseResIdNameMap(
        rJarFile: File,
        packageName: String,
        resType: String,
        logger: ((String) -> Unit)? = null
    ): Map<Int, String> {
        return parseResNameIdMap(
            rJarFile,
            packageName,
            resType,
            logger
        ).entries.associate { (k, v) -> v to k }
    }

}