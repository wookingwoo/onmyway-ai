"""Regenerate reviewed Android examples and the OpenAPI schema from API models."""
import json
from pathlib import Path

from onmyway.demo import demo_request
from onmyway.main import app
from onmyway.planner import plan


def write(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    target = Path(__file__).resolve().parents[2] / "contracts"
    target.mkdir(exist_ok=True)
    request = demo_request()
    write(target / "openapi.json", app.openapi())
    write(target / "demo-plan-request.json", request.model_dump(mode="json"))
    write(target / "demo-plan-response.json", plan(request).model_dump(mode="json"))
    write(target / "parse-request.json", {"text": "약국에서 약 사고 세탁물 찾아줘. 시간 되면 우유도 사고 7시까지 집에 도착하고 싶어.",
                                           "reference_time": "2026-10-23T18:00:00+09:00"})
    # This is an illustrative editable extraction, not a claim of a live LLM call.
    write(target / "parse-response-example.json", {
        "errands": [e.model_dump(mode="json", exclude={"candidates"}) for e in request.errands],
        "deadline": request.deadline.isoformat(), "clarification_questions": [],
        "warnings": ["ILLUSTRATIVE EXAMPLE, not a live OpenAI response. Dwell durations are editable estimates."]})
    write(target / "discover-request.json", request.model_dump(mode="json", exclude={"travel_edges", "errands"}) | {
        "errands": [e.model_dump(mode="json", exclude={"candidates"}) for e in request.errands]})
