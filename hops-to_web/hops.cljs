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

(defn parse-links [article k]
  ;; (set-iframe article)
  ;; (out "parse-links" article)
  (let [x (js* "new XMLHttpRequest()")
        k2 (fn []
             (when (= 4 (. x readyState))
               ;; (out "parse-links on " article " ready")
               (k (reader/read-string (. x responseText)))))]
    (. x (open "GET" (str "/links?" article) true))
    (js* "~{x}.onreadystatechange=~{k2}")
    (. x (send))))



;; returns:
;; path-type: [[<article> <sentence> <next>] ...]
;; [:path <path-type>]
;; [:cyclestop <path-type>]
;; [:ttl]
;; [:end]
(defn wikipath
  "[current path seen ttl stop cyclestop]
   Pre: ttl non-negative.
   Post: seen set and path vector increased by next link (next
  current). current is included in seen."
  [cur path seen ttl stop cyclestop k]
  ;;(out " " cur "(ttl:" (str ttl ")"))
  (out-link cur)
  (cond
   (= stop cur) (k [:path path]) ;;win
   (seen cur)   (k (if cyclestop [:cyclestop path] [:cyclecontinue]))
   (zero? ttl)  (k [:ttl])
   :default
   ;; this. shit. is. insane.
   (parse-links cur
                (fn rec [sentences-with-links]
                  ;; (out "running. next: " (second (first sentences-with-links)))
                  ;; (out "next next: " (second (second sentences-with-links)))
                  (let [[[sentence next] & rest] sentences-with-links
                        k2 (fn [[type _ :as all]]
                             (if (not= type :cyclecontinue)
                               (k all)
                               (do (out "cycled back to " next "! skipping")
                                   (rec rest))))]
                    (if (empty? sentences-with-links)
                      (k [:end])
                      (wikipath next
                                (conj path [cur sentence next])
                                (conj seen cur)
                                (dec ttl) stop cyclestop
                                k2)))))))

;; easy no cycles, no cps version
(defn wikipath-nocycles
  "[current path seen ttl stop]
   Pre: ttl non-negative.
   Post: seen set and path vector increased by next link (next
  current). current is included in seen."
  [cur path seen ttl stop]
  ;;(out " " cur "(ttl:" (str ttl ")"))
  (out-link cur)
  (cond
   (= stop cur) [:path path] ;win
   (seen cur) [:cyclestop path]
   (zero? ttl) [:ttl]
   :default
   (let [sentences-with-links (parse-links cur)
         [[sentence next]] sentences-with-links]
     (recur next (conj path [cur sentence next]) (conj seen cur)
            (dec ttl) stop))))

(defn transition-map
  "Takes a sequence of states and return a map of state transitions,
where transitions are a cycle.
 [] -> {}, [1] -> {1 1}, [1 2 3] -> {1 2, 2 3, 3 1}"
  ([xs] (if-let [f (first xs)] (transition-map f {} xs) {}))
  ([head acc xs] ;; head is for the last node
     (cond (empty? xs) acc
           (empty? (rest xs)) (assoc acc (first xs) head)
           :default
           (recur head
                  (assoc acc (first xs) (second xs))
                  (rest xs)))))

(def spinner (dom/getElement "spinner"))
(def spinner-state (atom nil)) ;; "direction" keyword, or nil at start
(def spinner-transition (transition-map [:vert :tlbr :hor :bltr]))
(defn spinner-set-dir [dir]
  (let [txt ({:vert "|", :tlbr "\\", :hor "-", :bltr "/", :done "done!"} dir)]
    (js* "~{spinner}.innerHTML=~{txt}")))
(defn start-spinner []
  (let [anim (fn anim []
               (let [s @spinner-state]
                 (spinner-set-dir s)
                 (when (not= s :done)
                   (swap! spinner-state spinner-transition)
                   (js* "setTimeout(~{anim}, 150)"))))]
    (swap! spinner-state (constantly :hor))
    (js* "setTimeout(~{anim}, 150)")))

(defn stop-spinner [] (swap! spinner-state (constantly :done)))

(defn ^:export go
  "[start stop cyclestop] Start at <start>, hop til <stop>, print out
steps with sentences. cyclestop controls whether to stop on cycles, or
try the next sentence."
  ([start stop] (go start stop 100 true))
  ([start stop ttl cyclestop]
     (start-spinner)
     (let [k (fn [[tag rest]]
               (stop-spinner)
               (cond
                (#{:end :ttl} tag) (out "Failed! Dead end or ttl expired." rest)
                (#{:cyclestop :path} tag)
                (do
                  (when (= tag :cyclestop) (out "Stopped at cycle!"))
                  (doseq [[i [article _sentence next]] (map #(vector %1 %2) (all-ints) rest)]
                    (out (str i ". ")
                         (when (= i 1) article)
                         "→" next)))
                :default (out "...unexpected:" tag rest)))]
       (wikipath (normalize start) [] #{} ttl (normalize stop) cyclestop k))))

