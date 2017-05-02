package greenbuild;

import org.junit.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.seleniumhq.selenium.fluent.FluentWebDriver;

import static org.hamcrest.MatcherAssert.assertThat;

public class RadiatorWebDriverTest {

  public static class FuncTestGreenBuild extends GreenBuildMain {

    private boolean appStarted;

    public FuncTestGreenBuild() {
      onStarted(() -> appStarted = true);
    }

  }

  private static ChromeDriver DRIVER;
  private static FluentWebDriver FWD;
  private static int testNum;

  private FuncTestGreenBuild app;

  @BeforeClass
  public static void sharedForAllTests() {
    // Keep the WebDriver browser window open between tests
    DRIVER = new ChromeDriver();
    FWD = new FluentWebDriver(DRIVER);
  }

  @AfterClass
  public static void tearDown() {
    DRIVER.close();
    DRIVER.quit();
  }

  private String domain;

  @Before
  public void perTest() {
    // anySubDomainOf.devd.io maps to 127.0.0.1
    // I sure hope those people don't let the domain go, or remap it
    // it is a decent way to ensure nothing is shared between tests (mostly)
    domain = "http://t" + testNum++ + ".devd.io:8080";
  }

  @After
  public void stopServer() {
    app.stop();
  }


  @Test
  public void pageContainsDataFromServer() throws InterruptedException {
    app = new FuncTestGreenBuild() {
      @Override
      protected RadiatorStore getResultsStore() {
        RadiatorStore rs = new RadiatorStore();
        rs.createRadiator("xxx", "A", "B", "C");
        Radiator aaabbbccc = rs.get("xxx");
        aaabbbccc.startStep(1, "A");
        aaabbbccc.stepFailed(1, "A");
        aaabbbccc.startStep(2, "A");
        return rs;
      }

    };
    startApp();
    openRadiatorPage();
    //Thread.sleep(1000 * 1000);
    FWD.td().getText().shouldBe("Main Project Trunk Build");
    FWD.trs().get(1).getText().shouldBe("2 A\n0ms\n(running) B\n0ms\nC\n0ms");
    FWD.trs().get(2).getText().shouldBe("1 A\n0ms\n(failed) B\n0ms\n(skipped) C\n0ms\n(skipped)");
  }

  private void openRadiatorPage() {
    DRIVER.get(domain + "/#" + "xxx/Main_Project_Trunk_Build");
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

}
