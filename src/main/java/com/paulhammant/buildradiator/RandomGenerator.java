package com.paulhammant.buildradiator;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RandomGenerator {

    private final SecureRandom random = new SecureRandom();

    protected String generateRadiatorCode() {
        return new BigInteger(90, random).toString(32);
    }

    protected String generateSecret() {
        return new BigInteger(48, random).toString(32);
    }



}
