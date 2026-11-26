# NIFTY Seller Intelligence — Implementation Roadmap

**Status:** Phase 1 COMPLETED on the feature branch; Phases 2–12 remain NOT STARTED and require separate authorisation. PR #1 remains draft/unmerged; `main` is unchanged. See `docs/PHASE_PROGRESS.md` for acceptance evidence.  
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

## Phase 2 — NSE data access and source verification

**Goal:** Verify the exact authorised data sources and historical coverage needed by the application.

**Work items**
- Catalogue UDiFF Common Bhavcopy Final, MII Contract File, NIFTY index history, contract-wise price-volume and applicable OI reports.
- Investigate historical option-chain availability and fields.
- Investigate authorised F&O order and trade data products, including their historical coverage, delivery format, event fields and entitlement requirements.
- Confirm whether order events can be reconstructed into a sufficiently complete order book and whether trade records can be linked to the associated order events.
- Record availability delays for trimmed and full order/trade products.
- Document access method, terms, credentials required, retention and date coverage.
- Identify required versus optional reports and fields.

**Deliverables:** Source catalogue, access/entitlement decision, report coverage matrix and field dictionary.

**Acceptance criteria**
- Every planned source has documented evidence of availability and an authorised access method.
- Order/trade reconstruction feasibility is proven with permitted sample data before it is treated as an available feature.
- Unavailable data fields are explicitly documented.
- Unauthorised scraping or access-control circumvention is not used.

**Dependency:** Data-provider/NSE entitlement and sample data may be required.

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

## Phase 4 — Automatic historical report collector

**Goal:** Automatically collect every applicable required report for all 22 sessions using authorised access.

**Work items**
- Implement report-source adapters based on Phase 2 findings.
- Resolve report/date combinations from the report catalogue.
- Collect applicable EOD, contract master, index, price-volume, OI, option-chain, order and trade products.
- Download original report bytes and preserve original filenames.
- Implement timeouts, bounded retries, idempotency and duplicate detection.
- Validate date, file format, size, checksum and expected schema.
- Record unavailable, partial, invalid and pending reports.

**Deliverables:** Collection engine, report manifest, retry/error logs and per-session collection results.

**Acceptance criteria**
- Every required report/date combination has a recorded outcome.
- Successful downloads are integrity-checked.
- Failed or unavailable sources are visible and never silently replaced.
- No report is claimed as automatically collected until an authorised end-to-end download test succeeds.

**Dependency:** Phase 2 authorised access verification.

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

## Phase 8 — Multiple-strike CE/PE and order-flow analysis

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

## Phase 9 — Seller intelligence engine

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

## Phase 10 — Seller panic-exit risk engine

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

## Phase 11 — Machine learning

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
