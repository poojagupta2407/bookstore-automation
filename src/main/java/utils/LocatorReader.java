package utils;

import org.openqa.selenium.By;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LocatorReader - turns XPath entries in locators.yml into Selenium By objects.
 *
 *   locators.yml:   login_button: "//button[@id='login']"
 *   Java:           By login = LocatorReader.get("LoginPage", "login_button");
 *                   -> By.xpath("//button[@id='login']")
 *
 *   locators.yml:   book_link: "//td//a[normalize-space()='{bookTitle}']"
 *   Java:           LocatorReader.getDynamic("BooksPage", "book_link", "Git Pocket Guide")
 *                   -> By.xpath("//td//a[normalize-space()='Git Pocket Guide']")
 *
 * Every locator is XPath (project convention, 2026-11-02), so each entry is
 * just a string - no type to declare.
 *
 * Page classes normally don't call this directly - BasePage.loc("key") does,
 * using the page's own class name as the section name.
 *
 * Why locators live in YAML (Framework-Design.md, D2): a UI change is a data
 * edit with no recompile, and every locator is in one file. The cost is no
 * compile-time check on key names, so a wrong key fails with a message that
 * lists the keys that DO exist.
 *
 * The file is loaded once, when the class is first used, and only read after
 * that - so it is safe when tests run in parallel.
 */
public final class LocatorReader {

    private static final String LOCATOR_FILE = "locators.yml";

    /** Finds a placeholder wrapped in quotes, e.g.  '{bookTitle}'  or  "{bookTitle}" */
    private static final Pattern QUOTED_PLACEHOLDER = Pattern.compile("(['\"])\\{[^}]+}\\1");

    /** Finds a bare placeholder, e.g.  {index}  in  //tbody/tr[{index}] */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    /**
     * The whole file, loaded once:  page name -> (locator key -> XPath string).
     * Held as plain Objects and checked on every lookup, because the YAML is
     * hand-edited and may not have the shape we expect (see xpathFor()).
     */
    private static final Map<String, Object> LOCATORS = load();

    private LocatorReader() {
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /** A fixed locator:  get("LoginPage", "login_button")  ->  By.xpath("//button[@id='login']") */
    public static By get(String page, String key) {
        return By.xpath(xpathFor(page, key));
    }

    /**
     * A locator with a {placeholder} filled in at runtime.
     *
     * Apostrophes: in XPath, 'You Don't Know JS' is broken - the apostrophe
     * ends the string early, and XPath has no escape character. So when the
     * placeholder sits inside quotes, the quotes are replaced by a safe XPath
     * string instead:
     *   "You Don't Know JS"                     (no double quote in the value)
     *   concat('a', "'", 'b')                   (value has both kinds of quote)
     * A placeholder NOT inside quotes - e.g. a row number in //tbody/tr[{index}] -
     * is filled in as-is.
     */
    public static By getDynamic(String page, String key, String value) {
        String template = xpathFor(page, key);

        Matcher quoted = QUOTED_PLACEHOLDER.matcher(template);
        String filled;
        if (quoted.find()) {
            filled = quoted.replaceFirst(Matcher.quoteReplacement(xpathString(value)));
        } else if (PLACEHOLDER.matcher(template).find()) {
            filled = PLACEHOLDER.matcher(template).replaceFirst(Matcher.quoteReplacement(value));
        } else {
            throw new IllegalArgumentException(
                "Locator " + page + "." + key + " has no {placeholder} - use get() instead of getDynamic().");
        }
        return By.xpath(filled);
    }

    // ── Internals ───────────────────────────────────────────────────────────

    /** Builds a valid XPath string literal for any text, including apostrophes. */
    static String xpathString(String text) {
        if (!text.contains("'")) {
            return "'" + text + "'";
        }
        if (!text.contains("\"")) {
            return "\"" + text + "\"";
        }
        // Contains both ' and " - split on ' and glue the pieces back with concat()
        String[] parts = text.split("'", -1);
        StringBuilder xpath = new StringBuilder("concat(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                xpath.append(", \"'\", ");
            }
            xpath.append('\'').append(parts[i]).append('\'');
        }
        return xpath.append(')').toString();
    }

    /**
     * Finds one locator and checks it really is an XPath string.
     *
     * The YAML is edited by hand, so these mistakes are easy to make:
     *   login_button: login                       <- an id, not an XPath
     *   login_button: { type: id, value: login }  <- the old format
     *   login_button:                             <- left empty
     * Each one fails here with a message naming the page, the key and the fix,
     * instead of a confusing Selenium error later.
     */
    private static String xpathFor(String page, String key) {
        Object section = LOCATORS.get(page);
        if (section == null) {
            throw new IllegalArgumentException(
                "No section '" + page + "' in " + LOCATOR_FILE + ". Sections that exist: " + LOCATORS.keySet());
        }
        if (!(section instanceof Map)) {
            throw new IllegalArgumentException(
                "Section '" + page + "' in " + LOCATOR_FILE
                + " must list locators, like:  login_button: \"//button[@id='login']\"");
        }
        Map<?, ?> locators = (Map<?, ?>) section;

        Object raw = locators.get(key);
        if (raw == null) {
            throw new IllegalArgumentException(
                "No locator '" + key + "' under '" + page + "' in " + LOCATOR_FILE
                + ". Keys that exist: " + locators.keySet());
        }
        if (!(raw instanceof String) || ((String) raw).isBlank()) {
            throw new IllegalArgumentException(
                "Locator " + page + "." + key + " must be an XPath string, like  \"//button[@id='login']\""
                + " - but is: " + raw);
        }

        String xpath = ((String) raw).trim();
        if (!(xpath.startsWith("/") || xpath.startsWith("(") || xpath.startsWith("."))) {
            throw new IllegalArgumentException(
                "Locator " + page + "." + key + " doesn't look like an XPath (it should start with / or ( ): "
                + xpath + "   Did you mean  \"//*[@id='" + xpath + "']\" ?");
        }
        return xpath;
    }

    @SuppressWarnings("unchecked") // checked just above the cast: it IS a Map
    private static Map<String, Object> load() {
        InputStream stream = LocatorReader.class.getClassLoader().getResourceAsStream(LOCATOR_FILE);
        if (stream == null) {
            throw new IllegalStateException(
                LOCATOR_FILE + " was not found on the classpath. Expected it at src/test/resources/" + LOCATOR_FILE);
        }
        try (InputStream in = stream) {
            Object loaded = new Yaml().load(in);
            if (loaded == null) {
                throw new IllegalStateException(LOCATOR_FILE + " is empty - it has no page sections yet.");
            }
            if (!(loaded instanceof Map)) {
                throw new IllegalStateException(
                    LOCATOR_FILE + " must start with page sections, like  LoginPage:  on its own line.");
            }
            return (Map<String, Object>) loaded;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not read " + LOCATOR_FILE + ": " + e.getMessage(), e);
        }
    }
}
