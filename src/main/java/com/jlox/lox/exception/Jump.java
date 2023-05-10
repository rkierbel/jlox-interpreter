package com.jlox.lox.exception;

public class Jump {

  private static class JumpException extends RuntimeException {
    protected JumpException() {

    }
  }
  public static class Break extends JumpException {

  }

  public static class Continue extends JumpException {

  }
}
