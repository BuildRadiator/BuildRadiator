package com.paulhammant.buildradiator.radiator.model;

public class NotAnIPAddress extends BuildRadiatorException {
    public NotAnIPAddress() {
        super("not an IP address");
    }
}
