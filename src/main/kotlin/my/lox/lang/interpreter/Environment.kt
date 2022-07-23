package my.lox.lang.interpreter

import my.lox.lang.interpreter.Interpreter.RuntimeError
import my.lox.lang.lex.Token


class Environment(private val enclosing : Environment?) {
    private val vals = mutableMapOf<String,Any?>()

    fun define(name : String, value : Any?) = vals.put(name,value)

    fun get(vaar : Token) : Any? = when (vaar.lexeme in vals) {
        true -> vals[vaar.lexeme]
        false -> if (enclosing != null) enclosing.get(vaar)
                 else throw RuntimeError(vaar,"Lox Interpreter : Undefined variable ${vaar.lexeme} at line ${vaar.pos}")
    }
    fun get(i : Int,name : String) = nthAncestor(i).vals[name]

    fun assign(name: Token, value: Any?) {
        if (name.lexeme in vals) {
            vals[name.lexeme] = value
        }
        else {
            if (enclosing != null) {
                enclosing.assign(name,value)
            }
            else {
                throw RuntimeError(
                    name,
                    "Lox interpreter Undefined variable '" + name.lexeme + "' at line ${name.pos}")
            }
        }
    }

    fun assign(i : Int,name : Token, value : Any?) {
        nthAncestor(i).vals[name.lexeme] = value
    }

    private fun nthAncestor(n : Int) : Environment {
        var ancestor = this
        for (i in 0 until n) ancestor = ancestor.enclosing!!
        return ancestor
    }
}