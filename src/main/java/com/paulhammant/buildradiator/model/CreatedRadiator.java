package com.paulhammant.buildradiator.model;

public class CreatedRadiator {
    public String code;
    public String secret;

    public CreatedRadiator withCode(String code, String secret) {
        this.code = code;
        this.secret = secret;
        return this;
    }

}
