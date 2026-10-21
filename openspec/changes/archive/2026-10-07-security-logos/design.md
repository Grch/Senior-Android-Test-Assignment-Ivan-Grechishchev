## Context

See proposal.md — Why, and the `security-logos` spec. This change depends on `live-quotes-board`, which already renders the compact quote rows. Logos are a separate optional concern and must not couple to quote data or block rows. The image format served by the endpoint is unconfirmed and must be verified first.

## Goals / Non-Goals

**Goals:**
- Build logo URLs from the exact prefix `https://tradernet.com/logos/get-logo-by-ticker?ticker=` plus the lowercased ticker.
- Show a placeholder until/unless a logo loads.
- Guarantee that logo failures never block or degrade a row.

**Non-Goals:**
- Changing quote fields, ordering, coloring, or the socket/source behavior.
- Caching/persistence strategy beyond the loader's default.
- Min-step formatting (separate change).
- Tests or test coverage.

## Decisions

- **Use Coil for Compose image loading.** Coil is the standard Compose-native image loader and integrates with `AsyncImage`. Alternative considered: Glide — rejected because the project is Compose-first and Coil needs less boilerplate.
- **Lowercase the ticker at URL construction.** A small pure function builds the URL; the ticker is lowercased there so callers pass the domain ticker unchanged. Alternative considered: normalizing tickers at the data layer — rejected because case normalization is a logo concern only.
- **Placeholder is a neutral monogram tile.** When no logo is available, show a neutral rounded tile containing the ticker's first letter, so rows keep a stable layout. Alternative considered: hide the logo slot — rejected because it would shift row layout. The exact visual is left to implementation within this contract.
- **Non-blocking by construction.** The row composes the logo slot with its placeholder as the default; loading only replaces it when success arrives. No exception path touches quote content. Alternative considered: await the logo before rendering — rejected as blocking.
- **Verify format before choosing decoding.** The endpoint may serve PNG, JPEG, SVG, or nothing. A task verifies the format and whether the loader needs an SVG plugin before implementation.

## Risks / Trade-offs

- [Unknown/possibly SVG format] → Verify format first; add the loader's SVG extension only if required.
- [Network churn from per-row requests] → Rely on the loader's memory/disk cache and stable keys per ticker; no custom cache initially.
- [Placeholder may resemble a real logo] → Use a visually neutral tile distinct from real logos.
- [Logos can slow perceived list rendering] → Placeholder-first composition keeps rows immediate.

## Open Questions

- The exact image response format (and whether it is an image at all). **Resolved**: a real logo is `image/png`. A missing logo is *not* an HTTP error — it's a `200 OK` `image/gif`, a 1x1 fully transparent pixel. Implementation treats a successfully-decoded 1x1 image the same as a failure (shows the placeholder), detected via the loaded image's intrinsic size rather than by content-length, so it isn't tied to the exact byte size of that sentinel.
- Coil version: pinned to **3.3.0** rather than the latest (3.6.3), because 3.6.3's `kotlin-stdlib` constraint (2.4.10) is unreadable by this project's Kotlin 2.2.10 compiler. 3.3.0 is the newest release that constrains `kotlin-stdlib` to ≤2.2.0. Revisit this pin if the project's Kotlin version is ever bumped.
