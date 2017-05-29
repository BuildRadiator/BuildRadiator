package com.paulhammant.buildradiator.hamcrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paulhammant.buildradiator.model.Radiator;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.io.IOException;

import static com.paulhammant.buildradiator.RadiatorUnitTest.assertRadiatorsEqual;
import static junit.framework.TestCase.fail;

public class IgnoringLastUpdatedIsTheSameRadiatorAs extends BaseMatcher<String> {
    private final Radiator expected;
    private ObjectMapper objectMapper = new ObjectMapper();
    private String message;

    public IgnoringLastUpdatedIsTheSameRadiatorAs(Radiator expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(Object o) {
        try {
            assertRadiatorsEqual(objectMapper.readValue((String) o, Radiator.class), expected);
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

    public static BaseMatcher<String> ignoringLastUpdatedIsTheSameRadiatorAs(Radiator expected) {
        return new IgnoringLastUpdatedIsTheSameRadiatorAs(expected);
    }

}
