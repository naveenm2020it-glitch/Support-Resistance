# Phase 2 — NSE Source Catalogue and Access Verification

**Status:** IN PROGRESS — official source discovery completed for the first pass; entitlement and sample-data verification remain open.  
**Research date:** 2026-11-26  
**Repository:** `naveenm2020it-glitch/Support-Resistance`  
**Branch:** `feature/nifty-seller-intelligence-foundation`  
**Requirements:** [REQUIREMENTS.md](REQUIREMENTS.md)  
**Roadmap:** [IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md)  
**Progress register:** [PHASE_PROGRESS.md](PHASE_PROGRESS.md)

## 1. Decision summary

The official NSE material identifies the following source families:

- Daily F&O UDiFF Common Bhavcopy Final (ZIP).
- Daily F&O-MII contract master files (GZ), including NSE-exclusive and NSE/BSE-exclusive variants; a separate spread contract file is also listed.
- NIFTY 50 historical index data with a CSV download path.
- Historical contract-wise price/volume data.
- F&O participant-wise OI and participant-wise trading-volume reports.
- Separate EOD and historical F&O order/trade data products offered through NSE Data & Analytics (NDAL).

The F&O order/trade product is the critical dependency for the planned event-flow analysis. NSE's published product page says EOD order/trade data is delivered through SFTP and historical order/trade data through an online platform. The current specification describes Trim and Full variants and their different availability delays. The repository does **not** contain evidence of an active NDAL subscription, approved credentials, a tested delivery channel, or downloaded licensed sample data.

The public NSE option-chain page provides a current contract/expiry selection and CSV download. The reviewed page does not expose a historical analysis-date control. This is not proof that NDAL has no separately licensed historical snapshot product; that question remains open and must be put to NDAL.

**Phase 2 is not complete.** No paid subscription, entitlement, sample file, report-by-date coverage, schema parser, or source integration has been tested in this first-pass research. Do not implement an automated public-page downloader. Manual downloads from official report pages may be used only through their permitted user workflow; automated collection must wait for an explicitly authorised access method.

## 2. Source and access catalogue

Status vocabulary used here:

- **IDENTIFIED:** an official NSE page or specification describes/lists the source.
- **PUBLIC UI:** an official report/download UI was found; this does not by itself grant automated-access permission.
- **SUBSCRIPTION:** NDAL describes the product as a subscription/data-vending product; project entitlement has not been verified.
- **UNRESOLVED:** the reviewed official material does not settle the exact historical coverage, fields, entitlement, or delivery needed by this project.
- **NOT SAMPLE-VERIFIED:** no permitted sample has yet been obtained and checked by this project.

| Source family | Official evidence and observed scope | Candidate use / requirement | Access decision and unresolved items |
|---|---|---|---|
| F&O UDiFF Common Bhavcopy Final (ZIP) | Listed on NSE All Reports – Derivatives as **F&O - UDiFF Common Bhavcopy Final (zip)**. NSE states the legacy F&O Bhavcopy CSV/Common Bhavcopy formats were discontinued from 2024-07-08 and directs users to UDiFF Common Bhavcopy Final. | Candidate mandatory daily EOD contract observation source. | **IDENTIFIED / PUBLIC UI / NOT SAMPLE-VERIFIED.** Must obtain the current UDiFF catalogue and F&O Bhavcopy file-format workbook, inspect the exact version and fields, and compare it with the project's required contract fields. Do not infer the UDiFF schema from the discontinued CSV format or an older market-feed format. Historical date coverage and any permitted automation method remain to be verified. |
| F&O-MII Contract File (GZ) — NSE Exclusive | NSE lists the NSE-exclusive F&O-MII Contract File. NSE circular NSE/MSD/60315 specifies the daily file naming pattern `NSE_FO_contract_ddmmyyyy.csv.gz`. NSE Master Data specification v1.6 describes the F&O contract master record. | Candidate mandatory daily contract-master/reference source for token-to-contract mapping and correct expiry/strike/option-type interpretation. | **IDENTIFIED / PUBLIC UI / NOT SAMPLE-VERIFIED.** Confirm the correct market/segment variant for each record and the schema version effective on each report date. Do not assume current contract tokens or lot sizes apply retrospectively. |
| F&O-MII Contract File (GZ) — NSE and BSE Exclusive | A separate NSE/BSE-exclusive contract master variant is listed by NSE. | Reference only where the source product/segment requires it; not automatically interchangeable with the NSE-exclusive file. | **IDENTIFIED / NOT SAMPLE-VERIFIED.** Confirm whether the selected NIFTY F&O product requires this variant, the exact field semantics, and whether the NSE-exclusive file alone is sufficient. |
| F&O-MII Spread Contract File (GZ) | A separate F&O-MII spread contract file is listed. | Optional for the initial single-leg NIFTY CE/PE analysis unless spread instruments are explicitly brought into scope. | **IDENTIFIED / NOT SAMPLE-VERIFIED.** Do not include spread records in single-leg CE/PE calculations without an explicit mapping decision. |
| NIFTY 50 historical index data (CSV) | NSE's historical index data page exposes date-range selection and CSV download. NSE Indices FAQ states NIFTY 50 historical data is available from 1990-07-03. The NSE All Reports index example displays Date, Open, High, Low, Close, Shares Traded and Turnover. | Candidate mandatory underlying/index reference series for daily context and cross-checks. | **IDENTIFIED / PUBLIC UI / NOT SAMPLE-VERIFIED.** Confirm the exact CSV headers, units, timezone/date conventions, correction policy and requested date range from an actual official export. The historical availability statement is not a substitute for validating each requested session. |
| Historical contract-wise price/volume data | NSE All Reports – Derivatives exposes Historical Contract-wise Price Volume Data with contract selection and From/To controls. Displayed fields include Trade Date, Open Price, High Price, Low Price, Close Price, Daily Settlement Price, Open Interest, No. of Contracts and Value (lakhs). | Supplementary cross-check for selected contract EOD values; useful when reconciling UDiFF field meanings. | **IDENTIFIED / PUBLIC UI / NOT SAMPLE-VERIFIED.** Confirm export schema, date coverage, option contract support and units. Do not silently use it to replace a missing mandatory UDiFF file. |
| F&O Participant-wise Open Interest (CSV) | Listed in NSE All Reports – Derivatives. | Optional broad participant-category context. | **IDENTIFIED / PUBLIC UI / NOT SAMPLE-VERIFIED.** Exact columns, participant buckets, publication time and historical coverage must be verified from an official export. This is not strike-wise trader identity or an individual seller's position. |
| F&O Participant-wise Trading Volumes (CSV) | Listed in NSE All Reports – Derivatives. | Optional broad participant-category volume context. | **IDENTIFIED / PUBLIC UI / NOT SAMPLE-VERIFIED.** Exact columns, participant buckets and historical coverage remain open. Do not infer per-strike order flow from this report. |
| Historical F&O option-chain snapshots / IV / quotes / depth | NSE's public Option Chain page exposes symbol, expiry and strike selection and a CSV download, but no historical analysis-date selector. NSE's FPI FAQ lists F&O Level 1 (best bid/ask) and Level 2 (best five bid/ask) market-data classes; its table does not list F&O Level 3 or MTBT. The FAQ does not provide a product-specific historical snapshot file specification, cadence or date range. | Desired only if a licensed historical source supplies time-stamped contract snapshots/quotes; not derivable from EOD bhavcopy. | **PARTIALLY IDENTIFIED / UNRESOLVED.** Ask NDAL whether F&O L1/L2 are available as historical time-series or snapshots, earliest/latest dates, cadence, IV/OI fields, delivery and exact pricing. Do not infer that the public Option Chain CSV is historical. Until a real historical archive is evidenced, mark these fields unavailable. |
| F&O Order Data — Trim | NSE Historical Order & Trade specification v1.19 describes F&O Trim order data as all order ticks in a fixed-length record format, available EOD. Described fields include order number, event time, buy/sell side, activity type (entry, modification, cancellation), contract dimensions and order quantity/price fields. The spec defines its jiffies timestamp scale (65,536 jiffies per second from 1980-01-01 midnight). | Core input to order-event sequence and order-book reconstruction, subject to completeness and entitlement. | **SUBSCRIPTION / NOT SAMPLE-VERIFIED.** The EOD order/trade product page says EOD delivery is via SFTP. Obtain written confirmation of F&O Trim entitlement, exact files/streams, history span, event completeness, sequencing guarantees, field units and delivery SLA. Decode timestamps only against the applicable specification version. |
| F&O Trade Data — Trim | NSE Historical Order & Trade specification v1.19 describes F&O Trim trade data as all trade ticks in a fixed-length record format, available EOD. Fields include trade reference/number, event time, contract dimensions, execution price/quantity and buy-order/sell-order references. | Core input to executions and linking executions to referenced order IDs. | **SUBSCRIPTION / NOT SAMPLE-VERIFIED.** Obtain an entitled sample and prove references resolve against the corresponding order-event files. Trade-to-order references alone do not prove aggressor side, participant identity, or open/close intent. |
| F&O Order Data — Full | The v1.19 specification describes Full F&O orders as all order ticks with additional fields such as limit/trigger prices and order flags. The published specification indicates a 90-calendar-day availability delay for Full order data. | Optional richer historical order detail / backfill if needed. | **SUBSCRIPTION / NOT SAMPLE-VERIFIED.** Confirm whether this delay is measured from trade date, whether it applies to the subscribed F&O product, and the actual earliest available history. |
| F&O Trade Data — Full | The v1.19 specification describes Full F&O trades with additional fields (including further participant/order metadata). The published specification indicates a 30-calendar-day availability delay for Full trade data. | Optional richer trade detail / backfill if needed. | **SUBSCRIPTION / NOT SAMPLE-VERIFIED.** Confirm availability delay, historical date range, F&O fields, order references, usage restrictions and sample. Additional client/proprietary classification is not a real-world trader identity. |
| Historical Trade Data (separate product) | NSE FPI data FAQ distinguishes Historical Order & Trade data (states F&O availability from January 2008) from Historical Trade Data (states availability from January 1994). | Potential long-history trade-only corroboration/backfill, not a replacement for order events. | **SUBSCRIPTION / SCOPE TO CONFIRM.** Treat the dates as published FAQ guidance, not a binding guarantee for a particular subscription. Confirm actual F&O product coverage, fields, granularity and earliest date with NDAL. |

## 3. What the specifications establish — and what they do not

### 3.1 UDiFF and EOD contract observations

NSE's report page explicitly directs users away from the discontinued legacy F&O Bhavcopy CSV/Common Bhavcopy and to the UDiFF Common Bhavcopy Final ZIP from 2024-07-08. NSE's forms/formats page lists UDiFF catalogue v4.0 and a UDiFF trade/Bhavcopy format workbook.

The workbook is the authority for exact UDiFF field names, types, scale factors, null codes and record meanings. It was linked on the official NSE forms/formats page, but this research pass could not retrieve the XLSX through the available source fetch. **No UDiFF parser schema is approved yet.** Obtain and review the actual current workbook and an authorised/manual sample before designing the parser. In particular, do not lift field names from a pre-UDiFF file.

### 3.2 Contract master

NSE circular NSE/MSD/60315 states the daily F&O MII contract master naming pattern and publication location. NSE Master Data v1.6 describes fields including market/version, token and asset token, instrument, symbol, series, expiry timestamp (based on midnight 1980-01-01), strike, option type, category, CA level, and additional trading/contract attributes.

The contract master must be versioned by report date. Validate lot size, tick size, expiry, strike scaling, option-type codes, active/tradable state and token identity against the exact daily master. A later master must not be used to rewrite an earlier contract without an explicit, documented rule.

### 3.3 Contract-wise EOD history and index series

The contract-wise price/volume page visibly lists OHLC, settlement price, open interest, contract count and value. It is a separate official report, useful for checking values and interpreting UDiFF data, but the exact exported file still needs a sample review.

The NIFTY 50 index history UI provides date-range and CSV download. NSE Indices FAQ states the NIFTY 50 historical series begins on 1990-07-03. Inspect the exported file for exact columns and units before mapping it to the application. Do not confuse index history with an options-chain archive.

### 3.4 Historical quote/depth availability

NSE's FPI FAQ states that NSE Data & Analytics supplies historical data feeds and its market-data level table marks F&O as supported for Level 1 (best bid/ask) and Level 2 (best five bid/ask). The same table does not mark F&O for Level 3 (best 20) or MTBT. This is useful evidence that F&O quote/depth data products exist at least at specified market-data levels, but the FAQ does not identify the historical product format, sampling cadence, coverage window, fields beyond level depth, or whether the required historical NIFTY option contracts are included. The public Option Chain UI itself still has no historical-date selector. Obtain the current F&O historical quote/snapshot specification and an authorised sample before marking option-chain/IV/depth coverage available.

### 3.5 Order and trade event data

The current specification reviewed is **NSE Historical Order & Trade Data v1.19 (2026-08-03)**. It distinguishes Trim and Full datasets. The Trim descriptions include order-entry, modification and cancellation events and trade events; the trade records contain buy- and sell-order references. The specification's timestamp representation requires an explicit, tested conversion; do not interpret jiffies as Unix seconds.

The specification indicates EOD delivery for the Trim products and 90 calendar days for Full orders / 30 calendar days for Full trades. NSE's subscription page says EOD Order & Trade is delivered through SFTP and Historical Order & Trade through its online platform. NSE's FPI FAQ states historical F&O Order & Trade data is available from January 2008; it separately states Historical Trade Data from January 1994. These dates must be confirmed with NDAL for the actual product, terms and delivery arrangement.

**Feasibility is not yet proven.** A specification showing order IDs and trade references does not establish that all necessary events for a complete order-book replay are present in the purchased product, correctly sequenced across all files, or available for the same dates. That requires a permitted sample and explicit completeness tests.

## 4. Candidate report coverage matrix for the 22-session dataset

Phase 3 will identify the exact 22 F&O sessions. This Phase 2 matrix defines what must be checked for each selected session; it does not claim that any particular date has already been downloaded or validated.

| Report family | Candidate coverage class | Per-session check | Current evidence state |
|---|---|---|---|
| UDiFF F&O Common Bhavcopy Final | Core EOD | File exists for session date; archive/member data date matches; checksum and current schema pass; required contract rows/fields present | Source identified; date coverage and sample validation open |
| F&O-MII Contract File | Core reference | Correct daily master exists; schema/version valid; option contracts resolve to exact underlying/expiry/strike/type | Source identified; sample and date coverage open |
| NIFTY 50 index history | Core underlying reference | Exact session date present; official export schema/units validated | Source identified; sample validation open |
| Contract-wise price/volume | Supplementary cross-check | Selected contract/date export available; values reconciled with primary EOD source | Source identified; sample validation open |
| Participant-wise OI | Optional context | Date present; category and unit semantics validated | Source identified; schema/date coverage open |
| Participant-wise volume | Optional context | Date present; category and unit semantics validated | Source identified; schema/date coverage open |
| Historical option-chain snapshots / F&O quote depth | Optional until licensing/spec confirms required status | A genuine timestamped historical file exists for the date/contract; exact L1/L2 fields and sampling cadence meet the agreed analysis requirement | F&O L1/L2 product classes identified in FAQ; historical format/date coverage/sample still open |
| F&O Trim order events | Required for event-flow/order-book features; not required to falsely label EOD-only metrics | Order stream exists for date; all applicable streams reconciled; event ordering and event types validate | Licensed access/sample not established |
| F&O Trim trade events | Required for execution flow and order linkage | Trade stream exists for date; order references resolve or are explicitly classified unresolved | Licensed access/sample not established |
| F&O Full order/trade | Optional/backfill | Date falls within actual product's available range and delay; schema matches selected version | Subscription and coverage not established |

A session/report combination may be marked Available only after the actual file has been obtained from the authorised source and passed integrity, date, format, schema and required-field validation. The page listing a report is not proof of file availability for every historical date.

## 5. Access, entitlement and compliance decision

| Data group | What is confirmed | Project decision |
|---|---|---|
| NSE All Reports public EOD reports | Official report listings and manual user interfaces exist for the report families above. | Manual access only through official permitted UI for feasibility and sample review. No automated website scraping or scripted public-page requests. |
| NDAL EOD Order & Trade | NSE says delivery is through SFTP; subscription inquiry is directed to marketdata@nse.co.in / +91-22-2659 8385. | No automated access until the actual F&O entitlement, agreement, credentials and delivery details are confirmed. |
| NDAL Historical Order & Trade | NSE says historical O&T is supplied through an online platform. | No download or integration until subscription, historical coverage, terms and sample are confirmed. |
| Historical F&O option-chain / IV / quotes / depth | Public option-chain page has no historical-date selector; FPI FAQ lists F&O market-data classes L1 and L2, but does not document the exact historical snapshot product/file, cadence or coverage. | Treat historical coverage as unresolved until NDAL confirms the specific historical product, fields, cadence, date span and sample. |
| Permanent archive / Ubuntu | Not in scope of source-verification evidence; server access and archive have not been tested in Phase 2. | Do not claim archive connectivity or create a data archive in this phase. Phase 6 owns archive implementation. |

The NSE Data & Analytics domestic tariff sheet effective 2026-04-01 (page updated 2026-04-24) publishes the following F&O rates. These are published list prices, not a project-specific quote; the applicable usage category, site scope, tax and written licence still need confirmation.

| Product | Published domestic F&O fee | Important distinction |
|---|---:|---|
| EOD Order & Trade Data | ₹12,50,000 per annum / enterprise single site | EOD O&T product; delivered by SFTP according to the product page. |
| Historical Order & Trade Data | ₹12,50,000 per annum / single site; ₹31,30,000 / global multiple sites | Includes order and trade history; the relevant standard product for the historical event-flow requirement. |
| Historical Trade Data only | ₹1,10,000 per annum / single site | Trade-only product; not a substitute for order messages, cancellations/modifications or order-book reconstruction. |
| Educational institution / student-researcher Historical Order & Trade Data | ₹6,25,000 per annum for F&O | Separate eligibility category; do not assume an individual/private project qualifies. NSE notes restrictions and delay conditions for near-real-time data in the educational category. |
| Educational institution / student-researcher Historical Trade Data only | ₹22,000 per annum for F&O | Trade-only; does not satisfy order-event reconstruction needs. |

Published prices exclude applicable taxes/levies. The tariff also specifies an additional 50% of the product fixed fee for each additional delivery channel. The standard and educational tariffs are different categories; written NDAL confirmation is required before relying on the educational rate. The separate 1-minute/5-minute snapshot product fees are not evidence that a historical options-chain archive is included.

## 6. NDAL questions that must be answered before source integration

The following request should be sent through the official subscription contact. This document records the questions; **no enquiry has been sent on the user's behalf**.

1. Which F&O products are available for this use: Trim EOD order events, Trim EOD trades, Full historical orders, Full historical trades, historical trade-only data, and historical market-data/quote snapshots?
2. What are the exact earliest and latest available F&O dates for each product, including any instrument/contract exclusions and the practical delay from trade date?
3. Are the Trim order and trade datasets complete for all order additions, modifications, cancellations and executions needed for replay? Are they delivered as one stream or multiple files/streams, and what are the sequencing, partitioning and end-of-day completion guarantees?
4. Which identifiers link trade records to order records? Are order references globally unique or date/segment scoped? How are corrections, busts, duplicate events and late events represented?
5. What are the exact units and encodings for timestamps, prices, quantities, lot sizes, strike prices, option types and contract identifiers for the currently supplied F&O files?
6. Can NDAL provide permitted F&O Trim and Full order/trade sample files, the current technical specification, and a field-by-field schema/version history for parser validation?
7. Does NDAL offer historical timestamped F&O option-chain or quote/depth archives? For each product, confirm frequency, best bid/ask or depth levels, OI, IV/Greeks, historical date range and file format.
8. What are the current domestic subscription prices, minimum subscription period, per-segment charges, one-time charges, data delivery charges and payment process for the required F&O products?
9. Is delivery via SFTP, NDAL downloader/online platform, cloud/AWS, or a combination? Confirm network/IP allowlisting, credential lifecycle, file naming, publication SLA, retry procedure and retention.
10. What usage, storage, derived-data and redistribution restrictions apply to this research application and its eventual dashboard?

Official contact published on NSE's EOD/Historical Data page: **marketdata@nse.co.in**, **+91-22-2659 8385**.

## 7. Acceptance checklist and blockers

| Phase 2 acceptance item | Status | Evidence / remaining work |
|---|---|---|
| Official source identified for each planned report family | PARTIAL PASS | Official listings/specifications found for EOD bhavcopy, MII contract masters, NIFTY index, contract-wise price/volume, participant OI/volume and order/trade. Historical option-chain archive still unresolved. |
| Authorised access method documented | PARTIAL PASS | NDAL documents SFTP for EOD O&T and online platform for historical O&T; public EOD report pages identified. Project-specific permissions/terms are not verified. |
| Historical coverage documented | PARTIAL PASS | NSE FPI FAQ states historical F&O O&T since Jan 2008 and historical trade data since Jan 1994; current subscribed product's actual availability still needs written confirmation. |
| Required and optional report families distinguished | PASS — first-pass | Coverage matrix above is the proposed classification; final mandatory status depends on the UDiFF schema and NDAL product decision. |
| Exact source fields, units and schema versions verified | IN PROGRESS | Contract-master and O&T specifications reviewed; UDiFF v4 workbook and actual EOD exports remain to be inspected. |
| Permitted source samples received and validated | NOT STARTED | No sample files have been obtained or processed. |
| Order/trade linkage and order-book reconstruction feasibility proven | NOT STARTED | Requires entitled samples and tests for complete event coverage, ordering, timestamps, references and reconstruction. |
| Project entitlement and commercial terms confirmed | PARTIAL / BLOCKED | Current domestic tariff is documented above, but there is no evidence of an active NDAL F&O subscription, approved agreement, project quote, confirmed usage category, credentials or sample. |
| Historical chain/quote/depth coverage confirmed | PARTIAL / BLOCKED | F&O L1/L2 levels are identified in the NSE FAQ, but the historical product format, snapshot cadence, date span, fields and authorised sample remain unverified. |

### Phase 2 current blockers

- **B1 — NDAL entitlement:** subscription status and applicable terms for F&O order/trade data are unknown.
- **B2 — Official sample/schema validation:** UDiFF v4 workbook and actual representative data files need review; current schemas must not be guessed.
- **B3 — Historical chain/quote archive:** public option-chain UI does not prove a historical archive; NDAL product coverage needs confirmation.
- **B4 — Event reconstruction:** trade-to-order linkage, full event completeness and sequence integrity have not been demonstrated on a permitted sample.
- **B5 — Target coverage:** exact 22 dates will be established by Phase 3; this document does not claim date-specific report availability.

### Next Phase 2 actions

1. Obtain the latest official UDiFF catalogue and F&O Bhavcopy format workbook and a permitted representative EOD ZIP; inspect headers, types, scale factors, null semantics and contract identifiers.
2. Obtain a representative MII F&O contract master and a NIFTY 50 historical index CSV through the official download workflow; verify real fields and units without changing source bytes.
3. Confirm whether the user/project already has an NDAL F&O subscription. If not, obtain the official current tariff/quotation and terms before any licensed-data access is attempted.
4. Request permitted F&O Trim order/trade samples and test event ordering, jiffies conversion, order references, trade linkage and missing-stream detection.
5. Obtain written NDAL confirmation on historical option-chain/quote/depth availability and the actual historical date range.
6. Update this catalogue and the progress register with sample hashes, source dates, exact schema versions, entitlement evidence and test results. Do not mark Phase 2 complete until its acceptance checklist passes.

## 8. Official NSE references

All references below are official NSE / NSE Data & Analytics sources. They establish product descriptions and public listings, not the user's project-specific entitlement.

1. [NSE All Reports — Derivatives](https://betanseapi.nseindia.com/all-reports-derivatives) — UDiFF Common Bhavcopy Final, F&O-MII contract files, participant OI/volume and historical contract-wise price-volume reports; also identifies the discontinued legacy Bhavcopy reports.
2. [NSE Forms and Formats — Members](https://www.nseindia.in/static/resources/forms-formats-members) — UDiFF catalogue v4.0 and UDiFF trade/Bhavcopy format workbook links.
3. [NSE circular NSE/MSD/60315 (19 January 2024)](https://nsearchives.nseindia.com/content/circulars/MSD60315.pdf) — daily F&O MII contract master file naming and publication.
4. [NSE Master Data Specification v1.6 (5 May 2025)](https://nsearchives.nseindia.com/web/sites/default/files/inline-files/NSE-Masters%20Data-v1.6.pdf) — F&O contract master record layout and fields.
5. [NSE Historical Index Data](https://betanseapi.nseindia.com/reports-indices-historical-index-data) — date-range selection and CSV download.
6. [NSE Indices FAQs](https://betanseapi.nseindia.com/static/products-services/indices-faqs) — published NIFTY 50 historical-data start date.
7. [NSE EOD / Historical Data Subscription](https://betanseapi.nseindia.com/static/market-data/eod-historical-data-subscription) — EOD and historical Order & Trade products, delivery channels, F&O samples/specification and subscription contact.
8. [NSE Historical Order & Trade Data Specification v1.19 (3 August 2026)](https://nsearchives.nseindia.com//web/mediaattachment/2026-09/NSE_Hist_Order_Trade_Data_1.19_20260902144045.pdf) — current reviewed F&O Trim/Full order and trade layouts and stated product delays.
9. [NSE Data & Info Vending](https://www.nseindia.com/static/nse-data-and-analytics/data-information-vending) — NSE Data & Analytics product categories and current tariff access. [Domestic tariff effective 1 April 2026 (PDF)](https://nsearchives.nseindia.com//web/mediaattachment/2026-04/Download_Pricing_file_-_Domestic_clients_20260424122229.pdf) — standard EOD/Historical F&O Order & Trade, trade-only and educational/researcher rates; all prices subject to stated conditions/taxes.
10. [NSE Option Chain](https://www.nseindia.com/option-chain) — public current option-chain interface reviewed; it does not itself establish historical-date archive coverage.
11. [NSE FPI FAQs](https://nsearchives.nseindia.com/web/sites/default/files/inline-files/FPI%20FAQs_Brochure.pdf) — published historical O&T / historical trade date-range guidance and description of market-data levels. Confirm current F&O terms with NDAL rather than treating the FAQ as a subscription contract.

## 9. Phase boundary

This source catalogue is research and requirements evidence only. It does not download, store, parse, or transform market data. It does not configure credentials, create an NDAL subscription, authorise automated public-site access, implement a collector, build the 22-session calendar, or prove order-book replay. Those actions remain gated by the applicable phase and acceptance evidence.
