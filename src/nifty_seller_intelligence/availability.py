"""Validate the 22-session sequence; this is not the final report-coverage gate."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from typing import Iterable

from .models import AnalysisStatus, AvailabilityStatus, SessionAvailability


@dataclass(frozen=True)
class AvailabilityResult:
    status: AnalysisStatus
    sessions: tuple[SessionAvailability, ...]
    missing_or_invalid: tuple[SessionAvailability, ...]
    message: str


def validate_22_sessions(sessions: Iterable[SessionAvailability], selected_date: date) -> AvailabilityResult:
    """Validate the numbered 17+4+1 session sequence for one report family."""
    rows = tuple(sorted(sessions, key=lambda item: item.session_number))
    if len(rows) != 22:
        return AvailabilityResult(AnalysisStatus.BLOCKED, rows, rows,
                                 f"Expected 22 session records; received {len(rows)}")
    if tuple(row.session_number for row in rows) != tuple(range(1, 23)):
        return AvailabilityResult(AnalysisStatus.BLOCKED, rows, rows,
                                 "Session numbers must be exactly 1 through 22")
    dates = tuple(row.session_date for row in rows)
    if len(set(dates)) != 22 or dates[-1] != selected_date:
        return AvailabilityResult(AnalysisStatus.BLOCKED, rows, rows,
                                 "Sessions must have unique dates and session 22 must equal selected_date")
    if tuple(sorted(dates)) != dates:
        return AvailabilityResult(AnalysisStatus.BLOCKED, rows, rows,
                                 "Session dates must be chronological")
    bad = tuple(row for row in rows if row.status is not AvailabilityStatus.AVAILABLE)
    if bad:
        state = AnalysisStatus.PARTIAL if len(bad) < 22 else AnalysisStatus.BLOCKED
        return AvailabilityResult(state, rows, bad,
                                 f"Session sequence is incomplete: {len(bad)} of 22 report records are not available")
    return AvailabilityResult(AnalysisStatus.COMPLETE, rows, (),
                              "All 22 records for this report family are available; required report-family coverage and field validation remain separate gates.")


def assert_required_fields(actual_fields: set[str] | frozenset[str], required_fields: set[str] | frozenset[str]) -> tuple[str, ...]:
    """Return absent fields; never synthesize them."""
    return tuple(sorted(set(required_fields) - set(actual_fields)))
