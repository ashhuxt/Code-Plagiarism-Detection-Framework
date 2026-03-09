package com.plagiarism.detector.service;

import com.plagiarism.detector.model.CodeFile;
import com.plagiarism.detector.model.SimilarityResult;
import com.plagiarism.detector.repository.CodeFileRepository;
import com.plagiarism.detector.repository.SimilarityResultRepository;
import com.plagiarism.detector.util.*;
import com.github.javaparser.ast.CompilationUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SimilarityService {

    @Autowired
    private CodeFileRepository repository;

    @Autowired
    private SimilarityResultRepository resultRepository;

    // --- API Service Methods ---
    public double compareFiles(Long file1Id, Long file2Id) {
        return compareHybrid(file1Id, file2Id);
    }

    public void compareAllFiles() {
        List<CodeFile> allFiles = repository.findAll();
        for (int i = 0; i < allFiles.size(); i++) {
            for (int j = i + 1; j < allFiles.size(); j++) {
                CodeFile f1 = allFiles.get(i);
                CodeFile f2 = allFiles.get(j);
                double score = compareHybrid(f1.getId(), f2.getId());
                // Store suspicious pairs (Threshold > 50%)
                if (score > 0.5) {
                    resultRepository.save(new SimilarityResult(f1.getId(), f2.getId(), f1.getName(), f2.getName(), score));
                }
            }
        }
    }

    public List<SimilarityResult> getSuspiciousPairs(double threshold) {
        return resultRepository.findBySimilarityScoreGreaterThan(threshold);
    }

    // --- ID-based wrappers for ExperimentRunner ---
    public double compareASTOnly(Long id1, Long id2) {
        CodeFile f1 = repository.findById(id1).orElseThrow();
        CodeFile f2 = repository.findById(id2).orElseThrow();
        return calculateASTSimilarity(f1.getCodeContent(), f2.getCodeContent());
    }

    public double compareLCSOnly(Long id1, Long id2) {
        CodeFile f1 = repository.findById(id1).orElseThrow();
        CodeFile f2 = repository.findById(id2).orElseThrow();
        return calculateLCSSimilarity(f1.getCodeContent(), f2.getCodeContent());
    }

    public double compareTFIDFOnly(Long id1, Long id2) {
        CodeFile f1 = repository.findById(id1).orElseThrow();
        CodeFile f2 = repository.findById(id2).orElseThrow();
        return calculateTFIDFSimilarity(f1.getCodeContent(), f2.getCodeContent());
    }


    public double calculateASTSimilarity(String codeA, String codeB) {
        try {
            CompilationUnit cu1 = ASTGenerator.generateAST(codeA);
            CompilationUnit cu2 = ASTGenerator.generateAST(codeB);
            return SimilarityCalculator.calculateFrequencySimilarity(ASTGenerator.extractNodeTypes(cu1), ASTGenerator.extractNodeTypes(cu2));
        } catch (Exception e) {
            return 0.0; // Return 0 if AST parsing fails
        }
    }

    public double calculateLCSSimilarity(String codeA, String codeB) {
        try {
            CompilationUnit cu1 = ASTGenerator.generateAST(codeA);
            CompilationUnit cu2 = ASTGenerator.generateAST(codeB);
            return LCSCalculator.calculateSimilarity(ASTGenerator.getPreorderTraversal(cu1), ASTGenerator.getPreorderTraversal(cu2));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public double calculateTFIDFSimilarity(String codeA, String codeB) {
        List<String> t1 = TokenExtractor.extractTokens(codeA);
        List<String> t2 = TokenExtractor.extractTokens(codeB);
        if (t1.isEmpty() || t2.isEmpty()) return 0.0;

        Map<String, Double> idf = TFIDFCalculator.computeIDF(Arrays.asList(t1, t2));
        return TFIDFCalculator.cosineSimilarity(
                TFIDFCalculator.computeTFIDF(TFIDFCalculator.computeTF(t1), idf),
                TFIDFCalculator.computeTFIDF(TFIDFCalculator.computeTF(t2), idf)
        );
    }

    public double compareHybrid(Long id1, Long id2) {
        CodeFile file1 = repository.findById(id1).orElseThrow();
        CodeFile file2 = repository.findById(id2).orElseThrow();
        return calculateHybridSimilarity(file1.getCodeContent(), file2.getCodeContent());
    }


    public double calculateHybridSimilarity(String code1, String code2) {
        if (code1 == null || code2 == null || code1.isBlank() || code2.isBlank()) return 0.0;

        // Weights: 40% Structural (LCS), 30% Frequency (AST), 30% Content (TF-IDF)
        double structural = calculateLCSSimilarity(code1, code2);
        double frequency = calculateASTSimilarity(code1, code2);
        double tfidf = calculateTFIDFSimilarity(code1, code2);

        double combinedScore = (0.40 * structural) + (0.30 * frequency) + (0.30 * tfidf);

        // Standardized rounding to 4 decimal places
        return Math.min(1.0, Math.round(combinedScore * 10000.0) / 10000.0);
    }
}