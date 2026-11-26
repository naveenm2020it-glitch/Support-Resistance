# Data policy and source integrity

## Approved zero-subscription direction

The project has selected an EOD, file-based workflow using official NSE/NIFTY Indices reports only where the intended acquisition, storage and use are permitted. See [Free-source operating mode](FREE_SOURCE_MODE.md) and [Phase 2 source catalogue](PHASE2_SOURCE_CATALOGUE.md).

This is **not a live or automatically polled feed**. The zero-subscription technical target is a local importer for user-supplied EOD report files. NSE website Terms of Use prohibit systematic or automated data collection and restrict copying/storage and redistribution. The NSE research-data programme is strictly academic and non-commercial; it is not assumed to cover this trading/seller-intelligence project.

A public report listing or download control does not by itself prove the right to bulk-store the files, create a persistent training dataset, train ML models for trading signals, or display/redistribute derived outputs. Obtain written confirmation from NSE covering the intended source, acquisition method, retention, ML/trading purpose and dashboard use before creating or retaining a persistent NSE data archive. Do not treat an academic research-data approval as granted.

## Allowed acquisition routes

1. User-initiated manual download through an official NSE/NIFTY Indices report interface, only where the report is offered for download and the applicable terms permit the intended download.
2. A separately documented and authorised NSE/data-provider product or written permission with its approved delivery mechanism and usage licence.
3. User-supplied source files for which the user can establish sufficient provenance and a permitted basis for the intended use.

Do not implement or operate scheduled NSE website polling, scraping, Python requests against public report pages, reverse-engineered endpoints, browser automation, cookie/session workarounds or automated extraction. No live/streaming feed is claimed in the free-source mode.

## Zero-subscription capability boundary

The candidate free EOD source tier may contain contract-level OHLC/settlement, traded-contract activity, turnover, OI/change-in-OI, contract metadata, NIFTY index OHLC and broad participant aggregates. Every field remains conditional on actual report content and verified schema.

It does not provide evidence for historical order additions/modifications/cancellations, order-book sequence, bid/ask depth/replenishment, aggressor-side execution classification or individual seller identity. Intraday seller panic-exit detection is unavailable without valid intraday event data. Historical IV, bid/ask, depth and full historical option-chain snapshots must remain unavailable unless a permitted source file actually contains them.

## Source eligibility and usage restrictions

- NSE's Data List Under the NSE Data Sharing Policy for Research and Analysis identifies F&O contract-wise price-volume reports, contract specifications and archives of daily/monthly F&O reports in its first, freely available research-data basket for non-commercial academic research.
- The same policy separately identifies voluminous historical EOD data and historical tick-by-tick data above 2 GB as chargeable. Those datasets must not be described as free.
- NSE's Research Initiatives page states that data provided for research purposes is strictly limited to academic and non-commercial use and describes a formal Data Seeking Request process. This route is not assumed to cover the user's trading/seller-signal use.
- NSE's website Terms of Use, clauses 8–9, restrict copying/storage and redistribution without prior written permission (subject to the wording for content specifically available for download) and prohibit systematic or automated data collection, including scraping, data mining, extraction and harvesting.
- A downloadable public report is not a blanket permission for automated collection, bulk retention, trading-related ML training, redistribution or commercial use. Obtain written confirmation for the intended usage before persisting source files or using them to train trading/signal models.

## Original file preservation

After the data-use gate is passed, raw files are write-once inputs:
- Keep the original filename and bytes.
- Do not extract into the raw archive directory.
- Do not rename, edit, recompress, or delete an original report.
- Store processing outputs separately.
- A working copy may be created for extraction; raw original remains untouched.
- Do not put raw NSE reports in normal Git commits or Git history.

Suggested layout (only after storage permission and archive controls are in place):
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

For an authorised source file, each file record must include provider, report type, source URL/source reference, original filename, report/session date, download timestamp with timezone, byte size, SHA-256 checksum, archive/container type, fields found after schema inspection, validation status/messages, permission/entitlement basis (never credentials), and import timestamp.

A file is not valid merely because it exists. Verify expected report date, archive integrity, expected structure, non-empty content and schema.

## Missing data

Use explicit states: `available`, `partial`, `unavailable`, `pending_eod`, `invalid`.
Do not fabricate fields, use current OI for historic dates, or use another date's data. Partial reports support only fields actually present.

## Bhavcopy limitation

UDiFF Common Bhavcopy is a report for traded contracts and EOD contract statistics. It must not be described as a full historical option-chain snapshot, and must not be assumed to contain every strike quote, IV, bid/ask, or order-book depth.

## ML data governance

- Only train from source files whose acquisition, storage and intended ML/trading use have been confirmed as permitted.
- Preserve source hashes and row-level lineage.
- Use chronological train/validation/test splits and features available at the prediction timestamp.
- Define a target before training; prevent look-ahead and leakage.
- Do not treat 22 sessions as an adequate training corpus by default.
- Never represent model scores as seller identity, true short inventory or guaranteed future price behaviour.
