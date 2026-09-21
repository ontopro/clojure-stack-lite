(ns {{main/ns}}.handlers-test
  "THE PATTERN TO COPY. A handler is a function from a request map to a response
  map, so it is tested as one: build a request, call the handler, look at the
  response. No server, no port, no database - these run in milliseconds, in any
  order, and beside any number of other test runs.

  Find things in a page by what they ARE (a tag, an attribute you put there on
  purpose), never by their position or their styling classes, which change."
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [hickory.select :as select]
            [{{main/ns}}.handlers :as handlers]
            [{{main/ns}}.test-utils :as utils]))

(deftest test-home-handler-returns-an-html-page
  (let [response (handlers/home-handler {:request-method :get
                                         :uri "/"})]
    (is (= 200 (:status response)))
    (is (str/starts-with? (get-in response [:headers "Content-Type"]) "text/html"))
    (testing "the body is a whole document with exactly one title"
      (is (= 1 (count (select/select (select/tag :title)
                                     (utils/response->hickory response))))))))

(deftest test-default-handler-carries-its-status-and-message
  (let [handler (handlers/default-handler "Page not found" 404)
        response (handler {:request-method :get
                           :uri "/no-such-page"})]
    (is (= 404 (:status response)))
    (is (str/includes? (:body response) "Page not found"))))
