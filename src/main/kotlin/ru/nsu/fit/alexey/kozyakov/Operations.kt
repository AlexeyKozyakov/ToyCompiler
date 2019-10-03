package ru.nsu.fit.alexey.kozyakov

import jdk.internal.org.objectweb.asm.Opcodes
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor

fun intEqual(): MethodVisitor.() -> Unit = {
    compare(Opcodes.IF_ICMPEQ)
}

fun intNotEqual(): MethodVisitor.() -> Unit = {
    compare(Opcodes.IF_ICMPNE)
}

fun intGreater(): MethodVisitor.() -> Unit = {
    compare(Opcodes.IF_ICMPGT)
}

fun intLess(): MethodVisitor.() -> Unit = {
    compare(Opcodes.IF_ICMPLT)
}

fun intGreaterOrEqual(): MethodVisitor.() -> Unit = {
    compare(Opcodes.IF_ICMPGE)
}

fun intLessOrEqual(): MethodVisitor.() -> Unit = {
    compare(Opcodes.IF_ICMPLE)
}

fun intAdd(): MethodVisitor.() -> Unit = {
    visitInsn(Opcodes.IADD)
}

fun intSub(): MethodVisitor.() -> Unit = {
    visitInsn(Opcodes.ISUB)
}

fun intMul(): MethodVisitor.() -> Unit = {
    visitInsn(Opcodes.IMUL)
}

fun intDiv(): MethodVisitor.() -> Unit = {
    visitInsn(Opcodes.IDIV)
}

fun intNeg(): MethodVisitor.() -> Unit = {
    visitInsn(Opcodes.INEG)
}

fun doubleEqual(): MethodVisitor.() -> Unit = {
    compareDouble(Opcodes.IF_ICMPEQ, 0)
}

fun doubleNotEqual(): MethodVisitor.() -> Unit = {
    compareDouble(Opcodes.IF_ICMPNE, 0)
}

fun doubleGreater(): MethodVisitor.() -> Unit = {
    compareDouble(Opcodes.IF_ICMPEQ, 1)
}

fun doubleLess(): MethodVisitor.() -> Unit = {
    compareDouble(Opcodes.IF_ICMPEQ, -1)
}

fun doubleGreaterOrEqual(): MethodVisitor.() -> Unit = {
    compareDouble(Opcodes.IF_ICMPGE, 0)
}

fun doubleLessOrEqual(): MethodVisitor.() -> Unit = {
    compareDouble(Opcodes.IF_ICMPLE, 0)
}

fun MethodVisitor.compareDouble(opcode: Int, target: Int) {
    visitInsn(Opcodes.DCMPG)
    visitIntInsn(Opcodes.BIPUSH, target)
    compare(opcode)
}

fun MethodVisitor.compare(opcode: Int) {
    val successLabel = Label()
    val failureLabel = Label()
    val endLabel = Label()
    compareImpl(opcode, successLabel, failureLabel, endLabel)
}

fun MethodVisitor.compareImpl(opcode: Int, successLabel: Label, failureLabel: Label, endLabel: Label) {
    visitJumpInsn(opcode, successLabel)
    visitJumpInsn(Opcodes.GOTO, failureLabel)
    visitLabel(successLabel)
    visitIntInsn(Opcodes.BIPUSH, 1)
    visitJumpInsn(Opcodes.GOTO, endLabel)
    visitLabel(failureLabel)
    visitIntInsn(Opcodes.BIPUSH, 0)
    visitLabel(endLabel)
}

fun doubleAdd(): MethodVisitor.() -> Unit = {
    visitInsn(Opcodes.DADD)
}

fun doubleSub(): MethodVisitor.() -> Unit = {
    visitInsn(Opcodes.DSUB)
}

fun doubleMul(): MethodVisitor.() -> Unit = {
    visitInsn(Opcodes.DMUL)
}

fun doubleDiv(): MethodVisitor.() -> Unit = {
    visitInsn(Opcodes.DDIV)
}

fun doubleNeg(): MethodVisitor.() -> Unit = {
    visitInsn(Opcodes.DNEG)
}

fun refEqual(): MethodVisitor.() -> Unit = {
    compare(Opcodes.IF_ACMPEQ)
}

fun refNotEqual(): MethodVisitor.() -> Unit = {
    compare(Opcodes.IF_ACMPNE)
}

fun push(value: Any): MethodVisitor.() -> Unit = {
    visitLdcInsn(value)
}

fun storeInt(variable: Int): MethodVisitor.() -> Unit = {
    visitVarInsn(Opcodes.ISTORE, variable)
}

fun storeDouble(variable: Int): MethodVisitor.() -> Unit = {
    visitVarInsn(Opcodes.DSTORE, variable)
}

fun storeReference(variable: Int): MethodVisitor.() -> Unit = {
    visitVarInsn(Opcodes.ASTORE, variable)
}

fun loadIntVar(variable: Int): MethodVisitor.() -> Unit = {
    visitVarInsn(Opcodes.ILOAD, variable)
}

fun loadDoubleVar(variable: Int): MethodVisitor.() -> Unit = {
    visitVarInsn(Opcodes.DLOAD, variable)
}

fun loadRefVar(variable: Int): MethodVisitor.() -> Unit = {
    visitVarInsn(Opcodes.ALOAD, variable)
}

fun goToIfFalse(label: Label): MethodVisitor.() -> Unit = {
    visitIntInsn(Opcodes.BIPUSH, 0)
    visitJumpInsn(Opcodes.IF_ICMPEQ, label)
}

fun goTo(label: Label): MethodVisitor.() -> Unit = {
    visitJumpInsn(Opcodes.GOTO, label)
}

fun label(label: Label): MethodVisitor.() -> Unit = {
    visitLabel(label)
}

fun loadPrint(): MethodVisitor.() -> Unit = {
    visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
}

fun printInt() = printCall("(I)")

fun printChar() = printCall("(C)")

fun printDouble() = printCall("(D)")

fun printString() = printCall("(Ljava/lang/String;)")

fun printBool() = printCall("(Z)")

fun printCall(type: String): MethodVisitor.() -> Unit = {
    visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "${type}V", false)
}