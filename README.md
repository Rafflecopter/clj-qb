# qb

A Message Queue and Work Queue interface for clojure apps.

```
[com.rafflecopter "0.1.0"]
```

## Usage

```clojure
(require '[qb.core :as qb]
         'qb.impl.relyq)
         ; from https://github.com/Rafflecopter/clj-relyq

(let [q (qb/init {:type :relyq :other :stuff})
      received-msgs (qb/listen q "source")]
    (qb/send! q "destination" {:message :stuff}))
```

## License

See [LICENSE](https://github.com/Rafflecopter/clj-qb/blob/master/LICENSE) file