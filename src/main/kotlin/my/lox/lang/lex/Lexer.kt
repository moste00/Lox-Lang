package my.lox.lang.lex

class Lexer(private val source : String) {
    private var start = 0
    private var current  = 0
    private var line = 1
    private var internalErrors = mutableListOf<LexerError>()

    //public interface
    //------------------------------------------------
    val tokens : List<Token> = lexAll()
    val errs   : List<LexerError> = internalErrors
    //------------------------------------------------

    private fun lexAll() : List<Token> {
        val result = mutableListOf<Token>()
        while (!end) {
            start = current
            val tok = lexToken()
            if (tok != null) result.add(tok)
        }
        result.add(Token(TokenType.EOF,line,null,""))
        return result
    }

    private fun lexToken() : Token? {
        return when (consume()) {
            '(' -> tokenOf(TokenType.LEFT_PAREN)
            ')' -> tokenOf(TokenType.RIGHT_PAREN)
            '{' -> tokenOf(TokenType.LEFT_BRACE)
            '}' -> tokenOf(TokenType.RIGHT_BRACE)
            ',' -> tokenOf(TokenType.COMMA)
            '.' -> tokenOf(TokenType.DOT)
            '-' -> tokenOf(TokenType.MINUS)
            '+' -> tokenOf(TokenType.PLUS)
            ';' -> tokenOf(TokenType.SEMICOLON)
            '*' -> tokenOf(TokenType.STAR)
            ' ','\r','\t' -> null
            '\n' -> {
                line++
                null
            }
            '!' -> isNext('=') { when(it) {
                     true  -> tokenOf(TokenType.BANG_EQUAL)
                     false -> tokenOf(TokenType.BANG)
                    }
                   }
            '=' -> isNext('=') {when(it) {
                     true  -> tokenOf(TokenType.EQUAL_EQUAL)
                     false -> tokenOf(TokenType.EQUAL)
                    }
                   }
            '<' -> isNext('=') {when(it) {
                     true  -> tokenOf(TokenType.LESS_EQUAL)
                     false -> tokenOf(TokenType.LESS)
                    }
                   }
            '>' -> isNext('=') {when(it) {
                     true  -> tokenOf(TokenType.GREATER_EQUAL)
                     false -> tokenOf(TokenType.GREATER)
                    }
                   }
            '/' -> isNext('/') {when(it) {
                     true  -> {
                         consumeUntilDelimiter('\n')
                         line++
                         null
                     }
                     false -> tokenOf(TokenType.SLASH)
                    }
                   }

            '"' -> {
                val str = consumeUntilDelimiter('"')
                if (str != null) tokenOf(TokenType.STRING,str)
                else {
                    internalErrors.add(
                        LexerError("Lox Lex : This string never ends",
                                    line,
                                    source.substring(start until current)

                    )
                    )
                    null
                }
            }

            in '0'..'9' -> {
                val num = consumeUntil(isNotANumeralCharacter)

                try {
                    tokenOf(TokenType.NUMBER,num.toDouble())
                }
                catch (_: java.lang.NumberFormatException) {
                    internalErrors.add(
                        LexerError("Lox Lex : Can't identify this number",
                                    line,
                                    source.substring(start until current)
                    )
                    )
                    null
                }
            }

            in 'a'..'z', in 'A'..'Z', '_' -> {
                val identifier = consumeUntil(isNotAnIdentifierCharacter)
                if (identifier in RESERVED_WORDS) {
                    tokenOf(RESERVED_WORDS[identifier]!!)
                }
                else {
                    tokenOf(TokenType.IDENTIFIER)
                }
            }

            else -> {
                internalErrors.add(
                    LexerError("Lox Lex : Can't identify this token",
                                line,
                                consumeUntil(isARecognizableCharacter)
                ))
                null
            }
        }
    }
    //Moves current pointer and returns the character it was pointing to before moving
    private fun consume() : Char {
        val c = source[current]
        current++
        return c
    }
    //Tests if there is a next character equal to the supplied one
    //If there is, it moves the current pointer and calls the supplied callback with true
    //If there isn't, calls the supplied callback with false (and doesn't mutate anything)
    private fun isNext(c : Char, action : (Boolean)-> Token?) : Token? {
        if (!end && source[current] == c) {
            current++
            return action(true)
        }
        return action(false)
    }
    //Takes a delimiter character, and keeps consuming input till current is pointing to after the delimiter character or the end is reached
    //Returns: If the end is reached without encountering the delimiter, null
    //         If the delimiter was reached, the substring starting from the old current up to and excluding the delimiter
    //Example: Suppose you are in the following string "\"my very good string\" 123a",
    //         you have consumed the first " character and current is now pointing to m
    //         calling consumeUntilDelimiter('"') will returns "my very good string" without the enclosing literal quotes
    //         and current will be pointing to the space character after the closing literal quote
    private fun consumeUntilDelimiter(c : Char) : String? {
        val curr = current
        while (!end && source[current] != c) current++
        return if (!end) {
            current++
            source.substring(curr until current-1)
        } else null
    }

    //Gets called with a termination test (char) -> bool, keeps consuming input till the test returns true
    //Pre-condition : current > 0
    //Post-condition: current > its old value and is the index of the first character for which the termination test succeeds
    //Returns: The substring including the character before the old current up to and excluding the first character for which terminationTest succeeds
    //Example: Suppose you are in the following string "a9bb_1 " and current is pointing to 9
    //         Calling consumeUntil(isNotAnIdentifierCharacter) will return the substring "a9bb_1"
    //         and leave current pointing to space character after 1
    private fun consumeUntil(terminationTest : (Char)->Boolean) : String {
        val curr = current - 1
        while (!end && !terminationTest(source[current])) current++
        return source.substring(curr until current)
    }

    private val end
        get() = current == source.length

    private fun tokenOf(type : TokenType) = tokenOf(type,null)

    private fun tokenOf(type : TokenType, value : Any?) = Token(type,
                                                               line,
                                                               value,
                                                               source.substring(start until current))
    companion object {
        @JvmStatic
        val RESERVED_WORDS = mapOf(
                            "and"    to TokenType.AND,
                            "class"  to TokenType.CLASS,
                            "else"   to TokenType.ELSE,
                            "false"  to TokenType.FALSE,
                            "for"    to TokenType.FOR,
                            "fun"    to TokenType.FUN,
                            "if"     to TokenType.IF,
                            "nil "   to TokenType.NIL,
                            "or"     to TokenType.OR,
                            "print"  to TokenType.PRINT,
                            "return" to TokenType.RETURN,
                            "super"  to TokenType.SUPER,
                            "this"   to TokenType.THIS,
                            "true"   to TokenType.TRUE,
                            "var"    to TokenType.VAR,
                            "while"  to TokenType.WHILE
        )

        @JvmStatic
        val isNotAnIdentifierCharacter = { it : Char ->
            it !in 'a'..'z' &&
            it !in 'A'..'Z' &&
            it !in '0'..'9' &&
            it != '_'
        }
        @JvmStatic
        val isNotANumeralCharacter = { it : Char -> it !in '0'..'9' && it != '.' }
        @JvmStatic
        //Not including the 3 ranges 'a'..'z', 'A'..'Z' and '0'..'9'
        val RECOGNIZABLE_CHARACTERS = setOf('(',')','{','}',',','.','*','+','-','/',' ','\r','\n','\t',';','<','>','=','!','"')
        @JvmStatic
        val isARecognizableCharacter = { it : Char ->
            it in 'a'..'z' ||
            it in 'A'..'Z' ||
            it == '_'            ||
            it in '0'..'9' ||
            it in RECOGNIZABLE_CHARACTERS
        }
    }
}