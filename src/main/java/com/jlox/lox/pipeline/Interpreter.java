package com.jlox.lox.pipeline;

import com.jlox.lox.Lox;
import com.jlox.lox.exception.CtrlFlow;
import com.jlox.lox.exception.RuntimeError;
import com.jlox.lox.grammar.string.Expr;
import com.jlox.lox.grammar.string.Stmt;
import com.jlox.lox.grammar.token.Token;
import com.jlox.lox.object.Environment;
import com.jlox.lox.object.LoxCallable;
import com.jlox.lox.object.LoxClass;
import com.jlox.lox.object.LoxFunction;
import com.jlox.lox.object.LoxInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jlox.lox.grammar.token.TokenType.*;

/**
 * Provides the evaluation logic for each expression in order to produce a value from chunks of code.
 */
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

  public final Environment globals = new Environment(); //Fixed reference to the outermost global environment
  private Environment environment = globals; //Tracks the current environment
  private final Map<Expr, Integer> locals = new HashMap<>();

  public Interpreter() {
    globals.define("clock", new LoxCallable() {
      @Override
      public Object call(Interpreter interpreter, List<Object> args) {
        return (double) System.currentTimeMillis() / 1000.0;
      }

      @Override
      public int arity() {
        return 0;
      }

      @Override
      public String toString() { return "<native function>";}
    });
  }
  /**
   * Takes in a syntax tree for an expression, and evaluates it.
   *
   * @param statements -> program is a list of statements.
   */
  public void interpret(List<Stmt> statements) {
    try {
      for (Stmt stmt : statements) execute(stmt);
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  public String interpret(Expr expr) {
    try {
      Object value = evaluate(expr);
      return stringify(value);
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
      return null;
    }
  }

  private void execute(Stmt statement) {
    statement.accept(this);
  }

  public void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  public void executeBlock(List<Stmt> statements,
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

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    Object superclass = null;
    if (stmt.superclass != null) {
      superclass = evaluate(stmt.superclass);
      if (!(superclass instanceof LoxClass)) {
        throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
      }
    }

    environment.define(stmt.name.lexeme(), null); //Declare class name in current environment

    if (stmt.superclass != null) {
      environment = new Environment(environment);
      environment.define("super", superclass);
    }

    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      LoxFunction function = new LoxFunction(
              method.name.lexeme().equals("init"), method, environment);
      methods.put(method.name.lexeme(), function);
    }
    //Turn class syntax node into its runtime representation
    LoxClass clazz = new LoxClass(
            stmt.name.lexeme(), (LoxClass) superclass, methods);
    if (superclass != null) {
      environment = environment.enclosing;
    }
    environment.assign(stmt.name, clazz); //Store the runtime object in the variable previously created
    return null;
  }

  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookUpVariable(expr.keyword, expr);
  }


  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    int hops = locals.get(expr);
    LoxClass superclass = (LoxClass) environment.getFromEnvt(hops, "super");
    /* the env in which 'this' is bound is always inside the env where we store 'super'
    => offsetting the scope by one looks up 'this' in super's inner env
     */
    LoxInstance obj = (LoxInstance) environment.getFromEnvt(hops - 1, "this");
    LoxFunction method = superclass.findMethod(expr.method.lexeme());
    if (method == null) {
      throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme() + "'.");
    }
    return method.bind(obj);
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object obj = evaluate(expr.object);

    if (obj instanceof LoxInstance instance)
      return instance.get(expr.name);

    //If the evaluated object is not a class instance, eg a literal, throw a runtime error
    throw new RuntimeError(expr.name, "Only instances have properties.");
  }

  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object obj = evaluate(expr.object);

    if (!(obj instanceof LoxInstance instance)) {
      throw new RuntimeError(expr.name, "Only instances have fields.");
    }

    Object value = evaluate(expr.value);
    instance.set(expr.name, value);

    return value;
  }

  /**
   * Takes a function syntax node (compile-time representation of the function).
   * Converts it to its runtime representation.
   */
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    //Captures the current environment as closure when creating the function
    LoxFunction function = new LoxFunction(false, stmt, environment);
    environment.define(stmt.name.lexeme(), function);
    return null;
  }

  /**
   * @return Void because statements do not produce value.
   */
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
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
    environment.define(stmt.name.lexeme(), value);
    return null;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);
    if (List.of(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, MINUS, SLASH, STAR)
            .contains(expr.operator.type())) {
      checkNumberOperands(expr.operator, left, right);
    }

    return switch (expr.operator.type()) {
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
        throw new RuntimeError(expr.operator, "Operands must be two numbers or left operand must be a String.");
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

    return switch (expr.operator.type()) {
      case MINUS -> {
        checkNumberOperand(expr.operator, right);
        yield -(double) right;
      }
      case BANG -> !isTruthy(right);
      default -> null;
    };
  }

  /**
   * First, eval expression for the callee (should be an identifier that looks up the function by name).
   * Then, eval each argument expression in order, store the resulting values in a list.
   * Finally, perform the call
   */
  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> args = new ArrayList<>();
    for (Expr arg : expr.arguments) {
      args.add(evaluate(arg));
    }

    if (!(callee instanceof LoxCallable function)) {
      throw new RuntimeError(expr.paren, "Can only call functions and classes.");
    }

    if (args.size() != function.arity()) {
      throw new RuntimeError(
              expr.paren,
              "Expected " + function.arity() + " arguments but got " + args.size() + ".");
    }
    return function.call(this, args);
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  private Object lookUpVariable(Token name, Expr expr) {
    //Look up the resolved distance in the map
    final Integer hops = locals.get(expr);

    if (hops != null) {
      return environment.getFromEnvt(hops, name.lexeme());
    }
    if (!globals.contains(name)) {
      throw new RuntimeError(name, "Use of undeclared variable '" + name.lexeme() + "'.");
    } //If hops is null, then must be a global variable
    return globals.get(name);
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    final Integer hops = locals.get(expr);

    if (hops != null) {
      environment.assignToEnvt(hops, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }

    return value;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  /**
   * Since Lox is dynamically typed and allows for any operand to represent truthiness.
   * As such, a logic operator will return a value with appropriate truthiness : the operands themselves.
   */
  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    //Evaluates the left operand first to see if we can short-circuit
    Object left = evaluate(expr.left);

    if (expr.operator.type() == OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }
    //Evaluates right only if it cannot short-circuit
    return evaluate(expr.right);
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      try {
        execute(stmt.body);
      } catch (CtrlFlow.Break b) {
        break;
      } catch (CtrlFlow.Continue ignored) {
        continue;
      }
    }
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) value = evaluate(stmt.value);
    throw new CtrlFlow.Return(value);
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    throw new CtrlFlow.Break();
  }

  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    throw new CtrlFlow.Continue();
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
