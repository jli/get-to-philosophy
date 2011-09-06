(defproject hops-to "1.0.0-SNAPSHOT"
  :description "Get to philosophy!"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojars.mikejs/ring-gzip-middleware "0.1.0-SNAPSHOT"]
                 [ring/ring-core "0.3.11"]
                 [ring/ring-devel "0.3.11"]
                 [ring/ring-jetty-adapter "0.3.11"]
                 [enlive "1.0.0-SNAPSHOT"]
                 [swank-clojure/swank-clojure "1.3.2"]]
  :main hops-to.core)
