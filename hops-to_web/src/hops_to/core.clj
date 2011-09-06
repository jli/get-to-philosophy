(ns hops-to.core
  (:use [ring.adapter.jetty :only [run-jetty]]
        [ring.util.response :only [file-response]]
        [ring.middleware.file :only [wrap-file]]
        ;; [ring.middleware.reload :only [wrap-reload]]
        [ring.middleware.stacktrace :only [wrap-stacktrace]]
        [ring.middleware.gzip :only [wrap-gzip]]
        [clojure.contrib.command-line :only [with-command-line]]
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

(defn base-app [req]
  (condp #(.startsWith %2 %1) (:uri req)
    "/fetch" (response (fetch-page (:query-string req)))
    "/wiki" (response (fetch-wiki-page (:query-string req)))
    "/links" (response (str (into [] (parse-links/parse-links (:query-string req)))))
    "/love" (response "fear is the heart of love")
    (file-response "index.html")
    ))

(def app
     (-> #'base-app
         (wrap-file ".")
         ;; (wrap-reload '[hops-to.core hops-to.parse-links]) ; too slow
         wrap-stacktrace
         wrap-gzip))

(defn -main [& args]
  (with-command-line args
    "hoppity!"
    [[jetty-port j "jetty port" "8080"]
     [no-swank? "don't start swank server" false]
     [swank-port s "swank port" "8081"]]
    (when (not no-swank?)
      (swank.swank/start-server :port (Integer/parseInt swank-port)))
    (run-jetty #'app {:port (Integer/parseInt jetty-port)})))
