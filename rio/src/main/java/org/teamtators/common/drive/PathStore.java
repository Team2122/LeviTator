package org.teamtators.common.drive;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PathStore {
    private final Map<String, Double> variables = new HashMap<>();
    private final Map<String, PathPoint> points = new HashMap<>();
    private final Map<String, DrivePath> paths = new HashMap<>();

    public static void main(String[] args) {
        Binding binding = new Binding();
        GroovyScriptEngine scriptEngine = null;
        try {
            scriptEngine = new GroovyScriptEngine("./config/paths");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            Object paths = scriptEngine.run("Paths.groovy", binding);
            System.out.println(paths);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}