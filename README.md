# qb [![Build Status][1]][2]

A Message Queue and Work Queue interface for clojure apps.

qb is only an interface, so you'll need to include one of the following qb-compatible implementations:

- [relyq](https://github.com/Rafflecopter/clj-relyq)

## Usage

### High Level

`qb` is built on the notion that everything is a channel of items to be piped through operations. Applications can produce items to be put on queues, or consume them, but most applications do both. Here is an example of how to connect a listener from a source location through an operation to a destination location. See documentation for [pipeline](http://clojure.github.io/core.async/#clojure.core.async/pipeline) to get familiar with how this works.

```clojure
(require '[qb.core :as qb]
         'qb.relyq.core ; your implementation here
         '[clojure.core.async :as async])

(let [q (qb/init! config) ; See each implementation's Configuration section
      {from-listener :data} (qb/listen q "source")
      {to-sender :data} (qb/send-chan q "destination")]
  (async/pipeline parallelism to-sender my-operation to-sender))
```

### Graceful Shutdown

When everything is in a piped channels, graceful shutdown becomes easy to do. Closing the `stop` channel, returned by `qb/listen`, will notify the listener to stop what it is doing and close the `from-listener` channel. Through the design of `core.async`, this will in turn close all channels once their work is done. Eventually the `done` channel, returned by `qb/send-chan`, will be closed when all work has been stopped. Then it is OK to go ahead with shutdown. This example shows how to use the JVM shutdown hook to block shutdown until you have gracefully stopped execution. Consider using `async/timeout` and `async/alt!!` to add a timeout here.

```clojure
(require '[qb.core :as qb]
         '[clojure.core.async :as async :refer (close!)])

(let [q (qb/init! config) ; See Configuration section
      {from-listener :data stop :stop} (qb/listen q "source")
      {to-sender :data done :done} (qb/send-chan q "destination")]
  ...
  (.addShutdownHook (Runtime/getRuntime)
    (Thread. #(do (close! stop)
                  (<!! done)))))
```


## Writing implementations

At the moment, check out the documentation in `qb.core` and look at `qb.relyq.core` in [clj-relyq](https://github.com/Rafflecopter/clj-relyq).

## License

See [LICENSE](https://github.com/Rafflecopter/clj-qb/blob/master/LICENSE) file


[1]: https://travis-ci.org/Rafflecopter/clj-qb.png?branch=master
[2]: http://travis-ci.org/Rafflecopter/clj-qb