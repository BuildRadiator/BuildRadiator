package com.paulhammant.buildradiator.main;

import com.paulhammant.buildradiator.urleditor.UrlEditorApp;
import com.paulhammant.buildradiator.radiator.RadiatorApp;
import com.paulhammant.buildradiator.staticresources.BuildRadiatorStaticResources;
import io.jooby.Jooby;

public class BuildRadiatorMain extends Jooby {

    public BuildRadiatorMain() {
        use("/url-editor/", new UrlEditorApp());
        use("/r/", new RadiatorApp());
        use(new BuildRadiatorStaticResources());
    }

    public static void main(final String[] args) {
        runApp(args, BuildRadiatorMain::new);
    }

}
