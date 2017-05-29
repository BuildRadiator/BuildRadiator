package com.paulhammant.buildradiator;

import com.paulhammant.buildradiator.model.Radiator;

import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class TestVersionOfBuildRadiatorApp extends BuildRadiatorApp {

    protected boolean appStarted;
    protected String radCode;
    protected String radSecret;

    public TestVersionOfBuildRadiatorApp() {
        onStarted(() -> {
            loadStoreWithTestData(radiatorStore);
            appStarted = true;
        });
    }

    protected abstract void loadStoreWithTestData(RadiatorStore require);

    protected void serveIndexPageButWithReplacement(String from, String to) {
        use("").get("/r/", (request, response) -> {
            response.type("text/html");
            response.send(new String(Files.readAllBytes(Paths.get("src/main/resources/radiator.html")))
                    .replace(from, to));
        });
    }

    public static class ThatHasNoRadiators extends TestVersionOfBuildRadiatorApp {
        @Override
        protected final void loadStoreWithTestData(RadiatorStore require) {
        }
    }

    public static class ThatHasSimpleABCRadiatorCalledXXXWithOneFailedAndOneRunningBuild
            extends TestVersionOfBuildRadiatorApp {
        @Override
        protected void loadStoreWithTestData(RadiatorStore require) {
            Radiator rad = radiatorStore.createRadiator(new TestRandomGenerator("xxx", "sseeccrreett"), "A", "B", "C");
            radCode = rad.code;
            rad.startStep("1", "A");
            rad.stepFailed("1", "A");
            rad.startStep("2", "A");
            rad.builds.get(0).dur = 2000;
            rad.builds.get(1).dur = 4000;
            rad.ips = new String[]{"127.0.0.1", "111.222.33.44"};
        }
    }

    public static class ThatHasOneRadiatorAndOneBuildStarted extends TestVersionOfBuildRadiatorApp {
        private String[] stepNames;
        private String buildNum;

        public ThatHasOneRadiatorAndOneBuildStarted(String buildNum, String... stepNames) {
            this.buildNum = buildNum;
            this.stepNames = stepNames;
        }

        @Override
        protected void loadStoreWithTestData(RadiatorStore require) {
            Radiator rad = radiatorStore.createRadiator(new RandomGenerator(), stepNames);
            radCode = rad.code;
            radSecret = rad.secret;
            radiatorStore.get(radCode, "127.0.0.1").startStep(buildNum, stepNames[0]);
        }

    }
}
