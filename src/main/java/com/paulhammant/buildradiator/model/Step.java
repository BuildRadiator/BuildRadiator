package com.paulhammant.buildradiator.model;

public class Step {

    public String name;
    public int dur;
    public String status = "";
    private long started;

    public Step() {
    }

    public Step(String name) {
        this.name = name;
    }

    public void start() {
        if (!status.equals("")) {
            throw new WrongBuildState();
        }
        started = System.currentTimeMillis();
        status = "running";
    }

    public void pass() {
        if (!status.equals("running")) {
            throw new WrongBuildState();
        }
        dur = (int) (System.currentTimeMillis() - started);
        status = "passed";
    }


    public void fail() {
        if (!status.equals("running")) {
            throw new WrongBuildState();
        }
        dur = (int) (System.currentTimeMillis() - started);
        status = "failed";
    }

    public void skipped() {
        status = "skipped";
    }

    @Override
    public String toString() {
        return "Step{" +
                "name='" + name + '\'' +
                ", dur=" + dur +
                ", status='" + status + '\'' +
                ", started=" + started +
                '}';
    }

    public void cancel() {
        if (status.equals("failed") || status.equals("passed")) {
            return;
        }
        if (status.equals("running")) {
            dur = (int) (System.currentTimeMillis() - started);
        }
        status = "cancelled";
    }

}
