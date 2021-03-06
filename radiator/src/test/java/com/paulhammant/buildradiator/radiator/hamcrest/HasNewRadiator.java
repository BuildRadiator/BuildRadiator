package com.paulhammant.buildradiator.radiator.hamcrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paulhammant.buildradiator.radiator.model.CreatedRadiator;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.io.IOException;

import static java.lang.String.join;
import static junit.framework.TestCase.fail;


public class HasNewRadiator {

    public static BaseMatcher<String> captureCreatedRadiator(CreatedRadiator createdRadiator) {
        return new BaseMatcher<String>() {

            @Override
            public boolean matches(Object o) {
                try {
                    CreatedRadiator cr = new ObjectMapper().readValue((String) o, CreatedRadiator.class);
                    createdRadiator.code = cr.code;
                    createdRadiator.secret = cr.secret;
                } catch (IOException e) {
                    System.out.println("Error: Jackson couldn't parse JSON into 'CreatedRadiator' instance: " + o);
                    return false;
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
            }
        };
    }

}
