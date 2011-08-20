(ns cljs-compile
  (:require [cljs.closure]
            [swank.swank]))

(def runtime (java.lang.Runtime/getRuntime))

(defn run-full [cmd]
  (let [p (.exec runtime cmd)
        r (java.io.BufferedReader. (java.io.InputStreamReader. (.getInputStream p)))
        loop (fn []
               (if-let [line (.readLine r)]
                 (do (println line) (recur))))]
    (.waitFor p)
    (loop)))

(def last-copy (atom (slurp "hops.cljs")))

(defn b [] (cljs.closure/build "hops.cljs" {:optimizations :simple :output-to "hops.js"}))

(defn bloop []
  (println "\n\nwaiting for changes...")
  (.waitFor (.exec runtime "inotifywait -e modify hops.cljs"))
  (spit "tmp" @last-copy)
  (run-full "diff -w -d tmp hops.cljs")
  (run-full "rm tmp")
  (swap! last-copy (constantly (slurp "hops.cljs")))
  (println (java.util.Date.) "saw change, recompiling!")
  (b)
  (recur))

(swank.swank/start-server :port 8082)
(bloop)
