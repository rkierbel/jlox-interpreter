package com.jlox.lox;

/**
 * Base class from which all expressions inherit.
 * Each production under expression gets a dedicated subclass.
 * Each subclass has fields for every non-terminal symbols specific to that rule.
 */
abstract class Expr {

  static class Binary extends Expr {
    final Expr left;
    final Expr right;
    final Token operator;

    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.right = right;
      this.operator = operator;
    }
  }
}
