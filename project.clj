(defproject org.fundingcircle/do-make-rethink "0.2.0"
  :description "A Clojure library for making RethinkDB databases"
  :url "https://github.com/FundingCircle/do-make-rethink"
  :license {:name "BSD 3-clause"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [rethinkdb "0.10.1"]]
  :deploy-repositories {"releases" {:url "https://clojars.org/repo"
                                    :sign-releases false}})
