package com.paulhammant.buildradiator;

import static org.jooby.Jooby.run;

public class BuildRadiatorMain {

    public static void main(final String[] args) {
        run(BuildRadiatorApp::new, args);
    }

}
