package my.lox.lang.interpreter

interface LoxPrinter {
    fun print(vararg things : String)
}

object DefaultLoxPrinter : LoxPrinter {
    override fun print(vararg things: String) {
        val str = java.lang.StringBuilder()
        things.forEach { str.append(it) }
        println(str.toString())
    }

}