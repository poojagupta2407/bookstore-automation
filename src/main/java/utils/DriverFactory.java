package utils;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * DriverFactory - creates, hands out and closes the browser.
 *
 * The driver is kept in a ThreadLocal: every thread gets its own browser.
 * With a plain static WebDriver, scenarios running in parallel would share
 * one browser and overwrite each other's pages. With ThreadLocal, page
 * classes can still call DriverFactory.getDriver() from anywhere and always
 * get the browser belonging to their own thread.
 *
 * No chromedriver download or path setting is needed: Selenium Manager,
 * built into Selenium since 4.6, finds the right driver automatically.
 *
 * No implicit wait is set, on purpose - all waiting is explicit, in BasePage
 * (Framework-Design.md, D4).
 *
 * DemoQA and ads (first-run finding, 2026-11-02): DemoQA loads ~20 requests to
 * Google ad and tracking servers. With Selenium's default "normal" page-load
 * strategy, driver.get() waits for EVERY one of them, so one slow ad server
 * made get() hang for the full 60s page-load timeout. Two settings fix it:
 *   pageLoadStrategy=eager  get() returns when the HTML is ready, not when
 *                           every ad has loaded. Safe, because BasePage waits
 *                           explicitly for each element it uses.
 *   blockAds=true           Chrome/Edge never request the ad domains at all.
 *
 * DemoQA's own app bundle (second finding, 2026-11-02): the page is an empty
 * 436-byte HTML shell plus ONE 899 KB JavaScript file, sent uncompressed by a
 * server that can be very slow. Nothing appears until that file arrives. Every
 * Selenium browser starts with an empty profile, so every scenario downloaded
 * it again. browserCache=true gives Chrome/Edge a shared cache folder, so only
 * the first load pays that cost. Cookies are NOT shared (they live in the
 * profile, not the cache), so every test still starts logged out.
 */
public final class DriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    /**
     * Ad and tracking domains DemoQA calls (measured on the live site). The
     * site works without any of them. '*' matches anything.
     */
    private static final List<String> AD_URL_PATTERNS = List.of(
        "*doubleclick.net*",
        "*googlesyndication.com*",
        "*googletagservices.com*",
        "*googletagmanager.com*",
        "*google-analytics.com*",
        "*adtrafficquality.google*"
    );

    private DriverFactory() {
    }

    /** Starts a browser for the current thread. Called by Hooks before each scenario. */
    public static void initDriver(String browser, boolean headless) {
        if (DRIVER.get() != null) {
            return; // this thread already has a browser
        }

        // Read config BEFORE opening a browser: if a setting is missing or wrong,
        // we fail without leaving a browser window open.
        Duration pageLoadTimeout = Duration.ofSeconds(ConfigReader.getInt("pageLoadTimeout"));
        PageLoadStrategy loadStrategy = pageLoadStrategy();
        boolean blockAds = ConfigReader.getBoolean("blockAds", true);
        String cacheDir = browserCacheDir();

        WebDriver driver = switch (browser.trim().toLowerCase()) {
            case "chrome"  -> new ChromeDriver(chromeOptions(headless, loadStrategy, cacheDir));
            case "firefox" -> new FirefoxDriver(firefoxOptions(headless, loadStrategy));
            case "edge"    -> new EdgeDriver(edgeOptions(headless, loadStrategy, cacheDir));
            default -> throw new IllegalArgumentException(
                "Unsupported browser '" + browser + "'. Use chrome, firefox or edge.");
        };

        // Store it straight away, so quitDriver() can always close it - even if
        // one of the setup lines below fails.
        DRIVER.set(driver);

        // Must happen before the first page is opened.
        if (blockAds && driver instanceof ChromiumDriver chromium) {
            blockAdDomains(chromium);
        }

        driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);
        if (!headless) {
            driver.manage().window().maximize(); // headless size is set in the options instead
        }
    }

    /** The browser for the current thread. Fails clearly if none was started. */
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException(
                "No browser on this thread. Hooks.startBrowser() starts it before each scenario - "
                + "check that 'hooks' is in the glue list in TestRunner.");
        }
        return driver;
    }

    /**
     * Closes the browser and forgets it.
     * remove() is in a finally block so the ThreadLocal is always cleared, even
     * if quit() fails - otherwise a reused thread could pick up a dead browser.
     */
    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } finally {
            DRIVER.remove();
        }
    }

    // ── Page loading and ads ────────────────────────────────────────────────

    /** pageLoadStrategy from config: normal | eager | none (default eager). */
    private static PageLoadStrategy pageLoadStrategy() {
        String value = ConfigReader.get("pageLoadStrategy", "eager");
        try {
            return PageLoadStrategy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "Config key 'pageLoadStrategy' must be normal, eager or none - but was '" + value + "'.", e);
        }
    }

    /**
     * Tells Chrome/Edge, through the DevTools Protocol (CDP), to refuse requests
     * to the ad domains. Firefox has no CDP, so it relies on eager loading alone.
     *
     * If blocking fails (for example a future Chrome changes the CDP command),
     * the run carries on without it rather than failing every test.
     */
    private static void blockAdDomains(ChromiumDriver driver) {
        try {
            driver.executeCdpCommand("Network.enable", Map.of());
            driver.executeCdpCommand("Network.setBlockedURLs", Map.of("urls", AD_URL_PATTERNS));
        } catch (RuntimeException e) {
            System.err.println("Could not block ad domains - continuing without it: " + e.getMessage());
        }
    }

    /**
     * A cache folder shared by every Chrome/Edge the tests start, or null if
     * browserCache=false. It lives in your home folder, NOT under target/, so
     * `mvn clean` doesn't empty it and the slow first download isn't repeated.
     *
     * Note for parallel runs (Session 17): browsers running at the same time
     * shouldn't share one cache folder - switch this off or make it per-thread.
     */
    private static String browserCacheDir() {
        if (!ConfigReader.getBoolean("browserCache", true)) {
            return null;
        }
        Path dir = Path.of(System.getProperty("user.home"), ".bookstore-automation", "browser-cache");
        try {
            Files.createDirectories(dir);
            return dir.toAbsolutePath().toString();
        } catch (IOException e) {
            System.err.println("Could not create browser cache folder " + dir
                + " - continuing without a shared cache: " + e.getMessage());
            return null;
        }
    }

    // ── Browser options ─────────────────────────────────────────────────────

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase().contains("linux");
    }

    private static ChromeOptions chromeOptions(boolean headless, PageLoadStrategy loadStrategy, String cacheDir) {
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(loadStrategy);
        if (cacheDir != null) {
            options.addArguments("--disk-cache-dir=" + cacheDir); // reuse downloaded files across tests
        }
        if (headless) {
            options.addArguments("--headless=new");          // Chrome's current headless mode
            options.addArguments("--window-size=1920,1080"); // headless has no screen to maximise to
        }
        options.addArguments("--disable-notifications");    // no "allow notifications?" pop-ups
        if (isLinux()) {
            options.addArguments("--no-sandbox");             // needed in Linux CI containers
            options.addArguments("--disable-dev-shm-usage");  // avoids crashes on small CI machines
        }
        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless, PageLoadStrategy loadStrategy) {
        FirefoxOptions options = new FirefoxOptions();
        options.setPageLoadStrategy(loadStrategy);
        if (headless) {
            options.addArguments("-headless");
        }
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless, PageLoadStrategy loadStrategy, String cacheDir) {
        EdgeOptions options = new EdgeOptions();
        options.setPageLoadStrategy(loadStrategy);
        if (cacheDir != null) {
            options.addArguments("--disk-cache-dir=" + cacheDir);
        }
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }
        return options;
    }
}
