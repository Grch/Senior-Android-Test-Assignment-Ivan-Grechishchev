## Why

The live quotes board currently uses a fixed ticker list. The board should source its ticker set from Tradernet's top-securities REST endpoint — live-verified to work unauthenticated — by default whenever the quotes screen opens, so the watchlist reflects real market data, while still falling back to the fixed list when the API is unavailable or empty.

## What Changes

- Add an optional top-securities REST source in `:feature:quotes:impl` using the already-configured Retrofit 3.0.0 and OkHttp 5.5.0. The request is an HTTP GET to `https://tradernet.com/api/` whose `q` query parameter is the JSON serialization of `{"cmd":"getTopSecurities","params":{"type":"stocks","exchange":"russia","gainers":0,"limit":30}}`. The documentation page is `https://tradernet.com/tradernet-api/quotes-get-top-securities` (and its example `https://tradernet.com/tradernet-api/get-example?id=quotes-get-top-securities`); these are documentation URLs, not the request URL.
- Record the mismatch truthfully: the documentation page labels the request as HTTPS POST, but the page's executable browser example performs `GET https://tradernet.com/api/` with the `q` parameter, and a live GET to that contract succeeded. Use the live-verified GET contract.
- Treat the response as an ordered `tickers` array of ticker IDs with an optional `details` map of names. The documented success shape is `{"tickers":[...]}` and the documented failure shape is `{"code","error","errMsg"}`. The response is primarily the ordered ticker-ID source; quote values continue to come from the existing live WebSocket, and `details` is used only as optional name metadata.
- Whenever the quotes screen opens, the board SHALL request the top-securities API. When the response is non-empty, use the API ticker IDs in API order as the board's active set, replacing the fixed list. On transport/HTTP/API-declared/parse failure or an empty list, use the exact fixed list from `live-quotes-board` in its original order.
- Update the quote subscription so it uses the active ticker set and cancels/reconnects/resubscribes when the active set changes, reusing the already-verified one-frame `["quotes", [<tickers>]]` format. This change MUST NOT invent, alter, or duplicate the subscription message framing, which remains governed by `live-quotes-board`.
- Preserve unknown-active-ticker filtering, partial quote merge, no-auth behavior, lifecycle stop/reconnect behavior, Compose + MVVM ownership, and screen row semantics.
- The invocation mechanism is fixed by user agreement: the API is requested by default on every quotes-screen open, and the fixed list is used completely as the active set on failure or empty. This change MUST NOT introduce a UI toggle, feature flag, build switch, or persisted source preference. While the request is in flight the current fixed rows MAY remain on screen as a temporary display before the set is replaced on success.
- Android only. No iOS. No tests or test coverage. No logos or min-step work.

## Capabilities

### New Capabilities
- `top-securities-source`: REST-backed replacement of the fixed watchlist with ordered Tradernet top-securities ticker IDs, requested by default on quotes-screen open, including complete fallback to the fixed list when the API is unavailable or empty and wiring of the active ticker set into the existing live quote subscription.

### Modified Capabilities
- `live-quotes-board`: The fixed list is the default and fallback active set, but the top-securities API ticker set becomes the active set in API order when the default screen-open request returns a non-empty response; the subscription frame stays identical and uses the active tickers; partial-update acceptance and unknown-ticker filtering apply to the current active set. No socket framing change.

## Impact

- `:feature:quotes:impl`: new Retrofit GET service, response parsing, a top-securities data source returning a typed success/failure result, and screen-open source resolution/fallback logic in the ViewModel plus active-ticker subscription wiring.
- `gradle/libs.versions.toml` and `:feature:quotes:impl`: Retrofit 3.0.0 and OkHttp 5.5.0 are already present; no new dependency is required.
- Depends on `live-quotes-board` and `android-modular-foundation`; both are merged into `develop`. Fixed-list fallback is defined by `live-quotes-board`; this change reuses it.
- OpenSpec change artifacts (proposal/design/specs/tasks) are local/ignored and remain so.
