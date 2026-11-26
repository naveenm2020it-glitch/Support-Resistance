# Data policy and source integrity

## Allowed acquisition routes
1. Manual download by an authorised user from the official NSE report page.
2. A documented NSE-authorised data product/entitlement with approved delivery mechanism (for example, licensed SFTP/API if included in the subscription).

The public NSE website is not to be scraped. No automated browser scraping, Python requests, reverse-engineered endpoints, or cookie/session workarounds are implemented.

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
Each file record must include provider, report type, source URL, original filename, report/session date, download timestamp with timezone, byte size, SHA-256 checksum, archive/container type, fields found after schema inspection, validation status/messages, and optional entitlement reference (never credentials).

A file is not valid merely because it exists. Verify expected report date, archive integrity, expected structure, non-empty content and schema.

## Missing data
Use explicit states: `available`, `partial`, `unavailable`, `pending_eod`, `invalid`.
Do not fabricate fields, use current OI for historic dates, or use another date's data. Partial reports support only fields actually present.

## Bhavcopy limitation
UDiFF Common Bhavcopy is a report for traded contracts and EOD contract statistics. It must not be described as a full historical option-chain snapshot, and must not be assumed to contain every strike quote, IV, bid/ask, or order-book depth.
