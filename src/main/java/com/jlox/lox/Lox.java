package com.jlox.lox;

import com.jlox.lox.exception.RuntimeError;
import com.jlox.lox.grammar.string.Expr;
import com.jlox.lox.grammar.string.Stmt;
import com.jlox.lox.grammar.token.Token;
import com.jlox.lox.grammar.token.TokenType;
import com.jlox.lox.pipeline.Interpreter;
import com.jlox.lox.pipeline.Parser;
import com.jlox.lox.pipeline.Resolver;
import com.jlox.lox.pipeline.Scanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

  //Successive calls to run() inside a REPL session will use the same interpreter
  private static final Interpreter interpreter = new Interpreter();
  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  public static void main(String... args) throws IOException {
    if (args.length > 1) {
      System.exit(64); // exit codes the use conventions defined in the UNIX “sysexits.h” header
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  /**
   * Starting jlox from the CLI : reads the file and executes it.
   */
  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    if (hadError) System.exit(65);
    if (hadRuntimeError) System.exit(70);
    run(new String(bytes, Charset.defaultCharset()));
  }

  /**
   * Running the interpreter interactively : execute code one line at a time.
   */
  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (; ; ) {
      hadError = false; //reset the flag -> if user makes a mistake, session is preserved

      System.out.print(">_ ");
      String line = reader.readLine();
      if (line == null) break;
      runLineFromPrompt(line);
    }
  }

  private static void runLineFromPrompt(String line) {
    Scanner scanner = new Scanner(line);
    List<Token> tokens = scanner.scanTokens();
    if (hadError) return;

    Parser parser = new Parser(tokens);
    Resolver resolver = new Resolver(interpreter);
    Object syntax = parser.parseREPL();
    if (hadError) return;

    if (syntax instanceof List statements) {
      resolver.resolve(statements);
      if (hadError) return;
      interpreter.interpret(statements);
    } else {
      String result = interpreter.interpret((Expr) syntax);
      if (result != null) System.out.println(result);
    }
  }

  /**
   * runFile() and runPrompt() are 'wrappers' around this core method.
   */
  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();
    if (hadError) return;

    Parser parser = new Parser(tokens);
    List<Stmt> statements = parser.parse();
    if (hadError) return;

    Resolver resolver = new Resolver(interpreter);
    resolver.resolve(statements);
    if (hadError) return;

    interpreter.interpret(statements);
  }

  /**
   * Tells the user, with the help of {@link #report(int, String, String)}, that some syntax error occurred.
   */
  public static void error(int line, String message) {
    report(line, "", message);
  }

  public static void error(Token token, String message) {
    if (token.type() == TokenType.EOF) {
      report(token.line(), " at end", message);
    } else {
      report(token.line(), " at '" + token.lexeme() + "'", message);
    }
  }

  private static void report(int line,
                             String where,
                             String message) {
    System.err.println(
            "[line " + line + "] Error" + where + ":" + message);
    hadError = true;
  }

  public static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() +
            "\n[line " + error.token.line() + "]");
    hadRuntimeError = true;
  }
}
