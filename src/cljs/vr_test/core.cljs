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
         "init" (js* "function () {
                     var clickFn = function (evt) {
                            console.log('oof', evt.detail)
                            this['el'].setAttribute('material', 'color', '#'+(Math.random()*0xFFFFFF<<0).toString(16))
                        }.bind(this)
                        document.addEventListener('click', clickFn)
                        document.addEventListener('touchend', clickFn)
                     }")
         ;; Commenting out the function, rewriting it above as js might work??
         #_(fn [] (this-as this
                                (let []
                                  #_(js/console.log "color on click" (.-el this))
                                  (js/console.log "listenerssss (from color-on-click component): " (clj->js @event-listeners))
                                  (js/document.addEventListener
                                    "click"
                                    (-> #_listener-function
                                        (fn [event]
                                          (println "oof" (.-detail event))
                                          ;; TODO figure out how to get this to execute when compiled to CLJSJS :/
                                          ;; Get compilation minification errors rn

                                          ;; For now, generate a random color via the JS string :/
                                          (js* (str "this.el.setAttribute('material', 'color', '#'+(Math.random()*0xFFFFFF<<0).toString(16))"))
                                          #_(.setAttribute (.-el this) "material" "color" (cljs->js (rand-nth colors))) )
                                        (.bind this)))
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
         "schema" #js {:axis #js {:default "z"}
                       :increment #js {:default "1"
                                       :type "number"}}
         "init" (js* "function () {
                     var clickFn = function (evt) {
                     var x = this.el.getAttribute('position', 'x')
                     var y = this.el.getAttribute('position', 'y')
                     var z = this.el.getAttribute('position', 'z')

                     var newPos = {x: x, y: y,z: z}
                     for (var i in newPos) {
                     if (i === this.data.axis) {
                     newPos[i] += this.data.increment
                     }

                     this.el.setAttribute('position', newPos)
                     }
                        }.bind(this)

                        document.addEventListener('click', clickFn)
                        document.addEventListener('touchend', clickFn)
                     }")
         #_(fn []
                  (this-as this
                                (js/document.addEventListener
                                  "click"
                                  (-> (fn [event]
                                        (js/console.log "bound \"this\"from CLJS! " (.-el this) event)

                                        ;; TODO ;; TODO ;; TODO ;; TODO ;; TODO
                                        ;; TODO ;; TODO ;; TODO ;; TODO ;; TODO

                                        ;; This is a niche issue, you should just rewrite this method in JS* and move
                                        ;; on to porting over the cube and mouse follow and the other pages :)
                                        ;;
                                        ;; TODO ;; TODO ;; TODO ;; TODO ;; TODO
                                        ;; TODO ;; TODO ;; TODO ;; TODO ;; TODO

                                        ;; (js/console.log (aget (.-el this) "object3D"))
                                        (let [position (js* "this['el'].getAttribute('position')")
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
    (js* "{
         init : function () {
         console.log('-- hello from CLJS ! -- ')
         }
         }")
    ;; #js {
    ;;  "init" (fn [] (println "-- hello from CLJS --"))
    ;;  ;; "tick" (fn [] (println (mod (js* "parseInt(performance.now())") 60)))
    ;;  }
    ))

(def responsive-header
    [:div.responsive-header
     [:a {:href "/"}
      [:img.logo {:src "images/big-logo.png"
                  :alt "LOGO"}]
      [:div.nameContainer
       [:span.nameLine "Andres"]
       [:span.nameLine "Cuervo"]]]
     ])

(def c [:0 :1 :2 :3 :4 :5 :6 :7 :8 :9 :A :B :C :D :E :F])
(defn random-hex [] (str "#" (string/join (map name (repeatedly 6 #(rand-nth c))))))

(defn load-script []
  ;; JS that checks if the script's been attached to the head already
  ;;
  ;; Checking was too cumbersone in CLJS
  (js* "function loadScript(url, callback){

       if (document.querySelectorAll('[src=\"' + url + '\"]').length > 0) {
       return
       }
           var script = document.createElement('script')
           script.type = 'text/javascript';

           if (script.readyState){  //IE
           script.onreadystatechange = function(){
           if (script.readyState == 'loaded' ||
           script.readyState == 'complete'){
           script.onreadystatechange = null;
           callback();
           }
           };
           } else {  //Others
           script.onload = function(){
           callback();
           };
           }

           script.src = url;
           console.log('loaded ' + url )
           document.getElementsByTagName('head')[0].appendChild(script);
           }"))

(defn make-cards [info]
  [:ul.card-list (for [card info
                       :let [title (:title card)
                             image (:image card)
                             cardType (:type card)]]
                   [:li {:key (str cardType title)}
                    [:a.card-link {:href (:url card)}
                     [:div.card {:style
                                 (if image
                                   #js {"backgroundImage" (str "url(" (:href image) ")")
                                        "backgroundColor" "black"}
                                   ;; #js {"backgroundColor" "#2EAFAC"}
                                   #js {"backgroundColor" (str "hsl(" (rand-int 255) ", 70%, 40%)")})
                                 }
                      [:h5.title (when cardType [:code.title-type "["  cardType "]"]) title]
                      [:div.card-description (:description card)]
                      ]]])]
  #_(map info (fn [card]
                (let [title (:title card)
                      image (:image card)
                      cardType (:type card)]
                  [:li {:key (str cardType title)}
                   [:a.card-link {:href (:url card)}
                    [:div.card {:style
                                (if image
                                  #js {"backgroundImage" (str "url(" (:href image) ")")
                                       "backgroundColor" "black"}
                                  ;; TODO make this a nice gradient was you go through the map, need an index number though :/

                                  ;;  ------------ TODO --------------
                                  ;; This is almost identical to the otehr card
                                  ;; for loop - just make this a method.  Better yet, you can pass in a
                                  ;; base CLJS value and then each card in the list can make a CSS linear
                                  ;; gradient (with backgroundColor first, as a fall back) from the initial
                                  ;; HSL to a slightly slightly higher hue + darker saturation (+ 10, 15?
                                  ;; play around with this number)
                                  ;;  ------------ TODO --------------

                                  #js {"backgroundColor" (str "hsl(" (rand-int 255) ", 70%, 80%)")})
                                }
                     ;; TODO -- maybe make this header symbol configurable?
                     ;; Can put title in {:class "title"} and then just pass in the symbol to use, or default it to h5 or h2 or whatever
                     [:h5.title (when type [:code.title-type "["  type "]"]) title]
                     [:div.card-description (:description card)]
                     ]]]))))

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
       "Go to " [:a {:href "/about"} "the about page!"]]
    [:div.floating-page.home-page
     responsive-header
     [:div
      "Some contentttttt"
      [:div [:a {:href "/about"} "go to the about page"]]
      "Hello, my name is Andres Cuervo!"]
     (make-cards [{:title "Virtual Reality"
                   :url "/vr"
                   :description "A collection of links a few of my VR projects/demos"}
                  {:title "Augmented Reality"
                   :url "/ar"
                   :description "A collection of links a few of my AR projects/demos"
                   :image {:href "images/sun-detail.png" :alt "A screenshot from my AR Medusa refraction experiment"}}])]

    ;; TODO : --- Bring back the a-scene below, make it a box, so that you can
    ;; use either your mouse to rotate or on a phone you always see some interesting part of the shader
    ;; but can tell movement is happening :) ---


    ((load-script) "//cdn.rawgit.com/donmccurdy/aframe-physics-system/v2.0.0/dist/aframe-physics-system.min.js" #())
    ;; ((load-script) "https://unpkg.com/aframe-particle-system-component@1.0.x/dist/aframe-particle-system-component.min.js" #())
    (register-inc-on-click)
    (register-thing)
    (register-color-on-click)

    ;; Write a CLJS macro to do the inserting of the empty strings at the end of vectors, since it isn't ISeqable so it can't
    ;; pass through the CLJS parser
    [:a-scene {:dangerouslySetInnerHTML {:__html (html
                                                       [:a-box#lilBox {:dynamic-body ""
                                                                       :change-color-on-click ""
                                                                       :increase-position-on-click "increment: 2; axis: x"
                                                                       :my-component ""
                                                                       :position "-2 0 -4"} ""]
                                                       ;; [:a-entity {:position "0 2.25 -15" :particle-system "color: #EF0000,#44CC00"} ""]
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
    [:span#topnote
       "Go to " [:a {:href "/"} "the home page!"]]

    ((load-script) "https://unpkg.com/aframe-particle-system-component@1.0.x/dist/aframe-particle-system-component.min.js" #())
    (register-inc-on-click)
    (register-thing)
    (register-color-on-click)

    [:a-scene {:dangerouslySetInnerHTML {:__html (html
                                                       [:a-entity {:position "0 2.25 -15"
                                                                   :particle-system "preset: snow; color: #EF0000,#44CC00"} ""]
                                                       [:a-box {:color "black"
                                                                :my-component ""
                                                                :position "1 0 -4"
                                                                :scale "0.5 2 0"
                                                                :change-color-on-click ""
                                                                :increase-position-on-click "axis: y;"
                                                                } ""]
                                                       [:a-camera
                                                        [:a-cursor]]
                                                       [:a-sky {:color "#2EAFAC"} ""])}}]])

(defn hsl-test-page []
    [:div
      [:ul
       (for [n (range 0 255 2)]
           [:li.test-hsl-box {:key n
                 :style #js {"backgroundColor" (str "hsl(" n ", 80%, 80%)")}}])]])

(defn thoughts-notice [thoughts-url]
    [:div
     thoughts-url])

(defn vr-page []
   [:div
    [:div.floating-page
     responsive-header
     (make-cards [{:title "Imagine Trees Like These"
                   :type "Project"
                   :url "https://vr.cwervo.com/scenes/itlt/"
                   :description "This was my creative writing capstone project at Oberlin College. I
                                wanted to explore an abstract immersive narrative about nature using VR."
                   :image {:href "images/sun-detail.png" :alt "A screenshot from my AR Medusa refraction experiment"}}
                  {:title "Imagine Trees Like These"
                   :type "Presentation"
                   :url "https://www.youtube.com/watch?v=Ca6quGC_hUk"
                   :description "I gave an in-depth, 16 minute talk about my capstone project."
                   :image {:href "images/sun-detail.png" :alt "Sun detail"}}
                  {:title "A-Frame Workshop"
                   :type "Presentation"
                   :url "stefie.github.io/aframe-workshop-berlin/"
                   :description "Stefanie Doll & I organized the first Web XR Meetup in Berlin, at Mozilla's offices.
                                During this meetup we taught an 2-hour introductory workshop on A-Frame."
                   }
                  ])]])

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


(secretary/defroute "/vr" []
  (do
    (reset! page #'vr-page)))

(secretary/defroute "/about" []
  (do
    ;; (delete-components)
    ;; (println "wah" (vec @registered-components))
  (reset! page #'about-page)))


(secretary/defroute "/thoughts/*thoughts-url" [thoughts-url]
  (reset! page #(fn [] (thoughts-notice thoughts-url))))

(secretary/defroute "*" []
  (do
    (reset! page (fn [] [:div " womp womp : 404"]))))

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
