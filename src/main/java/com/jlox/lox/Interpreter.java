package com.jlox.lox;

import java.util.List;

import static com.jlox.lox.TokenType.*;

/**
 * Provides the evaluation logic for each expression in order to produce a value from chunks of code.
 */
public class Interpreter implements Expr.Visitor<Object>,
                                    Stmt.Visitor<Void> {

  private Environment environment = new Environment();
  /**
   * Takes in a syntax tree for an expression, and evaluates it.
   *
   * @param  statements -> program is a list of statements.
   */
  void interpret(List<Stmt> statements) {
    try {
      for (Stmt stmt : statements) execute(stmt);
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private void execute(Stmt statement) {
    statement.accept(this);
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  private void executeBlock(List<Stmt> statements,
                            Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;
      //Executes a list of statements in a given current Environment
      for (Stmt stmt : statements) execute(stmt);
    } finally {
      this.environment = previous;
    }
  }

  /**
   *
   * @return Void because statements do not produce value.
   */
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    System.out.println(stringify(evaluate(stmt.expression)));
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }
    //In the absence of an initializer, the value is set to 'nil' in Lox -> null in Java
    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);
    if (List.of(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, MINUS, SLASH, STAR)
            .contains(expr.operator.type)) {
      checkNumberOperands(expr.operator, left, right);
    }

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
        if (left instanceof String l &&
                (right instanceof Double || right instanceof String)) {
          yield l + right;
        }
        throw new RuntimeError(expr.operator, "Operands must be two numbers or two Strings.");
      }
      case SLASH -> {
        if ((double) right == 0) throw new RuntimeError(expr.operator, "Division by zero!");
        else yield (double) left / (double) right;
      }
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
      case MINUS -> {
        checkNumberOperand(expr.operator, right);
        yield -(double) right;
      }
      case BANG -> !isTruthy(right);
      default -> null;
    };
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return environment.get(expr.name);
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    environment.assign(expr.name, value);

    return value;
  }

  private String stringify(Object obj) {
    if (obj == null) return "nil";
    if (obj instanceof Double) {
      String txt = obj.toString();
      if (txt.endsWith(".O")) {
        //Lox uses double-precision numbers only. For integer values, prints without decimal point
        txt = txt.substring(0, txt.length() - 2);
      }
      return txt;
    }
    return obj.toString();
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

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }
}
