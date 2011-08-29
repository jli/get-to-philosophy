(ns hops.main
  (:require [goog.dom :as dom]
            [goog.net.XhrIo :as Xhr]
            [goog.Timer :as Timer] ;; ??? why do I still have to say goog.Timer?
            [goog.events :as events]
            [cljs.reader :as reader]))

(defn capitalize [s]
 (let [first (. s (substring 0 1))
             rest (. s (substring 1))]
         (str (. first (toUpperCase)) rest)))

;; needs work
(defn normalize [article]
  (-> article
      (.replace " " "_")
      capitalize))

(defn out-prim [s]
  (dom/append (dom/getElement "out") s))

(defn out-line [] (out-prim (dom/htmlToDocumentFragment "<br>")))

(defn out [& args]
  (out-prim (apply str args))
  (out-prim (dom/htmlToDocumentFragment " ")))

(def wiki-prefix "http://en.wikipedia.org/wiki/")

(defn wiki-url [article] (str wiki-prefix (normalize article)))

(defn out-link [article]
  (let [link (wiki-url article)]
    (out-prim (dom/htmlToDocumentFragment (str "<a href=\"" link "\">" article "</a>")))
    (out-prim (dom/htmlToDocumentFragment " "))))

(defn parse-links [article k]
  (let [k2 (fn [e]
             (-> e (. target) (. (getResponseText)) reader/read-string k))]
    (Xhr/send (str "/links?" article) k2)))

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
  (out-link cur)
  (when (not= stop cur) (out "→"))
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
                               (do (out "cycle! →")
                                   (rec rest))))]
                    (if (empty? sentences-with-links)
                      (k [:end])
                      (wikipath next
                                (conj path [cur sentence next])
                                (conj seen cur)
                                (dec ttl) stop cyclestop
                                k2)))))))

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
(def spinner-transition (transition-map [:bltr :hor :tlbr :vert]))
(defn spinner-set-dir [dir]
  (let [txt ({:vert "|", :tlbr "\\", :hor "-", :bltr "/", :done ""} dir)]
    (dom/setTextContent spinner txt)))
(defn start-spinner []
  (let [timer (goog.Timer. 250)
        anim (fn []
               (let [s @spinner-state]
                 (spinner-set-dir s)
                 (if (not= s :done)
                   (swap! spinner-state spinner-transition)
                   (. timer (stop)))))]
    (reset! spinner-state :hor)
    (. timer (start))
    (events/listen timer goog.Timer/TICK anim)))

(defn stop-spinner [] (reset! spinner-state :done))

(defn ^:export go
  "[start stop cyclestop] Start at <start>, hop til <stop>, print out
steps with sentences. cyclestop controls whether to stop on cycles, or
try the next sentence."
  ([start stop] (go start stop 100 false))
  ([start stop ttl cyclestop]
     (start-spinner)
     (wikipath (normalize start) [] #{} ttl (normalize stop) cyclestop
               (fn [[tag rest]]
                 (stop-spinner)
                 (cond
                  (#{:end :ttl} tag) (out "failed! dead end or ttl expired" rest)
                  (= tag :cyclestop) (out "stopped at cycle!")
                  (= tag :path)      (do (out) (out "win! " (count rest)))
                  :default           (out "jli sux. unexpected:" tag rest))))))
