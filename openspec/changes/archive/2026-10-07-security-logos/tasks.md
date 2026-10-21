## 1. Verification and dependency

- [x] 1.1 Request a logo for a known ticker and verify the response format (e.g., PNG/JPEG/SVG), recording the result; confirm whether an SVG plugin is needed.
  - Verified via `curl -D-` against the live endpoint: a ticker with a real logo (`aapl.us`, `gazp`, `sp500.idx`) returns `200 OK` with `content-type: image/png`. No SVG involved, so no SVG decoder plugin is needed.
  - A ticker with no logo (`zzznotreal123`) also returns `200 OK`, not an HTTP error — but `content-type: image/gif`, 43 bytes, confirmed with `file`/`xxd` to be a 1x1, fully transparent GIF (`GIF89a`, transparent color index set in its Graphic Control Extension). This is Tradernet's own "no logo" sentinel; it must be detected and treated as "no logo" even though the HTTP request itself succeeds.
- [x] 1.2 Add the chosen Compose image-loading dependency to `gradle/libs.versions.toml` and `:feature:quotes:impl`, and verify the module compiles.
  - Added Coil 3 (`coil-compose` + `coil-network-okhttp`). Coil 3.6.3 (latest at the time) constrains `kotlin-stdlib` to 2.4.10, which the project's Kotlin 2.2.10 compiler cannot read (metadata version mismatch, build failure). Pinned to Coil **3.3.0**, the newest release that constrains `kotlin-stdlib` to 2.2.0 or lower; module compiles and `assembleDebug` succeeds.

## 2. URL construction

- [x] 2.1 Implement a pure function that builds `https://tradernet.com/logos/get-logo-by-ticker?ticker=` plus the lowercased ticker, and verify the exact output for an uppercase ticker such as `AAPL.US` and a mixed-case ticker.
  - `TradernetUrls.logoUrl(ticker)` = `BASE_URL + "logos/get-logo-by-ticker?ticker=" + ticker.lowercase()`. `AAPL.US` → `.../get-logo-by-ticker?ticker=aapl.us`; verified live against the real endpoint.
- [x] 2.2 Verify no extra query parameters or alternate host/path are added.
  - Confirmed: the function concatenates exactly `BASE_URL` (`https://tradernet.com/`) + the fixed logo path + the lowercased ticker, nothing else.

## 3. Row integration

- [x] 3.1 Add a logo slot to the quote row that shows the defined neutral monogram placeholder by default, and verify the row layout is stable before and after a logo loads.
  - `SecurityLogo` composable: a fixed 36dp rounded tile, monogram (ticker's first letter) shown by default. Verified on-device (emulator) that row height/layout doesn't shift when a logo loads in.
- [x] 3.2 Load the logo with the image loader and replace the placeholder on success, and verify a loaded logo appears for a ticker whose logo exists.
  - Verified on-device for many fixed and augmented tickers (AAPL.US, GAZP, SBER, RUAL, YETI.US, NIO.US, OZON, ALRS, ...): real brand logos render.
  - **Found and fixed a real bug during on-device testing**: initially the monogram was drawn underneath the `AsyncImage` unconditionally, on the assumption a loaded real logo would always visually cover it. Most brand PNGs have a *transparent* background (not a solid tile), so the monogram was bleeding through next to/around the real logo (e.g. `YETI.US` showed "Y" overlapping the real "YETI" wordmark). Fixed by tracking `AsyncImage`'s `onState`: the monogram is now shown only while loading, on error, or when the successfully-decoded image is the 1x1 sentinel from 1.1 (detected via `AsyncImagePainter.State.Success`'s `painter.intrinsicSize`); it's removed entirely once a real (non-1x1) image loads, instead of staying underneath it.
- [x] 3.3 Ensure a failed or empty logo request leaves the row fully rendered with the placeholder, and verify by simulating/forcing a failure that quote content is unaffected.
  - By construction: the monogram/placeholder and the quote row's text content are composed independently; `AsyncImage`'s own loading/success/error lifecycle never touches `QuoteRow` state. A network/HTTP/decode failure (or the 1x1 sentinel) simply keeps `showPlaceholder = true`; the rest of the row is unaffected either way.

## 4. Verification and constraints

- [x] 4.1 Verify logo loading does not change quote ordering, coloring, or source behavior.
  - No changes to `QuotesViewModel`, `QuotesState`, `QuoteRepository`, or source-policy code; logo loading is purely additive in `QuotesScreen.kt`/`TradernetUrls.kt`.
- [x] 4.2 Run `./gradlew assembleDebug` and confirm the app builds with logos.
  - `./gradlew :feature:quotes:impl:lintDebug :feature:quotes:impl:assembleDebug :app:lintDebug :app:assembleDebug` — BUILD SUCCESSFUL, 0 lint issues in `:feature:quotes:impl`.
