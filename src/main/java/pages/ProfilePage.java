package pages;

import base.BasePage;
import org.openqa.selenium.WebDriver;

/**
 * ProfilePage - https://demoqa.com/profile, where a successful login lands.
 *
 * Locators: locators.yml -> ProfilePage.
 * Watch out: this page has four buttons with id="submit" (Framework-Design.md
 * section 6). When buttons are added here, locate them by their text.
 */
public class ProfilePage extends BasePage {

    public ProfilePage(WebDriver driver) {
        super(driver);
    }

    /**
     * Loaded = the logged-in username is shown.
     * Checking a page-specific element rather than the URL: the URL can change
     * before the page has finished drawing, which makes URL checks flaky.
     */
    @Override
    public boolean isLoaded() {
        return isVisible(loc("username_value"));
    }

    public String getUsername() {
        return getText(loc("username_value"));
    }
}
