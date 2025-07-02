package com.kernelflux.plugin.resguarder

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ResguarderMethodVisitor(
    mv: MethodVisitor,
    private val resIdTypeMap: Map<Int, String>,
    private val bigImageResIds: Set<Int>
) : MethodVisitor(Opcodes.ASM9, mv) {

    private var lastResId: Int? = null
    private var lastViewVarIndex: Int? = null

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        if (opcode == Opcodes.ALOAD) {
            lastViewVarIndex = `var`
        }
        super.visitVarInsn(opcode, `var`)
    }

    override fun visitLdcInsn(value: Any?) {
        if (value is Int) {
            lastResId = value
        }
        super.visitLdcInsn(value)
    }

    override fun visitMethodInsn(
        opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean
    ) {
       // ResguarderLogger.log("ResguarderPlugin visitMethodInsn owner:${owner},name:$name")
        if ((name == "setImageResource" || name == "setBackgroundResource") && descriptor == "(I)V") {
            lastResId?.also {
                val type = resIdTypeMap[it] ?: "other"
                if (type == "bitmap" && bigImageResIds.contains(it)) {
                    mv.visitVarInsn(Opcodes.ALOAD, lastViewVarIndex?:0)
                    mv.visitLdcInsn(it)
                    mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/kernelflux/resguarder/ResguarderImageLoader",
                        "load",
                        "(Landroid/view/View;I)V",
                        false
                    )
                    lastResId = null
                    return
                }
            }
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
}