import asyncio
import json

import httpx
import pytest

from onmyway.models import DiscoverRequest, ErrandInput, ParseRequest
from onmyway.providers import ProviderError, discover, parse_text
from test_planner import request


def test_venue_identity_defaults_to_fixed():
    assert ErrandInput(id="pickup", label="세탁물", query="세탁소").place_mode == "fixed"


def test_unselected_fixed_pickup_is_rejected_before_provider_calls():
    raw = request()
    raw.pop("travel_edges")
    raw["errands"] = [{"id": "pickup", "label": "세탁물", "query": "세탁소"}]
    calls = []
    async def run():
        async with httpx.AsyncClient(transport=httpx.MockTransport(lambda req: calls.append(req))) as client:
            with pytest.raises(ProviderError) as caught:
                await discover(DiscoverRequest.model_validate(raw), client)
            assert caught.value.code == "fixed_place_required"
    asyncio.run(run())
    assert calls == []


def test_flexible_discovery_proposes_three_without_inventing_availability(monkeypatch):
    monkeypatch.setenv("KAKAO_REST_API_KEY", "test")
    monkeypatch.setenv("KAKAO_MOBILITY_REST_API_KEY", "test")
    raw = request()
    raw.pop("travel_edges")
    raw["errands"] = [{"id": "medicine", "label": "약 구매", "query": "약국", "place_mode": "flexible"}]
    def handler(req):
        if "local/search" in str(req.url):
            assert req.url.params["size"] == "3"
            return httpx.Response(200, json={"documents": [{"id": str(i), "place_name": f"약국{i}", "x": "127", "y": "37.5"} for i in range(3)]})
        return httpx.Response(200, json={"routes": [{"result_code": 0, "summary": {"duration": 300}}]})
    async def run():
        async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
            result = await discover(DiscoverRequest.model_validate(raw), client)
            assert len(result.errands[0].candidates) == 3
            assert all(p.hours_status == p.service_status == "unknown" for p in result.errands[0].candidates)
    asyncio.run(run())


def test_clarification_cannot_turn_a_fixed_pickup_into_a_replaceable_shop(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "test")
    monkeypatch.setenv("OPENAI_MODEL", "test-model")
    old = {"id": "pickup", "label": "세탁물", "query": "세탁소", "place_mode": "fixed"}
    raw = ParseRequest(text="맡긴 세탁물 찾아줘", reference_time="2026-10-23T18:00:00+09:00", previous_errands=[old])
    output = {"errands": [{**old, "place_mode": "flexible"}], "deadline": None, "clarification_questions": [], "warnings": []}
    async def run():
        async with httpx.AsyncClient(transport=httpx.MockTransport(lambda _: httpx.Response(200, json={"status": "completed", "output": [{"content": [{"type": "output_text", "text": json.dumps(output)}]}]}))) as client:
            with pytest.raises(ProviderError):
                await parse_text(raw, client)
    asyncio.run(run())
