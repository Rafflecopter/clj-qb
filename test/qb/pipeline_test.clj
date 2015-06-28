(ns qb.pipeline-test
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer (<!! >!!) :as async]
            [qb.core :as qb]
            [qb.util :as qbu]
            [qb.pipeline :refer (mux-senders)]))

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

(facts "about mux-senders"
  (let [c (mux-senders [{:sender (TestSender.) :dest-fn (fn [_] "one") :filter-fn :one}
                        {:sender (TestSender.) :dest-fn (fn [_] "two") :filter-fn :two}])
        rc (qbu/result-chan)]
    (>!! c {:msg "abc" :one true})
    (>!! c {:msg "def" :two true :result rc})
    (>!! c {:msg "ghi" :one true :two true})
    (fact "result channel should be closed"
      (pull!! rc) => nil)
    (fact "calls to TestSender are correct"
      (get-calls)
      => #{{:dest "one" :msg "abc"}
           {:dest "one" :msg "ghi"}
           {:dest "two" :msg "def"}
           {:dest "two" :msg "ghi"}})))
