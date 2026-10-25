import httpx
from fastapi import FastAPI, HTTPException

from .demo import demo_request
from .models import DiscoverRequest, ParseRequest, ParseResponse, PlaceSearchRequest, PlaceSearchResponse, PlanRequest, PlanResponse
from .planner import plan
from .providers import ProviderError, configured, discover, parse_text, search_places


app = FastAPI(title="OnMyWay AI", version="0.1.0", description="Stateless errand planning. Fixtures are explicitly labelled; draft output requires user review.")


@app.get("/health")
def health():
    return {"status": "ok", "providers": {"openai": configured("OPENAI_API_KEY") and configured("OPENAI_MODEL"),
                                           "kakao_local": configured("KAKAO_REST_API_KEY"),
                                           "kakao_mobility": configured("KAKAO_MOBILITY_REST_API_KEY")}}


@app.get("/v1/demo", response_model=PlanRequest)
def demo():
    return demo_request()


@app.post("/v1/plan", response_model=PlanResponse)
def make_plan(request: PlanRequest):
    return plan(request)


@app.post("/v1/parse", response_model=ParseResponse)
async def parse(request: ParseRequest):
    try:
        async with httpx.AsyncClient() as client:
            return await parse_text(request, client)
    except ProviderError as error:
        raise HTTPException(error.status_code, detail={"code": error.code, "message": error.message}) from None


@app.post("/v1/discover", response_model=PlanRequest)
async def discover_places(request: DiscoverRequest):
    try:
        async with httpx.AsyncClient() as client:
            return await discover(request, client)
    except ProviderError as error:
        raise HTTPException(error.status_code, detail={"code": error.code, "message": error.message}) from None


@app.post("/v1/places", response_model=PlaceSearchResponse)
async def find_places(request: PlaceSearchRequest):
    try:
        async with httpx.AsyncClient() as client:
            return await search_places(request, client)
    except ProviderError as error:
        raise HTTPException(error.status_code, detail={"code": error.code, "message": error.message}) from None
