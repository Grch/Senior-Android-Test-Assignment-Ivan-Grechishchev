# top-securities-source Specification

## Purpose

Replaces the live quotes board's fixed watchlist with an ordered set of ticker IDs returned by Tradernet's top-securities REST endpoint, requested by default every time the quotes screen opens, wires the active ticker set into the existing live subscription so displayed tickers update in real time, and preserves the exact fixed list as a complete fallback when the API is unavailable or empty. There is no toggle or persisted source preference. This capability requires no automated tests: it is verified against the official documentation and live-probe evidence.

## Requirements

### Requirement: Top-securities request uses the verified request contract

The documentation page for this capability is `https://tradernet.com/tradernet-api/quotes-get-top-securities` (which redirects to its `?site_lang=en` variant) and its executable example is `https://tradernet.com/tradernet-api/get-example?id=quotes-get-top-securities`. These are documentation URLs and MUST NOT be used as the request URL.

The request SHALL be an HTTP GET to `https://tradernet.com/api/` whose query parameter `q` is the JSON serialization of:

```json
{"cmd":"getTopSecurities","params":{"type":"stocks","exchange":"russia","gainers":0,"limit":30}}
```

No authentication credential, token, login, or session SHALL be sent. The documentation page labels the request as HTTPS POST, but the page's executable browser example issues a GET with the `q` query parameter and a live GET to that contract succeeded; the GET-plus-`q` contract SHALL be used.

The response schema SHALL be taken from the official documentation plus the live-verified payload and MUST NOT be assumed. On success the response contains an ordered `tickers` array of ticker IDs (the documented example is `{"tickers":[...]}`) and MAY contain a `details` map keyed by ticker with a `name`; on failure the documented shape is `{"code":<int>,"error":<string>,"errMsg":<string>}`.

#### Scenario: Request is issued with the verified contract

- **WHEN** the quotes screen opens and the top-securities source is invoked
- **THEN** an HTTP GET is sent to `https://tradernet.com/api/`
- **AND** the request includes a `q` query parameter containing `cmd=getTopSecurities` with `type=stocks`, `exchange=russia`, `gainers=0`, and `limit=30`
- **AND** no authentication credential is sent

#### Scenario: Documentation URL is not used as the request URL

- **WHEN** the request URL is inspected
- **THEN** it is `https://tradernet.com/api/`, not `https://tradernet.com/tradernet-api/quotes-get-top-securities`
- **AND** the documentation page's HTTPS POST label is not treated as the executable contract, because the browser example and a live GET both succeed with GET plus `q`

#### Scenario: Response mapping follows documentation and the live sample

- **WHEN** a successful response is mapped
- **THEN** the ordered `tickers` array supplies the securities in response order
- **AND** any `details[<ticker>].name` value is used only as optional name metadata
- **AND** the mapping is recorded with a reference to the documentation page and the live-verified response

### Requirement: Ticker IDs, not quote fields, are the source

The top-securities response SHALL be treated as a source of ticker IDs (and at most optional name metadata). It SHALL NOT be treated as a source of quote values. Live quote fields (exchange, last-trade price, change, percent change, previous close) SHALL continue to come from the existing live quote stream defined by `live-quotes-board`.

#### Scenario: API supplies only ticker IDs and optional names

- **WHEN** the API response is parsed
- **THEN** only `tickers` and the optional `details` names are consumed
- **AND** no quote values are read from the top-securities response

#### Scenario: Quote values still arrive over the existing stream

- **WHEN** the board is displaying API-provided tickers
- **THEN** their quote values continue to arrive from the existing live quote stream

### Requirement: API is requested by default on every quotes-screen open

The board SHALL request the top-securities API by default every time the quotes screen opens, without any user action. There SHALL be no UI toggle, feature flag, build switch, or persisted source preference. While the request is in flight, the current active set (the fixed list on first open) MAY remain on screen as a temporary display. When the API returns a non-empty `tickers` list, the API-returned ticker IDs SHALL become the active set in the order returned by the API, in place of the fixed ticker list, and each row SHALL display the row content required by `live-quotes-board`. On any failure or an empty response, the exact fixed ticker list defined by `live-quotes-board` SHALL be the active set in its original order.

#### Scenario: Screen open requests the API with no source control

- **WHEN** the quotes screen opens
- **THEN** the top-securities API is requested with no user action
- **AND** no UI toggle, feature flag, build switch, or persisted preference selects the source

#### Scenario: Non-empty API response replaces the fixed list in API order

- **WHEN** the quotes screen opens
- **AND** the API returns one or more tickers
- **THEN** the board displays the API-returned tickers
- **AND** the rows appear in the order returned by the API
- **AND** the fixed ticker list is not the active set

#### Scenario: Board remains functional with API securities

- **WHEN** the board is displaying API-returned securities
- **THEN** each row still shows the quote fields required by `live-quotes-board`

#### Scenario: Fixed list may show while the request is in flight

- **WHEN** the quotes screen opens and the API request has not yet resolved
- **THEN** the current active set (the fixed list on first open) may remain on screen
- **AND** it is replaced by the API set only when a non-empty response is applied

### Requirement: Fallback to the fixed list when the API is unavailable or empty

When the screen-open request fails — transport failure, HTTP failure, an API-declared failure (`code`/`error`/`errMsg`), a parse failure, or an empty `tickers` list — the board SHALL use the exact fixed ticker list defined by `live-quotes-board` as the active set, preserving its order. The fallback SHALL NOT block the board from rendering.

#### Scenario: Transport, HTTP, API-declared, or parse failure falls back

- **WHEN** the quotes screen opens
- **AND** the request fails at the transport, HTTP, API-declared, or parse stage
- **THEN** the board displays the fixed ticker list in its exact order
- **AND** the board remains usable

#### Scenario: Empty API response falls back

- **WHEN** the quotes screen opens
- **AND** the API returns an empty `tickers` list
- **THEN** the board displays the fixed ticker list in its exact order

### Requirement: Active ticker set is subscribed on the live quote stream

The active ticker set SHALL drive the existing live quote subscription. When API-returned tickers are active, the API-returned tickers SHALL be provided to the existing quote-stream/subscription boundary; when the board falls back to the fixed ticker list, the fixed tickers SHALL be the active set provided to that boundary. When the active ticker set changes, the current subscription SHALL be cancelled and the stream SHALL reconnect/resubscribe with the new active set. This SHALL ensure every displayed ticker receives real-time updates. The subscription message framing SHALL remain exactly the one defined and verified by `live-quotes-board`; this change MUST NOT invent, alter, or duplicate a wire format.

#### Scenario: API-selected tickers are subscribed for live updates

- **WHEN** a non-empty API response is applied
- **THEN** the API-returned tickers are the active set provided to the existing live quote subscription boundary
- **AND** every displayed ticker receives real-time quote updates

#### Scenario: Fallback fixed list is subscribed for live updates

- **WHEN** the board falls back to the fixed ticker list
- **THEN** the fixed tickers are the active set provided to the existing live quote subscription boundary
- **AND** every displayed ticker receives real-time quote updates

#### Scenario: Changing the active ticker set resubscribes

- **WHEN** the active ticker set changes from the fixed fallback to the API set
- **THEN** the current subscription is cancelled
- **AND** the stream reconnects and resubscribes with the new active ticker set
- **AND** no displayed ticker is left without live updates

#### Scenario: Subscription framing is not redefined

- **WHEN** the active ticker set reaches the subscription boundary
- **THEN** the subscription message framing remains the one defined and verified by `live-quotes-board`
- **AND** this change does not introduce an alternate or assumed wire format

### Requirement: Active-set filtering for incoming quotes

Incoming quote updates SHALL be merged only for tickers in the current active set. An update whose ticker is not in the current active set SHALL be ignored and SHALL NOT add or modify a row, and partial updates SHALL continue to preserve prior non-null values without changing row order. This extends the `live-quotes-board` partial-merge behavior from the fixed list to the current active set.

#### Scenario: Unknown active-set ticker is ignored

- **WHEN** a quote update arrives for a ticker that is not in the current active set
- **THEN** the update is ignored
- **AND** no row is added or modified

#### Scenario: Partial update preserves prior values on the active set

- **WHEN** a row in the active set has prior non-null fields
- **AND** a later update carries only some fields
- **THEN** the carried fields update the row
- **AND** the omitted fields retain their prior non-null values
- **AND** row order is unchanged
