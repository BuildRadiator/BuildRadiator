package com.paulhammant.buildradiator.radiator;

import com.paulhammant.buildradiator.radiator.model.Radiator;
import com.paulhammant.buildradiator.staticresources.BuildRadiatorStaticResources;

import java.nio.file.Files;
import java.nio.file.Paths;

public class TestVersionOfBuildRadiatorApp extends RadiatorApp {

    public static final String CONTRIVED_FOR_TESTING = "/contrived/for/testing";

    {
        use(new BuildRadiatorStaticResources());
    }

    @Override
    public String getBasePath() {
        return super.getBasePath() + CONTRIVED_FOR_TESTING;
    }

    protected boolean appStarted;

    public TestVersionOfBuildRadiatorApp(Radiator testRadiator) {
        onStarted(() -> {
            if (testRadiator != null) {
                radiatorStore.actualRadiators.put(testRadiator.code, testRadiator);
            }
            appStarted = true;
            deleteDefaultRadiator();
        });
    }

    protected void deleteDefaultRadiator() {
        radiatorStore.actualRadiators.remove(DEMO_RADIATOR_CODE);
    }

    protected void serveIndexPageButWithReplacements(String... replacements) {

        path("", () -> {
            get(getBasePath() + "/", (request, response) -> {
                String orig = new String(Files.readAllBytes(Paths.get("src/main/resources/radiator/radiator.html")));
                for (int i = 0; i < replacements.length; i = i +2) {
                    String from = replacements[i];
                    String to = replacements[i+1];
                    orig = orig.replace(from, to);
                }
                response.type("text/html");
                response.send(orig);
            });
        });
    }

}
