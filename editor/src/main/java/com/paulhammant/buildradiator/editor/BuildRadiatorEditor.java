package com.paulhammant.buildradiator.editor;

import org.jooby.Jooby;

@SuppressWarnings({"unchecked", "rawtypes"})
public class BuildRadiatorEditor extends Jooby {

    {
        assets("/vue.min.js", "vue.min.js");
        assets("/", "index.html");
        err(404, (req, rsp, err) -> {
            System.out.println(req.route() + " page missing from " + req.ip());
            rsp.status(404);
            rsp.send("");
        });
    }

}
