package com.plagiarism.detector.mutation.strategies; // Matches: src/main/java/com/plagiarism/detector/mutation/strategies

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import java.util.HashMap;
import java.util.Map;


public class VariableRenamer extends ModifierVisitor<Void> {
    private final Map<String, String> nameMap = new HashMap<>();
    private int counter = 1;

    @Override
    public Visitable visit(VariableDeclarator n, Void arg) {

        super.visit(n, arg);

        String oldName = n.getNameAsString();

        String newName = "var_" + (counter++);
        nameMap.put(oldName, newName);


        n.setName(newName);
        return n;
    }

    @Override
    public Visitable visit(NameExpr n, Void arg) {
        super.visit(n, arg);

        String currentName = n.getNameAsString();

        if (nameMap.containsKey(currentName)) {
            n.setName(nameMap.get(currentName));
        }
        return n;
    }
}