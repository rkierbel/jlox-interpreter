package com.jlox.lox.object;

import com.jlox.lox.pipeline.Interpreter;

import java.util.List;

public interface LoxCallable {

  Object call(Interpreter interpreter, List<Object> args);

  int arity();
}
