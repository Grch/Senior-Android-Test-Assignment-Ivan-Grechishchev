<p align="center"><img src="docs/logo.png" width="120"></p>

# Tradernet Quotes

An Android test assignment: a single Jetpack Compose screen (MVVM) showing a live quotes board in the style of the Tradernet app, fed by the Tradernet WebSocket API (`wss://wss.tradernet.com`).

## Features

- Live quotes table for a fixed ticker list, subscribed via `["quotes", [...]]`; `q` frames are parsed and merged per ticker (`min_step`, name and exchange arrive only in a ticker's first frame).
- Rows render immediately; a shimmer is shown only for fields that have no data yet.
- The percent-change badge flashes green or red for 300 ms when the last price ticks up or down.
- Prices and changes are rounded to the security's `min_step`.
- Company logos loaded from `https://tradernet.com/logos/get-logo-by-ticker?ticker=<lowercase ticker>`.
- Extra tickers from the top-securities REST endpoint (stocks, exchange `russia`, gainers `0`, limit 30).
- The socket is closed when the app goes to the background and survives rotation.
- Connection loss handling: automatic reconnect, then an error screen with "Try again" (details below).
- Localized strings: English (default) and Russian.

## Architecture

Package root: `com.grch.tradernettestassignment`. The quotes feature is split into three layers.

```text
:app                           Hilt composition root (@HiltAndroidApp), theme, single activity
:feature:quotes:domain         Pure Kotlin/JVM: models, repository interfaces, FixedTickers
:feature:quotes:data           OkHttp WebSocket, Retrofit + Moshi REST, parsers, Hilt module
:feature:quotes:presentation   ViewModel, Compose UI, Coil logos
```

Dependencies point inwards: `presentation` and `data` depend on `domain`, and `:app` wires everything together.

## Tech stack

Kotlin, Jetpack Compose + Material 3, Hilt, Coroutines/Flow, OkHttp (WebSocket), Retrofit, Moshi, Coil 3. minSdk 28.

## Notable decisions

- **Top securities augment the fixed list instead of replacing it.** The fixed ticker list is a mandatory requirement, so tickers returned by the REST call are appended after it. If the call fails, the board silently falls back to the fixed list.
- **Cyrillic "А" in `АI.US`.** In the assignment PDF the last ticker starts with a Cyrillic "А" (U+0410); the app subscribes to the Latin `AI.US`.
- **Connection loss.** The data layer reconnects automatically after a 2 s delay. After 3 consecutive failed attempts the UI shows an error screen with "Try again". Tapping it shows the dimmed board with a spinner until the connection is restored.
- **`min_step` rounding.** A price is rounded to the nearest multiple of `min_step` and shown with `min_step`'s own number of decimals. Because `min_step` arrives only in the first frame, it is kept per ticker across later partial updates. Without a `min_step`, the raw value is formatted.

## Build & test

```bash
./gradlew assembleDebug                # build the debug APK
./gradlew lint testDebugUnitTest       # lint + JVM unit tests (data, presentation)
./gradlew connectedDebugAndroidTest    # Compose UI tests (needs a device or emulator)
```
