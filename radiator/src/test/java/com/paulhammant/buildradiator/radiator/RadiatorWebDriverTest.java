package com.paulhammant.buildradiator.radiator;

import com.google.common.io.CharStreams;
import com.paulhammant.buildradiator.radiator.model.Radiator;
import io.jooby.Route;
import io.jooby.Server;
import org.junit.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.seleniumhq.selenium.fluent.FluentWebDriver;
import org.seleniumhq.selenium.fluent.FluentWebElement;
import org.seleniumhq.selenium.fluent.TestableString;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.paulhammant.buildradiator.radiator.TestVersionOfBuildRadiatorApp.CONTRIVED_PATH_FOR_TESTING;
import static com.paulhammant.buildradiator.radiator.model.TestRadBuilder.*;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.seleniumhq.selenium.fluent.Period.millis;
import static org.seleniumhq.selenium.fluent.Period.secs;

public class RadiatorWebDriverTest {

    private static ChromeDriver DRIVER;
    private static FluentWebDriver FWD;
    private static int testNum;

    private TestVersionOfBuildRadiatorApp app;
    private Server stoppable;

    @BeforeClass
    public static void sharedForAllTests() {
        // Keep the WebDriver browser window open between tests
        ChromeOptions co = new ChromeOptions();
        boolean headless = Boolean.parseBoolean(System.getProperty("HEADLESS", "false"));
        if (headless) {
            co.addArguments("headless");
        }
        co.addArguments("window-size=1200x800");
        DRIVER = new ChromeDriver(co);
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
        // anySubDomainOf.localhost maps to 127.0.0.1
        // I sure hope those people don't let the domain go, or remap it
        // it is a decent way to ensure nothing is shared between tests (mostly)
        domain = "http://t" + testNum++ + ".localhost:8080";
        //domain = "http://localhost:8080";
    }

    @After
    public void stopServer() throws InterruptedException {
        //Thread.sleep(1000000);
        stoppable.stop();
        app = null;
    }

    @Test
    public void pageContainsDataFromServer()  {

        Radiator rad = rad("xxx", "sseeccrreett", stepNames("A", "B", "C"),
                build("2", "running", 2000, step("A", 2000, "running"), step("B"), step("C")),
                build("1", "running", 4000, step("A", 4000, "failed"), skip("B"), skip("C")))
                .withIpAccessRestrictedToThese("0:0:0:0:0:0:0:1", "127.0.0.1", "111.222.33.44");

        app = new TestVersionOfBuildRadiatorApp(rad);

        startAppAndOpenWebDriverOnRadiatorPage(CONTRIVED_PATH_FOR_TESTING + "#xxx/Main_Project_Trunk_Build");

        FWD.trs().getText().within(secs(2)).shouldContain("Main Project Trunk");
        FWD.td().getText().within(secs(3)).shouldBe("Main Project Trunk Build\nchange URL to customize the title ↑ or step codes ↓ Edit the title and step descriptions");

        FWD.body().getText().within(secs(2)).shouldContain("(running)");
        FWD.trs().get(1).getText().shouldBe("2\n2 secs\nA\n2 secs\n(running) B\n0 secs\nC\n0 secs");
        FWD.trs().get(2).getText().shouldBe("1\n4 secs\nA\n4 secs\n(failed) B\n0 secs\n(skipped) C\n0 secs\n(skipped)");
    }

    @Test
    public void trailingSlashOnPageTitleIsIgnored() {

        Radiator rad = rad("xxx", "sseeccrreett", stepNames("A"));

        app = new TestVersionOfBuildRadiatorApp(rad);

        startAppAndOpenWebDriverOnRadiatorPage(CONTRIVED_PATH_FOR_TESTING + "#xxx/Main_Project_Trunk_Build/");

        FWD.td().getText().within(secs(3)).shouldBe("Main Project Trunk Build\nchange URL to customize the title ↑ or step codes ↓ Edit the title and step descriptions");
        FWD.trs().getText().within(secs(2)).shouldContain("Main Project Trunk");
        FWD.url().shouldMatch(endsWith("Trunk_Build/")); // unchanged
    }


    @Test
    public void pageCanHaveStepsReplaceOnBrowserSide()  {

        Radiator rad = rad("xxx", "sseeccrreett", stepNames("A", "B", "C"),
                build("2", "running", 2000, step("A", 2000, "running"), step("B"), step("C")),
                build("1", "running", 4000, step("A", 4000, "failed"), skip("B"), skip("C")))
                .withIpAccessRestrictedToThese("0:0:0:0:0:0:0:1", "127.0.0.1", "111.222.33.44");

        app = new TestVersionOfBuildRadiatorApp(rad);

        startAppAndOpenWebDriverOnRadiatorPage(CONTRIVED_PATH_FOR_TESTING + "#xxx/Main_Project_Trunk_Build/A/Ant/B/Bat/C/Clever_Cat");

        FWD.td().getText().within(secs(2)).shouldBe("Main Project Trunk Build\nchange URL to customize the title ↑ or step codes ↓ Edit the title and step descriptions");
        FWD.trs().getText().within(secs(2)).shouldContain("running");
        FWD.trs().get(1).getText().shouldBe("2\n2 secs\nAnt\n2 secs\n(running) Bat\n0 secs\nClever Cat\n0 secs");
        FWD.trs().get(2).getText().shouldBe("1\n4 secs\nAnt\n4 secs\n(failed) Bat\n0 secs\n(skipped) Clever Cat\n0 secs\n(skipped)");
    }

    @Test
    public void pageCanHandleWrongOrMissingRadiatorCode()  {

        app = new TestVersionOfBuildRadiatorApp(null);

        startAppAndOpenWebDriverOnRadiatorPage(CONTRIVED_PATH_FOR_TESTING + "#missing_radiator_code/Main_Project_Trunk_Build");

        TestableString text = FWD.div().within(secs(2)).getText();
        text.shouldContain("Radiator code missing_radiator_code not recognized");
        text.shouldContain("Did you type it correctly?");
        text.shouldContain("Maybe the radiator DOES exist but this egress");
        text.shouldContain("TCP/IP address (0:0:0:0:0:0:0:1) is not allowed");
    }

    @Test
    public void confirmDataRefreshes() {

        Radiator rad = rad("xxx", "sseeccrreett", stepNames("A"),
                build("1", "running", 0, step("A", 0, "running")));

        app = new TestVersionOfBuildRadiatorApp(rad).withFasterRefresh();

        startAppAndOpenWebDriverOnRadiatorPage(CONTRIVED_PATH_FOR_TESTING + "#xxx/Main_Project_Trunk_Build");

        final FluentWebElement rootDiv = FWD.within(secs(2)).div();
        rootDiv.getText().within(secs(2)).shouldContain("(running)");
        rad.stepPassed("1", "A");
        FWD.trs().get(1).getText().within(secs(4)).shouldContain("(passed)");
        rootDiv.getText().shouldNotContain("(running)");

    }

    @Test
    public void columnsShouldBeProportional() {

        Radiator rad = rad("xxx", "sseeccrreett", stepNames("A", "B"),
                build("1", "passed", 30000, step("A", 8000, "passed"), step("B", 32000, "passed")),
                build("2", "passed", 30000, step("A", 12000, "passed"), step("B", 28000, "passed")));

        app = new TestVersionOfBuildRadiatorApp(rad);

        startAppAndOpenWebDriverOnRadiatorPage(CONTRIVED_PATH_FOR_TESTING + "#xxx/Main_Project_Trunk_Build");

        StringBuilder percentages = new StringBuilder();
        FWD.body().within(secs(3)).getText().shouldContain("Main");
        FWD.tds().each((fluentWebElement, i) -> rhs(fluentWebElement.getAttribute("style").toString().split("width:"), percentages));
        assertThat(percentages.toString(), equalTo("8%;23%;69%;8%;23%;69%;"));

    }

    @Test
    public void columnsShouldBeProportionalEvenWithDurationsOfZero()  {

        Radiator rad = rad("xxx", "sseeccrreett", stepNames("A", "B"),
                build("1", "passed", 30000, step("A", 8000, "passed"), step("B", 30000, "passed")),
                build("2", "running", 30000, step("A", 12000, "running"), step("B", 0, "")));

        app = new TestVersionOfBuildRadiatorApp(rad);

        startAppAndOpenWebDriverOnRadiatorPage(CONTRIVED_PATH_FOR_TESTING + "#xxx/Main_Project_Trunk_Build");

        FWD.body().within(secs(3)).getText().shouldContain("Main");
        StringBuilder percentages = new StringBuilder();
        FWD.tds().each((fluentWebElement, i) -> rhs(fluentWebElement.getAttribute("style").toString().split("width:"), percentages));
        assertThat(percentages.toString(), equalTo("8%;23%;69%;8%;23%;69%;"));

    }

    private void rhs(String[] split, StringBuilder percentages) {
        percentages.append(split.length > 1 ? split[1].trim() : "");
    }

    private void startAppAndOpenWebDriverOnRadiatorPage(String path) {
        stoppable = app.start();
        while (!app.appStarted) {
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
            }
        }
        DRIVER.get(domain + path);
    }


}
