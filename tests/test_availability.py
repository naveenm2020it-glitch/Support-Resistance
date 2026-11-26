from datetime import date, timedelta

from nifty_seller_intelligence.availability import assert_required_fields, validate_22_sessions
from nifty_seller_intelligence.models import AnalysisStatus, AvailabilityStatus, SessionAvailability


def make_sessions(selected=date(2026, 11, 25), missing=None):
    # Fixture dates stand in for an exchange calendar already verified by the caller.
    dates = [selected - timedelta(days=21 - i) for i in range(22)]
    rows = []
    for n, session_date in enumerate(dates, 1):
        status = AvailabilityStatus.PENDING_EOD if n == missing else AvailabilityStatus.AVAILABLE
        rows.append(SessionAvailability(n, session_date, "udiff_bhavcopy", status, frozenset({"strike", "expiry", "option_type", "oi", "volume"})))
    return rows


def test_all_22_available_can_pass_core_gate():
    rows = make_sessions()
    result = validate_22_sessions(rows, rows[-1].session_date)
    assert result.status is AnalysisStatus.COMPLETE
    assert len(result.sessions) == 22


def test_missing_session_cannot_be_complete():
    rows = make_sessions(missing=22)
    result = validate_22_sessions(rows, rows[-1].session_date)
    assert result.status is not AnalysisStatus.COMPLETE
    assert result.missing_or_invalid[0].session_number == 22


def test_missing_required_field_is_reported_not_filled():
    assert assert_required_fields({"strike", "expiry", "oi"}, {"strike", "expiry", "oi", "iv"}) == ("iv",)


def test_duplicate_dates_fail_gate():
    rows = make_sessions()
    rows[-1] = SessionAvailability(22, rows[-2].session_date, "udiff_bhavcopy", AvailabilityStatus.AVAILABLE)
    result = validate_22_sessions(rows, rows[-2].session_date)
    assert result.status is AnalysisStatus.BLOCKED
