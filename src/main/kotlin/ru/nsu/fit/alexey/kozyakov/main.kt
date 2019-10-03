package ru.nsu.fit.alexey.kozyakov

import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        return
    }
    val codeFile = File(args[0])
    if (!codeFile.exists() || !codeFile.canRead()) {
        println("Cannot open input file")
        printUsage()
        return
    }
    val outDir = if (args.size < 2) codeFile.parentFile else File(args[1])
    if (!outDir.exists()) {
        val created = outDir.createNewFile()
        if (!created) {
            println("Cannot create or find output dir")
            printUsage()
            return
        }
    }
    val parser = Parser(codeFile)
    val codeGenerator = BytecodeWriter(outDir)
    val compiler = Compiler(parser, codeGenerator)
    compiler.compile()
}

private fun printUsage() {
    println("Usage: java <mainClass> <inputFile> <outDir>")
}
