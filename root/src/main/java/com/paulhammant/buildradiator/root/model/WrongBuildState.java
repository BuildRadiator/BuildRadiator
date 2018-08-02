package com.paulhammant.buildradiator.root.model;

public class WrongBuildState extends BuildRadiatorException {
    public WrongBuildState() {
        super("wrong build state");
    }
}
