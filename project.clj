(defproject mailcheap-operation "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.csv "0.1.4"]
                 [clj-http "3.10.0"]
                 [cheshire "5.8.1"]
                 [cljs-ajax "0.8.0"]
                 [org.jsoup/jsoup "1.9.2"]]
  :main ^:skip-aot mailcheap-operation.core
  :target-path "target/%s"
  :repl-options {:init-ns mailcheap-operation.core}
  :profiles {:uberjar {:aot :all}})
