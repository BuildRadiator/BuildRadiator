package com.paulhammant.buildradiator.root.model;

public class IpNotAuthorized extends BuildRadiatorException {
    public IpNotAuthorized(String ipAddress) {
        super("ip address " + ipAddress + " not authorized");
    }
}
