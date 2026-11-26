# NIFTY Seller Intelligence — Requirements

**Status:** Proposed Android-delivery revision recorded for user review; implementation is not authorised by this document
**Repository:** `naveenm2020it-glitch/Support-Resistance`  
**Target branch:** `feature/nifty-seller-intelligence-foundation`

## 1. Project objective

Build a data-provenance-first NIFTY Options Seller Intelligence system that analyses multiple Call (CE) and Put (PE) strikes for a selected analysis date and expiry. The system must use genuine historical exchange data and distinguish measured facts from analytical interpretations.

The system is intended to:
- Analyse multiple strikes, not only the at-the-money (ATM) strike.
- Compare longer-term positioning with recent changes and the selected completed EOD session.
- Identify potential support and resistance zones (S1, S2, R1 and R2).
- Assess potential seller strength, weakness and panic-exit risk.
- Provide evidence, data coverage and confidence/limitations alongside every analytical result.
- Introduce machine learning only after a sufficiently large, validated historical dataset is available.

Open interest (OI), change in OI, volume and price are positioning proxies. They do not establish the identity of a buyer or seller, nor prove that a particular participant opened or closed a position.

## 2. User inputs and analysis scope

The application must support these inputs:
- **Underlying:** NIFTY.
- **Analysis date:** user-selected date.
- **Expiry:** user-selected option expiry.
- **Strike coverage:** multiple CE and PE strikes, with configurable range/selection.
- **Historical window:** 22 valid F&O trading sessions ending at the selected completed EOD session.

The application must display the selected inputs and the actual session dates used. It must not silently change the selected date or expiry.

## 3. The 22-session analysis model

The 22 valid F&O trading sessions are divided into three groups:

| Group | Sessions | Purpose |
|---|---:|---|
| Long-term baseline | 1–17 | Establish the longer-window positioning baseline |
| Recent changes | 18–21 | Identify recent positioning changes relative to the baseline |
| Selected EOD | 22 | Evaluate the latest selected completed session |

Rules:
1. Determine sessions from the official exchange trading calendar and actual F&O trading dates. Exclude weekends, exchange holidays and non-trading dates.
2. Session 22 must be a completed EOD session. If its final reports have not been published, mark it **Pending EOD** and do not present it as complete.
3. Show the exact 22 dates in the user interface and in the analysis output.
4. Do not assume all 22 sessions share one option expiry. Contracts must be aligned using their actual underlying, expiry, strike, option type and session date. The selected expiry analysis must remain separately identifiable.
5. Do not fill gaps by copying adjacent dates, using current data as historical data, interpolation, estimation or simulation.

## 4. Automatic historical report collection

### 4.1 Requirement

The system must automatically identify and download all applicable required reports for each of the 22 sessions. Manual downloading must not be the normal operating workflow.

The collector must:
- Determine the required report dates from the 22-session calendar.
- Discover and download the applicable report for each session from an officially permitted source.
- Preserve the original downloaded bytes and original filename.
- Validate date, format, integrity and required fields.
- Retry transient download failures using bounded retries and record each failure.
- Record reports that are unavailable, partial, invalid or not yet published.
- Avoid silently substituting another report or another date.
- Be safe to rerun without creating misleading duplicate records.

### 4.2 Required report families

Collect the following report families where they are published and accessible under an authorised data-access arrangement:

1. **F&O — UDiFF Common Bhavcopy Final (ZIP):** primary EOD contract-level derivatives data.
2. **MII Contract File (GZ):** contract master/reference data needed to interpret contracts.
3. **NIFTY 50 Index historical data (CSV or official equivalent):** underlying index reference data.
4. **Historical contract-wise price-volume reports:** contract-level price and traded-activity information.
5. **Historical OI reports:** OI information where separately published and relevant.
6. **Participant-wise OI reports:** broad participant-category information; do not represent these as per-strike participant positions.
7. **Historical option-chain reports:** only for dates and fields for which an official historical source actually provides them.
8. **F&O Order data:** authorised historical order-event data, including order identifiers, timestamps, buy/sell side, price, quantity, additions, modifications and cancellations where the selected product supplies them.
9. **F&O Trade data:** authorised historical trade records, including trade identifiers, timestamps, price, quantity and linked buy/sell order identifiers where supplied.
10. **Other relevant official NSE reports:** only when justified by the data specification and available through an authorised source.

The collector must maintain a report catalogue describing which report families are required, optional, date-specific and applicable to each analysis. A report that does not exist for a date or is not officially accessible must be recorded as unavailable; it must not be manufactured.

### 4.3 Authorised access and compliance

Automatic collection is conditional on an officially permitted access method, such as an authorised API, SFTP/data feed, licensed data provider or another access method explicitly permitted by the source.

Do not implement unauthorised NSE website scraping, automated public-site requests or access-control circumvention. Public visibility of a report does not by itself establish permission for automated collection. Confirm the applicable entitlement, terms and technical access before enabling the collector.

If authorised access is not available, report collection must remain blocked or use a permitted manual/import process clearly labelled as such. Do not claim automatic downloading is operational until it has been tested against the authorised source.

## 5. Temporary GitHub storage and automatic five-hour deletion

### 5.1 Required behaviour

During collection and processing of the 22-session reports:
- Store downloaded source reports in temporary storage associated with this GitHub project.
- Keep each report available for the processing workflow.
- Start a separate five-hour retention clock for each report from its successful upload to temporary storage.
- Automatically delete each temporary report when its five-hour retention period expires.
- Record the upload time, expiry time and deletion result.
- Cleanup must not depend on a user manually deleting files.
- Failed cleanup must be logged and retried; it must not be silently treated as successful.

### 5.2 GitHub storage constraints

Raw NSE reports must **not** be committed to normal Git branches or added to Git history. Deleting a committed file from the latest branch does not remove its earlier copies from Git history.

Use a GitHub-hosted temporary artifact/storage mechanism associated with the repository (for example, workflow artifacts) and an automated cleanup process that deletes each artifact/report at its five-hour expiry. The chosen mechanism must be tested to confirm that it can meet the five-hour lifecycle; a default multi-day retention setting alone does not satisfy this requirement.

Temporary storage is for source reports only. It must not be confused with the permanent archive or the processed analytical dataset.

### 5.3 Processing and archival safety

Before a temporary report expires, the workflow must complete its required processing and preserve the original report in the designated permanent raw archive, where that archive is configured and available. Do not delete a report while an active process is reading it. If processing or archival has failed at expiry, raise a visible operational error and record the retention exception; never silently discard the only copy or claim that the report was safely archived.

Temporary cleanup must never delete permanent archive files, validated processed outputs, manifests, analysis results or audit records.

## 6. Original-file preservation and permanent archive

Maintain separate storage areas for:
- **Temporary GitHub storage:** short-lived report copies subject to five-hour automatic deletion.
- **Permanent raw archive:** original source reports retained for reproducibility, preferably on the configured Ubuntu server.
- **Working area:** disposable extracted or transformed copies used for processing.
- **Processed area:** validated normalised data and derived analytical outputs.

The original report must remain byte-for-byte unchanged in the permanent raw archive. Extract or transform only working copies. Never overwrite, rename or modify the original archived source file.

Ubuntu archive/deployment setup is dependent on authorised server access and must be reported as pending until connectivity and storage are verified.

## 7. Provenance and validation

For every downloaded report, retain a provenance record containing, where applicable:
- Source URL or authorised source identifier.
- Original filename.
- Report/session date.
- Download timestamp and temporary-storage upload timestamp.
- Five-hour expiry and cleanup timestamp/status.
- File size and SHA-256 checksum.
- Report family and expected format.
- Fields actually present in the report.
- Validation result and validation errors.
- Permanent archive location/status.
- Processing status and related analysis/session identifier.

The provenance record and validation results must remain available after temporary report deletion. Hashes must be calculated from the original downloaded bytes and used to verify transfers and archived copies.

## 8. Multiple-strike CE/PE data model and analysis

Analyse multiple strikes on both CE and PE sides. Do not restrict the analysis to ATM.

Where the official source contains the fields, capture:
- Underlying/symbol.
- Contract expiry.
- Strike price.
- Option type (CE or PE).
- Session/trade date.
- Open interest.
- Change in open interest, where supplied or validly derivable from two verified consecutive observations.
- Traded quantity/volume.
- Open, high, low and close/settlement price as actually supplied.
- Underlying index value.
- Implied volatility (IV), only when the source actually supplies a valid IV field.
- Bid, ask, market depth or other quote fields, only when an entitled source actually supplies them.

Where authorised order/trade products provide them, also preserve order ID, trade ID, buy/sell order references, event timestamp, order side, order event type, execution price and executed quantity. Link trade events to their referenced order events only when identifiers and timestamps support the match. Reconstruct order-book state only when the acquired event stream is sufficiently complete and correctly sequenced; otherwise mark aggressor classification and order-book metrics unavailable.

Align observations by underlying, expiry, strike, option type and session date. Retain source-field definitions and units. Do not treat missing fields as zero. Do not invent historical IV, bid/ask, depth or full option-chain snapshots from a bhavcopy.

A UDiFF EOD bhavcopy is contract-level EOD data; it is not, by itself, a full historical option-chain or intraday quote/depth record.

## 9. Seller positioning interpretation

Use an evidence hierarchy rather than the four-quadrant OI/price heuristic. The primary method, when the necessary authorised data is available, is trade/order-flow analysis; aggregate OI, price and volume remain supporting evidence.

### 9.1 Aggressive trade-flow evidence
- Reconstruct the order-book state immediately before each execution from correctly sequenced order events, where the acquired product provides sufficiently complete order data.
- Classify an execution as buyer-initiated or seller-initiated only when the order-book evidence supports that classification.
- Link trades to buy/sell order references where the source provides reliable identifiers.
- Mark midpoint, stale-quote, crossed-book, missing-event, complex-trade or otherwise ambiguous executions as unclassified rather than guessing.
- Report aggressive sell volume/value and aggressive buy volume/value separately, including classified-quantity coverage.

### 9.2 Passive sell-side supply and absorption
Where order-event data supports it, measure sell-order additions, cancellations, executions, persistence and replenishment at the ask. Repeated sell-side replenishment while aggressive buyers execute into the ask may indicate passive sell-side absorption. Displayed liquidity is not proof of hidden inventory or an identified writer.

### 9.3 Position build-up and close/open ambiguity
- Use explicit open/close or position-effect fields only if an authorised source actually supplies them and their semantics are verified.
- Otherwise use correctly aligned intraday/EOD OI snapshots as aggregate net-position evidence, not as proof that a specific trade opened or closed a short.
- Compare OI only for the same underlying, expiry, strike and option type.
- Label OI-based writing/covering interpretations as potential and disclose the limitations.

### 9.4 Premium and underlying confirmation
Where synchronised and valid inputs exist, evaluate option premium movement relative to underlying movement and available volatility/time inputs. Any Greek-based residual or derived feature must state its model, inputs and assumptions. Do not manufacture IV, quotes or depth.

### 9.5 Multi-strike and 17 + 4 + 1 confirmation
Compare CE/PE order-flow and positioning evidence across adjacent strikes, then compare sessions 1–17 against sessions 18–21 and session 22. Distinguish strike-local signals from wider strike-cluster signals.

### 9.6 Required outputs
Classify evidence into separate concepts such as aggressive sell pressure, passive seller absorption, potential short-side build-up, potential short covering, potential long liquidation and mixed/indeterminate activity. Provide the supporting observations, classified-flow coverage, missing data and evidence-quality tier for each output.

Do not claim to identify actual option sellers, their entry prices, intent or individual positions unless a source explicitly and lawfully supports that claim. Trade records show both sides of a transaction; an aggressor side must be inferred from adequate market-event evidence, not from the existence of buy/sell order IDs alone. Participant-wise OI is category-level data and must not be presented as strike-level participant identity.

## 10. Support and resistance zones

Identify and report:
- **S1:** primary potential support zone.
- **S2:** secondary potential support zone.
- **R1:** primary potential resistance zone.
- **R2:** secondary potential resistance zone.

Use multiple-strike evidence, OI and OI changes where available, price behaviour, volume and historical positioning. Explain the evidence and method behind each zone. A single high-OI strike must not automatically be declared confirmed support or resistance. Label these as analytical zones, not guaranteed price barriers.

## 11. Potential seller panic-exit risk

Assess potential seller stress or panic-exit risk using observable signals such as:
- Aggressive buy-side executions against previously observed sell-side liquidity.
- Short-covering evidence, where opening/closing data supports it, or qualified OI/price proxies otherwise.
- Ask-side liquidity depletion or reduced replenishment, where valid order events exist.
- Significant OI reduction/unwinding.
- Option premium movement adverse to a previously observed seller-supply zone.
- Unusual traded volume and changes in classified aggressive flow.
- Similar changes across adjacent strikes.
- Recent positioning diverging from the 17-session baseline.

Report a clearly qualified risk category (for example, Low, Moderate or High), the observed evidence, the affected strikes/side and data limitations. Do not describe the result as a certain prediction or guaranteed market event.

## 12. Machine learning requirements

The 22-session window is the current analysis window, not a sufficient default training dataset.

Machine learning must remain disabled or clearly marked unavailable until a substantially longer, validated historical dataset is available and the dataset is appropriate for the intended target.

When enabled:
- Use chronological training, validation and test splits.
- Prevent look-ahead and target leakage.
- Keep features restricted to information available at the prediction timestamp.
- Compare model results with suitable baselines.
- Report evaluation metrics, sample size, date range, model version and limitations.
- Show calibrated confidence/probability only where justified by validation.
- Do not fabricate missing training observations or labels.

## 13. Mandatory data-availability gate

Maintain a session-by-report availability matrix for all 22 sessions. Use these statuses:

| Status | Meaning |
|---|---|
| Available | Report obtained and passed required validation |
| Partial | Report exists but required fields or portions are missing |
| Unavailable | No valid report is available from the authorised source |
| Pending EOD | Final EOD report has not yet been published |
| Invalid | Report failed integrity, date, format or content validation |

Do not mark the 22-session dataset complete, or produce a fully qualified analysis, when required core data is missing or invalid. The application may show partial diagnostics, but must clearly identify the missing coverage and must not fill gaps with fabricated data.

Define mandatory versus optional report families explicitly in the report catalogue. Optional fields absent from a source must be shown as unavailable, not treated as a failed mandatory field unless the data specification says they are required.

## 14. Android application and analysis report

The primary user-facing product is an Android phone application packaged as an installable APK. Do not deliver a web dashboard as the primary interface. A separate web dashboard is out of scope unless approved later. The application communicates with the Ubuntu-hosted backend through authenticated, documented, secure APIs; it must not contain NSE credentials, GitHub tokens, provider secrets or privileged collection logic, and it must not download restricted NSE reports directly.

The app must support analysis date and expiry selection and present the following screens or tabs: Overview, CE Analysis, PE Analysis, Multiple Strikes, Seller Intelligence, Panic Risk, Historical Sessions, Data Coverage, and Settings. It must provide login and secure session management if authentication is required. Its mobile layout must be designed for Android phone screens, with accessible, readable charts and responsive states rather than a shrunken desktop dashboard.

The app and generated analysis report must display:
1. Selected underlying, analysis date and expiry.
2. The exact 22 trading-session dates and the 17 + 4 + 1 grouping.
3. Per-session, per-report availability and validation status.
4. Data coverage, missing fields and source/provenance information.
5. Multiple-strike CE and PE analysis.
6. Aggressive buy/sell flow, classification coverage and order-book evidence where available.
7. Passive sell-side supply/replenishment evidence where available.
8. OI, change in OI, volume and price evidence.
9. Baseline versus recent versus selected EOD comparison.
10. S1, S2, R1 and R2 zones with supporting evidence.
11. Potential seller strength/weakness and evidence-quality tier.
12. Potential seller panic-exit risk and its evidence.
13. ML output and confidence only when the ML eligibility gate is passed.
14. Warnings, assumptions, limitations and report-generation timestamp, plus visible error, missing-data and collection/download-status notifications.

## 15. Technical and operational requirements

The solution should use a modular Python-based architecture with independently testable components for:
- Trading-session calendar and 22-session selection.
- Authorised report collection, including applicable order and trade products.
- Temporary GitHub artifact storage and five-hour expiry cleanup.
- Permanent raw archive and transfer verification.
- Provenance manifests and data validation.
- Report parsing, order/trade event linking and normalisation.
- Order-book reconstruction and aggressive-flow classification where the acquired data supports it.
- Multiple-strike CE/PE analysis.
- Seller-positioning, passive supply and support/resistance analysis.
- Panic-exit risk assessment.
- ML training/inference eligibility and evaluation.
- Backend API and analysis-report generation for the Android client.
- Android application source, APK build/release and device/emulator verification in Phase 12.
- Phase-wise code, documentation, progress tracking and acceptance evidence.
- Logging, retries, monitoring and automated tests.

Secrets, access tokens and data-provider credentials must be stored in approved secret-management facilities, never committed to the repository or written into logs.

Ubuntu deployment and permanent archive configuration require verified server access. Do not claim deployment or server-side automation is complete before it is actually configured and tested.

## 16. Development phases (planning only)

1. Foundation and architecture.
2. NSE data access and historical report coverage verification.
3. 22-session calendar engine.
4. Automatic collection of EOD, order and trade reports through authorised access.
5. Temporary GitHub-associated report storage and verified five-hour cleanup.
6. Permanent raw archive, provenance and transfer verification.
7. Parsing, order/trade linking, validation and the availability gate.
8. Multiple-strike CE/PE and order-flow analysis.
9. Seller intelligence, passive supply, S1/S2/R1/R2 and evidence-quality assessment.
10. Seller panic-exit risk engine.
11. ML using a sufficiently large validated historical dataset.
12. Android application, secure API integration, testing, APK build/release and separate Ubuntu backend deployment.

All phase implementation code, tests, configuration and technical documentation must be stored in this repository under the approved project branch workflow. Progress must be recorded in `docs/PHASE_PROGRESS.md`, with phase deliverables, status, blockers, commit references, test evidence and acceptance decisions. See `docs/IMPLEMENTATION_ROADMAP.md` and `docs/WORKFLOW.md`.

Raw NSE reports, credentials and secrets must not be committed to Git history.

**This document records requirements only.** This proposed Android-delivery revision does not authorise implementation, downloading NSE reports, changing application code, building an APK, deploying services, creating scheduled workflows or modifying application files. Those activities require separate approval of the revised plan.


## 17. User-selected no-subscription / free-source mode — 2026-11-26

**Decision:** The user has confirmed that no NSE Data & Analytics subscription is held and has directed the project to use free official source reports where permitted. The project must not assume a paid NDAL entitlement or implement a paid O&T integration under this direction.

### 17.1 Operating model

- The technical target is user-initiated download through an official NSE/NIFTY Indices report interface followed by local file import, but only after the applicable download, storage and intended-use rights are confirmed.
- Ingest user-supplied original files through a local/file-import workflow only when the user has a permitted basis to retain and process them; preserve original bytes and provenance.
- Do not poll, scrape, automate browser downloads from, or reverse-engineer NSE public report pages. NSE's website Terms of Use prohibit systematic/automated data collection.
- Do not call this a live feed. The no-subscription source mode is a manual, file-based EOD import.
- A future authorised/licensed feed can be considered only after a separate user decision and written access/usage rights.

### 17.2 Eligible EOD input families

Candidate official inputs, subject to sample/schema validation:
1. F&O UDiFF Common Bhavcopy Final.
2. Daily F&O-MII contract master matching the report date.
3. Historical contract-wise price-volume data.
4. NIFTY 50 historical index CSV.
5. F&O participant-wise OI and participant-wise trading-volume reports as optional broad participant context.
6. Other official daily/monthly F&O reports only when their schema and purpose are verified.

The exact field map, null semantics, scales, units and historical date coverage must be established from actual permitted sample files. Do not infer a current UDiFF schema from a discontinued format.

### 17.3 Data-use eligibility gate

NSE's Data List Under the NSE Data Sharing Policy classifies specified derivatives contract-wise price-volume reports and daily/monthly F&O report archives in its first, freely available research-data basket for non-commercial academic research. The same policy places voluminous historical EOD and tick-by-tick datasets above 2 GB in a separate chargeable basket. NSE's Research Initiatives page states that research data is strictly limited to academic and non-commercial use. NSE's website Terms of Use (clauses 8–9) restrict copying/storage and redistribution without prior written permission (subject to the terms' download wording) and prohibit systematic or automated collection.

The user/project has no NDAL subscription and has not established an academic/non-commercial research entitlement or a written licence for a trading/seller-intelligence/ML use.

Therefore:
- Do not assume the project's trading/seller-signal use qualifies for NSE's no-cost academic research-data route.
- Before retaining downloaded reports in a persistent dataset or using them to train ML for trading/signal decisions, obtain written NSE confirmation covering the source, acquisition, storage, model-training purpose and derived-output/dashboard use.
- If the work is separately approved as eligible academic/non-commercial research, follow NSE's Data Seeking Request process and any NDA/reporting requirements.
- NSE identifies voluminous historical EOD and tick-by-tick datasets above 2 GB as chargeable. Do not claim those large datasets are free.
- Public visibility or a download button does not authorise scraping, automated collection, bulk storage or broader reuse.

### 17.4 Feature boundary

The no-subscription EOD dataset may support contract-level EOD CE/PE price, settlement, traded activity, turnover, OI/change-in-OI where supplied, matching contract identity, NIFTY index context, broad participant aggregates, qualified EOD support/resistance research and offline EOD ML after the longer-history/rights/validation gates pass.

It does not support order-book replay, order additions/modifications/cancellations, bid/ask depth history, aggressor-side flow, passive seller replenishment, individual seller identity, or intraday panic-exit detection. Those outputs must be disabled or explicitly unavailable. OI and EOD price/volume are aggregate positioning proxies, not proof of seller identity, new writing, covering or intent.

A public current option-chain CSV is not a historical archive. Do not backfill past snapshots from current data or bhavcopy.

### 17.5 ML eligibility

- The 22-session 17 + 4 + 1 window is for analysis, not a default training-set size.
- ML must wait for a sufficiently long and consistent historical EOD dataset, an approved target definition, source-use permission, chronological splits and out-of-sample evaluation.
- No model may be trained from data that has an unresolved acquisition/use-rights basis.
- Keep missing fields absent; never synthetically reconstruct historic IV, bid/ask, depth or order events.

The implementation details and source restrictions are recorded in [FREE_SOURCE_MODE.md](FREE_SOURCE_MODE.md).

### 17.6 Proposed target reconciliation

The later Android-delivery proposal changes the desired collection workflow from manual-only to automatic collection of applicable reports, but only through an explicitly authorised source and access method. It does not grant an NSE entitlement, override published terms, or authorise public-site scraping or access-control circumvention. The recorded absence of an NDAL subscription and unresolved written use rights remain blockers for any collector that requires such access. Until a compliant source route and applicable use rights are verified, automatic collection remains a requirement that is blocked, not an implemented or approved workaround. The app may report missing/unavailable data and must not substitute fabricated history.

Where entitled order/trade data is lawfully available, the target includes the evidence-led order/trade flow and passive seller-absorption analysis already specified above. Under the currently documented no-subscription EOD-only source set, these capabilities remain unavailable and must be presented as such.
