package com.paulhammant.buildradiator.editor;

import com.paulhammant.buildradiator.staticresources.BuildRadiatorStaticResources;

public class TestVersionOfEditorApp extends BuildRadiatorEditor {

    {
        use(new BuildRadiatorStaticResources());
    }

    protected boolean appStarted;

    public TestVersionOfEditorApp() {
        onStarted(() -> {
            appStarted = true;
        });
    }

}
