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

(defn register-home-page-shader []
  (register-component
    "custom-home-page-shader"
    #js {
         :schema #js {:color #js {:type "color"}}
         ;; NOTE: The Google Closure compiler complains about a global use of this
         ;; but it's actual nestesed in this object, it just can't tell that it is,
         ;; so idk lol
         ;;
         ;; Actualllllllly, I just turned off the :global-this Closure warning in
         ;; the project.clj 😬 -- Gotta figure out a better way to get around this tbh
         :init (js* "function () {
                    const data = this.data;

                    var vertexShader = `varying vec2 vUv;

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
                    //gl_FragColor = vec4(mod(vUv , 0.05), sin(time) * color.b, 1.0);
                    gl_FragColor = mix(
                    vec4(mod(vUv , 0.05) * sin(20.0 * time), sin(time) * 0.5, 1.0),
                    //vec4(mod(vUv , 0.05) * 21.0, 0.25, 0.25),
                    //vec4(mod(vUv , 0.45) * 30.0, 1.0, 1.0),
                    vec4(color, 0.7),
                    cos(time)
                    );
                    }`;

                    vertexShader = `
                        varying float noise;
                    uniform float time;

                    // Modulo 289 without a division (only multiplications)
                    vec3 mod289(vec3 x) {
                        return x - floor(x * (1.0 / 289.0)) * 289.0;
                    }

vec4 mod289(vec4 x)
{
  return x - floor(x * (1.0 / 289.0)) * 289.0;
}

                    // Modulo 7 without a division
                    vec3 mod7(vec3 x) {
                        return x - floor(x * (1.0 / 7.0)) * 7.0;
                    }

                    // Permutation polynomial: (34x^2 + x) mod 289
                    vec3 permute(vec3 x) {
                        return mod289((34.0 * x + 1.0) * x);
                    }

                    // https://github.com/hughsk/glsl-noise/blob/master/periodic/3d.glsl
vec4 permute(vec4 x)
{
  return mod289(((x*34.0)+1.0)*x);
}

                    // Cellular noise, returning F1 and F2 in a vec2.
                    // 3x3x3 search region for good F2 everywhere, but a lot
                    // slower than the 2x2x2 version.
                    // The code below is a bit scary even to its author,
                    // but it has at least half decent performance on a
                    // modern GPU. In any case, it beats any software
                    // implementation of Worley noise hands down.

                    vec2 cellular(vec3 P) {
#define K 0.142857142857 // 1/7
#define Ko 0.428571428571 // 1/2-K/2
#define K2 0.020408163265306 // 1/(7*7)
#define Kz 0.166666666667 // 1/6
#define Kzo 0.416666666667 // 1/2-1/6*2
#define jitter 1.0 // smaller jitter gives more regular pattern

                        vec3 Pi = mod289(floor(P));
                        vec3 Pf = fract(P) - 0.5;

                        vec3 Pfx = Pf.x + vec3(1.0, 0.0, -1.0);
                        vec3 Pfy = Pf.y + vec3(1.0, 0.0, -1.0);
                        vec3 Pfz = Pf.z + vec3(1.0, 0.0, -1.0);

                        vec3 p = permute(Pi.x + vec3(-1.0, 0.0, 1.0));
                        vec3 p1 = permute(p + Pi.y - 1.0);
                        vec3 p2 = permute(p + Pi.y);
                        vec3 p3 = permute(p + Pi.y + 1.0);

                        vec3 p11 = permute(p1 + Pi.z - 1.0);
                        vec3 p12 = permute(p1 + Pi.z);
                        vec3 p13 = permute(p1 + Pi.z + 1.0);

                        vec3 p21 = permute(p2 + Pi.z - 1.0);
                        vec3 p22 = permute(p2 + Pi.z);
                        vec3 p23 = permute(p2 + Pi.z + 1.0);

                        vec3 p31 = permute(p3 + Pi.z - 1.0);
                        vec3 p32 = permute(p3 + Pi.z);
                        vec3 p33 = permute(p3 + Pi.z + 1.0);

                        vec3 ox11 = fract(p11*K) - Ko;
                        vec3 oy11 = mod7(floor(p11*K))*K - Ko;
                        vec3 oz11 = floor(p11*K2)*Kz - Kzo; // p11 < 289 guaranteed

                        vec3 ox12 = fract(p12*K) - Ko;
                        vec3 oy12 = mod7(floor(p12*K))*K - Ko;
                        vec3 oz12 = floor(p12*K2)*Kz - Kzo;

                        vec3 ox13 = fract(p13*K) - Ko;
                        vec3 oy13 = mod7(floor(p13*K))*K - Ko;
                        vec3 oz13 = floor(p13*K2)*Kz - Kzo;

                        vec3 ox21 = fract(p21*K) - Ko;
                        vec3 oy21 = mod7(floor(p21*K))*K - Ko;
                        vec3 oz21 = floor(p21*K2)*Kz - Kzo;

                        vec3 ox22 = fract(p22*K) - Ko;
                        vec3 oy22 = mod7(floor(p22*K))*K - Ko;
                        vec3 oz22 = floor(p22*K2)*Kz - Kzo;

                        vec3 ox23 = fract(p23*K) - Ko;
                        vec3 oy23 = mod7(floor(p23*K))*K - Ko;
                        vec3 oz23 = floor(p23*K2)*Kz - Kzo;

                        vec3 ox31 = fract(p31*K) - Ko;
                        vec3 oy31 = mod7(floor(p31*K))*K - Ko;
                        vec3 oz31 = floor(p31*K2)*Kz - Kzo;

                        vec3 ox32 = fract(p32*K) - Ko;
                        vec3 oy32 = mod7(floor(p32*K))*K - Ko;
                        vec3 oz32 = floor(p32*K2)*Kz - Kzo;

                        vec3 ox33 = fract(p33*K) - Ko;
                        vec3 oy33 = mod7(floor(p33*K))*K - Ko;
                        vec3 oz33 = floor(p33*K2)*Kz - Kzo;

                        vec3 dx11 = Pfx + jitter*ox11;
                        vec3 dy11 = Pfy.x + jitter*oy11;
                        vec3 dz11 = Pfz.x + jitter*oz11;

                        vec3 dx12 = Pfx + jitter*ox12;
                        vec3 dy12 = Pfy.x + jitter*oy12;
                        vec3 dz12 = Pfz.y + jitter*oz12;

                        vec3 dx13 = Pfx + jitter*ox13;
                        vec3 dy13 = Pfy.x + jitter*oy13;
                        vec3 dz13 = Pfz.z + jitter*oz13;

                        vec3 dx21 = Pfx + jitter*ox21;
                        vec3 dy21 = Pfy.y + jitter*oy21;
                        vec3 dz21 = Pfz.x + jitter*oz21;

                        vec3 dx22 = Pfx + jitter*ox22;
                        vec3 dy22 = Pfy.y + jitter*oy22;
                        vec3 dz22 = Pfz.y + jitter*oz22;

                        vec3 dx23 = Pfx + jitter*ox23;
                        vec3 dy23 = Pfy.y + jitter*oy23;
                        vec3 dz23 = Pfz.z + jitter*oz23;

                        vec3 dx31 = Pfx + jitter*ox31;
                        vec3 dy31 = Pfy.z + jitter*oy31;
                        vec3 dz31 = Pfz.x + jitter*oz31;

                        vec3 dx32 = Pfx + jitter*ox32;
                        vec3 dy32 = Pfy.z + jitter*oy32;
                        vec3 dz32 = Pfz.y + jitter*oz32;

                        vec3 dx33 = Pfx + jitter*ox33;
                        vec3 dy33 = Pfy.z + jitter*oy33;
                        vec3 dz33 = Pfz.z + jitter*oz33;

                        vec3 d11 = dx11 * dx11 + dy11 * dy11 + dz11 * dz11;
                        vec3 d12 = dx12 * dx12 + dy12 * dy12 + dz12 * dz12;
                        vec3 d13 = dx13 * dx13 + dy13 * dy13 + dz13 * dz13;
                        vec3 d21 = dx21 * dx21 + dy21 * dy21 + dz21 * dz21;
                        vec3 d22 = dx22 * dx22 + dy22 * dy22 + dz22 * dz22;
                        vec3 d23 = dx23 * dx23 + dy23 * dy23 + dz23 * dz23;
                        vec3 d31 = dx31 * dx31 + dy31 * dy31 + dz31 * dz31;
                        vec3 d32 = dx32 * dx32 + dy32 * dy32 + dz32 * dz32;
                        vec3 d33 = dx33 * dx33 + dy33 * dy33 + dz33 * dz33;


                        // Do it right and sort out both F1 and F2
                        vec3 d1a = min(d11, d12);
                        d12 = max(d11, d12);
                        d11 = min(d1a, d13); // Smallest now not in d12 or d13
                        d13 = max(d1a, d13);
                        d12 = min(d12, d13); // 2nd smallest now not in d13
                        vec3 d2a = min(d21, d22);
                        d22 = max(d21, d22);
                        d21 = min(d2a, d23); // Smallest now not in d22 or d23
                        d23 = max(d2a, d23);
                        d22 = min(d22, d23); // 2nd smallest now not in d23
                        vec3 d3a = min(d31, d32);
                        d32 = max(d31, d32);
                        d31 = min(d3a, d33); // Smallest now not in d32 or d33
                        d33 = max(d3a, d33);
                        d32 = min(d32, d33); // 2nd smallest now not in d33
                        vec3 da = min(d11, d21);
                        d21 = max(d11, d21);
                        d11 = min(da, d31); // Smallest now in d11
                        d31 = max(da, d31); // 2nd smallest now not in d31
                        d11.xy = (d11.x < d11.y) ? d11.xy : d11.yx;
                        d11.xz = (d11.x < d11.z) ? d11.xz : d11.zx; // d11.x now smallest
                        d12 = min(d12, d21); // 2nd smallest now not in d21
                        d12 = min(d12, d22); // nor in d22
                        d12 = min(d12, d31); // nor in d31
                        d12 = min(d12, d32); // nor in d32
                        d11.yz = min(d11.yz,d12.xy); // nor in d12.yz
                        d11.y = min(d11.y,d12.z); // Only two more to go
                        d11.y = min(d11.y,d11.z); // Done! (Phew!)
                        return sqrt(d11.xy); // F1, F2
                    }


vec4 taylorInvSqrt(vec4 r)
{
  return 1.79284291400159 - 0.85373472095314 * r;
}

vec3 fade(vec3 t) {
  return t*t*t*(t*(t*6.0-15.0)+10.0);
}

// Classic Perlin noise, periodic variant
float pnoise(vec3 P, vec3 rep)
{
  vec3 Pi0 = mod(floor(P), rep); // Integer part, modulo period
  vec3 Pi1 = mod(Pi0 + vec3(1.0), rep); // Integer part + 1, mod period
  Pi0 = mod289(Pi0);
  Pi1 = mod289(Pi1);
  vec3 Pf0 = fract(P); // Fractional part for interpolation
  vec3 Pf1 = Pf0 - vec3(1.0); // Fractional part - 1.0
  vec4 ix = vec4(Pi0.x, Pi1.x, Pi0.x, Pi1.x);
  vec4 iy = vec4(Pi0.yy, Pi1.yy);
  vec4 iz0 = Pi0.zzzz;
  vec4 iz1 = Pi1.zzzz;

  vec4 ixy = permute(permute(ix) + iy);
  vec4 ixy0 = permute(ixy + iz0);
  vec4 ixy1 = permute(ixy + iz1);

  vec4 gx0 = ixy0 * (1.0 / 7.0);
  vec4 gy0 = fract(floor(gx0) * (1.0 / 7.0)) - 0.5;
  gx0 = fract(gx0);
  vec4 gz0 = vec4(0.5) - abs(gx0) - abs(gy0);
  vec4 sz0 = step(gz0, vec4(0.0));
  gx0 -= sz0 * (step(0.0, gx0) - 0.5);
  gy0 -= sz0 * (step(0.0, gy0) - 0.5);

  vec4 gx1 = ixy1 * (1.0 / 7.0);
  vec4 gy1 = fract(floor(gx1) * (1.0 / 7.0)) - 0.5;
  gx1 = fract(gx1);
  vec4 gz1 = vec4(0.5) - abs(gx1) - abs(gy1);
  vec4 sz1 = step(gz1, vec4(0.0));
  gx1 -= sz1 * (step(0.0, gx1) - 0.5);
  gy1 -= sz1 * (step(0.0, gy1) - 0.5);

  vec3 g000 = vec3(gx0.x,gy0.x,gz0.x);
  vec3 g100 = vec3(gx0.y,gy0.y,gz0.y);
  vec3 g010 = vec3(gx0.z,gy0.z,gz0.z);
  vec3 g110 = vec3(gx0.w,gy0.w,gz0.w);
  vec3 g001 = vec3(gx1.x,gy1.x,gz1.x);
  vec3 g101 = vec3(gx1.y,gy1.y,gz1.y);
  vec3 g011 = vec3(gx1.z,gy1.z,gz1.z);
  vec3 g111 = vec3(gx1.w,gy1.w,gz1.w);

  vec4 norm0 = taylorInvSqrt(vec4(dot(g000, g000), dot(g010, g010), dot(g100, g100), dot(g110, g110)));
  g000 *= norm0.x;
  g010 *= norm0.y;
  g100 *= norm0.z;
  g110 *= norm0.w;
  vec4 norm1 = taylorInvSqrt(vec4(dot(g001, g001), dot(g011, g011), dot(g101, g101), dot(g111, g111)));
  g001 *= norm1.x;
  g011 *= norm1.y;
  g101 *= norm1.z;
  g111 *= norm1.w;

  float n000 = dot(g000, Pf0);
  float n100 = dot(g100, vec3(Pf1.x, Pf0.yz));
  float n010 = dot(g010, vec3(Pf0.x, Pf1.y, Pf0.z));
  float n110 = dot(g110, vec3(Pf1.xy, Pf0.z));
  float n001 = dot(g001, vec3(Pf0.xy, Pf1.z));
  float n101 = dot(g101, vec3(Pf1.x, Pf0.y, Pf1.z));
  float n011 = dot(g011, vec3(Pf0.x, Pf1.yz));
  float n111 = dot(g111, Pf1);

  vec3 fade_xyz = fade(Pf0);
  vec4 n_z = mix(vec4(n000, n100, n010, n110), vec4(n001, n101, n011, n111), fade_xyz.z);
  vec2 n_yz = mix(n_z.xy, n_z.zw, fade_xyz.y);
  float n_xyz = mix(n_yz.x, n_yz.y, fade_xyz.x);
  return 2.2 * n_xyz;
}


                    float turbulence( vec3 p ) {

                        float w = 100.0;
                        float t = -.5;

                        for (float f = 1.0 ; f <= 10.0 ; f++ ){
                            float power = pow( 2.0, f );
                            t += abs( pnoise( vec3( power * p ), vec3( 10.0, 10.0, 10.0 ) ) / power );
                        }

                        return t;

                    }

                    void main() {
                        float turb = 10.0 *  -.10 * turbulence( .9 * normal + vec3(0.0, -0.1 * time, 0.0) );

                        vec3 src = vec3(0.0, -0.1*time/10.0, 0.0);
                        src += position.xyz;
                        vec2 c = cellular( src * 6.0);


                        if (turb < 0.001){
                            c = vec2(turb, turb);
                            c*= 100.0;
                            noise = c.x*10.0;
                        } else {
                            noise = c.x*10.0;
                        }

                        vec3 newPosition = position + normal * (-c.x*.2);
                        gl_Position = projectionMatrix * modelViewMatrix * vec4( newPosition, 1.0 );
                    }
                    `

                    fragmentShader = `
varying float noise;
uniform float time;

vec3 pal( in float t, in vec3 a, in vec3 b, in vec3 c, in vec3 d ) {
    return a + b*cos( 6.28318*(c*t+d) );
}


void main() {
  float val = 1.0 - noise*0.1;

  vec3 color;
  if (noise > 0.0) {
    color = pal( noise*.1, vec3(0.5,0.5,0.5),vec3(0.5,0.5,0.5),vec3(1.0,1.0,0.5),vec3(0.8,0.90,0.30) );
  } else {
    color = pal( noise*.01, vec3(0.5),vec3(1.0),vec3(1.0),vec3(0.0) );

  }
  gl_FragColor = vec4( color.rgb, 1.0 );
}
                        `

                    this.material  = new THREE.ShaderMaterial({
                             side: THREE.BackSide,
                    uniforms: {
                    time: { value: 0.0 },
                    color: { value: new THREE.Color(data.color) },
                    resolution: { value: { x : window.innerWidth, y: window.innerHeight } }
                    },
                    vertexShader,
                    fragmentShader
                    });

                    this.applyToMesh();
                    //this.el.addEventListener('model-loaded', () => this.applyToMesh());
                    }")
         :update (js* "function () {
                      this.material.uniforms.color.value.set(this.data.color);
                      }")
         :applyToMesh (js* "function() {
                           const mesh = this.el.getObject3D('mesh');
                           if (mesh) {
                           mesh.material = this.material;
                           }
                           }" )
         :tick (js* "function (t) {
                    this.material.uniforms.time.value = t / 1000;
                    }")
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

                                       this.el.sceneEl.camera.rotation.y += this.getSpeed(e.clientX, this.data.mouse.x, this.data.speed);
                                       this.el.sceneEl.camera.rotation.x += this.getSpeed(e.clientY, this.data.mouse.x, this.data.speed / 1000);

                                       //this.el.sceneEl.camera.rotation.y += this.getSpeed(e['clientX'], this['data']['mouse']['x'], this['data']['speed']);
                                       //this.el.sceneEl.camera.rotation.x += this.getSpeed(e['clientY'], this['data']['mouse']['x'], this['data']['speed'] / 1000);
                                       this['data']['mouse']['x'] = e['clientX']
                                       this['data']['mouse']['y'] = e['clientY']

                                       //this.data.mouse.x = e.clientX;
                                       //this.data.mouse.y = e.clientY;
                                       }.bind(this))
                                       }")
                           }))

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
                                                    color 240}}]
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
                                         (let [color-difference 3
                                               l1 (- 50 (* color-difference index))
                                               l2 (- l1 15)
                                               ;; l2 l1
                                               color-1 (str "hsl(" color ", 70%, " l1 "%)")
                                               color-2 (str "hsl(" (+ color color-difference)", 70%, " l2 "%)")]
                                           #js {"background" (str "linear-gradient(180deg, " color-1 ", " color-2 ")")})
                                         )
                                       }
                            (when cardType [:h6.title-type [:code "["  (string/join ", " cardType )"]"]])
                            ;; TODO muahAHHAHAHA, use index to make some beautiful stylezzzz
                            [h-size {:class "title"} (str  title)]
                            [:div.card-description (:description card)]
                            ]]]))
                     (map-indexed info))])

(defn toggle-VR []
  (let [style (aget (js/document.querySelector ".floating-page") "style")
        v (aget style "visibility")]
    (aset style "visibility" (condp = v
                               "hidden" "visible"
                               "visible" "hidden"
                               "" "hidden"))))

(def page-toggle-button [:button.eye-button {:title "Show VR mode and make page content transparent"
                                           :onClick toggle-VR} "◉"])

(defn home-page []
   [:div
    page-toggle-button
    [:div.floating-page.home-page
     responsive-header
     [:div.about "Hello there! I’m a software developer, human computer interaction researcher, & VR/AR artist. Currently, I’m at " [:a {:href "https://circleci.com/"} "CircleCI"] " working on frontend development in Clojure & Clojurescript."]
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
    ;; (register-inc-on-click)
    ;; (register-thing)
    ;; (register-color-on-click)

    (mouse-camera-rotation)

    (register-home-page-shader)

    ;; Write a CLJS macro to do the inserting of the empty strings at the end of vectors, since it isn't ISeqable so it can't
    ;; pass through the CLJS parser
    [:a-scene {:dangerouslySetInnerHTML {:__html (html
                                                   [:a-entity {:mouse-camera-rotation ""}]
                                                   [:a-icosahedron {
                                                   ;; [:a-sphere {
                                                                     ;; :rotation "0 90 0"
                                                                     :custom-home-page-shader "color: #2EAFAC;"
                                                                     ;;:dynamic-body ""
                                                                     ;; :change-color-on-click ""
                                                                     ;; :increase-position-on-click "increment: 2; axis: x"
                                                                     ;; :my-component ""
                                                                     :scale "1 1 1"
                                                                     :position "0 1.6 0"} ""]
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
    ;; page-toggle-button
    [:div.floating-page
     responsive-header
     (make-cards [{:title "3D Model Style Transfer Demos"
                   :types ["Project"]
                   :url "https://codepen.io/collection/AGzjZj/"
                   :description [:span "A collection of Codepens showing off the results of my experiments with
                                       applying machine learning style transfer techniques to 3D models. Displayed
                                       using A-Frame."
                                 [:img.card-photo {:src "https://media.giphy.com/media/xT9IgGZnFzjZGVikBW/giphy.gif"}]]
                   }
                  {:title "Imagine Trees Like These"
                   :types ["Project" "VR"]
                   :url "https://vr.cwervo.com/scenes/itlt/"
                   :description "At Oberlin College I majored in Computer Science and Creative Writing, and this project was my creative writing capstone. I
                                wanted to explore an abstract immersive narrative about nature using the new medium of virtual reality."
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
