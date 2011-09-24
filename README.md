# hops-to

A small Clojure page ([Clojure] (https://github.com/clojure/clojure)
backend, [ClojureScript] (https://github.com/clojure/clojurescript)
front) for playing the [Get To Philosophy game]
(http://en.wikipedia.org/wiki/Wikipedia:Get_to_Philosophy).

A demo: http://circularly.org/hops/

## Usage

FIXME: details.

The backend uses Ring and runs a Jetty server. Loading the site brings
you to the only page. The page loads a single javascript file, which
is compiled from hops.cljs.

## Todo

* Multi-level page cache - LRU memo parse-links, disk cache for html
* Escape page names.(?)
* Show sentences for context.
* Correct parens parsing.(!)

## License

Copyright (C) 2011 WTFPL
