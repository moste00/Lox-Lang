package my.lox.lang.semantica

import my.lox.lang.ast.*
import my.lox.lang.ast.Function
import my.lox.lang.interpreter.Interpreter
import my.lox.lang.lex.Token
import java.util.*


class SemanticAnalyzer(private val interpreter : Interpreter) : ExprVisitor<Unit>,StmtVisitor<Unit> {
    private enum class IdentState {DECLARED,DEFINED}
    private val scopes = Stack<MutableMap<String,IdentState>>()

    private val internalErrors = mutableListOf<String>()

    private enum class FunctionType { NONE, FUNCTION }
    private var currentFunction: FunctionType = FunctionType.NONE


    val errs : List<String> = internalErrors

    override fun visitAssignExpr(assignExpr: Assign) {
        analyze(assignExpr.value)
        resolveIdentifier(assignExpr,assignExpr.name)
    }

    override fun visitBinaryExpr(binaryExpr: Binary) {
        analyze(binaryExpr.left)
        analyze(binaryExpr.right)
    }

    override fun visitLogicalExpr(logicalExpr: Logical) {
        analyze(logicalExpr.left)
        analyze(logicalExpr.right)
    }

    override fun visitGroupingExpr(groupingExpr: Grouping) = analyze(groupingExpr.expression)

    override fun visitLiteralExpr(literalExpr: Literal) {
    }

    override fun visitUnaryExpr(unaryExpr: Unary) = analyze(unaryExpr.right)

    override fun visitVariableExpr(variableExpr: Variable) {
        if (!scopes.empty() && scopes.peek()[variableExpr.name.lexeme] == IdentState.DECLARED) {
            internalErrors.add("Lox Analysis : Can't reference a local variable inside its own initializer expression at line ${variableExpr.name.pos}")
        }
        resolveIdentifier(variableExpr,variableExpr.name)
    }

    override fun visitCallExpr(callExpr: Call) {
        analyze(callExpr.callee)
        callExpr.arguments.forEach{ analyze(it) }
    }

    override fun visitExpressionStmt(expressionStmt: Expression) {
        analyze(expressionStmt.expression)
    }

    override fun visitPrintStmt(printStmt: Print) {
        analyze(printStmt.expression)
    }

    override fun visitVarStmt(varStmt: Var) {
        declare(varStmt.name)
        if (varStmt.initializer != null) {
            analyze(varStmt.initializer)
        }
        define(varStmt.name)
    }

    override fun visitBlockStmt(blockStmt: Block) {
        createScope()
        analyze(blockStmt.statements)
        destroyScope()
    }

    override fun visitIfStmt(ifStmt: If) {
        analyze(ifStmt.condition)
        analyze(ifStmt.thenBranch)
        if (ifStmt.elseBranch != null) analyze(ifStmt.elseBranch)
    }

    override fun visitWhileStmt(whileStmt: While) {
        analyze(whileStmt.condition)
        analyze(whileStmt.body)
    }

    override fun visitFunctionStmt(functionStmt: Function) {
        declare(functionStmt.name)
        define(functionStmt.name)
        analyzeFunction(functionStmt,FunctionType.FUNCTION)
    }

    override fun visitReturnStmt(returnStmt: Return) {
        if (currentFunction == FunctionType.NONE) internalErrors.add("Lox Analyzer : Return statement at the top-level")
        if (returnStmt.value != null) analyze(returnStmt.value)
    }

    override fun visitClassStmt(classStmt: Class) {
        TODO("Not yet implemented")
    }

    //public interface
    //------------------------------------------------------------
    fun analyze(ss: List<Stmt>) = ss.forEach{ analyze(it) }
    //------------------------------------------------------------

    private fun analyze(s : Stmt) = s.accept(this)
    private fun analyze(e : Expr) = e.accept(this)
    private fun createScope() = scopes.push(mutableMapOf())
    private fun destroyScope()= scopes.pop()
    private fun declare(t : Token) {
        if (scopes.empty()) return

        val scope = scopes.peek()
        if (scope.containsKey(t.lexeme)) internalErrors.add("Lox Analyzer : variable ${t.lexeme} is already declared in current scope, variable redeclaration is only allowed in global scope")

        scope[t.lexeme] = IdentState.DECLARED
    }
    private fun define(t : Token) {
        if (scopes.empty()) return
        scopes.peek()[t.lexeme] = IdentState.DEFINED
    }

    private fun resolveIdentifier(e : Expr, t : Token) {
        for (i in scopes.size - 1 downTo 0) {
            if (scopes[i].containsKey(t.lexeme)) {
                interpreter.resolveExpr(e,scopes.size - 1 - i)
            }
        }
    }

    private fun analyzeFunction(f : Function, ft : FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = ft

        createScope()
        f.params.forEach{ declare(it) ; define(it) }
        analyze(f.body)
        destroyScope()
        currentFunction = enclosingFunction
    }
}