# MOMENT traffic departure updates

Connection is optional and disabled by default. No service has been deployed and no Google billing has been enabled by this change.

## What the app does

Add a calendar event with a destination and local arrival time. Open its details, enter the fixed starting address and an arrival buffer, then choose **Watch traffic** within two hours of arrival. Notification permission is required. The watch polls Google traffic through your server approximately every five minutes, increasing to every minute within 30 minutes of departure. It alerts on the initial estimate, a leave-time change of at least three minutes, the ten-minute get-ready threshold, and the leave-now threshold. Improvements as well as delays can change the leave time.

Leave time = arrival time − current traffic-aware driving duration − buffer. Google Routes returns a duration for leaving now; this is a rolling estimate, not an optimized future departure prediction or an arrival guarantee. Traffic coverage depends on Google. The app rejects Google fallback responses instead of labeling them current traffic.

The ongoing notification identifies the event, last check time, driving duration, buffer and suggested leave time. Errors explicitly invalidate previous traffic estimates and recommend Maps. Stop from the event or notification. Only one watch runs at a time. Each session ends at arrival or after two hours. Trips use the phone's timezone at watch creation; ambiguous/nonexistent daylight-saving times are rejected.

This is not automatic all-day monitoring, live GPS navigation, or an exact alarm. No location permission is requested. Android battery/Doze restrictions, network loss, force-stop and process termination can interrupt or delay checks. Watch sessions do not restart after reboot/process death. The status text includes its check time; the presence of an ongoing traffic notification indicates a running session. Check Maps before leaving. Updating/deleting an event does not change an already-running watch; stop and restart it with the new details.

## Connect later

1. Create a Google Cloud project, enable **Routes API** and billing, and set a strict Routes API quota. Traffic-aware requests may be billed. Restrict the Google key to Routes API and your server's egress IP where supported.
2. Generate a random private token, e.g. `python -c "import secrets; print(secrets.token_urlsafe(32))"`. Keep the Google key and token in the server environment as `GOOGLE_ROUTES_API_KEY` and `MOMENT_ACCESS_TOKEN`. Never commit them.
3. Run Python 3.10+ `python server.py` behind an HTTPS reverse proxy forwarding only `/route` to `127.0.0.1:8080`. The server binds loopback by default. Set reverse-proxy body/time/connection limits. Do not expose the plain HTTP listener. Use one server process for this private prototype. No deployment or paid hosting is provisioned here.
4. In event details → **Traffic connection settings**, enter `https://YOUR_HOST/route` and the private token (not the Google key). This explicitly enables the connection. Start a watch and allow notifications. Disconnect stops the watch and removes saved settings.

The endpoint sends only origin and destination to Google, using `DRIVE` and `TRAFFIC_AWARE_OPTIMAL`; no event title or calendar data is sent. The access token is an app-private credential stored in Android private preferences and excluded from cloud backup/device transfer; it is not hardware-encrypted. Do not reuse it for any other service. The prototype server is for a single trusted user, not public multi-user distribution. Add per-user authentication and durable distributed limits before public release.

Server guardrails: constant-time token checks, no request logging, no-store responses, bounded input/output, network timeouts, 45-second minimum spacing and default 150-request daily cap. `DAILY_REQUEST_LIMIT` can lower the cap. In-memory counters reset on restart, so configure Google's quota as the durable spending limit. Do not use request/response body logging at your reverse proxy. No historical route cache is kept.

## Validation

`python -m unittest discover -s traffic-server -v` validates the outbound Google request, duration rounding, invalid addresses, missing routes, malformed durations, and fallback rejection without making paid requests. `tests/DeparturePlanTest.java` verifies departure/buffer math, get-ready/leave-now boundaries and traffic-change thresholds. CI runs these plus the Android debug build.

Live Google credentials, a deployed HTTPS proxy and phone notification/Doze testing are still required for end-to-end validation. The app must not be advertised as live-traffic-ready until these pass.

References: https://developers.google.com/maps/documentation/routes/compute_route_directions , https://developers.google.com/maps/documentation/routes/config_trade_offs , https://developers.google.com/maps/documentation/routes/usage-and-billing , https://developer.android.com/develop/background-work/services/fgs/timeout
