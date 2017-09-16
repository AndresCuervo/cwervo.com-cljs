(ns vr-test.core
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [cljsjs.aframe :as aframe]
            [reagent.core :as reagent :refer [atom]]
            [secretary.core :as secretary :include-macros true]
            [clojure.string :as string]
            [hiccups.runtime :as hiccupsrt]
            [accountant.core :as accountant]))

(enable-console-print!)

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
                     }")}))

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
                                       :types "number"}}
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
                     }")})))

(defn register-glitch-shader []
  (js* "function () {var vertexShader = `varying vec2 vUv;

       void main() {
       vUv = uv;
       gl_Position = projectionMatrix * modelViewMatrix * vec4( position, 1.0 );
       }`;
       var fragmentShader = `varying vec2 vUv;
       uniform vec3 color;
       uniform float time;

       void main() {
       // Use sin(time), which curves between 0 and 1 over time,
       // to determine the mix of two colors:
       //    (a) Dynamic color where 'R' and 'B' channels come
       //        from a modulus of the UV coordinates.
       //    (b) Base color.
       //
       // The color itself is a vec4 containing RGBA values 0-1.
       gl_FragColor = mix(
       //vec4(mod(vUv , 0.05) * 20.0, 1.0, 1.0),
       vec4(mod(vUv , 0.05) * 21.0, 0.25, 0.25),
       //vec4(mod(vUv , 0.45) * 30.0, 1.0, 1.0),
       vec4(color, 1.0),
       cos(time)
       );
       }`;

      AFRAME.registerComponent('material-grid-glitch', {
        schema: {color: {type: 'color'}},

        /**
        * Creates a new THREE.ShaderMaterial using the two shaders defined
        * in vertex.glsl and fragment.glsl.
        */
        init: function () {
          const data = this.data;

          this.material  = new THREE.ShaderMaterial({
            uniforms: {
              time: { value: 0.0 },
              color: { value: new THREE.Color(data.color) }
            },
            vertexShader,
            fragmentShader
          });

          this.applyToMesh();
          this.el.addEventListener('model-loaded', () => this.applyToMesh());
        },


        /**
        * Update the ShaderMaterial when component data changes.
        */
        update: function () {
          this.material.uniforms.color.value.set(this.data.color);
        },

        /**
        * Apply the material to the current entity.
        */
        applyToMesh: function() {
          const mesh = this.el.getObject3D('mesh');
          if (mesh) {
            mesh.material = this.material;
          }
        },

        /**
        * On each frame, update the 'time' uniform in the shaders.
        */
        tick: function (t) {
          this.material.uniforms.time.value = t / 1000;
        }

      })}()"))

(defn mouse-camera-rotation []
  (register-component "mouse-camera-rotation"
                      #js {
                           "schema" #js {:color #js {:type "color"}
                                         :speed #js {:default "0.2"}
                                         :mouse #js {:type "vec2" :default "0 0"}}
                           "getSpeed" (js* "function (current, past, speed) {
                                           return Math.max(Math.min((current - past) * 0.001, speed), - speed)
                                           }")
                           "init" (js* "function () {
                                       document.addEventListener('mousemove', function (e) {
                                       //this.el.sceneEl.camera.position.x += this.getSpeed(e.clientX, this.data.mouse.x / 100, this.data.speed / 100)
                                       //this.el.sceneEl.camera.rotation.x += this.getSpeed(e.clientY, this.data.mouse.x / 100, this.data.speed / 100)

                                       //this.el.sceneEl.camera.rotation.y += this.getSpeed(e.clientX, this.data.mouse.x, this.data.speed);
                                       //this.el.sceneEl.camera.rotation.x += this.getSpeed(e.clientY, this.data.mouse.x, this.data.speed / 1000);

                                       this.el.sceneEl.camera.rotation.y += this.getSpeed(e['clientX'], this['data']['mouse']['x'], this['data']['speed']);
                                       this.el.sceneEl.camera.rotation.x += this.getSpeed(e['clientY'], this['data']['mouse']['x'], this['data']['speed'] / 1000);
                                       //this.data.mouse.x = e.clientX;
                                       this['data']['mouse']['x'] = e['clientX']
                                       //this.data.mouse.y = e.clientY;
                                       this['data']['mouse']['y'] = e['clientY']
                                       }.bind(this))
                                       }")
                           })
  #_(js* "AFRAME.registerComponent('mouse-camera-rotation', {
  schema: {
    color: {type: 'color'},
    speed : { default : 0.2 },
    mouse : {
      type : 'vec2',
      'default' : "0 0"
    }
  },

  getSpeed : function (current, past, speed) {
    return Math.max(Math.min((current - past) * 0.001, speed), - speed)
  },
    /**
     * Creates a new THREE.ShaderMaterial using the two shaders defined
     * in vertex.glsl and fragment.glsl.
     */
    init: function () {

        const data = this.data;

        //this.material  = new THREE.ShaderMaterial({
            //uniforms: {
                //time: { value: 0.0 },
                //color: { value: new THREE.Color(0.8,0,0.7)}
            //},
            //vertexShader,
            //fragmentShader
        //});
        this.applyToMesh();
      this.material.visible = true
      this.el.addEventListener('model-loaded', () => this.applyToMesh());

      document.addEventListener('mousemove', function (e) {
        //this.el.sceneEl.camera.position.x += this.getSpeed(e.clientX, this.data.mouse.x, this.data.speed)
        this.el.sceneEl.camera.rotation.y += this.getSpeed(e.clientX, this.data.mouse.x, this.data.speed)
        this.data.mouse.x = e.clientX
        this.data.mouse.y = e.clientY
      }.bind(this))
    },
    /**
     * Update the ShaderMaterial when component data changes.
     */
    update: function () {
        this.material.uniforms.color.value.set(this.data.color);
    },

    /**
     * Apply the material to the current entity.
     */
    applyToMesh: function() {
        const mesh = this.el.getObject3D('mesh')
        if (mesh) {
          var mat = this.material
          mesh.traverse(function (node) {
            if (node.isMesh) {
              node.material = mat
            }
          })
        }
    },
    /**
     * On each frame, update the 'time' uniform in the shaders.
     */
    tick: function (t) {
        this.material.uniforms.time.value = t / 1000;
    }
})"))

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
      [:img.logo {:src "/images/big-logo.png"
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

(defn make-cards [info & {:keys [h-size color] :or {h-size :h3
                                                    color 220}}]
  [:ul.card-list (-> (fn [index card]
                       (let [title (:title card)
                             image (:image card)
                             cardType (:types card)]
                         [:li {:key (str cardType title)}
                          [:a.card-link {:href (:url card)}
                           [:div.card {:style
                                       ;; (if image
                                       (if false
                                         #js {"backgroundImage" (str "url(" (:href image) ")")
                                              "backgroundSize" "100%";
                                              "backgroundColor" "black"}
                                         ;; TODO TODO AHHHhhh :) Turn this into a gradient!!!
                                         (let [color-difference 10
                                               l1 (- 50 (* color-difference index))
                                               ;; l2 (- l1 color-difference)
                                               l2 l1
                                               color-1 (str "hsl(" color ", 70%, " l1 "%)")
                                               color-2 (str "hsl(" (+ color color-difference)", 70%, " l2 "%)")]
                                           #js {#_#_"backgroundColor" color-1
                                                "background" (str "linear-gradient(180deg, " color-1 ", " color-2 ")")
                                              #_#_"backgroundColor" (str "hsla(" color ", 70%, " (-  30 (* 5 index)) "%, 0.5)")})
                                         )
                                       }
                            (when cardType [:h6.title-type [:code "["  (string/join ", " cardType )"]"]])
                            ;; TODO muahAHHAHAHA, use index to make some beautiful stylezzzz
                            [h-size {:class "title"} (str  title)]
                            [:div.card-description (:description card)]
                            ]]]))
                     (map-indexed info))])

(defn home-page []
   [:div
    [:div.floating-page.home-page
     responsive-header
     ;; [:div
     ;;  "Some contentttttt"
     ;;  [:div [:a {:href "/about"} "go to the about page"]]
     ;;  "Hello, my name is Andres Cuervo!"]
     (make-cards [{:title "Projects 💻🗂✨"
                   :url "/projects"
                   :description "A collection of links to my some projects - a resumé/portfolio thing."}
                  {:title "Contact ☎️📣📬"
                   :url "/contact"
                   :description "Say hi on the interwebs!"}])]

    ;; TODO : --- Bring back the a-scene below, make it a box, so that you can
    ;; use either your mouse to rotate or on a phone you always see some interesting part of the shader
    ;; but can tell movement is happening :) ---


    ((load-script) "//cdn.rawgit.com/donmccurdy/aframe-physics-system/v2.0.0/dist/aframe-physics-system.min.js" #())
    ;; ((load-script) "https://unpkg.com/aframe-particle-system-component@1.0.x/dist/aframe-particle-system-component.min.js" #())
    (register-inc-on-click)
    (register-thing)
    (register-color-on-click)
    (mouse-camera-rotation)

    (register-glitch-shader)

    ;; Write a CLJS macro to do the inserting of the empty strings at the end of vectors, since it isn't ISeqable so it can't
    ;; pass through the CLJS parser
    [:a-scene {:dangerouslySetInnerHTML {:__html (html
                                                   [:a-entity {:mouse-camera-rotation ""}]
                                                   (let [c "#2EAFAC"
                                                         distance 0.5
                                                         base-y 1.6]
                                                     (map (fn [attrs] [:a-plane (merge attrs {:material-grid-glitch "color: blue;" :color "#2EAFAC"}) ""])
                                                          [{:position (str "0 " base-y " "(- 0 distance)) :rotation "0 0 0"} ;; front
                                                           {:position (str "0 " base-y " " distance) :rotation "0 180 0"} ;; back
                                                           {:position (str distance " " base-y " 0") :rotation "0 -90 0"} ;; right
                                                           {:position (str (- 0 distance) " " base-y " 0") :rotation "0 90 0"} ;; left
                                                           {:position (str "0 " (+ base-y distance) " 0") :rotation "90 0 0"} ;; top
                                                           {:position (str "0 " (- base-y distance) " 0") :rotation "-90 0 0"} ;; bottom
                                                           ]))
                                                   ;; [:a-box#lilBox {:dynamic-body ""
                                                   ;;                 :change-color-on-click ""
                                                   ;;                 :increase-position-on-click "increment: 2; axis: x"
                                                   ;;                 :my-component ""
                                                   ;;                 :position "-2 0 -4"} ""]
                                                   ;; ;; [:a-entity {:position "0 2.25 -15" :particle-system "color: #EF0000,#44CC00"} ""]
                                                   ;; [:a-box {:color "black"
                                                   ;;          :my-component ""
                                                   ;;          :position "1 0 -4"
                                                   ;;          :scale "0.5 2 0"
                                                   ;;          :change-color-on-click ""
                                                   ;;          :increase-position-on-click "axis: y;"
                                                   ;;          } ""]
                                                   ;; [:a-camera
                                                   ;;  [:a-cursor]]
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

(defn projects-page []
   [:div
    [:div.floating-page
     responsive-header
     (make-cards [{:title "Imagine Trees Like These"
                   :types ["Project" "VR"]
                   :url "https://vr.cwervo.com/scenes/itlt/"
                   :description "This was my creative writing capstone project at Oberlin College. I
                                wanted to explore an abstract immersive narrative about nature using VR."
                   :image {:href "images/sun-detail.png" :alt "A screenshot from my AR Medusa refraction experiment"}}
                  {:title "Imagine Trees Like These"
                   :types ["Presentation" "VR"]
                   :url "https://www.youtube.com/watch?v=Ca6quGC_hUk"
                   :description "I gave an in-depth, 16 minute talk about my capstone project."
                   :image {:href "images/capstonetalk-head.png" :alt "Sun detail"}}
                  {:title "A-Frame Workshop"
                   :types ["Presentation" "Workshop" "VR"]
                   :url "https://stefie.github.io/aframe-workshop-berlin/"
                   :description "Stefanie Doll & I organized the first Web XR Meetup in Berlin, at Mozilla's offices.
                                During this meetup we taught an 2-hour introductory workshop on A-Frame."
                   }
                  {:title "vr.cwervo.com"
                   :types ["Website" "VR"]
                   :url "https://vr.cwervo.com"
                   :description "This is my first (& now defunct) VR portfolio website. Feel free to take a look at my old VR (and some AR) projects!"
                   }
                  ])]])

(defn vr-page []
   [:div
    [:div.floating-page
     responsive-header
     (make-cards [{:title "Virtual Reality"}
                  ])]])

(defn contact-page []
   [:div
    [:div.floating-page
     responsive-header
     (make-cards [{:title "Twitter 🐦"
                  :url "https://twitter.com/acwervo/"}
                  {:title "Instagram 📸"
                   :url "https://www.instagram.com/cwervo.gif/"}
                  {:title "E-mail 📩"
                   :url "mailto:acwervo+vr.cwervo.com@gmail.com"}
                  {:title "Github ⌨️"
                   :url "https://github.com/AndresCuervo/"}
                  ]
                 :h-size :h2
                 :color 200)]])

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


(secretary/defroute "/projects/vr" []
  (do
    (reset! page #'vr-page)))

(secretary/defroute "/projects" []
  (do
    (reset! page #'projects-page)))

(secretary/defroute "/contact" []
  (do
    (reset! page #'contact-page)))

(secretary/defroute "/about" []
  (do
    ;; (delete-components)
    ;; (println "wah" (vec @registered-components))
  (reset! page #'about-page)))

;; (secretary/defroute "/thoughts/*thoughts-url" [thoughts-url]
;;   (reset! page #(fn [] (thoughts-notice thoughts-url))))
;;
;; (secretary/defroute "*" []
;;   (do
;;     (reset! page (fn [] [:div " womp womp : 404"]))))

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
