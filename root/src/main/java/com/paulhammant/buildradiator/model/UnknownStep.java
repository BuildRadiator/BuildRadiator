package com.paulhammant.buildradiator.model;

public class UnknownStep extends BuildRadiatorException {
    public UnknownStep() {
        super("unknown step");
    }
}
