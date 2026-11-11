"""Private single-user Google Routes proxy. Run behind an HTTPS reverse proxy."""
import hmac
import json
import math
import os
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.request import Request, urlopen

GOOGLE_URL = "https://routes.googleapis.com/directions/v2:computeRoutes"
lock = threading.Lock()
last_request = 0.0
requests_today = 0
quota_day = None


def compute_route(payload, key, opener=urlopen):
    if not isinstance(payload, dict):
        raise ValueError("Expected object")
    addresses = {}
    for name in ("origin", "destination"):
        value = payload.get(name)
        if not isinstance(value, str) or not 3 <= len(value.strip()) <= 500:
            raise ValueError("Invalid address")
        addresses[name] = {"address": value.strip()}
    body = dict(addresses, travelMode="DRIVE", routingPreference="TRAFFIC_AWARE_OPTIMAL")
    request = Request(GOOGLE_URL, data=json.dumps(body).encode(), headers={
        "Content-Type": "application/json", "X-Goog-Api-Key": key,
        "X-Goog-FieldMask": "routes.duration,fallbackInfo"}, method="POST")
    with opener(request, timeout=15) as response:
        raw = response.read(65537)
    if len(raw) > 65536:
        raise ValueError("Response too large")
    result = json.loads(raw)
    if "fallbackInfo" in result:
        raise ValueError("Traffic unavailable")
    routes = result.get("routes", [])
    if not routes:
        raise ValueError("No driving route")
    duration = routes[0].get("duration", "")
    if not isinstance(duration, str) or not duration.endswith("s"):
        raise ValueError("Missing duration")
    seconds = float(duration[:-1])
    if not math.isfinite(seconds) or not 0 < seconds <= 86400:
        raise ValueError("Invalid duration")
    return {"durationSeconds": math.ceil(seconds), "checkedAt": int(time.time())}


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass  # Never log addresses, credentials, or request bodies.

    def respond(self, code, body):
        data = json.dumps(body).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        global last_request, quota_day, requests_today
        if self.path != "/route":
            self.respond(404, {"error": "Not found"})
            return
        expected = "Bearer " + os.environ["MOMENT_ACCESS_TOKEN"]
        if not hmac.compare_digest(self.headers.get("Authorization", "").encode(), expected.encode()):
            self.respond(401, {"error": "Unauthorized"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if not 0 < length <= 4096:
                raise ValueError()
            self.connection.settimeout(10)
            payload = json.loads(self.rfile.read(length))
            # Validate before spending quota; compute_route also validates.
            if not isinstance(payload, dict) or any(not isinstance(payload.get(k), str) or not 3 <= len(payload[k].strip()) <= 500 for k in ("origin", "destination")):
                raise ValueError()
        except Exception:
            self.respond(400, {"error": "Invalid request"})
            return
        with lock:
            today = time.strftime("%Y-%m-%d", time.gmtime())
            if today != quota_day:
                quota_day, requests_today = today, 0
            if time.monotonic() - last_request < 45 or requests_today >= int(os.environ.get("DAILY_REQUEST_LIMIT", "150")):
                self.respond(429, {"error": "Request limit reached"})
                return
            last_request = time.monotonic()
            requests_today += 1
        try:
            self.respond(200, compute_route(payload, os.environ["GOOGLE_ROUTES_API_KEY"]))
        except Exception:
            self.respond(503, {"error": "Current traffic unavailable"})


if __name__ == "__main__":
    if len(os.environ.get("MOMENT_ACCESS_TOKEN", "")) < 32 or not os.environ.get("GOOGLE_ROUTES_API_KEY"):
        raise SystemExit("Set GOOGLE_ROUTES_API_KEY and a random MOMENT_ACCESS_TOKEN (32+ characters)")
    ThreadingHTTPServer(("127.0.0.1", int(os.environ.get("PORT", "8080"))), Handler).serve_forever()
