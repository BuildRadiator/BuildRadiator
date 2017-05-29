package com.paulhammant.buildradiator.model;

public class IpNotAuthorized extends BuildRadiatorException {
    public IpNotAuthorized(String ipAddress) {
        super("ip address " + ipAddress + " not authorized");
    }
}
