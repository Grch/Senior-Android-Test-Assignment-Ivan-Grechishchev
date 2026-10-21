## Purpose

Governs an optional, agreement-gated refinement to how quote prices are formatted based on a security's `min_step`, without committing to any rounding rule until the source, meaning, and user-agreed semantics are established.

## ADDED Requirements

### Requirement: Min-step source and meaning are established before any formatting change

Before any price or delta formatting is changed, the system's implementers SHALL determine where `min_step` is obtained (for example, from a live quote update, from the top-securities response, or from a separate lookup) and SHALL document its meaning based on authoritative sources. When actual source data or a representative real payload is obtainable, discovery SHALL inspect that data rather than relying only on documentation or illustrative examples. The findings, or any limitation preventing them, SHALL be recorded in the change artifacts.

#### Scenario: Source of min_step is identified

- **WHEN** this change is investigated
- **THEN** the artifact records the concrete source from which each security's `min_step` is read
- **AND** the artifact records the documented meaning of that value

#### Scenario: Actual sample data is inspected when available

- **WHEN** actual `min_step` source data or a representative real payload (such as a captured live/socket update or a real top-securities response) can be obtained
- **THEN** discovery inspects that real data rather than documentation or illustrative examples alone
- **AND** the artifact records where the inspected data came from

#### Scenario: Sample data is unavailable

- **WHEN** actual `min_step` sample data cannot be obtained
- **THEN** the limitation is recorded in the change artifacts
- **AND** the limitation is reported to the user
- **AND** no `min_step` semantics are inferred and no rounding or formatting rule is chosen

#### Scenario: No formatting change before findings

- **WHEN** the min_step source or meaning is not yet established
- **THEN** no price or delta formatting behavior is changed

### Requirement: No rounding or formatting rule is assumed

This change SHALL NOT encode any assumption about how `min_step` translates to displayed prices. In particular, it MUST NOT assume decimal-place formatting, rounding to the nearest tick, step-grid alignment, truncation, or any other rule. The rule remains undefined until explicitly agreed.

#### Scenario: Artifacts contain no chosen rule

- **WHEN** the proposal, spec, design, and tasks are reviewed before agreement
- **THEN** none of them specifies a concrete rounding or formatting algorithm for `min_step`

#### Scenario: Undecided rule is not implemented

- **WHEN** implementation is attempted before a rule is agreed
- **THEN** it MUST NOT proceed, because no rule is defined

### Requirement: An agreed rule is recorded before implementation

Once the source and meaning are established, the formatting options SHALL be discussed with the user. Any agreed rule SHALL be recorded in these artifacts, and implementation SHALL begin only after that agreement and artifact update.

#### Scenario: Agreement precedes implementation

- **WHEN** the user agrees on a formatting/rounding rule
- **THEN** the artifacts are updated to record that rule
- **AND** implementation proceeds using the recorded rule

#### Scenario: Formatted prices match the agreed rule

- **WHEN** the agreed rule is recorded and implemented
- **THEN** displayed prices and deltas follow that rule for securities with a known `min_step`
- **AND** securities without a usable `min_step` fall back to the board's default price display

### Requirement: Investigation findings are recorded with their source

The investigation SHALL record where `min_step` is delivered from and SHALL base that record on an inspected live payload rather than documentation alone.

`min_step` is delivered only by the WebSocket `q` quote event (field `min_step`, documented alongside the existing `c`/`ltr`/`name`/`ltp`/`chg`/`pcp`/`pp` fields at the "Subscribe to stock quotes updates" doc page). It is absent from the `quotes-get-top-securities` REST response. A companion field, `step_price`, is also documented ("Price increment") and was observed numerically identical to `min_step` in every captured sample; this change uses `min_step` only.

A live capture (browser WebSocket, same technique as the documented example) across all 33 fixed-list tickers confirmed: `min_step` arrives on a ticker's first (`init`) frame and is omitted from later partial-update frames, consistent with the project's existing sparse-update merge pattern. Every observed `ltp`/`chg` value was an exact multiple of that ticker's `min_step` (e.g. `RSTI` `min_step=0.0001`, `ltp=0.5533` = 5533 × 0.0001). Observed `min_step` values ranged from `0.000002` (`TGKA`) to `0.01` (most US/EU tickers), with `0.005` (`RUAL`, `VTBR`), `0.0005` (`MRKS`), `0.00005` (`MRKZ`), `0.00002` (`FEES`), and `0.0001` (`HYDR`, `RSTI`, `SGGD.EU`) also observed.

#### Scenario: Live payload confirms the source and its values

- **WHEN** the investigation inspects a live WebSocket `q` capture across the fixed ticker list
- **THEN** `min_step` is present on each ticker's first frame
- **AND** every observed `lastPrice`/`change` value is an exact multiple of that ticker's `min_step`

### Requirement: Agreed rule

Displayed `lastPrice` and `change` (the points delta) SHALL be rounded to the nearest multiple of the security's `min_step`, then formatted with exactly as many decimal places as `min_step` itself has (trailing zeros padded, not stripped) — the decimal-place count is `min_step`'s own scale after stripping its trailing zeros (e.g. `min_step=0.0001` → always 4 decimal places; `min_step=0.000002` → always 6). `percentChange` is unaffected (it is not a price-scale value). A row with no known `min_step` yet (not received, or the fixed-list default row before any quote arrives) keeps the board's existing default display (`BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()`).

This rule was chosen over leaving the current trailing-zero-stripped display unrounded because it matches the assignment's own reference screenshot: of the 8 reference rows whose ticker could be matched against a live-captured `min_step`, 7 matched exactly against "decimal places = min_step's scale" (`TGKA` 6, `FEES` 5, `MRKZ` 5, `HYDR` 4, `MRKS` 4, `RSTI` 4, `GAZP`/`SBER` 2); `VTBR` was the one mismatch, plausibly because the exchange has adjusted its tick size since that reference was captured.

#### Scenario: Price rounded and padded to min_step's decimal places

- **WHEN** a row has a known `min_step` and a `lastPrice`
- **THEN** the displayed price is rounded to the nearest multiple of `min_step`
- **AND** it is shown with exactly as many decimal places as `min_step` has, including trailing zeros

#### Scenario: Delta follows the same rule as price

- **WHEN** a row has a known `min_step` and a `change` value
- **THEN** the displayed delta (in parentheses) follows the same rounding and decimal-place rule as the price

#### Scenario: Percent change is unaffected

- **WHEN** a row's `percentChange` is displayed
- **THEN** its formatting does not depend on `min_step`
