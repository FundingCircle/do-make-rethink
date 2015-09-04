(ns do-make-rethink.core-test
  (:require [clojure.test :refer :all]
            [rethinkdb.query :as r]
            [do-make-rethink.core :as mr]))

(def db-name "do_make_rethink_test")
(def table-name "my_table")
(def simple-index-name "simple_index")
(def multi-index-name "multi_index")

(defn connect []
  (r/connect :host "localhost"
             :port 28015
             :db db-name))

(defn index-status [conn table-name index-name]
  (-> (r/table table-name)
      (r/index-status index-name)
      (r/run conn)
      first
      (select-keys [:geo :index :multi :ready])))

(defn drop-db-if-exists [conn name]
  (if (mr/db-exists? conn name)
    (-> (r/db-drop name)
        (r/run conn))))

(defn drop-db-fixture [f]
  (with-open [conn (connect)]
    (drop-db-if-exists conn db-name))
  (f))

(use-fixtures :each drop-db-fixture)

(def db-schema {table-name {simple-index-name `(r/fn [row] (r/get-field row :field))
                            multi-index-name {:index-fn `(r/fn [row]
                                                           [(r/get-field row :field1)
                                                            (r/get-field row :field2)])
                                              :opts {:multi true}}}})

(deftest build-db
  (with-open [conn (connect)]
    (is (nil? (mr/build-db conn db-name db-schema))) ; Does DB setup
    (is (nil? (mr/build-db conn db-name db-schema))) ; Should do nothing

    (is (true? (mr/db-exists? conn db-name)))
    (is (true? (mr/table-exists? conn db-name table-name)))

    (testing "with a simple index"
      (is (true? (mr/index-exists? conn db-name table-name simple-index-name)))
      (is (= {:geo false, :index simple-index-name, :multi false, :ready true}
             (index-status conn table-name simple-index-name))))

    (testing "with a multi index"
      (is (true? (mr/index-exists? conn db-name table-name multi-index-name)))
      (is (= {:geo false, :index multi-index-name, :multi true, :ready true}
             (index-status conn table-name multi-index-name))))))
