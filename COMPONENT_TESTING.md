# How mocking works in this repo

The "mocking" here is a **subclass-and-override** style — there are no Mockito or framework mocks. Two layers get swapped:

## 1. The data store layer (`RadiatorStore`)

See `radiator/src/main/java/com/paulhammant/buildradiator/radiator/RadiatorStore.java:88`.

`RadiatorStore` is the in-memory base class with two extension hooks: `getFromDataService()` (returns `null` by default) and `saveInDataService()` (no-op). The real persistence lives in the inner subclass `RadiatorStore.BackedByGoogleCloudDataStore` (line 108), which overrides those hooks to talk to Google Cloud Datastore.

`RadiatorApp` picks one or the other at boot based on the `GCLOUD_PROJECT` env var (`RadiatorApp.java:33-37`). In tests, that env var is unset, so the plain in-memory `RadiatorStore` is used — Datastore is effectively stubbed out by *not selecting* it. Tests then poke radiators directly into `radiatorStore.actualRadiators` (a `ConcurrentHashMap`).

## 2. The Jooby app itself (`RadiatorApp`)

Replaced with `TestVersionOfBuildRadiatorApp` (`radiator/src/test/java/com/paulhammant/buildradiator/radiator/TestVersionOfBuildRadiatorApp.java`).

This subclass:

- Overrides `getBasePath()` to mount under `/contrived/for/testing` so the test domain space is isolated.
- In its constructor, hooks `onStarted` to seed a test-supplied `Radiator` straight into `radiatorStore.actualRadiators` and to `deleteDefaultRadiator()` (wiping the demo data the parent's `starterData()` injects).
- Overrides `serveRadiatorPage()` / `serveRadiatorComponent()` to rewrite the HTML/Vue on the fly: swaps `vue.min.js` → `vue.js` for debuggability, and (when `withFasterRefresh()` is set) rewrites the JS poll interval from `30000` → `300` ms so `confirmDataRefreshes` doesn't have to wait 30s.
- Bolts on `BuildRadiatorStaticResources` via `use(...)` because the test app is a standalone Jooby module without the main wiring.

## 3. Selenium test flow

See `radiator/src/test/java/com/paulhammant/buildradiator/radiator/RadiatorWebDriverTest.java`.

Each test:

1. Builds a `Radiator` POJO with `TestRadBuilder` helpers (`rad(...)`, `build(...)`, `step(...)`).
2. Hands it to `new TestVersionOfBuildRadiatorApp(rad)`, calls `app.start()`, waits for `appStarted`.
3. Points ChromeDriver at `http://t<N>.localhost:8080/contrived/for/testing#...`. The `tN.localhost` trick gives each test a fresh subdomain so browser-side state (LocalStorage, etc.) isn't shared.
4. Asserts the rendered DOM via FluentSelenium.
5. `confirmDataRefreshes` mutates the in-memory `Radiator` mid-test (`rad.stepPassed("1","A")`) and watches the page re-render — the "live" data is just a Java object the test still holds a reference to.

There's also a `TestRandomGenerator` (subclass of `RandomGenerator`) used in unit tests to make `generateRadiatorCode()` / `generateSecret()` deterministic — same pattern, override-not-mock.

## Summary

The lower tier (GCP Datastore) is replaced by *not instantiating it* and using the in-memory parent class; the app under test is a `Jooby` subclass that exposes seams for seeding state, faster polling, and a sandboxed base path; tests drive a real Chrome browser against this lighter app on localhost.

## Patterns used (and not used)

- **Template Method (GoF)** — the dominant pattern. `RadiatorStore` exposes `getFromDataService()` / `saveInDataService()` as protected hooks; `BackedByGoogleCloudDataStore` overrides them. `RadiatorApp` exposes `serveRadiatorPage()`, `serveRadiatorComponent()`, `getBasePath()`, `getResultsStore()`; `TestVersionOfBuildRadiatorApp` overrides them. `RandomGenerator` is subclassed inline (anonymous class at `RadiatorApp.java:278`) and as `TestRandomGenerator` to get deterministic codes.
- **Method-argument injection (mild "by-hand DI")** — `createRadiator(RandomGenerator codeGenerator, ...)` takes the collaborator as a parameter. That is the only place a dependency is actually passed in. Constructor-injection-style by-hand DI is *not* used: `RadiatorApp` `new`s its own `RadiatorStore` and `RandomGenerator` in an instance initializer.
- **Env-var-driven factory choice** — the `if (gae_appId != null) ... else ...` block at `RadiatorApp.java:33-37` picks the store implementation by environment. It is the *spirit* of a service locator (runtime selection of an impl) but without a registry/locator object — just an `if`.
- **Jooby module install** — `install(new JacksonModule())` is a Jooby plugin mechanism, not bean-graph DI.

### Restated, briefly

- No singletons (no private-constructor + `getInstance()`).
- No service locator (no central registry / JNDI-style lookup).
- No DI containers (no Guice, Spring, Dagger, `@Inject`, `Injector`, `Module` wiring).
- No mocking framework (no Mockito, no EasyMock, no proxy-based fakes).
- Substitution is achieved by subclass-and-override, not by interfaces + injected impls.
- Tests reach into `protected` fields (e.g. `radiatorStore.actualRadiators`) rather than going through a seam — the template-method seams are the seam.

## Possible future directions

These are not endorsements — just sketches of where the design could go if the test surface or the production wiring grew enough to justify the change.

### Introduce Mockito (or similar)

Mockito *could* be used, but is not presently. Whether it would make tests more elegant or terse is genuinely mixed:

- **Where it would help.** A few unit tests (`RadiatorStoreUnitTest`, `RadiatorUnitTest`) currently rely on bespoke subclasses like `TestRandomGenerator` to stub one or two methods. A one-liner `when(rng.generateRadiatorCode()).thenReturn("xxx")` is shorter than a named subclass file, and `verify(...)` would replace ad-hoc "did the right method get called?" assertions that today read state back out of the model.
- **Where it would not help.** The Selenium tests do not have anything Mockito would naturally replace. The "mock" there is the *whole app stack minus Datastore*, exercised end-to-end through a real browser. Mockito is for collaborator-level stubbing, not for swapping out an entire persistence tier behind a running HTTP server. Mocking `RadiatorStore` in those tests would defeat the point — the test wants the real in-memory store so state mutations from the test thread are visible to the HTTP-handling thread.
- **Cost.** Adopting Mockito tends to pull the codebase toward interface-per-collaborator and constructor injection, because mocking concrete classes with state (like `RadiatorStore`) is awkward and brittle. That is a larger refactor than just adding a dependency.

Net: worth it for the small unit tests, marginal-to-negative for the WebDriver tests.

### Constructor injection + interfaces

If `RadiatorStore` and `RandomGenerator` became interfaces, and `RadiatorApp` took them as constructor arguments, the env-var `if/else` in `RadiatorApp.java:33-37` would move to a small composition root (`BuildRadiatorMain`). Tests would pass fakes in directly instead of subclassing the app. This would also unlock Mockito cleanly. The cost is a meaningful rewrite of the Jooby app's instance-initializer style and the loss of the "just override one protected method" ergonomic that tests currently enjoy.

### A DI container (Guice / Dagger / Spring)

Almost certainly not worth it at this size. The graph is two collaborators deep. A container would add ceremony without reducing wiring code.

### Pull seams out of `protected` fields

Tests currently reach into `radiatorStore.actualRadiators` (a `ConcurrentHashMap`) to seed state. A small `seed(Radiator)` method on `RadiatorStore` (test-only or package-private) would document the seam and let `actualRadiators` go back to being private. Low-cost, modest readability win.

### Replace the env-var switch with an explicit factory

`RadiatorApp` could accept a `RadiatorStoreFactory` (or just a `Supplier<RadiatorStore>`) instead of branching on `GCLOUD_PROJECT` inside its initializer. Production wiring picks the GCP-backed factory; tests pick the in-memory one; the conditional disappears. Pairs naturally with constructor injection above.

### Adopt a BDD-style runner (e.g. Cuppa)

[Cuppa](https://github.com/cuppa-framework/cuppa) brings Mocha-style `describe(...)` / `it(...)` blocks to Java. It is orthogonal to the mocking strategy — `TestVersionOfBuildRadiatorApp` would still be the seam — but it would change the *shape* of the test suite:

- **Names become sentences.** `pageCanHaveStepsReplaceOnBrowserSide` → `it("replaces step codes from URL fragment with friendly names", ...)`. Reports read like docs.
- **Shared setup via nested `describe`.** Most WebDriver tests build a `Radiator`, wrap it in `TestVersionOfBuildRadiatorApp`, start the server, open a page — a ~5-line ritual. JUnit `@Before` lifts it one level; Cuppa lets you nest, so e.g. `describe("with faster refresh", () -> { beforeEach(() -> app.withFasterRefresh()); ... })` scopes `withFasterRefresh()` to one subtree without a new test class.
- **Parameterized tests trivially.** `columnsShouldBeProportional` and `columnsShouldBeProportionalEvenWithDurationsOfZero` are nearly identical — a plain `for` loop inside a `describe` collapses them.

What it does *not* change:

- Mocking story is identical. Cuppa is a runner/structure, not a fakes library.
- ChromeDriver lifecycle is still a once-per-suite concern; Cuppa has hooks for it but it is not noticeably terser than `@BeforeClass`.
- It is a niche framework (ForgeRock-origin, low recent activity vs. JUnit 5). Adoption cost: every reader has to learn it.

Net: real ergonomic win for the shape of the suite, zero change to the mocking strategy. JUnit 5's `@Nested` + `@DisplayName` covers some of the same ground with a more familiar dependency, and is worth weighing against Cuppa.

#### Other Mocha/Jest-alikes for Java

Cuppa is not the only one. The shape is well-trodden ground; the catch is that nearly all options are lightly maintained.

- **[Spectrum](https://github.com/greghaskins/spectrum)** — JUnit runner, Jasmine/RSpec-inspired `describe`/`it`. Last release September 2017.
- **[j8spec](https://github.com/j8spec/j8spec)** — `describe`/`it` for Java, RSpec/Jasmine-style. Last release February 2022 — the freshest of the Mocha-alikes.
- **[Oleaster](https://github.com/mscharhag/oleaster)** — JUnit runner with `describe`/`it`, plus an optional Jasmine-style matcher library (`expect(...).toEqual(...)`). Last release June 2017.
- **Cuppa** itself — same family, same caveats.

All four share the same DNA: JDK 8+, lambdas inside an instance initializer (`{{ describe(...) }}` is the giveaway), and small-team/one-person stewardship. None has the institutional weight of JUnit 5 or TestNG.

Two honourable mentions that are *not* quite the same shape but solve overlapping problems:

- **JUnit 5 with `@Nested` + `@DisplayName`** — gets ~70% of the readability win without a third-party dependency. Less terse (a class per `describe`, a method per `it`), but you keep the entire JUnit ecosystem: IDE integration, CI reporters, Surefire, every assertion library. For this repo, the pragmatic answer.
- **[Spock](https://spockframework.org/)** (Groovy) — `def "should do X"() { ... }` with `given:` / `when:` / `then:` blocks. Not Mocha-shaped (no nested `describe`), but BDD-flavoured and *actively maintained* with a real community. Cost: introduces Groovy.

**Recommendation for this repo.** If the goal is sentence-style names + nested setup, **JUnit 5 `@Nested`** is the lowest-risk move. If lambda `describe`/`it` is genuinely wanted, **j8spec** is the freshest of the Mocha-alikes — but it is still a bet on a small project. **Spock** is the outlier: different shape, different language, but actually maintained.
