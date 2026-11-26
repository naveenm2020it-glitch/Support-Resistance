from datetime import date, datetime, timezone

import pytest

from nifty_seller_intelligence.models import ContractObservation, OptionType, ReportManifest, SessionAvailability, AvailabilityStatus


VALID_SHA256 = "a" * 64


def make_manifest(**overrides):
    values = {
        "provider": "NSE",
        "report_type": "udiff_bhavcopy",
        "source_url": "authorised-source:nse/udiff",
        "original_filename": "NIFTY_2026-11-25.zip",
        "report_date": date(2026, 11, 25),
        "downloaded_at": datetime(2026, 11, 26, 9, 30, tzinfo=timezone.utc),
        "size_bytes": 1024,
        "sha256": VALID_SHA256,
        "available_fields": {"strike", "expiry"},
    }
    values.update(overrides)
    return ReportManifest(**values)


def make_observation(**overrides):
    values = {
        "session_date": date(2026, 11, 25),
        "underlying": "NIFTY",
        "expiry": date(2026, 12, 3),
        "strike": 26000,
        "option_type": OptionType.CE,
        "source_report_sha256": VALID_SHA256,
        "source_record_id": "row:1",
        "oi": 100,
        "change_in_oi": -10,
        "volume": 50,
        "close": 125.0,
        "iv": None,
    }
    values.update(overrides)
    return ContractObservation(**values)


def test_report_manifest_accepts_timezone_aware_source_metadata():
    manifest = make_manifest()
    assert manifest.provider == "NSE"
    assert manifest.source_exchange == "NSE"
    assert manifest.available_fields == frozenset({"strike", "expiry"})


def test_report_manifest_distinguishes_authorised_provider_from_exchange():
    manifest = make_manifest(provider="Licensed NSE Data Vendor")
    assert manifest.provider == "Licensed NSE Data Vendor"
    assert manifest.source_exchange == "NSE"


def test_report_manifest_normalizes_source_exchange_and_digest():
    manifest = make_manifest(source_exchange=" nse ", sha256=VALID_SHA256.upper())
    assert manifest.source_exchange == "NSE"
    assert manifest.sha256 == VALID_SHA256


def test_report_manifest_rejects_non_nse_exchange():
    with pytest.raises(ValueError, match="source_exchange"):
        make_manifest(source_exchange="BSE")


@pytest.mark.parametrize("filename", [r"folder\\report.zip", r"folder\report.zip", "../report.zip", "folder/report.zip"])
def test_report_manifest_rejects_path_components(filename):
    with pytest.raises(ValueError, match="basename"):
        make_manifest(original_filename=filename)


def test_report_manifest_rejects_timezone_naive_download_time():
    with pytest.raises(ValueError, match="timezone"):
        make_manifest(downloaded_at=datetime(2026, 11, 26, 9, 30))


@pytest.mark.parametrize("size", [1.5, True, "1024"])
def test_report_manifest_requires_integer_file_size(size):
    with pytest.raises(ValueError, match="non-negative integer"):
        make_manifest(size_bytes=size)


def test_contract_observation_requires_source_lineage():
    with pytest.raises(ValueError, match="source_record_id"):
        make_observation(source_record_id="")


def test_missing_and_zero_values_remain_distinct():
    row = make_observation(oi=None, change_in_oi=None, volume=0, close=0.0, iv=None)
    assert row.oi is None
    assert row.volume == 0
    assert row.close == 0.0
    assert row.iv is None


def test_contract_observation_normalizes_underlying_and_digest():
    row = make_observation(underlying=" nifty ", source_report_sha256=VALID_SHA256.upper())
    assert row.underlying == "NIFTY"
    assert row.source_report_sha256 == VALID_SHA256


def test_contract_observation_rejects_non_finite_strike():
    with pytest.raises(ValueError, match="finite positive"):
        make_observation(strike=float("nan"))


@pytest.mark.parametrize("field,value", [("oi", 1.5), ("change_in_oi", True), ("volume", 2.5)])
def test_contract_observation_requires_integer_counts(field, value):
    with pytest.raises(ValueError, match=f"{field} must be an integer"):
        make_observation(**{field: value})


@pytest.mark.parametrize("field,value", [("close", -0.01), ("iv", -1.0), ("close", float("inf"))])
def test_contract_observation_rejects_invalid_price_or_iv(field, value):
    with pytest.raises(ValueError):
        make_observation(**{field: value})


def test_session_availability_rejects_boolean_session_number():
    with pytest.raises(ValueError, match="integer between"):
        SessionAvailability(True, date(2026, 11, 25), "udiff_bhavcopy", AvailabilityStatus.PENDING_EOD)
