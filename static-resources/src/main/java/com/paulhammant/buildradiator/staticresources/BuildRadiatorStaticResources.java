package com.paulhammant.buildradiator.staticresources;

import org.jooby.Jooby;

@SuppressWarnings({"unchecked", "rawtypes"})
public class BuildRadiatorStaticResources extends Jooby {

    {

        get("/_ah/health", () -> {
            return "yup, am healthy, Google App Engine";
        });
        // Routes /_ah/start and /_ah/stop - not enabled on Flex containers

        assets("/br.png", "br.png");
        assets("/favicon.png", "favicon.png");
        assets("/robots.txt", "robots.txt");
        assets("/moment.min.js", "moment.min.js");
        assets("/moment-duration-format.js", "moment-duration-format.js");
        if (Boolean.parseBoolean(System.getProperty("DBG", "true"))) {
            assets("/vue.js", "vue.js");
        } else {
            assets("/vue.js", "vue.min.js");
        }
        assets("/", "index.html");
    }

}
