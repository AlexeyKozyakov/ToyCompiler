package ru.nsu.fit.alexey.kozyakov

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.parboiled.Node
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

private const val RESERVED_VARS = 1

class Compiler(private val parser: Parser,
               private val bytecodeWriter: BytecodeWriter) {

    private val localVariables = ArrayDeque<HashMap<String, Variable>>()

    fun compile() {
        val parseTreeRoot = parser.parse()
        if (parseTreeRoot == null || parseTreeRoot.hasError()) {
            fail(parseTreeRoot, "Syntax error")
        }
        compileProgram(parseTreeRoot)
        bytecodeWriter.writeCode()
    }

    private fun findVariable(name: String)
            = localVariables.find { it.containsKey(name) }?.get(name)


    private fun declareVariable(name: String, type: VarType): Variable? {
        val totalVarCount = localVariables.sumBy { it.size }
        val blockVariables = localVariables.first
        if (name !in blockVariables) {
            val varNumber = totalVarCount + RESERVED_VARS
            val variable = Variable(name, type, varNumber)
            blockVariables[name] = variable
            return variable
        }
        return null
    }

    private fun compileProgram(root: Node<Any>) {
        localVariables.addFirst(hashMapOf())
        root.children.forEach {
            compileStatement(it.children[0])
        }
        localVariables.removeFirst()
    }

    private fun compileStatement(root: Node<Any>) {
        val child = root.children[1].children[0]
        when(child.label) {
            "Assign" -> compileAssign(child)
            "If" -> compileIf(child)
            "While" -> compileWhile(child)
            "Print" -> compilePrint(child)
        }
    }

    private fun compileAssign(root: Node<Any>) {
        val varKeyword = root.children[0]
        val variable = root.children[1]
        val expression = root.children[3]
        val exprType = compileExpr(expression)
        val variableName = variable.data
        val varToAssign = if (varKeyword.present) {
            declareVariable(variableName, exprType)
                    ?: fail(root, "Variable already exists")
        } else {
            findVariable(variableName)
                    ?: fail(root, "Variable is not declared")
        }
        if (varToAssign.type != exprType) {
            fail(root, "Incompatible expression and variable types")
        }
        with(bytecodeWriter) {
            when (exprType) {
                VarType.INT, VarType.CHAR, VarType.BOOL -> visitOperation(storeInt(varToAssign.index))
                VarType.DOUBLE -> visitOperation(storeDouble(varToAssign.index))
                VarType.STRING -> visitOperation(storeReference(varToAssign.index))
            }
        }
    }

    private fun compileIf(root: Node<Any>) {
        val exprType = compileExpr(root.children[1])
        if (exprType != VarType.BOOL) {
            fail(root, "If operator requires boolean expression")
        }
        with(bytecodeWriter) {
            val failLabel = Label()
            visitOperation(goToIfFalse(failLabel))
            compileProgram(root.children[2].children[1])
            visitOperation(label(failLabel))
        }
    }

    private fun compileWhile(root: Node<Any>) {
        val condStart = Label()
        val loopStart = Label()
        val loopEnd = Label()
        with(bytecodeWriter) {
            visitOperation(label(condStart))
            compileExpr(root.children[1])
            visitOperation(goToIfFalse(loopEnd))
            visitOperation(label(loopStart))
            compileProgram(root.children[2].children[1])
            visitOperation(goTo(condStart))
            visitOperation(label(loopEnd))
        }
    }

    private fun compilePrint(root: Node<Any>) {
        with(bytecodeWriter) {
            visitOperation(loadPrint())
            val type = compileExpr(root.children[1])
            visitOperation(when(type) {
                VarType.INT -> printInt()
                VarType.BOOL -> printBool()
                VarType.STRING -> printString()
                VarType.CHAR -> printChar()
                VarType.DOUBLE -> printDouble()
            })
        }
    }

    private fun compileExpr(root: Node<Any>): VarType {
        val comparePartOptional = root.children[1]
        if (!comparePartOptional.present) {
            return compileSum(root.children[0])
        }
        val comparePart = comparePartOptional.children[0]
        val operator = comparePart.children[0]
        val firstOp = root.children[0]
        val secondOp = comparePart.children[1]
        val firstType = compileSum(firstOp)
        val secondType = compileSum(secondOp)
        if (firstType != secondType) {
            fail(firstOp, "Incompatible types in compare expression")
        }
        when(operator.data) {
            " > " -> compileExprWithComparableOps(firstOp, firstType, intGreater(), doubleGreater())
            " < " -> compileExprWithComparableOps(firstOp, firstType, intLess(), doubleLess())
            " == " -> compileExprWithSameTypeOps(firstType, intEqual(), doubleEqual(), refEqual())
            " != " -> compileExprWithSameTypeOps(firstType, intNotEqual(), doubleNotEqual(), refNotEqual())
            " >= " -> compileExprWithComparableOps(firstOp, firstType, intGreaterOrEqual(), doubleGreaterOrEqual())
            " <= " -> compileExprWithComparableOps(firstOp, firstType, intLessOrEqual(), doubleLessOrEqual())
            else -> fail(root, "Unsupported compare operator")
        }
        return VarType.BOOL
    }

    private fun compileExprWithComparableOps(root: Node<Any>,
                                             type: VarType,
                                             intOp: MethodVisitor.() -> Unit,
                                             doubleOp: MethodVisitor.() -> Unit) {
        with(bytecodeWriter) {
            when(type) {
                VarType.INT, VarType.CHAR -> {
                    visitOperation(intOp)
                }
                VarType.DOUBLE -> {
                    visitOperation(doubleOp)
                }
                else -> fail(root, "Unsupported types in compare operation")
            }
        }
    }

    private fun compileExprWithSameTypeOps(type: VarType,
                                           intOp: MethodVisitor.() -> Unit,
                                           doubleOp: MethodVisitor.() -> Unit,
                                           refOp: MethodVisitor.() -> Unit) {
        with(bytecodeWriter) {
            when(type) {
                VarType.INT, VarType.CHAR, VarType.BOOL -> {
                    visitOperation(intOp)
                }
                VarType.DOUBLE -> {
                    visitOperation(doubleOp)
                }
                VarType.STRING -> {
                    visitOperation(refOp)
                }
            }
        }
    }

    private fun compileSum(root: Node<Any>): VarType {
        val sumPartOptional = root.children[1]
        val resultType = compileProd(root.children[0])
        if (!sumPartOptional.present) {
            return resultType
        }
        sumPartOptional.children.forEach {
            val type = compileProd(it.children[1])
            if (type != resultType) {
                fail(it, "Incompatible types in sum")
            }
            when(it.children[0].data) {
                " + " -> compileAdd(it, type)
                " - " -> compileSub(it, type)
                else -> fail(root, "Unsupported operand")
            }
        }
        return resultType
    }

    private fun compileAdd(root: Node<Any>, type: VarType) {
        with(bytecodeWriter) {
            when(type) {
                VarType.INT -> visitOperation(intAdd())
                VarType.DOUBLE -> visitOperation(doubleAdd())
                else -> fail(root, "Unsupported types in add expression")
            }
        }
    }

    private fun compileSub(root: Node<Any>, type: VarType) {
        with(bytecodeWriter) {
            when(type) {
                VarType.INT -> visitOperation(intSub())
                VarType.DOUBLE -> visitOperation(doubleSub())
                else -> fail(root, "Unsupported types in sub expression")
            }
        }
    }

    private fun compileProd(root: Node<Any>): VarType {
        val prodPartOptional = root.children[1]
        val resultType = compileUnary(root.children[0])
        if (!prodPartOptional.present) {
            return resultType
        }
        prodPartOptional.children.forEach {
            val type = compileUnary(it.children[1])
            if (type != resultType) {
                fail(it, "Incompatible types in prod")
            }
            when(it.children[0].data) {
                " * " -> compileMul(it, type)
                " / " -> compileDiv(it, type)
                else -> fail(root, "Unsupported operand")
            }
        }
        return resultType
    }

    private fun compileMul(root: Node<Any>, type: VarType) {
        with(bytecodeWriter) {
            when(type) {
                VarType.INT -> visitOperation(intMul())
                VarType.DOUBLE -> visitOperation(doubleMul())
                else -> fail(root, "Unsupported types in add expression")
            }
        }
    }

    private fun compileDiv(root: Node<Any>, type: VarType) {
        with(bytecodeWriter) {
            when(type) {
                VarType.INT -> visitOperation(intDiv())
                VarType.DOUBLE -> visitOperation(doubleDiv())
                else -> fail(root, "Unsupported types in sub expression")
            }
        }
    }
    private fun compileUnary(root: Node<Any>): VarType {
        val minusOptional = root.children[0]
        val resultType = compileBrackets(root.children[1])
        if (!minusOptional.present) {
            return resultType
        }
        with(bytecodeWriter) {
            when(resultType) {
                VarType.INT -> visitOperation(intNeg())
                VarType.DOUBLE -> visitOperation(doubleNeg())
                else -> fail(root, "Unsupported type in unary expression")
            }
        }
        return resultType
    }

    private fun compileBrackets(root: Node<Any>): VarType {
        val child = root.children[0]
        return when(child.label) {
            "Literal" -> compileLiteral(child)
            "Var" -> compileVar(child)
            "Sequence" -> compileExpr(child.children[1])
            else -> fail(root, "Unexpected expression in brackets")
        }
    }

    private fun compileVar(root: Node<Any>): VarType {
        findVariable(root.data)?.let {
            with(bytecodeWriter) {
                when(it.type) {
                    VarType.INT, VarType.BOOL, VarType.CHAR -> visitOperation(loadIntVar(it.index))
                    VarType.DOUBLE -> visitOperation(loadDoubleVar(it.index))
                    VarType.STRING -> visitOperation(loadRefVar(it.index))
                }
                return it.type
            }
        }
        fail(root, "Variable is not declared")
    }

    private fun compileLiteral(root: Node<Any>): VarType {
        with(bytecodeWriter) {
            val literal = root.children[0]
            return when(literal.label) {
                "IntLiteral" -> {
                    visitOperation(push(literal.data.toInt()))
                    VarType.INT
                }
                "DoubleLiteral" -> {
                    visitOperation(push(literal.data.toDouble()))
                    VarType.DOUBLE
                }
                "StringLiteral" -> {
                    val str = literal.data
                    visitOperation(push(str.substring(1, str.length - 1)))
                    VarType.STRING
                }
                "CharLiteral" -> {
                    visitOperation(push(literal.data[1].toInt()))
                    VarType.CHAR
                }
                "BoolLiteral" -> {
                    when(literal.data) {
                        "true" -> visitOperation(push(true))
                        "false" -> visitOperation(push(false))
                    }
                    VarType.BOOL
                }
                else -> fail(root, "Unsupported literal")
            }
        }
    }

    private val Node<Any>.data
        get() = parser.code.substring(this.startIndex until this.endIndex)

    private val Node<Any>.present
        get() = this.startIndex != this.endIndex

    private data class Variable(val name: String, val type: VarType, val index: Int)

    private fun fail(node: Node<Any>?, desc: String): Nothing = throw CompilerException(node, desc)

    private enum class VarType {
        INT,
        STRING,
        CHAR,
        BOOL,
        DOUBLE
    }

    private inner class CompilerException(node: Node<Any>?, desc: String):
            Exception("Compilation error in pos: ${node?.startIndex ?: ""} (${node?.data ?: ""})\n$desc")

}
