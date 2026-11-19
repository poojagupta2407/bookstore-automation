# ============================================================
# Feature:     Login
# Scenarios:   LOG-01 to LOG-08 in Test-Scenarios.md
# Pages:       pages/LoginPage.java, pages/ProfilePage.java
# Steps:       stepDefinitions/LoginSteps.java
# Locators:    locators.yml -> LoginPage, ProfilePage
#
# Credentials never appear in this file. "valid credentials" and
# "my username" are read by the step code from the environment
# variables BOOKSTORE_USERNAME and BOOKSTORE_PASSWORD.
#
# @wip = Gherkin agreed, step code not written yet. The runner skips @wip.
#
# @disabled (whole feature, 2026-11-02): login runs were failing, so login is
# parked and the other features no longer depend on it. The runner skips
# @disabled. To bring login back: remove @disabled below, check the stored
# BOOKSTORE_PASSWORD (a 10-character value was typed; the account's password
# is 12 characters), then run LOG-01 on its own first.
#
# Run this feature:   mvn clean test -Dcucumber.filter.tags="@login"
# ============================================================

@ui @login @regression @disabled
Feature: Login

  As a registered Book Store user
  I want to log in with my username and password
  So that I can see and manage my book collection

  Background:
    Given I am on the login page


  # LOG-01
  @wip
  Scenario: Log in with valid credentials
    When I log in with valid credentials
    Then I should be on my profile page
    And my username should be shown on the profile


  # LOG-02 (a) - a username that doesn't exist
  @negative @wip
  Scenario Outline: Logging in with an unknown username is rejected
    When I log in with username "<username>" and password "<password>"
    Then I should see the login error "Invalid username or password!"
    And I should be on the login page

    Examples:
      | username            | password      |
      | not_a_real_user_zz9 | Wrong@12345   |
      | another_ghost_user  | AlsoWrong@123 |


  # LOG-02 (b) - a real username with the wrong password
  # Expect the SAME message as (a): a login form should never reveal
  # whether a username exists. (Message to confirm on the first run.)
  @negative @wip
  Scenario: Logging in with the wrong password is rejected
    When I log in with my username and password "Wrong@12345"
    Then I should see the login error "Invalid username or password!"
    And I should be on the login page


  # LOG-03
  @negative @wip
  Scenario: Logging in with both fields empty keeps you on the login page
    When I log in with username "" and password ""
    Then I should be on the login page


  # LOG-04  (Session 12 - needs the Excel reader)
  @negative @wip
  Scenario Outline: Invalid logins from the spreadsheet are rejected (<caseId>)
    When I log in using case "<caseId>" from sheet "InvalidLogins"
    Then I should see that case's expected login error

    Examples:
      | caseId |
      | IL-01  |
      | IL-02  |
      | IL-03  |


  # LOG-05
  @wip
  Scenario: Logging out returns to the login page
    When I log in with valid credentials
    And I log out
    Then I should be on the login page


  # LOG-06
  @wip
  Scenario: The password field hides what is typed
    Then the password field should hide its input


  # LOG-07
  @wip
  Scenario: The New User button opens the registration page
    When I choose to register as a new user
    Then I should be on the registration page


  # LOG-08
  @wip
  Scenario: The user stays logged in after refreshing the page
    When I log in with valid credentials
    And I refresh the page
    Then I should be on my profile page
