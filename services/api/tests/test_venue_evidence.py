"""Exact venue identity and evidence safety at the API/planner boundary."""
import asyncio

import httpx
import pytest
from pydantic import ValidationError

from onmyway.main import app
from onmyway.models import DiscoverRequest, ErrandInput, PlanRequest
from onmyway.planner import plan
from onmyway.providers import ProviderError, discover, strict_schema
from test_api import call
from test_planner import request


def live_request():
    raw = request()
    raw["errands"][0]["candidates"][0].pop("availability_evidence", None)
    return raw


def discovery_request():
    raw = live_request()
    raw.pop("travel_edges")
    place = raw["errands"][0].pop("candidates")[0]
    place.update(address="서울 테스트로 42", availability_evidence={
        "basis": "User called this branch and confirmed pickup and opening window.",
        "confirmed_at": "2026-10-23T17:50:00+09:00"})
    raw["errands"][0]["selected_place"] = place
    return raw


def test_verified_live_flags_without_availability_basis_cannot_establish_feasibility():
    result = plan(PlanRequest.model_validate(live_request()))
    assert result.status == "needs_verification"
    assert result.stops and any("availability evidence" in w.lower() for w in result.warnings)


@pytest.mark.parametrize("confirmed_at,expected", [
    ("2026-10-22T18:00:00+09:00", "feasible"),
    ("2026-10-22T17:59:59+09:00", "needs_verification"),
    ("2026-10-23T18:00:01+09:00", "needs_verification"),
    ("2026-10-23T17:50:00+09:00", "feasible"),
])
def test_availability_basis_must_be_nonfuture_and_at_most_24_hours_old(confirmed_at, expected):
    raw = live_request()
    raw["errands"][0]["candidates"][0]["availability_evidence"] = {
        "basis": "Confirmed pickup with the selected branch by phone", "confirmed_at": confirmed_at}
    result = plan(PlanRequest.model_validate(raw))
    assert result.status == expected
    assert result.stops


def test_fixture_availability_remains_explicitly_labelled_without_live_basis():
    raw = live_request()
    raw["errands"][0]["candidates"][0]["source"]["is_fixture"] = True
    result = plan(PlanRequest.model_validate(raw))
    assert result.status == "feasible"
    assert result.is_fixture and any("FIXTURE" in w for w in result.warnings)


@pytest.mark.parametrize("basis,confirmed_at", [
    (" ", "2026-10-23T17:50:00+09:00"),
    ("a" * 501, "2026-10-23T17:50:00+09:00"),
    ("Confirmed", "2026-10-23T17:50:00"),
])
def test_availability_basis_rejects_blank_unbounded_or_naive_evidence(basis, confirmed_at):
    raw = discovery_request()
    raw["errands"][0]["selected_place"]["availability_evidence"] = {
        "basis": basis, "confirmed_at": confirmed_at}
    with pytest.raises(ValidationError):
        DiscoverRequest.model_validate(raw)


def test_discovery_pins_selected_place_skips_search_and_refreshes_road_matrix(monkeypatch):
    # Pinned venues require Mobility only; no Local key is needed or consulted.
    monkeypatch.delenv("KAKAO_REST_API_KEY", raising=False)
    monkeypatch.setenv("KAKAO_MOBILITY_REST_API_KEY", "mock-mobility")
    raw = discovery_request()
    def handler(req):
        assert req.url.host == "apis-navi.kakaomobility.com", "Pinned venue must not be searched again"
        return httpx.Response(200, json={"routes": [{"result_code": 0, "summary": {"duration": 321}}]})
    async def run():
        async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
            result = await discover(DiscoverRequest.model_validate(raw), client)
        selected = result.errands[0].selected_place.model_dump(mode="json")
        assert selected == raw["errands"][0]["selected_place"]
        assert result.errands[0].candidates[0].model_dump(mode="json") == selected
        assert len(result.travel_edges) == 3
        assert all(e.seconds == 321 and e.source.provider == "kakao-mobility-current-traffic" for e in result.travel_edges)
    asyncio.run(run())


def test_live_discovery_rejects_selected_fixture_before_provider_access(monkeypatch):
    monkeypatch.delenv("KAKAO_REST_API_KEY", raising=False)
    monkeypatch.delenv("KAKAO_MOBILITY_REST_API_KEY", raising=False)
    raw = discovery_request()
    raw["errands"][0]["selected_place"]["source"]["is_fixture"] = True
    async def run():
        async with httpx.AsyncClient(transport=httpx.MockTransport(lambda _: pytest.fail("must not contact providers"))) as client:
            with pytest.raises(ProviderError) as caught:
                await discover(DiscoverRequest.model_validate(raw), client)
        assert caught.value.status_code == 422
        assert caught.value.code == "fixture_place_not_allowed"
    asyncio.run(run())


def test_places_endpoint_returns_three_addressed_unknown_candidates(monkeypatch):
    monkeypatch.setenv("KAKAO_REST_API_KEY", "mock-local")
    monkeypatch.delenv("KAKAO_MOBILITY_REST_API_KEY", raising=False)
    original_client = httpx.AsyncClient
    def handler(req):
        assert req.url.host == "dapi.kakao.com"
        assert req.url.params["size"] == "3"
        assert req.url.params["query"] == "지정 세탁소"
        assert req.url.params["x"] == "127.0" and req.url.params["y"] == "37.5"
        return httpx.Response(200, json={"documents": [
            {"id": str(i), "place_name": f"Branch {i}", "x": "127.1", "y": "37.6",
             "road_address_name": "서울 도로 42" if i == 0 else "", "address_name": "서울 지번 7"}
            for i in range(4)]})
    monkeypatch.setattr("onmyway.main.httpx.AsyncClient", lambda: original_client(transport=httpx.MockTransport(handler)))
    async def run():
        async with original_client(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
            return await client.post("/v1/places", json={"query": "지정 세탁소", "origin": live_request()["origin"]})
    response = asyncio.run(run())
    assert response.status_code == 200
    places = response.json()["places"]
    assert [p["id"] for p in places] == ["kakao:0", "kakao:1", "kakao:2"]
    assert places[0]["address"] == "서울 도로 42" and places[1]["address"] == "서울 지번 7"
    assert all(p["hours_status"] == p["service_status"] == "unknown" and not p["opening_intervals"]
               and p["availability_evidence"] is None and not p["source"]["is_fixture"] for p in places)


@pytest.mark.parametrize("query", ["", " " , "a" * 201])
def test_places_query_is_bounded_nonblank(query):
    response = call("POST", "/v1/places", json={"query": query, "origin": live_request()["origin"]})
    assert response.status_code == 422


def test_places_missing_configuration_returns_explicit_503(monkeypatch):
    monkeypatch.delenv("KAKAO_REST_API_KEY", raising=False)
    response = call("POST", "/v1/places", json={"query": "약국", "origin": live_request()["origin"]})
    assert response.status_code == 503
    assert response.json()["detail"]["code"] == "provider_not_configured"
    assert "places" not in response.json()


def test_parse_schema_is_not_broadened_with_discovery_fields():
    assert "selected_place" not in ErrandInput.model_fields
    assert "selected_place" not in strict_schema()["$defs"]["ErrandInput"]["properties"]


def test_uncertain_relaxed_route_has_conditional_proposal_without_upgrading_status():
    raw = live_request()
    raw["deadline"] = "2026-10-23T18:19:00+09:00"
    for edge in raw["travel_edges"]:
        edge["source"]["provider"] = "kakao-mobility-current-traffic"
    result = plan(PlanRequest.model_validate(raw))
    assert result.status == "infeasible" and not result.stops
    assert result.relaxation is not None
    assert result.relaxation.required_deadline.isoformat() == "2026-10-23T18:20:00+09:00"
    assert result.relaxation.extension_seconds == 60
    assert result.relaxation.requires_verification
    assert any("current-traffic" in w for w in result.relaxation.warnings)
    assert any("availability evidence" in w for w in result.relaxation.warnings)
    assert "conditional" in result.relaxation.description.lower()
    assert raw["deadline"] == "2026-10-23T18:19:00+09:00"


@pytest.mark.parametrize("constraint", ["edge", "closing", "errand_deadline", "candidate"])
def test_relaxation_never_invents_road_or_ignores_other_constraints(constraint):
    raw = live_request()
    raw["deadline"] = "2026-10-23T18:19:00+09:00"
    if constraint == "edge":
        raw["travel_edges"] = raw["travel_edges"][1:]
    elif constraint == "closing":
        raw["errands"][0]["candidates"][0]["opening_intervals"][0]["end"] = "2026-10-23T18:10:00+09:00"
    elif constraint == "errand_deadline":
        raw["errands"][0]["latest_finish"] = "2026-10-23T18:10:00+09:00"
    else:
        raw["errands"][0]["candidates"] = []
    result = plan(PlanRequest.model_validate(raw))
    assert result.relaxation is None


@pytest.mark.parametrize("change", ["identity", "coordinates", "evidence", "source", "missing", "multiple"])
def test_plan_rejects_candidates_that_differ_from_selected_place(change):
    from copy import deepcopy
    raw = request()
    errand = raw["errands"][0]
    errand["selected_place"] = deepcopy(errand["candidates"][0])
    if change == "identity":
        errand["candidates"][0].update(id="q", name="Different branch")
    elif change == "coordinates":
        errand["candidates"][0]["latitude"] = 37.6
    elif change == "evidence":
        errand["candidates"][0]["availability_evidence"]["basis"] = "Different confirmation"
    elif change == "source":
        errand["candidates"][0]["source"]["provider"] = "Different source"
    elif change == "missing":
        errand["candidates"] = []
    else:
        other = deepcopy(errand["candidates"][0])
        other["id"] = "q"
        errand["candidates"].append(other)
    response = call("POST", "/v1/plan", json=raw)
    assert response.status_code == 422


def test_plan_accepts_exact_selected_place_and_legacy_unselected_candidates():
    from copy import deepcopy
    for selected in (False, True):
        raw = request()
        if selected:
            raw["errands"][0]["selected_place"] = deepcopy(raw["errands"][0]["candidates"][0])
        response = call("POST", "/v1/plan", json=raw)
        assert response.status_code == 200
        assert response.json()["status"] == "feasible"
        assert response.json()["stops"][0]["place"]["id"] == "p"


def test_unknown_hours_closing_interval_still_prevents_impossible_deadline_proposal():
    raw = request()
    raw["deadline"] = "2026-10-23T18:19:00+09:00"
    place = raw["errands"][0]["candidates"][0]
    place["hours_status"] = "unknown"
    place["opening_intervals"][0]["end"] = "2026-10-23T18:10:00+09:00"
    result = plan(PlanRequest.model_validate(raw))
    assert result.status == "infeasible"
    assert result.relaxation is None and not result.stops


def test_unknown_hours_opening_wait_is_included_in_conditional_deadline_proposal():
    raw = request()
    raw["deadline"] = "2026-10-23T18:19:00+09:00"
    place = raw["errands"][0]["candidates"][0]
    place["hours_status"] = "unknown"
    place["opening_intervals"][0]["start"] = "2026-10-23T18:20:00+09:00"
    result = plan(PlanRequest.model_validate(raw))
    assert result.status == "infeasible"
    assert result.relaxation.required_deadline.isoformat() == "2026-10-23T18:35:00+09:00"
    assert result.relaxation.extension_seconds == 960
    assert result.relaxation.requires_verification
    assert any("opening hours" in warning for warning in result.relaxation.warnings)
