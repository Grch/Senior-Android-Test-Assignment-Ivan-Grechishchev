## Context

See proposal.md — Why, and the `live-quotes-board` spec for required behavior. This change depends on `android-modular-foundation` for the `:feature:quotes:api` / `:feature:quotes:impl` split and the Compose + MVVM contract. The board is unauthenticated and reads a fixed ticker list (defined in the spec) over `wss.tradernet.com`. The wire format of the Tradernet socket subscription and updates is now verified — from the official "quotes changes" documentation and a live socket probe — and is recorded in the "Verified wire format" section below, which keeps official-documentation facts separate from live-probe observations.

## Goals / Non-Goals

**Goals:**
- Render the fixed watchlist in the specified order with the required per-row fields.
- Keep a single ViewModel-owned stream that survives recomposition and is started/stopped by app foreground and screen visibility.
- Provide sign coloring and a complementary directional flash.
- Isolate the Tradernet wire format behind a data source so mapping changes stay local.

**Non-Goals:**
- Authentication, credentials, or user sessions.
- Replacing the fixed source with the top-securities API (separate change).
- Ticker logos (separate change) and min-step price formatting (separate change).
- A foreground service or background streaming while the app is not visible.
- Tests or test coverage.

## Decisions

- **Retrofit for HTTP/REST and OkHttp WebSocket for the live socket.** Retrofit is the assignment's REST choice; OkHttp's WebSocket is the natural companion since Retrofit already pulls in OkHttp. Use the latest stable OkHttp WebSocket and resolve a compatible stable version pair at implementation time. Alternative considered: a single third-party socket library — rejected because the assignment's socket contract is a plain WebSocket and OkHttp is already on the classpath.
- **ViewModel owns the stream and lifecycle.** The ViewModel exposes state and accepts actions, including lifecycle/visibility signals. A dedicated `QuoteSocketDataSource` (in `:feature:quotes:impl`) owns the raw socket and emits parsed updates; the ViewModel decides when to start/stop it. Composables only render and dispatch actions, so no socket lives in composition. Alternative considered: starting the socket in a composable with `DisposableEffect` — rejected because it couples streaming to UI lifecycle and risks duplicate sockets on recomposition.
- **Lifecycle = foreground AND visible.** Track app foreground/background and quotes-screen visibility as two booleans in the ViewModel; the socket runs only when both are true. Transitions to `false` stop the socket; transitions back to `true` reconnect and resubscribe. This satisfies the "OR" for stopping and the "AND" for running. Alternative considered: a foreground service — explicitly out of scope and disallowed.
- **Verify the wire format against documentation plus a live probe before mapping (done).** The first implementation task recorded the official `quotes`-changes reference and a live capture, reconciled in the "Verified wire format" section below. Official documentation supplies the subscription value and the named `q` fields; the live probe supplies the actual `q` envelope (object at index 1) and the partial-update behavior that the page's array-oriented helper does not. Alternative considered: mapping from an inferred/typical shape — rejected by the assignment.
- **Directional flash default of 300 ms, assumed.** A brief flash is required but no duration was specified. 300 ms is chosen as a reasonable default and is treated as an assumption; it can be tuned without changing the spec. Alternative considered: until the next update — rejected as potentially indefinite.

## Risks / Trade-offs

- [Verified wire format could drift from live behavior] → Recorded official-documentation facts and live-probe facts separately in the "Verified wire format" section below, and kept the parser isolated so only that file changes. (The earlier "unknown format blocks mapping" risk is retired because the format is now verified.)
- [Two lifecycle signals can get out of sync] → Model both booleans explicitly in the ViewModel and derive a single `shouldStream` value; stop on any transition to false.
- [Reconnect storms on rapid foreground/visibility flips] → Reconnect only on the rising edge to `shouldStream = true`; ignore redundant signals while already connected.
- [Flash on every update can be visually noisy] → Keep the flash short and only trigger it when the last-trade price direction actually changes.
- [Fixed list includes non-US/odd tickers] → Treat tickers as opaque strings and preserve the exact order; no parsing or validation of the suffix.

## Verified wire format (implementation evidence)

The facts below are separated by source: **official documentation** versus a **live socket probe**. Where the two disagree, the live probe is authoritative for the observed runtime and the discrepancy is recorded explicitly.

**Official documentation**

- Sources: `https://tradernet.com/tradernet-api/quotes-get-changes/?site_lang=en` and its dynamically loaded example `https://tradernet.com/tradernet-api/get-example?id=quotes-get-changes`.
- The assignment PDF specifies the intended subscription value: the `quotes` event carrying the fixed ticker list, `["quotes", [<ticker>, ...]]`.
- The `q` carrier's fields are **named, not positional**: `c` ticker; `ltr` exchange of the latest trade; `name` security name; `pp` previous close; `op` session open; `ltp` last-trade price; `chg` signed point change vs previous close; `pcp` percent change vs previous close; `name2` Latin security name.
- The page's browser helper shows `ws.send("quotes", JSON.stringify(['quotes', tickers]))`. That two-argument `ws.send` is **not a valid native WebSocket API call** and must not be copied; the valid send is one serialized text frame, e.g. `["quotes",["AFLT","AAPL.US"]]`.
- The page renders the `q` payload as an **object** (`{"q": {...}}`) while annotating the helper `QuoteInfoAnswer[]` and iterating with `data.forEach`, which implies an **array**. The documentation is internally inconsistent on this point; the live probe resolves it.

**Live socket probe**

- **Socket host:** `wss://wss.tradernet.com/` (no auth, no credentials).
- **Subscription confirmed at runtime:** the single text frame `["quotes",[<fixed tickers>]]` is accepted. This verifies the one-frame serialized form; the two-argument browser helper is invalid and is not used.
- **Observed `q` envelope:** a top-level JSON **array** `["q", { ...quote object... }, "wstm=..."]`. Index 0 is the event `"q"`, index 1 is a single quote **object**, and the optional trailing `wstm=...` element is a server timestamp that is ignored. The array-oriented `QuoteInfoAnswer[]` form does not occur, so only the object form is handled; the array form is intentionally unsupported.
- **Partial updates observed:** later frames omit `ltr`, `name`, `pp`, and `pcp` and carry only the changed fields, so the parser must tolerate missing fields and the stream consumer must merge by `c` ticker (see the spec's partial-update merge requirement).
- **Live capture sample (AAPL.US):** `["q",{"c":"AAPL.US","ltr":"FIX","name":"Apple Inc.","pp":334.1,"ltp":333.23,"chg":-0.87,"pcp":-0.26},"wstm=..." ]`.

## Open Questions

- ~~The exact subscription frame and the field layout under `q`.~~ Resolved above: official documentation plus a live capture were reconciled before mapping, and the answer does not change the approach, specs, or task breakdown.
- Final stable OkHttp/Retrofit version pair compatible with the project's Gradle/AGP versions. Resolved when dependencies are added.
