package greenbuild;

import org.junit.After;
import org.junit.Test;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;

public class RadiatorIntegrationTest {

  private IntegTestGreenBuild app;

  public static class IntegTestGreenBuild extends GreenBuildMain {
    private boolean appStarted;
    public IntegTestGreenBuild() {
      onStarted(() -> appStarted = true);
    }
  }

  @Test
  public void knownCodeHasListOfBuildsAvailableAsJson() {
    app = new IntegTestGreenBuild() {
      @Override
      protected RadiatorStore getResultsStore() {
        RadiatorStore rs = new RadiatorStore();
        rs.createRadiator("aaabbbccc", "A", "B", "C");
        rs.get("aaabbbccc").startStep(1, "A");
        return rs;
      }
    };
    startApp();
    get("/p/aaabbbccc")
            .then()
            .assertThat()
            .body(equalTo(("{'builds':[{'num':1,'steps':[{'name':'A','dur':0,'status':'running'}," +
                    "{'name':'B','dur':0,'status':''},{'name':'C','dur':0,'status':''}]," +
                    "'status':'running'}]}").replace("'","\"")))
            .statusCode(200)
            .contentType("application/json;charset=UTF-8");

  }

  @Test
  public void unknownCodeHasNoBuildsList() {
    app = new IntegTestGreenBuild() {
      @Override
      protected RadiatorStore getResultsStore() {
        RadiatorStore rs = new RadiatorStore();
        rs.createRadiator("aaabbbccc", "A", "B", "C");
        rs.get("aaabbbccc").startStep(1, "A");
        return rs;
      }
    };
    startApp();
    get("/p/wewwewwewe")
            .then()
            .assertThat()
            .body(equalTo(("{'message':'nothing here'}").replace("'","\"")))
            .statusCode(200)
            .contentType("application/json;charset=UTF-8");

  }

  @Test
  public void listOfCodesNotAllowed() {
    app = new IntegTestGreenBuild();
    startApp();
    get("/p")
            .then()
            .assertThat()
            .body(equalTo(("{'message':'nothing here'}").replace("'","\"")))
            .statusCode(200)
            .contentType("application/json;charset=UTF-8");

  }

  private void startApp() {
    app.start("server.join=false");
    while (!app.appStarted) {
      try {
        Thread.sleep(15);
      } catch (InterruptedException e) {
      }
    }
  }

  @After
  public void stopServer() {
    app.stop();
  }


}
