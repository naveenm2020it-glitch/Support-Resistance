# NIFTY Seller Intelligence — Data Dictionary Baseline

**Status:** Phase 1 schema baseline. Fields described as planned are design contracts only; they are not implemented or populated by the current application.

## 1. Implemented records

### ReportManifest

One record for an original source artifact.

| Field | Type | Meaning / rule |
|---|---|---|
| `provider` | string | Source provider; current model accepts NSE |
| `report_type` | string | Stable report-family identifier; non-empty |
| `source_url` | string | Source URL or authorised source identifier; non-empty |
| `original_filename` | string | Original basename; path components are rejected |
| `report_date` | date | Exchange/report date represented by the artifact |
| `downloaded_at` | datetime | Download timestamp with timezone information |
| `size_bytes` | integer | Original artifact size in bytes; cannot be negative |
| `sha256` | string | 64-character hexadecimal digest of original bytes |
| `available_fields` | frozenset[string] | Fields actually observed after schema inspection |
| `validation_status` | AvailabilityStatus | `available`, `partial`, `unavailable`, `pending_eod` or `invalid` |
| `validation_messages` | tuple[string] | Validation findings and reasons |

The artifact digest is the content reference used by downstream observations. The logical source context remains the provider, report type and report date; equal bytes do not remove the need to retain that context.

### SessionAvailability

One report-family availability record for one numbered session in an analysis window.

| Field | Type | Meaning / rule |
|---|---|---|
| `session_number` | integer | 1 through 22; 1–17 baseline, 18–21 recent, 22 selected EOD |
| `session_date` | date | Actual F&O trading date; not inferred from calendar days |
| `report_type` | string | Report family represented by this row |
| `status` | AvailabilityStatus | Availability/validation state for the report |
| `available_fields` | frozenset[string] | Fields actually present in that report |
| `source_filename` | optional string | Original report filename when present |
| `source_report_sha256` | optional string | Original report hash; required when status is `available` |
| `validation_message` | optional string | Report-specific failure, pending or limitation details |

Record identity within an analysis run is `(session_number, report_type)`. The initial sequence helper handles one report family at a time. A complete report-family matrix and report-specific required-field validation are later validation-layer responsibilities.

### ContractObservation

One observed contract row from a validated source report.

| Field | Type | Meaning / rule |
|---|---|---|
| `session_date` | date | Source observation's F&O session date |
| `underlying` | string | Underlying symbol; non-empty |
| `expiry` | date | Actual contract expiry; do not assume one expiry spans all sessions |
| `strike` | finite number | Positive strike price |
| `option_type` | OptionType | `CE` or `PE` |
| `source_report_sha256` | string | SHA-256 reference to the original report |
| `source_record_id` | string | Native source row/event ID or deterministic source-row locator |
| `oi` | optional integer | Open interest; `None` is missing, zero is an observed zero |
| `change_in_oi` | optional integer | Change in OI; negative values are valid |
| `volume` | optional integer | Traded volume; cannot be negative |
| `close` | optional number | Source close/settlement value; not backfilled |
| `iv` | optional number | Only populated when a valid IV value is genuinely supplied |

Observation identity is `(session_date, underlying, expiry, strike, option_type, source_report_sha256, source_record_id)`. The source report digest plus record locator makes the observation auditable back to original bytes.

## 2. Shared enumerations

### AvailabilityStatus

| Value | Meaning |
|---|---|
| `available` | Source report exists and has passed the checks applied at this layer |
| `partial` | Report exists, but required content or fields are incomplete |
| `unavailable` | No valid report is available |
| `pending_eod` | Final EOD publication is not yet available |
| `invalid` | Integrity, date, format, schema or content validation failed |

### OptionType

| Value | Meaning |
|---|---|
| `CE` | Call option |
| `PE` | Put option |

### AnalysisStatus

| Value | Meaning |
|---|---|
| `complete` | The specific gate that produced this status passed; not necessarily the entire application analysis |
| `partial` | Some required records for that gate are incomplete |
| `blocked` | A structural or dependency condition prevents that gate from passing |

## 3. Planned records — not implemented in Phase 1

| Record | Planned identity | Required dimensions / references |
|---|---|---|
| `AnalysisRun` | Stable analysis-run ID | Underlying, selected analysis date, selected expiry, 22-session manifest, created timestamp, software/config version and run status |
| `OrderEvent` | Provider event ID, scoped by source report | Instrument/contract reference, event timestamp and sequence, order ID, side, event type, price, quantity, source-report hash and source-record ID; only fields supplied by an authorised product |
| `TradeEvent` | Provider trade ID, scoped by source report | Instrument/contract reference, trade timestamp, price, quantity, buy/sell order references where supplied, source-report hash and source-record ID |
| `DerivedMetric` | Run ID + metric key + contract/time scope | Formula/method version, value, unit, input observation/event references, coverage and limitations |
| `AnalyticalSignal` | Run ID + signal type + contract/zone scope | Evidence references, rule/model version, classification, coverage, quality tier and caveats |

These schemas must be refined against actual authorised source specifications before the corresponding parsing or analytical phase begins. Do not infer unavailable order/trade fields or create synthetic historical rows to satisfy a planned schema.

## 4. Data semantics

- Missing values use `None`; they are never silently converted to zero.
- Negative `change_in_oi` is valid. Negative OI or volume is invalid.
- A source report must be retained as original bytes and processed only through a working copy.
- Derived records must refer to the original source report and the exact observations/events used.
- A missing historical session or source field cannot be filled from a neighbouring date, live data, interpolation or simulation.
