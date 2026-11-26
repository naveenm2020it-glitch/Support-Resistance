import csv
import gzip
import hashlib
import io
import zipfile
from pathlib import Path

import pytest

from nifty_seller_intelligence.nse_download import (
    candidate_urls,
    fetch_report,
    parse_session_dates,
    validate_report,
)


def make_bhavcopy_zip() -> bytes:
    output = io.BytesIO()
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("BhavCopy.csv", "FinInstrmId,TckrSymb,OptnTp\n1,NIFTY,CE\n")
    return output.getvalue()


def test_parse_session_dates_rejects_duplicates_and_invalid_dates():
    assert parse_session_dates("2026-11-18,2026-11-19") == ["2026-11-18", "2026-11-19"]
    with pytest.raises(ValueError, match="Duplicate"):
        parse_session_dates("2026-11-19,2026-11-19")
    with pytest.raises(ValueError, match="Invalid"):
        parse_session_dates("19-11-2026")


def test_candidate_urls_use_three_official_host_fallbacks():
    urls = candidate_urls("bhavcopy_udiff", "2026-11-19")
    assert urls == [
        "https://archives.nseindia.com/content/fo/BhavCopy_NSE_FO_0_0_0_20261119_F_0000.csv.zip",
        "https://nsearchives.nseindia.com/content/fo/BhavCopy_NSE_FO_0_0_0_20261119_F_0000.csv.zip",
        "https://www.nseindia.com/content/fo/BhavCopy_NSE_FO_0_0_0_20261119_F_0000.csv.zip",
    ]


def test_validate_bhavcopy_zip_checks_integrity_and_csv_header(tmp_path):
    path = tmp_path / "bhavcopy.zip"
    path.write_bytes(make_bhavcopy_zip())
    result = validate_report(path, "bhavcopy_udiff", "2026-11-19")
    assert result["container"] == "zip"
    assert result["columns"] == ["FinInstrmId", "TckrSymb", "OptnTp"]


def test_validate_mii_gzip_checks_header(tmp_path):
    path = tmp_path / "mii.csv.gz"
    path.write_bytes(gzip.compress(b"FinInstrmId,TckrSymb,FinInstrmNm\n100,NIFTY,OPTIDX\n"))
    result = validate_report(path, "mii_contract", "2026-11-19")
    assert result["container"] == "gzip"
    assert "FinInstrmId" in result["columns"]


def test_validate_index_csv_requires_nifty50_and_matching_date(tmp_path):
    path = tmp_path / "index.csv"
    path.write_text(
        "Index Name,Index Date,Open Index Value,High Index Value,Low Index Value,Closing Index Value\n"
        "NIFTY 50,19-Nov-2026,26000,26100,25900,26050\n",
        encoding="utf-8",
    )
    assert validate_report(path, "nifty50_index", "2026-11-19")["date_verified"] is True
    with pytest.raises(ValueError, match="does not match"):
        validate_report(path, "nifty50_index", "2026-11-18")


class FakeResponse:
    def __init__(self, body: bytes, status: int, url: str):
        self.body = body
        self.status_code = status
        self.url = url
        self.headers = {"Content-Type": "application/zip"}
    def __enter__(self):
        return self
    def __exit__(self, *args):
        return False
    def raise_for_status(self):
        if self.status_code >= 400:
            raise RuntimeError(f"HTTP {self.status_code}")
    def iter_content(self, chunk_size):
        yield self.body


class FakeSession:
    def __init__(self, response_by_url):
        self.response_by_url = response_by_url
        self.urls = []
    def get(self, url, **kwargs):
        self.urls.append(url)
        return self.response_by_url[url]


def test_fetch_falls_back_to_second_official_host_and_records_sha256(tmp_path):
    urls = candidate_urls("bhavcopy_udiff", "2026-11-19")
    body = make_bhavcopy_zip()
    fake = FakeSession({
        urls[0]: FakeResponse(b"unavailable", 503, urls[0]),
        urls[1]: FakeResponse(body, 200, urls[1]),
    })
    result = fetch_report(fake, "bhavcopy_udiff", "2026-11-19", tmp_path, attempts_per_host=1)
    assert result["status"] == "downloaded"
    assert fake.urls == urls[:2]
    assert result["sha256"] == hashlib.sha256(body).hexdigest()
    assert Path(result["path"]).read_bytes() == body
