package com.jlox.lox.grammar.token;

/**
 * Accurate lexical analysis involves bundling the lexeme with specific data.
 * Such data is reflected in the Token class' state.
 * The scanner must know whether the lexeme is a reserved keyword, thus what kind of token the lexeme represents.
 * The scanner must know whether the lexeme stands for a literal value.
 * For the purpose of tracking and reporting syntax errors, the location of the token must be remembered.
 */
public class Token {
  public final TokenType type;
  public final String lexeme;
  public final Object literal;
  public final int line;

  public Token(TokenType type,
               String lexeme,
               Object literal,
               int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  @Override
  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}
