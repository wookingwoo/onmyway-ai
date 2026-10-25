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


def test_explicit_deadline_conflict_example_is_conditional_fixture_and_retains_mandatory_errands(monkeypatch):
    for name in ("OPENAI_API_KEY", "OPENAI_MODEL", "KAKAO_REST_API_KEY", "KAKAO_MOBILITY_REST_API_KEY"):
        monkeypatch.delenv(name, raising=False)
    def no_provider_calls(*args, **kwargs):
        raise AssertionError("fixture must not call providers")
    monkeypatch.setattr("onmyway.providers.request_json", no_provider_calls)
    response = call("GET", "/v1/demo?scenario=deadline_conflict")
    assert response.status_code == 200
    raw = response.json()
    assert raw["deadline"] == "2026-10-23T18:20:00+09:00"
    mandatory = [e for e in raw["errands"] if e["mandatory"]]
    assert [e["id"] for e in mandatory] == ["medicine", "laundry"]
    assert mandatory[1]["latest_finish"] == "2026-10-23T18:30:00+09:00"
    assert mandatory[0]["candidates"][0]["hours_status"] == "unknown"
    assert mandatory[0]["candidates"][0]["service_status"] == "unknown"
    assert mandatory[0]["candidates"][0]["opening_intervals"] == []
    assert all(p["source"]["is_fixture"] for e in raw["errands"] for p in e["candidates"])
    assert all(e["source"]["is_fixture"] for e in raw["travel_edges"])
    body = call("POST", "/v1/plan", json=raw).json()
    assert body["status"] == "infeasible" and body["is_fixture"]
    assert body["relaxation"]["requires_verification"]
    assert body["relaxation"]["required_deadline"] == "2026-10-23T18:31:00+09:00"
    assert body["relaxation"]["extension_seconds"] == 660
    assert any("FIXTURE" in w for w in body["warnings"])
    assert any("opening hours" in w for w in body["relaxation"]["warnings"])
    raw["deadline"] = body["relaxation"]["required_deadline"]
    conditional = call("POST", "/v1/plan", json=raw).json()
    assert conditional["status"] == "needs_verification"
    assert {s["errand_id"] for s in conditional["stops"]} == {"medicine", "laundry"}
    laundry = next(s for s in conditional["stops"] if s["errand_id"] == "laundry")
    assert laundry["end_time"] == "2026-10-23T18:25:00+09:00"


def test_demo_default_remains_basic_and_unrecognized_scenario_is_rejected():
    assert call("GET", "/v1/demo").json() == call("GET", "/v1/demo?scenario=basic").json()
    assert call("GET", "/v1/demo?scenario=unrecognized").status_code == 422
