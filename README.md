# BookStore Automation
[![CI](https://github.com/poojagupta2407/bookstore-automation/actions/workflows/ci.yml/badge.svg)](https://github.com/poojagupta2407/bookstore-automation/actions/workflows/ci.yml)

BDD UI automation for the [DemoQA Book Store](https://demoqa.com/books) —
Selenium 4 · Cucumber 7 · TestNG · Maven · Java 21.

| Document | What it's for |
|---|---|
| `../Framework-Design.md` | The frozen design (v1.0). Every class traces back to it. |
| `../Test-Scenarios.md` | What to automate — 87 scenarios with IDs and priorities |
| `../Learning-Log.md` | Concepts learned, in interview-answer form |

**Status:** empty skeleton — folders, build files and config only. No Java yet.

---

## Open in IntelliJ

1. **File → Open** → select this folder's **`pom.xml`** → **Open as Project** → **Trust Project**
2. Wait for the Maven import to finish (progress bar, bottom right)
3. Check the folder colours in the Project panel:
   - `src/main/java` — **blue** (sources)
   - `src/test/java` — **green** (test sources)
   - `src/test/resources` — resources icon
   If they're grey: right-click `pom.xml` → **Maven → Reload Project**
4. **File → Project Structure → Project** → SDK **21**
5. Maven panel (right edge) → **Lifecycle → compile** → expect `BUILD SUCCESS`

`mvn clean test` will fail with *Cannot find class runners.TestRunner* until
build step 7. That's expected.

---

## Structure

```
src/
├── main/java/
│   ├── base/              BasePage.java
│   ├── pages/             LoginPage, ProfilePage, BooksPage (+ BookDetailPage)
│   └── utils/             DriverFactory, ConfigReader, LocatorReader
└── test/
    ├── java/
    │   ├── stepDefinitions/
    │   ├── hooks/
    │   └── runners/
    └── resources/
        ├── features/
        ├── config.properties
        └── locators.yml
```

`.gitkeep` files only hold empty folders in place for Git. Delete each one once
its folder has a real file.

---

## Credentials

There are none in this repo. **The first test (LOG-01, build step 7) logs in,
so set them before step 7.** Once, in PowerShell:

```powershell
setx BOOKSTORE_USERNAME "your_demoqa_user"
setx BOOKSTORE_PASSWORD "your_demoqa_password"
```

Then **fully close and reopen IntelliJ** — `setx` only affects programs started
afterwards. Check in a *new* PowerShell window: `echo $env:BOOKSTORE_USERNAME`.

---

## Build order

Each step ends with something you can check. Tick it off before moving on.

| Step | Write | How you know it works | Scenarios |
|---|---|---|---|
| ☑ 1 | — (skeleton) | `mvn clean compile` → BUILD SUCCESS | |
| ✍ 2 | `utils/ConfigReader.java` | Compiles | |
| ✍ 3 | `utils/DriverFactory.java` | Compiles | |
| ✍ 4 | `utils/LocatorReader.java` + `locators.yml` (LoginPage, ProfilePage, BooksPage) | Compiles | |
| ✍ 5 | `base/BasePage.java` | Compiles | |
| ✍ 6 | `pages/LoginPage.java`, `pages/ProfilePage.java` | Compiles | |
| ✍ 7 | `hooks/Hooks.java`, `runners/TestRunner.java`, `stepDefinitions/LoginSteps.java`. **Credentials must be set first.** | **First green test** — Chrome opens, logs in, profile shows your username | LOG-01 |
| ✍ 8 | `pages/BooksPage.java`, `stepDefinitions/BooksSteps.java` (all steps real except BKS-09's API step) | Logs in, opens the catalogue, lists 8 books | BKS-01 |
| ☐ 9 | Remaining login steps in `LoginSteps`; remove `@wip` | Pass | LOG-02, LOG-03, LOG-05 |
| ☐ 10 | Remove `@wip` from BKS-02 – BKS-08 one at a time (step code already written) | Pass | BKS-02 – BKS-08 |
| ☐ 11 | `pages/BookDetailPage.java`, `features/book-detail.feature` | Detail scenarios pass *(apostrophe-safe XPath already in `LocatorReader`)* | DTL-01, DTL-03, DTL-04 |
| ☐ 12 | `features/profile.feature`, `stepDefinitions/ProfileSteps.java` | Profile scenarios pass | PRF-01, PRF-09, PRF-10 |

Steps 1–12 complete **Wave 1** of `Test-Scenarios.md`.

☑ done and verified on your machine · ✍ written, waiting for your first run · ☐ not started

---

## Conventions

- Put the scenario ID in a comment above each scenario, so a failing report
  points straight back to `Test-Scenarios.md`:
  ```gherkin
  # LOG-01
  @smoke
  Scenario: Log in with valid credentials
  ```
- No locator in any `.java` file — `locators.yml` only.
- No assertion in a page class — step definitions only.
- No `Thread.sleep`.
- Any change to the design gets a line in `Framework-Design.md` section 11.

---

## Commands

```bash
mvn clean compile                                                # build only
mvn clean test                                                   # every scenario except @wip
mvn clean test -Dcucumber.filter.tags="@smoke and not @wip"
mvn clean test -Dcucumber.filter.tags="@books and not @wip"
mvn clean test -Dbrowser=firefox -Dheadless=true
```

**Always add `and not @wip` when you filter.** A `-Dcucumber.filter.tags`
value *replaces* the runner's default `not @wip` rather than adding to it —
without it, unfinished scenarios run and fail on their missing steps.

Report after each run: `target/cucumber-reports/cucumber.html` (open in a browser).
