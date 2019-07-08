package com.paulhammant.buildradiator.staticresources;

import io.jooby.Jooby;

@SuppressWarnings({"unchecked", "rawtypes"})
public class BuildRadiatorStaticResources extends Jooby {

    {

        get("/_ah/health", ctx ->  "yup, am healthy, Google App Engine");
        // Routes /_ah/start and /_ah/stop - not enabled on Flex containers

        assets("/br.png", "br.png");
        assets("/favicon.png", "favicon.png");
        assets("/robots.txt", "robots.txt");
        assets("/moment.min.js", "moment.min.js");
        assets("/moment-duration-format.js", "moment-duration-format.js");
        assets("/vue.js", "vue.js");
        assets("/vue.min.js", "vue.min.js");
        assets("/httpVueLoader.js", "httpVueLoader.js");
        assets("/", "index.html");

        // From https://gist.github.com/tildebyte/c85f65c1e474a6c4a6188755e710979b for LetsEncrypt
        //    assets("/well-known/acme-challenge/xxx", "well-known/acme-challenge/xxx");
        //    assets("/.well-known/acme-challenge/xxx", "well-known/acme-challenge/xxx");

    }

}
