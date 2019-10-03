package ru.nsu.fit.alexey.kozyakov

import org.parboiled.BaseParser
import org.parboiled.Parboiled
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.parserunners.ReportingParseRunner
import java.io.File

class Parser(codeFile: File) {

    val code = codeFile.readText()

    private val parser = Parboiled.createParser(ParseTreeBuilder::class.java)
    private val runner = ReportingParseRunner<Any>(parser.Program())

    fun parse() = runner.run(code).parseTreeRoot

    @BuildParseTree
    open class ParseTreeBuilder: BaseParser<Any>() {

        open fun Program(): Rule = OneOrMore(Sequence(Statement(), '\n'))

        open fun Statement(): Rule = Sequence(ZeroOrMore(Whitepace()), FirstOf(Assign(), If(), While(), Print()))

        open fun Assign(): Rule = Sequence(Optional("var "), Var(), " = ", Expr())

        open fun If(): Rule = Sequence("if ", Expr(), Block())

        open fun While(): Rule = Sequence("while ", Expr(), Block())

        open fun Print(): Rule = Sequence("print ", Expr())

        open fun Block(): Rule = Sequence(" {\n", Program(), ZeroOrMore(Whitepace()), "}")

        open fun Expr(): Rule = Sequence(Sum(), Optional(Sequence(FirstOf(" < ", " > ", " == ", " != ", " >= ", " <= "), Sum())))

        open fun Sum(): Rule = Sequence(Prod(), ZeroOrMore(Sequence(FirstOf(" + ", " - "), Prod())))

        open fun Prod(): Rule = Sequence(Unary(), ZeroOrMore(Sequence(FirstOf(" * ", " / "), Unary())))

        open fun Unary(): Rule = Sequence(Optional('-'), Brackets())

        open fun Brackets(): Rule = FirstOf(Literal(), Var(), Sequence('(',  Expr(), ')'))

        open fun Var(): Rule = Sequence(Letter(), ZeroOrMore(FirstOf(Letter(), Digit(), '_')))

        open fun Literal(): Rule = FirstOf(DoubleLiteral(), IntLiteral(), StringLiteral(), CharLiteral(), BoolLiteral())

        open fun IntLiteral(): Rule = Sequence(Optional('-'), OneOrMore(Digit()))

        open fun DoubleLiteral(): Rule = Sequence(Optional('-'), ZeroOrMore(Digit()), '.', OneOrMore(Digit()))

        open fun StringLiteral(): Rule = Sequence('"', ZeroOrMore(NoneOf("\"")), '"')

        open fun CharLiteral(): Rule = Sequence('\'', NoneOf("\'"), '\'')

        open fun BoolLiteral(): Rule = FirstOf("true", "false")

        open fun Digit() = CharRange('0', '9')

        open fun Letter() = FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'))

        open fun Whitepace() = AnyOf(" \t")
    }
}