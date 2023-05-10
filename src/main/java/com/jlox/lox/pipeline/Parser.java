package com.jlox.lox.pipeline;

import com.jlox.lox.Lox;
import com.jlox.lox.exception.ParseError;
import com.jlox.lox.grammar.string.Expr;
import com.jlox.lox.grammar.string.Stmt;
import com.jlox.lox.grammar.token.Token;
import com.jlox.lox.grammar.token.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jlox.lox.grammar.token.TokenType.*;

public class Parser {

  //Consumes a flat input sequence
  private final List<Token> tokens;
  //Points to next token to be parsed
  private int current = 0;

  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  public List<Stmt> parse() {
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
    if (match(FOR)) return forStatement();
    if (match(IF)) return ifStatement();
    if (match(PRINT)) return printStatement();
    if (match(WHILE)) return whileStatement();
    if (match(LEFT_BRACE)) return new Stmt.Block(block());

    return expressionStatement();
  }

  private Stmt forStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'for'.");

    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement(); //If neither the above keywords match, initializer must be an expression
    }

    Expr condition = null;
    if (!check(SEMICOLON)) {
      condition = expression();
    }
    consume(SEMICOLON, "Expect ';' after loop condition.");

    Expr increment = null;
    if (!check(SEMICOLON)) {
      increment = expression();
    }
    consume(RIGHT_PAREN, "Expect ')' after 'for' clauses.");

    Stmt body = statement();

    //De-sugaring the 'for' loop
    if (increment != null) {
      body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
    }
    //Build the loop using primitive while
    if (condition == null) condition = new Expr.Literal(true);
    body = new Stmt.While(condition, body);
    //If there's an initializer, runs once before the entire loop
    if (initializer != null) body = new Stmt.Block(Arrays.asList(initializer, body));
    return body;
  }

  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after 'if' condition.");

    Stmt thenBranch =statement();
    Stmt elseBranch = null;
    if (match(ELSE)) { //Will be bound to the nearest 'if'
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after 'if' condition.");
    Stmt body = statement();

    return new Stmt.While(condition, body);
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    //Check for isAtEnd is necessary if user forgets closing right brace
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }
    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
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
    return assignment();
  }

  /**
   * The receiver (lvalue) of an assignment can be any expression (unbounded number of tokens).
   *
   */
  private Expr assignment() {
    Expr expr = or();

    //Parse the right hand side only if it finds an '='
    if (match(EQUAL)) {
      Token equals = previous();
      //Assignment being right associative, call is recursive to parse the right hand side
      Expr value = assignment();

      if (expr instanceof Expr.Variable variable) {
        Token name = variable.name;
        return new Expr.Assign(name, value);
      }
      //Error example -> a + b = c; (a) = 3;
      error(equals, "Invalid assignment target.");
    }

    return expr;
  }

  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
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