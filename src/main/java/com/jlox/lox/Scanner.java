package com.jlox.lox;

import java.util.ArrayList;
import java.util.List;

import static com.jlox.lox.TokenType.COMMA;
import static com.jlox.lox.TokenType.DOT;
import static com.jlox.lox.TokenType.LEFT_BRACE;
import static com.jlox.lox.TokenType.LEFT_PAREN;
import static com.jlox.lox.TokenType.MINUS;
import static com.jlox.lox.TokenType.PLUS;
import static com.jlox.lox.TokenType.RIGHT_BRACE;
import static com.jlox.lox.TokenType.RIGHT_PAREN;
import static com.jlox.lox.TokenType.SEMICOLON;
import static com.jlox.lox.TokenType.STAR;

/**
 * Stores the source code as a String from which it generates a list of tokens.
 */
public class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0; //first char in lexeme being scanned
  private int current = 0; //char being considered currently
  private int line = 1; //tracks what source code's line the current lexeme stands on

  Scanner(String src) {
    this.source = src;
  }

  List<Token> scanTokens() {
    while(!isAtEnd()) {
      //beginning of a lexeme
      start = current;
      scanToken();
    }
    //adds tokens until runs out of characters
    tokens.add(new Token(TokenType.EOF, "", null, line));
    return tokens;
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(' -> addToken(LEFT_PAREN);
      case ')' -> addToken(RIGHT_PAREN);
      case '{' -> addToken(LEFT_BRACE);
      case '}' -> addToken(RIGHT_BRACE);
      case ',' -> addToken(COMMA);
      case '.' -> addToken(DOT);
      case '-' -> addToken(MINUS);
      case '+' -> addToken(PLUS);
      case ';' -> addToken(SEMICOLON);
      case '*' -> addToken(STAR);
    }
  }

  /**
   * Consumes the next character in the source code, returns it.
   */
  private char advance() {
    return source.charAt(current++);
  }

  /**
   * Creates a token from the current lexeme.
   */
  private void addToken(TokenType type) {
    addToken(type, null);
  }

  /**
   * Handles tokens with literal value.
   */
  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}
