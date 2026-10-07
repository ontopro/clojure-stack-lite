(ns {{main/ns}}.server-test
  "The application's handler as a whole - its middleware, its routes, its static files and its
  page not found - called as a function, the way `handlers-test` calls one handler. No server,
  no port, no database: what the middleware adds to a response is seen on the response map.
  The exception middleware is tried behind a route of the test's own, which throws or answers
  wrongly on purpose."
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [reitit.coercion.malli :as coercion-malli]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as ring-coercion]
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
  "Call `route-handler` behind the server's exception middleware, as the application would."
  [route-handler]
  (let [handler (ring/ring-handler
                  (ring/router
                    [["/probe" {:get {:handler route-handler
                                      :responses {200 {:body string?}}}}]]
                    {:data {:coercion coercion-malli/coercion
                            :middleware [server/exception-middleware
                                         ring-coercion/coerce-response-middleware]}}))]
    (handler {:request-method :get
              :uri "/probe"})))

(deftest test-an-exception-answers-the-error-page-and-nothing-of-it
  ; the log line and stack trace this prints are expected: the exception is thrown on purpose
  (let [response (answer-from (fn [_] (throw (ex-info "thrown on purpose by server-test"
                                                      {:data-only-in-the-exception true}))))]
    (is (= 500 (:status response)))
    (is (str/starts-with? (get-in response [:headers "Content-Type"]) "text/html"))
    (doseq [internal ["thrown on purpose" "data-only-in-the-exception" "ExceptionInfo" "server_test"]]
      (is (not (str/includes? (:body response) internal)) internal))))

(deftest test-a-failed-response-coercion-answers-the-error-page-and-nothing-of-it
  (let [response (answer-from (fn [_] {:status 200
                                       :body {:value-only-in-the-handler true}}))]
    (is (= 500 (:status response)))
    (doseq [internal ["value-only-in-the-handler" "string?"]]
      (is (not (str/includes? (:body response) internal)) internal))))
