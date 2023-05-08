package com.jlox.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {

  /**
   * Most tokens refer to a unit of code at a specific place in the source text.
   * Unlike these tokens, Identifier tokens should always refer to the same variable.
   * Hence, the choice of String as keys.
   */
  private final Map<String, Object> values = new HashMap<>();

  /**
   * If the variable is found, return the value bound to it,
   */
  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }
    //It's ok to refer to a variable before it is defined only if the statement doesn't cause the variable to be evaluated
    throw new RuntimeError(name, "Undefined variable '" + name + "'.");
  }

  /**
   * We voluntarily don't check if the name is present in values : this allows variable redefinition.
   */
  void define(String name, Object value) {
    values.put(name, value);
  }
}
