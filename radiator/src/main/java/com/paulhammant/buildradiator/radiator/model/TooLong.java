package com.paulhammant.buildradiator.radiator.model;

public class TooLong extends BuildRadiatorException {
    public TooLong(String message) {
        super(message + " parameter too long");
    }
}
