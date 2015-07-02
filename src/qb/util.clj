(ns qb.util
  (:require [clojure.core.async :refer (go go-loop chan >! <! close! alt!)])
  (:import [clojure.core.async.impl.channels ManyToManyChannel]))

;; Result channels

(defn result-chan
  "Create a result channel.
  Result channels close on success
  and put an error through on error (then closed.)"
  [] (chan))
(defn success [rc]
  (if (instance? ManyToManyChannel rc)
    (close! rc)))
(defn error [rc err]
  (if (instance? ManyToManyChannel rc)
    (go (>! rc err) (close! rc))))

(defn wrap-result-chan-xf
  "Create an xform that can be used to map a channel of messages
  to a channel of {:result result-chan :msg msg}.
  on-success and on-error run asynchronously when result-chan is resolved"
  [on-success on-error]
  (map (fn [msg]
    (let [rc (result-chan)]
      (go (if-let [res (<! rc)]
            (on-error msg res)
            (on-success msg)))
      {:result rc :msg msg}))))

;; Creating data channels from repeated blocking calls

(defn blocking-listener
  "Start a listener executing a blocking call.
  Returns a an object containing keys:
    - :data channel of messages from the queue
    - :stop channel that when closed, will stop
            the listener and close the data chan"
  [block-op & args]
  (let [data (chan)
        stopper (chan)]

    (go-loop [datum nil]
      (when datum (>! data datum))
      (when (= :continue (alt! stopper ([_] (close! data))
                               :default :continue))
        (recur (apply block-op args))))

    {:data data :stop stopper}))