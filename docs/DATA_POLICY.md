# Data policy and source integrity

## Approved zero-subscription direction

The project has selected an EOD, file-based workflow using official NSE reports where the intended use is permitted. See [Free-source operating mode](FREE_SOURCE_MODE.md) and [Phase 2 source catalogue](PHASE2_SOURCE_CATALOGUE.md).

This decision does **not** authorise automated downloads from NSE's website or establish a right to use NSE research data for a trading/speculative product. NSE's current website terms prohibit systematic or automated data collection. NSE's no-cost research-data route is restricted to eligible academic/research institutions and non-commercial purposes; it excludes commercial and speculative uses. Obtain written confirmation from NSE where the intended use is unclear.

## Allowed acquisition routes

1. User-initiated manual download through an official NSE/NIFTY Indices report UI, only where the report is offered for download and the user's intended storage/use complies with the applicable terms.
2. A separately documented and authorised NSE/data-provider product or research-data approval with its approved delivery mechanism and usage licence.
3. User-supplied source files with sufficient provenance and proof of permitted use.

Do not implement or operate scheduled NSE website polling, scraping, Python requests against public report pages, reverse-engineered endpoints, browser automation, cookie/session workarounds or automated extraction. No live/streaming feed is claimed in the free-source mode.

## Zero-subscription capability boundary

The free-source mode is EOD only. It may support contract-level OHLC/settlement, traded-contract activity, turnover, OI/change-in-OI where present, contract metadata, NIFTY index OHLC and broad participant aggregates where those fields are present in a verified source file.

It does not provide evidence for historical order additions/modifications/cancellations, order-book depth/replenishment, aggressor-side execution classification, individual seller identity, or intraday seller panic exit. Historical IV, bid/ask, depth and full historical option-chain snapshots must remain unavailable unless a permitted source file actually contains them.

## Source eligibility and usage restrictions

- NSE's current disclaimer (updated 2026-11-06) states that no-cost research-oriented data under the aggregate 2 GB threshold is restricted to individuals from accredited academic institutions, recognised research organisations and think tanks, and excludes commercial, speculative or investigative purposes. NSE may require an NDA and periodic research progress/data-use reports.
- NSE's Data List Under the NSE Data Sharing Policy lists equity-derivatives contract-wise price-volume reports and daily/monthly report archives in the research data basket. It separately lists historical EOD and anonymised tick-by-tick data in the **voluminous >2 GB chargeable** basket.
- NSE's website Terms of Use prohibit systematic or automated data collection, including scraping, data mining, extraction and harvesting. Downloadable public reports must not be treated as permission to automate collection or to redistribute/reuse data outside the applicable terms.
- A personal trading, speculative or commercial model must not claim eligibility for the no-cost academic research route. Ask NSE for written confirmation of the specific permitted data source and use.

## Original file preservation

Raw files are write-once inputs:
- Keep the original filename and bytes.
- Do not extract into the raw archive directory.
- Do not rename, edit, recompress, or delete an original report.
- Store processing outputs separately.
- A working copy may be created for extraction; raw original remains untouched.

Suggested layout:
```
data/
  raw/
    nse/
      YYYY-MM-DD/
        <original-NSE-filename>.zip
        manifest.json
  working/
    YYYY-MM-DD/
  processed/
    YYYY-MM-DD/
  reports/
```

## Provenance manifest

Each file record must include provider, report type, source URL/source reference, original filename, report/session date, download timestamp with timezone, byte size, SHA-256 checksum, archive/container type, fields found after schema inspection, validation status/messages, permission/entitlement basis where applicable (never credentials), and import timestamp.

A file is not valid merely because it exists. Verify expected report date, archive integrity, expected structure, non-empty content and schema.

## Missing data

Use explicit states: `available`, `partial`, `unavailable`, `pending_eod`, `invalid`.
Do not fabricate fields, use current OI for historic dates, or use another date's data. Partial reports support only fields actually present.

## Bhavcopy limitation

UDiFF Common Bhavcopy is a report for traded contracts and EOD contract statistics. It must not be described as a full historical option-chain snapshot, and must not be assumed to contain every strike quote, IV, bid/ask, or order-book depth.

## ML data governance

- Only train from source files whose acquisition and intended use have been verified as permitted.
- Preserve source hashes and row-level lineage.
- Use chronological train/validation/test splits and features available at the prediction timestamp.
- Define a target before training; prevent look-ahead and leakage.
- Do not treat 22 sessions as an adequate training corpus by default.
- Never represent model scores as seller identity, true short inventory or guaranteed future price behaviour.
