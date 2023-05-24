package com.jlox.lox.object;

import com.jlox.lox.exception.RuntimeError;
import com.jlox.lox.grammar.token.Token;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {

  private LoxClass clazz;
  private final Map<String, Object> fields = new HashMap<>();

  public LoxInstance(LoxClass clazz) {
    this.clazz = clazz;
  }

  public Object get(Token name) {
    if (fields.containsKey(name.lexeme()))
      return fields.get(name.lexeme());

    throw new RuntimeError(name, "Undefined property '" + name.lexeme() + "'.");
  }

  public void set(Token name, Object value) {
    fields.put(name.lexeme(), value);
  }

  @Override
  public String toString() {
    return clazz.name + " instance.";
  }
}
