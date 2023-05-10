package com.jlox.lox.pipeline;

import com.jlox.lox.grammar.string.Expr;

public class AstPrinter implements Expr.Visitor<String> {

  public String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(
            expr.operator.lexeme,
            expr.left,
            expr.right);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return null;
  }

  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return null;
  }

  /**
   * It calls accept() on each subexpression and passes in itself.
   * This is the recursive step that lets us print an entire tree.
   */
  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr e: exprs) {
      builder.append(" ");
      builder.append(e.accept(this));
    }
    builder.append(")");

    return builder.toString();
  }
}
