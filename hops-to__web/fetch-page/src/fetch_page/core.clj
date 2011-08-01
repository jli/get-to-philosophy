(ns fetch-page.core
  (:use [ring.adapter.jetty :only [run-jetty]])
  (:gen-class))

(defn app [req]
  (let [url (:query-string req)
        page (.getContent (java.net.URL. url))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body page}))

(defn -main []
   (run-jetty app {:port 8080}))
