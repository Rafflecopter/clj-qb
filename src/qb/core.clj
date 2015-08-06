(ns qb.core
  (:require [clojure.core.async :refer (chan) :as async]))

(defmulti init!
  "Initialize using a config object.
  Return an instance of a Listener, Sender, or both"
  :type)

(defprotocol Listener
  "A Listener listens for messages from an external service.
  Listeners could be push-based like HTTP endpoints,
  or pull-based like a RabbitMQ work queue or message topic."
  (listen [listener source]
    "Start a listener.
    source is implementation-dependent. Could be a path, topic, etc.
    Return a map of {:data chan :stop chan}.
    data is a channel of items: {:ack ack-chan :msg msg}.
    stop is a channel, which upon closing, all listener threads
    should cease and data closed."))

(defprotocol Sender
  "A Sender sends messages to an external service."
  (send! [sender destination msg]
    "Send a message to a destination.
    Destination is implementation-dependent. Could be a url, hostname, topic, etc.
    Return an ack-chan managed by sender."))

;; Helpers for applications

(defn- const [obj] (fn [& _] obj))

(defn- send-chan-exec [sender dest-fn ?map-fn {:keys [msg ack] :as item} done]
  (let [msg (if (and ack msg) msg item)
        dest (dest-fn msg)
        msg (if ?map-fn (?map-fn msg) msg)
        s-ack (send! sender dest msg)
        s-ack-mult (async/mult s-ack)]
    (if ack (async/tap s-ack-mult ack))
    (async/tap s-ack-mult done)
    (async/close! done)))

(defn send-chan
  "Wrap a sender with a channel of messages to send.
  Returns {:data data :done done}
  data is a channel that should have data of a msg or {:msg msg :ack ack-chan}
  put on it.
  done is a channel that will be closed when data is closed an all ack-chans
  returned by send! have closed.
  dest is either a destination or a function of msg that returns a destination.
  Optionally, if a map-fn is supplied, it will be applied to msg before sending.
  If an item contains an ack-chan, it will be connected with send!'s returned ack-chan."
  [^qb.core.Sender sender & {:keys [dest dest-fn map-fn]}]
  (let [dest-fn (or dest-fn (const dest))
        data (chan)
        done (chan 1 (filter (fn [_] false)))]
    (async/pipeline-async 100 done (partial send-chan-exec sender dest-fn map-fn) data)
    {:data data :done done}))