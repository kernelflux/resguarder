package com.kernelflux.plugin.resguarder


import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ResguarderClassVisitor(
    cv: ClassVisitor,
    private val resIdTypeMap: Map<Int, String>,
    private val bigImageResIds: Set<Int>
) : ClassVisitor(Opcodes.ASM9, cv) {
    override fun visitMethod(
        access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, desc, signature, exceptions)
        return ResguarderMethodVisitor(mv, resIdTypeMap, bigImageResIds)
    }
}