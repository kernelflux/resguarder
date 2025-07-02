package com.kernelflux.plugin.resguarder

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor

abstract class ResguarderClassVisitorFactory :
    AsmClassVisitorFactory<ResguarderClassVisitorFactory.Params> {

    interface Params : InstrumentationParameters {
        @get:Input
        val bigImageResIds: SetProperty<Int>

        @get:Input
        val resIdTypeMap: MapProperty<Int, String>
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return ResguarderClassVisitor(
            nextClassVisitor,
            parameters.get().resIdTypeMap.get(),
            parameters.get().bigImageResIds.get()
        )
    }

    override fun isInstrumentable(classData: com.android.build.api.instrumentation.ClassData): Boolean =
        true
}