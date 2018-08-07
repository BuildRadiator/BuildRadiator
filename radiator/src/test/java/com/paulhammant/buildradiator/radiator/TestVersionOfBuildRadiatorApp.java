package com.paulhammant.buildradiator.radiator;

import com.paulhammant.buildradiator.radiator.model.Radiator;
import com.paulhammant.buildradiator.staticresources.BuildRadiatorStaticResources;

import java.nio.file.Files;
import java.nio.file.Paths;

public class TestVersionOfBuildRadiatorApp extends RadiatorApp {

    public static final String CONTRIVED_PATH_FOR_TESTING = "/contrived/for/testing";

    {
        use(new BuildRadiatorStaticResources());
    }

    @Override
    public String getBasePath() {
        return super.getBasePath() + CONTRIVED_PATH_FOR_TESTING;
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

}
