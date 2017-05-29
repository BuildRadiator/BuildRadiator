package com.paulhammant.buildradiator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paulhammant.buildradiator.model.Build;
import com.paulhammant.buildradiator.model.CreatedRadiator;
import com.paulhammant.buildradiator.model.Radiator;
import com.paulhammant.buildradiator.model.Step;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static com.paulhammant.buildradiator.BuildRadiatorApp.DEMO_RADIATOR_CODE;
import static com.paulhammant.buildradiator.BuildRadiatorApp.NO_UPDATES;
import static com.paulhammant.buildradiator.hamcrest.IgnoringLastUpdatedIsTheSameRadiatorAs.ignoringLastUpdatedIsTheSameRadiatorAs;
import static com.paulhammant.buildradiator.model.TestRadBuilder.*;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.lang.String.join;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RadiatorIntegrationTest {

    private TestVersionOfBuildRadiatorApp app;

    private final String RAD_CODE = "RAD_CODE";
    private final String SECRET = "XYZ";
    private final RandomGenerator codeGenerator = new RandomGenerator() {
        @Override
        protected String generateRadiatorCode() {
            return RAD_CODE;
        }

        @Override
        protected String generateSecret() {
            return SECRET;
        }
    };

    @Before
    public void setup() {
        this.app = null;
    }

    @Test
    public void knownCodeHasListOfBuildsAvailableAsJson() throws InterruptedException {
        app = new TestVersionOfBuildRadiatorApp() {
            @Override
            protected void loadStoreWithTestData(RadiatorStore require) {
                Radiator rad = radiatorStore.createRadiator(codeGenerator, "A", "B", "C");
                radCode = rad.code;
                radSecret = rad.secret;
                radiatorStore.get(app.radCode, "1.1.1.1").startStep("1", "A");
            }
        };
        startApp();

        Radiator expected = rad("RAD_CODE", null,
                stepNames("A", "B", "C"), build("1", "running", step("A", 0, "running"),
                        step("B", 0, ""), step("C", 0, "")));

        get("/r/" + app.radCode)
                .then()
                .assertThat()
                .body(ignoringLastUpdatedIsTheSameRadiatorAs(expected))
                .statusCode(200)
                .contentType("application/json;charset=UTF-8");
    }

    @Test
    public void radiatorCanBeCreatedAndTransformUnderscoresInStepNames() {
        app = new TestVersionOfBuildRadiatorApp.ThatHasNoRadiators();
        startApp();

        given()
                .params("stepNames", join(",", "P_P", "Q q", "R-R"))
                .when()
                .post("/r/create")
                .then()
                .statusCode(200)
                .body(hasNewRadiator("P P", "Q q", "R-R"));
    }

    @Test
    public void radiatorCannotBeCreatedWithStepNamesThatAreTooLong() {
        app = createBuildRadiatorServerWithOneRadiatorAndNoBuilds("A");
        startApp();

        given()
                .params("stepNames", join(",", "short1,short 2,1234567890123456789012"))
                .when()
                .post("/r/create")
                .then()
                .statusCode(200)
                .body(equalTo("a stepName parameter too long"));
    }

    @Test
    public void radiatorCanBeCreatedWithStepNamesThatAreNotTooLong() {
        app = createBuildRadiatorServerWithOneRadiatorAndNoBuilds("A");
        startApp();

        given()
                .params("stepNames", join(",", "short1,short 2,short 333"))
                .when()
                .post("/r/create")
                .then()
                .statusCode(200)
                .body(startsWith("{\"code"));
    }

    @Test
    public void radiatorCanBeCreatedWithRestrictedIpAddressesAndStillAllowAccess() {
        app = new TestVersionOfBuildRadiatorApp() {
            @Override
            protected void loadStoreWithTestData(RadiatorStore require) {
            }
        };
        startApp();
        app.radiatorStore.createRadiator(new TestRandomGenerator("aaa", "sseeccrreett"), "A")
                .withIpAccessRestrictedToThese("111.111.111.111", "127.0.0.1")
                .builds.add(new Build("1", stepNames("A")));
        app.radCode = "aaa";
        postStepStartedAndConfirm(200, "OK", "sseeccrreett");
    }

    @Test
    public void radiatorCanBeCreatedWithRestrictedIpAddressesAndBlockAccess() {
        app = new TestVersionOfBuildRadiatorApp() {
            @Override
            protected void loadStoreWithTestData(RadiatorStore require) {
            }
        };
        startApp();
        app.radiatorStore.createRadiator(new TestRandomGenerator("aaa", "sseeccrreett"), "A")
                .withIpAccessRestrictedToThese("111.111.111.111", "222.222.222.222")
                .builds.add(new Build("1", stepNames("A")));
        app.radCode = "aaa";
        postStepStartedAndConfirm(200, "ip address 127.0.0.1 not authorized", "sseeccrreett");
    }

    private BaseMatcher<String> hasNewRadiator(String... expectedSteps) {
        return new BaseMatcher<String>() {
            private String radCode = "unknown";

            @Override
            public boolean matches(Object o) {
                try {
                    CreatedRadiator cr = new ObjectMapper().readValue((String) o, CreatedRadiator.class);
                    radCode = cr.code;
                    Radiator rad = app.radiatorStore.get(cr.code, "127.0.0.1");
                    app.radCode = radCode;
                    app.radSecret = rad.secret;
                    return rad.code.length() > 9 && rad.secret.length() > 6 && Arrays.deepEquals(rad.stepNames, expectedSteps);
                } catch (IOException e) {
                    fail("IOE encountered " + e.getMessage());
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a radiator " + radCode + ", secret: " + app.radSecret + " with steps " + join(",", expectedSteps) + ")");
            }
        };
    }

    @Test
    public void radiatorCanBeUpdatedWithWithBuildStart() {
        app = createBuildRadiatorServerWithOneRadiatorAndNoBuilds("A");
        startApp();

        postStepStartedAndConfirm(200, "OK", app.radSecret);

        assertThat(app.radiatorStore.get(app.radCode, "127.0.0.1").builds.get(0).steps.get(0).toString(),
                startsWith("Step{name='A', dur=0, status='running', started="));
    }

    @Test
    public void radiatorCannotBeUpdatedWithWithBuildStartIfTheSecretIsWrong() {
        app = createBuildRadiatorServerWithOneRadiatorAndNoBuilds("A");
        startApp();

        postStepStartedAndConfirm(200, "secret doesnt match",  "wrong-secret");

        assertThat(app.radiatorStore.get(app.radCode, "127.0.0.1").builds.size(), equalTo(0));
    }

    @Test
    public void radiatorCannotBeUpdatedWithWithBuildStartIfTheSecretIsTooLong() {
        app = createBuildRadiatorServerWithOneRadiatorAndNoBuilds("A");
        startApp();

        postStepStartedAndConfirm(200, "secret parameter too long",  "secret--way--too--long");

        assertThat(app.radiatorStore.get(app.radCode, "127.0.0.1").builds.size(), equalTo(0));
    }

    @Test
    public void demoRadiatorCannotBeUpdated() {
        app = new TestVersionOfBuildRadiatorApp.ThatHasNoRadiators();
        startApp();
        assertNotNull(app.radiatorStore.get(DEMO_RADIATOR_CODE, "127.0.0.1"));

        for (String barred : stepNames("startStep", "stepPassed", "stepFailed", "buildCancelled")) {
            given()
                    .params("build", "1", "step", "Compile", "secret", NO_UPDATES)
                    .when()
                    .post("/r/" + DEMO_RADIATOR_CODE + "/" + barred)
                    .then()
                    .statusCode(200)
                    .body(equalTo("secret doesnt match"));
        }

    }


    @Test
    public void radiatorCanBeUpdatedWithWithCancelledBuild() {
        app = createBuildRadiatorServerWithOneRadiatorAndNoBuilds("A", "B");
        startApp();

        postStepStartedAndConfirm(200, "OK", app.radSecret);

        given()
                .params("build", "1", "step", "A", "secret", app.radSecret)
                .when()
                .post("/r/" + app.radCode + "/buildCancelled")
                .then()
                .statusCode(200)
                .body(equalTo("OK"));

        ArrayList<Step> steps = app.radiatorStore.get(app.radCode, "127.0.0.1").builds.get(0).steps;
        assertThat(steps.get(0).name, equalTo("A"));
        assertThat(steps.get(0).status, equalTo("cancelled"));
        assertThat(steps.get(1).name, equalTo("B"));
        assertThat(steps.get(1).status, equalTo("cancelled"));
    }

    @Test
    public void radiatorCanBeUpdatedWithWithBuildStepCompletion() {
        app = createBuildRadiatorServerWithOneRadiatorAndNoBuilds("A");
        startApp();

        postStepStartedAndConfirm(200, "OK", app.radSecret);

        given()
                .params("build", "1", "step", "A", "secret", app.radSecret)
                .when()
                .post("/r/" + app.radCode + "/stepPassed")
                .then()
                .statusCode(200)
                .body(equalTo("OK"));

        ArrayList<Step> steps = app.radiatorStore.get(app.radCode, "127.0.0.1").builds.get(0).steps;
        assertThat(steps.get(0).name, equalTo("A"));
        assertThat(steps.get(0).status, equalTo("passed"));

    }

    @Test
    public void radiatorCanBeUpdatedWithWithBuildStepCompletionAndStartOfNewStepInOneGo() {
        app = createBuildRadiatorServerWithOneRadiatorAndNoBuilds("A", "B");
        startApp();

        postStepStartedAndConfirm(200, "OK", app.radSecret);

        given()
                .params("build", "1", "pStep", "A", "step", "B", "secret", app.radSecret)
                .when()
                .post("/r/" + app.radCode + "/stepPassedAndStartStep")
                .then()
                .statusCode(200)
                .body(equalTo("OK"));

        ArrayList<Step> steps = app.radiatorStore.get(app.radCode, "127.0.0.1").builds.get(0).steps;
        assertThat(steps.get(0).name, equalTo("A"));
        assertThat(steps.get(0).status, equalTo("passed"));
        assertThat(steps.get(1).name, equalTo("B"));
        assertThat(steps.get(1).status, equalTo("running"));

    }

    @Test
    public void radiatorCanBeUpdatedWithWithBuildStepFailure() {
        app = createBuildRadiatorServerWithOneRadiatorAndNoBuilds("A");
        startApp();

        postStepStartedAndConfirm(200, "OK", app.radSecret);

        given()
                .params("build", "1", "step", "A", "secret", app.radSecret)
                .when()
                .post("/r/" + app.radCode + "/stepFailed")
                .then()
                .statusCode(200)
                .body(equalTo("OK"));

        String actual = app.radiatorStore.get(app.radCode, "127.0.0.1").builds.get(0).steps.get(0).toString();
        assertThat(actual, startsWith("Step{name='A', dur="));
        assertThat(actual, containsString(", status='failed', started=1"));

    }

    private void postStepStartedAndConfirm(int expectedStatusCode, String expectedBody, String secret) {
        given()
                .params("build", "1", "step", "A", "secret", secret)
                .when()
                .post("/r/" + app.radCode + "/startStep")
                .then()
                .statusCode(expectedStatusCode)
                .body(equalTo(expectedBody));
    }

    @Test
    public void radiatorCannotBeUpdatedWithWithBuildStartForBogusStep() {
        app = createBuildRadiatorServerWithOneRadiatorAndNoBuilds("A", "B", "C");
        startApp();
        given()
                .params("build", "1", "step", "DoesntExist", "secret", app.radSecret)
                .when()
                .post("/r/" + app.radCode + "/startStep")
                .then()
                .statusCode(200)
                .body(equalTo("StepNotFound"));

        assertThat(app.radiatorStore.get(app.radCode, "127.0.0.1").builds.get(0).steps.get(0).toString(),
                startsWith("Step{name='A', dur=0, status='', started=0"));

    }

    @Test
    public void buildCannotBeStartedTwice() {
        app = new TestVersionOfBuildRadiatorApp.ThatHasOneRadiatorAndOneBuildStarted("222", "A", "B");
        startApp();
        given()
                .params("build", "222", "step", "A", "secret", app.radSecret)
                .when()
                .post("/r/" + app.radCode + "/startStep")
                .then()
                .statusCode(200)
                .body(equalTo("wrong build state"));

    }

    private TestVersionOfBuildRadiatorApp createBuildRadiatorServerWithOneRadiatorAndNoBuilds(final String... stepNames) {
        return new TestVersionOfBuildRadiatorApp() {
            @Override
            protected void loadStoreWithTestData(RadiatorStore require) {
                Radiator rad = radiatorStore.createRadiator(codeGenerator, stepNames);
                radCode = rad.code;
                radSecret = rad.secret;
            }
        };
    }

    @Test
    public void unknownCodeHasNoBuildsList() {
        app = new TestVersionOfBuildRadiatorApp() {
            @Override
            protected void loadStoreWithTestData(RadiatorStore require) {
                Radiator rad = radiatorStore.createRadiator(codeGenerator, "A", "B", "C");
                radCode = rad.code;
                radSecret = rad.secret;
                radiatorStore.get(radCode, "127.0.0.1").startStep("1", "A");
            }
        };
        startApp();
        get("/r/wewwewwewe/")
                .then()
                .assertThat()
                .body(equalTo(("{\"message\":\"nothing here\",\"egressIpAddress\":\"127.0.0.1\"}")))
                .statusCode(200)
                .contentType("application/json;charset=UTF-8");
    }

    @Test
    public void listOfCodesNotAllowed() {
        app = new TestVersionOfBuildRadiatorApp() {
            @Override
            protected void loadStoreWithTestData(RadiatorStore store) {
            }
        };
        startApp();
        get("/r/")
                .then()
                .assertThat()
                .body(containsString("<title>Build Radiator</title>"))
                .statusCode(200)
                .contentType("text/html;charset=UTF-8");
    }

    @Test
    public void radiatorStoreCannotBeGotIfCodeIsTooLong() {

        final String longCode = "12345678901234567890123";

        app = new TestVersionOfBuildRadiatorApp() {
            @Override
            protected void loadStoreWithTestData(RadiatorStore store) {
                Radiator r = rad(longCode, null,
                        stepNames("A"), build("1", "running", step("A", 0, "running")));
                store.actualRadiators.put(longCode, r);
                app.radCode = longCode;
            }
        };
        startApp();

        get("/r/" + longCode)
                .then()
                .assertThat()
                .body(equalTo("\"radiatorCode parameter too long\""))
                .statusCode(200)
                .contentType("application/json;charset=UTF-8");

        assertThat(app.radCode, equalTo(longCode));
    }


    private void startApp() {
        app.start("server.join=false");
        while (!app.appStarted) {
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
            }
        }
    }

    @After
    public void stopServer() {
        app.stop();
        app = null;
    }

}
