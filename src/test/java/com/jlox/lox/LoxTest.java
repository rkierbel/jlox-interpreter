package com.jlox.lox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class LoxTest {


  @Test
  void testScriptScopes_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\testScriptScopes.txt");
  }

  @Test
  void testForLoop_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\testScriptForLoop.txt");
  }

  @Test
  void testBreakContinue_RunFromFile() throws IOException {
    //continue fails - infinite loop check token handling
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\testBreakContinue.txt");
  }

  @Test
  void testBadReturnStatement_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\badReturnStatement.txt");
  }

  @Test
  void testFibFunction_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\fibFunction.txt");
  }

  @Test
  void testCounterFunction_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\counterFunction.txt");
  }

  @Test
  void testPropertyAccessMethod_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\propertyAccessMethod.txt");
  }

  @Test
  void testMethodRefUserDefField_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\methodRefUserDefField.txt");
  }

  @Test
  void testMethodRefUserDefField2_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\methodRefUserDefField2.txt");
  }

  @Test
  void testBasicClass_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\basicClass.txt");
  }

  @Test
  void testBasicInstance_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\basicInstance.txt");
  }

  @Test
  void testInvalidThis_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\invalidThis.txt");
  }

  @Test
  void testBasicThis_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\basicThis.txt");
  }

  @Test
  void testBasicThis2_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\basicThis2.txt");
  }

  @Test
  void testCallbackThis_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\callbackThis.txt");
  }

  @Test
  void testBasicMethodInheritance_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\basicMethodInheritance.txt");
  }
}
