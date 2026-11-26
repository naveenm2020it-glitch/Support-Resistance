# Configuration Standards

**Status:** Phase 1 convention baseline. This directory contains documentation only; runtime configuration loading is not implemented yet.

## 1. File format and ownership

- Use TOML for future non-secret application configuration. Python 3.11 provides the standard-library `tomllib` reader, so a TOML parser is not an additional runtime dependency.
- Keep committed defaults and examples non-secret. Do not commit a populated production configuration file.
- Keep future source/report catalogues separate from generic application settings. Define the actual report catalogue only after authorised source products and their field specifications have been verified in Phase 2.
- Do not put NSE report bytes, historical market data, account details, credentials, API keys, access tokens, private keys or signed URLs in this directory.

## 2. Schema and validation

- Every runtime configuration document must declare an integer `config_schema_version`.
- Parse configuration into typed settings and validate it at application startup, before any data operation.
- Reject unknown keys, malformed values, invalid enum names and missing mandatory settings. Do not silently ignore invalid configuration.
- Separate required settings from optional settings. Optional defaults must be documented and deterministic; never default a missing market observation, report, session date, exchange field or analytical input.
- Validate paths and retention values against their owning component's rules. Do not let a configuration value bypass source permissions, five-hour artifact retention safeguards or permanent-archive safeguards.
- Record the non-secret configuration schema version and SHA-256 of the effective canonical configuration in each future analysis-run manifest. Do not include secret values in the hash input unless they are represented by stable secret-version identifiers.

## 3. Secret handling

- Load secrets from the approved secret manager or process environment at runtime.
- In GitHub Actions, use repository/environment secrets or OIDC-backed access where supported. Grant only the minimum permissions required.
- Never print secrets, environment dumps, authorization headers, credentials embedded in URLs or signed query parameters to logs.
- A manifest may record a secret reference/version label for auditability, but never the secret value.

## 4. Versioning and reproducibility

- The Python package version is maintained in `pyproject.toml` using semantic versioning. Current foundation version: `0.1.0`.
- Increment the patch component for backward-compatible fixes, the minor component for backward-compatible capabilities, and the major component for incompatible public data/API contract changes.
- Version configuration schemas independently from the package version.
- Future analysis runs must record package version, configuration schema version and effective configuration hash, source-catalogue version, parser version and analytics/methodology version where those components are used.
- Derived metrics/signals must identify their calculation or methodology version and the source observations/reports used. A rerun must not silently mix outputs from different methodology versions.

## 5. Logging and operational errors

- Use Python's standard `logging` package and module-level loggers. The logging implementation/configuration is not part of Phase 1.
- Emit structured, searchable log records. Where available, include timestamp, severity, component, run ID, report type, session date, outcome and a concise error code.
- Log state transitions and actionable diagnostics; do not log raw report contents, entire market-data payloads, secrets, tokens or private URLs.
- Keep operational logs separate from immutable source manifests and durable provenance/audit records. Logs are not a substitute for report manifests or checksums.
- Distinguish configuration errors, source unavailability, pending EOD, integrity failures, schema/validation failures and unexpected application defects.
- Fail closed for integrity, schema, permission and mandatory-data failures. Never convert failures to an empty successful result or substitute another session/live data.
- Future collector retries may apply only to explicitly classified transient failures, with bounded attempts, backoff and an audit record. Never retry malformed data or permanent permission/entitlement failures as though they were transient.

## 6. Testing conventions

- Unit tests must be deterministic, isolated and offline. Use small synthetic fixtures only to test structural rules; label them as test fixtures, never as NSE market observations.
- Do not call NSE, a broker, a licensed provider or external network service from ordinary unit tests.
- Integration tests requiring source-specific samples must be introduced only after the relevant source-access phase authorises the sample and defines its handling.
- Test missing, partial, unavailable, invalid and pending states separately. Include negative tests for fabricated substitutions, schema errors and provenance loss.
- Keep the same test command in local development and CI: `python -m pytest -q`.

## 7. Phase boundary

These standards define conventions only. They do not add a runtime config loader, secrets integration, report catalogue, logger setup, downloader, archive, scheduler or data provider. Implement those capabilities only in their approved phases and record the actual evidence in `docs/PHASE_PROGRESS.md`.
