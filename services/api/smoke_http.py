"""Real localhost HTTP smoke test; deliberately disables every live credential."""
import os
from pathlib import Path
import socket
import subprocess
import sys
import time

import httpx


if __name__ == "__main__":
    with socket.socket() as reservation:
        reservation.bind(("127.0.0.1", 0))
        port = reservation.getsockname()[1]
    environment = dict(os.environ)
    for name in ("OPENAI_API_KEY", "OPENAI_MODEL", "KAKAO_REST_API_KEY", "KAKAO_MOBILITY_REST_API_KEY"):
        environment.pop(name, None)
    process = subprocess.Popen(
        [sys.executable, "-m", "uvicorn", "onmyway.main:app", "--host", "127.0.0.1", "--port", str(port), "--log-level", "warning"],
        cwd=Path(__file__).resolve().parent, env=environment,
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
    try:
        with httpx.Client(base_url=f"http://127.0.0.1:{port}", timeout=3, trust_env=False) as client:
            for attempt in range(50):
                if process.poll() is not None:
                    raise RuntimeError("Uvicorn exited before becoming ready")
                try:
                    health = client.get("/health")
                    break
                except httpx.ConnectError:
                    time.sleep(0.1)
            else:
                raise RuntimeError("Uvicorn did not become ready within five seconds")
            assert health.status_code == 200
            assert health.json()["providers"] == {"openai": False, "kakao_local": False, "kakao_mobility": False}
            demo = client.get("/v1/demo")
            assert demo.status_code == 200
            result = client.post("/v1/plan", json=demo.json())
            assert result.status_code == 200
            body = result.json()
            assert body["status"] == "feasible" and body["is_fixture"] and len(body["stops"]) == 3
            parse = client.post("/v1/parse", json={"text": "약국", "reference_time": "2026-10-23T18:00:00+09:00"})
            assert parse.status_code == 503 and parse.json()["detail"]["code"] == "provider_not_configured"
            print("PASS: real localhost HTTP health -> demo -> plan (3 fixture stops), parse missing-key 503; zero live provider calls")
    finally:
        process.terminate()
        process.wait(timeout=5)
