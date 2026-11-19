package utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * ConfigReader - the single place every setting is read from.
 *
 * Each key is looked up in this order; the first non-blank value wins:
 *
 *   1. System property      mvn clean test -Dbrowser=firefox
 *   2. Environment variable BOOKSTORE_BROWSER=firefox
 *   3. config.properties    browser=chrome
 *
 * Why this order: the file holds safe defaults that are committed to Git.
 * Anything secret (credentials) or run-specific (headless in CI) is supplied
 * from outside, so the same code runs on your laptop and in GitHub Actions
 * without being edited.
 *
 * Why the BOOKSTORE_ prefix on environment variables: Windows already defines
 * USERNAME as your Windows account name. Without a prefix, the key "username"
 * would silently pick that up instead of your DemoQA user.
 *
 * Design reference: Framework-Design.md, section 2 (utils) and decision D5.
 */
public final class ConfigReader {

    private static final String CONFIG_FILE = "config.properties";
    private static final String ENV_PREFIX  = "BOOKSTORE_";

    private static final Properties FILE_PROPERTIES = load();

    private ConfigReader() {
        // Utility class: only static methods, so nobody should create an instance.
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Returns the value for a required key.
     * Fails immediately if the key is missing, rather than returning null and
     * causing a confusing NullPointerException somewhere else later.
     */
    public static String get(String key) {
        String value = resolve(key);
        if (value == null) {
            throw new IllegalStateException(
                "Missing config key '" + key + "'. Set it in one of these places:\n"
                + "  1. Maven:       mvn clean test -D" + key + "=value\n"
                + "  2. Environment: " + envName(key) + "=value\n"
                + "  3. File:        src/test/resources/" + CONFIG_FILE);
        }
        return value;
    }

    /** Returns the value for an optional key, or the default if it isn't set. */
    public static String get(String key, String defaultValue) {
        String value = resolve(key);
        return value == null ? defaultValue : value;
    }

    /** Returns a required whole-number setting, such as a timeout in seconds. */
    public static int getInt(String key) {
        String value = get(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                "Config key '" + key + "' must be a whole number, but was '" + value + "'.", e);
        }
    }

    /**
     * Returns a true/false setting.
     *
     * Deliberately strict: Java's own Boolean.parseBoolean("yes") quietly
     * returns false, so a typo like "headless=ture" would run with a visible
     * browser and nobody would notice. Here anything other than true/false fails.
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = resolve(key);
        if (value == null) {
            return defaultValue;
        }
        if (value.equalsIgnoreCase("true"))  return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalStateException(
            "Config key '" + key + "' must be true or false, but was '" + value + "'.");
    }

    // ── Internals ───────────────────────────────────────────────────────────

    /** Applies the lookup order. Returns null if the key isn't set anywhere. */
    private static String resolve(String key) {
        String fromSystem = System.getProperty(key);
        if (isSet(fromSystem)) {
            return fromSystem.trim();
        }

        String fromEnvironment = System.getenv(envName(key));
        if (isSet(fromEnvironment)) {
            return fromEnvironment.trim();
        }

        String fromFile = FILE_PROPERTIES.getProperty(key);
        if (isSet(fromFile)) {
            return fromFile.trim();
        }

        return null;
    }

    /** username -> BOOKSTORE_USERNAME,  page.timeout -> BOOKSTORE_PAGE_TIMEOUT */
    private static String envName(String key) {
        return ENV_PREFIX + key.replace('.', '_').toUpperCase();
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Loads config.properties from the classpath once, when the class is first used.
     *
     * Read as UTF-8 on purpose: Properties.load(InputStream) assumes ISO-8859-1,
     * which would mangle any non-English character in a value.
     */
    private static Properties load() {
        Properties properties = new Properties();

        InputStream stream = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        if (stream == null) {
            throw new IllegalStateException(
                CONFIG_FILE + " was not found on the classpath. "
                + "Expected it at src/test/resources/" + CONFIG_FILE);
        }

        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read " + CONFIG_FILE + ": " + e.getMessage(), e);
        }
        return properties;
    }
}
