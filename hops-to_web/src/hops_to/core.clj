(ns hops-to.core
  (:use [ring.adapter.jetty :only [run-jetty]]
        [ring.util.response :only [file-response]]
        [ring.middleware.file :as file]
        [hops-to.parse-links :as parse-links])
  (:require [swank.swank])
  (:gen-class))


;;; util

(defn response [body]
  {:status 200
   :headers {"Content-Type" "text/html;charset=utf-8"}
   :body body})

(defn fetch-page [url]
  (.getContent (java.net.URL. url)))

(defn fetch-wiki-page [article] (parse-links/fetch article))


;;; routing/app

(defn app [req]
  (condp #(.startsWith %2 %1) (:uri req)
    "/fetch" (response (fetch-page (:query-string req)))
    "/wiki" (response (fetch-wiki-page (:query-string req)))
    "/links" (response (str (into [] (parse-links/parse-links (:query-string req)))))
    "/love" (response "fear is the heart of love")
    ;;"/hops.js" (file-response "/home/jli/projects/hops-to/hops-to_web/hops.js")
    (file-response "hops.html")
    ))

(defn -main []
  ;; DYNAMISM
  (swank.swank/start-server :port 8081)
  (run-jetty (file/wrap-file #'app ".") {:port 8080}))
