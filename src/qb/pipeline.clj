(ns qb.pipeline
  (:require [qb.core :as qb]
            [clojure.core.async :refer (chan) :as async]))

(defn- make-send-xform [{:keys [sender filter-fn dest-fn]}]
  (comp (filter (or filter-fn (fn [_] true)))
        (map #(assoc % :dest (dest-fn %)))
        (map #(assoc % :send-rc (qb/send! sender (:dest %) (:msg %))))
        (filter :result)
        (map #(async/pipe (:send-rc %) (:result %)))
        (filter (fn [_] false))))

(defn mux-senders
  "Takes a list of {:sender :filter-fn :dest-fn}
  Returns a channel that should have messages put on it.
  The messages should have key :msg containing the message to send!
  If the message contains :result, it will be piped to send!'s result-chan"
  [senders]
  (let [c (chan)
        mult (async/mult c)]
    (run! (fn [sender-obj]
            (let [c (chan)]
              (async/tap mult c)
              (async/pipe c (chan 1 (make-send-xform sender-obj)))))
          senders)
    c))