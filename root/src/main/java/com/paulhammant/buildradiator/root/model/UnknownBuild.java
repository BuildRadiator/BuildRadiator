package com.paulhammant.buildradiator.root.model;

public class UnknownBuild extends BuildRadiatorException {
    public UnknownBuild() {
        super("Unknown build number");
    }
}

