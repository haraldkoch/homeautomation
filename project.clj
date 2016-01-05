(defproject homeautomation "0.1.0-SNAPSHOT"

  :description "Harald plays with home automation"
  :url "https://github.com/haraldkoch/homeautomation"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [selmer "0.9.8"]
                 [com.taoensso/timbre "4.2.0"]
                 [com.taoensso/tower "3.0.2"]
                 [markdown-clj "0.9.85"]
                 [environ "1.0.1"]
                 [compojure "1.4.0"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.1.5"]
                 [ring "1.4.0"
                  :exclusions [ring/ring-jetty-adapter]]
                 [metosin/ring-middleware-format "0.6.0"]
                 [metosin/ring-http-response "0.6.5"]
                 [bouncer "1.0.0"]
                 [prone "1.0.0"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.webjars/bootstrap "3.3.5"]
                 [org.webjars/jquery "2.1.4"]
                 [migratus "0.8.8"]
                 [conman "0.2.8"]
                 [mysql/mysql-connector-java "5.1.38"]
                 [org.clojure/clojurescript "1.7.122" :scope "provided"]
                 [org.clojure/tools.reader "0.10.0"]
                 [reagent "0.5.1"]
                 [reagent-forms "0.5.13"]
                 [reagent-utils "0.1.7"]
                 [secretary "1.2.3"]
                 [org.clojure/core.async "0.2.374"]
                 [cljs-ajax "0.5.2"]
                 [org.immutant/web "2.1.1"]

                 ; local additions
                 [com.andrewmcveigh/cljs-time "0.3.14"]
                 [clojurewerkz/machine_head "1.0.0-beta9"]]

  :min-lein-version "2.0.0"
  :uberjar-name "homeautomation.jar"
  :jvm-opts ["-server"]

  :main homeautomation.core
  :migratus {:store :database}

  :plugins [[lein-environ "1.0.1"]
            [migratus-lein "0.1.7"]
            [lein-cljsbuild "1.1.0"]
	    [lein-kibit "0.1.2"]]
  :clean-targets ^{:protect false} [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src-cljs"]
     :compiler
     {:output-to "resources/public/js/app.js"
      :externs ["react/externs/react.js"]
      :pretty-print true}}}}
  
  :profiles
  {:uberjar {:omit-source true
             :env {:production true}
              :hooks [leiningen.cljsbuild]
              :cljsbuild
              {:jar true
               :builds
               {:app
                {:source-paths ["env/prod/cljs"]
                 :compiler {:optimizations :advanced :pretty-print false}}}} 
             
             :aot :all}
   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]
   :project/dev  {:dependencies [[ring/ring-mock "0.3.0"]
                                 [ring/ring-devel "1.4.0"]
                                 [pjstadig/humane-test-output "0.7.1"]
                                 [lein-figwheel "0.5.0-2"]]
                  :plugins [[lein-figwheel "0.4.0"]]
                   :cljsbuild
                   {:builds
                    {:app
                     {:source-paths ["env/dev/cljs"] :compiler {:source-map true}}}}
                  
                  :figwheel
                  {:http-server-root "public"
                   :server-port 3449
                   :nrepl-port 7002
                   :css-dirs ["resources/public/css"]
                   :ring-handler homeautomation.handler/app}
                  
                  :repl-options {:init-ns homeautomation.core}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]
                  ;;when :nrepl-port is set the application starts the nREPL server on load
                  :env {:dev        true
                        :port       3000
                        :nrepl-port 7000}}
   :project/test {:env {:test       true
                        :port       3001
                        :nrepl-port 7001}}
   :profiles/dev {}
   :profiles/test {}})
