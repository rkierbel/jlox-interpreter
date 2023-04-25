package com.jlox.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jlox.lox.TokenType.*;

/**
 * Stores the source code as a String from which it generates a list of tokens.
 */
public class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0; //first char in lexeme being scanned
  private int current = 0; //char being considered currently
  private int line = 1; //tracks what source code's line the current lexeme stands on

  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and", AND);
    keywords.put("class", CLASS);
    keywords.put("else", ELSE);
    keywords.put("false", FALSE);
    keywords.put("for", FOR);
    keywords.put("fun", FUN);
    keywords.put("if", IF);
    keywords.put("nil", NIL);
    keywords.put("or", OR);
    keywords.put("print", PRINT);
    keywords.put("return", RETURN);
    keywords.put("super", SUPER);
    keywords.put("this", THIS);
    keywords.put("true", TRUE);
    keywords.put("var", VAR);
    keywords.put("while", WHILE);
  }

  Scanner(String src) {
    this.source = src;
  }

  List<Token> scanTokens() {
    while (!isAtEnd()) {
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
      case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
      case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
      case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);
      case '>' -> addToken(match('=') ? GREATER_EQUAL : GREATER);
      case '/' -> {
        //keep consuming comment's characters : addToken() not called because comments are discarded
        if (match('/')) {
          while (peek() != '\n' && !isAtEnd()) advance();
        } else if (match('*')) {
          while (peek() != '*' && peekNext() != '/' && !isAtEnd()) advance();
        } else {
          addToken(SLASH);
        }
      }
      //ignore whitespaces : starts a new lexeme after a whitespace char
      case ' ', '\r', '\t' -> {
      }
      case '\n' -> line++;
      case '"' -> string();
      default -> {
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) { //handle identifiers and reserved words
          identifier();
        } else {
          Lox.error(line, "Unexpected character."); //erroneous character still consumed
        }
      }
    }
  }

  /**
   * Consumes the next character in the source code, returns it.
   */
  private char advance() {
    return source.charAt(current++);
  }

  /**
   * Creates a token from the current lexeme
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

  /**
   * Works like a conditional advance() that consumes the character only if it is expected.
   */
  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    current++;
    return true;
  }

  /**
   * Lookahead : looks at the current unconsumed char.
   */
  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  private void string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') line++;
      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }

    advance(); //to closing double quote char
    String value = source.substring(start + 1, current - 1); //strip surrounding quotes
    addToken(STRING, value);
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private void number() {
    while (isDigit(peek())) advance();

    if (peek() == '.' && isDigit(peekNext())) { //check if there's a character after the decimal point
      advance();
      while (isDigit(peek())) advance();
    }

    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  private void identifier() {
    while (isAlphanumeric(peek())) advance();

    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null) type = IDENTIFIER;
    addToken(type);
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            c == '_';
  }

  private boolean isAlphanumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }
}
