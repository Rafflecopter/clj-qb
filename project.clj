(defproject com.rafflecopter/qb "0.2.0-SNAPSHOT"
  :description "Queue interface and helpers"
  :url "https://github.com/Rafflecopter/clj-qb"
  :license {:name "MIT"
            :url "https://github.com/Rafflecopter/clj-qb/blob/master/LICENSE"}
  :scm {:name "git"
        :url "https://github.com/Rafflecopter/clj-qb"}
  :deploy-repositories [["clojars" {:creds :gpg}]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy" "clojars"]]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]]}})
