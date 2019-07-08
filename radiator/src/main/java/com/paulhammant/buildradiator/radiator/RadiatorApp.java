package com.paulhammant.buildradiator.radiator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paulhammant.buildradiator.radiator.model.*;
import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.RouterOptions;
import io.jooby.StatusCode;
import io.jooby.json.JacksonModule;

@SuppressWarnings({"unchecked", "rawtypes"})
public class RadiatorApp extends Jooby {

    public static final String DEMO_RADIATOR_CODE = "ueeusvcipmtsb755uq";
    public static final String NO_UPDATES = "NO-UPDATES";
    public static final String HTML_PAGE = "radiator/radiator.html";
    public static final String VUE_COMPONENT = "radiator/radiator.vue";

    protected final RadiatorStore radiatorStore;
    private RandomGenerator randomGenerator = new RandomGenerator();

    public static void main(final String[] args) {
        runApp(args, RadiatorApp::new);
    }

    {

        String gae_appId = System.getenv("GCLOUD_PROJECT");

        install(new JacksonModule());

        if (gae_appId != null) {
            radiatorStore = new RadiatorStore.BackedByGoogleCloudDataStore();
        } else {
            radiatorStore = new RadiatorStore();
        }

        before((ctx) -> {
            try {
                if (gae_appId != null && ctx.header("X-Forwarded-Proto").value().equals("http")) {
                    ctx.sendRedirect("https://" + ctx.getHost() + ctx.path());
                }
            } catch (Throwable throwable) {
                ctx.send(ctx.path()+ " (before) " + throwable.getMessage());
            }
        });

        serveRadiatorPage();
        serveRadiatorComponent();

        setRouterOptions(new RouterOptions().setIgnoreTrailingSlash(true));

        path(getBasePath(), () -> {
            // used by radiator.html
            get("/{radiatorCode}/", this::getRadiatorByCode);
            post("/{radiatorCode}/stepPassed", this::stepPassed);
            post("/{radiatorCode}/stepFailed", this::stepFailed);
            post("/{radiatorCode}/startStep", this::startStep);
            post("/{radiatorCode}/buildCancelled", this::buildCancelled);
            post("/{radiatorCode}/updateIps", this::updateIps);
            post("/create", this::createRadiator);

        });

        error(RadiatorDoesntExist.class, (ctx, cause, statusCode) -> {
            ctx.setResponseCode(200);
            ctx.setResponseType("application/json");
            try {
                ctx.send(new ObjectMapper().writeValueAsString(nothingHere(ctx)));
            } catch (JsonProcessingException e) {
                throw new UnsupportedOperationException(e);
            }
        });

        error(BuildRadiatorException.class, (ctx, cause, statusCode) -> {
            ctx.send(cause.getMessage());
        });

        error(BuildRadiatorException.class, (ctx, cause, statusCode) -> {
            ctx.send(cause.getMessage());
        });

//        err(Err.Missing.class, (req, rsp, err) -> {
//            rsp.status(200);
//            String message = err.getMessage();
//            rsp.send(message.substring(message.indexOf(":")+2));
//        });

        error(StatusCode.NOT_FOUND, (ctx, cause, statusCode) -> {
            System.out.println(ctx.path() + " page missing from " + ctx.getRemoteAddress());
            ctx.setResponseCode(404);
            ctx.send("");
        });

        error((ctx, cause, statusCode) -> {
            ctx.setResponseCode(200);
            ctx.setResponseType("application/json");
            ctx.render(nothingHere(ctx));
        });

        error(StatusCode.METHOD_NOT_ALLOWED, (ctx, cause, statusCode) -> {
            System.out.println(ctx.path() + " blocked from " + ctx.getRemoteAddress() + ", type:" + ctx.getRequestType());
            ctx.setResponseCode(404);
        });

        onStarted(this::starterData);
    }

    public String getBasePath() {
        return "";
    }

    protected void serveRadiatorPage() {
        assets (getBasePath() + "/", HTML_PAGE);
    }

    protected void serveRadiatorComponent() {
        assets (getBasePath() + "/" + "radiator.vue", VUE_COMPONENT);
    }

    protected Object getRadiatorByCode(Context ctx) {
        String lastUpdated = ctx.header("lastUpdated").value("");
        if (!lastUpdated.equals("")) {
            lastUpdated = new Long(lastUpdated).toString(); // ensure is a number
        }
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(ctx);
        Radiator radiator = getResultsStore().get(radiatorCode, ctx.getRemoteAddress());
        if (lastUpdated.equals("" + radiator.lastUpdated)) {
            ctx.setResponseCode(204);
            return null;
        }
        return radiator.withoutSecret();
    }

    protected String startStep(Context ctx) {
        ctx.setResponseType("text/plain");
        String build = getBuildIdButVerifyParamFirst(ctx);
        String step = getStepButVerifyParamFirst(ctx);
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(ctx);
        String secret = getRadiatorSecretButVerifyParamFirst(ctx);
        getResultsStore().get(radiatorCode, ctx.getRemoteAddress()).verifySecret(secret).startStep(build, step);
        return "OK";
    }

    protected String stepPassed(Context ctx) {
        ctx.setResponseType("text/plain");
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(ctx);
        String build = getBuildIdButVerifyParamFirst(ctx);
        String step = getStepButVerifyParamFirst(ctx);
        String secret = getRadiatorSecretButVerifyParamFirst(ctx);
        getResultsStore().get(radiatorCode, ctx.getRemoteAddress()).verifySecret(secret).stepPassed(build, step);
        return "OK";
    }

    private String getRadiatorCodeButVerifyParamFirst(Context ctx) {
        return verifyNotTooLong("radiatorCode", 21, ctx.path("radiatorCode").value());
    }

    private String getBuildIdButVerifyParamFirst(Context ctx) {
        return getParamStringAndVerify(ctx, "build", 12);
    }

    private String getRadiatorSecretButVerifyParamFirst(Context ctx) {
        return getParamStringAndVerify(ctx, "secret", 12);
    }

    private String getStepButVerifyParamFirst(Context ctx) {
        return getParamStringAndVerify(ctx, "step", 21);
    }

    private String getPreviousStepButVerifyParamFirst(Context ctx) {
        return getParamStringAndVerify(ctx, "pStep", 21);
    }

    private String getParamStringAndVerify(Context ctx, String name, int len) {
        return verifyNotTooLong(name, len, ctx.form(name).value());
    }

    private String verifyNotTooLong(String name, int len, String val) {
        if (val.length() > len) {
            throw new TooLong(name);
        }
        return val;
    }

    protected String stepFailed(Context ctx) {
        ctx.setResponseType("text/plain");
        String step = getStepButVerifyParamFirst(ctx);
        String build = getBuildIdButVerifyParamFirst(ctx);
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(ctx);
        String secret = getRadiatorSecretButVerifyParamFirst(ctx);
        getResultsStore().get(radiatorCode, ctx.getRemoteAddress()).verifySecret(secret).stepFailed(build, step);
        return "OK";
    }

    protected String updateIps (Context ctx) {
        ctx.setResponseType("text/plain");
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(ctx);
        String secret = getRadiatorSecretButVerifyParamFirst(ctx);
        String[] ips = ctx.form("ips").value("").split(",");
        if (ips.length == 1 && ips[0].equals("")) {
            ips = new String[0];
        }
        getResultsStore().get(radiatorCode, ctx.getRemoteAddress()).verifySecret(secret).updateIps(ips);

        return "OK";
    }

    protected String buildCancelled(Context ctx) {
        ctx.setResponseType("text/plain");
        String radiatorCode = getRadiatorCodeButVerifyParamFirst(ctx);
        String secret = getRadiatorSecretButVerifyParamFirst(ctx);
        getResultsStore().get(radiatorCode, ctx.getRemoteAddress()).verifySecret(secret).cancel(ctx.form("build").value());
        return "OK";
    }

    protected Object createRadiator(Context ctx) {
        ctx.setResponseType("text/plain");
        String[] stepNames = ctx.form("stepNames").value()
                .replace("_", " ").split(",");
        for (String stepName : stepNames) {
            verifyNotTooLong("a stepName", 21, stepName);
        }
        String[] ips = ctx.form("ips").value("").split(",");
        if (ips.length == 1 && ips[0].equals("")) {
            ips = new String[0];
        }
        ctx.setResponseType("application/json");
        return getResultsStore().createRadiator(randomGenerator, stepNames)
                .withIpAccessRestrictedToThese(ips).codeAndSecretOnly();
    }

    protected Object nothingHere(Context ctx) {
        ctx.setResponseType("application/json");
        return new ErrorMessage().withEgressIpAddress(ctx.getRemoteAddress());
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
