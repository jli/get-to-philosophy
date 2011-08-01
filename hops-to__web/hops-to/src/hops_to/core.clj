(ns hops-to.core
  (:use [ring.adapter.jetty :only [run-jetty]])
  (:gen-class))


;;; util

(defn response [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(defn fetch-page [url]
  (.getContent (java.net.URL. url)))


;;; routing/app

(defn app [req]
  (if (.startsWith (:uri req) "/fetch")
    (response (fetch-page (:query-string req)))
    (response "oh hi")))

(defn -main []
   (run-jetty app {:port 8080}))
