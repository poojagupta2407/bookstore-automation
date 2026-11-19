package stepDefinitions;

import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import pages.BooksPage;
import utils.DriverFactory;

import java.util.List;

/**
 * BooksSteps - step definitions for features/books.feature (BKS-01 to BKS-09).
 *
 * Every step is real code except the API comparison (BKS-09), which waits for
 * the API layer. Scenarios other than BKS-01 are still tagged @wip in the
 * feature file - remove the tag once a scenario has passed.
 *
 * Rules for this class (Framework-Design.md, section 3):
 *   - no locators, no waits, no driver.findElement
 *   - every assertion lives here, never in BooksPage
 *   - every assertion message says what was expected AND what was found
 *
 * "Given I am logged in" (first Background step) lives in LoginSteps.
 *
 * Cucumber creates a new BooksSteps object for every scenario, so the
 * booksPage field never carries over from one scenario to the next.
 */
public class BooksSteps {

    private BooksPage booksPage;

    // ── Navigation ──────────────────────────────────────────────────────────

    /** Background of every scenario. */
    @Given("I am on the book store page")
    public void iAmOnTheBookStorePage() {
        booksPage = new BooksPage(DriverFactory.getDriver()).open();
        Assert.assertTrue(booksPage.isLoaded(),
            "The book store did not load: no search box or no book rows. URL: "
            + booksPage.getCurrentUrl());
    }

    // ── Actions ─────────────────────────────────────────────────────────────

    /** BKS-02 to BKS-07.   When I search for "Git"   ->   keyword = "Git" */
    @When("I search for {string}")
    public void iSearchFor(String keyword) {
        booksPage.searchFor(keyword);
    }

    /** BKS-07. */
    @When("I clear the search")
    public void iClearTheSearch() {
        booksPage.clearSearch();
    }

    // ── Checks ──────────────────────────────────────────────────────────────

    /**
     * BKS-01, 02, 04, 06, 07.   Then the number of books listed should be 8  ->  expected = 8
     * hasResultCount waits briefly for the count to settle before failing.
     */
    @Then("the number of books listed should be {int}")
    public void theNumberOfBooksListedShouldBe(int expected) {
        Assert.assertTrue(booksPage.hasResultCount(expected),
            "Expected " + expected + " book(s) listed, but " + booksPage.getResultCount() + " are listed.");
    }

    /** BKS-02, 04, 06. */
    @Then("the book {string} should be listed")
    public void theBookShouldBeListed(String title) {
        Assert.assertTrue(booksPage.isBookListed(title),
            "Expected '" + title + "' to be listed. Listed instead: " + booksPage.getListedTitles());
    }

    /** BKS-03. */
    @Then("no books should be listed")
    public void noBooksShouldBeListed() {
        Assert.assertTrue(booksPage.hasNoResults(),
            "Expected no books, but these are listed: " + booksPage.getListedTitles());
    }

    /**
     * BKS-05. The single-column table under the step arrives as a List<String>.
     * Waits for the right NUMBER of rows first, then compares the titles in
     * order - so a slow re-draw can't cause a false failure.
     */
    @Then("the listed books should be:")
    public void theListedBooksShouldBe(List<String> expectedTitles) {
        Assert.assertTrue(booksPage.hasResultCount(expectedTitles.size()),
            "Expected " + expectedTitles.size() + " book(s), but these are listed: "
            + booksPage.getListedTitles());
        Assert.assertEquals(booksPage.getListedTitles(), expectedTitles,
            "Listed books don't match (order matters).");
    }

    /** BKS-08. */
    @Then("the book table should have the columns:")
    public void theBookTableShouldHaveTheColumns(List<String> expectedColumns) {
        Assert.assertEquals(booksPage.getColumnHeaders(), expectedColumns,
            "Book table columns don't match (order matters).");
    }

    /** BKS-09 (Wave 3). Compares the screen with GET /BookStore/v1/Books. */
    @Then("the listed books should match the Book Store API")
    public void theListedBooksShouldMatchTheBookStoreApi() {
        throw new PendingException("Needs the API layer - Session 14");
    }
}
