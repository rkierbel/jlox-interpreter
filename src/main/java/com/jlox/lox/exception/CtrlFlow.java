package com.jlox.lox.exception;

public class CtrlFlow {

  private static class CtrlFlowException extends RuntimeException {

    protected CtrlFlowException() {

    }

    public CtrlFlowException(String message,
                             Throwable cause,
                             boolean enableSuppression,
                             boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
    }
  }
  public static class Break extends CtrlFlowException {

  }

  public static class Continue extends CtrlFlowException {

  }

  public static class Return extends CtrlFlowException {
    public final Object value;

    public Return(Object value) {
      super(null, null, false, false);
      this.value = value;
    }
  }
}
