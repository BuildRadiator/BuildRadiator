package com.paulhammant.buildradiator.root.hamcrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paulhammant.buildradiator.root.model.CreatedRadiator;
import com.paulhammant.buildradiator.root.model.Radiator;
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
