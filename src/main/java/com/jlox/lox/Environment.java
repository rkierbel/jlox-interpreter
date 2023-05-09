package com.jlox.lox;

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
  Environment() {
    this.enclosing = null;
  }

  /**
   * Creates new local scope within an enclosing Environment.
   */
  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  /**
   * If the variable is found, return the value bound to it.
   * Walks up the chain of Environments to find the variable.
   */
  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }
    if (enclosing != null) return enclosing.get(name);
    //It's ok to refer to a variable before it is defined only if the statement doesn't cause the variable to be evaluated
    throw new RuntimeError(name, "Undefined variable '" + name + "'.");
  }

  /**
   * We voluntarily don't check if the name is present in values : this allows variable redefinition.
   */
  void define(String name, Object value) {
    values.put(name, value);
  }

  /**
   * Assignment cannot create a new variable.
   * Walks up the Environment parent-pointer tree.
   */
  public void assign(Token name, Object value) {
    Object replaced = values.computeIfPresent(name.lexeme, (k, v) -> value);
    if (replaced == null) {
      if (enclosing != null) enclosing.assign(name, value);
      throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
  }
}
