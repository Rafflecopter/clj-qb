(ns qb.pipeline-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer (<!! >!!) :as async]
            [qb.core :as qb :refer (send-chan)]
            [qb.util :refer (ack-chan ack-success)]))

(def calls (atom #{}))
(defn get-calls [] (let [c @calls] (reset! calls #{}) c))
(defrecord TestSender []
  qb/Sender
  (send! [_ dest msg]
    (swap! calls conj {:dest dest :msg msg})
    (doto (qbu/result-chan) qbu/success)))

(defn pull!! [c & [timeout-time]]
  (let [t (async/timeout (or timeout-time 100))]
    (async/alt!! c ([v] v)
                 t ([_] :timeout))))

(facts "about send-chan"
  (let [{:keys [items done]} (send-chan (TestSender.) "mydest")
        ack (ack-chan)]
    (>!! items {:text "abc"})
    (>!! items {:msg {:text "def"} :ack ack})
    (fact "ack channel should be closed"
      (pull!! ack) => nil)
    (<!! (async/timeout 50))
    (fact "calls to TestSender are correct"
      (get-calls)
      => #{{:dest "mydest" :msg {:text "abc"}}
           {:dest "mydest" :msg {:text "def"}}})))
