# security-logos Specification

## Purpose
Optionally augments each quote row with the security's logo fetched from Tradernet, using a well-defined URL and a placeholder so that logo loading never blocks or breaks the board.

## Requirements

### Requirement: Logo URL is built from the documented prefix and lowercased ticker

The system SHALL request each security's logo from `https://tradernet.com/logos/get-logo-by-ticker?ticker=` concatenated with the ticker converted to lowercase. No other host, path, or parameter SHALL be used.

#### Scenario: URL uses prefix plus lowercased ticker

- **WHEN** a logo is requested for a ticker
- **THEN** the request URL is `https://tradernet.com/logos/get-logo-by-ticker?ticker=` followed by the lowercased ticker
- **AND** no additional query parameters are appended

#### Scenario: Case is normalized

- **WHEN** the ticker contains uppercase characters
- **THEN** the ticker used in the URL is lowercased before concatenation

### Requirement: Logo failure never blocks quote rows

Logo loading SHALL be non-blocking. If a logo request fails for any reason (network, HTTP, or decode) or no logo is available, the row SHALL still render all required quote content and SHALL show a defined placeholder in place of the logo.

#### Scenario: Failed logo request still renders the row

- **WHEN** a logo request fails or an empty/failed image is returned
- **THEN** the quote row still renders its required fields
- **AND** a placeholder is shown where the logo would appear

#### Scenario: Missing logo does not degrade the board

- **WHEN** one or more logos cannot be loaded
- **THEN** the board remains usable and other rows are unaffected

### Requirement: Logo response format is verified before loading

The image response format served by the logo endpoint SHALL be verified before an image loader is chosen and before decoding is implemented. The loader SHALL support the verified format.

#### Scenario: Format verified before implementation

- **WHEN** logo loading is implemented
- **THEN** a task has verified the endpoint's image response format and recorded it
- **AND** the chosen loader supports that format
