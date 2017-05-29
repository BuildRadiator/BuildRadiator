package com.paulhammant.buildradiator;

import com.paulhammant.buildradiator.model.Radiator;
import org.junit.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.seleniumhq.selenium.fluent.FluentWebDriver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.seleniumhq.selenium.fluent.Period.secs;

public class RadiatorWebDriverTest {

    private static ChromeDriver DRIVER;
    private static FluentWebDriver FWD;
    private static int testNum;

    private TestVersionOfBuildRadiatorApp app;

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
        app = null;
    }

    @Test
    public void pageContainsDataFromServer() throws InterruptedException {
        app = new TestVersionOfBuildRadiatorApp.ThatHasSimpleABCRadiatorCalledXXXWithOneFailedAndOneRunningBuild();

        startAppAndOpenWebDriverOnRadiatorPage("xxx");

        FWD.td().getText().shouldBe("Main Project Trunk Build\nchange URL to customize the title ↑ or step codes ↓");
        FWD.trs().get(1).getText().shouldBe("2\n2 secs\nA\n0 secs\n(running) B\n0 secs\nC\n0 secs");
        FWD.trs().get(2).getText().shouldBe("1\n4 secs\nA\n0 secs\n(failed) B\n0 secs\n(skipped) C\n0 secs\n(skipped)");
    }

    @Test
    public void pageCanHaveStepsReplaceOnBrowserSide() throws InterruptedException {
        app = new TestVersionOfBuildRadiatorApp.ThatHasSimpleABCRadiatorCalledXXXWithOneFailedAndOneRunningBuild();

        startAppAndOpenWebDriverOnRadiatorPage("xxx", "/A/Ant/B/Bat/C/Clever_Cat");

        FWD.td().getText().shouldBe("Main Project Trunk Build\nchange URL to customize the title ↑ or step codes ↓");
        FWD.trs().get(1).getText().shouldBe("2\n2 secs\nAnt\n0 secs\n(running) Bat\n0 secs\nClever Cat\n0 secs");
        FWD.trs().get(2).getText().shouldBe("1\n4 secs\nAnt\n0 secs\n(failed) Bat\n0 secs\n(skipped) Clever Cat\n0 secs\n(skipped)");
    }

    @Test
    public void pageCanHandleWrongOrMissingRadiatorCode() throws InterruptedException {

        app = new TestVersionOfBuildRadiatorApp.ThatHasNoRadiators();

        startAppAndOpenWebDriverOnRadiatorPage("missing_radiator_code");

        //Thread.sleep(10000000);
        FWD.div().getText().shouldBe("Radiator code missing_radiator_code not recognized.\n\n" +
                "Did you type it correctly?\n\n" +
                "Maybe the radiator DOES exist but this egress\n" +
                "TCP/IP address (127.0.0.1) is not allowed.");
    }

    private void startAppAndOpenWebDriverOnRadiatorPage(String code) {
        startAppAndOpenWebDriverOnRadiatorPage(code, "");
    }

    private void startAppAndOpenWebDriverOnRadiatorPage(String code, String extraUrl) {
        app.start("server.join=false");
        while (!app.appStarted) {
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
            }
        }
        DRIVER.get(domain + "/r#" + code + "/Main_Project_Trunk_Build" + extraUrl);
    }

    @Test
    public void confirmDataRefreshes() throws InterruptedException {
        app = new TestVersionOfBuildRadiatorApp() {
            @Override
            protected void loadStoreWithTestData(RadiatorStore require) {
                Radiator rad = radiatorStore.createRadiator(new TestRandomGenerator("xxx", "sseeccrreett"), "A");
                app.radCode = rad.code;
                radiatorStore.get(app.radCode, "127.0.0.1").startStep("1", "A");
            }

            @Override
            protected void serveRadiatorPage() {
                // speed up refresh interval - hack radiator.html as it is send to the browser
                super.serveIndexPageButWithReplacement("30000", "300");
            }
        };

        startAppAndOpenWebDriverOnRadiatorPage("xxx");

        FWD.trs().get(1).getText().shouldContain("(running)");
        app.radiatorStore.get(app.radCode, "127.0.0.1").stepPassed("1", "A");
        FWD.trs().get(1).getText().within(secs(4)).shouldContain("(passed)");

    }

}
