(ns qb.util
  "Helpers for implementations of qb."
  (:require [clojure.core.async :refer (go go-loop chan >! <! close! alt!)])
  (:import [clojure.core.async.impl.channels ManyToManyChannel]))

;; ack channels

(defn ack-chan
  "Create a ack channel.
  ack channels close on success
  and put an error through on error (then closed.)"
  [] (chan))
(defn ack-success [ackc]
  (if (instance? ManyToManyChannel ackc)
    (close! ackc)))
(defn nack-error [ackc err]
  (if (instance? ManyToManyChannel ackc)
    (go (>! ackc err) (close! ackc))))

;; pipes an ack chan to possibly many other ack chans

(defn pipe-ack [from & tos]
  (go-loop []
    (let [v (<! from)]
      (if v (do (doseq [to tos] (>! to v)) (recur))
            (doseq [to tos] (close! to))))))

(defn wrap-ack-chan-xf
  "Create an xform that can be used to map a channel of messages
  to a channel of {:ack ack-chan :msg msg}.
  on-success and on-error run asynchronously when ack-chan is resolved"
  [on-success on-error]
  (map (fn [msg]
    (let [ackc (ack-chan)]
      (go (if-let [res (<! ackc)]
            (on-error msg res)
            (on-success msg)))
      {:ack ackc :msg msg}))))

(defmacro ack-blocking-op
  "Wrap a blocking operation in a try/catch
  Acknowledge the ack-chan with success or failure"
  [ack & op]
  `(try ~@op
        (ack-success ~ack)
     (catch Exception e#
       (nack-error ~ack {:error (.getMessage e#)}))))

(defmacro ack-blocking-op*
  "Create an ack-chan and perform ack-blocking-op*"
  [& op]
  `(let [ack# (ack-chan)]
    (ack-blocking-op ack# ~@op)
    ack#))

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
