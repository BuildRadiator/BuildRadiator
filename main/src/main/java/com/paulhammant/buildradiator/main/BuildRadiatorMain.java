package com.paulhammant.buildradiator.main;

import com.paulhammant.buildradiator.editor.BuildRadiatorEditor;
import com.paulhammant.buildradiator.root.BuildRadiatorRoot;
import org.jooby.Jooby;

import static org.jooby.Jooby.run;

public class BuildRadiatorMain extends Jooby {

    public BuildRadiatorMain() {
        use("/", new BuildRadiatorRoot());
        use("/editor", new BuildRadiatorEditor());
    }

    public static void main(final String[] args) {
        Jooby.run(BuildRadiatorMain::new, args);
    }

}
