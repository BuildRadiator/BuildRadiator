package com.paulhammant.buildradiator;

import com.paulhammant.buildradiator.model.*;
import org.jooby.Err;
import org.jooby.Jooby;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.json.Jackson;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@SuppressWarnings({"unchecked", "rawtypes"})
public class BuildRadiatorApp extends Jooby {

    public static final String DEMO_RADIATOR_CODE = "ueeusvcipmtsb755uq";
    public static final String NO_UPDATES = "NO-UPDATES";

    protected final RadiatorStore radiatorStore;

    {

        String gae_appId = System.getenv("GCLOUD_PROJECT");

        use(new Jackson());

        if (gae_appId != null) {
            radiatorStore = new RadiatorStore.BackedByGoogleCloudDataStore();
        } else {
            radiatorStore = new RadiatorStore();
        }

        before((req, rsp) -> {
            try {
                if (gae_appId != null && req.header("X-Forwarded-Proto").value().equals("http")) {
                    rsp.redirect("https://" + req.hostname() + req.path());
                }
            } catch (Throwable throwable) {
                rsp.send(req.path()+ " (before) " + throwable.getMessage());
            }
        });

        use("/r")
                .get("/:radiatorCode/", this::getRadiatorByCode) // used by radiator.html
                .post("/:radiatorCode/stepPassed", this::stepPassed)
                .post("/:radiatorCode/stepPassedAndStartStep", this::stepPassedAndStartStep)
                .post("/:radiatorCode/stepFailed", this::stepFailed)
                .post("/:radiatorCode/startStep", this::startStep)
                .post("/:radiatorCode/buildCancelled", this::buildCancelled)
                .post("/create", this::createRadiator);

        get("/_ah/health", () -> {
            return "yup, am healthy, Google App Engine";
        });
        // Routes /_ah/start and /_ah/stop - not enabled on Flex containers

        assets("/", "index.html");
        assets("/moment.min.js", "moment.min.js");
        assets("/moment-duration-format.js", "moment-duration-format.js");
        assets("/vue.min.js", "vue.min.js");
        serveRadiatorPage();

        err(RadiatorDoesntExist.class, (req, rsp, err) -> {
            rsp.status(200);
            nothingHere(req, rsp);
        });

        err(BuildRadiatorException.class, (req, rsp, err) -> {
            rsp.status(200);
            rsp.send(err.getCause().getMessage());
        });

        err(Err.Missing.class, (req, rsp, err) -> {
            rsp.status(200);
            String message = err.getMessage();
            rsp.send(message.substring(message.indexOf(":")+2));
        });

        err((req, rsp, err) -> {
            rsp.status(200);
            nothingHere(req, rsp);
        });

        err(404, (req, rsp, err) -> {
            System.out.println(req.route() + " page missing from " + req.ip());
            rsp.status(404);
            rsp.send("");
        });

        err(405, (req, rsp, err) -> {
            System.out.println(req.route() + " blocked from " + req.ip() + ", type:" + req.type());
            rsp.status(404);
            rsp.send("");
        });

        onStart(this::starterData);

    }

    protected void serveRadiatorPage() {
        assets("/r/", "radiator.html");
        // From https://gist.github.com/tildebyte/c85f65c1e474a6c4a6188755e710979b for LetsEncrypt
        //    assets("/well-known/acme-challenge/xxx", "well-known/acme-challenge/xxx");
        //    assets("/.well-known/acme-challenge/xxx", "well-known/acme-challenge/xxx");
    }

    protected void getRadiatorByCode(Request req, Response rsp) throws Throwable {
        String lastUpdated = req.header("lastUpdated").value("");
        if (!lastUpdated.equals("")) {
            lastUpdated = new Long(lastUpdated).toString(); // ensure is a number
        }
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(req);
        Radiator radiator = getResultsStore().get(radiatorCode, req.ip());
        if (lastUpdated.equals("" + radiator.lastUpdated)) {
            rsp.status(204);
            return;
        }
        rsp.status(200).type("application/json").send(radiator.withoutSecret());
    }

    protected void startStep(Request req, Response rsp) throws Throwable {
        rsp.type("text/plain");
        String build = getBuildIdButVerifyParamFirst(req);
        String step = getStepButVerifyParamFirst(req);
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(req);
        String secret = getRadiatorSecretButVerifyParamFirst(req);
        getResultsStore().get(radiatorCode, req.ip()).verifySecret(secret).startStep(build, step);
        rsp.send("OK");
    }

    protected void stepPassed(Request req, Response rsp) throws Throwable {
        rsp.type("text/plain");
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(req);
        String build = getBuildIdButVerifyParamFirst(req);
        String step = getStepButVerifyParamFirst(req);
        String secret = getRadiatorSecretButVerifyParamFirst(req);
        getResultsStore().get(radiatorCode, req.ip()).verifySecret(secret).stepPassed(build, step);
        rsp.send("OK");
    }

    protected void stepPassedAndStartStep(Request req, Response rsp) throws Throwable {
        rsp.type("text/plain");
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(req);
        String build = getBuildIdButVerifyParamFirst(req);
        String step = getStepButVerifyParamFirst(req);
        String pStep = getPreviousStepButVerifyParamFirst(req);
        String secret = getRadiatorSecretButVerifyParamFirst(req);
        getResultsStore().get(radiatorCode, req.ip()).verifySecret(secret).stepPassed(build, pStep);
        getResultsStore().get(radiatorCode, req.ip()).verifySecret(secret).startStep(build, step);
        rsp.send("OK");

    }

    private String getRadiatorCodeButVerifyParamFirst(Request req) {
        return getParamStringAndVerify(req, "radiatorCode", 21);
    }

    private String getBuildIdButVerifyParamFirst(Request req) {
        return getParamStringAndVerify(req, "build", 12);
    }

    private String getRadiatorSecretButVerifyParamFirst(Request req) {
        return getParamStringAndVerify(req, "secret", 12);
    }

    private String getStepButVerifyParamFirst(Request req) {
        return getParamStringAndVerify(req, "step", 21);
    }

    private String getPreviousStepButVerifyParamFirst(Request req) {
        return getParamStringAndVerify(req, "pStep", 21);
    }

    private String getParamStringAndVerify(Request req, String name, int len) {
        return verifyNotTooLong(name, len, req.param(name).value());
    }

    private String verifyNotTooLong(String name, int len, String val) {
        if (val.length() > len) {
            throw new TooLong(name);
        }
        return val;
    }

    protected void stepFailed(Request req, Response rsp) throws Throwable {
        rsp.type("text/plain");
        String step = getStepButVerifyParamFirst(req);
        String build = getBuildIdButVerifyParamFirst(req);
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(req);
        String secret = getRadiatorSecretButVerifyParamFirst(req);
        getResultsStore().get(radiatorCode, req.ip()).verifySecret(secret).stepFailed(build, step);
        rsp.send("OK");
    }

    protected void buildCancelled(Request req, Response rsp) throws Throwable {
        rsp.type("text/plain");
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(req);
        String secret = getRadiatorSecretButVerifyParamFirst(req);
        getResultsStore().get(radiatorCode, req.ip()).verifySecret(secret).cancel(req.param("build").value());
        rsp.send("OK");
    }

    protected void createRadiator(Request req, Response rsp) throws Throwable {
        rsp.type("text/plain");
        String[] stepNames = req.param("stepNames").value()
                .replace("_", " ").split(",");
        for (String stepName : stepNames) {
            verifyNotTooLong("a stepName", 21, stepName);
        }
        String[] ips = req.param("ips").value("").split(",");
        if (ips.length == 1 && ips[0].equals("")) {
            ips = new String[0];
        }
        Radiator rad = getResultsStore().createRadiator(require(RandomGenerator.class), stepNames).withIpAccessRestrictedToThese(ips);
        final CreatedRadiator createdRadiator = new CreatedRadiator();
        rsp.type("application/json");
        rsp.send(createdRadiator.withCode(rad.code, rad.secret));
    }

    protected void nothingHere(Request req, Response rsp) throws Throwable {
        rsp.type("application/json").send(new ErrorMessage().withEgressIpAddress(req.ip()));
    }

    public static class ErrorMessage {

        public ErrorMessage() {
            message = "nothing here";
        }

        public final String message;
        public String egressIpAddress = "";

        public ErrorMessage withEgressIpAddress(String ip) {
            egressIpAddress = ip;
            return this;
        }
    }

    protected RadiatorStore getResultsStore() {
        return radiatorStore;
    }

    private void starterData() {
        RadiatorStore store = getResultsStore();
        String C = "Compile";
        String UT = "Unit Tests";
        String IT = "Integration Tests";
        String FT = "Functional Tests";
        String P = "Package";

        // Recreate the demo radiator each boot of the stack.
        Radiator radiator = store.createRadiator(new RandomGenerator() {
            protected String generateRadiatorCode() {
                return DEMO_RADIATOR_CODE;
            }

            @Override
            protected String generateSecret() {
                return NO_UPDATES;
            }
        }, C, UT, IT, FT, P);
        radiator.startStep("111", C);
        radiator.stepPassed("111", C);
        radiator.builds.get(0).steps.get(0).dur = 31230;
        radiator.startStep("111", UT);
        radiator.stepPassed("111", UT);
        radiator.builds.get(0).steps.get(1).dur = 46610;
        radiator.startStep("111", IT);
        radiator.stepPassed("111", IT);
        radiator.builds.get(0).steps.get(2).dur = 120000;
        radiator.startStep("111", FT);
        radiator.stepPassed("111", FT);
        radiator.builds.get(0).steps.get(3).dur = 180020;
        radiator.startStep("111", P);
        radiator.stepPassed("111", P);
        radiator.builds.get(0).steps.get(4).dur = 22200;
        radiator.builds.get(0).dur = 185000;

        radiator.startStep("112", C);
        radiator.stepPassed("112", C);
        radiator.builds.get(0).steps.get(0).dur = 33300;
        radiator.startStep("112", UT);
        radiator.stepPassed("112", UT);
        radiator.builds.get(0).steps.get(1).dur = 45500;
        radiator.startStep("112", IT);
        radiator.stepPassed("112", IT);
        radiator.builds.get(0).steps.get(2).dur = 123400;
        radiator.startStep("112", FT);
        radiator.stepPassed("112", FT);
        radiator.builds.get(0).steps.get(3).dur = 173000;
        radiator.startStep("112", P);
        radiator.stepPassed("112", P);
        radiator.builds.get(0).steps.get(4).dur = 21100;
        radiator.builds.get(0).dur = 215300;

        radiator.startStep("113", C);
        radiator.stepPassed("113", C);
        radiator.builds.get(0).steps.get(0).dur = 31000;
        radiator.startStep("113", UT);
        radiator.stepFailed("113", UT);
        radiator.builds.get(0).steps.get(1).dur = 42600;
        radiator.builds.get(0).dur = 68300;

        radiator.startStep("114", C);
        radiator.stepPassed("114", C);
        radiator.builds.get(0).steps.get(0).dur = 800;
        radiator.startStep("114", UT);
        radiator.stepPassed("114", UT);
        radiator.builds.get(0).steps.get(1).dur = 42600;
        radiator.startStep("114", IT);
        radiator.builds.get(0).dur = 43300;
    }

}
