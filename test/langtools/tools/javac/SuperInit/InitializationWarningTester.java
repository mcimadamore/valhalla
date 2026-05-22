/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.util.ArrayList;
import java.io.StringWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.charset.Charset;

import javax.tools.JavaFileObject;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.ListBuffer;

public class InitializationWarningTester {
    public static void main(String... args) throws Throwable {
        String testSrc = System.getProperty("test.src");
        Path baseDir = Paths.get(testSrc);
        InitializationWarningTester tester = new InitializationWarningTester();
        Assert.check(args.length > 0, "no args, ending");
        Assert.check(args.length <= 2, "unexpected number of arguments");
        String className = args[0];
        String warningsGoldenFileName = args.length > 1 ? args[1] : null;
        tester.test(baseDir, className, warningsGoldenFileName);
    }

    java.util.List<String> compilationOutput = new ArrayList<>();

    void test(Path baseDir, String className, String warningsGoldenFileName) throws Throwable {
        Path javaFile = baseDir.resolve(className + ".java");
        Path goldenFile = warningsGoldenFileName != null ? baseDir.resolve(warningsGoldenFileName) : null;

        compile(javaFile);
        if (goldenFile != null) {
            java.util.List<String> goldenFileContent = Files.readAllLines(goldenFile);
            if (goldenFileContent.size() != compilationOutput.size()) {
                System.err.println("compilation output length mismatch");
                System.err.println("    golden file content:");
                for (String s : goldenFileContent) {
                    System.err.println("        " + s);
                }
                System.err.println("    warning compilation result:");
                for (String s : compilationOutput) {
                    System.err.println("        " + s);
                }
                throw new AssertionError("compilation output length mismatch");
            }
            for (int i = 0; i < goldenFileContent.size(); i++) {
                String goldenLine = goldenFileContent.get(i);
                String warningLine = compilationOutput.get(i);
                Assert.check(warningLine.equals(goldenLine), "error, found:\n" + warningLine + "\nexpected:\n" + goldenLine);
            }
        } else {
            if (compilationOutput.size() != 0) {
                System.err.println("    expecting empty compilation output, got:");
                for (String s : compilationOutput) {
                    System.err.println("        " + s);
                }
                throw new AssertionError("expected empty compilation output");
            }
        }
    }

    void compile(Path javaFile) throws Throwable {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        Assert.checkNonNull(javaCompiler);
        StringWriter output = new StringWriter();
        try (StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(
                null, null, Charset.defaultCharset())) {
            JavacTask task = (JavacTask) javaCompiler.getTask(output, fileManager, null,
                    java.util.List.of("--enable-preview",
                            "--source", Integer.toString(Runtime.version().feature()),
                            "-Xlint:initialization",
                            "-XDrawDiagnostics"),
                    null,
                    fileManager.getJavaFileObjects(javaFile));
            for (CompilationUnitTree unit : task.parse()) {
                new SuperCallRemover().translate((JCTree)unit);
            }
            task.analyze();
        }
        for (String line : output.toString().split("\\R")) {
            if (!line.isEmpty() && !isSummaryLine(line)) {
                compilationOutput.add(line);
            }
        }
    }

    boolean isSummaryLine(String line) {
        return line.matches("\\d+ (error|errors|warning|warnings)");
    }

    static class SuperCallRemover extends TreeTranslator {
        @Override
        public void visitMethodDef(JCMethodDecl tree) {
            if (TreeInfo.isConstructor(tree)) {
                /* Remove no-arg super() calls, so javac attributes the constructor
                 * as if it had an implicit super() at the start.
                 */
                if (TreeInfo.hasAnyConstructorCall(tree)) {
                    ListBuffer<JCStatement> newStats = new ListBuffer<>();
                    for (JCStatement statement : tree.body.stats) {
                        if (statement instanceof JCExpressionStatement expressionStatement &&
                                expressionStatement.expr instanceof JCMethodInvocation methodInvocation) {
                            if (TreeInfo.isConstructorCall(methodInvocation) &&
                                methodInvocation.args.isEmpty()) {
                                continue;
                            }
                        }
                        newStats.add(statement);
                    }
                    tree.body.stats = newStats.toList();
                }
            }
            super.visitMethodDef(tree);
        }
    }
}
