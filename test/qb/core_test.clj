(ns qb.core-test
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
    (doto (ack-chan) ack-success)))

(defn pull!! [c & [timeout-time]]
  (let [t (async/timeout (or timeout-time 100))]
    (async/alt!! c ([v] v)
                 t ([_] :timeout))))

(facts "about send-chan"
  (facts "with static dest and no map-fn"
    (let [{:keys [data done]} (send-chan (TestSender.) :dest "mydest")
          ack (ack-chan)]
      (>!! data {:text "abc"})
      (>!! data {:msg {:text "def"} :ack ack})
      (fact "ack channel should be closed"
        (pull!! ack) => nil)
      (<!! (async/timeout 50))
      (fact "calls to TestSender are correct"
        (get-calls)
        => #{{:dest "mydest" :msg {:text "abc"}}
             {:dest "mydest" :msg {:text "def"}}})))
  (facts "with dest-fn map-fn"
    (let [{:keys [data done]} (send-chan (TestSender.) :dest-fn :dest :map-fn #(dissoc % :dest))
          ack (ack-chan)]
      (>!! data {:text "abc" :dest "foo"})
      (>!! data {:msg {:text "def" :dest "bar"} :ack ack})
      (fact "ack channel should be closed"
        (pull!! ack) => nil)
      (<!! (async/timeout 50))
      (fact "calls to TestSender are correct"
        (get-calls)
        => #{{:dest "foo" :msg {:text "abc"}}
             {:dest "bar" :msg {:text "def"}}}))))
