package com.paulhammant.buildradiator.radiator;

import com.paulhammant.buildradiator.radiator.model.Build;
import com.paulhammant.buildradiator.radiator.model.CreatedRadiator;
import com.paulhammant.buildradiator.radiator.model.Radiator;
import com.paulhammant.buildradiator.radiator.model.Step;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static com.paulhammant.buildradiator.radiator.RadiatorApp.DEMO_RADIATOR_CODE;
import static com.paulhammant.buildradiator.radiator.RadiatorApp.NO_UPDATES;
import static com.paulhammant.buildradiator.radiator.TestVersionOfBuildRadiatorApp.CONTRIVED_PATH_FOR_TESTING;
import static com.paulhammant.buildradiator.radiator.hamcrest.HasNewRadiator.captureCreatedRadiator;
import static com.paulhammant.buildradiator.radiator.hamcrest.IgnoringLastUpdatedFieldTheRadiatorIsTheSameAs.ignoringLastUpdatedFieldTheRadiatorIsTheSameAs;
import static com.paulhammant.buildradiator.radiator.model.TestRadBuilder.*;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.lang.String.join;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RadiatorIntegrationTest {

    private TestVersionOfBuildRadiatorApp app;

    @Before
    public void setup() {
        this.app = null;
    }

    @Test
    public void knownCodeHasListOfBuildsAvailableAsJson() throws InterruptedException {

        Radiator rad = rad("RAD_CODE", "a_secret", stepNames("A", "B", "C"),
                build("1", "running", 0, step("A", 0, "running"), step("B"), step("C")))
                .withIpAccessRestrictedToThese("127.0.0.1");

        app = new TestVersionOfBuildRadiatorApp(rad);
        startApp();

        get(CONTRIVED_PATH_FOR_TESTING + "/RAD_CODE" )
                .then()
                .assertThat()
                .body(ignoringLastUpdatedFieldTheRadiatorIsTheSameAs(rad.withoutSecret()))
                .statusCode(200)
                .contentType("application/json;charset=UTF-8");
    }

    @Test
    public void radiatorCanBeCreatedAndTransformUnderscoresInStepNames() {

        app = new TestVersionOfBuildRadiatorApp(null);
        startApp();

        CreatedRadiator createdRadiator = new CreatedRadiator();
        given()
                .params("stepNames", join(",", "P_P", "Q q", "R-R"))
            .when()
                .post(CONTRIVED_PATH_FOR_TESTING + "/create")
            .then()
                .statusCode(200)
                .body(captureCreatedRadiator(createdRadiator));

        Radiator rad = app.getResultsStore().actualRadiators.values().iterator().next();
        assertThat(rad.code, equalTo(createdRadiator.code));
        assertThat(rad.secret, equalTo(createdRadiator.secret));
        assertThat(rad.stepNames, equalTo(stepNames("P P", "Q q", "R-R")));

    }

    @Test
    public void radiatorCannotBeCreatedWithStepNamesThatAreTooLong() {

        app = new TestVersionOfBuildRadiatorApp(null);
        startApp();

        given()
                .params("stepNames", join(",", "short1", "short 2", "1234567890123456789012"))
            .when()
                .post(CONTRIVED_PATH_FOR_TESTING + "/create")
            .then()
                .statusCode(200)
                .body(equalTo("a stepName parameter too long"));
    }

    @Test
    public void radiatorCanBeCreatedWithStepNamesThatAreNotTooLong() {

        app = new TestVersionOfBuildRadiatorApp(null);
        startApp();

        CreatedRadiator createdRadiator = new CreatedRadiator();
        given()
                .params("stepNames", join(",", "short1", "short 2", "short 333"))
            .when()
                .post(CONTRIVED_PATH_FOR_TESTING + "/create")
            .then()
                .statusCode(200)
                .body(captureCreatedRadiator(createdRadiator));

        Radiator rad = app.getResultsStore().actualRadiators.values().iterator().next();
        assertThat(rad.code, equalTo(createdRadiator.code));
        assertThat(rad.secret, equalTo(createdRadiator.secret));
        assertThat(rad.stepNames, equalTo(stepNames("short1", "short 2", "short 333")));
    }

    @Test
    public void radiatorCanBeCreatedWithRestrictedIpAddressesAndStillAllowAccess() {

        Radiator rad = rad("aaa", "sseeccrreett", stepNames("A"))
                .withIpAccessRestrictedToThese("127.0.0.1");

        app = new TestVersionOfBuildRadiatorApp(rad);
        startApp();

        postStepStartedAndConfirmTestResponse("aaa", "sseeccrreett", "1", "A", 200, "OK");
    }

    @Test
    public void radiatorCanBeCreatedWithRestrictedIpAddressesAndBlockAccess() {

        Radiator rad = rad("aaa", "sseeccrreett", stepNames("A"))
                .withIpAccessRestrictedToThese("111.111.111.111", "222.222.222.222");

        app = new TestVersionOfBuildRadiatorApp(rad);
        startApp();

        postStepStartedAndConfirmTestResponse("aaa", "sseeccrreett", "1", "A", 200, "ip address 127.0.0.1 not authorized");
    }

    @Test
    public void radiatorCanBeUpdatedWithRestrictedIpAddresses() {

        Radiator rad = rad("aaa", "sseeccrreett", stepNames("A"));

        app = new TestVersionOfBuildRadiatorApp(rad);
        startApp();

        given()
                .params( "ips", "111.111.111.111", "secret", "sseeccrreett")
            .when()
                .post(CONTRIVED_PATH_FOR_TESTING +"/aaa/updateIps")
            .then()
                .statusCode(200)
                .body(equalTo("OK"));


        postStepStartedAndConfirmTestResponse("aaa", "sseeccrreett", "1", "A", 200, "ip address 127.0.0.1 not authorized");
    }

    @Test
    public void radiatorCanBeUpdatedWithWithBuildStart() {

        Radiator rad = rad("aaa", "sseeccrreett", stepNames("A"));

        app = new TestVersionOfBuildRadiatorApp(rad);
        startApp();

        postStepStartedAndConfirmTestResponse("aaa", "sseeccrreett", "1", "A", 200, "OK");

        assertThat(rad.builds.get(0).steps.get(0).status, equalTo("running"));
    }

    @Test
    public void radiatorCannotBeUpdatedWithWithBuildStartIfTheSecretIsWrong() {

        Radiator rad = rad("aaa", "sseeccrreett", stepNames("A"));

        app = new TestVersionOfBuildRadiatorApp(rad);
        startApp();

        postStepStartedAndConfirmTestResponse("aaa", "wrong-secret", "1", "A", 200, "secret doesnt match");

        assertThat(rad.builds.size(), equalTo(0));
    }

    @Test
    public void radiatorCannotBeUpdatedWithWithBuildStartIfTheSecretIsTooLong() {

        Radiator rad = rad("aaa", "sseeccrreett", stepNames("A"));

        app = new TestVersionOfBuildRadiatorApp(rad);
        startApp();

        postStepStartedAndConfirmTestResponse("aaa", "secret--way--too--long", "1", "A", 200, "secret parameter too long");

        assertThat(rad.builds.size(), equalTo(0));
    }

    @Test
    public void demoRadiatorCannotBeUpdated() {

        app = new TestVersionOfBuildRadiatorApp(null) {
            @Override
            protected void deleteDefaultRadiator() {
                // nope, don't
            }
        };
        startApp();

        assertNotNull(app.radiatorStore.get(DEMO_RADIATOR_CODE, "127.0.0.1"));

        for (String barredOperation : stepNames("startStep", "stepPassed", "stepFailed", "buildCancelled")) {
            given()
                    .params("build", "1", "step", "Compile", "secret", NO_UPDATES)
                .when()
                    .post(CONTRIVED_PATH_FOR_TESTING + "/" + DEMO_RADIATOR_CODE + "/" + barredOperation)
                .then()
                    .statusCode(200)
                    .body(equalTo("secret doesnt match")); // in lieu of specific method
        }

    }

    @Test
    public void radiatorCanBeUpdatedWithWithCancelledBuild() {

        Radiator rad = rad("aaa", "sseeccrreett", stepNames("A", "B"),
                build("1", "running", 0, step("A", 0, "running"), step("B")));

        app = new TestVersionOfBuildRadiatorApp(rad);
        startApp();

        given()
                .params("build", "1", "step", "A", "secret", "sseeccrreett")
            .when()
                .post(CONTRIVED_PATH_FOR_TESTING + "/aaa/buildCancelled")
            .then()
                .statusCode(200)
                .body(equalTo("OK"));

        Build build = rad.builds.get(0);
        assertThat(build.status, equalTo("cancelled"));
        ArrayList<Step> steps = build.steps;
        assertThat(steps.get(0).name, equalTo("A"));
        assertThat(steps.get(0).status, equalTo("cancelled"));
        assertThat(steps.get(1).name, equalTo("B"));
        assertThat(steps.get(1).status, equalTo("cancelled"));
    }

    @Test
    public void radiatorCanBeUpdatedWithWithBuildStepCompletion() {

        Radiator rad = rad("aaa", "sseeccrreett", stepNames("A", "B"),
                build("1", "running", 0, step("A", 0, "running"), step("B")));

        app = new TestVersionOfBuildRadiatorApp(rad);
        startApp();

        given()
                .params("build", "1", "step", "A", "secret", "sseeccrreett")
            .when()
                .post(CONTRIVED_PATH_FOR_TESTING + "/aaa/stepPassed")
            .then()
                .statusCode(200)
                .body(equalTo("OK"));

        ArrayList<Step> steps = rad.builds.get(0).steps;
        assertThat(steps.get(0).name, equalTo("A"));
        assertThat(steps.get(0).status, equalTo("passed"));

    }

    @Test
    public void radiatorCanBeUpdatedWithWithBuildStepFailure() {

        Radiator rad = rad("aaa", "sseeccrreett", stepNames("A", "B"),
                build("1", "running", 0, step("A", 0, "running"), step("B")));

        app = new TestVersionOfBuildRadiatorApp(rad);
        startApp();

        given()
                .params("build", "1", "step", "A", "secret", "sseeccrreett")
            .when()
                .post(CONTRIVED_PATH_FOR_TESTING + "/aaa/stepFailed")
            .then()
                .statusCode(200)
                .body(equalTo("OK"));

        Build build = rad.builds.get(0);
        assertThat(build.status, equalTo("failed"));
        assertThat(build.steps.get(0).status, equalTo("failed"));
        assertThat(build.steps.get(1).status, equalTo("skipped"));

    }

    @Test
    public void radiatorCannotBeUpdatedWithWithBuildStartForBogusStep() {

        Radiator rad = rad("aaa", "sseeccrreett", stepNames("A"));

        app = new TestVersionOfBuildRadiatorApp(rad);
        startApp();

        postStepStartedAndConfirmTestResponse("aaa", "sseeccrreett", "1", "DoesntExist", 200, "unknown step");

        Step step = rad.builds.get(0).steps.get(0);
        assertThat(step.status, equalTo(""));

    }

    @Test
    public void buildCannotBeStartedTwice() {

        Radiator rad = rad("aaa", "sseeccrreett", stepNames("A"),
                build("222", "running", 0, step("A", 0, "running")));

        app = new TestVersionOfBuildRadiatorApp(rad);
        startApp();

        postStepStartedAndConfirmTestResponse("aaa", "sseeccrreett", "222", "A", 200, "wrong build state");

    }

    @Test
    public void unknownCodeHasNoBuildsList() {

        app = new TestVersionOfBuildRadiatorApp(null);
        startApp();

        when()
                .get(CONTRIVED_PATH_FOR_TESTING + "/wewwewwewe/")
            .then()
                .assertThat()
                .body(equalTo(("{\"message\":\"nothing here\",\"egressIpAddress\":\"127.0.0.1\"}")))
                .statusCode(200)
                .contentType("application/json;charset=UTF-8");
    }

    @Test
    public void listOfCodesNotAllowed() {

        app = new TestVersionOfBuildRadiatorApp(null);
        startApp();

        when()
                .get(CONTRIVED_PATH_FOR_TESTING + "/")
            .then()
                .assertThat()
                .body(containsString("<title>Build Radiator</title>"))
                .statusCode(200)
                .contentType("text/html;charset=UTF-8");
    }

    @Test
    public void radiatorStoreCannotBeGotIfCodeIsTooLong() {

        app = new TestVersionOfBuildRadiatorApp(null);
        startApp();

        when()
                .get(CONTRIVED_PATH_FOR_TESTING + "/12345678901234567890123")
            .then()
                .assertThat()
                .body(equalTo("\"radiatorCode parameter too long\""))
                .statusCode(200)
                .contentType("application/json;charset=UTF-8");
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

    private void postStepStartedAndConfirmTestResponse(String radCode, String secret, String buildId, String stepName, int expectedStatusCode, String expectedBody) {
        given()
                .params("build", buildId, "step", stepName, "secret", secret)
            .when()
                .post(CONTRIVED_PATH_FOR_TESTING + "/" + radCode + "/startStep")
            .then()
                .statusCode(expectedStatusCode)
                .body(equalTo(expectedBody));
    }


}
