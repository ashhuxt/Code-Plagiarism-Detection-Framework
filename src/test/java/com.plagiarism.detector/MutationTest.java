package com.plagiarism.detector;

import com.plagiarism.detector.service.SimilarityService;
import com.plagiarism.detector.mutation.MutatorEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@SpringBootTest
public class MutationTest {

    @Autowired
    private SimilarityService similarityService;

    @Test
    public void runRobustnessExperiment() throws Exception {
        MutatorEngine engine = new MutatorEngine();


        Path datasetDir = Path.of("src/main/resources/dataset");
        Path outputDir = Path.of("src/test/resources/mutants");


        if (!Files.exists(datasetDir)) {
            System.err.println("Dataset directory not found at: " + datasetDir.toAbsolutePath());
            return;
        }
        Files.createDirectories(outputDir);

        System.out.println("\n--- Robustness Experiment: Variable Renaming Mutation ---");
        System.out.printf("%-20s | %-20s%n", "FILE NAME", "SIMILARITY SCORE");
        System.out.println("-------------------------------------------------------");

        try (Stream<Path> paths = Files.list(datasetDir)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            Path mutantPath = outputDir.resolve(fileName);


                            engine.generateVariableMutant(path.toString(), mutantPath.toString());


                            String originalCode = Files.readString(path);
                            String mutatedCode = Files.readString(mutantPath);


                            double score = similarityService.calculateHybridSimilarity(originalCode, mutatedCode);


                            System.out.printf("%-20s | %.2f%%%n", fileName, score * 100);

                        } catch (Exception e) {
                            System.err.println("Error processing " + path.getFileName() + ": " + e.getMessage());
                        }
                    });
        }

        System.out.println("-------------------------------------------------------");
        System.out.println("Experiment Complete. Mutants saved to: " + outputDir.toAbsolutePath());
    }
}