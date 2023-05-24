package com.jlox.lox.pipeline;

import com.jlox.lox.Lox;
import com.jlox.lox.grammar.string.Expr;
import com.jlox.lox.grammar.string.Stmt;
import com.jlox.lox.grammar.token.Token;
import com.jlox.lox.helper.FunctionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Performs one tree walk between parsing and interpreting to resolve all variables it contains.
 * Performs a static analysis without any dynamic execution (no side effects, no control flow).
 * The Resolver is interested in the following syntax tree nodes : <br>
 * block and function declaration statements,<br>
 * variables declarations and assignment.
 */
public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

  private final Interpreter interpreter;
  private FunctionType currentFunction = FunctionType.NONE;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>(); //Used for local block scopes

  public Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (scopes.isEmpty() &&
            scopes.peek().get(expr.name.lexeme()) == Boolean.FALSE) {
      //Handle case where value is used while having been declared but not defined
      Lox.error(expr.name,
              "Can't read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    //First resolve the expression for the assigned value, in case it references to other variables
    resolve(expr.value);
    //Then resolve the variable being assigned to
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);

    for (Expr argument : expr.arguments) resolve(argument);
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    /*
    Defining the name eagerly (unlike when resolving variables) before resolving the function's body
    allows the function to recursively refer to itself
     */
    define(stmt.name);
    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  /**
   * Binding is split in two steps.
   * First, declaring : adds a variable to the innermost scope, shadowing any preexisting outer one.
   * Second, defining : sets the variable's value in the scope map to true, meaning it is fully resolved.
   */
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) resolve(stmt.initializer);
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    return null;
  }

  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Can't return from top-level code.");
    }
    if (stmt.value != null) resolve(stmt.value);
    return null;
  }

  private void beginScope() {
    scopes.push(new HashMap<>());
  }

  private void endScope() {
    scopes.pop();
  }

  public void resolve(List<Stmt> statements) {
    for (Stmt stmt : statements) resolve(stmt);
  }

  /**
   * Using indirection, apply the Visitor pattern to the given syntax tree node.
   */
  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  /**
   * The binding is marked as 'not ready' using 'false', meaning the resolving process is not done yet.
   */
  private void declare(Token name) {
    if (scopes.isEmpty()) return;

    Map<String, Boolean> scope = scopes.peek();
    if (scope.containsKey(name.lexeme())) {
      Lox.error(name, "Already a variable with this name in this scope.");
    }
    scope.put(name.lexeme(), false);
  }

  private void define(Token name) {
    if (scopes.isEmpty()) return;

    scopes.peek().put(name.lexeme(), true);
  }

  /**
   * Start from innermost scope and work outwards.
   */
  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme())) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
  }

  private void resolveFunction(Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

    beginScope();
    for (Token param : function.params) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();
    currentFunction = enclosingFunction;
  }
}
