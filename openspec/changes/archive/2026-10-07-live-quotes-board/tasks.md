## 1. Documentation verification and dependencies

- [x] 1.1 Reconcile the subscription frame from three sources: the assignment PDF's intended `quotes` value, the official Tradernet "quotes changes" page `https://tradernet.com/tradernet-api/quotes-get-changes/?site_lang=en` and its example `https://tradernet.com/tradernet-api/get-example?id=quotes-get-changes` (identifying the two-argument `ws.send("quotes", ...)` browser helper as an invalid native WebSocket call), and live-frame evidence; record the single text-frame form `["quotes", [<tickers>]]` and verify it is used by the subscription implementation/runtime verification in task 2.1.
- [x] 1.2 Separate documented field names from the live `q` envelope shape: record the named documented fields (`c`, `ltr`, `name`, `pp`, `ltp`, `chg`, `pcp`) and the live-observed top-level array shape (`"q"` at index 0, quote object at index 1, optional trailing `wstm=...` ignored), citing the source URL and noting that the runtime probe resolves the documentation's object-vs-array conflict.
- [x] 1.3 Add Retrofit and the latest stable OkHttp WebSocket to `gradle/libs.versions.toml` and `:feature:quotes:impl`, resolving a compatible stable version pair, and verify the module compiles.

## 2. Socket data source and mapping

- [x] 2.1 Implement a quotes socket data source in `:feature:quotes:impl` that connects to `wss.tradernet.com` without credentials and sends the one-frame `quotes` subscription; verify against a raw live capture that the message is a top-level array with the quote object at index 1 and an optional trailing `wstm=...` timestamp trailer.
- [x] 2.2 Define DTOs with nullable fields matching the documented `q` field names and mappers to the domain `Quote` model; verify `chg` maps as the signed point change versus previous close and that partial `q` objects are merged by `c` ticker preserving prior non-null fields (referencing the URL recorded in 1.2).
- [x] 2.3 Expose a stream of quote updates from the data source and verify it emits parsed `Quote` values for arriving messages.

## 3. ViewModel and state

- [x] 3.1 Define `QuotesState`, `QuotesAction`, and the ordered fixed ticker list (from the spec) in `:feature:quotes:impl`, and verify the list order matches the spec exactly, including `AI.US`.
- [x] 3.2 Implement the quotes ViewModel that owns the socket stream, merges updates into state by ticker without reordering, and exposes the state flow; verify updates change only the matching row's fields, and verify by implementation inspection that `QuotesViewModel.mergeQuote` looks up the ticker in `state.rows` and returns before any state mutation when the index is absent (so an update whose ticker is outside the fixed watchlist is ignored and adds no row), and when present replaces only the matched row index, confirming no new row, no row-count change, and no order change.
- [x] 3.3 Track directional price changes in the ViewModel to drive flash state, and verify upward and downward moves are distinguishable.

## 4. Compose UI

- [x] 4.1 Implement the compact quote row showing ticker, secondary exchange/company label, trailing percentage, price with parenthesized delta, divider, and trailing chevron; verify against the required layout in the spec.
- [x] 4.2 Color positive signs green and negative signs red; verify by inspecting rows with positive and negative values.
- [x] 4.3 Implement the directional flash using a brief default of 300 ms (assumption) without causing continuous recomposition; verify an update briefly flashes the row and then settles.

## 5. Lifecycle coordination

- [x] 5.1 Have the screen report visibility changes to the ViewModel as actions, and track app foreground/background, and verify no composable creates or holds the socket.
- [x] 5.2 Stop the socket when the app backgrounds OR the screen is hidden, and verify the connection closes in each case (observe logs/connection state).
- [x] 5.3 Reconnect and resubscribe only when the app is foregrounded AND the screen is visible, and verify the socket re-establishes and resubscribes on the rising edge without starting a foreground service.

## 6. Integration verification

- [x] 6.1 Run the app and confirm the board shows all fixed tickers in the exact order and that visible values update live.
- [x] 6.2 Confirm no authentication is required and no credentials are sent when opening the socket.
- [x] 6.3 Run `./gradlew assembleDebug` and confirm the app builds with the quotes board.
