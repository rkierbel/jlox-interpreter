package com.jlox.lox.object;

import com.jlox.lox.grammar.string.Stmt;
import com.jlox.lox.pipeline.Interpreter;

import java.util.List;

public class LoxFunction implements LoxCallable {

  private final Stmt.Function declaration;

  public LoxFunction(Stmt.Function declaration) {
    this.declaration = declaration;
  }

  /**
   * One function call triggers the creation of its dedicated Environment.
   * Within this Environment, variables are created using parameters' names and bound to the arguments' values.
   */
  @Override
  public Object call(Interpreter interpreter,
                     List<Object> args) {
    Environment env = new Environment(interpreter.globals);
    //Walk the parameters' list and bind variables
    for (int i = 0; i < declaration.params.size(); i++)
      env.define(declaration.params.get(i).lexeme(), args.get(i));

    /*
    executeBlock() will then discard the function local environment and restore the one active at the callsite
    This is where the code of the function becomes a living invocation
     */
    interpreter.executeBlock(declaration.body, env);
    return null;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme() + ">";
  }
}
