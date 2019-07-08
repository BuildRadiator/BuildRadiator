package com.paulhammant.buildradiator.radiator;

import com.google.common.io.CharStreams;
import com.paulhammant.buildradiator.radiator.model.Radiator;
import com.paulhammant.buildradiator.staticresources.BuildRadiatorStaticResources;
import io.jooby.Route;
import io.jooby.Router;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestVersionOfBuildRadiatorApp extends RadiatorApp {

    public static final String CONTRIVED_PATH_FOR_TESTING = "/contrived/for/testing";

    private boolean fasterRefresh;

    {
        use(new BuildRadiatorStaticResources());
    }

    @Override
    public String getBasePath() {
        return super.getBasePath() + CONTRIVED_PATH_FOR_TESTING;
    }

    protected boolean appStarted;
    protected boolean appStopped;

    public TestVersionOfBuildRadiatorApp(Radiator testRadiator) {
        this.fasterRefresh = fasterRefresh;
        onStarted(() -> {
            if (testRadiator != null) {
                radiatorStore.actualRadiators.put(testRadiator.code, testRadiator);
            }
            appStarted = true;
            deleteDefaultRadiator();
        });
        onStop(() -> {
            appStopped = true;

        });
    }

    public TestVersionOfBuildRadiatorApp withFasterRefresh() {
        fasterRefresh = true;
        return this;
    }

    @Override
    protected void serveRadiatorPage() {

        get(getBasePath() + "/", (ctx) -> {
            String page = getStringFromResource(HTML_PAGE);
            // change from minified Vue to non-minified to allow in-page debugging
            page = page.replace("vue.min.js", "vue.js");
            // speed up refresh interval - hack radiator.html as it is send to the browser
            if (fasterRefresh) {
                page = page.replace("30000", "300");
            }
            ctx.setResponseType("text/html");
            return page;
        });

    }

    @Override
    protected void serveRadiatorComponent() {
        get(getBasePath() + "/" + "radiator.vue", (ctx) -> {
            String page = getStringFromResource(VUE_COMPONENT);
            page = page.replace("</div>\n</template>", "<h2>For Testing:</h2><pre>{{ rad }}</pre>\n</div>\n</template>");
            page = page.replace("id=\"radiator\" style=\"", "id=\"radiator\" style=\"border: 1px solid red; ");
            ctx.setResponseType("text/vue");
            return page;
        });
    }


    protected String getStringFromResource(String file) throws IOException {
        file = Router.normalizePath(file, false, false);
        InputStream stream = getClass().getResourceAsStream(file);
        return CharStreams.toString(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }


    protected void deleteDefaultRadiator() {
        radiatorStore.actualRadiators.remove(DEMO_RADIATOR_CODE);
    }

}
