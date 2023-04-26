package com.jlox.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {

  private static final String fourSpaces = "    ";
  private static final String twoSpaces = "  ";

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }
    String outputDir = args[0];
    defineAst(outputDir, "Expr", Arrays.asList(
            "Binary   : Expr left, Token operator, Expr right",
            "Grouping : Expr expression",
            "Literal  : Object value",
            "Unary    : Token operator, Expr right"
    ));
  }

  private static void defineAst(String outputDir,
                                String baseName,
                                List<String> types) throws IOException {
    String path = outputDir + "\\" + baseName + ".java";

    try (var writer = new PrintWriter(path, StandardCharsets.UTF_8)) {
      writer.println("package com.jlox.lox;");
      writer.println();
      writer.println("import java.util.List;");
      writer.println();
      writer.println("abstract class " + baseName + " {");

      defineVisitor(writer, baseName, types);

      for (String type : types) {
        String className = type.split(":")[0].trim();
        String fields = type.split(":")[1].trim();
        defineType(writer, baseName, className, fields);
      }

      writer.println();
      writer.println(twoSpaces + "abstract <R> R accept(Visitor<R> visitor);");

      writer.println("}");
    }
  }

  private static void defineVisitor(PrintWriter writer,
                                    String baseName,
                                    List<String> types) {
    writer.println(twoSpaces + "interface Visitor<R> {");
    for (String type : types) {
      String typeName = type.split(":")[0].trim();
      writer.println(fourSpaces + "R visit" + typeName + baseName + "(" +
              typeName + " " + baseName.toLowerCase()  +");");
    }
    writer.println("}");
  }

  private static void defineType(PrintWriter writer,
                                 String baseName,
                                 String className,
                                 String fields) {
    writer.println(
            twoSpaces + "static class " + className +
            " extends " + baseName + " {");

    writer.println(fourSpaces + className + "(" + fields + ") {");
    String[] fieldsArr = fields.split(", ");
    for (String field : fieldsArr) {
      String name = field.split(" ")[1];
      writer.println(fourSpaces + twoSpaces + "this." + name + " = " + name + ";");
    }
    writer.println(fourSpaces + "}");

    writer.println();
    writer.println(fourSpaces + "@Override");
    writer.println(fourSpaces + "<R> R accept(Visitor<R> visitor) {");
    writer.println(fourSpaces + "return visitor.visit" +
            className + baseName + "(this);");
    writer.println(fourSpaces + "}");

    for (String field : fieldsArr) {
      writer.println(fourSpaces + "final " + field + ";");
    }
    writer.println(twoSpaces + "}");
  }
}
