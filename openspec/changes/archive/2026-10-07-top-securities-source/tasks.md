## 1. Documentation verification

- [x] 1.1 Locate and cite the official documentation page (NOT the request URL) `https://tradernet.com/tradernet-api/quotes-get-top-securities` and its example `https://tradernet.com/tradernet-api/get-example?id=quotes-get-top-securities`; record the response schema (ordered `tickers` array, optional `details` name map, and failure `code`/`error`/`errMsg`), and verify the recorded mapping is used by the DTO/parser task.
- [x] 1.2 Confirm the request contract against the documentation, its executable browser example, and a live GET: the request is `GET https://tradernet.com/api/` with `q` equal to the serialized `{"cmd":"getTopSecurities","params":{"type":"stocks","exchange":"russia","gainers":0,"limit":30}}`; record the documentation URL, the page's HTTPS POST label mismatch, the executable GET example, and the successful live GET response.

## 2. REST source

- [x] 2.1 Add a Retrofit service interface declaring `@GET("api/")` with `@Query("q")` carrying the serialized request object, using the existing `TradernetClientFactory.retrofit` (`https://tradernet.com/`) and returning `Response<ResponseBody>`; verify the generated request URL is `https://tradernet.com/api/` and that the `q` query value contains `cmd=getTopSecurities`, `type=stocks`, `exchange=russia`, `gainers=0`, and `limit=30`.
- [x] 2.2 Parse the response into the ordered `tickers` list plus optional `details` name metadata, referencing the URL recorded in 1.1; verify the live-verified sample maps to the expected ticker IDs in the exact API order and that `details` supplies optional names only.
- [x] 2.3 Implement a top-securities data source returning a typed result that returns non-empty success only for a non-empty parsed `tickers` list, and distinguish transport, HTTP, API-declared (`code`/`error`/`errMsg`), parse, and empty failures from that success.

## 3. Source selection and fallback

- [x] 3.1 Resolve the active ticker set when the quotes screen opens: request the top-securities API and, on a non-empty success, select the API ticker IDs in API order as the active set; record the active source (`QuoteSource`) as read-only state. Verify the spec's fixed order is replaced by API order while the API set is active, and that the fixed list is the initial (in-flight) and fallback active set.
- [x] 3.2 On API transport/HTTP/API-declared/parse failure or an empty response, fall back to the exact fixed ticker list from `live-quotes-board` in its original order, and verify the board renders the fixed list and stays usable.
- [x] 3.3 Verify that API-returned securities still render the row fields required by `live-quotes-board`.
- [x] 3.4 Pass the active ticker set into the existing quote subscription boundary — the API-returned tickers when the API response has been applied with a non-empty set, and the fixed ticker list when falling back — and verify every displayed ticker is covered by the live subscription. Do not define, alter, or duplicate the subscription framing; it remains governed by `live-quotes-board`.
- [x] 3.5 Verify that an API-success screen open applies the API set and subscribes it, and that a failure/empty screen open applies the fixed fallback and subscribes it, each updating the subscribed active ticker set (cancel/reconnect/resubscribe only when the set changes); there is no back-and-forth toggle, so no displayed ticker is left without live updates.
- [x] 3.6 Verify that both active-source subscription and unknown-ticker handling are covered: incoming updates are merged only for tickers in the current active set, an update for an unknown active-set ticker is ignored without adding or modifying a row, and partial updates preserve prior non-null values without changing row order.

## 4. Enablement wiring (agreed)

- [x] 4.1 Confirm the enablement mechanism with the user and record the agreed mechanism: invoke the top-securities API by default whenever the quotes screen opens; on error or an empty response switch completely to the exact fixed list; no toggle, persisted preference, or build flag. Verify no toggle, flag, or default was invented ahead of this agreement.
- [x] 4.2 Wire the agreed screen-open invocation: request the top-securities API on the quotes-screen-open edge (no UI control), apply the API ticker set on a non-empty success, and apply the full fixed list on failure/empty. Verify the board shows API securities on success and the fixed list as the fallback, with no toggle or persisted preference.

## 5. Verification and constraints

- [x] 5.1 Verify no authentication is added and Retrofit is used for the REST call.
- [x] 5.2 Run `./gradlew assembleDebug` and confirm the app builds with the API source requested on quotes-screen open and no tests required.
