package com.paulhammant.buildradiator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.paulhammant.buildradiator.model.Build;
import com.paulhammant.buildradiator.model.Radiator;
import com.paulhammant.buildradiator.model.Step;
import com.paulhammant.buildradiator.model.UnknownStep;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static com.paulhammant.buildradiator.model.TestRadBuilder.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class RadiatorUnitTest {

    @Test
    public void buildCanHaveThreeStepsAndFailOnSecond() throws InterruptedException, JsonProcessingException {
        Radiator r = new RadiatorStore().createRadiator(new TestRandomGenerator("abc123", "123"), "compile", "unit tests", "func tests");

        r.startStep("123", "compile");
        Thread.sleep(50); // test on timing too
        r.stepPassed("123", "compile");

        r.startStep("123", "unit tests");
        r.stepFailed("123", "unit tests");

        assertRadiatorsEqualIgnoringLastUpdatedField(r, rad("abc123", "123", stepNames("compile", "unit tests", "func tests"),
                build("123", "failed", 0, step("compile", 50, "passed"),
                step("unit tests", 0, "failed"), step("func tests", 0, "skipped"))));


    }

    public static void assertRadiatorsEqualIgnoringLastUpdatedField(Radiator actual, Radiator expected) {

        assertThat("Diff radiator code", actual.code, is(expected.code));
        assertThat("Diff radiator secret", actual.secret, is(expected.secret));
//        assertThat("Diff lastUpdated code", actual.lastUpdated, is(expected.lastUpdated));
        assertThat("Diff ips code", Arrays.toString(actual.ips), is(Arrays.toString(expected.ips)));
        assertThat("Diff ips stepNames", Arrays.toString(actual.stepNames), is(Arrays.toString(expected.stepNames)));
        assertThat("Diff number of builds", actual.builds.size(), is(expected.builds.size()));
        ArrayList<Build> foo = actual.builds;
        for (int i = 0; i < actual.builds.size(); i++) {
            Build build = actual.builds.get(i);
            Build build2 = expected.builds.get(i);
            assertThat("Diff build ref for build[" + i + "]", build.ref, is(build2.ref));
            assertThat("Diff status for build[" + i + "]", build.status, is(build2.status));
            assertThat("Diff number of steps for build[" + i + "]", build.steps.size(), is(build2.steps.size()));
            for (int y = 0; y < build.steps.size(); y++) {
                Step step = build.steps.get(i);
                Step step2 = build2.steps.get(i);
                assertThat("Diff name for build[" + i + "].step[" + y + "]", step.name, is(step2.name));
                assertThat("Diff duration for build[" + i + "].step[" + y + "]", ((int) step.dur / 10) * 10, is(step2.dur));
                assertThat("Diff status for build[" + i + "].step[" + y + "]", step.status, is(step2.status));
            }
        }
    }

    @Test
    public void radiatorCanOnlyHoldTenBuildsAndInReverseOrder() throws InterruptedException, JsonProcessingException {
        Radiator rad = new RadiatorStore().createRadiator(new TestRandomGenerator("X", "sseeccrreett"), "A");

        for (int i = 1; i <= 11; i++) {
            rad.startStep("" + i, "A");
        }

        assertThat(rad.builds.size(), is(10));

    }

    @Test
    public void radiatorCanIgnoreABogusStepName() throws InterruptedException, JsonProcessingException {
        Radiator rad = new RadiatorStore().createRadiator(new TestRandomGenerator("X", "sseeccrreett"), "A");
        try {
            rad.startStep("1", "NoMatch");
            fail();
        } catch (UnknownStep e) {
        }

        assertRadiatorsEqualIgnoringLastUpdatedField(rad, rad("X", "sseeccrreett", stepNames("A"),
                build("1", "", 0, step("A", 0, ""))));

    }

    private Matcher<Integer> between(int from, int to) {
        return is(both(greaterThanOrEqualTo(from)).and(lessThanOrEqualTo(to)));
    }


}
