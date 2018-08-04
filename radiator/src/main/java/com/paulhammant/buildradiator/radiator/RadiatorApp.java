package com.paulhammant.buildradiator.radiator;

import com.paulhammant.buildradiator.radiator.model.*;
import org.jooby.Err;
import org.jooby.Jooby;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.json.Jackson;

@SuppressWarnings({"unchecked", "rawtypes"})
public class RadiatorApp extends Jooby {

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

        path(getBasePath(), () -> {
            // used by radiator.html
            get("/:radiatorCode/", this::getRadiatorByCode);
            post("/:radiatorCode/stepPassed", this::stepPassed);
            post("/:radiatorCode/stepFailed", this::stepFailed);
            post("/:radiatorCode/startStep", this::startStep);
            post("/:radiatorCode/buildCancelled", this::buildCancelled);
            post("/:radiatorCode/updateIps", this::updateIps);
            post("/create", this::createRadiator);

        });

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

    public String getBasePath() {
        return "/r";
    }

    protected void serveRadiatorPage() {
        assets (getBasePath() + "/", "radiator/radiator.html");
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

    protected void updateIps (Request req, Response rsp) throws Throwable {
        rsp.type("text/plain");
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(req);
        String secret = getRadiatorSecretButVerifyParamFirst(req);
        String[] ips = req.param("ips").value("").split(",");
        if (ips.length == 1 && ips[0].equals("")) {
            ips = new String[0];
        }
        getResultsStore().get(radiatorCode, req.ip()).verifySecret(secret).updateIps(ips);

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
        rsp.type("application/json");
        rsp.send(getResultsStore().createRadiator(require(RandomGenerator.class), stepNames)
                .withIpAccessRestrictedToThese(ips).codeAndSecretOnly());
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

        //  c = compile
        //  u = unit tests
        //  i = integration tests
        //  f = functional tests
        //  p = package

        // Recreate the demo radiator each boot of the stack.
        Radiator radiator = store.createRadiator(new RandomGenerator() {
            protected String generateRadiatorCode() {
                return DEMO_RADIATOR_CODE;
            }

            @Override
            protected String generateSecret() {
                return NO_UPDATES;
            }
        }, "c", "u", "i", "f", "p");

        radiator.startStep("111", "c");
        radiator.stepPassed("111", "c");
        radiator.builds.get(0).steps.get(0).dur = 31230;
        radiator.startStep("111", "u");
        radiator.stepPassed("111", "u");
        radiator.builds.get(0).steps.get(1).dur = 46610;
        radiator.startStep("111", "i");
        radiator.stepPassed("111", "i");
        radiator.builds.get(0).steps.get(2).dur = 120000;
        radiator.startStep("111", "f");
        radiator.stepPassed("111", "f");
        radiator.builds.get(0).steps.get(3).dur = 180020;
        radiator.startStep("111", "p");
        radiator.stepPassed("111", "p");
        radiator.builds.get(0).steps.get(4).dur = 22200;
        radiator.builds.get(0).dur = 185000;

        radiator.startStep("112", "c");
        radiator.stepPassed("112", "c");
        radiator.builds.get(0).steps.get(0).dur = 33300;
        radiator.startStep("112", "u");
        radiator.stepPassed("112", "u");
        radiator.builds.get(0).steps.get(1).dur = 45500;
        radiator.startStep("112", "i");
        radiator.stepPassed("112", "i");
        radiator.builds.get(0).steps.get(2).dur = 123400;
        radiator.startStep("112", "f");
        radiator.stepPassed("112", "f");
        radiator.builds.get(0).steps.get(3).dur = 173000;
        radiator.startStep("112", "p");
        radiator.stepPassed("112", "p");
        radiator.builds.get(0).steps.get(4).dur = 21100;
        radiator.builds.get(0).dur = 215300;

        radiator.startStep("113", "c");
        radiator.stepPassed("113", "c");
        radiator.builds.get(0).steps.get(0).dur = 31000;
        radiator.startStep("113", "u");
        radiator.stepFailed("113", "u");
        radiator.builds.get(0).steps.get(1).dur = 42600;
        radiator.builds.get(0).dur = 68300;

        radiator.startStep("114", "c");
        radiator.stepPassed("114", "c");
        radiator.builds.get(0).steps.get(0).dur = 800;
        radiator.startStep("114", "u");
        radiator.stepPassed("114", "u");
        radiator.builds.get(0).steps.get(1).dur = 42600;
        radiator.startStep("114", "i");
        radiator.builds.get(0).dur = 43300;
    }

}
