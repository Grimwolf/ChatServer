(ns ChatServer.core-test
  (:use clojure.test
        ChatServer.core))

(deftest print-commands-returns-empty-string-success
  (testing "print-commands"
    (is (= (print-commands) ""))))

(deftest list-channels-returns-empty-string-success
  (testing "list-channels"
    (is (= (list-channels) ""))))

(deftest display-users-returns-empty-string-success
  (testing "display-users"
    (is (= (display-users "") ""))))

(deftest parse-string-number-success
  (testing "parse-string successfully parses a number"
    (is (= (parse-string "123z") 123))))