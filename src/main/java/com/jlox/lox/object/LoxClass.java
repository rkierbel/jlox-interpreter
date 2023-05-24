package com.jlox.lox.object;

import com.jlox.lox.pipeline.Interpreter;

import java.util.List;

public class LoxClass implements LoxCallable {

  final String name;

  public LoxClass(String name) {
    this.name = name;
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
