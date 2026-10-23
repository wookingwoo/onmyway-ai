from copy import deepcopy

import pytest
from pydantic import ValidationError

from onmyway.models import PlanRequest
from onmyway.planner import plan


def source(fixture=False):
    return {"provider": "test-road", "retrieved_at": "2026-10-23T18:00:00+09:00", "is_fixture": fixture}


def request():
    location = lambda id: {"id": id, "name": id, "latitude": 37.5, "longitude": 127.0}
    place = {**location("p"), "hours_status": "verified", "service_status": "verified",
             "opening_intervals": [{"start": "2026-10-23T17:00:00+09:00", "end": "2026-10-23T20:00:00+09:00"}], "source": source()}
    return {"origin": location("origin"), "home": location("home"),
            "departure_time": "2026-10-23T18:00:00+09:00", "deadline": "2026-10-23T19:00:00+09:00",
            "errands": [{"id": "e", "label": "medicine", "query": "pharmacy", "mandatory": True, "dwell_seconds": 600, "candidates": [place]}],
            "travel_edges": [{"from_id": a, "to_id": b, "seconds": 300, "source": source()} for a, b in [("origin", "p"), ("p", "home"), ("origin", "home")]]}


def solve(raw):
    return plan(PlanRequest.model_validate(raw))


def test_counts_travel_dwell_and_home_leg():
    result = solve(request())
    assert result.status == "feasible"
    assert result.home_arrival_time.isoformat() == "2026-10-23T18:20:00+09:00"
    assert result.total_travel_seconds == 600
    assert result.stops[0].end_time.isoformat() == "2026-10-23T18:15:00+09:00"


def test_mandatory_without_candidate_never_silently_disappears():
    raw = request()
    raw["errands"][0]["candidates"] = []
    raw["travel_edges"] = [raw["travel_edges"][-1]]
    result = solve(raw)
    assert result.status == "needs_verification"
    assert "e" in " ".join(result.reasons)
    assert not result.stops


def test_cannot_finish_after_closing_or_home_deadline():
    raw = request()
    raw["errands"][0]["candidates"][0]["opening_intervals"][0]["end"] = "2026-10-23T18:10:00+09:00"
    assert solve(raw).status == "infeasible"
    raw = request()
    raw["deadline"] = "2026-10-23T18:19:00+09:00"
    assert solve(raw).status == "infeasible"


def test_errand_latest_finish_is_distinct_from_home_deadline():
    raw = request()
    raw["errands"][0]["latest_finish"] = "2026-10-23T18:10:00+09:00"
    assert solve(raw).status == "infeasible"


def test_waits_until_an_interval_that_fits_whole_dwell():
    raw = request()
    raw["errands"][0]["candidates"][0]["opening_intervals"] = [
        {"start": "2026-10-23T17:00:00+09:00", "end": "2026-10-23T18:07:00+09:00"},
        {"start": "2026-10-23T18:20:00+09:00", "end": "2026-10-23T19:00:00+09:00"}]
    result = solve(raw)
    assert result.status == "feasible"
    assert result.stops[0].wait_seconds == 900
    assert result.home_arrival_time.isoformat() == "2026-10-23T18:35:00+09:00"


@pytest.mark.parametrize("missing", ["hours", "service", "edge"])
def test_unknown_evidence_never_verified(missing):
    raw = request()
    if missing == "edge":
        raw["travel_edges"] = raw["travel_edges"][1:]
    else:
        raw["errands"][0]["candidates"][0][missing + "_status"] = "unknown"
    result = solve(raw)
    assert result.status == "needs_verification"
    assert result.warnings or result.reasons


def test_optional_skipped_with_reason_and_missing_edge_does_not_poison_valid_route():
    raw = request()
    optional = deepcopy(raw["errands"][0])
    optional.update(id="optional", mandatory=False)
    optional["candidates"][0]["id"] = "other"
    raw["errands"].append(optional)
    result = solve(raw)
    assert result.status == "feasible"
    assert [stop.errand_id for stop in result.stops] == ["e"]
    assert result.skipped_errands[0].id == "optional"
    assert result.skipped_errands[0].reason


def test_searches_order_and_candidates_deterministically_and_includes_optional():
    raw = request()
    optional = deepcopy(raw["errands"][0])
    optional.update(id="optional", mandatory=False, dwell_seconds=60)
    optional["candidates"][0]["id"] = "q"
    raw["errands"].append(optional)
    for a, b in [("p", "q"), ("q", "home")]:
        raw["travel_edges"].append({"from_id": a, "to_id": b, "seconds": 60, "source": source()})
    first = solve(raw)
    assert [s.errand_id for s in first.stops] == ["e", "optional"]
    raw["errands"].reverse()
    assert solve(raw).model_dump() == first.model_dump()


def test_fixture_label_propagates_and_duplicates_naive_datetimes_rejected():
    raw = request()
    raw["travel_edges"][0]["source"]["is_fixture"] = True
    assert solve(raw).is_fixture
    raw["departure_time"] = "2026-10-23T18:00:00"
    with pytest.raises(ValidationError):
        solve(raw)
    raw = request()
    raw["errands"].append(deepcopy(raw["errands"][0]))
    with pytest.raises(ValidationError):
        solve(raw)


def test_stale_road_snapshot_requires_verification():
    raw = request()
    raw["travel_edges"][0]["source"]["retrieved_at"] = "2026-10-23T17:00:00+09:00"
    result = solve(raw)
    assert result.status == "needs_verification"
    assert any("stale" in warning.lower() for warning in result.warnings)


def test_infeasible_home_deadline_gets_exact_mandatory_relaxation():
    raw = request()
    raw["deadline"] = "2026-10-23T18:19:00+09:00"
    result = solve(raw)
    assert result.status == "infeasible"
    assert result.relaxation.extension_seconds == 60
    assert result.relaxation.required_deadline.isoformat() == "2026-10-23T18:20:00+09:00"


def test_fractional_second_deadline_extension_rounds_up_for_safety():
    raw = request()
    raw["deadline"] = "2026-10-23T18:19:00.500+09:00"
    result = solve(raw)
    assert result.relaxation.extension_seconds == 60


def test_empty_errands_and_superset_matrix_support_home_replanning():
    raw = request()
    raw["origin"] = {key: raw["errands"][0]["candidates"][0][key] for key in ("id", "name", "latitude", "longitude")}
    raw["errands"] = []
    result = solve(raw)
    assert result.status == "feasible"
    assert result.stops == []
    assert result.home_arrival_time.isoformat() == "2026-10-23T18:05:00+09:00"
