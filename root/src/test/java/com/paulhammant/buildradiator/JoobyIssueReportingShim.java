package com.paulhammant.buildradiator;

import org.jooby.Request;
import org.jooby.Results;
import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Ignore;
import org.junit.Test;

public class JoobyIssueReportingShim extends ServerFeature {

    public static class WhoaNow extends RuntimeException {
        public WhoaNow() {
            super("whoa now");
        }
    }

    {
        use(new Jackson());

        get("/resultsOkTest", (Request req) -> {
            throw new WhoaNow();
        });

        err(WhoaNow.class, (req, rsp, x) -> {
            rsp.send(Results.ok(x.getCause().getMessage()).type("text"));
        });
    }

    @Test @Ignore
    public void iAmNotSureHowToGetThatResultsOKThingIsWorking() throws Exception {
        request()
                .get("/resultsOkTest")
                .expect(200);
    }

    @Test @Ignore
    public void iAmNotSureHowToGetThatResultsOKThingIsWorking2() throws Exception {
        request()
                .get("/resultsOkTest")
                .expect("whoa now");
    }

}