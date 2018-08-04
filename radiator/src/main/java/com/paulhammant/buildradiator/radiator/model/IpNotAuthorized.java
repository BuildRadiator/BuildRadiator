package com.paulhammant.buildradiator.radiator.model;

public class IpNotAuthorized extends BuildRadiatorException {
    public IpNotAuthorized(String ipAddress) {
        super("ip address " + ipAddress + " not authorized");
    }
}
