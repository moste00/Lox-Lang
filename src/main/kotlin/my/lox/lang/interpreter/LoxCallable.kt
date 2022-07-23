package my.lox.lang.interpreter

interface LoxCallable {
    val arity : Int
    fun call(interpreter : Interpreter,args : Array<Any?>) : Any?
    override fun toString() : String
}
