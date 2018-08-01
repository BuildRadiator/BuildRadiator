package com.paulhammant.buildradiator;

import com.paulhammant.buildradiator.model.Radiator;

import java.io.IOException;
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

    protected void serveIndexPageButWithReplacements(String... replacements) {

        path("", () -> {
            get("/r/", (request, response) -> {
                String orig = new String(Files.readAllBytes(Paths.get("src/main/resources/radiator.html")));
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
