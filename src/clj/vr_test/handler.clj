(ns vr-test.handler
  (:refer-clojure :exclude [slurp])
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [vr_test.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/aframe/0.6.0/aframe-master.min.js"}]
   ;; TODO figure out how to dynamically change this??? injection, I suppose???
   ;; [:script {:src "//cdn.rawgit.com/donmccurdy/aframe-physics-system/v2.0.0/dist/aframe-physics-system.min.js"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))


(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/about" [] (loading-page))
  (GET "/hsl-test-page" [] (loading-page))

  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
