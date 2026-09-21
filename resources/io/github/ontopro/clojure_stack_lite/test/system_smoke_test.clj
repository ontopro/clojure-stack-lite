(ns {{main/ns}}.system-smoke-test
  "THE ONE TEST THAT BOOTS THE WHOLE SYSTEM - web server, database and all - on a
  free port, and talks to it over HTTP. It answers a single question: does the
  system start and respond? Nothing else can: every other gate can be green over
  a system that refuses to boot (a component config key the schema does not
  allow, a missing resource, a bad migration).

  DO NOT COPY THIS PATTERN FOR ORDINARY TESTS. It is slow, it needs a port and a
  database, and it cannot run beside another copy of itself. A handler is a
  function of a request map: test it as one, the way `handlers-test` does. Keep
  this namespace to a handful of checks that the running system answers at all,
  and keep them independent of page content, which changes."
  (:require [clj-http.client :as http]
            [clojure.test :refer :all]
            [reitit-extras.tests :as reitit-extras]
            [{{main/ns}}.test-utils :as utils]))

(use-fixtures :once
  (utils/with-system))

(use-fixtures :each
  utils/with-truncated-tables)

(deftest test-system-boots-and-answers
  (let [url (reitit-extras/get-server-url (utils/server) :host)]
    (testing "the health endpoint"
      (let [response (http/get (str url "/health"))]
        (is (= 200 (:status response)))
        (is (= "OK" (:body response)))))
    (testing "the home page is served as HTML"
      (let [response (http/get url)]
        (is (= 200 (:status response)))
        (is (some? (utils/response->hickory response)))))))
