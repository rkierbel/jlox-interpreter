package com.jlox.lox.object;

import com.jlox.lox.exception.RuntimeError;
import com.jlox.lox.grammar.token.Token;

import java.util.HashMap;
import java.util.Map;

public class Environment {

  final Environment enclosing;

  /**
   * Most tokens refer to a unit of code at a specific place in the source text.
   * Unlike these tokens, Identifier tokens should always refer to the same variable.
   * Hence, the choice of String as keys.
   */
  private final Map<String, Object> values = new HashMap<>();

  /**
   * For global scope Environment : ends the parent-pointer tree.
   */
  public Environment() {
    this.enclosing = null;
  }

  /**
   * Creates new local scope within an enclosing Environment.
   */
  public Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  /**
   * If the variable is found, return the value bound to it.
   * Walks up the chain of Environments to find the variable.
   */
  public Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }
    if (enclosing != null) return enclosing.get(name);
    //It's ok to refer to a variable before it is defined only if the statement doesn't cause the variable to be evaluated
    throw new RuntimeError(name, "Undefined variable while getting '" + name + "'.");
  }

  public Object getFromEnvt(Integer scope, String lexeme) {
    return parent(scope).values.get(lexeme);
  }


  /**
   * We voluntarily don't check if the name is present in values : this allows variable redefinition.
   */
  public void define(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      throw new RuntimeError(name, "A variable named '" + name.lexeme +  "' has already been declared before.");
    }
    values.put(name.lexeme, value);
  }

  /**
   * Assignment cannot create a new variable.
   * Walks up the Environment parent-pointer tree.
   */
  public void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

    throw new RuntimeError(name, "Undefined variable while assigning '" + name.lexeme + "'.");
  }

  public void assignToEnvt(int scope, Token token, Object value) {
    final Environment environment = parent(scope);

    if (!environment.values.containsKey(token.lexeme)) {
      throw new RuntimeError(token, "Assigning to undefined variable '" + token.lexeme + "'.");
    }
    environment.values.put(token.lexeme, value);
  }

  private Environment parent(int scope) {
    Environment current = this;

    for (int i = 0; i < scope; i++) {
      if (current != null) current = current.enclosing;
    }
    if (current == null) throw new RuntimeError("Error retrieving environment for variable definition or reference.");
    return current;
  }

  public boolean contains(Token token) {
    if (values.containsKey(token.lexeme)) return true;
    if (enclosing != null) return enclosing.contains(token);
    return false;
  }

}
