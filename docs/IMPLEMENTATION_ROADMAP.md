# NIFTY Seller Intelligence — Implementation Roadmap

**Status:** Phase 1 COMPLETED; Phase 2 IN PROGRESS on the feature branch; Phases 3–12 remain NOT STARTED and require separate authorisation. PR #1 remains draft/unmerged; `main` is unchanged. See `docs/PHASE_PROGRESS.md`, `docs/PHASE2_SOURCE_CATALOGUE.md` and `docs/FREE_SOURCE_MODE.md` for current evidence.  
**Repository:** `naveenm2020it-glitch/Support-Resistance`  
**Working branch:** `feature/nifty-seller-intelligence-foundation`  
**Requirements:** `docs/REQUIREMENTS.md`  
**Progress register:** `docs/PHASE_PROGRESS.md`

## Delivery principles

1. Develop in the 12 phases below and validate each phase against its acceptance criteria.
2. Store application source code, tests, configuration, technical documentation and phase progress in this repository.
3. Keep development on the approved feature branch. Do not modify `main` or merge to it without separate approval.
4. Do not mark a phase complete based only on code being written. Required tests, evidence and acceptance criteria must pass.
5. Keep raw NSE reports, downloaded market data, credentials and secrets out of Git history.
6. Keep raw report storage separate from code. Use approved temporary storage for the five-hour lifecycle and the configured permanent archive for original reports.
7. Never claim a source integration, test, archive transfer, cleanup job or deployment works until it has been executed and verified.
8. A blocked dependency must be visible in the progress register. Do not work around data-access restrictions or fabricate missing data.

## Phase 1 — Foundation and architecture

**Status:** COMPLETED — 2026-11-26. All Phase 1 acceptance criteria are recorded as PASS in `docs/PHASE_PROGRESS.md`. Native GitHub Actions run #23 passed on Python 3.11.18 with **48 tests passed**. The completion applies only to the foundation architecture, data contracts, validation boundaries, documentation/conventions and test workflow; it does not mean NSE data access, collection, storage, parsing, analytics or deployment is implemented. PR #1 has not been merged and `main` remains unchanged.

**Goal:** Establish the architecture and development conventions before expanding the application.

**Work items**
- Review the existing repository foundation and identify reusable code.
- Define module boundaries, data flow, typed models and configuration.
- Define identifiers and schemas for analysis runs, sessions, reports, contract observations, order/trade events and analytical outputs.
- Define logging, error handling, testing, versioning and configuration rules.
- Document data lineage from source report to final analytical result.

**Planned code/documentation**
- `src/nifty_seller_intelligence/models.py` and other core model modules.
- `config/` for non-secret application settings and report catalogues.
- Architecture and data-dictionary documentation.
- Unit-test conventions under `tests/`.

**Deliverables:** Architecture, data contracts, configuration standards and test strategy.

**Acceptance criteria**
- Every data record has a defined identity and date/contract dimensions.
- Missing values remain distinguishable from zero.
- Source lineage can be retained for derived observations.
- Existing foundation code is reviewed and baseline tests are executed.
- No production functionality is claimed before it is implemented and tested.

## Phase 2 — NSE source and usage-rights verification

**Status:** IN PROGRESS — the user selected a no-subscription direction and has no NDAL subscription. Official NSE EOD report families are inventoried in `docs/PHASE2_SOURCE_CATALOGUE.md`; the intended technical path is manual, user-supplied EOD files followed by local import. This is not a live or automated NSE feed. The NSE research-data programme is strictly academic/non-commercial, the website terms restrict storage/reuse and prohibit automated collection, and the project has not obtained written permission for persistent dataset creation or trading-related ML. UDiFF/export samples and exact field mappings also remain untested. Paid O&T and historical tick data are out of current scope.

**Goal:** Identify an affordable official EOD input set and confirm that acquisition, storage, analysis and intended ML/trading use are permitted.

**Work items**
- Catalogue F&O UDiFF Common Bhavcopy Final, the matching daily F&O-MII contract master, historical contract-wise price-volume, NIFTY 50 historical index CSV and optional participant-wise OI/volume reports.
- Obtain written clarification from NSE on whether the project may manually download, retain, analyse and train ML models on the selected reports for its seller-intelligence/trading purpose.
- Distinguish the research-policy data basket from public website downloads and from chargeable >2 GB historical EOD/tick-by-tick products.
- After use rights are confirmed, obtain a permitted representative sample of each selected EOD family; inspect field names, schema/version, units, scales, null codes and date coverage.
- Confirm historic report date availability and identify mandatory versus supplementary reports for the 22-session window.
- Record fields unavailable from EOD reports, including order events, historical IV, quotes and depth when absent.
- Keep paid Order & Trade, historical tick-by-tick and order-book replay out of the current no-subscription scope; do not implement or imply those capabilities.

**Deliverables:** EOD source catalogue, written data-use decision, verified sample/schema map, candidate 22-session report-coverage matrix and explicit unavailable-field register.

**Acceptance criteria**
- Every selected EOD report family has an official source and documented permitted acquisition route.
- Written permission/terms cover local or server-side retention, ML training, derived seller-intelligence signals and dashboard use for the intended project.
- Each selected report has at least one permitted real sample whose date, format, fields, units and null semantics have been inspected.
- Required versus optional report families are defined from verified samples, not assumed.
- Unavailable data and unsupported analyses are explicitly identified.
- No automated collection from NSE public pages or access-control circumvention is used.

**Dependency:** NSE written data-use clarification/permission may be required. If permission is not granted, NSE files must not be retained for model training; an alternative data source with an explicit usable licence must be identified.

## Phase 3 — 22-session calendar engine

**Goal:** Produce the exact 22 completed F&O trading sessions for the requested analysis date.

**Work items**
- Resolve the verified F&O trading calendar.
- Exclude holidays and non-trading dates.
- Select 22 valid completed sessions.
- Assign sessions 1–17, 18–21 and 22 to their specified groups.
- Check actual EOD publication status for the selected session.
- Store the exact date list and calendar/source reference in the analysis-run manifest.

**Deliverables:** Calendar module, session manifest and calendar tests.

**Acceptance criteria**
- Weekends and exchange holidays are handled correctly.
- Session 22 cannot be marked complete before EOD publication and validation.
- All 22 dates are explicit, reproducible and non-overlapping.
- Missing calendar information blocks a definitive session selection rather than triggering a guess.

## Phase 4 — EOD report file ingestion and validation

**Goal:** Build a local file-import workflow for original EOD report files that the user has a confirmed right to download, retain and process. No remote NSE collector or scheduled website polling is part of the no-subscription mode.

**Work items**
- Resolve required report/date combinations from the approved Phase 2 EOD catalogue.
- Accept user-supplied original files through a local import boundary.
- Preserve original filename and bytes; calculate SHA-256 and record source reference, report type, report date and import time.
- Validate archive integrity, expected report date, file type, schema version, required fields, units and row counts using the verified Phase 2 schema.
- Detect duplicate imports idempotently by source context and content hash.
- Record missing, partial, invalid and pending reports without silent substitution.
- Keep raw files outside normal Git commits and ensure that any persistent storage location is covered by the approved data-use rights.

**Deliverables:** File-import/validation workflow for permitted EOD inputs, provenance manifest and per-session report availability results. An authorised remote collector is not included.

**Acceptance criteria**
- Each imported file has a provenance record and verified checksum.
- Invalid, duplicate, partial and wrong-date files are handled explicitly.
- Missing dates/reports are not backfilled from another session or live data.
- The import workflow operates on user-supplied files only; no scheduled polling, scraping or automated collection from NSE public pages is present.
- Persistent storage is not enabled until Phase 2 data-use permission is established.

**Dependency:** Phase 2 permitted-use decision and verified report schemas; Phase 3 session manifest.

## Phase 5 — Temporary GitHub storage and five-hour cleanup

**Goal:** Store analysis source reports temporarily in a repository-associated GitHub storage mechanism and delete eligible copies automatically after five hours.

**Work items**
- Select and verify the appropriate GitHub artifact/storage mechanism.
- Register each uploaded artifact with analysis ID, report ID, upload time and exact expiry timestamp.
- Implement automated expiry checks and deletion.
- Handle processing locks, active reads, archive transfer failure, deletion errors and retries.
- Preserve a cleanup audit record after temporary artifact deletion.
- Confirm that raw reports never enter normal Git commits or Git history.

**Deliverables:** Temporary artifact store, expiry registry, cleanup automation and lifecycle test report.

**Acceptance criteria**
- A test artifact remains available for the expected processing period.
- The cleanup process deletes it after its five-hour expiry, subject to documented safety conditions.
- A report is not silently deleted while it is being processed or before the required permanent archive safeguard passes.
- Failed cleanup is visible and retried.
- Git history contains no raw report files.

**Technical note:** Do not assume GitHub's native artifact retention setting supports a five-hour duration. Validate the exact lifecycle. If the available GitHub mechanism cannot provide the required timing, present an alternative with a suitable expiry mechanism for approval.

## Phase 6 — Permanent archive and provenance

**Goal:** Preserve original NSE source reports independently from temporary GitHub storage.

**Work items**
- Configure the approved Ubuntu raw archive after access is verified.
- Transfer original report bytes without modification.
- Verify SHA-256 and file size after transfer.
- Preserve original filename and report date.
- Store manifests with source, download, upload, expiry, archive, processing and cleanup states.
- Keep raw, working and processed data in separate locations.
- Block normal temporary deletion when archival/processing safeguards have failed; raise and log an exception rather than losing the only copy.

**Deliverables:** Raw archive layout, transfer verifier, provenance manifest and archive audit log.

**Acceptance criteria**
- Archived checksum matches the original downloaded file.
- Original source bytes remain unchanged.
- Temporary cleanup cannot delete permanent archive data.
- Ubuntu setup is reported as pending until server access and transfer tests succeed.

**Dependency:** Verified Ubuntu server access or another separately approved permanent archive.

## Phase 7 — Parsing, order/trade linking and validation

**Goal:** Parse source reports into a validated dataset while preserving lineage.

**Work items**
- Implement parsers for each verified report format.
- Parse EOD contract observations and reference contract data.
- Parse order events and trade records when the entitled source supplies them.
- Link trade records to order references using verified identifiers.
- Reconstruct order-book state only if event completeness, sequencing and field semantics support it.
- Validate date, instrument, expiry, strike, option type, units, duplicates and numeric ranges.
- Track missing, partial and invalid fields.
- Implement the 22-session mandatory data-availability gate.

**Deliverables:** Parsers, normalised data model, order/trade event model, validation reports and availability matrix.

**Acceptance criteria**
- Each observation links to its source report and source row/event where possible.
- Ambiguous or incomplete order-flow events remain unclassified.
- Missing data is never converted into zero or fabricated.
- Required reports and fields pass validation before a dataset is marked complete.

## Phase 8 — Multiple-strike CE/PE EOD analysis (order-flow only if separately authorised)

**Free-source scope:** With EOD files alone, implement only contract-aligned EOD price, settlement, volume/contract count, turnover and OI/change-in-OI analysis where present. Order-flow, order-book reconstruction and aggressor-side classification remain unavailable unless a separately authorised, sufficiently complete tick/order/trade dataset is approved.

**Goal:** Analyse multiple strikes for both option sides across the 22 sessions.

**Work items**
- Align records by underlying, expiry, strike, option type and session/timestamp.
- Calculate verified OI changes and price/volume changes.
- Measure aggressive buy/sell flow where supported by reconstructed order-book evidence.
- Calculate classified-flow coverage and keep ambiguous trades separate.
- Measure sell-order additions, cancellations, executions, persistence and replenishment where order-event data permits.
- Compare strikes and neighbouring strike clusters.
- Compare sessions 1–17 against 18–21 and session 22.
- Preserve source and calculation lineage.

**Deliverables:** Contract-alignment module, strike-wise CE/PE dataset, order-flow metrics and comparison outputs.

**Acceptance criteria**
- Calculations can be traced to source observations.
- Contract and expiry mismatches are prevented.
- Aggressor classification is only used when supported by the available data.
- OI remains an aggregate positioning measure, not proof of seller identity.

## Phase 9 — EOD seller-positioning proxies and support/resistance zones

**Free-source scope:** Interpret only qualified EOD positioning proxies. Do not claim actual seller identity, confirmed writing/covering, passive absorption or intent from OI/price/volume.

**Goal:** Produce evidence-based seller supply and positioning assessments.

**Work items**
- Separate aggressive sell pressure from passive seller absorption.
- Evaluate potential short-side build-up only with appropriate supporting evidence.
- Evaluate potential short covering and long liquidation with explicit caveats where open/close data is absent.
- Combine order flow, order-book behaviour, OI, price, volume and premium/underlying context.
- Compare adjacent strikes and the 17 + 4 + 1 session groups.
- Calculate S1, S2, R1 and R2 as potential zones.
- Implement transparent features and calibrated scoring; do not assign arbitrary weights.
- Produce evidence-quality tier and classification coverage with each result.

**Deliverables:** Seller intelligence engine, seller-supply assessment, positioning explanation and support/resistance zones.

**Acceptance criteria**
- No actual trader identity or short inventory is inferred from aggregate OI.
- Every signal has evidence, limitations and coverage.
- Levels are explained and are not presented as guaranteed barriers.
- Scores/thresholds are tested and calibrated before being presented as confidence.

## Phase 10 — Potential seller-stress assessment (EOD-only limitations)

**Free-source scope:** Intraday seller panic-exit/order-book detection is unavailable without authorised intraday event data. Any later EOD risk flag must be labelled a historical positioning/price-risk proxy, not a detected intraday exit.

**Goal:** Detect potential seller stress and exit pressure.

**Work items**
- Detect aggressive buy-side executions against previously observed sell-side liquidity.
- Evaluate ask-side depletion and reduced replenishment when order events exist.
- Evaluate OI unwinding and covering proxies.
- Detect unusual volume and premium changes.
- Confirm signals across adjacent strikes and recent/baseline comparisons.
- Produce Low/Moderate/High risk with reasons, affected strikes and evidence quality.
- Backtest and review false positives and missed events.

**Deliverables:** Risk detectors, risk assessment output, explanations and historical evaluation report.

**Acceptance criteria**
- An ordinary OI change does not automatically become a high-risk event.
- Ambiguous evidence reduces confidence or produces an indeterminate result.
- Risk outputs are qualified as potential conditions, not certain predictions.

## Phase 11 — Machine learning on permitted EOD history

**Free-source scope:** Train only after a sufficiently long, use-authorised EOD history has been imported and validated. The 22-session analysis window is not by itself a training corpus. No model may use data whose acquisition/use rights are unresolved.

**Goal:** Build predictive models only when longer validated data supports them.

**Work items**
- Define the prediction target and time horizon before training.
- Build a sufficiently long dataset from validated historical observations.
- Construct features using only information available at the prediction timestamp.
- Create chronological train/validation/test splits.
- Build baseline models before complex models.
- Evaluate predictive metrics, calibration and performance across market regimes.
- Track model versions, training dates, features, sample counts and limitations.
- Disable ML when data eligibility or performance criteria fail.

**Deliverables:** Training dataset, feature/target documentation, evaluation report and versioned model where justified.

**Acceptance criteria**
- No look-ahead or target leakage.
- No fabricated training samples or labels.
- Performance is measured on chronological unseen data against a suitable baseline.
- Confidence/probability is displayed only when supported by validation.

## Phase 12 — Dashboard, testing, monitoring and deployment

**Goal:** Deliver the complete application and operational workflow.

**Work items**
- Build analysis-date, expiry and strike controls.
- Display 22-session availability and 17 + 4 + 1 comparisons.
- Display multiple-strike CE/PE analysis and order-flow coverage.
- Display seller supply, S1/S2/R1/R2 and panic-exit risk.
- Display ML only when eligible.
- Show provenance, warnings, missing data and evidence quality.
- Run unit, integration, data-quality, historical and end-to-end tests.
- Configure monitoring, failure alerts, secure credentials and recovery.
- Deploy to Ubuntu after server access and release approval are verified.

**Deliverables:** Dashboard, report export, test evidence, monitoring and deployment documentation.

**Acceptance criteria**
- The complete workflow is tested from analysis request to final report.
- Failed collection, validation, archival and cleanup are visible.
- Secrets do not enter Git.
- Deployment is claimed complete only after a verified deployment and operational check.

## Milestone gates

| Milestone | Phases | Exit condition |
|---|---|---|
| M1 — Foundation | 1–3 | Architecture approved and 22-session selection tested |
| M2 — Data pipeline | 4–7 | Authorised collection, storage, archival and validation demonstrated |
| M3 — Analytics | 8–10 | Multiple-strike seller intelligence and risk tested against historical evidence |
| M4 — ML | 11 | ML eligibility and out-of-sample evaluation passed |
| M5 — Production | 12 | Dashboard, monitoring and deployment verified |

A milestone is not complete until its deliverables, tests and acceptance evidence are recorded in `docs/PHASE_PROGRESS.md`.
