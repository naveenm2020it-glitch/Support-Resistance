# NIFTY Seller Intelligence — Architecture Baseline

**Status:** Phase 1 architecture boundary review completed; implementation remains limited to the foundation modules described below.  
**Working branch:** `feature/nifty-seller-intelligence-foundation`

## 1. Architecture objectives

The application is a provenance-first research system. Every reported market observation must be traceable to an authorised source artifact and an identifiable source record. Missing source data must remain missing. A calculated metric or interpretation must be distinguishable from an exchange-reported value.

The initial architecture is modular and phase-gated. Source acquisition, raw-file retention, parsing, analytics, machine learning and deployment are separate responsibilities. A later phase must not be treated as operational merely because an earlier phase has created its data contracts.

## 2. Logical components and ownership

| Component | Responsibility | Planned phase | Current state |
|---|---|---:|---|
| Typed domain models | Define report, session and contract-observation records | 1 | Initial models implemented |
| Session-sequence validation | Validate numbering, date order and availability for one report family | 1 | Initial helper implemented |
| Multi-strike grouping | Group actual observations by underlying, expiry, strike and CE/PE | 1 | Initial helper implemented |
| Source catalogue and access verification | Document report families, entitlement and historical coverage | 2 | Not implemented |
| Trading-session calendar | Resolve 22 actual completed F&O sessions | 3 | Not implemented |
| Historical collector | Acquire authorised reports and preserve original bytes | 4 | Not implemented |
| Temporary artifact lifecycle | Apply and audit the five-hour temporary retention requirement | 5 | Not implemented |
| Permanent archive and provenance | Preserve raw reports and verify transfers by checksum | 6 | Not implemented |
| Parsing and full report validation | Parse source formats, link source rows and enforce report/field coverage | 7 | Not implemented |
| CE/PE order-flow analysis | Build multi-strike metrics from validated observations and entitled event data | 8 | Not implemented |
| Seller and panic-risk assessment | Produce qualified signals and support/resistance zones | 9–10 | Not implemented |
| Machine learning | Train and evaluate only on an eligible validated history | 11 | Not implemented |
| Dashboard and deployment | Present results, monitor and deploy after acceptance | 12 | Not implemented |

## 3. Data flow and trust boundaries

1. **Authorised source boundary:** NSE reports or another explicitly authorised provider are acquired only after access terms and delivery method are verified. Public-site scraping and access-control workarounds are outside the design.
2. **Immutable raw artifact boundary:** downloaded bytes and original filename are preserved unchanged. The SHA-256 digest and source metadata identify the artifact. Raw market data is not committed to ordinary Git history.
3. **Working-copy boundary:** extraction and transformation operate on disposable working copies, never on the archived original.
4. **Normalisation boundary:** parsers convert source rows into typed records. Each observation retains the source report hash and a stable source-record locator.
5. **Validation boundary:** report date, format, integrity, schema, required fields, contract dimensions and session coverage are checked. Missing values are not replaced with zero or values from another date.
6. **Analytics boundary:** derived metrics retain their input/source references, formula or method version, coverage and limitations. OI and volume are aggregate evidence, not proof of an individual seller's identity or intent.
7. **Presentation boundary:** outputs distinguish measured observations, derived metrics and qualified interpretations. An incomplete dataset cannot be presented as a complete analysis.

## 4. Current foundation contract boundaries

- `ReportManifest` records the delivery provider separately from `source_exchange` (fixed to NSE for this project), plus report family, source reference, original filename, report date, timezone-aware download time, file size, SHA-256, observed fields and validation outcome.
- `SessionAvailability` records one report family's availability for one numbered session. An `AVAILABLE` record must refer to an original filename and a valid source-report SHA-256.
- `ContractObservation` records the contract dimensions and EOD values available in the source. It requires both the source-report SHA-256 and a source-record identifier.
- `validate_22_sessions` validates a 22-record sequence for **one report family**. It is not the complete session-by-report availability matrix and does not by itself authorise a complete seller analysis. Later report-coverage and required-field gates must pass as well.
- `group_observations_by_strike` groups only supplied observations. It does not create missing strikes, dates, IV values, quotes or order-book events.

The field-level `assert_required_fields` helper reports absent fields. A later parser/validation layer must apply the applicable report-specific required-field catalogue before a full analysis can be accepted.

## 5. Identity and provenance rules

- A source artifact is identified by its source context `(provider, source_exchange, report_type, report_date)` plus its SHA-256 content digest. The digest identifies bytes; the context identifies why/where those bytes were obtained. Provider entitlement itself must be verified in Phase 2.
- A session/report availability item is identified by its session number and report type within the selected 22-session run.
- A contract observation is identified using session date, canonical uppercase underlying, expiry, strike, option type, source-report SHA-256 and source-record ID.
- If a source format has no native row ID, the parser must use a deterministic locator such as a one-based source row number, scoped to the source-report hash.
- Derived records must carry references to their input observations/reports and a versioned calculation or methodology identifier. These derived-record models are not implemented in Phase 1.

## 6. Missing data and numerical semantics

`None` means the source did not supply a value. Numeric zero is an observed value and must remain distinguishable from `None`. A negative change in OI is valid; OI and traded volume themselves cannot be negative. Strike must be finite and positive. Fields such as IV, bid/ask, depth and order events remain unavailable unless the authorised source actually supplies them.

## 7. Runtime and configuration boundaries

- Python package: `src/nifty_seller_intelligence/`.
- Automated tests: `tests/`.
- Non-secret configuration and future report catalogues: `config/` (planned; no configuration module is implemented in Phase 1).
- Technical documentation and phase evidence: `docs/`.
- CI: `.github/workflows/`.
- Secrets belong in approved secret storage, not source files, manifests, commits or logs.
- Raw NSE report artifacts and permanent market-data archives must remain outside normal Git commits.

## 8. Test and acceptance strategy

The foundation suite checks model invariants, provenance references, missing-versus-zero semantics, 22-session sequence checks and multi-strike grouping. CI runs the same pytest suite on pushes to the working feature branch and pull requests targeting `main`.

Passing unit tests is necessary but not sufficient for Phase 1 acceptance. Phase 1 also requires architecture/data-contract review and explicit evidence that the tests ran. It does not verify NSE data entitlement, historical report availability, data acquisition, archive retention, order-book reconstruction, trading performance or deployment.

## 9. Phase 1 architecture boundary review — 2026-11-26

**Review result: PASS for the current foundation scope.** The following boundaries match the implementation and the approved phase plan:

- The current Python modules are pure data/model and in-memory helper code. They do not perform network requests, access credentials, download reports, write raw files, connect to a database or run market analysis.
- `ReportManifest` represents source-artifact metadata. It does not download a report or independently prove that its bytes match the recorded digest; byte-level hashing/verification belongs to the acquisition/archive implementation phases.
- `SessionAvailability` represents one report family for one numbered session. The sequence helper handles one report family at a time. The complete report-by-session matrix and report-specific mandatory fields are explicitly deferred to later validation work.
- `ContractObservation` is an observed source-row contract, not a synthetic option-chain quote. Missing fields remain `None`; source-report and source-record references preserve the intended lineage.
- Multi-strike grouping is an in-memory grouping function over rows already supplied. It does not imply a historical collector, a trading-session calendar or a seller-intelligence engine exists.
- The architecture/data dictionary describe planned downstream records without claiming those records or services are already implemented.
- The CI workflow runs tests only. It has no NSE credentials, external data acquisition, archive, scheduled collector or deployment steps.

**Architecture decision:** keep the current Phase 1 package deliberately small and side-effect free. Implement source-specific acquisition, format-dependent schemas, report matrix validation, derived metrics and analytics only in their approved later phases, after their dependencies are verified.

## 10. Phase 1 data-model identity review — 2026-11-26

**Review result: PASS for the foundation record scope.** The data dictionary and implementation now distinguish delivery provider from the represented exchange, scope session/report records to an analysis run, and identify each contract observation by contract dimensions plus source artifact and source-row lineage. Runtime checks enforce the documented date, numeric, enum, checksum and canonical-symbol constraints.

The model layer does not claim to prove provider entitlement, verify downloaded bytes against a digest, supply a native exchange row identifier or define source-specific mandatory fields. Those are external evidence/parsing responsibilities for later phases.

**Remaining Phase 1 gates:** native GitHub Actions execution evidence and final Phase 1 acceptance. These are not implied by architecture or data-model review.
