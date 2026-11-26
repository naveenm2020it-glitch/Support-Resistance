from __future__ import annotations

import argparse
import csv
import gzip
import hashlib
import io
import json
import os
import re
import time
import zipfile
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Callable, Iterable

HOSTS = ("archives.nseindia.com", "nsearchives.nseindia.com", "www.nseindia.com")
HEADERS = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
    "Accept": "text/html,application/zip,application/gzip,text/csv,*/*",
    "Accept-Language": "en-US,en;q=0.9",
    "Referer": "https://www.nseindia.com/",
}
DEFAULT_SESSION_DATES = (
    "2026-10-21", "2026-10-22", "2026-10-23", "2026-10-26", "2026-10-27",
    "2026-10-28", "2026-10-29", "2026-10-30", "2026-11-02", "2026-11-03",
    "2026-11-04", "2026-11-05", "2026-11-06", "2026-11-08", "2026-11-09",
    "2026-11-11", "2026-11-12", "2026-11-13", "2026-11-16", "2026-11-17",
    "2026-11-18", "2026-11-19",
)
REPORT_TYPES = ("bhavcopy_udiff", "mii_contract", "nifty50_index")


def parse_session_dates(value: str | Iterable[str]) -> list[str]:
    raw = value.split(",") if isinstance(value, str) else list(value)
    dates = [item.strip() for item in raw if item and item.strip()]
    if not dates:
        raise ValueError("At least one ISO session date is required")
    parsed = []
    for item in dates:
        try:
            parsed.append(date.fromisoformat(item).isoformat())
        except ValueError as exc:
            raise ValueError(f"Invalid session date {item!r}; expected YYYY-MM-DD") from exc
    if len(parsed) != len(set(parsed)):
        raise ValueError("Duplicate session dates are not allowed")
    return parsed


def _report_filename(report_type: str, session_date: str) -> str:
    day = date.fromisoformat(session_date)
    ymd, dmy = day.strftime("%Y%m%d"), day.strftime("%d%m%Y")
    if report_type == "bhavcopy_udiff":
        return f"BhavCopy_NSE_FO_0_0_0_{ymd}_F_0000.csv.zip"
    if report_type == "mii_contract":
        return f"NSE_FO_contract_{dmy}.csv.gz"
    if report_type == "nifty50_index":
        return f"ind_close_all_{dmy}.csv"
    raise ValueError(f"Unsupported report type: {report_type}")


def candidate_urls(report_type: str, session_date: str) -> list[str]:
    filename = _report_filename(report_type, session_date)
    path = "/content/indices/" if report_type == "nifty50_index" else "/content/fo/"
    return [f"https://{host}{path}{filename}" for host in HOSTS]


def _normalise(value: str | None) -> str:
    return "".join(character.lower() for character in (value or "") if character.isalnum())


def _date_matches(value: str, session_date: str) -> bool:
    candidate = value.strip()
    expected = date.fromisoformat(session_date)
    accepted = {
        expected.isoformat(),
        expected.strftime("%d-%m-%Y"),
        expected.strftime("%d-%b-%Y").lower(),
        expected.strftime("%d/%m/%Y"),
        expected.strftime("%d-%m-%y"),
    }
    return candidate.lower() in accepted


def validate_report(path: Path, report_type: str, session_date: str) -> dict:
    if not path.is_file() or path.stat().st_size == 0:
        raise ValueError("Downloaded report is empty or missing")

    if report_type == "bhavcopy_udiff":
        with zipfile.ZipFile(path) as archive:
            corrupt_member = archive.testzip()
            if corrupt_member:
                raise ValueError(f"Corrupt ZIP member: {corrupt_member}")
            members = [name for name in archive.namelist() if name.lower().endswith(".csv")]
            if len(members) != 1:
                raise ValueError(f"Expected exactly one CSV in Bhavcopy ZIP; found {members}")
            with archive.open(members[0]) as source:
                header = next(csv.reader(io.TextIOWrapper(source, encoding="utf-8-sig", newline="")))
            if not header or not any(value.strip() for value in header):
                raise ValueError("Bhavcopy CSV has no usable header")
            return {"container": "zip", "csv_member": members[0], "columns": header}

    if report_type == "mii_contract":
        with gzip.open(path, "rt", encoding="utf-8-sig", newline="") as source:
            header = next(csv.reader(source))
        if not header or not any(value.strip() for value in header):
            raise ValueError("MII GZIP CSV has no usable header")
        return {"container": "gzip", "columns": header}

    if report_type == "nifty50_index":
        with path.open("r", encoding="utf-8-sig", newline="") as source:
            reader = csv.DictReader(source)
            columns = reader.fieldnames or []
            by_name = {_normalise(column): column for column in columns}
            index_column = by_name.get("indexname") or by_name.get("index")
            date_column = by_name.get("indexdate") or by_name.get("date")
            required = {"openindexvalue", "highindexvalue", "lowindexvalue", "closingindexvalue"}
            missing = sorted(required - set(by_name))
            if not index_column or not date_column or missing:
                raise ValueError(f"NIFTY index CSV is missing required columns: {missing}")
            row = next((item for item in reader if (item.get(index_column) or "").strip().upper() == "NIFTY 50"), None)
            if row is None:
                raise ValueError("NIFTY 50 row is absent from index CSV")
            if not _date_matches(row.get(date_column, ""), session_date):
                raise ValueError(f"NIFTY 50 index date does not match requested session {session_date}")
        return {"container": "csv", "columns": columns, "index_row": "NIFTY 50", "date_verified": True}

    raise ValueError(f"Unsupported report type: {report_type}")


def fetch_report(
    session,
    report_type: str,
    session_date: str,
    output_root: Path,
    *,
    attempts_per_host: int = 3,
    sleep_fn: Callable[[float], None] = time.sleep,
) -> dict:
    filename = _report_filename(report_type, session_date)
    target = output_root / "raw" / session_date / filename
    target.parent.mkdir(parents=True, exist_ok=True)
    result = {
        "provider": "NSE public archive host",
        "source_exchange": "NSE",
        "report_type": report_type,
        "report_date": session_date,
        "original_filename": filename,
        "path": str(target),
        "status": "failed",
        "attempts": [],
    }

    for url in candidate_urls(report_type, session_date):
        for attempt_number in range(1, attempts_per_host + 1):
            partial = target.with_name(target.name + ".part")
            try:
                with session.get(url, timeout=(20, 90), stream=True) as response:
                    response.raise_for_status()
                    digest = hashlib.sha256()
                    byte_count = 0
                    prefix = b""
                    with partial.open("wb") as destination:
                        for chunk in response.iter_content(chunk_size=1024 * 1024):
                            if not chunk:
                                continue
                            if len(prefix) < 32:
                                prefix += chunk[: 32 - len(prefix)]
                            destination.write(chunk)
                            digest.update(chunk)
                            byte_count += len(chunk)
                    if byte_count == 0 or prefix.lstrip().lower().startswith((b"<!doctype", b"<html")):
                        raise ValueError("Response was empty or HTML, not a report archive")
                    validation = validate_report(partial, report_type, session_date)
                    os.replace(partial, target)
                    attempt_record = {
                        "url": url,
                        "http_status": response.status_code,
                        "final_url": response.url,
                        "content_type": response.headers.get("Content-Type"),
                        "bytes": byte_count,
                        "sha256": digest.hexdigest(),
                        "attempt": attempt_number,
                        "status": "downloaded",
                    }
                    result.update(attempt_record)
                    result["validation"] = validation
                    result["downloaded_at_utc"] = datetime.now(timezone.utc).isoformat()
                    result["status"] = "downloaded"
                    result["attempts"].append(attempt_record)
                    return result
            except Exception as exc:
                partial.unlink(missing_ok=True)
                result["attempts"].append({
                    "url": url,
                    "attempt": attempt_number,
                    "status": "failed",
                    "error": f"{type(exc).__name__}: {exc}",
                })
                if attempt_number < attempts_per_host:
                    sleep_fn(attempt_number * 2)

    result["error"] = "All official NSE archive host candidates failed validation or download"
    return result


def run_download(session_dates: list[str], output_root: Path) -> dict:
    try:
        import requests
    except ImportError as exc:
        raise RuntimeError('Install the optional downloader dependency with: pip install -e ".[download]"') from exc

    session = requests.Session()
    session.headers.update(HEADERS)
    results = []
    for session_date in session_dates:
        for report_type in REPORT_TYPES:
            item = fetch_report(session, report_type, session_date, output_root)
            results.append(item)
            print(f"{session_date}: {report_type}={item['status']}", flush=True)

    manifest = {
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "source_policy": "Official NSE archive URLs; direct public-file requests; no API credentials or access-control bypass",
        "acquisition_mode": "manual GitHub Actions dispatch or one-time user-requested run",
        "session_dates": session_dates,
        "report_types": list(REPORT_TYPES),
        "results": results,
        "downloaded_count": sum(item["status"] == "downloaded" for item in results),
        "failed_count": sum(item["status"] != "downloaded" for item in results),
        "rights_status": "NOT_ASSESSED — technical download success is not permission to retain or use data",
    }
    output_root.mkdir(parents=True, exist_ok=True)
    (output_root / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    return manifest


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Download NSE EOD report files for explicitly supplied sessions")
    parser.add_argument("--session-dates", default=os.getenv("SESSION_DATES", ",".join(DEFAULT_SESSION_DATES)),
                        help="Comma-separated YYYY-MM-DD trading sessions; supply verified sessions only")
    parser.add_argument("--output-dir", type=Path, default=Path("artifacts/nse-eod"))
    args = parser.parse_args(argv)
    try:
        session_dates = parse_session_dates(args.session_dates)
        manifest = run_download(session_dates, args.output_dir)
    except Exception as exc:
        parser.error(str(exc))
    print(f"Downloaded {manifest['downloaded_count']}/{len(session_dates) * len(REPORT_TYPES)} report files")
    return 1 if manifest["failed_count"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
