package com.paulhammant.buildradiator.model;

public class NotAnIPAddress extends BuildRadiatorException {
    public NotAnIPAddress() {
        super("not an IP address");
    }
}
