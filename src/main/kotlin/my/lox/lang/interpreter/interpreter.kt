package my.lox.lang.interpreter

import my.lox.lang.ast.*
import my.lox.lang.ast.Function
import my.lox.lang.lex.Token
import my.lox.lang.lex.TokenType

class Interpreter : ExprVisitor<Any?> , StmtVisitor<Unit> {
    private val locals = mutableMapOf<Expr,Int>()
    //public interface
    //----------------------------------------------------
    var result  : String        = ""                   //always reflects the last evaluated expression
    var printer : LoxPrinter    = DefaultLoxPrinter    //the calling code of Interpreter can swap with any printer
    var err     : RuntimeError? = null                 //if not null, the program has a runtime error
    val globals : Environment   = Environment(null)
    var env     : Environment   = globals
    fun interpret(program : List<Stmt>) {
        try {
            //We 'forgive' every time we're asked to interpret a program
            //This is for the repl, we can't keep fixating on a single error forever,
            //We will be called repeatedly, so we must forget the errors of the last repl entry each time
            err = null
            program.forEach { execute(it) }
        }
        catch (ex : RuntimeError) {
            err = ex
        }
    }
    fun execFun(funStmts : List<Stmt>, funEnv : Environment) = execBlock(funStmts,funEnv)
    //----------------------------------------------------
    init {
        globals.define("clock", object : LoxCallable {
            override val arity: Int
                get() = 0
            override fun call(interpreter: Interpreter, args: Array<Any?>): Any?
                                            = System.currentTimeMillis()/1000.0
            override fun toString(): String = "<native fn clock>"
        })
    }

    override fun visitExpressionStmt(expressionStmt: Expression) {
        result = loxValToStr(eval(expressionStmt.expression))
    }

    override fun visitPrintStmt(printStmt: Print) {
        result = loxValToStr(eval(printStmt.expression))
        printer.print(result)
    }

    override fun visitVarStmt(varStmt: Var) {
        var init : Any? = null
        if (varStmt.initializer != null) {
            init = eval(varStmt.initializer)
            result = loxValToStr(init)
        }
        env.define(varStmt.name.lexeme,init)
    }

    override fun visitBlockStmt(blockStmt: Block) {
        execBlock(blockStmt.statements, Environment(env))
    }

    override fun visitIfStmt(ifStmt: If) {
        val condResult = eval(ifStmt.condition)
        result = loxValToStr(condResult)

        if (truthy(condResult)) execute(ifStmt.thenBranch)
        else if (ifStmt.elseBranch != null) execute(ifStmt.elseBranch)
    }

    override fun visitWhileStmt(whileStmt: While) {
        var condResult = eval(whileStmt.condition)
        result = loxValToStr(condResult)

        while (truthy(condResult)) {
            execute(whileStmt.body)
            condResult = eval(whileStmt.condition)
            result = loxValToStr(condResult)
        }
    }

    override fun visitFunctionStmt(functionStmt: Function) {
        val loxFun = LoxFunction(functionStmt,this.env)
        env.define(functionStmt.name.lexeme,loxFun)
    }

    override fun visitReturnStmt(returnStmt: Return) {
        var retVal : Any? = null
        if (returnStmt.value != null) {
            retVal = eval(returnStmt.value)
            result = loxValToStr(retVal)
        }
        throw LoxFunction.Return(retVal)
    }

    override fun visitClassStmt(classStmt: Class) {
        TODO("Not yet implemented")
    }

    private fun execBlock(ss : List<Stmt>, env : Environment) {
        val prev = this.env
        try {
            this.env = env
            ss.forEach {
                execute(it)
            }
        } finally {
            this.env = prev
        }
    }

    override fun visitAssignExpr(assignExpr: Assign): Any? {
        val v = eval(assignExpr.value)

        val distance = locals[assignExpr]
        if (distance != null){
            env.assign(distance,assignExpr.name,v)
        }
        else globals.assign(assignExpr.name,v)

        return v
    }

    override fun visitBinaryExpr(binaryExpr: Binary): Any? {
        val v1 = eval(binaryExpr.left)
        val v2 = eval(binaryExpr.right)

        return when (binaryExpr.operator.type) {
            TokenType.PLUS  -> {
                when {
                    v1 is Double && v2 is Double -> v1 + v2
                    v1 is String && v2 is String -> v1 + v2
                    v1 is String || v2 is String -> "$v1$v2"

                    else -> throw RuntimeError(binaryExpr.operator,
                                       "Lox Interpreter : You can only add numbers and strings")
                }
            }
            TokenType.MINUS -> {
                when {
                    v1 is Double && v2 is Double -> v1 - v2

                    else -> throw RuntimeError(binaryExpr.operator,
                                       "Lox Interpreter : Can't subtract non-number objects")
                }
            }
            TokenType.STAR  -> {
                when {
                    v1 is Double && v2 is Double -> v1 * v2

                    else -> throw RuntimeError(binaryExpr.operator,
                                       "Lox Interpreter : Can't multiply non-number objects")
                }
            }
            TokenType.SLASH -> {
                when {
                    v1 is Double && v2 is Double -> v1/v2

                    else -> throw RuntimeError(binaryExpr.operator,
                                       "Lox Interpreter : Can't divide non-number objects")
                }
            }
            TokenType.GREATER -> {
                when {
                    v1 is Double && v2 is Double -> v1 > v2

                    else -> throw RuntimeError(binaryExpr.operator,
                                       "Lox Interpreter : Can't compare non-number objects with >")
                }
            }
            TokenType.GREATER_EQUAL -> {
                when {
                    v1 is Double && v2 is Double -> v1 >= v2

                    else -> throw RuntimeError(binaryExpr.operator,
                                       "Lox Interpreter : Can't compare non-number objects with >=")
                }
            }
            TokenType.LESS -> {
                when {
                    v1 is Double && v2 is Double -> v1 < v2

                    else -> throw RuntimeError(binaryExpr.operator,
                                       "Lox Interpreter : Can't compare non-number objects with <")
                }
            }
            TokenType.LESS_EQUAL  -> {
                when {
                    v1 is Double && v2 is Double -> v1 <= v2

                    else -> throw RuntimeError(binaryExpr.operator,
                                       "Lox Interpreter : Can't compare non-number objects with <=")
                }
            }
            TokenType.EQUAL_EQUAL ->  isEqual(v1,v2)
            TokenType.BANG_EQUAL  -> !isEqual(v1,v2)

            else -> throw RuntimeError(binaryExpr.operator,
                               "Lox Interpreter : Unrecognized Binary Operator ${binaryExpr.operator.lexeme}")
        }
    }

    override fun visitLogicalExpr(logicalExpr: Logical): Any? {
        val leftResult = eval(logicalExpr.left)

        //An Or with a truthy left-hand side short-circuits the right-hand side
        //Same as an And with a falsey left-hand side
        if ( (logicalExpr.operator.type == TokenType.OR  && truthy(leftResult))
           ||(logicalExpr.operator.type == TokenType.AND &&!truthy(leftResult))) {
               return leftResult
            }
        //else, both of them return the right-hand side
        return eval(logicalExpr.right)
    }

    override fun visitGroupingExpr(groupingExpr: Grouping): Any? = eval(groupingExpr.expression)

    override fun visitLiteralExpr(literalExpr: Literal): Any?    = literalExpr.value

    override fun visitUnaryExpr(unaryExpr: Unary): Any? {
        val value = eval(unaryExpr.right)

        return when (unaryExpr.operator.type) {
            TokenType.MINUS -> -(value as Double)
            TokenType.BANG -> !truthy(value)
            else -> throw RuntimeError(unaryExpr.operator,
                               "Lox Interpreter : Unrecognized Unary Operator ${unaryExpr.operator.lexeme}")
        }
    }

    override fun visitVariableExpr(variableExpr: Variable): Any? {
        return lookUpVar(variableExpr,variableExpr.name)
    }

    override fun visitCallExpr(callExpr: Call): Any? {
        val callee = eval(callExpr.callee)

        if (callee is LoxCallable) {
            if (callee.arity == callExpr.arguments.size) {
                val evaledArgs = Array(callExpr.arguments.size) {
                                     eval(callExpr.arguments[it])
                                 }
                return callee.call(this, evaledArgs)
            }
            else throw RuntimeError(callExpr.paren,"Lox Interpreter : Callable expected ${callee.arity} args but got ${callExpr.arguments.size}")
        }
        else throw RuntimeError(callExpr.paren,"Lox Interpreter : Can't call a non-callable expression")
    }

    class RuntimeError(val token: Token, message: String?) : RuntimeException(message)

    private fun execute(s : Stmt) = s.accept(this)
    private fun eval(e : Expr) = e.accept(this)
    private fun truthy(e : Any?) = when (e) {
        null -> false
        is Boolean -> e
        else -> true
    }
    private fun isEqual(e1 : Any?, e2 : Any?) : Boolean {
        if (e1 == null && e2 == null) return true
        if (e1 == null || e2 == null) return false

        return e1 == e2
    }
    private fun loxValToStr(v : Any?) : String {
        if (v == null) return "nil"

        if (v is Double) {
            val vText = v.toString()
            return if (vText.endsWith(".0")) vText.substring(0 ..vText.length - 3 )
            else vText
        }
        return v.toString()
    }

    fun resolveExpr(e: Expr, i: Int) {
        locals[e] = i
    }
    private fun lookUpVar(e: Expr, name: Token) : Any? {
        val distance = locals[e]
        return if (distance != null) env.get(distance,name.lexeme)
        else globals.get(name)
    }
}