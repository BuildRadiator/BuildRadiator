package com.paulhammant.buildradiator.urleditor;

import org.jooby.Jooby;

@SuppressWarnings({"unchecked", "rawtypes"})
public class UrlEditorApp extends Jooby {

    {
        assets(getBasePath(), "url-editor/index.html");
        err(404, (req, rsp, err) -> {
            System.out.println(req.route() + " page missing from " + req.ip());
            rsp.status(404);
            rsp.send("");
        });
    }

    public String getBasePath() {
        return "/";
    }

}
