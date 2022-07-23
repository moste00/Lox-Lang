package my.lox.lang.misc

import my.lox.lang.ast.*

class PrettyPrinter : ExprVisitor<String> {
    override fun visitAssignExpr(assignExpr: Assign): String {
        TODO("Not yet implemented")
    }

    override fun visitBinaryExpr(binary: Binary): String {
        return "(${binary.operator.lexeme} ${binary.left.accept(this)} ${binary.right.accept(this)})"
    }

    override fun visitLogicalExpr(logicalExpr: Logical): String {
        TODO("Not yet implemented")
    }

    override fun visitGroupingExpr(grouping: Grouping): String {
        return "(${grouping.expression.accept(this)})"
    }

    override fun visitLiteralExpr(literal: Literal): String {
        return literal.value.toString()
    }

    override fun visitUnaryExpr(unary: Unary): String {
        return "(${unary.operator.lexeme} ${unary.right.accept(this)})"
    }

    override fun visitVariableExpr(variable: Variable): String {
        return variable.name.lexeme
    }

    override fun visitCallExpr(callExpr: Call): String {
        TODO("Not yet implemented")
    }
}