package com.paulhammant.buildradiator.main;

import com.paulhammant.buildradiator.editor.BuildRadiatorEditor;
import com.paulhammant.buildradiator.root.BuildRadiatorRoot;
import com.paulhammant.buildradiator.staticresources.BuildRadiatorStaticResources;
import org.jooby.Jooby;

import static org.jooby.Jooby.run;

public class BuildRadiatorMain extends Jooby {

    public BuildRadiatorMain() {
        System.out.println("--> REGISTERING EDITOR");
        use("/editor/", new BuildRadiatorEditor());
        System.out.println("--> REGISTERING ROOT");
        use("/", new BuildRadiatorRoot());
        use(new BuildRadiatorStaticResources());
    }

    public static void main(final String[] args) {
        Jooby.run(BuildRadiatorMain::new, args);
    }

}
