package greenbuild;

import org.jooby.Jooby;
import org.jooby.Request;
import org.jooby.json.Jackson;

@SuppressWarnings({"unchecked", "rawtypes" })
public class GreenBuildMain extends Jooby {

  {

    /** JSON: */
    use(new Jackson());

    /** CORS: */
    use("*", (req, rsp) -> {
      rsp.header("Access-Control-Allow-Origin", "*");
      rsp.header("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PATCH");
      rsp.header("Access-Control-Max-Age", "3600");
      rsp.header("Access-Control-Allow-Headers", "x-requested-with", "origin", "content-type",
          "accept");
      if (req.method().equalsIgnoreCase("options")) {
        rsp.end();
      }
    });

    /* Todo API: */
    use("/p")
        /* List all codes. */
        .get(this::nothingHere)
        /* Get todo by code. */
        .get("/:code", this::getRadiatorByCode);

    assets("/", "index.html");

    onStart(() -> {
      //starterData();
    });
  }

  protected Object getRadiatorByCode(Request req) {
    RadiatorStore store = getResultsStore();
    Radiator proj = store.get(req.param("code").value());
    if (proj == null) {
      return nothingHere();
    }
    return proj;
  }

  protected RadiatorStore getResultsStore() {
    return require(RadiatorStore.class);
  }

  protected Object nothingHere() {
    return new NothingHere();
  }

  public static void main(final String[] args) {
    run(GreenBuildMain::new, args);
  }

  public static class NothingHere {
    public String message = "nothing here";
  }

  private void starterData() {
    RadiatorStore store = getResultsStore();
    store.createRadiator("aa-bb-cc-dd", "Compile", "Unit Tests", "Integration Tests");
    Radiator radiator = store.get("aa-bb-cc-dd");
    radiator.startStep(1, "Compile");
    radiator.stepPassed(1, "Compile");
    radiator.builds.get(0).steps.get(0).dur = 33000;
    radiator.startStep(1, "Unit Tests");
    radiator.stepPassed(1, "Unit Tests");
    radiator.builds.get(0).steps.get(1).dur = 45500;
    radiator.startStep(1, "Integration Tests");
    radiator.stepPassed(1, "Integration Tests");
    radiator.builds.get(0).steps.get(2).dur = 120000;
    radiator.startStep(2, "Compile");
    radiator.stepPassed(2, "Compile");
    radiator.builds.get(0).steps.get(0).dur = 31000;
    radiator.startStep(2, "Unit Tests");
    radiator.stepFailed(2, "Unit Tests");
    radiator.builds.get(0).steps.get(1).dur = 42600;
    radiator.startStep(3, "Compile");
    radiator.stepPassed(3, "Compile");
    radiator.builds.get(0).steps.get(0).dur = 800;
    radiator.startStep(3, "Unit Tests");
  }


}
