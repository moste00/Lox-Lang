package my.lox.lang.entry

import my.lox.lang.interpreter.Interpreter
import my.lox.lang.lex.Lexer
import my.lox.lang.parser.Parser
import my.lox.lang.semantica.SemanticAnalyzer
import java.nio.file.InvalidPathException
import java.nio.file.Paths

object Lox {
    @JvmStatic
    val int = Interpreter()

    @JvmStatic
    fun runFile(path : String) {
        try {
            val source = Paths.get(path).toUri().toURL().readText(Charsets.US_ASCII)
            run(source)
        } catch (_: InvalidPathException) {
            println("Lox couldn't find $path :(\nGoodBye")
        }
    }
    @JvmStatic
    fun runPrompt() {
        while (true) {
            print("Lox :>")

            try {
                val input = readln()
                run(input)
                if (int.err == null) println(int.result)
            } catch (_: java.lang.RuntimeException) {
                println("Enjoyed? :)\nGoodBye")
                return
            }
        }
    }
    @JvmStatic
    private fun run(source : String) {
        val scanner = Lexer(source)

        if (scanner.errs.isEmpty()) {
            val parser = Parser(scanner.tokens)

            if (parser.errs.isEmpty()) {
                val analyzer = SemanticAnalyzer(int)
                analyzer.analyze(parser.ast)

                if (analyzer.errs.isEmpty()) {
                    int.interpret(parser.ast)
                    if (int.err != null) {
                        println("Runtime Error :(")
                        println(int.err)
                    }
                } else {
                    println("Semantic Analysis Errors :(")
                    analyzer.errs.forEach{ println(it) }
                }
            } else {
                println("Parsing Errors :(")
                parser.errs.forEach { println(it) }
            }
        } else {
            println("Lexing Error :(")
            scanner.errs.forEach { println(it) }
        }
    }
}