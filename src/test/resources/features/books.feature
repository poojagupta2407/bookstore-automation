# ============================================================
# Feature:     Book Store catalogue
# Scenarios:   BKS-01 to BKS-09 in ../../../../../Test-Scenarios.md
# Page:        pages/BooksPage.java
# Steps:       stepDefinitions/BooksSteps.java
# Locators:    locators.yml -> BooksPage
#
# Login is BYPASSED (decision 2026-11-02): the book store is a public page,
# so every scenario opens it directly - no credentials needed, and a login
# problem can no longer fail these tests. (From 2026-10-25 to 2026-11-02 the
# Background logged in first; that dependency was removed while login is
# @disabled.)
#
# Expected data verified on the live site on 2026-10-24.
#
# @wip = Gherkin agreed, step code not written yet. The runner skips @wip.
#        Remove the tag when a scenario's steps are automated.
#
# Run this feature:   mvn clean test -Dcucumber.filter.tags="@books"
# Run smoke only:     mvn clean test -Dcucumber.filter.tags="@smoke"
# ============================================================

@ui @books @regression
Feature: Book Store catalogue

  As a visitor to the Book Store
  I want to browse and search the book catalogue
  So that I can find the books I'm interested in

  Background:
    # LOGIN BYPASSED (2026-11-02) - kept for when login works again on the UI.
    # To restore the logged-in journey: uncomment the two lines below and
    # delete the active "Given I am on the book store page" line under them.
    #   Given I am logged in
    #   And I am on the book store page
    Given I am on the book store page


  # BKS-01
  @toBeRunNow
  Scenario: The catalogue lists every book
    Then the number of books listed should be 8


  # BKS-02, BKS-04, BKS-06
  # One scenario, three sets of examples: the search box matches title,
  # author and publisher, and ignores letter case - same steps every time.
  Scenario Outline: Searching shows the matching book
    When I search for "<keyword>"
    Then the book "<title>" should be listed
    And the number of books listed should be <count>

    # BKS-02
    @toBeRunNow
    Examples: By title
      | keyword   | title                                     | count |
      | Git       | Git Pocket Guide                          | 1     |
      | Design    | Learning JavaScript Design Patterns       | 2     |
      | Evolvable | Designing Evolvable Web APIs with ASP.NET | 1     |

    # BKS-04
    Examples: By author
      | keyword | title            | count |
      | Richard | Git Pocket Guide | 2     |

    # BKS-06
    @wip
    Examples: Letter case is ignored
      | keyword | title            | count |
      | GIT     | Git Pocket Guide | 1     |


  # BKS-03
  @negative
  Scenario: A search with no match lists no books
    When I search for "zzzz-no-such-book"
    Then no books should be listed


  # BKS-05
  @wip
  Scenario: Searching by publisher lists exactly that publisher's books
    When I search for "No Starch"
    Then the listed books should be:
      | Eloquent JavaScript, Second Edition |
      | Understanding ECMAScript 6          |


  # BKS-07
  @wip
  Scenario: Clearing the search lists every book again
    When I search for "Git"
    And I clear the search
    Then the number of books listed should be 8


  # BKS-08
  @wip
  Scenario: The book table shows the expected columns
    Then the book table should have the columns:
      | Image     |
      | Title     |
      | Author    |
      | Publisher |


  # BKS-09  (Wave 3 - needs the API layer, Session 14)
  @wip
  Scenario: The catalogue on screen matches the Book Store API
    Then the listed books should match the Book Store API
