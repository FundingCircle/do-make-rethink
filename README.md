# Do Make Rethink

[![Circle CI](https://circleci.com/gh/FundingCircle/do-make-rethink.svg?style=svg)](https://circleci.com/gh/FundingCircle/do-make-rethink)

A Clojure library for making [RethinkDB](https://rethinkdb.com/) databases.

## Usage


[![Clojars Project](https://img.shields.io/clojars/v/org.fundingcircle/do-make-rethink.svg)](https://clojars.org/org.fundingcircle/do-make-rethink)

```clojure
(require '[do-make-rethink.core :as mr])
(require '[rethinkdb.query :as r])

(def db-schema {:easy_table {}
                :medium_table {:my_index `(r/fn [row] (r/get-field row :some_field))}
                :hard_table {:multi_index {:index-fn `(r/fn [row]
                                                        [(r/get-field row :field_a)
                                                         (r/get-field row :field_b)])
                                           :opts {:multi true}}}})

(with-open [conn (r/connect :host "localhost" :port 28015 :db "my-db")]
  (mr/build-db conn "my-db" db-schema))
```

The main entry point is the `build-db` function. `build-db` will traverse over the provided
database schema and creating any missing databases/tables/indexes. Upon return, your database
should be in a consistent state with the schema.


## License

Copyright © 2015 Funding Circle

Distributed under the BSD 3-Clause License.
