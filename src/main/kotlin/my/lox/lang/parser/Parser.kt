package my.lox.lang.parser

import my.lox.lang.ast.*
import my.lox.lang.lex.Token
import my.lox.lang.lex.TokenType
import kotlin.math.exp


class Parser(private val toks: List<Token>) {
    internal class ParseException : java.lang.RuntimeException()

    private var current = 0
    private val internalErrors = mutableListOf<String>()
    //public interface
    //------------------------------
    val ast : List<Stmt> = parse()
    val errs : List<String> = internalErrors
    //------------------------------

    private fun parse() : List<Stmt> {
            val result = mutableListOf<Stmt>()
            while (!end) {
                val d = parseDeclaration()
                if (d != null) result.add(d)
            }
            return result
    }
    private fun parseDeclaration() : Stmt? {
        try {
            if (isNextToken(TokenType.VAR)) return parseVariableDeclaration()
            if (isNextToken(TokenType.FUN)) return parseFunctionDeclaration()
            return parseStatement()
        } catch (_: ParseException) {
            synchronizeTokenStream()
            return null
        }
    }

    private fun parseVariableDeclaration() : Stmt {
        expect(TokenType.IDENTIFIER,"Lox Parse : Expected an identifier in the variable declaration at line ${toks[current].pos}")

        val name = previousToken()
        var init : Expr? = null
        if (isNextToken(TokenType.EQUAL)) {
            init = parseExpression()
        }

        expect(TokenType.SEMICOLON,"Lox Parse : Expected a semicolon at the end of variable declaration at line ${toks[current].pos}")
        return Var(name,init)
    }

    private fun parseFunctionDeclaration(functionKind : String = "function") : Stmt {
        expect(TokenType.IDENTIFIER,"Lox Parse : Expected $functionKind name")
        val name = previousToken()
        expect(TokenType.LEFT_PAREN,"Lox Parse : Expected '(' after $functionKind name")

        val funcParams = mutableListOf<Token>()
        if (!isNextToken(TokenType.RIGHT_PAREN)) {
            do {
                expect(TokenType.IDENTIFIER,"Expected $functionKind paramter name")
                funcParams.add(previousToken())
            } while (isNextToken(TokenType.COMMA))

            expect(TokenType.RIGHT_PAREN,"Lox Parse : No ')' to terminate function call at line ${toks[current].pos}")
        }
        if (funcParams.size >= 255) {
            internalErrors.add("Lox Parse : Can't declare $functionKind to have more than 254 parameter, ${funcParams.size} are declared")
        }

        expect(TokenType.LEFT_BRACE,"Expected '{' before $functionKind body")
        val body = parseBlockStatement() as Block
        return Function(name,funcParams,body.statements)
    }

    private fun parseStatement() : Stmt {
        if (isNextToken(TokenType.IF)) return parseIfStatement()
        if (isNextToken(TokenType.WHILE)) return parseWhileStatement()
        if (isNextToken(TokenType.RETURN)) return parseReturnStatement()
        if (isNextToken(TokenType.FOR)) return parseForStatement()
        if (isNextToken(TokenType.PRINT)) return parsePrintStatement()
        if (isNextToken(TokenType.LEFT_BRACE)) return parseBlockStatement()

        return parseExpressionStatement()
    }

    private fun parseReturnStatement() : Stmt {
        val kword = previousToken()
        val value : Expr? = if (!isNextToken(TokenType.SEMICOLON)) {
                                val e = parseExpression()
                                expect(TokenType.SEMICOLON,"Lox Parse : Expected ';' after return keyword")
                                e
                            } else null
        return Return(kword,value)
    }

    private fun parseForStatement(): Stmt {
        expect(TokenType.LEFT_PAREN, "Lox Parse : Expected an '(' after 'for' at line ${toks[current].pos}")

        val init = if (isNextToken(TokenType.SEMICOLON)) null
                  else if (isNextToken(TokenType.VAR)) parseVariableDeclaration()
                  else parseExpressionStatement()

        val cond = if (isNextToken(TokenType.SEMICOLON)) null
                   else (parseExpressionStatement() as Expression).expression

        val incr = if (isNextToken(TokenType.RIGHT_PAREN)) null
                   else {
                       val e = parseExpression()
                        expect(TokenType.RIGHT_PAREN,"Lox Parse : Expected an ')' after the for clause at line ${toks[current].pos}")
                        e
                   }
        val body = parseStatement()

        //Now we have all components of a for loop, we desugar it into a while loop
        val whileBody = if (incr == null) body
                        else Block(listOf(body,Expression(incr)))

        val whileCond = cond ?: Literal(true)

        val aWhile = While(whileCond,whileBody)

        return if (init == null) aWhile
        else   Block(listOf(init,aWhile))
    }

    private fun parseWhileStatement(): Stmt {
        expect(TokenType.LEFT_PAREN, "Lox Parse : Expected an '(' after 'while' at line ${toks[current].pos}")
        val cond = parseExpression()
        expect(TokenType.RIGHT_PAREN, "Lox Parse : Expected a ')' after the condition of the while at line ${toks[current].pos}")

        val body = parseStatement()
        return While(cond,body)
    }

    private fun parseIfStatement(): Stmt {
        expect(TokenType.LEFT_PAREN, "Lox Parse : Expected an '(' after 'if' at line ${toks[current].pos}")
        val cond = parseExpression()
        expect(TokenType.RIGHT_PAREN, "Lox Parse : Expected a ')' after the condition of the if at line ${toks[current].pos}")

        val thenbranch = parseStatement()
        var elsebranch : Stmt? = null
        if (isNextToken(TokenType.ELSE)) elsebranch = parseStatement()

        return If(cond,thenbranch,elsebranch)
    }

    private fun parsePrintStatement() : Stmt {
        val e = parseExpression()
        expect(TokenType.SEMICOLON,"Lox Parse : Expected a ; after print statement at line ${toks[current].pos}")
        return Print(e)
    }

    private fun parseBlockStatement() : Stmt {
        val stats = mutableListOf<Stmt>()

        while (!isNextToken(TokenType.RIGHT_BRACE) && !end) {
            val d = parseDeclaration()

            if (d != null) stats.add(d)
        }
        if (end && previousToken().type != TokenType.RIGHT_BRACE) {
            internalErrors.add("Lox Parse : Unterminated block at line ${previousToken().pos}")
        }
        return Block(stats)
    }

    private fun parseExpressionStatement() : Stmt {
        val e = parseExpression()
        expect(TokenType.SEMICOLON,"Lox Parse : Expected a ; after expression at line ${toks[current].pos}")
        return Expression(e)
    }


    private fun parseExpression() = parseAssignmentExpression()

    private fun parseAssignmentExpression() : Expr {
        val exp = parseOrExpression()

        //The previous was the left-hand side of an assignment
        if (isNextToken(TokenType.EQUAL)) {
            val tokEquals = previousToken()
            val rightHandSide = parseAssignmentExpression()

            if (exp is Variable) {
                val name = exp.name
                return Assign(name,rightHandSide)
            }
            error("Lox Parse : Invalid assignment to non-variable at line ${tokEquals.pos}")
        }
        return exp
    }

    private fun parseOrExpression(): Expr {
        var e1 = parseAndExpression()

        while (isNextToken(TokenType.OR)) {
            val op = previousToken()
            val e2 = parseAndExpression()
            e1 = Logical(e1,op,e2)
        }
        return e1
    }

    private fun parseAndExpression(): Expr {
        var e1 = parseEqualityExpression()

        while (isNextToken(TokenType.AND)) {
            val op = previousToken()
            val e2 = parseEqualityExpression()
            e1 = Logical(e1,op,e2)
        }
        return e1
    }

    private fun parseEqualityExpression() : Expr {
        var e1 = parseComparisonExpression()

        while (isNextToken(TokenType.BANG_EQUAL,TokenType.EQUAL_EQUAL)) {
            val operatorTok = previousToken()
            val e2 = parseComparisonExpression()
            e1 = Binary(e1,operatorTok,e2)
        }
        return e1
    }

    private fun parseComparisonExpression() : Expr {
        var e1 = parseTermExpression()

        while (isNextToken(TokenType.GREATER,TokenType.GREATER_EQUAL,TokenType.LESS,TokenType.LESS_EQUAL)) {
            val operatorTok = previousToken()
            val e2 = parseTermExpression()
            e1 = Binary(e1,operatorTok,e2)
        }
        return e1
    }

    private fun parseTermExpression() : Expr {
        var e1 = parseFactorExpression()

        while (isNextToken(TokenType.MINUS,TokenType.PLUS)) {
            val operatorTok = previousToken()
            val e2 = parseFactorExpression()
            e1 = Binary(e1,operatorTok,e2)
        }
        return e1
    }

    private fun parseFactorExpression() : Expr {
        var e1 = parseUnaryExpression()

        while (isNextToken(TokenType.SLASH,TokenType.STAR)) {
            val operatorTok = previousToken()
            val e2 = parseUnaryExpression()
            e1 = Binary(e1,operatorTok,e2)
        }
        return e1
    }

    private fun parseUnaryExpression() : Expr {
        if (isNextToken(TokenType.BANG,TokenType.MINUS)) {
            val operatorTok = previousToken()
            val e = parseUnaryExpression()
            return Unary(operatorTok,e)
        }
        return parseCallExpression()
    }

    private fun parseCallExpression() : Expr {
        var e =  parsePrimaryExpression()

        while (true) {
            //e is a callee, and we have a list of arguments coming
            if (isNextToken(TokenType.LEFT_PAREN)) {
                e = parseRestOfCall(e)
            }
            else break
        }
        return e
    }
    //takes a callee, starts parsing the token stream from the point after consuming a left paren
    //Then consumes an arbitrary number of comma-separated arguments
    //Returns a Cell expression with the passed callee as its callee and the consumed arguments as its arguments
    private fun parseRestOfCall(callee : Expr) : Expr {
        val callArgs = mutableListOf<Expr>()

        if (!isNextToken(TokenType.RIGHT_PAREN)) {
            do {
                callArgs.add(parseExpression())
            } while (isNextToken(TokenType.COMMA))

            expect(TokenType.RIGHT_PAREN,"Lox Parse : No ')' to terminate function call at line ${toks[current].pos}")
        }
        val tok = previousToken()
        val pos = tok.pos

        if (callArgs.size >= 255) {
            internalErrors.add("Lox Parse : Function call can't have more than 254 arguments, call at line $pos has ${callArgs.size}")
        }

        return Call(callee,tok,callArgs)
    }

    private fun parsePrimaryExpression() : Expr {
        if (isNextToken(TokenType.FALSE)) return Literal(false)
        if (isNextToken(TokenType.TRUE))  return Literal(true)
        if (isNextToken(TokenType.NIL))   return Literal(null)

        if (isNextToken(TokenType.NUMBER,TokenType.STRING)) return Literal(previousToken().value)

        if (isNextToken(TokenType.LEFT_PAREN)) {
            val e = parseExpression()
            expect(TokenType.RIGHT_PAREN,"Lox Parse : Expected a closing ')' at line ${toks[current].pos}")
            return e
        }

        if (isNextToken(TokenType.IDENTIFIER)) {
            return Variable(previousToken())
        }

        throw error("Lox Parse : Expected an expression at line ${toks[current].pos}")
    }

    private val end
        get() = toks[current].type == TokenType.EOF

    private fun isNextToken(vararg tokTs : TokenType) : Boolean {
        if (!end && tokTs.any { it == toks[current].type }) {
            current++
            return true
        }
        return false
    }

    private fun previousToken() = toks[current-1]

    private fun expect(tokT : TokenType, msg : String) {
        if (!isNextToken(tokT)) {
            throw error(msg)
        }
    }
    private fun error(msg : String) : ParseException {
        internalErrors.add(msg)
        return ParseException()
    }

    private fun synchronizeTokenStream() {
        while (!isNextToken(TokenType.CLASS,
                            TokenType.FUN,
                            TokenType.VAR,
                            TokenType.FOR,
                            TokenType.IF,
                            TokenType.WHILE,
                            TokenType.PRINT,
                            TokenType.RETURN)) {
            if (end) return
            if (isNextToken(TokenType.SEMICOLON)) return
            current++
        }
        current--
    }
}