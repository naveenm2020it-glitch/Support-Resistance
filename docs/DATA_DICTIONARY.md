# NIFTY Seller Intelligence — Data Dictionary Baseline

**Status:** Phase 1 schema baseline. Fields described as planned are design contracts only; they are not implemented or populated by the current application.

## 1. Implemented records

### ReportManifest

One record for an original source artifact.

| Field | Type | Meaning / rule |
|---|---|---|
| `provider` | string | Delivery/source provider, such as NSE or an authorised NSE data vendor; non-empty |
| `source_exchange` | string | Exchange represented by the report; must be `NSE` in this project |
| `report_type` | string | Stable report-family identifier; non-empty |
| `source_url` | string | Source URL or authorised source identifier; non-empty |
| `original_filename` | string | Original basename; path components are rejected |
| `report_date` | date | Exchange/report date represented by the artifact; datetime values are rejected |
| `downloaded_at` | datetime | Download timestamp with timezone information |
| `size_bytes` | integer | Original artifact size in bytes; non-negative integer, not bool |
| `sha256` | string | 64-character hexadecimal digest of original bytes; canonicalised to lowercase |
| `available_fields` | frozenset[string] | Iterable of non-empty field-name strings actually observed after schema inspection; canonicalised by trimming |
| `validation_status` | AvailabilityStatus | `available`, `partial`, `unavailable`, `pending_eod` or `invalid` |
| `validation_messages` | tuple[string] | Iterable of strings describing validation findings and reasons; plain string inputs are rejected |

The delivery provider and exchange are separate concepts. The model can record an authorised provider name while ensuring the exchange represented is NSE. The model does not itself prove that a provider entitlement is valid; that evidence belongs to the Phase 2 source-access review. The artifact digest is the content reference used by downstream observations. The logical source context remains the provider, exchange, report type and report date; equal bytes do not remove the need to retain that context.

### SessionAvailability

One report-family availability record for one numbered session in an analysis window.

| Field | Type | Meaning / rule |
|---|---|---|
| `session_number` | integer | 1 through 22; bool and non-integer values are rejected; 1–17 baseline, 18–21 recent, 22 selected EOD |
| `session_date` | date | Actual F&O trading date; not inferred from calendar days; datetime values are rejected |
| `report_type` | string | Report family represented by this row |
| `status` | AvailabilityStatus | Availability/validation state for the report |
| `available_fields` | frozenset[string] | Fields actually present in that report |
| `source_filename` | optional string | Original report basename when present |
| `source_report_sha256` | optional string | Original report hash; required with `source_filename` when status is `available` |
| `validation_message` | optional string | Report-specific failure, pending or limitation details; non-string values are rejected |

Record identity within an analysis run is `(session_number, report_type)`; the enclosing analysis-run context scopes this identity. The initial sequence helper handles one report family at a time. A complete report-family matrix and report-specific required-field validation are later validation-layer responsibilities.

### ContractObservation

One observed contract row from a validated source report.

| Field | Type | Meaning / rule |
|---|---|---|
| `session_date` | date | Source observation's F&O session date; datetime values are rejected |
| `underlying` | string | Non-empty symbol, trimmed and uppercased for canonical identity |
| `expiry` | date | Actual contract expiry; do not assume one expiry spans all sessions |
| `strike` | finite number | Positive strike price; bool, NaN and infinity are rejected |
| `option_type` | OptionType | `CE` or `PE` enum value |
| `source_report_sha256` | string | SHA-256 reference to the original report; canonicalised to lowercase |
| `source_record_id` | string | Non-empty native source row/event ID or deterministic source-row locator |
| `oi` | optional integer | Open interest; `None` is missing, zero is an observed zero, negative values are invalid |
| `change_in_oi` | optional integer | Change in OI; negative values are valid |
| `volume` | optional integer | Traded volume; cannot be negative |
| `close` | optional number | Finite, non-negative source close/settlement value; not backfilled |
| `iv` | optional number | Finite, non-negative value only when a valid IV value is genuinely supplied |

Count fields must be integers; booleans and fractional values are rejected. OI and volume cannot be negative; change in OI may be negative. Prices/IV must be finite, non-negative numeric values, not booleans. Underlying is canonicalised on the immutable model at construction so equality, grouping and identity use the same symbol representation. The original source bytes remain available through the report hash and source-record locator.

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
