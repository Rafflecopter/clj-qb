(ns qb.util
  (:require [clojure.core.async :refer (go chan >! close!)]))

(defn result-chan
  "Create a result channel.
  Result channels close on success
  and put an error through on error (then closed.)"
  [] (chan))
(defn success [rc] (close! rc))
(defn error [rc err] (go (>! rc err) (close! rc)))
