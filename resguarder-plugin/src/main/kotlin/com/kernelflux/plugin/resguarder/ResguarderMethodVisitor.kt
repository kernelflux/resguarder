package com.kernelflux.plugin.resguarder

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ResguarderMethodVisitor(
    mv: MethodVisitor
) : MethodVisitor(Opcodes.ASM9, mv) {

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        ResguarderLogger.log("ResguarderPlugin visitMethodInsn owner:${owner},name:$name,descriptor:$descriptor")
        if (name == "setImageResource" && descriptor == "(I)V") {
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/kernelflux/resguarder/Resguarder",
                "loadImageResource",
                "(Landroid/widget/ImageView;I)V",
                false
            )
            return
        } else if (name == "setBackgroundResource" && descriptor == "(I)V") {
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/kernelflux/resguarder/Resguarder",
                "loadBackgroundResource",
                "(Landroid/view/View;I)V",
                false
            )
            return
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
}