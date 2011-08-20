#!/bin/sh

if [ "$CLOJURESCRIPT_HOME" = "" ]; then
  CLOJURESCRIPT_HOME="`dirname $0`/.."
fi

CH=$CLOJURESCRIPT_HOME
CLJSC_CP=''
for next in $CH/lib/*: $CH/src/clj: $CH/src/cljs: $CH/test/cljs; do
  CLJSC_CP=$CLJSC_CP$next
done

java -server -cp $CLJSC_CP:$CLASSPATH clojure.main cljs-compile.clj