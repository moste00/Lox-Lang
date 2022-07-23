package my.lox.lang.ast

import my.lox.lang.lex.Token

interface Stmt {
    fun<T> accept(v : StmtVisitor<T>) : T
}

data class Expression(
					val expression : Expr
					) : Stmt {
    override fun<T> accept(v : StmtVisitor<T>) : T = v.visitExpressionStmt(this)
}

data class Print(
					val expression : Expr
					) : Stmt {
    override fun<T> accept(v : StmtVisitor<T>) : T = v.visitPrintStmt(this)
}

data class Var(
					val name : Token,
					val initializer : Expr?
					) : Stmt {
    override fun<T> accept(v : StmtVisitor<T>) : T = v.visitVarStmt(this)
}

data class Block(
					val statements : List<Stmt>
					) : Stmt {
    override fun<T> accept(v : StmtVisitor<T>) : T = v.visitBlockStmt(this)
}

data class If(
					val condition : Expr,
					val thenBranch : Stmt,
					val elseBranch : Stmt?
					) : Stmt {
    override fun<T> accept(v : StmtVisitor<T>) : T = v.visitIfStmt(this)
}

data class While(
					val condition : Expr,
					val body : Stmt
					) : Stmt {
    override fun<T> accept(v : StmtVisitor<T>) : T = v.visitWhileStmt(this)
}

data class Function(
					val name : Token,
					val params : List<Token>,
					val body : List<Stmt>
					) : Stmt {
    override fun<T> accept(v : StmtVisitor<T>) : T = v.visitFunctionStmt(this)
}

data class Return(
					val keyword : Token,
					val value : Expr?
					) : Stmt {
    override fun<T> accept(v : StmtVisitor<T>) : T = v.visitReturnStmt(this)
}

data class Class(
					val name : Token,
					val methods : List<Function>
					) : Stmt {
    override fun<T> accept(v : StmtVisitor<T>) : T = v.visitClassStmt(this)
}

