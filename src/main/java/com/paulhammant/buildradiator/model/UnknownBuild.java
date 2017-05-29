package com.paulhammant.buildradiator.model;

public class UnknownBuild extends BuildRadiatorException {
    public UnknownBuild() {
        super("Unknown build number");
    }
}

