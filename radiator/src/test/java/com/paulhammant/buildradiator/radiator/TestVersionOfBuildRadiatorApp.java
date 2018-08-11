package com.paulhammant.buildradiator.radiator;

import com.google.common.io.CharStreams;
import com.paulhammant.buildradiator.radiator.model.Radiator;
import com.paulhammant.buildradiator.staticresources.BuildRadiatorStaticResources;
import org.jooby.Route;

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

    public TestVersionOfBuildRadiatorApp(Radiator testRadiator) {
        this.fasterRefresh = fasterRefresh;
        onStarted(() -> {
            if (testRadiator != null) {
                radiatorStore.actualRadiators.put(testRadiator.code, testRadiator);
            }
            appStarted = true;
            deleteDefaultRadiator();
        });
    }

    public TestVersionOfBuildRadiatorApp withFasterRefresh() {
        fasterRefresh = true;
        return this;
    }

    @Override
    protected void serveRadiatorPage() {

        get(getBasePath() + "/", (request, response) -> {
            String page = getStringFromResource(HTML_PAGE);
            // change from minified Vue to non-minified to allow in-page debugging
            page = page.replace("vue.min.js", "vue.js");
            // speed up refresh interval - hack radiator.html as it is send to the browser
            if (fasterRefresh) {
                page = page.replace("30000", "300");
            }
            response.type("text/html");
            response.send(page);
        });

    }

    @Override
    protected void serveRadiatorComponent() {
        get(getBasePath() + "/" + "radiator.vue", (request, response) -> {
            String page = getStringFromResource(VUE_COMPONENT);
            page = page.replace("</div>\n</template>", "<h2>For Testing:</h2><pre>{{ rad | pretty }}</pre>\n</div>\n</template>");
            page = page.replace("id=\"radiator\" style=\"", "id=\"radiator\" style=\"border: 1px solid red; ");
            response.type("text/vue");
            response.send(page);
        });
    }


    protected String getStringFromResource(String file) throws IOException {
        file = Route.normalize(file);
        InputStream stream = getClass().getResourceAsStream(file);
        return CharStreams.toString(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }


    protected void deleteDefaultRadiator() {
        radiatorStore.actualRadiators.remove(DEMO_RADIATOR_CODE);
    }

}
