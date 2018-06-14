(ns user
  (:require [ring.adapter.jetty :as jetty]
            [cryogen.server :refer [handler init]]
            [clojure.java.io :as io]
            [clj-time.core :as time]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [cryogen-core.compiler :refer [read-config]]
            [clj-time.format :as tformat]))

(defonce server (atom nil))

(defn start-server
  ([] (start-server 3000))
  ([port]
   (when-not @server
     (init)
     (reset! server (jetty/run-jetty handler {:port port :join? false})))))

(defn stop-server []
  (when @server
    (.stop @server)
    (reset! server nil)))

(def base-opts {:layout :post
                :tags [""]
                :toc false})

(def base-text
  "###Header
Some Text")

(def base-dir "resources/templates/md/")

(defn- today-str [config]
  (-> config
      :post-date-format
      tformat/formatter
      (tformat/unparse-local-date (time/today))))

(defn new-post-template [config title]
  (let [opts (assoc base-opts
                    :title title
                    :date (today-str config))
        pp-opts (with-out-str (pprint opts))]
    (str pp-opts "\n\n" base-text)))

(defn title->filename [title]
  (-> title
      str/lower-case
      (str/replace #" +" "-")
      (str ".md")))

(defn new-post! [title]
  (let [config (read-config)
        template (new-post-template config title)
        out-location (str base-dir (:post-root config) "/" (title->filename title))]
    (spit (io/file out-location) template)
    (println out-location)))

;; (new-post! "My First Cryogen Blog")
