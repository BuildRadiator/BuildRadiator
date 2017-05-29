package com.paulhammant.buildradiator.model;

public class TooLong extends BuildRadiatorException {
    public TooLong(String message) {
        super(message + " parameter too long");
    }
}
