package org.teamtators.common.tester;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class ManualTestGroup {
    private ArrayList<ManualTest> tests;
    private String name;

    public ManualTestGroup(String name, ManualTest... tests) {
        this(name, Arrays.asList(tests));
    }

    public ManualTestGroup(String name, Collection<ManualTest> tests) {
        this.name = name;
        this.tests = new ArrayList<>(tests);
    }

    public String getName() {
        return name;
    }

    public void addTest(ManualTest test) {
        tests.add(test);
    }

    public void addTests(Collection<ManualTest> tests) {
        this.tests.addAll(tests);
    }

    public void addTests(ManualTest... tests) {
        addTests(Arrays.asList(tests));
    }

    public ArrayList<ManualTest> getTests() {
        return tests;
    }
}
