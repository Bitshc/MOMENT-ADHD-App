import io
import json
import unittest
import os
import threading
import urllib.error
import urllib.request
from unittest.mock import patch
import server
from server import compute_route


class RoutesTest(unittest.TestCase):
    def reply(self, value):
        return lambda *args, **kwargs: io.BytesIO(json.dumps(value).encode())

    def test_live_request_and_rounding(self):
        def opener(request, timeout):
            body = json.loads(request.data)
            self.assertEqual(body["routingPreference"], "TRAFFIC_AWARE_OPTIMAL")
            self.assertEqual(body["origin"], {"address": "123 Start St"})
            self.assertEqual(body["travelMode"], "DRIVE")
            self.assertEqual(timeout, 15)
            return io.BytesIO(b'{"routes":[{"duration":"120.2s"}]}')
        self.assertEqual(compute_route({"origin": "123 Start St", "destination": "456 End St"}, "test", opener)["durationSeconds"], 121)

    def test_no_fabricated_estimate(self):
        for response in ({}, {"routes": []}, {"routes": [{}]}, {"routes": [{"duration": "NaNs"}]}, {"routes": [{"duration": "-1s"}]}, {"routes": [{"duration": "999999s"}]}, {"fallbackInfo": {}, "routes": [{"duration": "120s"}]}):
            with self.subTest(response=response), self.assertRaises(ValueError):
                compute_route({"origin": "Start", "destination": "Finish"}, "test", self.reply(response))

    def test_validation_before_provider_call(self):
        for payload in ([], {}, {"origin": "", "destination": "Finish"}, {"origin": 42, "destination": "Finish"}):
            with self.assertRaises(ValueError):
                compute_route(payload, "test", lambda *a, **k: self.fail("Must not call Google"))


class HttpTest(unittest.TestCase):
    def setUp(self):
        self.env = patch.dict(os.environ, {"MOMENT_ACCESS_TOKEN": "x" * 40, "GOOGLE_ROUTES_API_KEY": "test"})
        self.env.start()
        server.last_request = 0
        server.requests_today = 0
        self.http = server.ThreadingHTTPServer(("127.0.0.1", 0), server.Handler)
        self.thread = threading.Thread(target=self.http.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.http.shutdown()
        self.http.server_close()
        self.thread.join()
        self.env.stop()

    def post(self, body, token="x" * 40):
        req = urllib.request.Request("http://127.0.0.1:%s/route" % self.http.server_port,
            data=json.dumps(body).encode(), headers={"Authorization": "Bearer " + token}, method="POST")
        try:
            with urllib.request.urlopen(req) as response:
                return response.status, json.load(response)
        except urllib.error.HTTPError as response:
            return response.code, json.load(response)

    @patch("server.compute_route")
    def test_auth_and_input_do_not_call_provider(self, compute):
        self.assertEqual(self.post({}, "wrong")[0], 401)
        self.assertEqual(self.post({})[0], 400)
        compute.assert_not_called()

    @patch("server.compute_route", return_value={"durationSeconds": 120, "checkedAt": 123})
    def test_success_then_rate_limit(self, compute):
        body = {"origin": "Start", "destination": "Finish"}
        self.assertEqual(self.post(body)[0], 200)
        self.assertEqual(self.post(body)[0], 429)
        self.assertEqual(compute.call_count, 1)

    @patch("server.compute_route", side_effect=RuntimeError("provider secret error"))
    def test_provider_failure_is_sanitized(self, compute):
        code, body = self.post({"origin": "Start", "destination": "Finish"})
        self.assertEqual(code, 503)
        self.assertNotIn("secret", str(body))


if __name__ == "__main__":
    unittest.main()
