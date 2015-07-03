# qb [![Build Status][1]][2]

A Message Queue and Work Queue interface for clojure apps.

```
[com.rafflecopter/qb "0.1.0"]
```

qb is only an interface, so you'll need to include one of the following qb-compatible implementations:

```
[com.rafflecopter/relyq "0.1.0"]
```

## Usage

```clojure
(ns your-namespace-here
  (:require [qb.core :as qb]
            [qb.util :as qbutil]
            qb.relyq.core
            [clojure.core.async :refer (go-loop <! close!)]))

;; See your implementation's docs for config options
(def config {:type :relyq ...})
(def q (qb/init! config))

;; Send messages to a destination
;; Destinations are implementation-specific
(qb/send! q "dest-location" {:some :message})

;; Start a message listener at a source location
;; Sources are implementation-specific
(let [{:keys [data stop]} (qb/listen q "source-location"))]

  ;; data is a channel of {:result result-chan :msg msg}
  (go-loop []
    (let [{:keys [result msg]} (<! data)]
      (try (handle-msg msg)
           ;; Notify the queue of successful processing
           (qbutil/success result)
        (catch Exception e
          ;; Notify the queue of an error in processing
          (qbutil/error result (.getMessage e))))
    (recur))

  ;; At some point, you can stop the listener by closing the stop channel
  ;; Some implementations take a bit to close, so you should wait
  ;; until the data channel is closed to exit gracefully.
  (close! stop))
```

## Writing implementations

At the moment, check out the documentation in `qb.core` and look at `qb.relyq.core` in [clj-relyq](https://github.com/Rafflecopter/clj-relyq).

## License

See [LICENSE](https://github.com/Rafflecopter/clj-qb/blob/master/LICENSE) file


[1]: https://travis-ci.org/Rafflecopter/clj-qb.png?branch=master
[2]: http://travis-ci.org/Rafflecopter/clj-qb