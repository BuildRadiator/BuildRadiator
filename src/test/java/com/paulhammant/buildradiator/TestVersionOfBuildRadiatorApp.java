package com.paulhammant.buildradiator;

import com.paulhammant.buildradiator.model.Radiator;

import java.nio.file.Files;
import java.nio.file.Paths;

public class TestVersionOfBuildRadiatorApp extends BuildRadiatorApp {

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

    protected void serveIndexPageButWithReplacement(String from, String to) {
        use("").get("/r/", (request, response) -> {
            response.type("text/html");
            response.send(new String(Files.readAllBytes(Paths.get("src/main/resources/radiator.html")))
                    .replace(from, to));
        });
    }

}
