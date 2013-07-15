(defproject ChatServer "0.1.0-SNAPSHOT"
  :description "Simple Chat Server"
  :url "http://www.chalgabox.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [server-socket "1.0.0"]]
  :main ChatServer.core
  :aot [ChatServer.core])
