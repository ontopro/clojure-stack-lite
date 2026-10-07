(ns {{main/ns}}.server-test
  "The application's handler as a whole - its middleware, its routes, its static files and its
  page not found - called as a function, the way `handlers-test` calls one handler. No server,
  no port, no database: what the middleware adds to a response is seen on the response map."
  (:require [clojure.test :refer :all]
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
        (is (some? (get-in response [:headers "Strict-Transport-Security"])))))))
