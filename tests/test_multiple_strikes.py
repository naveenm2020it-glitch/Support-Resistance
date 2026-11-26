from datetime import date

from nifty_seller_intelligence.models import ContractObservation, OptionType
from nifty_seller_intelligence.multiple_strikes import group_observations_by_strike


def test_groups_multiple_strikes_and_call_put_separately():
    expiry = date(2026, 12, 3)
    rows = [
        ContractObservation(date(2026, 11, 24), "NIFTY", expiry, 26000, OptionType.CE, 100, 10, 500, 120.0),
        ContractObservation(date(2026, 11, 24), "NIFTY", expiry, 26000, OptionType.PE, 150, 20, 600, 110.0),
        ContractObservation(date(2026, 11, 25), "NIFTY", expiry, 26100, OptionType.CE, 200, 30, 700, 90.0),
    ]
    grouped = group_observations_by_strike(rows)
    assert len(grouped) == 3
    assert {key.strike for key in grouped} == {26000, 26100}
    assert {key.option_type for key in grouped} == {OptionType.CE, OptionType.PE}


def test_missing_iv_stays_missing():
    row = ContractObservation(date(2026, 11, 25), "NIFTY", date(2026, 12, 3), 26000, OptionType.CE, 100, 0, 50, 125.0)
    grouped = group_observations_by_strike([row])
    assert next(iter(grouped.values()))[0].iv is None
