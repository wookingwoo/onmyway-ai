import asyncio
import json

import httpx
import pytest

from onmyway.models import DiscoverRequest, ParseRequest
from onmyway.providers import ProviderError, discover, parse_text
from test_planner import request


def parsed():
    return {"errands": [{"id": "medicine", "label": "약 구매", "query": "약국", "mandatory": True, "dwell_seconds": 600}],
            "deadline": "2026-10-23T19:00:00+09:00", "clarification_questions": [], "warnings": []}


def parse_request():
    return ParseRequest(text="약 사고 7시까지 집에", reference_time="2026-10-23T18:00:00+09:00")


def test_openai_strict_schema_and_validated_output(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "never-expose-test-key")
    monkeypatch.setenv("OPENAI_MODEL", "user-selected-model")
    def handler(req):
        body = json.loads(req.content)
        assert req.url == "https://api.openai.com/v1/responses"
        assert body["model"] == "user-selected-model"
        assert body["store"] is False
        format = body["text"]["format"]
        assert format["type"] == "json_schema" and format["strict"] is True
        assert set(format["schema"]["required"]) == set(format["schema"]["properties"])
        return httpx.Response(200, json={"status": "completed", "output": [{"type": "message", "content": [{"type": "output_text", "text": json.dumps(parsed())}]}]})
    async def run():
        async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
            result = await parse_text(parse_request(), client)
            assert result.errands[0].label == "약 구매"
            assert any("review" in warning.lower() for warning in result.warnings)
    asyncio.run(run())


@pytest.mark.parametrize("payload,code", [
    ({"status": "incomplete", "output": []}, "llm_incomplete"),
    ({"status": "completed", "output": [{"type": "message", "content": [{"type": "refusal", "refusal": "hidden"}]}]}, "llm_refusal"),
    ({"status": "completed", "output": [{"type": "message", "content": [{"type": "output_text", "text": "{}"}]}]}, "llm_invalid_output"),
])
def test_openai_failure_not_replaced_by_fixture(monkeypatch, payload, code):
    monkeypatch.setenv("OPENAI_API_KEY", "never-expose-test-key")
    monkeypatch.setenv("OPENAI_MODEL", "test-model")
    async def run():
        async with httpx.AsyncClient(transport=httpx.MockTransport(lambda _: httpx.Response(200, json=payload))) as client:
            with pytest.raises(ProviderError) as caught:
                await parse_text(parse_request(), client)
            assert caught.value.code == code
            assert "never-expose" not in str(caught.value)
    asyncio.run(run())


def test_timeout_is_safe(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "never-expose-test-key")
    monkeypatch.setenv("OPENAI_MODEL", "test-model")
    def timeout(req):
        raise httpx.ReadTimeout("secret body never-expose-test-key", request=req)
    async def run():
        async with httpx.AsyncClient(transport=httpx.MockTransport(timeout)) as client:
            with pytest.raises(ProviderError) as caught:
                await parse_text(parse_request(), client)
            assert caught.value.code == "provider_timeout"
            assert "never-expose" not in str(caught.value)
    asyncio.run(run())


def test_discovery_uses_road_seconds_but_never_invents_hours_or_service(monkeypatch):
    monkeypatch.setenv("KAKAO_REST_API_KEY", "test-key")
    monkeypatch.setenv("KAKAO_MOBILITY_REST_API_KEY", "mobility-test-key")
    raw = request()
    raw.pop("travel_edges")
    for e in raw["errands"]:
        e.pop("candidates")
    def handler(req):
        if req.url.host == "dapi.kakao.com":
            assert req.url.params["x"] == "127.0"
            assert req.url.params["y"] == "37.5"
            return httpx.Response(200, json={"documents": [{"id": "42", "place_name": "Test pharmacy", "x": "127.1", "y": "37.6"}]})
        assert req.url.host == "apis-navi.kakaomobility.com"
        assert req.url.params["origin"].startswith("127.")
        return httpx.Response(200, json={"routes": [{"result_code": 0, "summary": {"duration": 321}}]})
    async def run():
        async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
            result = await discover(DiscoverRequest.model_validate(raw), client)
            place = result.errands[0].candidates[0]
            assert place.id == "kakao:42"
            assert place.hours_status == "unknown" and place.service_status == "unknown"
            assert not place.opening_intervals and not place.source.is_fixture
            assert len(result.travel_edges) == 3
            assert all(e.seconds == 321 and not e.source.is_fixture for e in result.travel_edges)
    asyncio.run(run())


def test_provider_http_body_not_leaked(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "never-expose-test-key")
    monkeypatch.setenv("OPENAI_MODEL", "test-model")
    async def run():
        async with httpx.AsyncClient(transport=httpx.MockTransport(lambda _: httpx.Response(401, text="never-expose-test-key"))) as client:
            with pytest.raises(ProviderError) as caught:
                await parse_text(parse_request(), client)
            assert caught.value.code == "provider_http_error"
            assert "never-expose" not in str(caught.value)
    asyncio.run(run())
