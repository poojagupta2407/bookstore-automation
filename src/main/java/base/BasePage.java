package base;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.ConfigReader;
import utils.LocatorReader;

import java.time.Duration;
import java.util.List;

/**
 * BasePage - the parent of every page class, and the ONLY class that waits
 * or touches elements directly.
 *
 * Page classes say WHAT they do ("enter the username"). BasePage handles HOW:
 * waiting until the element is ready, scrolling to it, and coping with ad
 * overlays. So page classes contain no waits, no try/catch and no
 * JavascriptExecutor.
 *
 * Waiting rules (Framework-Design.md, D4):
 *   - implicit wait is never used
 *   - every interaction waits explicitly for the condition it needs
 *   - no Thread.sleep anywhere
 */
public abstract class BasePage {

    protected final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;
    private final int explicitWaitSeconds;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.explicitWaitSeconds = ConfigReader.getInt("explicitWait");
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(explicitWaitSeconds));
        this.js = (JavascriptExecutor) driver;
    }

    /**
     * Every page must prove it is the page actually on screen. Making this
     * abstract stops a test from "passing" while looking at the wrong page.
     */
    public abstract boolean isLoaded();

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    // ── Locators ────────────────────────────────────────────────────────────

    /** Looks up a locator in this page's own section of locators.yml. */
    protected By loc(String key) {
        return LocatorReader.get(getClass().getSimpleName(), key);
    }

    /** Same, filling in a {placeholder}:  loc("book_link", "Git Pocket Guide") */
    protected By loc(String key, String value) {
        return LocatorReader.getDynamic(getClass().getSimpleName(), key, value);
    }

    // ── Actions ─────────────────────────────────────────────────────────────

    /**
     * Waits until clickable, then clicks.
     * If a DemoQA ad overlay is covering the element, falls back to a
     * JavaScript click. That fallback is deliberate and narrow: a JavaScript
     * click skips the browser's own checks, so as a default it could hide a
     * real bug.
     */
    protected void click(By locator) {
        WebElement element = waitForElement(locator);
        scrollIntoView(element);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(element)).click();
        } catch (ElementClickInterceptedException e) {
            js.executeScript("arguments[0].click();", element);
        }
    }

    /** Waits, clears the field, then types. */
    protected void type(By locator, String text) {
        WebElement element = waitForElement(locator);
        scrollIntoView(element);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Empties a field the way a user would: select all, then delete.
     *
     * Why not element.clear()? On React pages like DemoQA, clear() empties
     * the box on screen but doesn't tell the page it changed - so a search
     * filter can stay applied even though the box looks empty.
     * (Ctrl+A is the Windows/Linux shortcut; macOS would need Command.)
     */
    protected void clearField(By locator) {
        WebElement element = waitForElement(locator);
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
    }

    // ── Reading ─────────────────────────────────────────────────────────────

    protected String getText(By locator) {
        return waitForElement(locator).getText().trim();
    }

    /** Text of every matching element, in page order. Empty list if none match. */
    protected List<String> getTexts(By locator) {
        return driver.findElements(locator).stream()
                .map(element -> element.getText().trim())
                .toList();
    }

    /**
     * How many elements match right now.
     * findElements (plural) returns an empty list when nothing matches;
     * findElement (singular) would throw instead.
     */
    protected int countOf(By locator) {
        return driver.findElements(locator).size();
    }

    // ── Waiting ─────────────────────────────────────────────────────────────

    /** Waits until visible and returns the element. Fails with the locator and URL. */
    protected WebElement waitForElement(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                "Element not visible after " + explicitWaitSeconds + "s: " + locator
                + " on " + driver.getCurrentUrl(), e);
        }
    }

    /**
     * Waits (up to explicitWait) for the element to be visible; true/false
     * instead of an exception, so the step definition can assert with its own
     * message. For checks where you EXPECT the element to appear.
     */
    protected boolean isVisible(By locator) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits (up to shortWait) until exactly {@code expected} elements match.
     * Counting immediately after a search can catch the OLD rows; waiting for
     * the expected count removes that race without a Thread.sleep.
     */
    protected boolean waitForCount(By locator, int expected) {
        try {
            shortWait().until(d -> d.findElements(locator).size() == expected);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Polls every 500ms for up to explicitWait, ignoring "not found yet" and
     * "element went stale" - for elements the page keeps re-drawing.
     */
    protected WebElement fluentWait(By locator) {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(explicitWaitSeconds))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElement(locator));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void scrollIntoView(WebElement element) {
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    }

    private WebDriverWait shortWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getInt("shortWait")));
    }
}
