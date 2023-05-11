package com.jlox.lox.exception;

import com.jlox.lox.grammar.token.Token;

public class RuntimeError extends RuntimeException {

  public Token token;

  public RuntimeError(Token token, String message) {
    super(message);
    this.token = token;
  }

  public RuntimeError(String message) {
    super(message);
  }
}
