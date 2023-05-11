package com.jlox.lox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class LoxTest {

  @Test
  void testScoping_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\testScriptScopes.txt");
  }

  @Test
  void testForLoop_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\testScriptForLoop.txt");
  }

  @Test
  void testBreakContinue_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\testBreakContinue.txt");
  }
}
