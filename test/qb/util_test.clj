(ns qb.util-test
  (:require [midje.sweet :refer :all]
            [qb.util :refer :all]
            [clojure.core.async :refer (<!! >!! chan close! alt!! pipe) :as async]))

(defn- take! [c]
  (async/alt!! c ([v] v)
               :default :na))
(defn- put! [c v]
  (async/alt!! [[c v]] :wrote
               :default :na))

(defn- blocking-testfn [c]
  (let [t (async/timeout 25)]
    (async/alt!! c ([v] v)
                 t ([_] nil))))

(facts "about blocking-listener"
  (let [c (chan 1)
        {:keys [data stop]} (blocking-listener blocking-testfn c)]
    (fact "data channel empty to start"
      (take! data) => :na)
    (fact "stop channel empty to start"
      (take! stop) => :na)
    (fact "can put a value on input chan"
      (put! c 10) => :wrote)
    (fact "can pull same value off of data chan"
      (<!! data) => 10)
    (fact "nothing more on data chan"
      (take! data) => :na)
    (fact "can put another value on input chan"
      (put! c 11) => :wrote)
    (<!! (async/timeout 50))
    (fact "stop channel still empty"
      (take! stop) => :na)
    (close! stop)
    (fact "second value still sitting on data chan after close"
      (<!! data) => 11)
    (fact "now data chan is closed after stop is closed"
      (<!! data) => nil)))

(facts "about ack-chan"
  (let [call (atom nil)
        in (chan 2)
        out (pipe in
                  (chan 1 (wrap-ack-chan-xf #(reset! call {:op :success :msg %})
                                            #(reset! call {:op :error :msg %1 :error %2}))))]
    (>!! in {:n 1}) (>!! in {:n 2})
    (fact "first message out"
      (let [msg1 (<!! out)]
        (fact "message is wrapped"
          msg1 => (contains {:msg {:n 1}}))
        (fact "ack-success causes on-success call"
          @call => nil
          (if (:ack msg1)
            (ack-success (:ack msg1))
            (:ack msg1) => some?)
          (<!! (async/timeout 50))
          @call => {:op :success :msg {:n 1}})))
    (fact "second message out"
      (let [msg2 (<!! out)]
        (fact "message is wrapped"
          msg2 => (contains {:msg {:n 2}}))
        (fact "nack-error causes on-error call"
          (reset! call nil)
          (if (:ack msg2)
            (nack-error (:ack msg2) {:ya "know"})
            (:ack msg2) => some?)
          (<!! (async/timeout 50))
          @call => {:op :error :msg {:n 2} :error {:ya "know"}})))))

(facts "about ack-blocking-op"
  (facts "when no error"
    (let [ack (ack-blocking-op* (<!! (async/timeout 10)))]
      (fact "ack succeeds" (<!! ack) => nil)))
  (facts "with passed in ack"
    (let [ack (ack-chan)]
      (ack-blocking-op ack (<!! (async/timeout 10)))
      (fact "ack succeeds" (<!! ack) => nil)))
  (facts "with throw"
    (let [ack (ack-blocking-op* (<!! (async/timeout 10)) (throw (Exception. "yo mama")))]
      (fact "ack errors" (<!! ack) => {:error "yo mama"}))))