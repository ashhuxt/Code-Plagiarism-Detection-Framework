package com.plagiarism.detector.mutation; // Matches: src/main/java/com/plagiarism/detector/mutation

import com.plagiarism.detector.mutation.strategies.VariableRenamer; // Matches the new location of your strategy
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class MutatorEngine {


    public void generateVariableMutant(String inputPath, String outputPath) throws Exception {

        CompilationUnit cu = StaticJavaParser.parse(new File(inputPath));


        VariableRenamer renamer = new VariableRenamer();
        renamer.visit(cu, null);


        Files.writeString(Path.of(outputPath), cu.toString());

        System.out.println("Mutation complete: " + outputPath);
    }
}