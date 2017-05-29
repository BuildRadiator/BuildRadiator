package com.paulhammant.buildradiator.model;

public class WrongBuildState extends BuildRadiatorException {
    public WrongBuildState() {
        super("wrong build state");
    }
}
