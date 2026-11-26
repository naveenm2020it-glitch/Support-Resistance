"""Small pure helpers for real observed multi-strike contract rows."""
from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass
from datetime import date
from typing import Iterable

from .models import ContractObservation, OptionType


@dataclass(frozen=True)
class StrikeKey:
    underlying: str
    expiry: date
    strike: float
    option_type: OptionType


def group_observations_by_strike(observations: Iterable[ContractObservation]) -> dict[StrikeKey, tuple[ContractObservation, ...]]:
    """Group actual rows by contract identity, retaining missing fields as None."""
    groups: dict[StrikeKey, list[ContractObservation]] = defaultdict(list)
    for item in observations:
        key = StrikeKey(item.underlying.strip().upper(), item.expiry, item.strike, item.option_type)
        groups[key].append(item)
    return {key: tuple(sorted(rows, key=lambda row: row.session_date)) for key, rows in groups.items()}
