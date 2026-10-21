## Context

See proposal.md — Why, and the `top-securities-source` and `live-quotes-board` specs. This change depends on `android-modular-foundation` (module boundary) and `live-quotes-board` (board, fixed list, and Retrofit/OkHttp dependencies), both merged into `develop`. The board already renders the fixed list and streams quotes over `wss.tradernet.com`. This change adds a top-securities source of ticker IDs that is requested by default whenever the quotes screen opens and falls back to the fixed list. The API contract is now verified from official documentation plus a live GET and is recorded in the "Verified API contract" section below.

## Goals / Non-Goals

**Goals:**
- Fetch top securities from the verified request contract via Retrofit with the `q` query parameter.
- Request the API by default every time the quotes screen opens; when the response is non-empty, replace the fixed list with the API ticker IDs in API order, and hand that active ticker set to the existing live quote subscription so every displayed ticker updates in real time.
- Fall back completely to the exact fixed list on any failure or empty response without breaking the board, and hand the fixed tickers to the same subscription boundary.
- Cancel/reconnect/resubscribe when the active ticker set changes, reusing the verified one-frame subscription format unchanged.
- Keep the invocation free of any UI toggle, feature flag, build switch, or persisted source preference.

**Non-Goals:**
- Adding a UI toggle, feature flag, build switch, or persisted preference for choosing the source; the invocation point is fixed (quotes-screen open).
- Defining or altering the subscription message framing (governed by `live-quotes-board`'s documented wire format).
- Treating the top-securities response as a source of quote values.
- Logos and min-step formatting (separate changes).
- Authentication.
- Tests or test coverage.

## Decisions

- **Retrofit GET to `api/` with a serialized `q` query.** A Retrofit interface declares `@GET("api/")` and `@Query("q") String`, where the value is the JSON serialization of `{"cmd":"getTopSecurities","params":{...}}`. The base URL is already `https://tradernet.com/` in `TradernetClientFactory`. The service returns `Response<ResponseBody>` (a built-in Retrofit type, so no converter dependency is added) and the body is parsed with `org.json`, matching the existing `QuoteMessageParser`. Alternative considered: a typed DTO with a JSON converter — rejected as unnecessary new dependency; the response is small and parsed once. Alternative considered: raw OkHttp — rejected because Retrofit is already required and gives declarative parameters.
- **Verified GET contract over the page's POST label.** The documentation page labels HTTPS POST, but its executable browser example issues `GET https://tradernet.com/api/` with `q`, and a live GET succeeded. The implementation follows the live-verified GET contract. Alternative considered: POST to the documentation page URL — rejected because it is a documentation page and the verified executable contract is GET plus `q`.
- **Ticker IDs and optional names only.** The response's ordered `tickers` array supplies the securities; `details[<ticker>].name` is optional name metadata. Quote values continue to come from the socket. Alternative considered: using the response as quote data — rejected; the response has no quote fields.
- **Typed success/failure result distinguishing failure kinds.** A feature-local `TopSecuritiesResult` returns `Success(securities)` only for a non-empty parsed list, and `Failure(error)` for `TRANSPORT`, `HTTP`, `API` (a `code`/`error`/`errMsg` response), `PARSE`, or `EMPTY`. This satisfies the requirement to distinguish each failure kind from non-empty success without introducing a new module; the project currently has no shared `Result` wrapper. Alternative considered: a generic `Result<T, DataError>` shared type — deferred because the foundation fixes the module set and no shared wrapper exists.
- **Screen-open source resolution lives in the ViewModel.** The ViewModel requests the API on the quotes-screen-open edge and resolves the active ticker set and fallback; the data source only fetches. This keeps composables free of business logic. Alternative considered: resolving the source in the data layer — rejected because the active-set decision is presentation state.
- **Replace, do not merge.** When the API list is active it replaces the fixed list entirely, matching the spec's "replace" behavior, preserving API order. Alternative considered: appending API results to the fixed list — rejected as contrary to the requirement.
- **Active ticker set drives the existing subscription boundary.** The ViewModel passes the currently active ticker set to `QuoteSocketDataSource.observeQuotes(tickers)`; when the resolved set actually changes, the current collection is cancelled and the stream reconnects/resubscribes with the new set. The subscription frame itself is unchanged (`subscriptionFrame(tickers)`) and remains governed by `live-quotes-board`'s verified wire format. Alternative considered: keep subscribing only the fixed list and merely render API rows — rejected because API tickers would then never receive real-time updates.
- **Fallback on failure or empty, non-blocking.** A failed or empty response resolves the active set to the fixed list and does not surface as a blocking error for the board.
- **Requested on every quotes-screen open, no control.** The ViewModel requests the API on the rising edge of the screen-visibility signal (`OnScreenVisibilityChanged(true)`) and keeps the current set (the fixed list on first open) rendering while the request is in flight; a newer open cancels an in-flight request. There is no source-selection action, UI toggle, flag, build switch, or persisted preference. `QuoteSource` is a read-only state indicator of which set is active. Alternative considered: an explicit source input or toggle — rejected by the user's confirmed choice; the API is the default invocation, not an opt-in control.

## Risks / Trade-offs

- [Verified contract could drift from live behavior] → Recorded documentation facts and the live GET separately in the "Verified API contract" section; the service and parser are isolated to a few files.
- [API list has different fields than the socket update] → Map API tickers only; quote fields continue to come from the socket, mapped to the same `Quote`/UI row contract required by `live-quotes-board`.
- [A fixed-list-only subscription would leave API tickers without live updates] → Feed the active ticker set into the existing subscription boundary and cancel/reconnect/resubscribe on source change; do not alter the message framing.
- [Changing the active set mid-stream could leak the old socket or duplicate rows] → The active-set change cancels the current stream job before starting a new one, and rows are rebuilt from the resolved active set.
- [Silent fallback hides real integration problems] → Fall back as required, but keep the failure kind in the typed result so it is observable in logging without blocking the UI.
- [Re-requesting on every screen open could churn the subscription or flash the wrong rows] → The current set stays on screen until the response resolves; each open cancels any in-flight request, and the socket only reconnects when the resolved active set actually differs from the current one, so an unchanged fixed fallback does not churn. There is never a back-and-forth toggle.

## Verified API contract (implementation evidence)

The facts below are separated by source: **official documentation** versus a **live GET**. Where the two disagree, the live-verified executable contract is authoritative and the discrepancy is recorded explicitly.

**Official documentation**

- Documentation page (NOT the request URL): `https://tradernet.com/tradernet-api/quotes-get-top-securities` (redirects to `?site_lang=en`).
- Dynamic example: `https://tradernet.com/tradernet-api/get-example?id=quotes-get-top-securities`.
- The page labels the request **HTTPS POST**.
- The executable browser example uses GET: `$.getJSON('https://tradernet.com/api/', {q: JSON.stringify(params)}, callback);`.
- Request command and parameters: `cmd = "getTopSecurities"`, `params = { type, exchange, gainers, limit }`. `gainers` is documented as a boolean list type (1 = top fastest-growing, 0 = top by trading volume). The example sample uses `exchange = "europe"`, `limit = 10` as illustration.
- Documented success shape: `{"tickers":["AAPL.US","T.US","F.US"]}` (`tickers` is an array of instruments).
- Documented failure shape: `{"code":5,"error":"Exchange is missing","errMsg":"Exchange is missing"}`.

**Live GET (authoritative for the executable contract)**

- Request performed: `GET https://tradernet.com/api/?q={"cmd":"getTopSecurities","params":{"type":"stocks","exchange":"russia","gainers":0,"limit":30}}` with no auth.
- Result: HTTP 200, successful. This confirms the GET-plus-`q` contract and the required query values (`type=stocks`, `exchange=russia`, `gainers=0`, `limit=30`).
- Observed success payload: an ordered `tickers` array of 30 ticker IDs (`GAZP`, `SIBN`, `VTBR`, ... `AFLT`) plus a `details` map keyed by ticker with `{"name":"..."}` and an unrelated `buttons` object. No quote price fields are present.
- The endpoint is dynamic (top securities by trading volume), so the exact ticker set and order vary between successful requests; each successful response is applied in the order it supplies, and the runtime reinstalled-app check applied a fresh 30-ticker set and resubscribed the socket.
- Therefore: `tickers` is used as the ordered ticker-ID source, `details` only as optional name metadata, and the `buttons` object is ignored.

## Open Questions

None. The invocation mechanism is agreed: the top-securities API is requested by default every time the quotes screen opens; on any failure or an empty response the board switches completely to the exact fixed list; there is no toggle, persisted preference, or build flag.
