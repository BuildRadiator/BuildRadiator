package com.paulhammant.buildradiator.staticresources;

import org.jooby.Jooby;

@SuppressWarnings({"unchecked", "rawtypes"})
public class BuildRadiatorStaticResources extends Jooby {

    {
        assets("/br.png", "br.png");
        assets("/favicon.png", "favicon.png");
        assets("/robots.txt", "robots.txt");
        assets("/moment.min.js", "moment.min.js");
        assets("/moment-duration-format.js", "moment-duration-format.js");
        assets("/vue.min.js", "vue.min.js");

    }

}
