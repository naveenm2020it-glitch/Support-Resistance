"""Core typed models. Missing source data is represented explicitly, never imputed."""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date, datetime
from enum import Enum
import math
from typing import FrozenSet, Optional


def _is_sha256(value: str) -> bool:
    return isinstance(value, str) and len(value) == 64 and all(char in "0123456789abcdefABCDEF" for char in value)


def _is_date(value: object) -> bool:
    return isinstance(value, date) and not isinstance(value, datetime)


def _is_number(value: object) -> bool:
    if not isinstance(value, (int, float)) or isinstance(value, bool):
        return False
    try:
        return math.isfinite(value)
    except OverflowError:
        return False


class AvailabilityStatus(str, Enum):
    AVAILABLE = "available"
    PARTIAL = "partial"
    UNAVAILABLE = "unavailable"
    PENDING_EOD = "pending_eod"
    INVALID = "invalid"


class OptionType(str, Enum):
    CE = "CE"
    PE = "PE"


class AnalysisStatus(str, Enum):
    COMPLETE = "complete"
    PARTIAL = "partial"
    BLOCKED = "blocked"


@dataclass(frozen=True)
class ReportManifest:
    provider: str
    report_type: str
    source_url: str
    original_filename: str
    report_date: date
    downloaded_at: datetime
    size_bytes: int
    sha256: str
    source_exchange: str = "NSE"
    available_fields: FrozenSet[str] = field(default_factory=frozenset)
    validation_status: AvailabilityStatus = AvailabilityStatus.UNAVAILABLE
    validation_messages: tuple[str, ...] = ()

    def __post_init__(self) -> None:
        if not isinstance(self.provider, str) or not self.provider.strip():
            raise ValueError("provider cannot be empty")
        if not isinstance(self.source_exchange, str) or self.source_exchange.strip().upper() != "NSE":
            raise ValueError("source_exchange must be NSE for this project")
        if not isinstance(self.report_type, str) or not self.report_type.strip():
            raise ValueError("report_type cannot be empty")
        if not isinstance(self.source_url, str) or not self.source_url.strip():
            raise ValueError("source_url or authorised source identifier cannot be empty")
        if not isinstance(self.original_filename, str) or not self.original_filename.strip() or self.original_filename in {".", ".."} or "/" in self.original_filename or "\\" in self.original_filename:
            raise ValueError("original_filename must be a basename")
        if not _is_date(self.report_date):
            raise ValueError("report_date must be a date, not a datetime")
        if not isinstance(self.downloaded_at, datetime) or self.downloaded_at.tzinfo is None or self.downloaded_at.utcoffset() is None:
            raise ValueError("downloaded_at must include a timezone")
        if not isinstance(self.size_bytes, int) or isinstance(self.size_bytes, bool) or self.size_bytes < 0:
            raise ValueError("size_bytes must be a non-negative integer")
        if not _is_sha256(self.sha256):
            raise ValueError("sha256 must be a 64-character hexadecimal digest")
        if not isinstance(self.validation_status, AvailabilityStatus):
            raise ValueError("validation_status must be an AvailabilityStatus value")
        object.__setattr__(self, "provider", self.provider.strip())
        object.__setattr__(self, "source_exchange", self.source_exchange.strip().upper())
        object.__setattr__(self, "report_type", self.report_type.strip())
        object.__setattr__(self, "source_url", self.source_url.strip())
        object.__setattr__(self, "original_filename", filename)
        object.__setattr__(self, "sha256", self.sha256.lower())
        object.__setattr__(self, "available_fields", frozenset(self.available_fields))
        object.__setattr__(self, "validation_messages", tuple(self.validation_messages))


@dataclass(frozen=True)
class SessionAvailability:
    session_number: int
    session_date: date
    report_type: str
    status: AvailabilityStatus
    available_fields: FrozenSet[str] = field(default_factory=frozenset)
    source_filename: Optional[str] = None
    source_report_sha256: Optional[str] = None
    validation_message: Optional[str] = None

    def __post_init__(self) -> None:
        if not isinstance(self.session_number, int) or isinstance(self.session_number, bool) or not 1 <= self.session_number <= 22:
            raise ValueError("session_number must be an integer between 1 and 22")
        if not _is_date(self.session_date):
            raise ValueError("session_date must be a date, not a datetime")
        if not isinstance(self.report_type, str) or not self.report_type.strip():
            raise ValueError("report_type cannot be empty")
        if not isinstance(self.status, AvailabilityStatus):
            raise ValueError("status must be an AvailabilityStatus value")
        if self.source_filename is not None and (not isinstance(self.source_filename, str) or not self.source_filename.strip() or "/" in self.source_filename or "\\" in self.source_filename):
            raise ValueError("source_filename must be a basename")
        if self.status is AvailabilityStatus.AVAILABLE:
            if not self.source_filename or not _is_sha256(self.source_report_sha256):
                raise ValueError("available reports require an original filename and source SHA-256")
        elif self.source_report_sha256 is not None and not _is_sha256(self.source_report_sha256):
            raise ValueError("source_report_sha256 must be a 64-character hexadecimal digest")
        object.__setattr__(self, "report_type", self.report_type.strip())
        if self.source_report_sha256 is not None:
            object.__setattr__(self, "source_report_sha256", self.source_report_sha256.lower())
        object.__setattr__(self, "available_fields", frozenset(self.available_fields))

    @property
    def period(self) -> str:
        if self.session_number <= 17:
            return "first_17"
        if self.session_number <= 21:
            return "recent_4"
        return "selected_eod"


@dataclass(frozen=True)
class ContractObservation:
    session_date: date
    underlying: str
    expiry: date
    strike: float
    option_type: OptionType
    source_report_sha256: str
    source_record_id: str
    oi: Optional[int]
    change_in_oi: Optional[int]
    volume: Optional[int]
    close: Optional[float]
    iv: Optional[float] = None

    def __post_init__(self) -> None:
        if not _is_date(self.session_date) or not _is_date(self.expiry):
            raise ValueError("session_date and expiry must be dates, not datetimes")
        if not isinstance(self.underlying, str) or not self.underlying.strip():
            raise ValueError("underlying cannot be empty")
        if not _is_number(self.strike) or self.strike <= 0:
            raise ValueError("strike must be a finite positive number")
        if not isinstance(self.option_type, OptionType):
            raise ValueError("option_type must be an OptionType value")
        if not _is_sha256(self.source_report_sha256):
            raise ValueError("source_report_sha256 must be a 64-character hexadecimal digest")
        if not isinstance(self.source_record_id, str) or not self.source_record_id.strip():
            raise ValueError("source_record_id cannot be empty")
        for name in ("oi", "change_in_oi", "volume"):
            value = getattr(self, name)
            if value is not None and (not isinstance(value, int) or isinstance(value, bool)):
                raise ValueError(f"{name} must be an integer when supplied")
        for name in ("oi", "volume"):
            value = getattr(self, name)
            if value is not None and value < 0:
                raise ValueError(f"{name} cannot be negative")
        for name in ("close", "iv"):
            value = getattr(self, name)
            if value is not None and not _is_number(value):
                raise ValueError(f"{name} must be a finite number when supplied")
            if value is not None and value < 0:
                raise ValueError(f"{name} cannot be negative")
        object.__setattr__(self, "underlying", self.underlying.strip().upper())
        object.__setattr__(self, "source_report_sha256", self.source_report_sha256.lower())
        object.__setattr__(self, "source_record_id", self.source_record_id.strip())
        # None means the source did not provide the field. It is never replaced here.
