package com.paulhammant.buildradiator.model;

import java.util.ArrayList;

public class Build {

    public String ref;
    public ArrayList<Step> steps = new ArrayList<>();
    public String status = "";
    public long started;
    public int dur;

    public Build() {
    }

    public Build(String ref, String[] stepsNames) {
        this.ref = ref;
        started = System.currentTimeMillis();
        for (String step : stepsNames) {
            this.steps.add(new Step(step));
        }
    }

    public void startStep(String step) {
        if (!status.equals("") && !status.equals("running")) {
            throw new WrongBuildState();
        }
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            if (s.name.equals(step)) {
                s.start();
                status = "running";
                if (i>0 && steps.get(i-1).status.equals("running")) {
                    steps.get(i-1).pass();
                }
                return;
            }
        }
        throw new UnknownStep();
    }

    public void stepPassed(String step) {
        if (!status.equals("running")) {
            throw new WrongBuildState();
        }
        boolean passed = true;
        boolean found = false;
        for (Step s : steps) {
            if (s.name.equals(step)) {
                s.pass();
                found = true;
            }
            if (!s.status.equals("passed")) {
                passed = false;
            }
        }
        if (!found) {
            throw new UnknownStep();
        }
        status = passed ? "passed" : status;
        dur = (int) (System.currentTimeMillis() - started);
    }

    public void stepFailed(String step) {
        if (!status.equals("running")) {
            throw new WrongBuildState();
        }
        boolean failureNoted = false;
        boolean found = false;
        for (Step s : steps) {
            if (s.name.equals(step)) {
                s.fail();
                failureNoted = true;
                found = true;
                continue;
            }
            if (failureNoted) {
                s.skipped();
            }
        }
        if (!found) {
            throw new UnknownStep();
        }
        status = "failed";
        dur = (int) (System.currentTimeMillis() - started);
    }

    public void cancel() {
        if (!status.equals("running")) {
            throw new WrongBuildState();
        }
        for (Step s : steps) {
            if (s.status.equals("passed")) {
                continue;
            }
            s.cancel();

        }
        dur = (int) (System.currentTimeMillis() - started);
        status = "cancelled";
    }
}
