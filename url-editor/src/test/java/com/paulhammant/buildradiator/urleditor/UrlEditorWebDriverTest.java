package com.paulhammant.buildradiator.urleditor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.seleniumhq.selenium.fluent.FluentWebDriver;
import org.seleniumhq.selenium.fluent.FluentWebElements;

import static com.paulhammant.buildradiator.urleditor.TestVersionOfEditorApp.CONTRIVED_PATH_FOR_TESTING;
import static org.junit.Assert.assertEquals;

public class UrlEditorWebDriverTest {

    private static ChromeDriver DRIVER;
    private static FluentWebDriver FWD;
    private static int testNum;

    private TestVersionOfEditorApp app;

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
    public static void tearDown() throws InterruptedException {
        //Thread.sleep(1000000);
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
    public void editorDisplayRemovesUnderScoresFromContrivedTitle() {
        app = new TestVersionOfEditorApp();
        startAppAndOpenWebDriverOnEditorPage(CONTRIVED_PATH_FOR_TESTING + "#abcde12345/a_contrived_title/0/a_a/1/b_b/2/c_c");
        FWD.div().getText().shouldContain("a contrived title");
        FluentWebElements lis = FWD.lis();
        lis.get(0).getText().shouldContain("0:");
        lis.get(0).input().getAttribute("value").shouldContain("a a");
        lis.get(1).getText().shouldContain("1:");
        lis.get(1).input().getAttribute("value").shouldContain("b b");
        lis.get(2).getText().shouldContain("2:");
        lis.get(2).input().getAttribute("value").shouldContain("c c");
    }

    @Test
    public void editorCanChangeTitleAndStepDescriptionsFromDemoRadiatorAndReturn() {
        app = new TestVersionOfEditorApp() {{
            get("/r", () -> "<html><body>OK</body></html>");
        }};

        startAppAndOpenWebDriverOnEditorPage(CONTRIVED_PATH_FOR_TESTING + "#ueeusvcipmtsb755uq/Example_Build_Radiator/c/compile/u/unit_tests/i/integration_tests/f/functional_tests/p/package");
        FWD.input().clearField().sendKeys("T I T L E");
        FWD.li().input().clearField().sendKeys("new STEP desc");
        FWD.button().click();
        FWD.url().shouldContain("/r#ueeusvcipmtsb755uq/T_I_T_L_E/c/new_STEP_desc/u/unit_tests/i/integration_tests/f/functional_tests/p/package");
        FWD.body().getText().shouldContain("OK"); // from mock response above
    }

    private void startAppAndOpenWebDriverOnEditorPage(String path) {
        app.start("server.join=false");
        while (!app.appStarted) {
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
            }
        }
        DRIVER.get(domain + "/" + path);
    }


}
