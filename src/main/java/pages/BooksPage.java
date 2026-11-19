package pages;

import base.BasePage;
import org.openqa.selenium.WebDriver;
import utils.ConfigReader;

import java.util.List;

/**
 * BooksPage - https://demoqa.com/books, the Book Store catalogue.
 *
 * Locators: locators.yml -> BooksPage.
 * The methods here are exactly the ones BooksSteps needs - writing the step
 * definitions first is what decided this list.
 */
public class BooksPage extends BasePage {

    public BooksPage(WebDriver driver) {
        super(driver);
    }

    public BooksPage open() {
        driver.get(ConfigReader.get("booksUrl"));
        return this;
    }

    /**
     * Loaded = search box visible AND at least one book row drawn.
     * The books are fetched after the page itself loads, so the search box
     * can appear while the table is still empty. Checking only the search box
     * would let "count the books" run too early and see 0.
     */
    @Override
    public boolean isLoaded() {
        return isVisible(loc("search_box")) && isVisible(loc("table_rows"));
    }

    // ── Actions ─────────────────────────────────────────────────────────────

    public BooksPage searchFor(String keyword) {
        type(loc("search_box"), keyword);
        return this;
    }

    public BooksPage clearSearch() {
        clearField(loc("search_box"));
        return this;
    }

    // ── Queries ─────────────────────────────────────────────────────────────

    /** Rows in the table right now. Use hasResultCount() to wait for a number. */
    public int getResultCount() {
        return countOf(loc("table_rows"));
    }

    /** Waits briefly for exactly {@code expected} rows; true if it happened. */
    public boolean hasResultCount(int expected) {
        return waitForCount(loc("table_rows"), expected);
    }

    /**
     * DemoQA shows no "no results" message - an unmatched search just leaves
     * zero rows. So "no results" is a row count of 0, not an element to find.
     */
    public boolean hasNoResults() {
        return hasResultCount(0);
    }

    public boolean isBookListed(String title) {
        return isVisible(loc("book_link", title));
    }

    /** Titles currently listed, top to bottom. */
    public List<String> getListedTitles() {
        return getTexts(loc("book_titles"));
    }

    /** Column headings, left to right. */
    public List<String> getColumnHeaders() {
        return getTexts(loc("column_headers"));
    }
}
