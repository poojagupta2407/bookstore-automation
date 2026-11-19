package stepDefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import pages.LoginPage;
import pages.ProfilePage;
import utils.ConfigReader;
import utils.DriverFactory;

/**
 * LoginSteps - step definitions for features/login.feature, plus the shared
 * "Given I am logged in" step used by books.feature.
 *
 * Written so far: LOG-01 and "I am logged in". The other login.feature
 * scenarios are @wip and get their steps in build step 9.
 *
 * Credentials come from ConfigReader - in practice the environment variables
 * BOOKSTORE_USERNAME and BOOKSTORE_PASSWORD. If they aren't set, the first
 * step that needs them fails with instructions. The password is never
 * printed in any message.
 *
 * "Then" steps build a fresh ProfilePage from the current browser rather than
 * reusing one stored by an earlier step. A Then step checks what is on
 * screen NOW, however the browser got there.
 */
public class LoginSteps {

    private LoginPage loginPage;

    // ── Given ───────────────────────────────────────────────────────────────

    /** Background of login.feature. */
    @Given("I am on the login page")
    public void iAmOnTheLoginPage() {
        loginPage = new LoginPage(DriverFactory.getDriver()).open();
        Assert.assertTrue(loginPage.isLoaded(),
            "The login page did not load. URL: " + loginPage.getCurrentUrl());
    }

    /**
     * Background of books.feature: a complete login in one step.
     * Fails, and stops the scenario, if the login doesn't reach the profile -
     * so catalogue steps never run on a broken login.
     */
    @Given("I am logged in")
    public void iAmLoggedIn() {
        ProfilePage profile = new LoginPage(DriverFactory.getDriver())
                .open()
                .login(username(), password());
        Assert.assertTrue(profile.isLoaded(),
            "Login as '" + username() + "' did not reach the profile page. "
            + "Check BOOKSTORE_USERNAME / BOOKSTORE_PASSWORD. URL: " + profile.getCurrentUrl());
    }

    // ── When ────────────────────────────────────────────────────────────────

    /** LOG-01 (later also LOG-05, LOG-08). */
    @When("I log in with valid credentials")
    public void iLogInWithValidCredentials() {
        loginPage.login(username(), password());
    }

    // ── Then ────────────────────────────────────────────────────────────────

    /** LOG-01 (later also LOG-08). */
    @Then("I should be on my profile page")
    public void iShouldBeOnMyProfilePage() {
        ProfilePage profile = new ProfilePage(DriverFactory.getDriver());
        Assert.assertTrue(profile.isLoaded(),
            "Expected the profile page, but the browser is on: " + profile.getCurrentUrl());
    }

    /** LOG-01. */
    @Then("my username should be shown on the profile")
    public void myUsernameShouldBeShownOnTheProfile() {
        ProfilePage profile = new ProfilePage(DriverFactory.getDriver());
        Assert.assertEquals(profile.getUsername(), username(),
            "The profile shows a different username from the one that logged in.");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static String username() {
        return ConfigReader.get("username");
    }

    private static String password() {
        return ConfigReader.get("password");
    }
}
