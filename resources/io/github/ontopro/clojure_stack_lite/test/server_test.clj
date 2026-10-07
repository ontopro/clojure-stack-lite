(ns {{main/ns}}.server-test
  "The application's handler as a whole - its middleware, its routes, its static files and its
  page not found - called as a function, the way `handlers-test` calls one handler. No server,
  no port, no database: what the middleware adds to a response is seen on the response map.
  An error is tried through the same handler, its routes swapped for one route of the test's
  own that throws, answers wrongly, or is sent a body it cannot decode - on purpose.

  DO NOT COPY THE ROUTE SWAP FOR ORDINARY TESTS. `with-redefs` replaces the routes for every
  thread, not this test's alone, and is safe here only because the test runner is
  single-threaded (`:multithread? false` in deps.edn and dev/user.clj). A handler is tested as a
  function, the way `handlers-test` does."
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [{{main/ns}}.routes :as app-routes]
            [{{main/ns}}.server :as server]
            [{{main/ns}}.test-utils :as utils]))

(defn- app
  []
  (server/ring-handler {:options {:session-secret-key utils/TEST-SECRET-KEY
                                  :cookie-attrs-secure? false}}))

(deftest test-security-headers-on-every-kind-of-response
  (doseq [[uri status] [["/" 200]
                        ["/no-such-page" 404]
                        ["/assets/js/htmx.min.js" 200]]]
    (testing uri
      (let [response ((app) {:request-method :get
                             :uri uri})]
        (is (= status (:status response)))
        (is (= "nosniff" (get-in response [:headers "X-Content-Type-Options"])))
        (is (= "SAMEORIGIN" (get-in response [:headers "X-Frame-Options"])))
        (is (some? (get-in response [:headers "Strict-Transport-Security"])))
        (is (= "0" (get-in response [:headers "X-XSS-Protection"])))
        (is (= "strict-origin-when-cross-origin" (get-in response [:headers "Referrer-Policy"])))))))

(deftest test-the-session-cookie-is-http-only-and-same-site-lax
  (let [set-cookie (-> ((app) {:request-method :get
                               :uri "/"})
                       (get-in [:headers "Set-Cookie"])
                       (first))]
    (is (str/starts-with? set-cookie "ring-session="))
    (is (str/includes? set-cookie "HttpOnly"))
    (is (str/includes? set-cookie "SameSite=Lax"))))

(defn- answer-from
  "Answer `request` with the application's own handler, its routes swapped for the one route
  `/probe` with `route-data`, so a failure made on purpose passes through the whole middleware."
  [route-data request]
  (with-redefs [app-routes/routes [["/probe" route-data]]]
    ((app) (merge {:uri "/probe"} request))))

(defn- names-none-of
  [response internals]
  (doseq [internal internals]
    (is (not (str/includes? (:body response) internal)) internal)))

(deftest test-an-exception-answers-the-error-page-and-nothing-of-it
  ; the log line and stack trace this prints are expected: the exception is thrown on purpose
  (let [response (answer-from {:get {:handler (fn [_] (throw (ex-info "thrown on purpose by server-test"
                                                                      {:data-only-in-the-exception true})))}}
                              {:request-method :get})]
    (is (= 500 (:status response)))
    (is (str/starts-with? (get-in response [:headers "Content-Type"]) "text/html"))
    (is (= "nosniff" (get-in response [:headers "X-Content-Type-Options"])))
    (names-none-of response ["thrown on purpose" "data-only-in-the-exception" "ExceptionInfo" "server_test"])))

(deftest test-a-failed-response-coercion-answers-the-error-page-and-nothing-of-it
  (let [response (answer-from {:get {:handler (fn [_] {:status 200
                                                       :body {:value-only-in-the-handler true}})
                                     :responses {200 {:body string?}}}}
                              {:request-method :get})]
    (is (= 500 (:status response)))
    (names-none-of response ["value-only-in-the-handler" "string?"])))

(deftest test-a-body-that-fails-to-decode-answers-the-error-page-and-nothing-of-it
  (let [response (answer-from {:post {:handler (fn [_] {:status 200
                                                        :body "never reached"})}}
                              {:request-method :post
                               :headers {"content-type" "application/json"}
                               :body (java.io.ByteArrayInputStream. (.getBytes "{not json"))})]
    (is (= 400 (:status response)))
    (names-none-of response ["Malformed" "JsonParseException" "muuntaja"])))
