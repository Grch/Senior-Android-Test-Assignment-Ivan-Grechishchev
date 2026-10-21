## Why

Rows currently show ticker text and labels only. As an optional visual improvement, each security can show its logo fetched from Tradernet's logo endpoint, making the board easier to scan, as long as logos never block or break quote rows.

## What Changes

- Add optional security logo loading to `:feature:quotes:impl`, fetching from `https://tradernet.com/logos/get-logo-by-ticker?ticker=` appended with the lowercased ticker.
- Display the logo in the quote row when available.
- Failures (network/HTTP/decode) SHALL NOT block or degrade quote rows; a defined placeholder SHALL be shown when a logo is missing or fails to load.
- Verify the image response format served by the endpoint as an implementation task before choosing a loader.
- Android only. No iOS. No tests or test coverage are planned.

## Capabilities

### New Capabilities
- `security-logos`: Optional per-security logo loading from Tradernet with a non-blocking placeholder fallback.

### Modified Capabilities
<!-- None. -->

## Impact

- `:feature:quotes:impl`: logo URL builder, image-loading integration, and row placeholder.
- `gradle/libs.versions.toml` and `:feature:quotes:impl`: image-loading dependency if not already present.
- Depends on `live-quotes-board`; OpenSpec does not enforce cross-change sequencing, so it MUST be applied first.
- No change to quote data fields or the fixed/API source behavior.
