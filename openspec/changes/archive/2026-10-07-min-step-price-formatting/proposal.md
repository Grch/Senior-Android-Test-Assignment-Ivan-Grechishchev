## Why

Different securities may trade with different minimum price steps (`min_step`), so displaying prices with a single fixed format can be misleading. As an optional refinement, the board should eventually format prices according to each security's `min_step`. The user does not yet know the intended rounding semantics, so this change deliberately investigates before committing to a rule.

## What Changes

- Optionally refine how the board formats last-trade price (and its delta) using the security's `min_step`.
- Do NOT commit to any formatting or rounding rule in this change. Specifically, this change MUST NOT assume decimal-place formatting, rounding to the nearest tick, alignment to a step grid, or any other rule.
- The investigation MUST first determine where `min_step` comes from (socket update, top-securities response, or a separate lookup) and its documented meaning, then discuss the options with the user, and only then update these artifacts to record the agreed rule.
- The investigation MUST inspect actual `min_step` source data or a representative real payload (for example a captured live/socket update or a real top-securities response) when such data can be obtained, rather than relying only on documentation or illustrative examples. If actual sample data cannot be obtained, the investigation MUST record that limitation and report it to the user, and MUST NOT infer `min_step` semantics or choose a rounding/formatting rule from examples or assumptions.
- Implementation MUST NOT begin until the rule is agreed and recorded in these artifacts.
- Android only. No iOS. No tests or test coverage are planned.

## Capabilities

### New Capabilities
- `min-step-price-formatting`: Optional, agreement-gated formatting of quote prices informed by a security's `min_step`, with the rule intentionally undecided.

### Modified Capabilities
<!-- None. -->

## Impact

- `:feature:quotes:impl`: possible changes to price/delta formatting once a rule is agreed; no formatting rule is added by this change as written.
- May depend on the `min_step` field from `live-quotes-board` and/or `top-securities-source`; the source of truth for `min_step` is part of the investigation.
- Depends on `live-quotes-board`; OpenSpec does not enforce cross-change sequencing, so it MUST be applied first.
- Blocks implementation on a user agreement; this change is not implementation-ready on its own.
