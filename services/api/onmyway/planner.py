"""Pure exhaustive search: at most five errands, three candidates per errand.

Every directed driving edge is required; coordinates are never substituted for
road times. A feasible result means feasible against the supplied evidence,
not a guarantee about future traffic or a shop's real-world availability.
"""
from datetime import timedelta
from math import ceil

from .models import DeadlineRelaxation, PlanRequest, PlanResponse, SkippedErrand, Stop


def plan(request: PlanRequest) -> PlanResponse:
    result = _solve(request)
    if result.status == "infeasible":
        relaxed_request = request.model_copy(update={
            "deadline": request.departure_time + timedelta(hours=24),
            "errands": [e for e in request.errands if e.mandatory],
        })
        relaxed = _solve(relaxed_request)
        if relaxed.status == "feasible" and relaxed.home_arrival_time > request.deadline:
            result.relaxation = DeadlineRelaxation(
                required_deadline=relaxed.home_arrival_time,
                extension_seconds=ceil((relaxed.home_arrival_time - request.deadline).total_seconds()),
                description="All mandatory errands fit if the home deadline is extended; opening hours and individual finish deadlines remain enforced. Optional errands are excluded. User approval is required.")
    return result


def _solve(request: PlanRequest) -> PlanResponse:
    errands = sorted(request.errands, key=lambda e: e.id)
    edges = {(e.from_id, e.to_id): e for e in request.travel_edges}
    fixture = any(e.source.is_fixture for e in request.travel_edges) or any(
        p.source.is_fixture for e in errands for p in e.candidates)
    uncertain = set()
    best = None
    best_key = None
    mandatory = {e.id for e in errands if e.mandatory}
    for errand in errands:
        if errand.mandatory and not errand.candidates:
            uncertain.add(f"{errand.id}: no place candidates; discovery or user verification required")

    def seconds_between(a, b):
        if a == b:
            return 0
        edge = edges.get((a, b))
        if edge is None:
            uncertain.add(f"Missing road travel time: {a} -> {b}")
            return None
        return edge.seconds

    def road_warnings(a, b):
        edge = edges.get((a, b))
        if a == b or edge is None or edge.source.is_fixture:
            return []
        warnings = []
        if abs((request.departure_time - edge.source.retrieved_at).total_seconds()) > 900:
            warnings.append(f"{a} -> {b}: road snapshot is stale or outside the departure time by more than 15 minutes; refresh required")
        if edge.source.provider == "kakao-mobility-current-traffic":
            warnings.append(f"{a} -> {b}: current-traffic snapshot is not a future-leg forecast; travel time needs verification")
        return warnings

    def visit(location_id, time, selected, stops, warnings, total_travel):
        nonlocal best, best_key
        if mandatory <= selected:
            seconds = seconds_between(location_id, request.home.id)
            if seconds is not None:
                home_time = time + timedelta(seconds=seconds)
                if home_time <= request.deadline:
                    final_warnings = warnings + road_warnings(location_id, request.home.id)
                    optional_count = len(selected - mandatory)
                    signature = tuple((s.errand_id, s.place.id) for s in stops)
                    key = (bool(final_warnings), -optional_count, home_time, total_travel + seconds, signature)
                    if best_key is None or key < best_key:
                        best_key = key
                        best = (list(stops), home_time, sorted(set(final_warnings)), total_travel + seconds, set(selected))
        for errand in errands:
            if errand.id in selected:
                continue
            for place in sorted(errand.candidates, key=lambda p: p.id):
                if place.hours_status == "closed" or place.service_status == "unavailable":
                    continue
                seconds = seconds_between(location_id, place.id)
                if seconds is None:
                    continue
                arrival = time + timedelta(seconds=seconds)
                start = arrival
                extra_warnings = road_warnings(location_id, place.id)
                if place.hours_status == "verified":
                    fits = [max(arrival, i.start) for i in place.opening_intervals
                            if max(arrival, i.start) + timedelta(seconds=errand.dwell_seconds) <= i.end]
                    if not fits:
                        continue
                    start = min(fits)
                else:
                    extra_warnings.append(f"{errand.id}/{place.id}: opening hours need verification")
                if place.service_status != "verified":
                    extra_warnings.append(f"{errand.id}/{place.id}: requested service needs verification")
                end = start + timedelta(seconds=errand.dwell_seconds)
                if end > request.deadline or (errand.latest_finish is not None and end > errand.latest_finish):
                    continue
                stop = Stop(errand_id=errand.id, label=errand.label, place=place,
                            arrival_time=arrival, start_time=start, end_time=end,
                            travel_seconds=seconds, wait_seconds=int((start - arrival).total_seconds()))
                visit(place.id, end, selected | {errand.id}, stops + [stop],
                      warnings + extra_warnings, total_travel + seconds)

    visit(request.origin.id, request.departure_time, set(), [], [], 0)
    common_warnings = ["DEMO FIXTURE: synthetic places and travel times; not a live route."] if fixture else []
    if best is None:
        reasons = sorted(uncertain) if uncertain else ["No route satisfies all mandatory errands, opening intervals, dwell and home deadline."]
        reasons += [f"Mandatory errand {e.id} was not scheduled." for e in errands if e.mandatory]
        return PlanResponse(status="needs_verification" if uncertain else "infeasible", is_fixture=fixture,
                            stops=[], home_arrival_time=None,
                            skipped_errands=[SkippedErrand(id=e.id, reason="No complete mandatory route found") for e in errands if not e.mandatory],
                            warnings=common_warnings, reasons=reasons, total_travel_seconds=0)
    stops, home_time, warnings, travel, selected = best
    return PlanResponse(status="needs_verification" if warnings else "feasible", is_fixture=fixture,
                        stops=stops, home_arrival_time=home_time,
                        skipped_errands=[SkippedErrand(id=e.id, reason="Optional errand excluded: unavailable, unverified, or no better feasible route within the deadline")
                                         for e in errands if e.id not in selected],
                        warnings=common_warnings + warnings, reasons=[], total_travel_seconds=travel)
