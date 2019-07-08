package com.paulhammant.buildradiator.urleditor;

import io.jooby.Jooby;
import io.jooby.StatusCode;

public class UrlEditorApp extends Jooby {

    {
        assets(getBasePath(), "url-editor/index.html");
        assets(getBasePath() + "url-editor.vue", "url-editor/url-editor.vue");
        error(StatusCode.NOT_FOUND, (ctx, cause, statusCode) -> {
            System.out.println(ctx.path() + " page missing from " + ctx.getRemoteAddress());
            ctx.setResponseCode(404);
        });
    }

    public String getBasePath() {
        return "/";
    }

}
