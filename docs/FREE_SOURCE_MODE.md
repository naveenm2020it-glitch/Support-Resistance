# Free-source operating mode — EOD reports and ML

**Decision date:** 2026-11-26  
**User decision:** No NSE Data & Analytics subscription; use free official source reports where permitted.  
**Status:** Selected as the zero-subscription design direction. Actual data use remains subject to NSE's applicable terms, eligibility and any required written approval.  
**Working branch:** `feature/nifty-seller-intelligence-foundation`

## 1. Executive decision

The project can be adapted to an **EOD (end-of-day), file-based research workflow** using official downloadable NSE reports and NIFTY Indices historical data, provided the user is entitled to use the downloaded data for this purpose.

This is not a real-time or intraday feed. The free/public report pages are not a permissible target for automated scraping or scheduled polling. The supported zero-subscription workflow is:

1. A user downloads a report through the official report UI, where download and use are permitted.
2. The user supplies the original downloaded file to the project's local import workflow.
3. The application validates the original date, report type, archive integrity, schema, required fields and checksum.
4. The application parses a working copy and preserves provenance for the original input.
5. The system creates EOD contract features and model-ready observations only from fields actually present.

No NSE public-page scraper, reverse-engineered endpoint, cookie/session workaround or automatic polling is allowed in this mode. Do not claim live feed delivery.

**Important legal eligibility gate:** NSE's current disclaimer states that no-cost research-oriented data up to an aggregate 2 GB is restricted to individuals from accredited academic institutions, recognised research organisations and think tanks, and excludes commercial, speculative or investigative purposes. The NSE Research Initiative page separately states that research data is strictly for academic and non-commercial use. A trading/signal product or speculative use cannot assume eligibility for this route. Obtain written permission/clarification from NSE before using the data for such a purpose. The public presence of a downloadable file is not by itself a licence to automate its collection, republish it or use it without restriction.

## 2. Official zero-subscription source inventory

| Source | Available information evidenced by NSE | Intended use in this project | Mode / limitation |
|---|---|---|---|
| [NSE All Reports — Derivatives](https://www.nseindia.com/all-reports-derivatives) | Daily F&O UDiFF Common Bhavcopy Final ZIP; F&O-MII contract-file variants; participant-wise OI and trading-volume reports; other F&O daily/monthly reports | Manual EOD download and local ingestion of contract observations and reference data | User-initiated download only. Current report UI; not a streaming endpoint. Validate each report and date. |
| [Historical Contract-wise Price Volume Data](https://www.nseindia.com/report-detail/fo_eq_security) | Contract-wise trade date, expiry, OHLC, LTP, settlement, contracts, turnover, OI, change in OI and underlying value are displayed in the official archive interface | Historical EOD features and contract-level cross-checks | Date/contract selection and export are user-driven. Sample, range and exact CSV schema still require verification. |
| [NIFTY 50 Historical Index Data](https://www.niftyindices.com/reports/historical-data) | Date-range historical index data with CSV output; NIFTY 50 historical series is stated by NSE Indices to begin on 3 July 1990 | Underlying EOD OHLC context and ML target/feature alignment | Manual official CSV download; inspect actual export headers, units and dates. |
| [NSE contract specifications / available contracts](https://www.nseindia.com/market-data/securities-information-contracts-available-for-trading) and daily F&O-MII master | Contract identity/specifications and daily contract master report family | Resolve underlying, expiry, strike, CE/PE, lot/tick and contract validity by date | Use the exact applicable date's master. Do not apply today's master retroactively. |
| [F&O participant-wise OI / trading volume](https://www.nseindia.com/all-reports-derivatives) | Broad participant-category aggregates | Optional market-wide positioning context | Not strike-wise and not an identified seller's position. Exact export fields require sample validation. |
| [NSE Option Chain](https://www.nseindia.com/option-chain) | Public option-chain UI with expiry/strike selections and CSV download | At most, a manually captured prospective snapshot if separately permitted | No historical analysis-date selector was identified. Cannot backfill historic chains from this page or bhavcopy. Manual capture does not authorise automated polling. |

NSE's Data List Under the NSE Data Sharing Policy for Research and Analysis places Equity Derivatives contract-wise price-volume data, contract specifications, daily/monthly report archives (including bhavcopy, client-category reports, market activity, settlement prices and OI) in its research data list.

The same list has a separate **First Basket (Voluminous data; >2 GB; Chargeable)** that includes historical end-of-day data and historical tick-by-tick trades/orders without client and broker information. The footnote describes historical tick-by-tick data as relating to the six months before the date of request or earlier. This is not a free entitlement; it is recorded here to avoid confusing the standard paid O&T product with the research-policy classification.

## 3. What the free EOD data can support

Subject to source schema and rights validation, the EOD dataset can support:

- Multi-strike CE/PE contract OHLC and settlement analysis.
- EOD traded-contract counts/volume and turnover.
- EOD OI and change-in-OI analysis where actually supplied.
- Contract expiry, strike and option-type alignment using the matching daily contract master.
- NIFTY 50 index OHLC comparison.
- Participant-category aggregate OI/volume context where the report supplies it.
- Rolling 17 + 4 + 1 EOD comparison for each selected analysis date.
- EOD support/resistance zone research from historical contract activity and price/underlying behaviour.
- Research ML using a sufficiently long, validated EOD history and labels derived from future observations without look-ahead.

The 22-session window is an **analysis feature window**, not automatically a sufficient ML training corpus. ML must use a much longer eligible history, chronological train/validation/test splits, a defined prediction target, out-of-sample evaluation and no future-data leakage.

## 4. What the free EOD data cannot prove

Do not infer or claim the following from EOD bhavcopy, contract-wise price-volume, index or participant aggregates:

- Intraday order additions, modifications or cancellations.
- Order-book sequence, best bid/ask history or depth replenishment.
- Buyer-initiated versus seller-initiated trade volume.
- Passive sell-side absorption or seller queue persistence.
- Individual trader, broker or seller identity, inventory, intent or entry price.
- Actual short opening/closing or panic exit from OI change alone.
- Historical IV, historical bid/ask, full option-chain snapshots or intraday depth unless a source file actually contains those fields.

Accordingly, the zero-subscription mode must disable or label unavailable: order-flow classification, order-book replay, passive seller replenishment, intraday seller panic exit, and historical IV/quote/depth metrics. It may report qualified EOD positioning proxies, not order-flow facts.

## 5. Data-sharing and website-use restrictions

Official NSE references reviewed on 2026-11-26:

- NSE's [Data List Under the NSE Data Sharing Policy](https://nsearchives.nseindia.com/web/sites/default/files/inline-files/Data%20list%20under%20NSE%20Data%20Sharing%20Policy%20for%20Research%20and%20Analysis_20250728.pdf) identifies the first-basket research reports and the separate >2 GB chargeable voluminous-data category.
- NSE's [Disclaimer](https://www.nseindia.com/static/nse-disclaimer), updated 6 November 2026, says no-cost research-oriented data up to the aggregate 2 GB threshold is limited to eligible accredited academic institutions, recognised research organisations and think tanks, and excludes commercial, speculative or investigative purposes. It also allows NSE to require an NDA and periodic usage/progress reports.
- NSE's [Research Initiatives](https://www.nseindia.com/static/research/research-initiatives) page says research data is strictly limited to academic and non-commercial use and provides the research request process at `nseri@nse.co.in`.
- NSE's [Terms of Use](https://www.nseindia.com/static/nse-terms-of-use) prohibit systematic or automated data collection (including scraping, data mining, extraction and harvesting) on or in relation to its website/mobile application. The terms also restrict copying, storage, redistribution and certain uses of website content.

The project must not assume that free web visibility grants permission for scheduled automated downloads, republishing, redistribution, commercial use, speculative use or ML training for a trading product. If the intended use is a trading/signal system, ask NSE for written confirmation of the permitted data source and use before ingesting their reports into that workflow.

For an eligible academic/non-commercial research project, the NSE research page provides a Data Seeking Request Form and the contact `nseri@nse.co.in`. Approval/eligibility is not assumed.

## 6. Revised zero-subscription pipeline

| Layer | Zero-subscription mode |
|---|---|
| Source acquisition | User-initiated manual download through official permitted UI |
| Input interface | Local file import; no NSE website polling or scraper |
| Raw source | Preserve exact uploaded bytes, original filename and source URL/reference |
| Validation | Verify file/date/report-family, container integrity, schema version, field units, row counts and SHA-256 |
| Normalisation | Parse only verified fields; maintain missing values as missing |
| Dataset | EOD contract observations + NIFTY index observations + optional participant aggregates |
| Analysis | Multi-strike EOD CE/PE, OI/volume/price comparisons and qualified S/R zones |
| ML | Offline EOD feature/label dataset after longer-history eligibility gate |
| Excluded features | Live feed, tick/order reconstruction, aggressor flow, quote/depth replay and intraday panic exit |

Do not proceed to ML until the dataset has enough valid sessions/contracts, consistent contract mapping, a defined target and a documented rights decision. If a user's upload does not contain a report or field, record it as unavailable; do not fill it from another date, another provider or live data.

## 7. Phase 2 acceptance under this choice

- [x] User selected the no-subscription/free-source direction.
- [x] Official NSE EOD report families and official index history identified.
- [x] Distinction between public EOD reports, research-only access and >2 GB chargeable historical datasets documented.
- [x] No-scraping restriction and research-use eligibility gate documented.
- [x] EOD-only ML and analysis capabilities distinguished from unavailable order-flow features.
- [ ] Confirm that the intended project use is permitted under NSE terms; if academic research, confirm eligible institution/status and receive required approval.
- [ ] Obtain a permitted sample of each proposed EOD source and inspect actual field schemas, date coverage, units and null semantics.
- [ ] Decide which EOD reports are mandatory versus supplementary based on the verified samples.
- [ ] Verify the available historical range and size for the selected report families.
- [ ] Update the canonical requirements and data dictionary with the approved EOD-only field map after samples are inspected.

**Phase status remains IN PROGRESS.** The design direction is selected, but legal eligibility and actual sample/schema validation have not passed.

## 8. References

1. [NSE All Reports — Derivatives](https://www.nseindia.com/all-reports-derivatives)
2. [NSE Historical Contract-wise Price Volume Data](https://www.nseindia.com/report-detail/fo_eq_security)
3. [NIFTY Indices — Historical Data](https://www.niftyindices.com/reports/historical-data)
4. [NSE Securities/Contracts Available for Trading](https://www.nseindia.com/market-data/securities-information-contracts-available-for-trading)
5. [NSE Data List Under the NSE Data Sharing Policy for Research and Analysis (28 July 2025)](https://nsearchives.nseindia.com/web/sites/default/files/inline-files/Data%20list%20under%20NSE%20Data%20Sharing%20Policy%20for%20Research%20and%20Analysis_20250728.pdf)
6. [NSE Disclaimer](https://www.nseindia.com/static/nse-disclaimer)
7. [NSE Research Initiatives](https://www.nseindia.com/static/research/research-initiatives)
8. [NSE Terms of Use](https://www.nseindia.com/static/nse-terms-of-use)
