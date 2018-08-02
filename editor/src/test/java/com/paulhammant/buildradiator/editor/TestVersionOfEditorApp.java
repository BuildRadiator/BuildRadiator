package com.paulhammant.buildradiator.editor;

public class TestVersionOfEditorApp extends BuildRadiatorEditor {

    protected boolean appStarted;

    public TestVersionOfEditorApp() {
        onStarted(() -> {
            appStarted = true;
        });
    }

}
