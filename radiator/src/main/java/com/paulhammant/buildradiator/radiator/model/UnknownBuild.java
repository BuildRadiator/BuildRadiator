package com.paulhammant.buildradiator.radiator.model;

public class UnknownBuild extends BuildRadiatorException {
    public UnknownBuild() {
        super("Unknown build number");
    }
}

