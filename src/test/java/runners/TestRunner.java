package runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * TestRunner - connects Cucumber to TestNG.
 *
 * Extending AbstractTestNGCucumberTests turns every Cucumber scenario into a
 * TestNG test. testng.xml points at this class, and Maven runs testng.xml.
 *
 *   features  where the .feature files are (on the classpath, so it works
 *             the same in IntelliJ, from the command line and in CI)
 *   glue      the packages Cucumber searches for step definitions AND hooks.
 *             Leave out "hooks" and no browser ever starts.
 *   tags      which scenarios run (see the TEMPORARY note below)
 *   plugin    pretty  = each step printed to the console as it runs
 *             summary = scenario/step totals at the end of the run
 *             html    = a report file you open in a browser
 *
 * Filtering from the command line REPLACES the tags below, so keep @wip and
 * @disabled out:
 *   mvn clean test -Dcucumber.filter.tags="@books and not @wip and not @disabled"
 *
 *   @wip       step code not written yet
 *   @disabled  parked on purpose (login.feature since 2026-11-02 - see its header)
 */
//json = machine-readable results, used by the Jenkins Cucumber reports plugin

@CucumberOptions(
        features = "classpath:features",
        glue = {"stepDefinitions", "hooks"},

        // Runs everything except unfinished (@wip) and parked (@disabled) scenarios.
        // To run a subset, filter on the command line instead of editing this line:
        //   mvn clean test -Dcucumber.filter.tags="@toBeRunNow and not @wip and not @disabled"
        tags = "not @wip and not @disabled",

        plugin = {
                "pretty",
                "summary",
                "html:target/cucumber-reports/cucumber.html",
                "json:target/cucumber-reports/cucumber.json"
        },
        monochrome = true
)
public class TestRunner extends AbstractTestNGCucumberTests {

    /**
     * Runs scenarios one at a time for now. Switching this to parallel = true
     * (Session 17) is safe because DriverFactory gives each thread its own browser.
     */
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
