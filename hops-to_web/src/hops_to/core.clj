(ns hops-to.core
  (:use [ring.adapter.jetty :only [run-jetty]]
        [ring.util.response :only [file-response]]
        [ring.middleware.file :as file]
        [hops-to.parse-links :only [parse-links]])
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
  (condp #(.startsWith %1 %2) (:uri req)
    "/fetch" (response (fetch-page (:query-string req)))
    "/links" (response (str (into [] (parse-links (:query-string req)))))
    ;;"/hops.js" (file-response "/home/jli/projects/hops-to/hops-to_web/hops.js")
    (file-response "hops.html")
    ))

(defn -main []
   (run-jetty (file/wrap-file app ".") {:port 8080}))
