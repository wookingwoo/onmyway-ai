from typing import Annotated, Literal

from pydantic import AwareDatetime, BaseModel, ConfigDict, Field, StrictInt, model_validator


class Model(BaseModel):
    model_config = ConfigDict(extra="forbid")


Identifier = Annotated[str, Field(min_length=1, max_length=100)]
Seconds = Annotated[StrictInt, Field(ge=0, le=86400)]


class Source(Model):
    provider: Annotated[str, Field(min_length=1, max_length=100)]
    retrieved_at: AwareDatetime
    is_fixture: bool = False


class Location(Model):
    id: Identifier
    name: Annotated[str, Field(min_length=1, max_length=200)]
    latitude: float = Field(ge=-90, le=90, allow_inf_nan=False)
    longitude: float = Field(ge=-180, le=180, allow_inf_nan=False)


class OpeningInterval(Model):
    start: AwareDatetime
    end: AwareDatetime

    @model_validator(mode="after")
    def ordered(self):
        if self.end <= self.start:
            raise ValueError("opening interval end must be after start")
        return self


class AvailabilityEvidence(Model):
    basis: Annotated[str, Field(min_length=1, max_length=500)]
    confirmed_at: AwareDatetime

    @model_validator(mode="after")
    def nonblank(self):
        if not self.basis.strip():
            raise ValueError("availability basis must not be blank")
        return self


class Place(Location):
    address: str = ""
    availability_evidence: AvailabilityEvidence | None = None
    opening_intervals: list[OpeningInterval] = Field(default_factory=list, max_length=20)
    hours_status: Literal["verified", "unknown", "closed"] = "unknown"
    service_status: Literal["verified", "unknown", "unavailable"] = "unknown"
    source: Source


class ErrandInput(Model):
    id: Identifier
    label: Annotated[str, Field(min_length=1, max_length=200)]
    query: Annotated[str, Field(min_length=1, max_length=200)]
    place_mode: Literal["fixed", "flexible"] = "fixed"
    mandatory: bool = True
    dwell_seconds: Seconds = 600
    latest_finish: AwareDatetime | None = None


class DiscoveryErrand(ErrandInput):
    selected_place: Place | None = None


class Errand(DiscoveryErrand):
    candidates: list[Place] = Field(default_factory=list, max_length=3)

    @model_validator(mode="after")
    def selected_place_is_authoritative(self):
        if self.selected_place is not None and self.candidates != [self.selected_place]:
            raise ValueError("candidates must contain exactly the selected place, including its evidence")
        return self


class TravelEdge(Model):
    from_id: Identifier
    to_id: Identifier
    seconds: Seconds
    source: Source


class PlaceSearchRequest(Model):
    query: Annotated[str, Field(min_length=1, max_length=200)]
    origin: Location

    @model_validator(mode="after")
    def nonblank(self):
        if not self.query.strip():
            raise ValueError("place query must not be blank")
        return self


class PlaceSearchResponse(Model):
    places: list[Place] = Field(default_factory=list, max_length=3)


class DiscoverRequest(Model):
    origin: Location
    home: Location
    departure_time: AwareDatetime
    deadline: AwareDatetime
    errands: list[DiscoveryErrand] = Field(max_length=5)

    @model_validator(mode="after")
    def valid_request(self):
        if self.deadline < self.departure_time:
            raise ValueError("deadline must not precede departure_time")
        if (self.deadline - self.departure_time).total_seconds() > 86400:
            raise ValueError("planning horizon must not exceed 24 hours")
        ids = [errand.id for errand in self.errands]
        if len(set(ids)) != len(ids):
            raise ValueError("errand ids must be unique")
        return self


class PlanRequest(DiscoverRequest):
    errands: list[Errand] = Field(max_length=5)
    travel_edges: list[TravelEdge] = Field(max_length=300)

    @model_validator(mode="after")
    def valid_matrix(self):
        locations = [self.origin, self.home] + [p for e in self.errands for p in e.candidates]
        coordinates = {}
        for location in locations:
            pair = (location.latitude, location.longitude)
            if location.id in coordinates and coordinates[location.id] != pair:
                raise ValueError("a location id must have consistent coordinates")
            coordinates[location.id] = pair
        for errand in self.errands:
            if len({p.id for p in errand.candidates}) != len(errand.candidates):
                raise ValueError("candidate ids within an errand must be unique")
        pairs = [(edge.from_id, edge.to_id) for edge in self.travel_edges]
        if len(set(pairs)) != len(pairs):
            raise ValueError("travel edges must have unique directed pairs")
        # Matrix supersets may retain completed stops during a replan; unused
        # pairs are ignored by the solver and never create phantom stops.
        return self


class Stop(Model):
    errand_id: str
    label: str
    place: Place
    arrival_time: AwareDatetime
    start_time: AwareDatetime
    end_time: AwareDatetime
    travel_seconds: int
    wait_seconds: int


class SkippedErrand(Model):
    id: str
    reason: str


class DeadlineRelaxation(Model):
    requires_verification: bool = False
    warnings: list[str] = Field(default_factory=list)
    required_deadline: AwareDatetime
    extension_seconds: int
    description: str


class PlanResponse(Model):
    status: Literal["feasible", "needs_verification", "infeasible"]
    is_fixture: bool
    stops: list[Stop]
    home_arrival_time: AwareDatetime | None
    skipped_errands: list[SkippedErrand]
    warnings: list[str]
    reasons: list[str]
    total_travel_seconds: int
    relaxation: DeadlineRelaxation | None = None


class QuestionAnswer(Model):
    question: Annotated[str, Field(min_length=1, max_length=500)]
    answer: Annotated[str, Field(min_length=1, max_length=1000)]

    @model_validator(mode="after")
    def nonblank(self):
        if not self.question.strip() or not self.answer.strip():
            raise ValueError("question and answer must not be blank")
        return self


class ParseRequest(Model):
    text: Annotated[str, Field(min_length=1, max_length=4000)]
    reference_time: AwareDatetime
    answers: list[QuestionAnswer] = Field(default_factory=list, max_length=10)
    previous_errands: list[ErrandInput] = Field(default_factory=list, max_length=5)
    previous_deadline: AwareDatetime | None = None

    @model_validator(mode="after")
    def unique_context(self):
        if len({e.id for e in self.previous_errands}) != len(self.previous_errands):
            raise ValueError("previous errand ids must be unique")
        if len({a.question for a in self.answers}) != len(self.answers):
            raise ValueError("answer questions must be unique")
        return self


class ParseResponse(Model):
    errands: list[ErrandInput] = Field(max_length=5)
    deadline: AwareDatetime | None
    clarification_questions: list[str]
    warnings: list[str]
