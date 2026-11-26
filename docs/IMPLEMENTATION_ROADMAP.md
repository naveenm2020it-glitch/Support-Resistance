# NIFTY Seller Intelligence — Implementation Roadmap

**Status:** Phase 1 COMPLETED; Phase 2 IN PROGRESS on the feature branch; Phases 3–12 remain NOT STARTED. The Android-delivery revision is proposed for review and does not authorise implementation. PR #1 remains draft/unmerged; `main` is unchanged. See `docs/PHASE_PROGRESS.md`, `docs/PHASE2_SOURCE_CATALOGUE.md` and `docs/FREE_SOURCE_MODE.md` for current evidence.
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

**Status:** IN PROGRESS — the user has no NDAL subscription. Official NSE EOD report families are inventoried in `docs/PHASE2_SOURCE_CATALOGUE.md`; the previously documented no-subscription path is manual, user-supplied EOD files. The proposed revised requirement is automatic collection through an authorised source/API/feed when available; entitlement and written data-use rights remain unresolved, so this target is blocked and no automated public-site collection is authorised. The NSE research-data programme is strictly academic/non-commercial, the website terms restrict storage/reuse and prohibit automated collection, and the project has not obtained written permission for persistent dataset creation or trading-related ML. UDiFF/export samples and exact field mappings also remain untested. Paid O&T and historical tick data are out of current scope.

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
- Every selected report family has an official source and documented, authorised acquisition route, including reports required by the proposed automated 22-session collection target.
- Written rights and credentials/entitlement are verified before enabling scheduled or automated collection; public-site scraping and access-control circumvention remain prohibited.
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

## Phase 7 — EOD report parsing and validation

**Free-source scope:** Parse only verified and permitted EOD report families. Order/trade event parsing, event linking and order-book replay are excluded from the current no-subscription route.

**Goal:** Parse permitted EOD source reports into a validated dataset while preserving lineage.

**Work items**
- Implement parsers for the Phase 2-verified UDiFF, daily contract-master, contract-wise price/volume and NIFTY index formats.
- Parse EOD contract observations and contract reference data.
- Align each observation to the correct report date, contract, expiry, strike and CE/PE type using the matching daily master.
- Validate date, instrument, expiry, strike, option type, units, duplicates, row counts and numeric ranges.
- Track missing, partial and invalid fields; keep source nulls distinct from numeric zero.
- Implement the per-session/per-report availability matrix and the 22-session mandatory-data gate.
- Preserve report hash and stable source-row locator on each normalised record.

**Deliverables:** Verified EOD parsers, normalised EOD data model, validation reports and availability matrix.

**Acceptance criteria**
- Each normalised observation links to its source report and source row.
- Contract mapping uses the correct date-specific contract master.
- Missing data is never converted into zero or fabricated.
- Required reports and fields pass validation before a dataset is marked complete.
- No parser assumes fields that have not been verified in a real source sample.

## Phase 8 — Multiple-strike CE/PE EOD analysis

**Current scope:** Analyse only observed EOD price, settlement, contract count/volume, turnover and OI/change-in-OI fields that are present in the verified report files. No order-flow or order-book replay.

**Goal:** Analyse multiple strikes for both option sides across the 22 sessions.

**Work items**
- Align records by underlying, expiry, strike, option type and session date.
- Calculate EOD price/settlement, traded activity, turnover and OI changes where the source provides the necessary fields.
- Compare strikes and neighbouring strike clusters.
- Compare sessions 1–17 against 18–21 and session 22.
- Preserve source and calculation lineage.
- Report missing fields and contract coverage; do not construct a synthetic option-chain.

**Deliverables:** Contract-alignment module, strike-wise CE/PE EOD dataset and comparison outputs.

**Acceptance criteria**
- Calculations can be traced to source observations.
- Contract and expiry mismatches are prevented.
- Missing fields remain unavailable rather than being estimated.
- OI remains an aggregate positioning measure, not proof of seller identity or intent.

## Phase 9 — EOD positioning proxies and support/resistance zones

**Current scope:** Build explainable EOD indicators from verified price, volume/contract count, turnover, OI/change-in-OI, NIFTY index and adjacent-strike comparisons. These are market-positioning proxies, not identified seller activity.

**Goal:** Produce evidence-based EOD positioning assessments and potential S1, S2, R1 and R2 zones.

**Work items**
- Compare option premium/settlement and OI changes across the same underlying, expiry, strike and CE/PE contract.
- Combine EOD contract activity with NIFTY index OHLC context and optional participant-category reports.
- Compare adjacent strikes and the 17 + 4 + 1 session groups.
- Calculate S1, S2, R1 and R2 as potential zones using a documented, testable methodology.
- Show source coverage, unavailable fields and evidence-quality limitations.
- Calibrate scoring only after historical evaluation; do not assign arbitrary confidence weights.

**Deliverables:** EOD positioning features, support/resistance zones and evidence explanations.

**Acceptance criteria**
- No actual trader identity, seller inventory, confirmed writing/covering, passive absorption or intent is inferred from aggregate OI/price/volume.
- Every signal has traceable evidence, coverage and limitations.
- Levels are explained and are not presented as guaranteed barriers.
- Scores/thresholds are tested and calibrated before being presented as confidence.

## Phase 10 — EOD positioning-risk proxies

**Current scope:** The free EOD source tier cannot detect intraday seller panic exits, ask-side depletion or order-book stress. Any future EOD risk indicator must be explicitly labelled a historical price/positioning-risk proxy.

**Goal:** Evaluate whether EOD price, volume and OI changes indicate unusual positioning or market risk.

**Work items**
- Evaluate unusual EOD volume/contract count and OI reductions or increases.
- Compare option premium/settlement changes with NIFTY index movement.
- Confirm changes across adjacent strikes and recent/baseline comparisons.
- Produce a qualified risk indicator with reasons, affected strikes and data-coverage limitations.
- Backtest and review false positives and missed events.

**Deliverables:** EOD risk indicators, explanations and historical evaluation report.

**Acceptance criteria**
- An ordinary OI change does not automatically become a high-risk event.
- Missing or weak evidence reduces confidence or produces an indeterminate result.
- Risk outputs are qualified as potential conditions, not detected intraday exits or certain predictions.

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

## Phase 12 — Android application, testing, APK build and deployment

**Status:** Proposed scope revision; implementation is not authorised until the user separately approves the revised plan.

**Goal:** Deliver a native Android phone application as an installable APK, integrated with the separately deployed Ubuntu backend through secure, documented APIs. The Android app is the primary UI; a web dashboard is not the delivery target.

**Technology decision for approval:** Kotlin with Jetpack Compose is recommended. It provides native Android UI and direct platform integration, strong fit with Android tooling and a single-platform codebase. Flutter is a viable alternative with a shared Dart UI layer and consistent rendering, but adds a second-language/toolchain choice for this Android-only deliverable. React Native is also viable, but introduces a JavaScript/native bridge ecosystem without a clear benefit for an Android-only app. For charting, use a maintained Android/Compose-compatible chart library selected and reviewed during implementation; keep charting behind app-owned data/view models to avoid coupling to a library. Confirm library health, accessibility, license, and phone-sized chart behaviour before adoption. Final library versions are deferred to the implementation phase.

**Work items**
- Design mobile-first UI/UX for Overview, CE Analysis, PE Analysis, Multiple Strikes, Seller Intelligence, Panic Risk, Historical Sessions, Data Coverage and Settings.
- Integrate secure backend APIs; keep all privileged data collection and credentials on the backend.
- Implement analysis date and expiry selection; surface the 22-session coverage and 17 + 4 + 1 comparison.
- Implement seller position, CE/PE strength, aggressive flow and passive absorption when authorised data supports them, S1/S2/R1/R2, panic-exit risk and ML output only when eligible.
- Implement loading, error, missing-data, download/progress, source, provenance and confidence states.
- Implement accessible charts and multiple-strike visualisations that fit Android phone screens.
- Test on representative Android screen sizes and a reliable Android emulator; test a physical Android device when available.
- Build a debug APK for internal testing, then a signed release APK using signing material kept outside Git and protected CI/secret storage.
- Verify installation and application/API functionality on an actual device or reliable emulator before release claims.
- Record APK version/build number, release notes, backend API compatibility, installation instructions, build/test evidence and APK SHA-256 checksum.
- Document versioning, signing-key custody/rotation and release process. Keep the Ubuntu backend as a separately deployed component; deploy only after its own access and release gates pass.

**Deliverables:** Android source, secure API integration, verified debug/release APK artifacts, APK checksum, release notes, compatibility and installation documentation, test evidence, and separate backend deployment documentation.

**Acceptance criteria**
- The Android app is usable on supported phone sizes and exposes all required screens and states.
- Backend/API integration is authenticated as required, transport-secured, documented and contains no privileged credentials in the APK.
- Unsupported or unavailable data is visibly unavailable; no fabricated, simulated or substituted historical data is presented.
- Debug and signed release APKs are built and their checksums/version metadata recorded; signing keys and secrets are excluded from Git.
- Installation and core application/API flows are verified on an actual Android device or reliable Android emulator. A successful build command alone is not acceptance evidence.
- Backend and app compatibility is documented, and backend deployment is independently verified before being described as deployed.
- The workflow has unit, integration, data-quality, historical, API-contract and Android end-to-end test evidence; data access/rights and ML gates remain enforced.

## Milestone gates

| Milestone | Phases | Exit condition |
|---|---|---|
| M1 — Foundation | 1–3 | Architecture approved and 22-session selection tested |
| M2 — Data pipeline | 4–7 | Authorised collection, storage, archival and validation demonstrated |
| M3 — Analytics | 8–10 | Multiple-strike seller intelligence and risk tested against historical evidence |
| M4 — ML | 11 | ML eligibility and out-of-sample evaluation passed |
| M5 — Production | 12 | Android APK install/functionality verified on a device or reliable emulator; API compatibility and any separately approved backend deployment verified |

A milestone is not complete until its deliverables, tests and acceptance evidence are recorded in `docs/PHASE_PROGRESS.md`.
