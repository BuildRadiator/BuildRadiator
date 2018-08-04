package com.paulhammant.buildradiator.radiator.model;

import org.junit.Test;

import static com.paulhammant.buildradiator.radiator.model.TestRadBuilder.build;
import static com.paulhammant.buildradiator.radiator.model.TestRadBuilder.rad;
import static com.paulhammant.buildradiator.radiator.model.TestRadBuilder.step;
import static com.paulhammant.buildradiator.radiator.model.TestRadBuilder.stepNames;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class RadiatorUnitTest {

    @Test
    public void stepPassedForUnknownBuildShouldHaveBarfed() {
        Radiator r = rad("X", "sseeccrreett", stepNames("A"), build("1", "running", 0, step("A", 0, "running")));
        try {
            r.stepPassed("2","A");
            fail("should have barfed");
        } catch (UnknownBuild e) {
            // expected
        }
    }

    @Test
    public void stepFailedForUnknownBuildShouldHaveBarfed() {
        Radiator r = rad("X", "sseeccrreett", stepNames("A"), build("1", "running", 0, step("A", 0, "running")));
        try {
            r.stepFailed("2","A");
            fail("should have barfed");
        } catch (UnknownBuild e) {
            // expected
        }
    }

    @Test
    public void buildCancellationForUnknownBuildShouldHaveBarfed() {
        Radiator r = rad("X", "sseeccrreett", stepNames("A"), build("1", "running", 0, step("A", 0, "running")));
        try {
            r.cancel("2");
            fail("should have barfed");
        } catch (UnknownBuild e) {
            // expected
        }
    }

    @Test
    public void buildCancellationForKnownBuildWorks() {
        Radiator r = rad("X", "sseeccrreett", stepNames("A"), build("1", "running", 0, step("A", 0, "running")));
        r.cancel("1");
        assertThat(r.builds.get(0).status, equalTo("cancelled"));
    }

    @Test
    public void startOfBuildStepFinishesPreviousOne() {
        Radiator r = rad("X", "sseeccrreett", stepNames("A", "B"),
                build("1", "running", 0, step("A", 0, "running"), step("B", 0, "")));
        r.startStep("1","B");
        assertThat(r.builds.get(0).steps.get(0).status, equalTo("passed"));
        assertThat(r.builds.get(0).steps.get(1).status, equalTo("running"));
    }

    @Test
    public void radiatorThatIsSentToBrowserShouldNotContainSecret() {
        Radiator r = rad("X", "sseeccrreett", stepNames("A"), build("1", "running", 0, step("A", 0, "running")));
        assertNotNull(r.secret);
        assertNull(r.withoutSecret().secret);
    }

    @Test
    public void radiatorsSecretIsUsedInVerification() {
        Radiator r = rad("X", "sseeccrreett", stepNames("A"), build("1", "running", 0, step("A", 0, "running")));
        assertThat(r.verifySecret("sseeccrreett"), sameInstance(r));
        try {
            r.verifySecret("sdfsdf");
            fail();
        } catch (SecretDoesntMatch e) {
        }
    }

    @Test
    public void radiatorsCanBeLockedToIPAddresses() {
        Radiator r = rad("X", "sseeccrreett", stepNames("A"), build("1", "running", 0, step("A", 0, "running")))
                .withIpAccessRestrictedToThese("11.22.33.44");
        r.verifyIP("11.22.33.44");
        try {
            r.verifyIP("66.66.66.66");
            fail();
        } catch (IpNotAuthorized e) {
        }
    }

    @Test
    public void radiatorsCanBeUpdatedWithNewIPAddresses() {
        Radiator r = rad("X", "sseeccrreett", stepNames("A"), build("1", "running", 0, step("A", 0, "running")))
                .withIpAccessRestrictedToThese("11.22.33.44");

        r.verifyIP("11.22.33.44");

        r.updateIps("33.44.55.66");

        assertThat(r.ips, equalTo(new String[] {"33.44.55.66"}));
        try {
            r.verifyIP("11.22.33.44");
            fail();
        } catch (IpNotAuthorized e) {
        }

    }


}
