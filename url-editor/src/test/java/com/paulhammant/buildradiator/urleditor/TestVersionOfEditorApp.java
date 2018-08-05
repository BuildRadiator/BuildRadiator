package com.paulhammant.buildradiator.urleditor;

import com.paulhammant.buildradiator.staticresources.BuildRadiatorStaticResources;

public class TestVersionOfEditorApp extends UrlEditorApp {

    public static final String CONTRIVED_PATH_FOR_TESTING = "contrived/for/testing/";

    {
        use(new BuildRadiatorStaticResources());
    }


    @Override
    public String getBasePath() {
        return super.getBasePath() + CONTRIVED_PATH_FOR_TESTING;
    }

    protected boolean appStarted;

    public TestVersionOfEditorApp() {
        onStarted(() -> {
            appStarted = true;
        });
    }

}
