package com.paulhammant.buildradiator.hamcrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paulhammant.buildradiator.model.CreatedRadiator;
import com.paulhammant.buildradiator.model.Radiator;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.io.IOException;
import java.util.Arrays;

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
                    fail("IOE encountered " + e.getMessage());
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
            }
        };
    }

}
