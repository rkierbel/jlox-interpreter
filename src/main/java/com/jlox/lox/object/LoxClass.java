package com.jlox.lox.object;

import com.jlox.lox.pipeline.Interpreter;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {

  final String name;
  private final Map<String, LoxFunction> methods;

  public LoxClass(String name,
                  Map<String, LoxFunction> methods) {
    this.name = name;
    this.methods = methods;
  }

  public LoxFunction findMethod(String name) {
    if (methods.containsKey(name)) return methods.get(name);
    return null;
  }

  @Override
  public Object call(Interpreter interpreter,
                     List<Object> args) {
    LoxInstance instance = new LoxInstance(this);
    return instance;
  }

  /**
   * used by the interpreter to validate the user passed an appropriate amount of arguments
   * to a user-defined constructor.
   */
  @Override
  public int arity() {
    return 0;
  }

  @Override
  public String toString() {
    return name;
  }
}
