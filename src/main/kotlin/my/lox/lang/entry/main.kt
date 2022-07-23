package my.lox.lang.entry

import kotlin.system.exitProcess

fun main(args : Array<String>) {
    when {
        args.size > 1 -> {
            println("Usage : jlox [script]")
            exitProcess(64)
        }
        args.size == 1 -> {
            Lox.runFile(args[0])
        }
        else -> {
            Lox.runPrompt()
        }
    }
}