# Data and analysis workflow

1. User selects underlying (NIFTY), analysis date and expiry.
2. Resolve 22 valid completed F&O trading sessions ending at the selected date using a verified exchange calendar and actual report dates.
3. Identify official NSE reports required for each session. Do not guess filenames or endpoints; resolve them from official catalogue or licensed delivery documentation.
4. Acquire reports manually or through an approved NSE entitlement.
5. Preserve original files in the raw archive and create a provenance manifest.
6. Validate report date, archive integrity, checksum, structure and schema.
7. Extract contract data into a separate working/processed area.
8. Normalize underlying, expiry, strike and option type; preserve source values and units.
9. Build per-session strike datasets for CE and PE using only observed rows.
10. Check coverage of all 22 sessions and required fields before analysis.
11. Compare sessions 1–17, sessions 18–21 and session 22 across multiple strikes.
12. Compute support/resistance and potential seller-exit risk as evidence-based indicators, with missing metrics and confidence/coverage shown.
13. Generate a report containing source coverage, provenance, methodology, findings and limitations.

## Completion gate
Analysis status is COMPLETE only when all 22 target sessions have validated required core reports and required fields are present. Otherwise status is PARTIAL or BLOCKED with exact session/field failures. ML must not run under a false COMPLETE status.

## 17 + 4 + 1
- Long-term baseline: sessions 1–17.
- Recent comparison: sessions 18–21.
- Selected EOD: session 22.
- All three groups refer to completed sessions and cannot overlap.
- If the selected date EOD report has not been published, session 22 is pending and analysis cannot be complete.
