package ru.nsu.fit.alexey.kozyakov

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File

class BytecodeWriter(private val outDir: File) {
    private val classWriter = createClassWtiter()
    private val mainVisitor = createMainVisitor()

    init {
        visitMainStart()
    }

    fun writeCode() {
        visitMainEnd()
        visitMainConstructor()
        outDir.resolve("Main.class").writeBytes(classWriter.toByteArray())
    }

    fun visitOperation(operation: MethodVisitor.() -> Unit) {
        mainVisitor.operation()
    }

    private fun createClassWtiter() = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

    private fun createMainVisitor() = classWriter.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
            "main", "([Ljava/lang/String;)V", null, null);


    private fun visitMainStart() {
        classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "Main",
                null, "java/lang/Object", null)
    }

    private fun visitMainEnd() {
        mainVisitor.visitInsn(Opcodes.RETURN)
        mainVisitor.visitMaxs(0, 0)
        mainVisitor.visitEnd()
    }

    private fun visitMainConstructor() {
        val constructor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                "()V", null, null)
        constructor.visitCode()
        constructor.visitVarInsn(Opcodes.ALOAD, 0)
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false)
        constructor.visitInsn(Opcodes.RETURN)
        constructor.visitMaxs(0, 0)
        constructor.visitEnd()
    }
}
