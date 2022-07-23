package my.lox.lang.lex

data class LexerError(val msg : String,
                      val pos : Int,
                      val sourceStr : String)
