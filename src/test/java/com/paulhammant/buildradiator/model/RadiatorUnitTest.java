package com.paulhammant.buildradiator.model;

import org.junit.Test;

import static com.paulhammant.buildradiator.model.TestRadBuilder.build;
import static com.paulhammant.buildradiator.model.TestRadBuilder.rad;
import static com.paulhammant.buildradiator.model.TestRadBuilder.step;
import static com.paulhammant.buildradiator.model.TestRadBuilder.stepNames;
import static junit.framework.TestCase.fail;

public class RadiatorUnitTest {

    @Test
    public void stepPassedForUnknownBuildShouldHaveBarfed() {
        Radiator r = rad("X", "sseeccrreett", stepNames("A"), build("1", "running", step("A", 0, "running")));
        try {
            r.stepPassed("2","A");
            fail("should have barfed");
        } catch (UnknownBuild e) {
            // expected
        }
    }

    @Test
    public void stepFailedForUnknownBuildShouldHaveBarfed() {
        Radiator r = rad("X", "sseeccrreett", stepNames("A"), build("1", "running", step("A", 0, "running")));
        try {
            r.stepFailed("2","A");
            fail("should have barfed");
        } catch (UnknownBuild e) {
            // expected
        }
    }

    @Test
    public void buildCancellationForUnknownBuildShouldHaveBarfed() {
        Radiator r = rad("X", "sseeccrreett", stepNames("A"), build("1", "running", step("A", 0, "running")));
        try {
            r.cancel("2");
            fail("should have barfed");
        } catch (UnknownBuild e) {
            // expected
        }
    }


}
