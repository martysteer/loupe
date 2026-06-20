# Loupe — Research & Planning Brief

*A Clojure-native OpenRefine extension for textual / character-level data-quality inspection, with a shared core that a CLI can later reuse.*

**Status:** research seed, not a frozen spec. Written to be dropped into Claude Code to brainstorm → spec → plan. The phases, layout, and open questions below are starting points to refine, not commitments.

---

## 0. One-liner

`loupe` is an OpenRefine extension that lets a non-technical user click a menu item (or call a GREL function) and get a quality report on a column — encoding errors, script/diacritic anomalies, whitespace gremlins, transliteration-apostrophe inconsistency, and so on. The checking logic is written in Clojure as a reusable, OpenRefine-agnostic core, so the same code can later back a command-line tool.

---

## 1. Background & motivation

The work began as a library of OpenRefine **custom-facet expressions written in Clojure**, used to QA messy multilingual manuscript metadata (FIHRIST-derived records: Arabic, Persian, Swahili-in-Roman, with heavy transliteration). That library already exists and is the **executable spec / parity target** for this project: see `OpenRefine_Data_Quality_Facets_v2.md`. It is mature but has two limitations we want to fix by turning it into a tool:

1. **Usability.** Pasting Clojure into a custom-facet box is fine for the author but a non-starter for non-technical colleagues. We want one-click / one-function usage.
2. **Reuse.** The checks are currently expression *strings*, not a tested namespace, so they can't be shared with a future batch/CLI reporting tool or unit-tested in isolation.

The destination is therefore: same logic, promoted to a proper Clojure namespace, surfaced through a friendly OpenRefine front end, and structured so a CLI can bolt on later.

---

## 2. Hard constraints / guiding principles

**These are decided. Treat them as guardrails, not open questions.**

1. **Clojure-native.** The check logic *and* the OpenRefine binding are written in Clojure (AOT-compiled to a jar). The only unavoidable non-Clojure surface is the thin client-side UI (menu item / dialog), which must be JavaScript because it runs in the browser. No rewriting the logic in Java.
2. **No CI pipelines.** Builds are local, driven by a **Makefile**. No GitHub Actions, no Travis, no cloud build. (CI is explicitly unwanted — too much maintenance friction.)
3. **GitHub-hosted artifacts.** Distribution is via **GitHub Releases**: a pre-built extension zip, cut manually with one `make` target. Optionally also attach the bare core jar for future library/CLI consumers. **No Maven Central publishing** (for now).
4. **Pure drop-in install.** A user installs by copying the extension folder into OpenRefine's workspace `extensions/` directory and restarting. **No central OS Java library paths, no `CLASSPATH`/`PATH` env vars, no admin rights.** Every dependency the extension needs is bundled inside the extension's own `module/MOD-INF/lib/`, scoped to the extension.
5. **Non-technical-user-first.** Both install and use must be doable from a short README by someone who has never opened a terminal. The install guide is a first-class deliverable, not an afterthought.
6. **One source of truth.** A single Clojure repo with an OpenRefine-agnostic core, so the future CLI shares the *exact* code rather than a parallel reimplementation.
7. **Scope = textual/character-level QA**, not statistical/numeric data profiling. (See Non-goals.)

---

## 3. Prior art reviewed (findings to build on)

### 3.1 RBGKew/String-Transformers — the pattern to borrow, with a caveat
- It is a **100% Java library** published to **Maven Central** (`org.kew.rmf:string-transformers`). Every transformer implements `transform(String) → String`. It contains **no OpenRefine code** — just `src` + `pom.xml`, MIT licence.
- "Using it in OpenRefine" = download the jar (+ Apache Commons-Lang3), drop both into the bundled **Jython** extension's `MOD-INF/lib`, then call them from Jython expressions (`from org.kew.rmf.transformers import CapitalLettersExtractor`).
- "Using it in other software" = just add the Maven dependency.
- **What to borrow:** the *core-library discipline* — logic lives in a standalone, framework-agnostic library; OpenRefine is merely one consumer. This is precisely the "one core, many front ends" shape that makes our CLI goal cheap.
- **What NOT to borrow:** its *user experience*. Manually placing jars in a deep folder and writing import expressions is power-user-grade and the instructions are dated. It also fails our drop-in and non-technical-user constraints. We keep their library discipline but put a click-driven front end on top.

### 3.2 OpenRefine/sample-extension — the front-end skeleton
- A **Butterfly module**: `module/MOD-INF/` (`module.properties`, `controller.js`), `src/` (Java), `pom.xml`; languages ~58% JS, 23% CSS, 19% Java. It's a "Use this template" repo.
- This is the shape of a *real* extension (menus, commands, registered functions) — the thing String-Transformers is *not*. We take the skeleton but drive the server side from Clojure rather than Java where we can.
- Official guide: https://openrefine.org/docs/technical-reference/writing-extensions

### 3.3 OpenRefine's Clojure support — the thing that makes "Clojure-native" plausible
- OpenRefine **bundles Clojure** (1.10.x era) as one of its expression languages, alongside GREL and Jython. Clojure is **not deprecated**; the manual still lists it.
- Limitation: OpenRefine exposes only `value`, `row`, `rowIndex`, `cell`, `cells` to Clojure *expressions*. (Our checks only need `value`, so this is irrelevant to the core; it matters only if we use the "inject a Clojure expression" binding — see §5.)
- Crucially, **OpenRefine already has Clojure on its JVM classpath**, which opens the door to calling our bundled Clojure namespace without necessarily shipping a second Clojure runtime (see Spike 2).

### 3.4 OpenRefine distribution reality
- There is **no in-app extension marketplace/installer**. The supported install is: unzip the extension into the workspace `extensions/` folder and restart. This *is* our drop-in model — it satisfies constraint #4 natively, and a self-contained Butterfly extension makes it the whole process (no jar-shuffling like String-Transformers).
- The workspace extensions directory is per-OS and user-writable (no admin): document the macOS / Windows / Linux paths in the README. That's documentation, not a configuration step.

### 3.5 CLI reuse note
- Because the core will be an OpenRefine-agnostic jar, a future CLI just depends on it. This would be a **JVM or GraalVM-native CLI**, **not babashka** — babashka can't load our compiled core jar as a dependency. The upside is one shared artifact feeding both the extension and the CLI, instead of two drifting implementations.

---

## 4. Proposed architecture

A **single Clojure (`deps.edn`) repo** with a strict layering: a pure core that knows nothing about OpenRefine or CLIs, and thin adapters on top.

```
loupe/
  deps.edn                 ; aliases: :test, :build, :cli
  Makefile                 ; orchestrates compile / jar / extension / zip / clean / test
  build.clj                ; (optional) tools.build script invoked by the Makefile
  src/
    loupe/
      checks.clj           ; THE CORE — pure fns, String -> finding. No OpenRefine, no CLI.
      report.clj           ; aggregation: per-column and per-dataset rollups over checks
      openrefine.clj       ; OpenRefine binding (AOT). gen-class GREL functions and/or
                           ;   callable entrypoints for injected Clojure expressions.
      cli.clj              ; FUTURE — reads CSV/TSV, calls report, prints console/HTML/JSON
  test/
    loupe/
      checks_test.clj      ; pure unit tests — run with no OpenRefine present
  extension/               ; static Butterfly bits; `make extension` fills lib/ with built jars
    module/
      MOD-INF/
        module.properties
        controller.js       ; registers the binding + the column-menu command(s)
        lib/                ; built jar(s) land here (gitignored; produced by the build)
      scripts/              ; thin client-side JS: menu item, results dialog
      styles/               ; CSS
  dist/                    ; build output: dist/loupe/ tree + dist/loupe-<ver>.zip (gitignored)
```

**Layering rule (enforced):**
- `loupe.checks` and `loupe.report` depend on nothing but `clojure.core` and `java.*` interop (`java.text.Normalizer`, `java.lang.Character`, `java.util.regex`). They are the reusable heart and the only thing the CLI needs.
- `loupe.openrefine` is the *only* file that knows OpenRefine exists.
- `loupe.cli` is the *only* file that knows about CLIs.
- Keeping the OpenRefine/CLI knowledge in thin, swappable adapters insulates the bulk of the code from OpenRefine's API churn (it's the adapter + UI that break across OpenRefine versions, not the agnostic core).

**Self-contained drop-in (constraint #4 in practice):** `make extension` AOT-compiles `loupe.openrefine` (pulling in `checks`/`report`), jars it, and copies the jar — plus the Clojure runtime jar *iff* we bundle it (Spike 2) — into `extension/module/MOD-INF/lib/`. OpenRefine adds each extension's `MOD-INF/lib/*.jar` to the classpath at load, scoped to that extension. The user's entire job: unzip `loupe/` into `<workspace>/extensions/` and restart. No global classpath, no env vars, no admin.

---

## 5. The central open question: the Clojure ↔ OpenRefine binding

This is the make-or-break unknown for "Clojure-native + drop-in," and it must be resolved **first** (Phase 0). Three candidate bindings, in rough order of Clojure-nativeness / least Java:

**Binding A — GREL functions via `gen-class`.**
AOT-compile Clojure classes that implement OpenRefine's GREL `Function` interface, and register them in `controller.js`. Users then call `value.loupeReport()` anywhere, with an optional menu command on top.
- *Pro:* cleanest UX; functions available everywhere expressions are.
- *Risk:* `gen-class` implementing OpenRefine's `Function` interface + AOT + classloading inside OpenRefine is the fiddly part; needs validation.

**Binding B — bundled namespace + injected Clojure expression.**
Ship the AOT'd `loupe` jar in `MOD-INF/lib`; a client-side menu command programmatically creates a facet whose Clojure expression calls our namespace (e.g. `(loupe.report/column-report value)`). The user types nothing — the command fills it in.
- *Pro:* most Clojure-native; no Java interface to implement; reuses OpenRefine's bundled Clojure evaluator.
- *Risk:* whether OpenRefine's Clojure expression evaluator can resolve/`require` a namespace from a classpath jar at eval time. Must be tested.

**Binding C — pure-JS expression injection (MVP shortcut / fallback).**
No jar at all: a JS menu that pastes the existing v2 facet expression strings into facets.
- *Pro:* fastest path to something usable; no build/AOT.
- *Con:* logic lives as strings in JS, not a tested namespace; does **not** advance the shared-core/CLI goal. Acceptable only as a stop-gap v0 or last-resort fallback.

**Recommended approach:** spike **B first** (most Clojure-native, lowest effort if namespace resolution works), fall back to **A** if B's eval-time resolution fails or the facet UX is too clunky. Treat **C** as either a throwaway MVP to get feedback early, or the floor if both A and B prove impractical. A thin Java shim around the Clojure jar exists as a final fallback but breaks constraint #1, so avoid unless forced.

---

## 6. De-risking spikes (do these before committing to phases)

- **Spike 1 — Binding feasibility (critical).** Get one trivial AOT'd Clojure function callable inside a *running* OpenRefine, via Binding B (and, if needed, A). Smallest possible end-to-end: `value` → our fn → result visible in a facet or new column. Decide A vs B from the result.
- **Spike 2 — Clojure version coupling.** Identify OpenRefine's bundled Clojure version. Decide: compile against the bundled version (tiny jar, no second runtime, but version-pinned to OpenRefine) **vs** bundle our own `clojure.jar` in `MOD-INF/lib` (fully self-contained, possible classloader/version clash). Validate no AOT conflict either way. *Lean:* compile against the bundled version if classloading cooperates — it keeps the extension jar tiny and the install trivially small.
- **Spike 3 — Menu-command UX.** Confirm a client-side menu command can create the facet / invoke the function so the user types nothing. This is what makes it non-technical-friendly; prove it early.
- **Spike 4 — GraalVM CLI (defer).** Only when the CLI is on the table: does the core AOT-compile under `native-image` (reflection config for `java.text.Normalizer` etc.)? Not blocking for the extension.

**Phase 0 exit criterion:** a "hello-loupe" extension where clicking a menu item runs *one real check* from a bundled Clojure jar and shows the result — installed purely by dropping the folder into `extensions/` and restarting, with no env/PATH setup. If that stands, the Clojure-native bet is proven and the rest is mostly porting.

---

## 7. Build & distribution plan (Makefile, no CI)

Proposed `make` targets (thin orchestration over `clojure`, `java`, `jar`/`zip`; the actual compile may delegate to `tools.build`/`build.clj`, Leiningen, or raw commands — see Open Decisions):

- `make deps` — prefetch dependencies (`clojure -P` / `-X:deps prep`).
- `make test` — run `loupe.checks` unit tests locally (no OpenRefine needed).
- `make compile` — AOT-compile `loupe.openrefine` (+ transitive `checks`/`report`) to `target/classes`.
- `make jar` — package compiled classes into `dist/loupe-<ver>.jar`.
- `make extension` — assemble `dist/loupe/`: the static `extension/module` tree + the built jar(s) (+ Clojure runtime jar if bundling) copied into `MOD-INF/lib/`.
- `make zip` — zip `dist/loupe/` → `dist/loupe-<ver>.zip` for upload.
- `make clean`.

**Releasing (manual, by design):** run `make zip`, then create a GitHub Release and drag the zip onto it. Optionally attach the bare `loupe-<ver>.jar` for future CLI/library consumers. That is the entire release process — no pipeline.

**Installing (what the README tells users):** download `loupe-<ver>.zip` from Releases → unzip into your OpenRefine workspace `extensions/` folder (README lists the macOS/Windows/Linux paths) → restart OpenRefine → the Loupe menu appears. No terminal, no admin, no environment variables.

---

## 8. Phased plan (refine in Claude Code)

- **Phase 0 — De-risk the binding.** Spikes 1–3. *Exit:* the "hello-loupe" drop-in described in §6.
- **Phase 1 — Port the core.** Move the v2 facet logic into `loupe.checks` + `loupe.report`, with unit tests. *Exit:* `make test` green; behavioural parity with `OpenRefine_Data_Quality_Facets_v2.md`; zero OpenRefine/CLI deps in the core.
- **Phase 2 — Wire the full binding.** Expose the whole check suite + report through the chosen binding (a couple of GREL functions and/or one "report this column" menu command). *Exit:* a real user gets a column quality report with one click, drop-in installed.
- **Phase 3 — UX & docs.** Menu structure, a readable results dialog/summary, and the dead-simple install README (per-OS paths, screenshots, maybe a 60-second screen recording). *Exit:* a non-technical colleague installs and runs it unaided from the README.
- **Phase 4 — Release hygiene.** Versioning scheme, `make zip`, first GitHub Release (extension zip + optional bare core jar). *Exit:* a tagged v0.1 download that installs cleanly on a fresh machine.
- **Phase 5 — CLI (future).** `loupe.cli` over `loupe.report`; JVM first, GraalVM single-binary optional (Spike 4). *Exit:* `loupe file.csv` prints a per-column report from the same core.

---

## 9. Decision log

**Settled (don't relitigate):**
- Clojure-native core + binding; JS only for the browser UI.
- Local Makefile build; no CI.
- Distribution via GitHub Releases (pre-built zip; optional bare core jar); no Maven Central for now.
- Self-contained extension; install = drop folder into workspace `extensions/` + restart; no admin/env/PATH.
- Single repo; agnostic-core discipline so the CLI shares the exact code.
- Scope = textual/character-level QA for multilingual manuscript metadata.

**Open (resolve in Claude Code, several pending spikes):**
- Binding mechanism: A vs B (vs C / Java-shim fallback) — pending Spike 1.
- Bundle Clojure runtime vs rely on OpenRefine's — pending Spike 2.
- Finding representation in `loupe.checks`: plain string vs structured map (affects report aggregation *and* how findings render in the UI). Recommend structured (e.g. `{:check :id :severity :critical :detail "…"}`) with a string-formatting layer, so the report and the UI can both consume it.
- Build tooling under the Makefile: `tools.build`/`build.clj` vs Leiningen vs raw `clojure` + `jar`.
- Menu surface: a few targeted GREL functions, one big "report" command, or both.
- Versioning + how the extension declares OpenRefine compatibility.

---

## 10. Risks & mitigations

- **Clojure-native binding harder than hoped.** → Spike first (Phase 0). Fallback ladder: B → A → Java shim → pure-JS expression injection (C).
- **OpenRefine API churn breaks the front end.** → Keep the binding/UI thin; logic in the agnostic core; pin and document a tested OpenRefine version range in the README.
- **Clojure version clash on the classpath.** → Spike 2; prefer compiling against OpenRefine's bundled Clojure version.
- **Install friction for non-technical users.** → The README + pre-built zip is a first-class deliverable; per-OS paths spelled out; consider a short screen recording. This is the single biggest adoption risk for the target audience.
- **No CI means manual release discipline.** → `make zip` makes the build one command; document the manual GitHub-Release step so it's repeatable.

---

## 11. Non-goals (for now)

- Maven Central publishing or any package-registry distribution.
- CI/CD of any kind.
- Numeric/statistical data profiling (distributions, outliers, type inference) — this is *textual* fidelity QA.
- Reconciliation features.
- Supporting install methods other than the drop-in extension folder.
- Rewriting the logic in Java.

---

## 12. References (fetchable by Claude Code)

- OpenRefine — writing extensions guide: https://openrefine.org/docs/technical-reference/writing-extensions
- OpenRefine — expressions (GREL/Jython/Clojure): https://openrefine.org/docs/manual/expressions
- OpenRefine — Jython & Clojure specifics: https://openrefine.org/docs/manual/jythonclojure
- OpenRefine/sample-extension (Butterfly skeleton, "Use this template"): https://github.com/OpenRefine/sample-extension
- RBGKew/String-Transformers (agnostic-core pattern; Java lib on Maven Central): https://github.com/RBGKew/String-Transformers
- The executable spec / parity target: `OpenRefine_Data_Quality_Facets_v2.md` (the existing Clojure facet library).

---

## 13. Suggested first prompts for Claude Code

1. *"Read this brief. Before any porting, run Spike 1: scaffold the smallest possible OpenRefine extension that bundles a one-function Clojure jar in `MOD-INF/lib` and a `controller.js` that adds a column-menu command calling it. Implement Binding B (injected Clojure expression). Tell me whether OpenRefine's bundled Clojure can `require` our namespace at expression-eval time; if it can't, switch to Binding A (`gen-class` implementing the GREL `Function` interface) and report which worked."*
2. *"Draft `deps.edn` and a `Makefile` with the targets in §7. Pick the simplest build tooling that AOT-compiles a Clojure namespace to a jar with no CI — justify tools.build vs Leiningen vs raw `clojure`+`jar` for this case."*
3. *"Propose the finding data shape for `loupe.checks` (string vs structured map) and sketch `loupe.report`'s per-column rollup, given that both the OpenRefine UI and a future CLI must consume it."*
4. *"Resolve Spike 2: determine the bundled Clojure version in the target OpenRefine release and recommend bundle-our-own vs compile-against-bundled, with the classloading trade-offs."*
5. *"Once the binding is proven, port the v2 facet library into `loupe.checks` with unit tests, keeping behavioural parity with `OpenRefine_Data_Quality_Facets_v2.md`."*

---

*Working name:* **loupe** (alt: `text-loupe` for an unambiguous repo slug; `Rubric` held in reserve if the grading angle becomes the headline).
