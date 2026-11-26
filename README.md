# NIFTY Seller Intelligence

A provenance-first NIFTY options research application. It analyses multiple CE/PE strikes for a selected expiry using genuine, validated historical NSE reports.

## Non-negotiable data integrity
- Never generate, simulate, estimate, reconstruct, or backfill a historical NSE option-chain snapshot.
- Never replace a missing historical session with live/current data or another date's data.
- Bhavcopy is contract-level EOD data, not a complete historical option-chain snapshot.
- Preserve original downloaded NSE files unchanged under `data/raw/`; process copies only.
- Record source URL, original filename, report date, download timestamp, SHA-256, available fields, and validation result for every source artifact.
- Never mark a 22-session analysis complete unless all required sessions and required core reports are actually present and validated.
- Public NSE website automated scraping/requests are disabled by design. Use manual downloads or an authorised NSE data entitlement/integration.

## Analysis window
For a selected completed F&O trading session:
- Sessions 1–17: long-term positioning baseline.
- Sessions 18–21: recent positioning comparison.
- Session 22: selected EOD positioning.
- Total: 22 valid completed F&O sessions, including the selected date.

Session identification must use a verified exchange trading calendar / actual report availability. A calendar day is not automatically a trading session. A future, non-trading, or not-yet-published EOD date cannot be declared complete.

## Multiple-strike analysis
For the selected underlying and expiry, process all strikes present in validated source reports (or a user-configured range), separately for CE and PE. Align observations by underlying, expiry, strike, option type, and session date. Aggregate only observed contract data. OI and OI change are positioning proxies; they do not identify the actual beneficial seller or prove that a particular participant wrote a particular strike.

## Source limitations
- Official EOD contract files may contain contract identity, prices, volume and OI; inspect each report schema before extracting.
- IV, bid/ask, and order-book depth are available only when the acquired official report genuinely contains those fields.
- Participant-wise OI is broad participant/category-level information, not strike-specific seller identity.
- Missing values remain missing. Model-derived values must be explicitly labelled as estimates and must never be presented as NSE-reported observations.

## Development
Initial project foundation is in `src/nifty_seller_intelligence/`. See `docs/DATA_POLICY.md` and `docs/WORKFLOW.md`.

No production downloader, broker integration, or live data substitution is enabled in this foundation.
