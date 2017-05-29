package com.paulhammant.buildradiator;

public class TestRandomGenerator extends RandomGenerator {
    private final String code;
    private final String secret;

    public TestRandomGenerator(String code, String secret) {
        this.code = code;
        this.secret = secret;
    }

    @Override
    protected String generateRadiatorCode() {
        return code;
    }

    @Override
    protected String generateSecret() {
        return secret;
    }
}
