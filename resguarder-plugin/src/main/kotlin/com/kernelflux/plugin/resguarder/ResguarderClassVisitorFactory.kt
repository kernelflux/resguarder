package com.kernelflux.plugin.resguarder

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor

abstract class ResguarderClassVisitorFactory :
    AsmClassVisitorFactory<InstrumentationParameters.None> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return ResguarderClassVisitor(nextClassVisitor)
    }

    override fun isInstrumentable(classData: com.android.build.api.instrumentation.ClassData): Boolean {
        val clzName = classData.className.replace("/", ".")
        return clzName.startsWith("com.kernelflux") && !clzName.startsWith("com.kernelflux.resguarder.Resguarder")
    }

}