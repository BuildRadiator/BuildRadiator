package com.paulhammant.buildradiator.main;

import com.paulhammant.buildradiator.editor.EditorApp;
import com.paulhammant.buildradiator.radiator.RadiatorApp;
import com.paulhammant.buildradiator.staticresources.BuildRadiatorStaticResources;
import org.jooby.Jooby;

public class BuildRadiatorMain extends Jooby {

    public BuildRadiatorMain() {
        use("/editor/", new EditorApp());
        use("/r/", new RadiatorApp());
        use(new BuildRadiatorStaticResources());
    }

    public static void main(final String[] args) {
        Jooby.run(BuildRadiatorMain::new, args);
    }

}
