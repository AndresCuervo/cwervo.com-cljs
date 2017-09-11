(ns vr-test.core
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [cljsjs.aframe :as aframe]
            [reagent.core :as reagent :refer [atom]]
            [secretary.core :as secretary :include-macros true]
            [clojure.string :as string]
            [hiccups.runtime :as hiccupsrt]
            [accountant.core :as accountant]))

(enable-console-print!)

;; (println "aframe" aframe)
;; (js/console.log"js/AFRAME" (js* "AFRAME.scenes"))

;; -------------------------
;; Views

;; (defn ^:export init-fn [] (println "hello from CLJS"))

;; (def scene-components js/AFRAME.scenes[0].components)


(def registered-components (atom ()))

(defn register-component [component-name component-map]
  (js-delete (aget js/AFRAME "components") component-name)
  (swap! registered-components conj component-name)
  (js/AFRAME.registerComponent
    component-name
    component-map))

(def colors ["pink" "blue" "yellow" "red" "peachpuff" "#2EAFAC" "#BAE"])

;; From: https://stackoverflow.com/questions/28039338/how-to-loop-a-javascript-object-and-push-each-into-an-array-in-clojurescript
(defn key-value [obj]
  (clj->js
   (reduce (fn [acc k]
             (conj acc (clj->js {k (aget obj k)})))
           []
           (.keys js/Object obj))))

(def event-listeners (atom ()))

(defn register-color-on-click []
  (register-component
     "change-color-on-click"
     #js {
          "schema" #js {:axis #js {:default "z"}}
          "init" (fn [] (this-as this
                                 (let [listener-function (clj->js
                                                           (fn [event]
                                                             (println "oof" (.-detail event))
                                                             (.setAttribute (.-el this) "material" "color" (rand-nth colors)) ))]
                                   #_(js/console.log "color on click" (.-el this))
                                   (js/console.log "listenerssss (from color-on-click component): " (clj->js @event-listeners))
                                   (cljs->js (js/document.addEventListener
                                               "click"
                                               (-> listener-function
                                                   (.bind this))))
                                 ;; TODO hmmmmmmmmm, figure out how to remove
                                 ;; event listeners dynamically --- store
                                 ;; lists of name/function pairs and remove on
                                 ;; reload or mount-root maybe???
                                 #_(swap! event-listeners conj  x))))
          }))

(defn register-inc-on-click []
  (let [component-name "increase-position-on-click"]
    (js-delete (aget js/AFRAME "components") component-name)
    (swap! registered-components conj component-name)
    component-name
    (js/AFRAME.registerComponent
      component-name
      #js {
         "schema" #js {:axis #js {:default "z"}}
         "init" (fn [] (this-as this
                                (js/document.addEventListener
                                  "click"
                                  (-> (fn [event]
                                        (js/console.log "bound \"this\"from CLJS! " (.-el this) event)
                                        ;; (js/console.log (aget (.-el this) "object3D"))
                                        ;; (js/console.log (.getAttribute (.-el this) "position"))
                                        (let [position (.getAttribute (.-el this) "position")
                                              x (js/parseInt (.-x position))
                                              y (js/parseInt (.-y position))
                                              z (js/parseInt (.-z position))
                                              axis (-> this
                                                       .-data
                                                       .-axis)]
                                          (.setAttribute (.-el this) "position"
                                                         (string/join " "
                                                                      (vec (for [loop-axis (js/Object.keys position)
                                                                                 :let [this-axis (aget position loop-axis )] ]
                                                                             (+ this-axis
                                                                                (if (= loop-axis axis)
                                                                                  1
                                                                                  0))))))))
                                      (.bind this)))))})))

(defn register-thing []
  (register-component
    "my-component"
    #js {
     "init" (fn [] (println "-- hello from CLJS --"))
     ;; "tick" (fn [] (println (mod (js* "parseInt(performance.now())") 60)))
     }))

(def responsive-header
    [:div.responsive-header
     [:img.logo {:src "images/big-logo.png"
            :alt "LOGO"}]
     [:div.nameContainer
      [:span.nameLine "Andres"]
      [:span.nameLine "Cuervo"]]
     ])

(def c [:0 :1 :2 :3 :4 :5 :6 :7 :8 :9 :A :B :C :D :E :F])
(defn random-hex [] (str "#" (string/join (map name (repeatedly 6 #(rand-nth c))))))

(defn home-page []
   ;; TODO maybe attach libraries like this? Also gotta remove them on mount-root tho!
   ;;
   ;; Hmmmmm, this doesn't seem to be the answer since it's registering everyhing again,
   ;; AND doesn't seem to load the script into head on first load????
   ;;
   ;; (def script (js/document.createElement "script"))
   ;; (aset script "type" "text/javascript")
   ;; (aset script "src" "//cdn.rawgit.com/donmccurdy/aframe-physics-system/v2.0.0/dist/aframe-physics-system.min.js")
   ;; (js/console.log "---WHAT---")
   ;; (js/console.log
   ;;   (js/document.head.appendChild script))

   [:div
    #_[:span#topnote
       "Go to " [:a {:href "about"} "the about page!"]]
    [:div.floating-page
     responsive-header
     [:div
      "Some contentttttt"
      [:div [:a {:href "about"} "go to the about page"]]
      "Hello, my name is Andres Cuervo!"]
     [:ul.card-list
       (for [card [{:title "VR"
                    :url "/vr/"
                    :description "A collection of links a few of my VR projects/demos"}
                   {:title "AR"
                    :url "/ar/"
                    :description "A collection of links a few of my AR projects/demos"
                    :image {:href "images/sun-detail.png" :alt "A screenshot from my AR Medusa refraction experiment"}}]
             :let [title (:title card)
                   image (:image card)]]
           [:li {:key (str "thing-" title)}
            [:a.card-link {:href (:url card)}
             [:div.card {:style
                        (if image
                            #js {"backgroundImage" (str "url(" (:href image) ")")
                                 "backgroundColor" "black"}
                            ;; #js {"backgroundColor" "#2EAFAC"}
                            #js {"backgroundColor" (str "hsl(" (rand-int 255) ", 70%, 80%)")})
                        }
             [:h2.title title]
             [:div.card-description (:description card)]
             ]]])]
     ]
    ;; [:script {:src "/js/my-component.js"}]
    ;; [:script {:src "//cdn.rawgit.com/donmccurdy/aframe-physics-system/v2.0.0/dist/aframe-physics-system.min.js"}]
    ;; TODO figure out how to load extenal libraries, also 😬
    ;; (.appendChild
    ;; ;; (js* "document.addEventListener('click', function () {console.log('clicked!')})")

    ;; TODO : --- Bring back the a-scene below, make it a box, so that you can
    ;; use either your mouse to rotate or on a phone you always see some interesting part of the shader
    ;; but can tell movement is happening :) ---

    ;; (register-inc-on-click)
    (register-thing)
    (register-color-on-click)

    [:a-scene {:dangerouslySetInnerHTML {:__html (html
                                                       [:a-box#lilBox {:dynamic-body ""
                                                                       :change-color-on-click ""
                                                                       :increase-position-on-click ""
                                                                       :my-component ""
                                                                       :position "-2 0 -4"} ""]
                                                       [:a-box {:color "black"
                                                                :my-component ""
                                                                :position "1 0 -4"
                                                                :scale "0.5 2 0"
                                                                :change-color-on-click ""
                                                                :increase-position-on-click "axis: y;"
                                                                } ""]
                                                       [:a-camera
                                                        [:a-cursor]]
                                                       [:a-sky {:color "blue"} ""])}}]])

;; (defn home-page []
;;   [:div [:h2 "Home page ???"]
;;    [:div [:a {:href "/about"} "go to the about page"]]])

(defn about-page []
  [:div
   responsive-header
   [:div [:h2 "About vr_test"]
   [:div [:a {:href "/"} "go to the home page"]]]])

(defn hsl-test-page []
    [:div
      [:ul
       (for [n (range 0 255 2)]
           [:li.test-hsl-box {:key n
                 :style #js {"backgroundColor" (str "hsl(" n ", 80%, 80%)")}}])]])

;; -------------------------
;; Routes

(def page (atom #'home-page))

(defn current-page []
  [:div [@page]])

(defn print-components []
  (js/console.log
    "\n---- registered component atom: \n"
    (clj->js @registered-components)
    "\n---- AFRAME.components:\n"
    (aget js/AFRAME "components")
    "\n----\n"
    ))

(defn delete-components []
  (doseq [c @registered-components]
      (js-delete (aget js/AFRAME "components") c)
      (js/console.log "➖ deleted component : " c))
  (reset! registered-components ()))

(secretary/defroute "/" []
  (do
    (reset! page #'home-page)))

(secretary/defroute "/about" []
  (do
    ;; (delete-components)
    ;; (println "wah" (vec @registered-components))
  (reset! page #'about-page)))

(secretary/defroute "/hsl-test-page" []
  (do
    (reset! page #'hsl-test-page)))

;; -------------------------
;; Initialize app

(defn mount-root []
  (print-components)
  (println "😲 ABOUT TO DELETE COMPONENTSSSS 😲")
  (delete-components)
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
