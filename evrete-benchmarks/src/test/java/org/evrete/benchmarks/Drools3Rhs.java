package org.evrete.benchmarks;

import java.util.LinkedList;
import java.util.List;

public class Drools3Rhs {
    private static final List<String> rules = new LinkedList<>();

    @SuppressWarnings("unused")
    public static void out(String rule) {
        rules.add(rule);
    }

    public static void reset() {
        rules.clear();
    }

    public static void assertCount(int count) {
        assert rules.size() == count : "Actual " + rules.size() + " vs expected " + count;
    }
}
