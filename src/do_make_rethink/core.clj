(ns do-make-rethink.core
  (:require [rethinkdb.query :as r]
            [taoensso.timbre :as log]
            [clojure.string :as s]))

(def ^:private any? (comp boolean some))

(defn- has-item? [x coll]
  (any? (partial = x) coll))

(defn- list-dbs [conn]
  (-> (r/db-list)
      (r/run conn)))

(defn- list-tables [conn]
  (-> (r/table-list)
      (r/run conn)))

(defn- db-exists? [conn db-name]
  (let [db-names (list-dbs conn)]
    (has-item? (name db-name) db-names)))

(defn- table-exists? [conn db-name table-name]
  (let [tables (-> (r/db db-name)
                   (r/table-list)
                   (r/run conn))]
    (has-item? (name table-name)
               tables)))

(defn- index-exists? [conn db-name table-name index-name]
  (let [indexes (-> (r/db db-name)
                    (r/table table-name)
                    (r/index-list)
                    (r/run conn))]
    (has-item? (name index-name)
               indexes)))

(defn- log-header [& headers]
  (->> headers
       (map name)
       (map #(str "[" % "]"))
       (s/join)))

(defn- log-action [headers action]
  (log/info (str (apply log-header headers) " " action)))

(defn- create-db-if-not-exists [conn db-name]
  (if-not (db-exists? conn db-name)
    (do
      (log-action [db-name] "Creating")
      (-> (r/db-create db-name)
          (r/run conn)))
    (log-action [db-name] "Skipping")))

(defn- create-table-if-not-exists [conn db-name table-name]
  (if-not (table-exists? conn db-name table-name)
    (do
      (log-action [db-name table-name] "Creating")
      (-> (r/db db-name)
          (r/table-create table-name)
          (r/run conn)))
    (log-action [db-name table-name] "Skipping")))

(defn- create-index-if-not-exists
  ([conn db-name table-name index-name index-fn]
   (create-index-if-not-exists conn db-name table-name index-name index-fn {}))
  ([conn db-name table-name index-name index-fn index-opts]
   (if-not (index-exists? conn db-name table-name index-name)
     (do
       (log-action [db-name table-name index-name] "Creating")
       (-> (r/db db-name)
           (r/table table-name)
           (r/index-create index-name (eval index-fn) index-opts)
           (r/run conn))
       (log-action [db-name table-name index-name] "Building")
       (-> (r/db db-name)
           (r/table table-name)
           (r/index-wait index-name)
           (r/run conn))
       (log-action [db-name table-name index-name] "Completed"))
     (log-action [db-name table-name index-name] "Skipping"))))

(defn- build-index [conn db-name table-name index-name index-spec]
  (if (map? index-spec)
    (let [{:keys [index-fn opts]} index-spec]
      (create-index-if-not-exists conn db-name table-name index-name index-fn opts))
    (create-index-if-not-exists conn db-name table-name index-name index-spec)))

(defn- build-table [conn db-name table-name table-spec]
  (create-table-if-not-exists conn db-name table-name)
  (doseq [[index-name index-spec] table-spec]
    (build-index conn db-name table-name index-name index-spec)))

(defn build-db [conn db-name db-spec]
  (create-db-if-not-exists conn db-name)
  (doseq [[table-name table-spec] db-spec]
    (build-table conn db-name table-name table-spec)))
