## 1. Investigation (no code changes)

- [x] 1.1 Search the Tradernet quotes-changes documentation and the top-securities documentation for any `min_step` field, recording whether and where it appears, with source URLs.
  - Found on `https://tradernet.com/tradernet-api/quotes-get-changes/?site_lang=en` ("Subscribe to stock quotes updates" doc section), field `min_step` = "Minimum price increment", alongside `step_price` = "Price increment". Not present on `https://tradernet.com/tradernet-api/quotes-get-top-securities`.
- [x] 1.2 If `min_step` appears in the data, obtain and inspect actual source data or a representative real payload (for example a captured live/socket update or a real top-securities response) and record where it is read from (socket update, REST response, or separate lookup). Do not substitute documentation or illustrative examples for real sample data.
  - Captured live via a browser WebSocket connection (same technique as the docs' own example) to `wss://wss.tradernet.com/`, subscribing to all 33 `live-quotes-board` fixed tickers. `min_step` arrived on each ticker's first frame; see spec.md for the full value table and the ltp-is-an-exact-multiple-of-min_step confirmation.
- [x] 1.3 N/A — real sample data was obtained (see 1.2); this fallback path was not needed.
- [x] 1.4 Record the documented meaning of `min_step` in the change artifacts, noting explicitly any ambiguity instead of resolving it by assumption.
  - Recorded in `specs/min-step-price-formatting/spec.md` ("Investigation findings"). No ambiguity found: `min_step` is unambiguously a per-security minimum price tick, and `step_price` tracked it exactly in every sample (not used further, to avoid depending on an unexplained duplicate field).
- [x] 1.5 Verify no price-formatting code was changed during investigation.
  - Confirmed: investigation was browser/`curl`-only, no app source files touched.

## 2. Discussion and artifact update (gate)

- [x] 2.1 Present the findings and possible formatting interpretations to the user and discuss the intended semantics without presupposing a rule.
  - Presented three options (fixed decimal places per `min_step`'s scale; round only, keep current stripped display; drop the change) via `AskUserQuestion`, with the fixed-decimal-places option recommended and backed by a 7/8 match against the assignment's reference screenshot.
- [x] 2.2 Once the user agrees on a rule, update `design.md` and `spec.md` to record that agreed rule, and verify the recorded rule matches the agreement.
  - User chose "Фикс. число знаков = разрядность min_step". Recorded in `specs/min-step-price-formatting/spec.md` ("Agreed rule") and `design.md` (Open Questions).
- [x] 2.3 Verify the artifacts still contain no unapproved rounding or formatting algorithm before proceeding.
  - Confirmed: the only rule recorded is the one just agreed.

## 3. Implementation (blocked until 2.2 is complete — now unblocked)

- [ ] 3.1 Implement the agreed rule in a single presentation-layer price/delta formatter and verify formatted output matches the recorded rule for securities with a known `min_step`.
- [ ] 3.2 Keep the board's existing default display for securities without a usable `min_step`, and verify those rows are unchanged.
- [ ] 3.3 Run `./gradlew assembleDebug` and confirm the app builds with the agreed formatting.
