package com.jlox.lox;

/**
 * Provides the evaluation logic for each expression in order to produce a value from chunks of code.
 */
public class Interpreter implements Expr.Visitor<Object> {

  @Override
  public Object visitBinaryExpr(Expr.Binary expr)  {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    return switch (expr.operator.type) {
      case GREATER -> (double) left > (double) right;
      case GREATER_EQUAL -> (double) left >= (double) right;
      case LESS -> (double) left < (double) right;
      case LESS_EQUAL -> (double) left <= (double) right;
      case BANG_EQUAL -> !isEqual(left, right);
      case EQUAL_EQUAL -> isEqual(left, right);
      case MINUS -> (double) left - (double) right;
      case PLUS -> { //Operator is overloaded
        if (left instanceof Double l && right instanceof Double r)
          yield l + r;
        if (left instanceof String l && right instanceof String r)
          yield l + r;
        yield null;
      }
      case SLASH -> (double) left / (double) right;
      case STAR -> (double) left * (double) right;
      default -> null;
    };
  }

  /**
   * A grouping node references a single inner node (the expr contained inside the parenthesis).
   * Recursively evaluates the subexpression and returns it.
   */
  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  /**
   * Evaluate operand expression first (subexpression), apply unary operator to value produced.
   * The interpreter does a post-order traversal whereby each node evaluates its children before doing is own work.
   * MINUS negates the result of a subexpression.
   * Type cast is necessary because Lox is dynamically typed.
   */
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    return switch (expr.operator.type) {
      case MINUS -> -(double)right;
      case BANG -> !isTruthy(right);
      default -> null;
    };
  }

  /**
   * Sends the expression back into the interpreter's visitor implementation.
   */
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;
    return a.equals(b);
  }

  boolean isTruthy(Object obj) {
    if (obj == null) return false;
    if (obj instanceof Boolean b) return b;
    return true;
  }
}
