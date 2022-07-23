package my.lox.tools

import java.io.File
import java.util.*


const val astRoot = "src/main/kotlin/my/lox/lang/ast/"
const val exprPath = "${astRoot}expr.kt"
const val exprVisitorPath = "${astRoot}exprVisitor.kt"
const val stmtPath = "${astRoot}stmt.kt"
const val stmtVisitorPath = "${astRoot}stmtVisitor.kt"
const val exprBase = "Expr"
val exprTypes = listOf(
        "Assign   : Token name, Expr value",
        "Binary   : Expr left, Token operator, Expr right",
        "Logical  : Expr left, Token operator, Expr right",
        "Grouping : Expr expression",
        "Literal  : Any? value",
        "Unary    : Token operator, Expr right",
        "Variable : Token name",
        "Call     : Expr callee, Token paren, List<Expr> arguments"
)
const val stmtBase = "Stmt"
val stmtTypes = listOf(
        "Expression : Expr expression",
        "Print      : Expr expression",
        "Var        : Token name, Expr? initializer",
        "Block      : List<Stmt> statements",
        "If         : Expr condition, Stmt thenBranch, Stmt? elseBranch",
        "While      : Expr condition, Stmt body",
        "Function   : Token name, List<Token> params, List<Stmt> body",
        "Return     : Token keyword, Expr? value",
        "Class      : Token name, List<Function> methods"
)
val exprFile = File(exprPath)
val exprVisitorFile = File(exprVisitorPath)
val stmtFile = File(stmtPath)
val stmtVisitorFile = File(stmtVisitorPath)

fun main() {
    defineExpr()
    defineStmt()
}

fun defineStmt() {
    val stmtText = java.lang.StringBuilder()
    val stmtVisitorText = java.lang.StringBuilder()

    stmtVisitorText.append("package my.lox.lang.ast\n\n")
    stmtVisitorText.append("interface ${stmtBase}Visitor<T> {")

    stmtText.append("package my.lox.lang.ast\n\n")
    stmtText.append("import my.lox.lang.lex.Token\n\n")
    stmtText.append("interface $stmtBase {\n")
    stmtText.append("    fun<T> accept(v : ${stmtBase}Visitor<T>) : T\n")
    stmtText.append("}\n\n")

    for (t in stmtTypes) {
        val typeDescription = parseType(t)
        val typename = typeDescription.first

        stmtText.append("data class $typename(")
        typeDescription.second.forEach {
            stmtText.append("\n\t\t\t\t\tval ${it.second} : ${it.first},")
        }
        stmtText.deleteCharAt(stmtText.length - 1)
        stmtText.append("\n\t\t\t\t\t) : $stmtBase {\n")
        stmtText.append("    override fun<T> accept(v : ${stmtBase}Visitor<T>) : T = v.visit$typename$stmtBase(this)\n")
        stmtText.append("}\n\n")

        stmtVisitorText.append("\n\tfun visit$typename$stmtBase(${typename.lowercase(Locale.getDefault())}$stmtBase : $typename) : T")
    }
    stmtVisitorText.append("\n}\n")

    stmtFile.writeText(stmtText.toString(), Charsets.US_ASCII)
    stmtVisitorFile.writeText(stmtVisitorText.toString(),Charsets.US_ASCII)
}

fun defineExpr() {
    val exprText = java.lang.StringBuilder()
    val exprVisitorText = java.lang.StringBuilder()

    exprVisitorText.append("package my.lox.lang.ast\n\n")
    exprVisitorText.append("interface ${exprBase}Visitor<T> {")

    exprText.append("package my.lox.lang.ast\n\n")
    exprText.append("import my.lox.lang.lex.Token\n\n")
    exprText.append("interface $exprBase {\n")
    exprText.append("    fun<T> accept(v : ${exprBase}Visitor<T>) : T\n")
    exprText.append("}\n\n")

    for (t in exprTypes) {
        val typeDescription = parseType(t)
        val typename = typeDescription.first

        exprText.append("data class $typename(")
        typeDescription.second.forEach {
            exprText.append("\n\t\t\t\t\tval ${it.second} : ${it.first},")
        }
        exprText.deleteCharAt(exprText.length - 1)
        exprText.append("\n\t\t\t\t\t) : $exprBase {\n")
        exprText.append("    override fun<T> accept(v : ${exprBase}Visitor<T>) : T = v.visit$typename$exprBase(this)\n")
        exprText.append("}\n\n")

        exprVisitorText.append("\n\tfun visit$typename$exprBase(${typename.lowercase(Locale.getDefault())}$exprBase : $typename) : T")
    }
    exprVisitorText.append("\n}\n")

    exprFile.writeText(exprText.toString(), Charsets.US_ASCII)
    exprVisitorFile.writeText(exprVisitorText.toString(),Charsets.US_ASCII)
}
//Given "Binary   : Expr left, Token operator, Expr right"
//Returns ("Binary",
//         [("Expr" ,"left")
//          ("Token","operator")
//          ("Expr" ,"right")]
fun parseType(t : String) : Pair<String,
                                 List<Pair<String,String>>> {
    val fields = t.split(":").map { it.trim() }
    val typename = fields[0]
    val typeVars = fields[1].split(",").map { it.trim() }

    val typeVarList = mutableListOf<Pair<String,String>>()
    typeVars.forEach {
        val tv = it.split(" ").map { it.trim() }
        typeVarList.add(Pair(tv[0],tv[1]))
    }
    return Pair(typename,typeVarList)
}