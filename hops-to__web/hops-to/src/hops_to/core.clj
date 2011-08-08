(ns hops-to.core
  (:use [ring.adapter.jetty :only [run-jetty]]
        [net.cgrand.enlive-html :only [at html-resource text select]])
  (:gen-class))


;;; util

(defn response [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(defn fetch-page [url]
  (.getContent (java.net.URL. url)))



(defn capitalize [s]
  (try (let [first (.substring s 0 1)
             rest (.substring s 1)]
         (str (.toUpperCase first) rest))
       (catch Exception _err s)))

(defn normalize [article]
  (-> article
      (.replace " " "_")
      capitalize))

;; lol, (comp (partial (filter identity)) map)
(defn filter-map [f coll] (filter identity (map f coll)))

(def wiki-prefix "http://en.wikipedia.org/wiki/")

(defn wiki-url [article] (str wiki-prefix (normalize article)))

(defn relevant-link [link]
  (when (.startsWith link "/wiki/")
    (let [base (.substring link (count "/wiki/"))]
      (when (not (or
                  (.startsWith base "Wikipedia:")
                  (.startsWith base "Special:")
                  (.startsWith base "File:")
                  (.startsWith base "Image:")
                  (.startsWith base "Category:")
                  (.startsWith base "Help:")
                  (.startsWith base "Portal:")
                  (.startsWith base "Talk:")
                  (.startsWith base "Template:")
                  (.startsWith base "List_of_")
                  (.equals base "Wiktionary")
                  ))
        base))))

(defn fetch [article]
  (-> article
      wiki-url
      java.net.URL.
      .getContent
      slurp))

;; YES, this is terrible. Sorry.
(defn remove-parens [str]
  (-> #"[^_]\([^)]*\)"
      (.matcher str)
      (.replaceAll "")))

(defn parse-links [article]
  (let [text (fetch article)
        no-parens-text (remove-parens text)
        html (html-resource (java.io.StringReader. no-parens-text))
        ;; drop disambig, coordinates
        filtered (at html
                     [#{:div.dablink
                        :span#coordinates
                        :table
                        :dl
                        :.infobox
                        :.thumb
                        :.vertical-navbox
                        }] nil)
        link-nodes (select filtered [:div#bodyContent :a])
        links (map #(:href (:attrs %)) link-nodes)
        relevant (filter-map relevant-link links)]
    ;; sentences for another day
    ;;(map #(vector "" %) relevant)
    (print-str relevant)))

;(defn parse-links [article]
  ;; don't hammer wiki :)
  ;(Thread/sleep 500)
  ;(parse-links-raw article))


;;; routing/app

(def page
     "<html><head><title>Get To Philosophy</title></head><body>
<iframe width=\"600\" height=\"500\" id=\"iframe\" src=\"http://en.wikipedia.org/wiki/Philosophy\"></iframe>
<br/>
<div id=\"out\"><u>out</u></div>
</body>
<script src=\"hops_to.js\"></script></html>")

(defn app [req]
  (let [uri (:uri req)]
    ;; uh, what's the right thing here?
    (if (.startsWith uri "/fetch")
      (response (fetch-page (:query-string req)))
      (if (.startsWith uri "/parse-links")
        (response (parse-links (:query-string req)))
        (response page)))))

(defn -main []
   (run-jetty app {:port 8080}))
