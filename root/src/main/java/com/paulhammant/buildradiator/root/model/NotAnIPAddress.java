package com.paulhammant.buildradiator.root.model;

public class NotAnIPAddress extends BuildRadiatorException {
    public NotAnIPAddress() {
        super("not an IP address");
    }
}
