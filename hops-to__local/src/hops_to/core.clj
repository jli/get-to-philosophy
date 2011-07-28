(ns hops-to.core
  (:use [net.cgrand.enlive-html :only [at html-resource text select]]
        [clojure.contrib.command-line :only [with-command-line]])
  (:gen-class))

;;; utils

(defn all-ints
  "[start] Infinite seq of ints, starting at <start> if given."
  ([] (all-ints 1))
  ([start] (iterate inc start)))

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

(defn to-html [article]
  (html-resource (java.net.URL. (wiki-url article))))

;; needs sentence work
(defn parse-links-raw [article]
  (let [html (to-html article)
        ;; drop disambig, coordinates
        filtered (at html
                     [#{:div.dablink
                        :span#coordinates
                        :.infobox
                        :.thumb
                        }] nil)
        link-nodes (select filtered [:div#bodyContent :a])
        links (map #(:href (:attrs %)) link-nodes)
        relevant (filter-map relevant-link links)]
    (map #(vector "" %) relevant)))

(defn parse-links [article]
  ;; don't hammer wiki :)
  (Thread/sleep 500)
  (parse-links-raw article))


;; logic

;; returns:
;; path-type: [[<article> <sentence> <next>] ...]
;; [:path <path-type>]
;; [:cyclestop <path-type>]
;; [:ttl]
;; [:end]
(defn wikidfs
  "[current path seen ttl stop cyclestop loud]
   Pre: ttl non-negative.
   Post: seen set and path vector increased by next link (next
  current). current is included in seen."
  [cur path seen ttl stop cyclestop loud]
  (when loud
  ;;   (println "\npath:")
  ;;   (doseq [[article] path] (println " " article))
  ;;   (println "ttl:" ttl))
    (println " " cur "(ttl:" (str ttl ")")))
  (cond
   (= stop cur) [:path path] ;win
   (and (seen cur) cyclestop) [:cyclestop path]
   (seen cur) [:end]
   (zero? ttl) [:ttl]
   :default
   (let [sentences-with-links (parse-links cur)
         results (map (fn [[sentence next]]
                        (wikidfs next (conj path [cur sentence next]) (conj seen cur)
                                 (dec ttl) stop cyclestop loud))
                      sentences-with-links)
         [result] (filter (fn [[type _rest :as all]]
                             (or (= type :path) (= type :cyclestop)))
                          results)]
     (or result [:end]))))

(defn go
  "[start stop cyclestop] Start at <start>, hop til <stop>, print out
steps with sentences. cyclestop controls whether to stop on cycles, or
try the next sentence."
  ([start stop] (go start stop 10 false true))
  ([start stop ttl cyclestop loud]
     (let [path (wikidfs (normalize start) [] #{} ttl (normalize stop) cyclestop loud)
           [tag rest] path]
       (cond
        (some #(= tag %) [:end :ttl]) (println "Fail!" path)
        (some #(= tag %) [:cyclestop :path])
        (do
          (when (= tag :cyclestop) (println "Stopped at cycle!"))
          (doseq [[i [article _sentence next]] (map #(vector %1 %2) (all-ints) rest)]
            (println (str i ".") article "->" next)))
        :default (println "...unexpected")))))

(defn -main [& args]
  (with-command-line args
    "Wikihop from page to page... [-cyclestop] [-stop <page>] <startpage>"
    [anon]
    ;; (when (not= 1 (count anon))
    ;;   (println "Expected exactly 1 arg: start page.")
    ;;   (System/exit 1))
    (let [[start stop ttl] anon]
      (go start stop (Integer. ttl) false true))))
