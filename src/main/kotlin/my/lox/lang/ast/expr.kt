package my.lox.lang.ast

import my.lox.lang.lex.Token

interface Expr {
    fun<T> accept(v : ExprVisitor<T>) : T
}

data class Assign(
					val name : Token,
					val value : Expr
					) : Expr {
    override fun<T> accept(v : ExprVisitor<T>) : T = v.visitAssignExpr(this)
}

data class Binary(
					val left : Expr,
					val operator : Token,
					val right : Expr
					) : Expr {
    override fun<T> accept(v : ExprVisitor<T>) : T = v.visitBinaryExpr(this)
}

data class Logical(
					val left : Expr,
					val operator : Token,
					val right : Expr
					) : Expr {
    override fun<T> accept(v : ExprVisitor<T>) : T = v.visitLogicalExpr(this)
}

data class Grouping(
					val expression : Expr
					) : Expr {
    override fun<T> accept(v : ExprVisitor<T>) : T = v.visitGroupingExpr(this)
}

data class Literal(
					val value : Any?
					) : Expr {
    override fun<T> accept(v : ExprVisitor<T>) : T = v.visitLiteralExpr(this)
}

data class Unary(
					val operator : Token,
					val right : Expr
					) : Expr {
    override fun<T> accept(v : ExprVisitor<T>) : T = v.visitUnaryExpr(this)
}

data class Variable(
					val name : Token
					) : Expr {
    override fun<T> accept(v : ExprVisitor<T>) : T = v.visitVariableExpr(this)
}

data class Call(
					val callee : Expr,
					val paren : Token,
					val arguments : List<Expr>
					) : Expr {
    override fun<T> accept(v : ExprVisitor<T>) : T = v.visitCallExpr(this)
}

