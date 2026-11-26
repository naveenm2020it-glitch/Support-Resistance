"""Core typed models. Missing source data is represented explicitly, never imputed."""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date, datetime
from enum import Enum
from typing import FrozenSet, Optional


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
    available_fields: FrozenSet[str] = field(default_factory=frozenset)
    validation_status: AvailabilityStatus = AvailabilityStatus.UNAVAILABLE
    validation_messages: tuple[str, ...] = ()

    def __post_init__(self) -> None:
        if self.provider.upper() != "NSE":
            raise ValueError("This project manifest currently accepts NSE reports only")
        if not self.original_filename or "/" in self.original_filename or "\\\\" in self.original_filename:
            raise ValueError("original_filename must be a basename")
        if self.size_bytes < 0:
            raise ValueError("size_bytes cannot be negative")
        if len(self.sha256) != 64 or any(c not in "0123456789abcdefABCDEF" for c in self.sha256):
            raise ValueError("sha256 must be a 64-character hexadecimal digest")


@dataclass(frozen=True)
class SessionAvailability:
    session_number: int
    session_date: date
    report_type: str
    status: AvailabilityStatus
    available_fields: FrozenSet[str] = field(default_factory=frozenset)
    source_filename: Optional[str] = None
    validation_message: Optional[str] = None

    def __post_init__(self) -> None:
        if not 1 <= self.session_number <= 22:
            raise ValueError("session_number must be between 1 and 22")

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
    oi: Optional[int]
    change_in_oi: Optional[int]
    volume: Optional[int]
    close: Optional[float]
    iv: Optional[float] = None

    def __post_init__(self) -> None:
        if self.strike <= 0:
            raise ValueError("strike must be positive")
        for name in ("oi", "volume"):
            value = getattr(self, name)
            if value is not None and value < 0:
                raise ValueError(f"{name} cannot be negative")
        # None means the source did not provide the field. It is never replaced here.
