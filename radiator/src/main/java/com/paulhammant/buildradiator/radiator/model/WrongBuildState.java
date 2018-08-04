package com.paulhammant.buildradiator.radiator.model;

public class WrongBuildState extends BuildRadiatorException {
    public WrongBuildState() {
        super("wrong build state");
    }
}
