package my.lox.lang.ast

interface StmtVisitor<T> {
	fun visitExpressionStmt(expressionStmt : Expression) : T
	fun visitPrintStmt(printStmt : Print) : T
	fun visitVarStmt(varStmt : Var) : T
	fun visitBlockStmt(blockStmt : Block) : T
	fun visitIfStmt(ifStmt : If) : T
	fun visitWhileStmt(whileStmt : While) : T
	fun visitFunctionStmt(functionStmt : Function) : T
	fun visitReturnStmt(returnStmt : Return) : T
	fun visitClassStmt(classStmt : Class) : T
}
