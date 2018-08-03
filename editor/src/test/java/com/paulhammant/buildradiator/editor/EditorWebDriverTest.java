package com.paulhammant.buildradiator.editor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.seleniumhq.selenium.fluent.FluentWebDriver;

public class EditorWebDriverTest {

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
    public void editorContainsContrivedTitle() {
        app = new TestVersionOfEditorApp();
        startAppAndOpenWebDriverOnEditorPage("abcde12345/a_contrived_title/a/aa/b/bb/c/cc");
        FWD.div().getText().shouldContain("a contrived title");
    }

    @Test
    public void editorContainsDemoRadiatorTitle() {
        app = new TestVersionOfEditorApp();
        startAppAndOpenWebDriverOnEditorPage("ueeusvcipmtsb755uq/Example_Build_Radiator/c/compile/u/unit_tests/i/integration_tests/f/functional_tests/p/package");
        FWD.div().getText().shouldContain("Example Build Radiator");
    }


    private void startAppAndOpenWebDriverOnEditorPage(String path) {
        app.start("server.join=false");
        while (!app.appStarted) {
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
            }
        }
        DRIVER.get(domain + "/#" + path);
    }


}
