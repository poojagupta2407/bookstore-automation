package pages;

import base.BasePage;
import org.openqa.selenium.WebDriver;
import utils.ConfigReader;

/**
 * LoginPage - https://demoqa.com/login
 *
 * Locators: locators.yml -> LoginPage. No assertions here: this class reports
 * what is on screen, and the step definitions decide whether that's right.
 */
public class LoginPage extends BasePage {

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        driver.get(ConfigReader.get("loginUrl"));
        return this;
    }

    @Override
    public boolean isLoaded() {
        return isVisible(loc("login_button"));
    }

    public LoginPage enterUsername(String username) {
        type(loc("username_field"), username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        type(loc("password_field"), password);
        return this;
    }

    public void clickLogin() {
        click(loc("login_button"));
    }

    /**
     * Fills both fields, clicks Login, and returns the profile page.
     *
     * It returns ProfilePage even if the login FAILS. That's deliberate: the
     * page object does what the user does and hands back the page they would
     * expect to reach. The step definition then checks where the browser
     * actually ended up - a page object never decides whether a test passed.
     */
    public ProfilePage login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        return new ProfilePage(driver);
    }
}
