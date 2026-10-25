#!/usr/bin/env python3
"""Explicit synthetic decision examples. No network, provider, or user evidence."""
import argparse
import json
from copy import deepcopy
from datetime import datetime, timedelta
from pathlib import Path

from onmyway.models import PlanRequest
from onmyway.planner import plan


STAMP = "2026-10-25T18:00:00+09:00"
SOURCE = {"provider": "hand-calculated-synthetic-evaluation", "retrieved_at": STAMP, "is_fixture": True}


def fixture_cases():
    """Literal road minutes and independent expected values; coordinates are dummy."""
    def location(identifier):
        return {"id": identifier, "name": "SYNTHETIC " + identifier, "latitude": 37.5, "longitude": 127.0}

    def place(identifier, opens):
        return {**location(identifier), "source": SOURCE.copy(), "hours_status": "verified",
                "service_status": "verified", "opening_intervals": [
                    {"start": f"2026-10-25T18:{opens}:00+09:00", "end": "2026-10-25T20:00:00+09:00"}]}

    # All directed edges reachable by either algorithm, including the home leg.
    roads = [("origin", "near", 2), ("origin", "pickup", 8), ("origin", "home", 10),
             ("near", "pickup", 8), ("near", "home", 5),
             ("pickup", "near", 8), ("pickup", "home", 8)]
    first = {"origin": location("origin"), "home": location("home"), "departure_time": STAMP,
             "deadline": "2026-10-25T18:45:00+09:00", "errands": [
                 {"id": "groceries", "label": "SYNTHETIC groceries", "query": "synthetic groceries",
                  "mandatory": True, "dwell_seconds": 600, "candidates": [place("near", "05")]},
                 {"id": "pickup", "label": "SYNTHETIC exact-venue pickup", "query": "synthetic pickup",
                  "mandatory": True, "dwell_seconds": 300, "latest_finish": "2026-10-25T18:15:00+09:00",
                  "candidates": [place("pickup", "00")]}],
             "travel_edges": [{"from_id": a, "to_id": b, "seconds": minutes * 60, "source": SOURCE.copy()}
                              for a, b, minutes in roads]}
    second = deepcopy(first)
    second["deadline"] = "2026-10-25T18:25:00+09:00"
    second["errands"][0]["mandatory"] = False
    third = deepcopy(first)
    third["deadline"] = "2026-10-25T18:20:00+09:00"
    third["errands"] = [third["errands"][1]]
    third["errands"][0]["candidates"][0].update(hours_status="unknown", service_status="unknown", opening_intervals=[])
    return [
        {"id": "pickup-before-nearest", "input": first,
         "hand_calculation": "Nearest: near arrive 18:02, wait 3m, finish 18:15; pickup arrive 18:23, finish 18:28 (13m late); home 18:36. Solver: pickup 18:08-18:13; near 18:21-18:31; home 18:36.",
         "expected": {"baseline_order": ["groceries", "pickup"], "baseline_home": "2026-10-25T18:36:00+09:00",
                      "baseline_violations": ["pickup: completion deadline missed"], "solver_order": ["pickup", "groceries"],
                      "solver_status": "feasible", "solver_home": "2026-10-25T18:36:00+09:00", "skipped": []}},
        {"id": "omit-optional-for-home", "input": second,
         "hand_calculation": "Nearest still finishes pickup at 18:28 and reaches home at 18:36, 11m after 18:25. Pickup alone: road 8m + dwell 5m + home road 8m = 21m; both errands require 36m. Omit optional groceries.",
         "expected": {"baseline_order": ["groceries", "pickup"], "baseline_home": "2026-10-25T18:36:00+09:00",
                      "baseline_violations": ["pickup: completion deadline missed", "home: deadline missed"], "solver_order": ["pickup"],
                      "solver_status": "feasible", "solver_home": "2026-10-25T18:21:00+09:00", "skipped": ["groceries"]}},
        {"id": "unknown-stays-conditional", "input": third,
         "hand_calculation": "Hypothetical pickup timeline: arrive 18:08, finish 18:13, home 18:21; 18:20 home deadline needs +60s. Unknown opening/service availability prevents a feasibility claim even with that extension.",
         "expected": {"baseline_order": ["pickup"], "baseline_home": "2026-10-25T18:21:00+09:00",
                      "baseline_violations": ["home: deadline missed"], "solver_order": [],
                      "solver_status": "infeasible", "solver_home": None, "skipped": [],
                      "required_deadline": "2026-10-25T18:21:00+09:00", "extension_seconds": 60}},
    ]


def nearest_first(request):
    """Visit every requested task by shortest next road edge; report every violation.

    This intentionally simple baseline does not optimize order or omit optional
    tasks. It still accounts for opening waits, dwell, completion deadlines and
    the final home leg, and never treats unknown availability as verified.
    """
    edges = {(edge.from_id, edge.to_id): edge.seconds for edge in request.travel_edges}
    remaining = list(request.errands)
    current, now = request.origin.id, request.departure_time
    stops, violations, warnings = [], [], []
    total_travel = 0
    while remaining:
        choices = [(edges[current, place.id], errand.id, place.id, errand, place)
                   for errand in remaining for place in errand.candidates
                   if (current, place.id) in edges]
        if not choices:
            raise ValueError("Baseline fixture lacks a road edge or candidate; no straight-line fallback")
        seconds, _, _, errand, place = min(choices, key=lambda item: item[:3])
        arrival = now + timedelta(seconds=seconds)
        start = arrival
        if place.hours_status == "closed" or place.service_status == "unavailable":
            violations.append(f"{errand.id}: unavailable")
        if place.opening_intervals:
            fits = [max(arrival, interval.start) for interval in place.opening_intervals
                    if max(arrival, interval.start) + timedelta(seconds=errand.dwell_seconds) <= interval.end]
            if fits:
                start = min(fits)
            else:
                violations.append(f"{errand.id}: no opening interval fits dwell")
        elif place.hours_status == "verified":
            violations.append(f"{errand.id}: no verified opening interval")
        if place.hours_status != "verified" or place.service_status != "verified":
            warnings.append(f"{errand.id}: opening hours/service unknown; timeline is hypothetical")
        end = start + timedelta(seconds=errand.dwell_seconds)
        if errand.latest_finish is not None and end > errand.latest_finish:
            violations.append(f"{errand.id}: completion deadline missed")
        stops.append({"errand_id": errand.id, "place_id": place.id, "mandatory": errand.mandatory,
                      "arrival_time": arrival.isoformat(), "start_time": start.isoformat(),
                      "end_time": end.isoformat(), "travel_seconds": seconds,
                      "wait_seconds": int((start - arrival).total_seconds()), "dwell_seconds": errand.dwell_seconds})
        total_travel += seconds
        current, now = place.id, end
        remaining.remove(errand)
    home_seconds = edges[current, request.home.id]
    home_arrival = now + timedelta(seconds=home_seconds)
    if home_arrival > request.deadline:
        violations.append("home: deadline missed")
    return {"status": "infeasible" if violations else "needs_verification" if warnings else "feasible",
            "is_fixture": True, "order": [stop["errand_id"] for stop in stops], "stops": stops,
            "home_leg_seconds": home_seconds, "home_arrival_time": home_arrival.isoformat(),
            "total_travel_seconds": total_travel + home_seconds, "violations": violations,
            "availability_warnings": warnings}


def evaluate_case(case):
    request = PlanRequest.model_validate(case["input"])
    if not all(edge.source.is_fixture for edge in request.travel_edges) or not all(
            place.source.is_fixture for errand in request.errands for place in errand.candidates):
        raise ValueError("Only explicitly labelled synthetic fixtures are allowed")
    return {**case, "baseline": nearest_first(request), "solver": plan(request).model_dump(mode="json")}


def check_case(case, result):
    expected = case["expected"]
    baseline = result["baseline"]
    assert baseline["order"] == expected["baseline_order"], case["id"] + ": baseline order"
    assert baseline["home_arrival_time"] == expected["baseline_home"], case["id"] + ": baseline home"
    assert baseline["violations"] == expected["baseline_violations"], case["id"] + ": baseline violations"
    solver = result["solver"]
    assert [stop["errand_id"] for stop in solver["stops"]] == expected["solver_order"], case["id"] + ": solver order"
    assert solver["status"] == expected["solver_status"], case["id"] + ": solver status"
    assert solver["home_arrival_time"] == expected["solver_home"], case["id"] + ": solver home"
    assert [item["id"] for item in solver["skipped_errands"]] == expected["skipped"], case["id"] + ": omission"
    assert solver["is_fixture"], case["id"] + ": fixture label"
    if "extension_seconds" in expected:
        assert baseline["availability_warnings"], "Unknown baseline availability must remain visible"
        assert solver["status"] != "feasible", "Unknown data cannot establish feasibility"
        proposal = solver["relaxation"]
        assert proposal["requires_verification"] and proposal["warnings"], "Extension must stay conditional"
        assert proposal["required_deadline"] == expected["required_deadline"]
        assert proposal["extension_seconds"] == expected["extension_seconds"]


def print_summary(report):
    print(report["label"])
    print("All times +09:00 on 2026-10-25; start 18:00; synthetic road minutes, not coordinates.")
    print("Roads: origin->near 2, origin->pickup 8, origin->home 10, near->pickup 8, near->home 5, pickup->near 8, pickup->home 8.")
    print("Near: opens 18:05-20:00, dwell 10m. Pickup: opens 18:00-20:00, dwell 5m, finish by 18:15.")
    for case in report["cases"]:
        baseline, solver = case["baseline"], case["solver"]
        tasks = ", ".join(f"{e['id']}={'mandatory' if e['mandatory'] else 'optional'}" for e in case["input"]["errands"])
        print(f"\n{case['id']}: {tasks}; home deadline {case['input']['deadline']}")
        if baseline["availability_warnings"]:
            print("  Availability UNKNOWN; opening interval omitted; times are hypothetical.")
        for label, result in [("nearest-first", baseline), ("solver", solver)]:
            order = " -> ".join(stop["errand_id"] for stop in result["stops"]) or "(no accepted route)"
            print(f"  {label}: {result['status']}; {order}; home {result['home_arrival_time']}")
            for stop in result["stops"]:
                dwell = int((datetime.fromisoformat(stop["end_time"]) - datetime.fromisoformat(stop["start_time"])).total_seconds())
                print(f"    {stop['errand_id']}: road {stop['travel_seconds']}s, arrive {stop['arrival_time'][11:19]}, wait {stop['wait_seconds']}s, dwell {dwell}s, finish {stop['end_time'][11:19]}")
        print("  Baseline violations: " + ("; ".join(baseline["violations"]) or "none"))
        print("  Solver omissions: " + (", ".join(item["id"] for item in solver["skipped_errands"]) or "none"))
        if solver["relaxation"]:
            proposal = solver["relaxation"]
            print(f"  CONDITIONAL suggestion: +{proposal['extension_seconds']}s to {proposal['required_deadline']}; requires_verification={proposal['requires_verification']}; user approval required.")
        print("  Hand calculation: " + case["hand_calculation"])
    print("\n" + " ".join(report["limitations"]))
    print("Use --json or --output for the full validated inputs, expected outcomes and planner response.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="assert independent hand-calculated outcomes (nonzero on failure)")
    parser.add_argument("--json", action="store_true", help="print full reproducible JSON instead of the short report")
    parser.add_argument("--output", type=Path, help="save JSON only beneath local artifacts/evidence/contest-core/")
    args = parser.parse_args()
    report = {"label": "SYNTHETIC FIXTURE COMPARISON - no live routes or observed users",
              "baseline_rule": "Shortest next directed road time, ties by errand/place ID; visit all requested tasks; no lookahead or optional omission.",
              "limitations": ["Three hand-selected examples demonstrate decision behavior, not an unbiased benchmark.",
                              "No live providers, traffic forecasts, execution timing, user tests, or time-saving claims.",
                              "Unknown availability remains unverified; a deadline proposal changes no user constraint until approved."],
              "checks": "not requested", "cases": [evaluate_case(case) for case in fixture_cases()]}
    if args.check:
        for case in report["cases"]:
            check_case(case, case)
        report["checks"] = "3/3 hand-calculated cases passed"
    payload = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        allowed = Path(__file__).resolve().parents[1] / "artifacts" / "evidence" / "contest-core"
        output = args.output.resolve()
        if output == allowed.resolve() or not output.is_relative_to(allowed.resolve()):
            parser.error("--output must be a file beneath this worktree's artifacts/evidence/contest-core/")
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(payload, encoding="utf-8")
    if args.json:
        print(payload, end="")
    else:
        print_summary(report)
        print("Checks: " + report["checks"])


if __name__ == "__main__":
    main()
