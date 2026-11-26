# Development and Data Workflow

This document defines how project code, report data and phase progress are handled. The phase-by-phase delivery plan is in `docs/IMPLEMENTATION_ROADMAP.md`; current status is in `docs/PHASE_PROGRESS.md`.

## 1. Development workflow

1. Work only in `naveenm2020it-glitch/Support-Resistance`.
2. Use `feature/nifty-seller-intelligence-foundation` as the active development branch unless a separately approved branch strategy replaces it.
3. Keep `main` unchanged until a separate merge/release approval is given.
4. Before starting a phase, record its scope, dependencies, expected files and acceptance criteria in `docs/PHASE_PROGRESS.md`.
5. Implement backend code under `src/nifty_seller_intelligence/`, tests under `tests/`, non-secret configuration under `config/`, Android client source in a separately defined Android project directory, and technical documentation under `docs/`. Do not create a web dashboard as the primary UI. Phase 12's Android/backend work remains unauthorised until the user approves the revised plan.
6. Make small, descriptive commits. Record commit SHAs, test commands and actual results in the progress register.
7. Update progress whenever a meaningful work item is completed, a blocker appears, or a phase enters review/completion.
8. Mark a phase COMPLETED only after all acceptance criteria and required tests pass and the evidence is recorded.
9. Do not claim tests, downloads, source access, archival, cleanup or deployment succeeded unless they were actually executed and verified.

## 2. Phase gates

Follow the 12 phases in `docs/IMPLEMENTATION_ROADMAP.md`:
1. Foundation and architecture.
2. NSE data access and source verification.
3. 22-session calendar engine.
4. Automatic historical report collection and validation through an authorised source route; until such access and rights are verified, collection is blocked or limited to a clearly labelled permitted manual/import process.
5. Temporary GitHub storage and five-hour cleanup.
6. Permanent archive and provenance.
7. Parsing and validation of permitted EOD reports (order/trade linking only if a separately authorised source is later approved).
8. Multiple-strike CE/PE EOD analysis; order-flow is excluded without separately authorised intraday event data.
9. Seller intelligence engine.
10. EOD positioning-risk proxies; intraday panic-exit detection is excluded without authorised event data.
11. Machine learning.
12. Android application, testing, APK build/release and separate backend deployment (proposed; pending user approval).

Do not skip a blocked data-access, usage-rights, archive or validation dependency by inventing or substituting data. The previously documented no-subscription mode uses user-supplied EOD files only, after permitted use is confirmed. The proposed target is automatic collection only through an authorised source; it does not permit public-site scraping or circumventing access controls. A separate one-off experimental downloader was added at the user's explicit request; it is not an approved source route and is documented in `docs/NSE_DOWNLOAD_EXPERIMENT.md`. Do not schedule or repeatedly run it; NSE terms may prohibit this automated retrieval. The Android/backend implementation revision remains pending separate user approval.

## 3. GitHub code and progress storage

- Store source code, tests, safe configuration templates, documentation and progress records in this repository.
- The working progress record is `docs/PHASE_PROGRESS.md`.
- The detailed plan is `docs/IMPLEMENTATION_ROADMAP.md`.
- Each meaningful phase change must be traceable to commit(s) and test evidence.
- Do not commit raw NSE reports or market-data archives to normal Git history. Git deletion does not erase earlier committed copies.
- Never commit credentials, API keys, access tokens, private keys or secret configuration.
- Keep `main` untouched until explicitly approved.

## 4. 22-session data workflow

1. User selects underlying (NIFTY), analysis date and expiry.
2. Resolve 22 valid completed F&O trading sessions using a verified exchange calendar and actual report dates.
3. Assign sessions 1–17 to the baseline, 18–21 to recent changes and 22 to the selected completed EOD session.
4. Identify required official reports for each date using the verified report catalogue.
5. The proposed product target is automatic discovery and collection of all applicable reports for the 22 sessions, using only an authorised source/API/feed. Do not automate public-page collection, scrape, or bypass access controls. Until an authorised route and rights are verified, the automated target remains blocked; a permitted manual/import fallback must be labelled clearly.
6. Before collecting, importing or retaining any report, confirm the source terms permit the acquisition, storage, ML training and trading/signal use. A public download control alone is not a data-use licence.
7. Collect or import only permitted original files; record source/date/file metadata and SHA-256.
8. Keep persistent raw storage, GitHub temporary artifacts and permanent archive disabled until the Phase 2 written data-use decision and later storage-phase safeguards are approved.
9. Validate date, archive integrity, checksum, format, schema and required fields.
10. Parse working copies only; preserve original raw files unchanged.
11. Link order/trade events and classify aggressive flow or passive seller absorption only when a separately authorised source provides sufficiently complete, correctly sequenced data. Under the current documented no-subscription EOD data set, these outputs remain unavailable; do not infer them from EOD OI/price/volume.
12. Normalise underlying, expiry, strike, option type and session date while preserving source values and units.
13. Build multi-strike CE and PE datasets from observed rows.
14. Check 22-session coverage and required fields before full analysis.
15. Compare sessions 1–17, sessions 18–21 and session 22.
16. Produce evidence-qualified positioning assessments and support/resistance levels with limitations; do not claim seller identity or infer unsupported order flow or intraday panic exits.
17. Permit ML only after its longer-history eligibility and evaluation requirements pass.
18. Generate an output that includes source coverage, provenance, calculations, findings and limitations.

## 5. Completion gate

Analysis status is COMPLETE only when all 22 target sessions have validated required core reports, required fields are present, and source acquisition/storage/analysis rights are confirmed. Otherwise status is PARTIAL or BLOCKED with exact session/field/rights failures. A data-coverage or rights failure must never be disguised as a successful analysis. ML must not run under a false COMPLETE status or on data whose ML/trading use is not permitted.

## 6. 17 + 4 + 1

- Long-term baseline: sessions 1–17.
- Recent comparison: sessions 18–21.
- Selected EOD: session 22.
- All groups refer to 22 distinct completed F&O sessions.
- If selected-date EOD data has not been published and validated, session 22 is pending and analysis cannot be complete.

## 7. Seller-intelligence evidence hierarchy

1. In the current no-subscription EOD mode: contract-level EOD price, settlement, traded activity and OI/change-in-OI where present.
2. NIFTY index EOD context and adjacent-strike/expiry-aligned comparisons.
3. Optional participant-category aggregates, with their scope limitations stated.
4. Reconstructed aggressive flow, passive sell-side replenishment and order-book evidence are excluded unless a separately authorised event-data source is approved.

Do not identify a seller or claim a new short solely from OI, price or volume. An execution always has both sides; aggressor classification requires adequate order-book evidence. Missing or ambiguous evidence must remain unclassified.

## 8. Five-hour temporary storage safety

- Start an expiry clock separately for each temporary report after successful upload.
- Do not assume GitHub artifact native retention provides five-hour expiry; verify the chosen mechanism.
- Cleanup must be automated and auditable.
- Do not delete a report while processing is active or before the required permanent archive safeguards pass.
- If the expiry is reached but archival or processing safeguards failed, raise and record an operational exception rather than silently losing the only copy.
- Permanent raw archive files, processed outputs and provenance records are outside the temporary cleanup scope.

## 9. Security and data integrity

- Keep secrets in approved secret-management facilities.
- Never expose credentials or tokens in commits, manifests or logs.
- Never scrape NSE public pages or bypass access controls without authorisation.
- Never fabricate, estimate or backfill missing historical NSE reports or option-chain observations.
- Preserve original files unchanged and process working copies only.
- Keep report source lineage through parsing and analysis.
