package com.paulhammant.buildradiator.model;

import java.util.Collections;

public class TestRadBuilder {

    public static Radiator rad(String code, String secret, String[] stepNames, Build... builds) {
        Radiator rad = new Radiator();
        rad.code = code;
        rad.secret = secret;
        rad.ips = new String[0];
        rad.stepNames = stepNames;
        Collections.addAll(rad.builds, builds);
        return rad;
    }

    public static String[] stepNames(String... steps) {
        return steps;
    }

    public static Build build(String num, String status, Step... steps) {
        Build build = new Build();
        build.ref = num;
        build.status = status;
        Collections.addAll(build.steps, steps);
        return build;
    }

    public static Step step(String name, int dur, String status) {
        Step step = new Step(name);
        step.dur = dur;
        step.status = status;
        return step;
    }

}
