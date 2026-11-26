"""Strict 22-session completeness gate; this module does not discover/download sessions."""
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
    """Validate ordered 17+4+1 sessions for a selected completed EOD date."""
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
                                 f"Analysis not complete: {len(bad)} of 22 sessions are not fully available")
    return AvailabilityResult(AnalysisStatus.COMPLETE, rows, (),
                              "All 22 core session reports are marked available; verify required field coverage before ML.")


def assert_required_fields(actual_fields: set[str] | frozenset[str], required_fields: set[str] | frozenset[str]) -> tuple[str, ...]:
    """Return absent fields; never synthesize them."""
    return tuple(sorted(set(required_fields) - set(actual_fields)))
