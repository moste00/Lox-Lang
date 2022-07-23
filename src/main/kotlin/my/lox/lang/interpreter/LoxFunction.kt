package my.lox.lang.interpreter

import my.lox.lang.ast.Function
class LoxFunction(private val declaration : Function,
                  private val closure     : Environment) : LoxCallable {
    override val arity: Int
        get() = declaration.params.size

    override fun call(interpreter: Interpreter, args: Array<Any?>): Any? {
        val funEnv = Environment(closure)
        declaration.params.forEachIndexed { i,param ->
            funEnv.define(param.lexeme,args[i])
        }

        return try {
            interpreter.execFun(declaration.body,funEnv)
            null
        } catch (rv : Return) {
            rv.value
        }
    }

    override fun toString(): String = "lox fn ${declaration.name.lexeme}"

    internal class Return(val value: Any?) : RuntimeException(null, null, false, false)
}