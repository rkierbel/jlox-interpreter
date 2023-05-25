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

    //If no matching field found, lookup methods
    LoxFunction method = clazz.findMethod(name.lexeme());
    /*
    Since the resolver has a scope for 'this', the interpreter needs to create a corresponding environment for it.
    The environment is created after the method on the instance has been found.
     */
    if (method != null) return method.bind(this);

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
