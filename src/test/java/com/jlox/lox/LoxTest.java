package com.jlox.lox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class LoxTest {

  @Test
  void test_RunFromFile() throws IOException {
    Lox.main("C:\\Users\\rkierbel\\IdeaProjects\\jlox\\src\\main\\resources\\testScriptOne.txt");
  }
}
