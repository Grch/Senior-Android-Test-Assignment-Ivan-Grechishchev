## Purpose

Provides the assignment's primary screen: a no-auth, real-time watchlist that streams live Tradernet quotes for a fixed set of tickers and presents each security compactly with sign coloring and update feedback.

## ADDED Requirements

### Requirement: Fixed watchlist order

The board SHALL present exactly the following tickers, in this order, as its default source:

`SP500.IDX`, `AAPL.US`, `RSTI`, `GAZP`, `MRKZ`, `RUAL`, `HYDR`, `MRKS`, `SBER`, `FEES`, `TGKA`, `VTBR`, `ANH.US`, `VICL.US`, `BURG.US`, `NBL.US`, `YETI.US`, `WSFS.US`, `NIO.US`, `DXC.US`, `MIC.US`, `HSBC.US`, `EXPN.EU`, `GSK.EU`, `SHP.EU`, `MAN.EU`, `DB1.EU`, `MUV2.EU`, `TATE.EU`, `KGF.EU`, `MGGT.EU`, `SGGD.EU`, `AI.US`.

The board SHALL NOT insert, remove, reorder, or re-sort these entries based on arriving data.

#### Scenario: Board renders the fixed list in order

- **WHEN** the quotes board is shown with the default source
- **THEN** it renders one row for each ticker above
- **AND** the rows appear in the exact order listed

#### Scenario: Data arrival does not reorder rows

- **WHEN** a quote update arrives for a ticker already in the list
- **THEN** that ticker's row stays in its original position
- **AND** no new ticker outside the list is added

### Requirement: No-auth live subscription over the Tradernet socket

The system SHALL connect to the WebSocket host `wss.tradernet.com` without sending authentication credentials. The subscription SHALL be a single WebSocket JSON text frame whose value is `["quotes", [<ticker>, ...]]`, using the fixed watchlist tickers: the first element is the literal event name `quotes` and the second element is the ticker array. The intended subscription value is specified by the assignment PDF and by the official Tradernet example at `https://tradernet.com/tradernet-api/get-example?id=quotes-get-changes` (page `https://tradernet.com/tradernet-api/quotes-get-changes/?site_lang=en`). The official page's browser helper shows a two-argument `ws.send("quotes", ...)`, which is an invalid native WebSocket call; the runtime probe verifies that the valid one-frame serialized form above is accepted.

Incoming update messages SHALL be read as a top-level JSON array in which index 0 is the event name `q`, index 1 is a single JSON object carrying the quote fields, and any optional trailing element `wstm=...` is a server timestamp that SHALL be ignored. The quote object's field names and meanings are cited from the official documentation: `c` is the ticker, `ltr` is the last-trade exchange, `name` is the security name, `pp` is the previous close, `ltp` is the last-trade price, `chg` is the signed point change versus the previous close, and `pcp` is the percent change versus the previous close. The actual `q` envelope shape (top-level array with the quote object at index 1) and the optional trailing `wstm=...` timestamp are based on live-probe evidence recorded in design.md and tasks.md. The documentation's example renders the `q` payload as an object while the page's helper is array-oriented (`data.forEach`, `QuoteInfoAnswer[]`); the runtime probe resolves that conflict to the single object at index 1, and only that object form SHALL be handled.

#### Scenario: Connection is unauthenticated

- **WHEN** the board opens its live connection
- **THEN** it connects to `wss.tradernet.com` without requiring or sending a token, login, or session credential

#### Scenario: Subscription uses the documented quotes event

- **WHEN** the connection is established
- **THEN** the client sends one text frame whose value is `["quotes", [<fixed tickers>]]`
- **AND** the first element is the literal `quotes` event name and the second element is the ticker array
- **AND** the invalid two-argument `ws.send("quotes", ...)` browser helper form is not used

#### Scenario: Update field mapping is documentation-backed

- **WHEN** an update message delivered under `q` is mapped to a row
- **THEN** the message is treated as a top-level array with the event `q` at index 0 and a single quote object at index 1
- **AND** any optional trailing `wstm=...` timestamp element is ignored
- **AND** the named fields `c`, `ltr`, `name`, `pp`, `ltp`, `chg`, and `pcp` map to the ticker, last-trade exchange, security name, previous close, last-trade price, signed points versus close, and percent versus close respectively
- **AND** the mapping is recorded with the field names and meanings citing the official documentation, while the actual `q` envelope shape and trailing timestamp cite live-probe evidence recorded in design.md and tasks.md

### Requirement: Partial quote update merge by ticker

The system SHALL key each incoming quote update by its `c` ticker and merge it into the existing row for that ticker. When a later `q` object omits a field, the row SHALL preserve its previously held non-null value for that field. The system SHALL ignore any update whose ticker is not one of the fixed watchlist tickers and SHALL NOT add a row for it. Merging SHALL NOT change the position or order of any row.

#### Scenario: Partial update preserves prior values

- **WHEN** a row already shows an exchange, security name, previous close, and percent change
- **AND** a later `q` object for that ticker carries only the last-trade price
- **THEN** the row updates its last-trade price
- **AND** the exchange, security name, previous close, and percent change retain their prior non-null values

#### Scenario: Unknown ticker is ignored

- **WHEN** a `q` object arrives whose `c` ticker is not in the fixed watchlist
- **THEN** the system ignores the update
- **AND** no row is added or modified for that ticker

#### Scenario: Merge preserves row order

- **WHEN** any quote update is merged into a row
- **THEN** the row keeps its original position
- **AND** the order of all rows is unchanged

### Requirement: Compact quote row content

Each row SHALL display: the ticker; the percent change relative to the previous close; the last-trade exchange; the security name; the last-trade price; and the absolute price delta relative to the previous close. The row SHALL be laid out compactly with the ticker and a secondary exchange/company label on the leading side, the percentage at the trailing side, the price with the parenthesized delta beneath or beside it, and a divider with a trailing chevron.

#### Scenario: Row shows all required fields

- **WHEN** a row has received a quote
- **THEN** the row shows the ticker, percent change, last-trade exchange, security name, last-trade price, and absolute delta versus previous close

#### Scenario: Row is compact with trailing chevron

- **WHEN** the board renders a row
- **THEN** the ticker and the secondary exchange/company label are shown leading
- **AND** the percentage is shown trailing
- **AND** the price and its parenthesized delta are shown
- **AND** a divider and a trailing chevron complete the row

### Requirement: Sign coloring and directional update flash

The system SHALL color positive changes green and negative changes red. On a directional price update, the row SHALL flash green for an upward move and red for a downward move. A brief flash duration SHALL be used as a default and MUST be treated as an assumption documented in the design.

#### Scenario: Positive and negative colors

- **WHEN** a row's percent change or delta is positive
- **THEN** its sign value is displayed in green
- **WHEN** a row's percent change or delta is negative
- **THEN** its sign value is displayed in red

#### Scenario: Directional update flashes

- **WHEN** an incoming update raises the last-trade price
- **THEN** the row flashes green
- **WHEN** an incoming update lowers the last-trade price
- **THEN** the row flashes red

### Requirement: Foreground and visibility aware socket lifecycle

The ViewModel SHALL coordinate the socket lifecycle. The socket SHALL stop when the app is backgrounded OR the quotes screen becomes hidden, and SHALL reconnect and resubscribe when the app is foregrounded AND the quotes screen is visible. No foreground service SHALL be used, and no composable SHALL own a socket or stream.

#### Scenario: Backgrounding stops the socket

- **WHEN** the app moves to the background
- **THEN** the quotes socket is stopped
- **AND** no foreground service is started to keep it alive

#### Scenario: Screen hidden stops the socket

- **WHEN** the quotes screen is navigated away from or otherwise hidden while the app is foreground
- **THEN** the quotes socket is stopped

#### Scenario: Foreground and visible reconnects

- **WHEN** the app is foregrounded AND the quotes screen is visible
- **THEN** the socket reconnects and resubscribes using the documented `quotes` event

#### Scenario: Socket is not owned by UI

- **WHEN** the composables are inspected
- **THEN** no composable creates or holds the socket or stream
- **AND** the ViewModel is the owner of stream orchestration
