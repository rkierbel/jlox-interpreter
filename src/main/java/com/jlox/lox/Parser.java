package com.jlox.lox;

import java.util.ArrayList;
import java.util.List;

import static com.jlox.lox.TokenType.BANG;
import static com.jlox.lox.TokenType.BANG_EQUAL;
import static com.jlox.lox.TokenType.EOF;
import static com.jlox.lox.TokenType.EQUAL;
import static com.jlox.lox.TokenType.EQUAL_EQUAL;
import static com.jlox.lox.TokenType.FALSE;
import static com.jlox.lox.TokenType.GREATER;
import static com.jlox.lox.TokenType.GREATER_EQUAL;
import static com.jlox.lox.TokenType.IDENTIFIER;
import static com.jlox.lox.TokenType.LEFT_PAREN;
import static com.jlox.lox.TokenType.LESS;
import static com.jlox.lox.TokenType.LESS_EQUAL;
import static com.jlox.lox.TokenType.MINUS;
import static com.jlox.lox.TokenType.NIL;
import static com.jlox.lox.TokenType.NUMBER;
import static com.jlox.lox.TokenType.PLUS;
import static com.jlox.lox.TokenType.PRINT;
import static com.jlox.lox.TokenType.RIGHT_PAREN;
import static com.jlox.lox.TokenType.SEMICOLON;
import static com.jlox.lox.TokenType.SLASH;
import static com.jlox.lox.TokenType.STAR;
import static com.jlox.lox.TokenType.STRING;
import static com.jlox.lox.TokenType.TRUE;
import static com.jlox.lox.TokenType.VAR;

public class Parser {

  //Consumes a flat input sequence
  private final List<Token> tokens;
  //Points to next token to be parsed
  private int current = 0;

  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

/*  Expr parse() {
    try {
      return expression();
    } catch (ParseError parsErr) {
      return null;
    }
  }*/

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();

    while (!isAtEnd())
      statements.add(declaration());

    return statements;
  }

  private Stmt declaration() {
    try {
      if (match(VAR)) return varDeclaration();
      return statement();
    } catch (ParseError error) {
      //Proceed to error recovery
      synchronize();
      return null;
    }
  }

  /**
   * A program is a list of statements, which are parsed by this method.
   * Parses an expression statement if no other statement matches.
   */
  private Stmt statement() {
    if (match(PRINT)) return printStatement();

    return expressionStatement();
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  /**
   * The expression is wrapped in the corresponding statement before being returned as such.
   */
  private Stmt expressionStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(value);
  }

  private Stmt varDeclaration() {
    //Parser has a match for the 'var' token, will then require an identifier token (variable name)
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) { //Parser then knows it's an initializer expression
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  /**
   * Each method parsing a grammar rule produces a syntax tree for the rule, then returns it to the caller.
   * Will report a syntax error if it can't parse an expression at the current token.
   */
  private Expr expression() {
    return equality();
  }

  private Expr equality() {
    // Non-terminal -> take the result, store it.
    Expr expr = comparison();

    //If we don't see a "!=" or "==" -> we are done with the sequence of equality operators
    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      /* For each iteration, store resulting expression back in expr,
       creating a left-associative nested tree of binary operator node */
      expr = new Expr.Binary(expr, operator, right);
    }
    //If never entered the loop, returns comparison(), matching an equality operator or anything of higher precedence
    return expr;
  }

  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);
    if (match(IDENTIFIER)) return new Expr.Variable(previous()); //Parsing variable expression

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  /**
   * Discard tokens until reach beginning of next statement - after a semicolon.
   * Purpose is to correctly parse the rest of the file after reporting a syntax error.
   */
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> {
          return;
        }
      }

      advance();
    }
  }

  /**
   * If tokens are of the given types, return true.
   */
  private boolean match(TokenType... types) {
    for (var t : types) {
      if (check(t)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type; //Never consumes the token, unlike match()
  }

  /**
   * Consumes the current token and returns it.
   */
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  /**
   * Checks if we are out of tokens to parse.
   */
  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  /**
   * Returns the current token we have to consume.
   */
  private Token peek() {
    return tokens.get(current);
  }

  /**
   * Returns the most recently consumed token.
   */
  private Token previous() {
    return tokens.get(current - 1);
  }
}
