# Schemathesis generation and configuration model

Research for [#59](https://github.com/alkolhar/servdesk/issues/59) (child of the
[#57](https://github.com/alkolhar/servdesk/issues/57) map). Captured 2026-07-30.

**Scope of trust.** Every claim below is sourced to Schemathesis's own documentation, its source at
tag `v4.24.3`, the `schemathesis/action` repository, or the GitHub API's own view of that repo's
tags/releases. No blog posts, no Stack Overflow. Where a question could *not* be settled from a
primary source it is called out explicitly under "Not established".

**What the repo runs today** (`.github/workflows/ci.yml`, `contract-tests` job): `schemathesis/action@v3`
with `max-examples: 10`, `checks: not_a_server_error,status_code_conformance,content_type_conformance,response_schema_conformance`,
an `authorization` input, and no `args`, no `config-file`, no `version` pin.

**What the action actually executes.** `schemathesis/action@v3` is a composite action; its final
command is literally:

```
schemathesis run "$SCHEMA" --generation-database=":memory:" --max-examples="$MAX_EXAMPLES" --checks="$CHECKS" [-H "Authorization: $AUTHORIZATION"] "${ARGS_ARRAY[@]}"
```

with `SCHEMATHESIS_BASE_URL` and `SCHEMATHESIS_WAIT_FOR_SCHEMA` exported as env vars.
Source: [`action.yml` @v3](https://github.com/schemathesis/action/blob/v3/action.yml).
Everything the CLI supports is therefore reachable from this job via the `args` or `config-file`
inputs — nothing about the action constrains the configuration surface.

---

## 1. Seed stability — **each run is an independent sample**

**Answer: no seed is fixed by default. Every process invents a fresh 128-bit random seed, so
re-running the same commit generates different data. N re-runs of one commit IS a meaningful
flakiness sample.**

- `--seed INTEGER` exists ("Random seed for reproducible test runs. Setting the same seed value will
  result in the same sequence of generated test cases") and is documented with **no default**.
  Source: [CLI reference → `--seed`](https://schemathesis.readthedocs.io/en/stable/reference/cli/#-seed-integer).
  Same option as `seed` in the config file, **Default: `null`**.
  Source: [Configuration reference → `seed`](https://schemathesis.readthedocs.io/en/stable/reference/configuration/#seed).
- What `null` means in practice, from the source — the seed is lazily *generated* per process, not
  left unset:

  ```python
  @property
  def seed(self) -> int:
      if self._seed is None:
          self._seed = Random().getrandbits(128)
      return self._seed
  ```

  Source: [`src/schemathesis/config/__init__.py#L137-L141` @v4.24.3](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/config/__init__.py#L137-L141).
  `Random()` with no argument seeds from OS entropy, so the value differs per invocation.
- That seed is then handed to Hypothesis per operation:
  `if config.seed is not None ...: hypothesis_test = hypothesis.seed(config.seed)(hypothesis_test)`.
  Source: [`generation/hypothesis/builder.py#L99-L100` @v4.24.3](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/generation/hypothesis/builder.py#L99-L100).
- The action passes `--generation-database=":memory:"` unconditionally
  ([`action.yml` @v3](https://github.com/schemathesis/action/blob/v3/action.yml)), whose documented
  meaning is "temporary storage" (default is the persistent `.hypothesis/examples`).
  Source: [CLI reference → `--generation-database`](https://schemathesis.readthedocs.io/en/stable/reference/cli/#-generation-database-text).
  So there is also no cross-run example reuse to smuggle state between CI runs — random seed *and*
  empty example database each time.
- **The seed used is printed in the run output**, so a red run's seed can be read from the CI log and
  replayed with `--seed`. Source: [`cli/output.py#L66-L73` @v4.24.3](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/cli/output.py#L66-L73)
  (`display_seed`), called from the run handler's summary.

**Consequences for the arrival test.**

- Re-running the same commit N times is a *legitimate* flakiness sample — the map's "streak must span
  distinct commits" fallback is not required.
- Not everything varies. Of the four phases, **examples** and **coverage** are schema-derived
  (examples phase replays `example`/`examples` from the schema; coverage phase "aims to exhaustively
  cover boundary values for every constraint"), while **fuzzing** is the random one. Source:
  [Data Generation → Testing Phases](https://schemathesis.readthedocs.io/en/stable/explanations/data-generation/#testing-phases).
  So a streak proves stability of the *random* half; the deterministic half is already stable by
  construction.
- If the gate ever needs to be pinned to one dataset, `args: '--seed 12345'` is the supported way.
  **Do not reach for `--generation-deterministic` instead**: it is documented as implying no
  database, and combining it with `--generation-database` is rejected as a usage error
  (`"--generation-deterministic implies no database, so passing --generation-database too is invalid."`) —
  and the action always passes `--generation-database`. Sources:
  [`cli/validation.py#L22-L24` and `#L95-L102` @v4.24.3](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/cli/validation.py#L22-L24).

---

## 2. Health checks — two different mechanisms, and the repo is looking at the *non-fatal* one

The two messages named in the ticket, "Failed Health Check: Authentication failed" and "Schema
validation mismatch", come from **two different subsystems** that behave oppositely with respect to
exit code. Getting this distinction right is the whole answer.

### 2a. Schemathesis **warnings** — never fatal by default

"Authentication failed" and "Schema validation mismatch" are the display titles of the warnings
`missing_auth` and `validation_mismatch`, printed under a `WARNINGS` section.
Source: [`cli/commands/run/handlers/output.py` `display_warnings` @v4.24.3](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/cli/commands/run/handlers/output.py#L937-L965)
(`title="Authentication failed"`, `title="Schema validation mismatch"`).

The full set, with triggers (source: [Warnings reference](https://schemathesis.readthedocs.io/en/stable/reference/warnings/)):

| Warning | Documented trigger |
| --- | --- |
| `missing_auth` | ≥ 90% of requests returned 401 or 403 |
| `missing_test_data` | ≥ 10% of requests returned 404 |
| `validation_mismatch` | ≥ 10% of requests returned 4xx excluding 401/403/404 |
| `missing_deserializer` | structured response schema, no deserializer for that content type |
| `unused_openapi_auth` | configured `[auth.openapi.<scheme>]` not in `securitySchemes` |
| `method_not_allowed` | operation only ever returned 405, and 405 is not documented |
| `constants_extraction` | a registered `@schemathesis.python.constants` source could not be scanned |

Key properties, all primary-sourced:

- **They do not stop execution.** "Warnings appear in your CLI output and don't stop test execution."
  Source: [Warnings reference](https://schemathesis.readthedocs.io/en/stable/reference/warnings/).
- **They do not touch the exit code unless you ask.** `WarningsConfig.__init__` defaults
  `fail_on` to `[]`, and the only place a warning sets `ctx.exit_code = 1` is guarded by
  `should_fail(...)`. Sources:
  [`config/_warnings.py#L23-L31`](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/config/_warnings.py#L23-L31),
  [`cli/commands/run/warnings.py` `_handle_warning`](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/cli/commands/run/warnings.py#L288-L298).
- **"Visible but non-blocking" is the default state**, and it is also explicitly configurable via the
  object form, which separates the two axes:

  ```toml
  [warnings]
  display = ["missing_auth", "missing_test_data", "validation_mismatch"]
  fail-on = []   # or omit entirely
  ```

  Source: [Warnings reference → Advanced Configuration](https://schemathesis.readthedocs.io/en/stable/reference/warnings/#advanced-configuration).
- To hide one instead: `--warnings=missing_auth,...` (allow-list) or `--warnings=off`; config
  `warnings = false` / `warnings = ["..."]`. Source:
  [CLI reference → `--warnings`](https://schemathesis.readthedocs.io/en/stable/reference/cli/#-warnings-warnings).

> **So: nothing in the current job configuration lets `missing_auth` or `validation_mismatch` fail
> `contract-tests`.** If those warnings are showing up in a red run, the redness is coming from
> somewhere else (a failed check, or an ERROR) and the warnings are noise printed alongside it. The
> #55/Q6 requirement "health checks must not be able to fail this job, but must stay visible" is
> already satisfied by the defaults for this class of message; there is nothing to configure, only
> something to *not* configure (never set `warnings.fail-on`).

### 2b. Hypothesis **health checks** — genuinely fatal, all-or-nothing suppression

These are the real "Failed Health Check" errors. Schemathesis titles an error `"Failed Health Check"`
for exactly four Hypothesis health-check kinds — `data_too_large`, `filter_too_much`, `too_slow`,
`large_base_example`. Source: [`engine/errors.py` `title` @v4.24.3](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/engine/errors.py#L79-L98)
and the classification map at [`engine/errors.py#L345-L354`](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/engine/errors.py#L345-L354).

- Documented as fatal-ish: "Health checks identify potential problems with test generation or
  performance and may stop tests early with an error."
  Source: [CLI reference → `--suppress-health-check`](https://schemathesis.readthedocs.io/en/stable/reference/cli/#-suppress-health-check-checks).
- They set the exit code: a `NonFatalError` event or a phase finishing with status `ERROR` sets
  `self.exit_code = 1`. Source: [`cli/commands/run/context.py#L14-L28`](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/cli/commands/run/context.py#L14-L28).
  Exit code `1` = "At least one check failed or a bug was reported".
  Source: [CLI reference → Exit codes](https://schemathesis.readthedocs.io/en/stable/reference/cli/#exit-codes).
- **Suppression is the only lever**: `--suppress-health-check data_too_large,filter_too_much,too_slow,large_base_example`
  (or `all`), default `[]`; config equivalent `suppress-health-check = [...]`. Sources:
  [CLI reference](https://schemathesis.readthedocs.io/en/stable/reference/cli/#-suppress-health-check-checks),
  [Configuration reference → `suppress-health-check`](https://schemathesis.readthedocs.io/en/stable/reference/configuration/#suppress-health-check).
- Schemathesis's own tuning guide endorses suppressing them for long gate runs, with a caveat:
  "For long release-gate runs, suppressing `filter_too_much` and `too_slow` is reasonable… Firing
  health checks is worth investigating locally — they may indicate a schema correctness issue."
  Source: [Optimizing Schemathesis](https://schemathesis.readthedocs.io/en/stable/guides/config-optimization/#-suppress-health-check-filter_too_muchtoo_slow).

**Not established:** there is **no** documented way to keep a *Hypothesis* health check visible while
making it non-fatal. Suppression removes the check (and therefore the message) entirely; the only
alternative is letting it error. Nothing in the CLI reference, the configuration reference, or the
source offers a warn-only mode for these four. If the gate needs "visible but non-blocking" for a
Hypothesis health check specifically, that has to be built outside Schemathesis (e.g. grep the log).

---

## 3. `max-examples` — the "mostly rejected" judgement really is noisier at 10

**Answer: yes, `max-examples: 10` genuinely makes `validation_mismatch` (and `missing_auth`) flakier,
and the mechanism is sharper than a ratio-variance argument.**

What `max-examples` controls (source: [CLI reference → `-n, --max-examples`](https://schemathesis.readthedocs.io/en/stable/reference/cli/#-n-max-examples-count),
documented default `100`; the action's own default is also `100`):

- **fuzzing** phase: max examples generated per API operation
- **stateful** phase: max distinct API call sequences
- **examples** and **coverage** phases: *no effect* — those use predetermined test cases

### Why 10 is noisy for `validation_mismatch`

The real trigger is stricter than the doc's "≥10% of 4xx" line. From
[`cli/commands/run/warnings.py` @v4.24.3](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/cli/commands/run/warnings.py),
a scenario raises `validation_mismatch` only when **all** of these hold:

1. the scenario finished `SUCCESS` and positive generation mode is enabled;
2. `all_positive_are_rejected(recorder)` — **not one** positive-mode case in that scenario got a 2xx
   ([`#L250-L263`](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/cli/commands/run/warnings.py#L250-L263));
3. `_can_warn_about_4xx()` — every observed status is 4xx (500 tolerated), and the status set is not
   a subset of `{401, 403, 500}` ([`#L106-L114`](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/cli/commands/run/warnings.py#L106-L114));
4. `count_other_4xx / total_4xx >= 0.1`, where `OTHER_CLIENT_ERRORS_THRESHOLD = 0.1`
   ([`#L127-L141`](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/cli/commands/run/warnings.py#L127-L141)).

Condition 2 is the sample-size lever, and it is an **all-or-nothing** condition, not a ratio. If an
operation accepts, say, 20% of generated positive payloads, then at 10 examples the probability that
*zero* of them succeeds is roughly `0.8^10 ≈ 11%` — the warning appears in about one run in nine and
vanishes in the others. At 100 examples it is `0.8^100 ≈ 2×10⁻¹⁰` — effectively never. Combined with
the per-run random seed from Q1, that is exactly the "appears on some runs, not others" pattern #55
observed. (The arithmetic is illustrative; the *structure* — a zero-success gate whose probability
decays geometrically in `max-examples` — is what the source establishes.)

`missing_auth` (`AUTH_ERRORS_THRESHOLD = 0.9`, same file) is a pure ratio with **no minimum-sample
guard**, so at n=10 a single non-401 response moves the ratio by 0.1 — one whole threshold's width.
At n=100 it moves it by 0.01. Same conclusion, weaker mechanism.

### Cost of raising it

- Runtime scales with `max-examples` only in the **fuzzing** and **stateful** phases; examples and
  coverage are unaffected (same CLI-reference entry). So 10 → 100 is *not* a 10× job, because part of
  the run is fixed cost.
- The upstream guidance is explicitly to prefer fewer, longer runs: "When running multiple
  iterations, prefer higher `--max-examples` with fewer iterations rather than low examples with many
  iterations. For example, 2 runs of 500 examples each are more effective than 10 runs of 100
  examples because Hypothesis can better explore the input space in longer runs."
  Source: [Optimizing Schemathesis](https://schemathesis.readthedocs.io/en/stable/guides/config-optimization/).
  That cuts against the map's instinct to buy confidence with more re-runs at n=10.
- Actual counts are often below the cap: "Up to `--max-examples` per operation (default: 100), but
  often fewer" — enums and small constraint spaces exhaust early.
  Source: [Data Generation → How Many Test Cases](https://schemathesis.readthedocs.io/en/stable/explanations/data-generation/#how-many-test-cases-does-schemathesis-generate).
- If wall-clock becomes the constraint, `-w/--workers` (default `1`, range 1–64 or `auto`) parallelises
  the unit phases (examples, coverage, fuzzing). Source:
  [CLI reference → `-w, --workers`](https://schemathesis.readthedocs.io/en/stable/reference/cli/#-w-workers-value).
  `--no-shrink` also trades debuggability for speed on failing runs
  ([CLI reference](https://schemathesis.readthedocs.io/en/stable/reference/cli/#-no-shrink)).

**Not established:** no primary source gives wall-clock numbers for this API at any `max-examples`
value. The 15-minute `timeout-minutes` on the job is the real budget, and the only way to size the
raise is to measure a CI run. Recommend stepping 10 → 50 → 100 and reading the job duration.

---

## 4. Operation exclusion — two supported routes, both reachable from the action

### Route A: CLI filter flags via the action's `args` input

`--include-TYPE VALUE` / `--exclude-TYPE VALUE` and their `-regex` variants, where TYPE ∈
`{path, method, name, tag, operation-id}`; plus `--include-by`/`--exclude-by` (JSON-Pointer
expressions) and `--exclude-deprecated`. Sources:
[CLI reference → Filtering](https://schemathesis.readthedocs.io/en/stable/reference/cli/#filtering),
and the option generator `_BY_VALUES = ("operation-id", "tag", "name", "method", "path")` in
[`cli/commands/run/filters.py` @v4.24.3](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/cli/commands/run/filters.py).

For the ticket's concrete example, the `name` filter takes a full operation label whose format is
`f"{method.upper()} {path}"` (source: [`schemas.py#L755`](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/schemas.py#L755)):

```yaml
- uses: schemathesis/action@v3
  with:
    args: '--exclude-name "DELETE /api/persons/{id}"'
```

The action `eval`s `ARGS` into a bash array (`eval "ARGS_ARRAY=($ARGS)"`, see `action.yml` @v3), so
quoted arguments containing spaces and braces survive intact.

**Caveat worth knowing:** these filter options are registered with `hidden=True` in Click
([`filters.py`](https://github.com/schemathesis/schemathesis/blob/v4.24.3/src/schemathesis/cli/commands/run/filters.py)),
so they do **not** appear in `schemathesis run --help`. They are supported and documented — just
invisible from the CLI's own help output. Don't conclude from `--help` that they were removed.

### Route B: `schemathesis.toml` via the action's `config-file` input

```toml
[[operations]]
include-name = "DELETE /api/persons/{id}"
enabled = false
```

"The config above will disable all operations matching the set of filters." Sources:
[Configuration reference → Operation-Specific Configuration](https://schemathesis.readthedocs.io/en/stable/reference/configuration/#operation-specific-configuration);
`config-file` input exists on the action since v2.1.0
([release notes](https://github.com/schemathesis/action/releases/tag/v2.1.0), [`action.yml` @v3](https://github.com/schemathesis/action/blob/v3/action.yml)).

**Recommendation for this repo.** Route B scales better than Route A here. The same file can carry
the exclusion *and* `[warnings]`, `suppress-health-check`, `seed`, per-operation `parameters` (which
is the documented cure for `missing_test_data`: "Provide realistic parameter values in your config
file so tests can access existing resources"), and it is reviewable in-repo rather than buried in a
YAML string. Config precedence is CLI > operation-phase > global-phase > `[[operations]]` > `[[project]]` >
global ([Configuration Resolution](https://schemathesis.readthedocs.io/en/stable/reference/configuration/#configuration-resolution)),
so the action's hard-coded `--max-examples`/`--checks`/`--generation-database` still win over the
file — plan around that rather than trying to override them from TOML.

---

## 5. Versioning

**Does `schemathesis/action@v4` exist? No.** The repository has no `v4` tag and no `v4.x` release.
Latest release is **`v3.0.0`, published 2026-03-11**. Full release list: v3.0.0, v2.1.0, v2.0.1,
v2.0.0, v1.1.1, v1.1.0, v1.0.4 … Source: GitHub API
`repos/schemathesis/action/releases` and `repos/schemathesis/action/tags`
(browsable at <https://github.com/schemathesis/action/releases> and <https://github.com/schemathesis/action/tags>).

**What `v3` resolves to today:** the moving tag `v3` points at the *same* commit as `v3.0.0`:

```
v3.0.0  806cace2053cbbac93188e1281ff7da415643160
v3      806cace2053cbbac93188e1281ff7da415643160
```

Source: GitHub API `repos/schemathesis/action/tags` (as of 2026-07-30). So `@v3` is currently
equivalent to `@806cace2053cbbac93188e1281ff7da415643160` — that is the SHA to pin to.

**Latest Schemathesis CLI: `4.24.3`, published 2026-07-25.** Sources:
[GitHub releases](https://github.com/schemathesis/schemathesis/releases),
[PyPI JSON API](https://pypi.org/pypi/schemathesis/json).

### The pinning ticket needs *two* pins, not one

Pinning the action alone does **not** pin the tool. `action.yml` @v3 resolves `version: latest`
(the default) to a **floating range**:

```bash
if [[ "$VERSION" == "latest" ]]; then
  BASE_SPEC="schemathesis>=4.11.1,<5.0"
else
  BASE_SPEC="schemathesis==$VERSION"
fi
uv tool install "$BASE_SPEC" --with "tracecov>=0.16.6"   # when coverage is enabled
```

Source: [`action.yml` @v3](https://github.com/schemathesis/action/blob/v3/action.yml). Every CI run
therefore installs whatever 4.x is newest on PyPI at that moment — today 4.24.3, tomorrow something
else. `tracecov` floats too. A reproducible gate needs `uses: schemathesis/action@806cace…` **and**
`version: "4.24.3"`.

### Inputs differ between major versions

Confirmed by diffing `action.yml` at `v2` and `v3`:

- **v2 inputs**: `schema`, `base-url`, `checks`, `wait-for-schema`, `max-examples`, `version`,
  `hooks`, `config-file`, `args`.
- **v3 adds**: `authorization` (this repo uses it — so a downgrade to v2 would silently drop auth),
  plus the whole `coverage*` family: `coverage` (**default `true`**), `coverage-report`,
  `coverage-report-path`, `coverage-artifact-name`, `coverage-pr-comment`, `coverage-step-summary`.
- v3 also bumps `actions/setup-python@v5→v6` and `astral-sh/setup-uv@v6→v7`, and raises the floating
  CLI floor from `>=4.0,<5.0` to `>=4.11.1,<5.0`.

Sources: [`action.yml` @v2](https://github.com/schemathesis/action/blob/v2/action.yml),
[`action.yml` @v3](https://github.com/schemathesis/action/blob/v3/action.yml),
[v3.0.0 release notes](https://github.com/schemathesis/action/releases/tag/v3.0.0).

**Side finding relevant to the map.** Because `coverage` defaults to `true` in v3, this job is
already installing `tracecov`, uploading a `schema-coverage-report` artifact, and attempting to post
a PR comment on every pull request. The PR comment needs `pull-requests: write`, which
`.github/workflows/ci.yml` does not grant — the action swallows the failure (`|| true`), so it is
harmless, but the coverage artifact and step summary are free signal nobody is reading yet.
Source: [`action.yml` @v3](https://github.com/schemathesis/action/blob/v3/action.yml).

---

## Summary of what changed in the map's assumptions

| Map assumption | Finding |
| --- | --- |
| Streak may have to span distinct commits if the seed is fixed | **Not needed.** Seed is random per run; N re-runs of one commit is a valid sample. |
| "Health checks must not be able to fail this job" needs configuration | **Already true** for `missing_auth`/`validation_mismatch` — they are *warnings*, non-fatal unless `warnings.fail-on` is set. Only the four Hypothesis health checks are fatal, and those are a different message class. |
| `max-examples: 10` makes "mostly rejected" noisier | **Confirmed**, via an all-or-nothing "no positive case got 2xx" gate whose probability decays geometrically in `max-examples`. |
| Operation exclusion is available as a fallback | **Confirmed**, two ways; `config-file` is the better fit for this repo. |
| Pin `schemathesis/action` | **Insufficient alone** — also pin `version:`, or the CLI floats across all of 4.x. |

## Things this research could not settle

1. **Wall-clock cost of raising `max-examples`** for this specific API — no primary source can give
   it; requires a measured CI run.
2. **Whether a Hypothesis health check can be made visible-but-non-fatal** — no such option exists in
   any primary source. Suppression hides it completely.
3. **Whether the exact `--generation-deterministic` + `--generation-database` conflict fires** in the
   action's argument order (Click's `ctx.params` is populated in parameter-processing order). The
   combination is *documented* as invalid; treat it as unusable and use `--seed` instead.
4. **Which specific operations in this API currently trip `validation_mismatch`** — that is an
   observation about a real run, not a documentation fact.
