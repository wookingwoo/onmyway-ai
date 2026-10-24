import asyncio
import httpx

from onmyway.main import app


def call(method, path, **kwargs):
    async def run():
        async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
            return await client.request(method, path, **kwargs)
    return asyncio.run(run())


def test_demo_round_trip_and_health_expose_no_secrets(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "never-expose-test-key")
    assert "never-expose" not in call("GET", "/health").text
    demo = call("GET", "/v1/demo")
    assert demo.status_code == 200
    result = call("POST", "/v1/plan", json=demo.json())
    assert result.status_code == 200
    body = result.json()
    assert body["status"] == "feasible"
    assert body["is_fixture"] is True
    assert len(body["stops"]) == 3
    assert body["warnings"]


def test_no_credentials_returns_actionable_503_without_fixture(monkeypatch):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    response = call("POST", "/v1/parse", json={"text": "약국", "reference_time": "2026-10-23T18:00:00+09:00"})
    assert response.status_code == 503
    assert response.json()["detail"]["code"] == "provider_not_configured"
    assert "errands" not in response.json()


def test_invalid_request_is_422():
    demo = call("GET", "/v1/demo").json()
    demo["deadline"] = "2026-10-23T17:00:00+09:00"
    assert call("POST", "/v1/plan", json=demo).status_code == 422


def test_clarification_context_rejects_blank_and_unbounded_answers():
    raw = {"text": "약국", "reference_time": "2026-10-23T18:00:00+09:00"}
    for answers in [[{"question": "언제?", "answer": "  "}],
                    [{"question": "언제?", "answer": "a" * 1001}],
                    [{"question": "언제?", "answer": "19시"}] * 2]:
        assert call("POST", "/v1/parse", json={**raw, "answers": answers}).status_code == 422
