(ns small)

(defn all-ints
  "[start] Infinite seq of ints, starting at <start> if given."
  ([] (all-ints 1))
  ([start] (iterate inc start)))

;; (defn capitalize [s]
;;   (try (let [first (.substring s 0 1)
;;              rest (.substring s 1)]
;;          (str (.toUpperCase first) rest))
;;        (catch Exception _ s)))


(defn ^:export main [n]
  (let [v [:yes :it :is]
        vs (map #(vector %1 %2) (all-ints) v)]
    (str "yoyo:" n vs)))
