package com.paulhammant.buildradiator.radiator.model;

public class UnknownStep extends BuildRadiatorException {
    public UnknownStep() {
        super("unknown step");
    }
}
