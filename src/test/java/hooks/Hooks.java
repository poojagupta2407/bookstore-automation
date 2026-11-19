package hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import utils.ConfigReader;
import utils.DriverFactory;

/**
 * Hooks - what happens around every scenario.
 *
 *   before:  start the browser
 *   after:   if the scenario failed, attach a screenshot to the report
 *            then close the browser
 *
 * This is the ONLY place a browser is started or stopped. Step definitions
 * and pages just use DriverFactory.getDriver().
 *
 * Order: Cucumber runs @Before hooks LOWEST number first, and @After hooks
 * HIGHEST number first. So the screenshot (order 1) is taken before the
 * browser is closed (order 0).
 */
public class Hooks {

    @Before(order = 0)
    public void startBrowser() {
        DriverFactory.initDriver(
            ConfigReader.get("browser"),
            ConfigReader.getBoolean("headless", false));
    }

    @After(order = 1)
    public void attachScreenshotOnFailure(Scenario scenario) {
        if (!scenario.isFailed()) {
            return;
        }
        try {
            byte[] screenshot = ((TakesScreenshot) DriverFactory.getDriver())
                    .getScreenshotAs(OutputType.BYTES);
            scenario.attach(screenshot, "image/png", "Screenshot at failure");
        } catch (Exception e) {
            // Never let a failed screenshot hide the real test failure.
            System.err.println("Could not take a screenshot: " + e.getMessage());
        }
    }

    @After(order = 0)
    public void quitBrowser() {
        DriverFactory.quitDriver();
    }
}
