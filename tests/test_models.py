from datetime import date, datetime, timezone

import pytest

from nifty_seller_intelligence.models import ContractObservation, OptionType, ReportManifest


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


def test_report_manifest_accepts_timezone_aware_source_metadata():
    manifest = make_manifest()
    assert manifest.provider == "NSE"
    assert manifest.available_fields == frozenset({"strike", "expiry"})


@pytest.mark.parametrize("filename", [r"folder\\report.zip", r"folder\report.zip", "../report.zip", "folder/report.zip"])
def test_report_manifest_rejects_path_components(filename):
    with pytest.raises(ValueError, match="basename"):
        make_manifest(original_filename=filename)


def test_report_manifest_rejects_timezone_naive_download_time():
    with pytest.raises(ValueError, match="timezone"):
        make_manifest(downloaded_at=datetime(2026, 11, 26, 9, 30))


def test_contract_observation_requires_source_lineage():
    with pytest.raises(ValueError, match="source_record_id"):
        ContractObservation(date(2026, 11, 25), "NIFTY", date(2026, 12, 3), 26000, OptionType.CE, VALID_SHA256, "", 100, 0, 50, 125.0)


def test_missing_and_zero_values_remain_distinct():
    row = ContractObservation(date(2026, 11, 25), "NIFTY", date(2026, 12, 3), 26000, OptionType.CE, VALID_SHA256, "row:1", None, None, 0, 0.0, None)
    assert row.oi is None
    assert row.volume == 0
    assert row.close == 0.0
    assert row.iv is None


def test_contract_observation_rejects_non_finite_strike():
    with pytest.raises(ValueError, match="finite positive"):
        ContractObservation(date(2026, 11, 25), "NIFTY", date(2026, 12, 3), float("nan"), OptionType.CE, VALID_SHA256, "row:1", 100, 0, 50, 125.0)
