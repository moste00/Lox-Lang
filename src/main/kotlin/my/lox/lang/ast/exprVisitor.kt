package my.lox.lang.ast

interface ExprVisitor<T> {
	fun visitAssignExpr(assignExpr : Assign) : T
	fun visitBinaryExpr(binaryExpr : Binary) : T
	fun visitLogicalExpr(logicalExpr : Logical) : T
	fun visitGroupingExpr(groupingExpr : Grouping) : T
	fun visitLiteralExpr(literalExpr : Literal) : T
	fun visitUnaryExpr(unaryExpr : Unary) : T
	fun visitVariableExpr(variableExpr : Variable) : T
	fun visitCallExpr(callExpr : Call) : T
}
