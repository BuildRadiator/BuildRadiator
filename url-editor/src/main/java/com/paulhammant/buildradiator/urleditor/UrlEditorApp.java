package com.paulhammant.buildradiator.urleditor;

import io.jooby.AssetHandler;
import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.RouterOptions;
import io.jooby.StatusCode;

import javax.annotation.Nonnull;

public class UrlEditorApp extends Jooby {

    {
        setRouterOptions(new RouterOptions().setIgnoreTrailingSlash(true));

        assets(getBasePath(), "url-editor/index.html");
        assets(getBasePath() + "url-editor.vue", "url-editor/url-editor.vue");
        error(StatusCode.NOT_FOUND, (ctx, cause, statusCode) -> {
            System.out.println(ctx.pathString() + " page missing from " + ctx.getRemoteAddress());
            ctx.setResponseCode(404);
            ctx.send("");
        });

        error((ctx, cause, statusCode) -> {
            cause.printStackTrace();
            System.out.println(ctx.path().value() + " page missing from " + ctx.getRemoteAddress());
            ctx.setResponseCode(404);
            ctx.send("");
        });
    }

    public String getBasePath() {
        return "/";
    }

}
