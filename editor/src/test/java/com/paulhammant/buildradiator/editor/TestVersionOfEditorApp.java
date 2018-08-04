package com.paulhammant.buildradiator.editor;

import com.paulhammant.buildradiator.staticresources.BuildRadiatorStaticResources;

public class TestVersionOfEditorApp extends BuildRadiatorEditor {

    public static final String CONTRIVED_FOR_TESTING = "contrived/for/testing/";

    {
        use(new BuildRadiatorStaticResources());
    }


    @Override
    public String getBasePath() {
        return super.getBasePath() + CONTRIVED_FOR_TESTING;
    }

    protected boolean appStarted;

    public TestVersionOfEditorApp() {
        onStarted(() -> {
            appStarted = true;
        });
    }

}
