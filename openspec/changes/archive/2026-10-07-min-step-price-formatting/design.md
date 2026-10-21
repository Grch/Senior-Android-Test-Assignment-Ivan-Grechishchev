## Context

See proposal.md — Why, and the `min-step-price-formatting` spec. This change depends on `live-quotes-board` for price display, and possibly on `top-securities-source` if that endpoint is the source of `min_step`. The user does not yet know the intended rounding semantics, so no formatting rule can be designed yet. This is an investigation-first, agreement-gated change.

## Goals / Non-Goals

**Goals:**
- Determine where `min_step` comes from and what it means, inspecting actual source data or a representative real payload when obtainable in addition to authoritative documentation.
- Surface the findings and formatting options to the user.
- Record the agreed rule in these artifacts before implementation.

**Non-Goals:**
- Choosing or encoding any formatting/rounding rule now.
- Changing price or delta display as part of this proposal.
- Logos, source selection, or socket behavior (separate changes).
- Tests or test coverage.

## Decisions

- **Investigate before design.** The first tasks locate `min_step` in the relevant Tradernet documentation/data (socket update, top-securities response, or a dedicated lookup) and record its documented meaning. Alternative considered: assume `min_step` is a decimal precision — rejected because the assignment explicitly forbids assuming the semantics.
- **Inspect actual source data, not just docs or examples.** Discovery obtains and inspects the real `min_step` payload from its actual source (a captured live/socket update or a real top-securities response) when obtainable; documentation and illustrative examples are supporting evidence, not a substitute for real data. Alternative considered: rely on documented examples alone — rejected because examples may not match the real payload or reveal its semantics.
- **Unobtainable sample data is a recorded limitation, not a license to infer.** If no actual sample data can be obtained, the investigation records that limitation and reports it to the user, and does not infer `min_step` semantics or choose a rounding/formatting rule. Alternative considered: fall back to documentation/examples and infer—rejected because the assignment forbids assuming the semantics.
- **Gate implementation on user agreement.** Design and tasks stop at the discussion and artifact-update step; no code task is written that applies an unapproved rule. Alternative considered: implement a configurable formatter with a guessed default — rejected as baking in an unstated assumption.
- **Keep any future rule isolated behind a single formatter.** Once agreed, the rule should live in one presentation-layer formatter so it can be revised without touching data or UI structure. Alternative considered: spreading formatting across rows — rejected for maintainability.
- **Default display for securities without `min_step`.** Securities lacking a usable `min_step` continue with the board's existing default display until the agreed rule says otherwise. Alternative considered: blank/placeholder price — rejected; existing display is already required by `live-quotes-board`.

## Risks / Trade-offs

- [`min_step` may not be present in the current data at all] → The investigation explicitly checks the socket and REST sources; if absent, the change is suspended or rescoped rather than guessed.
- [No real sample data can be obtained] → Record the limitation and report it to the user; do not substitute documentation or illustrative examples for real data, and do not infer semantics.
- [Agreement may take time and block delivery] → This is acceptable because the change is optional and must not encode an assumption.
- [A recorded rule could later change] → Isolating the formatter keeps the blast radius small.
- [Ambiguous meaning in docs] → Record the ambiguity and take it to the user rather than resolve it unilaterally.

## Open Questions

- Where does `min_step` come from, and what precisely does it mean? **Resolved.** It is a field on the WebSocket `q` quote event only (not in `quotes-get-top-securities`), documented as "Minimum price increment" alongside a `step_price` ("Price increment") field that was observed identical to it in every captured sample. Confirmed by a live browser WebSocket capture across all 33 fixed-list tickers (see `specs/min-step-price-formatting/spec.md` — "Investigation findings").
- Which formatting/rounding rule to adopt? **Resolved and agreed with the user**: round `lastPrice`/`change` to the nearest multiple of `min_step`, display with exactly `min_step`'s own decimal-place count (padded, not stripped). `percentChange` is unaffected. See `specs/min-step-price-formatting/spec.md` — "Agreed rule".
