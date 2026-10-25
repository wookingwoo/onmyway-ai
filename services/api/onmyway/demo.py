"""Explicit synthetic, repeatable example; never a provider fallback."""
from typing import Literal

from .models import PlanRequest


def demo_request(scenario: Literal["basic", "deadline_conflict"] = "basic") -> PlanRequest:
    source = {"provider": "synthetic-demo", "retrieved_at": "2026-10-23T17:00:00+09:00", "is_fixture": True}
    def location(id, name, lat, lng):
        return {"id": id, "name": name, "latitude": lat, "longitude": lng}
    origin = location("origin", "데모 출발지", 37.5000, 127.0000)
    home = location("home", "데모 집", 37.5100, 127.0200)
    places = [location("pharmacy", "온유약국", 37.503, 127.004),
              location("laundry", "한결세탁소", 37.506, 127.012),
              location("grocery", "봄날마트", 37.509, 127.017)]
    errands = []
    for id, label, query, dwell, place in zip(
        ["medicine", "laundry", "groceries"], ["약 구매", "세탁물 찾기", "우유 구매"],
        ["약국", "세탁소", "마트"], [600, 300, 300], places):
        errands.append({"id": id, "label": label, "query": query, "place_mode": "fixed" if id == "laundry" else "flexible", "mandatory": id != "groceries", "dwell_seconds": dwell,
                        "candidates": [{**place, "hours_status": "verified", "service_status": "verified", "source": source,
                                        "opening_intervals": [{"start": "2026-10-23T17:00:00+09:00", "end": "2026-10-23T20:00:00+09:00"}]}]})
    points = [origin] + places + [home]
    edges = [{"from_id": a["id"], "to_id": b["id"], "seconds": 240 + 60 * abs(i - j), "source": source}
             for i, a in enumerate(points) for j, b in enumerate(points) if a["id"] != b["id"]]
    deadline = "2026-10-23T19:00:00+09:00"
    if scenario == "deadline_conflict":
        deadline = "2026-10-23T18:20:00+09:00"
        errands[1]["latest_finish"] = "2026-10-23T18:30:00+09:00"
        errands[0]["candidates"][0].update(hours_status="unknown", service_status="unknown", opening_intervals=[])
    return PlanRequest(origin=origin, home=home, departure_time="2026-10-23T18:00:00+09:00",
                       deadline=deadline, errands=errands, travel_edges=edges)
