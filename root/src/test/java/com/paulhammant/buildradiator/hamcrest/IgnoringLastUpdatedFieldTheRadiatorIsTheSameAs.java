package com.paulhammant.buildradiator.hamcrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paulhammant.buildradiator.model.Radiator;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.io.IOException;

import static com.paulhammant.buildradiator.RadiatorUnitTest.assertRadiatorsEqualIgnoringLastUpdatedField;
import static junit.framework.TestCase.fail;

public class IgnoringLastUpdatedFieldTheRadiatorIsTheSameAs {

    public static BaseMatcher<String> ignoringLastUpdatedFieldTheRadiatorIsTheSameAs(Radiator expected) {

        return new BaseMatcher<String>() {

            String message;

            @Override
            public boolean matches(Object o) {
                try {
                    assertRadiatorsEqualIgnoringLastUpdatedField(new ObjectMapper().readValue((String) o, Radiator.class), expected);
                    return true;
                } catch (AssertionError e) {
                    message = e.getMessage();
                } catch (IOException e) {
                    fail("IOE encountered " + e.getMessage());
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(message);
            }

        };
    }

}
