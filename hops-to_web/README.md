# hops-to

A small Clojure page (Clojure backend, Clojurescript front) for
playing the [Get To Philosophy game]
(http://en.wikipedia.org/wiki/Wikipedia:Get_to_Philosophy).

## Usage

FIXME: details.

The backend uses Ring and runs a Jetty server. Loading the site brings
you to the only page. The page loads a single javascript file, which
is compiled from hops.cljs.

## Todo

* Clean up output. Replace trace and final list with constantly
  updating list (when avoiding cycles and backtracking, should delete
  link).
* _A_jax
* Make article names links.
* Load article in frame.
* Expose options (stop, cycle behavior).
* Enter.

## License

Copyright (C) 2011 WTFPL
