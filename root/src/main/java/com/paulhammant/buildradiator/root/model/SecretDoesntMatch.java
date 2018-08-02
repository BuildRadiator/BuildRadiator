package com.paulhammant.buildradiator.root.model;

public class SecretDoesntMatch extends BuildRadiatorException {
    public SecretDoesntMatch() {
        super("secret doesnt match");
    }
}
