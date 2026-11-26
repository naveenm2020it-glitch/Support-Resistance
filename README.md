# NIFTY Seller Intelligence

A provenance-first NIFTY options research application. It analyses multiple CE/PE strikes for a selected expiry using genuine, validated historical exchange reports and qualified order-flow evidence where authorised data is available.

## Project documentation

- [Approved requirements](docs/REQUIREMENTS.md)
- [Architecture baseline](docs/ARCHITECTURE.md)
- [Data dictionary](docs/DATA_DICTIONARY.md)
- [12-phase implementation roadmap](docs/IMPLEMENTATION_ROADMAP.md)
- [Current phase progress register](docs/PHASE_PROGRESS.md)
- [Development and data workflow](docs/WORKFLOW.md)
- [Data policy](docs/DATA_POLICY.md)

## Development status

The project is developed phase by phase on `feature/nifty-seller-intelligence-foundation`. Source code, tests, safe configuration templates, technical documentation and working progress are stored in this repository.

The progress register records each phase's status, blockers, changed files, commit references, test evidence and acceptance decision. Code being committed does not by itself mean that a phase is complete.

`main` remains unchanged until separately approved. The roadmap is planning documentation and does not by itself authorise implementation or deployment.

## Non-negotiable data integrity

- Never generate, simulate, estimate, reconstruct or backfill a historical NSE option-chain snapshot.
- Never replace a missing historical session with live/current data or another date's data.
- Bhavcopy is contract-level EOD data, not a complete historical option-chain snapshot.
- Preserve original downloaded NSE files unchanged in the approved permanent raw archive; process copies only.
- Record source URL/identifier, original filename, report date, download timestamp, SHA-256, available fields and validation result for every source artifact.
- Never mark a 22-session analysis complete unless all required sessions and required core reports are actually present and validated.
- Public NSE website automated scraping/requests are not permitted by this project design. Use only an authorised NSE data entitlement/provider or another permitted acquisition method.
- OI, price and volume are aggregate evidence; they do not identify an individual seller.
- Aggressor-side and order-book conclusions require reliable order/trade event evidence. Ambiguous events remain unclassified.
- Raw reports, credentials, access tokens and permanent market-data archives must not be committed to Git history.

## Analysis window

For a selected completed F&O trading session:
- Sessions 1–17: long-term positioning baseline.
- Sessions 18–21: recent positioning comparison.
- Session 22: selected EOD positioning.
- Total: 22 valid completed F&O sessions, including the selected date.

Session identification must use a verified exchange trading calendar and actual report availability. A calendar day is not automatically a trading session. A future, non-trading or not-yet-published EOD date cannot be declared complete.

## Seller-intelligence evidence

The planned evidence hierarchy prioritises reconstructed aggressive trade flow and passive sell-side order behaviour when the authorised order/trade data supports them. OI, premium, volume, underlying movement and adjacent-strike comparisons provide corroboration, not proof of actual seller identity or opening/closing intent.

## Code locations

- Python application: `src/nifty_seller_intelligence/`
- Tests: `tests/`
- Non-secret configuration: `config/`
- Dashboard: `dashboard/`
- Documentation and progress: `docs/`

No production downloader, live data substitution or deployment is enabled merely by the existing foundation.
