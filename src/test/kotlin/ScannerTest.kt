import io.kotlintest.forAll
import io.kotlintest.matchers.shouldBe
import io.kotlintest.properties.forAll
import io.kotlintest.specs.StringSpec
import my.lox.lang.lex.Lexer
import my.lox.lang.lex.TokenType

class ScannerTest : StringSpec() {
    init {

        "Concatenating All Token Lexemes Returns The Original Source Minus Spaces And Comments And Unrecognized Characters" {
            forAll { source : String ->
                val str1 = lexThenReconstructString(source)
                val str2 = removeCommentsWhitespaceUnrecognized(source)
                val equal = str1 == str2
                if (!equal) println("Failure !\n lexThenReconstructString             : $str1 \n removeCommentsWhitespaceUnrecognized : $str2 ")
                equal
            }
        }
        "Doesn't Crash" {
            forAll { source : String ->
                Lexer(source)
                true
            }
        }
    }

     private companion object {
        @JvmStatic
        fun lexThenReconstructString(s : String) : String {
            val sb = StringBuilder()
            Lexer(s).tokens.forEach {
                sb.append(it.lexeme)
            }
            return sb.toString()
        }
        @JvmStatic
        fun removeCommentsWhitespaceUnrecognized(s : String) : String {
            val sb = StringBuilder()

            var currentlyInsideComment = false
            var currentlyInsideString  = false
            val stringSB = StringBuilder()
            var previous : Char? = null

            s.forEach {
                if (currentlyInsideComment) {
                    if (it == '\n') currentlyInsideComment = false
                }
                else {
                    if (!currentlyInsideString) {
                        if (it == '/' && previous != null && previous == '/') {
                            currentlyInsideComment = true
                            sb.deleteCharAt(sb.length - 1)
                        }
                        if (it == '"') currentlyInsideString = true
                    }
                    else {
                        if (it == '"') {
                            currentlyInsideString = false
                            sb.append('"')
                            sb.append(stringSB)
                            stringSB.clear()
                        } else stringSB.append(it)
                    }
                }
                val isWhite = it == '\n' || it == '\t' || it == '\r' || it == ' '

                if (!currentlyInsideString && !currentlyInsideComment && !isWhite && Lexer.isARecognizableCharacter(it)) {
                    sb.append(it)
                }
                previous = it
            }

            return sb.toString()
        }
    }

}