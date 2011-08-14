(ns hops.main
  (:require [goog.dom :as dom]
            [cljs.reader :as reader]))

;;; doesn't work well - too jumpy. unless I can load in the text...
;; (defn set-iframe [a]
;;   (let [link (str "/wiki?" a )]
;;     (js* "document.getElementById('iframe').src=~{link};")))

;;; substring doesn't throw exceptions on oob indexes, hurray. but
;;; just for reference if it did...
;; (defn capitalize [s]
;;   (try (let [first (. s (substring 0 1))
;;              rest (. s (substring 1))]
;;          (str (. first (toUpperCase)) rest))
;;        (catch (js* "Object") e s)))
(defn capitalize [s]
 (let [first (. s (substring 0 1))
             rest (. s (substring 1))]
         (str (. first (toUpperCase)) rest)))

;; needs work
(defn normalize [article]
  (-> article
      (.replace " " "_")
      capitalize))

(defn out [& args]
  (let [add #(dom/append (dom/getElement "out") %)]
    (add (apply str args))
    (add (dom/htmlToDocumentFragment "<br>"))))

(def wiki-prefix "http://en.wikipedia.org/wiki/")

(defn wiki-url [article] (str wiki-prefix (normalize article)))

(defn out-link [article]
  (let [add #(dom/append (dom/getElement "out") %)
        link (wiki-url article)
        anchor (dom/createDom "a" (js* "{'href': ~{link}}") article)]
    (add anchor)
    (add (dom/htmlToDocumentFragment "<br>"))))

(defn all-ints
  "[start] Infinite seq of ints, starting at <start> if given."
  ([] (all-ints 1))
  ([start] (iterate inc start)))

(defn parse-links [article]
  ;; (set-iframe article)
  (let [x (js* "new XMLHttpRequest()")]
    (. x (open "GET" (str "/links?" article) false))
    (. x (send))
    (reader/read-string (. x responseText))))

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
  ;;   (println "ttl:" ttl)
    ;;(out " " cur "(ttl:" (str ttl ")"))
    (out-link cur))
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

(defn ^:export go
  "[start stop cyclestop] Start at <start>, hop til <stop>, print out
steps with sentences. cyclestop controls whether to stop on cycles, or
try the next sentence."
  ([start stop] (go start stop 100 true true))
  ([start stop ttl cyclestop loud]
     (let [path (wikidfs (normalize start) [] #{} ttl (normalize stop) cyclestop loud)
           [tag rest] path]
       (cond
        (some #(= tag %) [:end :ttl]) (out "Fail!" rest)
        (some #(= tag %) [:cyclestop :path])
        (do
          (when (= tag :cyclestop) (out "Stopped at cycle!"))
          (doseq [[i [article _sentence next]] (map #(vector %1 %2) (all-ints) rest)]
            (out (str i ". ") article "→" next)))
        :default (out "...unexpected")))))

