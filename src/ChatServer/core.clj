(ns ChatServer.core
  (:gen-class)
  (:use server.socket)
  (:import
    (java.io PrintWriter InputStreamReader OutputStreamWriter BufferedReader)
    (java.util Date)
    (java.text DateFormat)))

;(apply require clojure.main/repl-requires)

(def channels (ref {}))
(def nicknames (ref {}))
(def ^:dynamic *nickname* "Administrator")

; Utils
(defn parse-string [str]
  (read-string (second (re-matches (re-pattern "(\\d+).*") str))))
(defn now []
  (let [java-date (java.util.Date.)
        java-format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")]
    (.format java-format java-date)))

; Various message displaying functions
(defn print-usage []
  (println "Usage: ChatServer [port-number]")
  (println "Specify a valid port number between 1024 and 65535"))
(defn print-welcome-message []
  (println "Welcome to ChatServer 0.1")
  (print "Enter your nickname: ")
  (flush))
(defn print-help-message []
  (println "You are now known as" *nickname*)
  (println "Type '/help' for a list of commands")
  (flush))
(defn print-commands []
  (println "A list of supported commands:")
  (println "/help: displays this list")
  (println "/channels: shows a list of active channels on the server")
  (println "/join #channel: connects you to the specified channel. If such channel does not exist - creates one")
  (println "/who: shows a list of users in the current channel")
  (println "/leave: removes you from the active channel")
  (println "/exit: disconnects you from the server")
  "")

; Channel related functions
(defn get-channel! [name]
  (dosync
    (or (@channels name)
        (let [channel (agent (list))]
          (do (alter channels assoc name channel) channel)))))

(defn agent-broadcast-to-channel [channel message]
  (doseq [[nickname output] channel]
    (if (not= nickname *nickname*)
      (binding [*out* output]
        (println message))))
  channel)

(defn agent-add-user-to-channel [channel nickname output]
  (conj channel [nickname output]))

(defn agent-remove-user-from-channel [channel nickname]
  (remove #(= (% 0) nickname) channel))

(defn list-channels []
  (let [channel-names (keys @channels)]
    (doseq [channel channel-names]
      (println ">" channel)))
  "")

; User related functions
(defn join-channel [channel]
  (send channel agent-add-user-to-channel *nickname* *out*)
  (send channel agent-broadcast-to-channel (str "> " *nickname* " has joined the channel.")))

(defn leave-channel [channel]
  (send channel agent-remove-user-from-channel *nickname*)
  (send channel agent-broadcast-to-channel (str "> " *nickname* " has left the channel.")))

(defn display-users [channel-name]
  (println "> Users in" channel-name)
  (doseq [user @(get-channel! channel-name)]
    (println "*" (user 0)))
  "")

(defn send-chat-message [channel message]
  (send channel agent-broadcast-to-channel (str *nickname* ": " message))
  "")

(defn cleanup []
  (doseq [channel (vals @channels)]
    (when-first [active-channel (filter #(= *out* (% 1)) @channel)]
      (send channel agent-broadcast-to-channel (str *nickname* " has disconnected")))
    (send channel agent-remove-user-from-channel *nickname*)))

(defn parse-command [[first-letter & others :as everything] current-channel]
  (if (= first-letter \/)
    (let [split (clojure.string/split everything #" ")
          command (first split)]
      (condp = command
        "/help" (print-commands)
        "/channels" (list-channels)
        "/join" (let [new-channel-name (apply str (rest split))]
                  (if (= new-channel-name "")
                    (println "Please specify a valid channel name")
                    (do
                      (leave-channel (get-channel! current-channel))
                      (join-channel (get-channel! new-channel-name))))
                  new-channel-name)
        "/who" (display-users current-channel)
        "/exit" nil
        (println (str "Unknown command '" command "', type '/help' for a list of commands"))
        ""))
    (send-chat-message (get-channel! current-channel) everything)))

(defn new-connection [in out]
  (println (now) "New user connected")
  (binding [*in* (BufferedReader. (InputStreamReader. in))
            *out* (OutputStreamWriter. out)]
    (print-welcome-message)
    (binding [*nickname* (read-line)]
      (print-help-message)
      (println "You have joined #Bulgaria")
      (join-channel (get-channel! "Bulgaria"))
      ;(print (str *nickname* "@Bulgaria: "))
      ;(flush)
      (try
        (loop [input (read-line) current-channel "Bulgaria"]
          (let [result (parse-command input current-channel)]
            (cond
              (= result nil) :done
              (= result "") (do
                              ;(print (str *nickname* "@" current-channel ": "))
                              (flush)
                              (recur (read-line) current-channel))
              :else (do
                      ;(print (str *nickname* "@" result ": "))
                      (flush)
                      (recur (read-line) result))))) ; result contains the name of the new channel
      (finally (cleanup))))))

(defn chat-server [port]
  (create-server port new-connection))

(defn -main [& args]
  (let [port (first args)]
    (if
      (or
        (nil? port)
        (not (integer? (parse-string port)))
        (< (parse-string port) 1024)
        (> (parse-string port) 65535))
      (print-usage)
      (chat-server (parse-string port)))))