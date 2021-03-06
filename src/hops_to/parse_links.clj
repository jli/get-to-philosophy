(ns hops-to.parse-links
  (:use [net.cgrand.enlive-html :only [at html-resource text select]]))

;;; utils

(defn capitalize [s]
  (try (let [first (.substring s 0 1)
             rest (.substring s 1)]
         (str (.toUpperCase first) rest))
       (catch Exception _ s)))

;; lol, (comp (partial (filter identity)) map)
(defn filter-map [f coll] (filter identity (map f coll)))

;; needs work
(defn normalize [article]
  (-> article
      (.replace " " "_")
      capitalize))


;;; primitives

(def wiki-prefix "http://en.wikipedia.org/wiki/")

(defn wiki-url [article] (str wiki-prefix (normalize article)))

;; needs work
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

;; https://issues.apache.org/jira/browse/TIKA-434
(defn stupid-tagsoup-hack [text]
  (.replace text \return \ ))

;; omfg
(defn timestamp []
  (-> (java.util.Date.) .getTime java.sql.Timestamp. .toString))

;; needs sentence work
(defn parse-links-raw [article]
  (println (timestamp) "parse-links-raw" article)
  (let [text (fetch article)
        text (stupid-tagsoup-hack text)
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
                        :.boilerplate
                        :.further
                        }] nil)
        link-nodes (select filtered [:div#bodyContent :a])
        links (map #(:href (:attrs %)) link-nodes)
        relevant (filter-map relevant-link links)]
    (map #(vector "" %) relevant)))

(defn parse-links-safe [article]
  (try (parse-links-raw article)
       (catch Exception _ [])))

(def parse-links (memoize (comp parse-links-safe normalize)))
