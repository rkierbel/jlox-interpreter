package com.jlox.lox.object;

import com.jlox.lox.exception.CtrlFlow;
import com.jlox.lox.grammar.string.Stmt;
import com.jlox.lox.pipeline.Interpreter;

import java.util.List;

public class LoxFunction implements LoxCallable {

  private final boolean isInitializer;
  private final Stmt.Function declaration;
  private final Environment closure;

  public LoxFunction(boolean isInitializer,
                     Stmt.Function declaration,
                     Environment closure) {
    this.isInitializer = isInitializer;
    this.declaration = declaration;
    this.closure = closure;
  }

  /**
   * One function call triggers the creation of its dedicated Environment.
   * Within this Environment, variables are created using parameters' names and bound to the arguments' values.
   */
  @Override
  public Object call(Interpreter interpreter,
                     List<Object> args) {
    Environment env = new Environment(closure);
    //Walk the parameters' list and bind variables
    for (int i = 0; i < declaration.params.size(); i++)
      env.define(declaration.params.get(i).lexeme(), args.get(i));

    try {
      /*
       executeBlock() will then discard the function local environment and restore the one active at the callsite
       This is where the code of the function becomes a living invocation
     */
      interpreter.executeBlock(declaration.body, env);
    } catch (CtrlFlow.Return returnValue) {
      if (isInitializer) return closure.getFromEnvt(0, "this");
      return returnValue.value;
    }

    //init() always returns 'this' even when directly called
    if (isInitializer) return closure.getFromEnvt(0, "this");
    return null;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  /**
   * Creates a new environment nestled in the method's original closure.
   * When the method is called, that new environment will become the parent of the method's body environment.
   * 'this' is declared as a local variable in that new environment.
   * It is bound to the instance the method is being accessed from.
   */
  public LoxFunction bind(LoxInstance instance) {
    Environment env = new Environment(closure);
    env.define("this", instance);
    return new LoxFunction(isInitializer, declaration, env);
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme() + ">";
  }
}
