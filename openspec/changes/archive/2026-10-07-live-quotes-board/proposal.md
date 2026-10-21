## Why

The core of the assignment is a live quotes board: a watchlist of fixed tickers that updates in real time from Tradernet without authentication. This change delivers that board and the socket lifecycle behind it, building on the module foundation from `android-modular-foundation`.

## What Changes

- Add a no-auth live quotes board to `:feature:quotes:impl`, rendered with Jetpack Compose and driven by an MVVM ViewModel that owns screen state and stream orchestration.
- Connect to the Tradernet WebSocket host `wss.tradernet.com` using OkHttp's WebSocket client, subscribe using the documented `quotes` event, and map incoming updates delivered under the `q` carrier. The sources are distinct and are recorded separately in design.md: the assignment PDF specifies the intended single-frame subscription value; the official Tradernet "quotes changes" documentation provides the `quotes`/`q` event and the named field meanings; and a live raw probe confirms the actual outbound frame and the runtime `q` object envelope. The wire format is not invented here.
- Seed the board with the fixed, ordered ticker list (see spec.md), and render one compact row per ticker in that exact order.
- Each row shows: ticker; percent change versus previous close; last-trade exchange; security name; last-trade price; and the absolute price delta versus previous close.
- Color the sign of the percent/delta green when positive and red when negative, and flash the row green/red on a directional update. A short flash duration is chosen as a documented assumption.
- Coordinate socket lifecycle from the ViewModel: stop the socket when the app is backgrounded OR the quotes screen is hidden; reconnect and resubscribe when the app is foregrounded AND the screen is visible. No foreground service is used, and no socket is held by a composable.
- Use Retrofit for HTTP/REST needs (top-securities is a separate change) and the latest stable OkHttp WebSocket for the live socket; compatible stable versions are resolved at implementation time.
- Android only. No iOS. No tests or test coverage are planned.

## Capabilities

### New Capabilities
- `live-quotes-board`: Real-time, no-auth watchlist rendering of fixed tickers over the Tradernet WebSocket, including row content, sign coloring, update flashing, and foreground/visibility-aware socket lifecycle.

### Modified Capabilities
<!-- None. -->

## Impact

- `:feature:quotes:impl`: new ViewModel, state/action model, socket data source, DTOs/mappers, and Compose screen/rows.
- `:feature:quotes:api`: may expose the quotes screen entry point/models used by `:app`.
- `gradle/libs.versions.toml` and `:feature:quotes:impl`: add Retrofit and OkHttp WebSocket dependencies (stable, compatible versions pinned at implementation time).
- Depends on `android-modular-foundation`; OpenSpec does not enforce cross-change sequencing, so the foundation MUST be applied first.
- The optional `top-securities-source`, `security-logos`, and `min-step-price-formatting` changes extend this board but are not part of it.
