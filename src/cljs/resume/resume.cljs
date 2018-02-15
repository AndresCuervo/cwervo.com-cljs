(ns resume.resume
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [cljsjs.aframe :as aframe]
            [reagent.core :as reagent :refer [atom]]
            [secretary.core :as secretary :include-macros true]
            [clojure.string :as string]
            [hiccups.runtime :as hiccupsrt]
            [accountant.core :as accountant]))

(enable-console-print!)

(defn resume-page []
  [:div.resume
   [:style {:type "text/css" :media "print"}
    "@page {
      size: auto;
      margin: 0;
    }"]
   [:style
    "

    .resume {
      margin: 0 2%;
    }

    .resume h1,
    .resume h2,
    .resume h3,
    .resume h4,
    .resume h5,
    .resume h6 {
      margin-top: 0.25%;
      margin-bottom: 0.5%;
    }

    .job_description_list {
      border-left: 5px solid #2EAFAC;
      padding-left: 30px
    }

    .underline {
      text-decoration: underline;
    }

    /*
    .print-only {
        display: none;
    }

    @media print {
        .no-print {
            display: none;
        }

        .print-only{
            display: block;
        }
}
    */
    "]

   [:h1 "Andrés Cuervo"]
   [:div.contact-info "acwervo@gmail.com | cwervo.com"]
   [:hr]
   [:h2 "Education"]
   [:div "Bachelor of Arts | Creative Writing, Computer Science (Oberlin College '17)"]
   [:hr]
   [:h2 "Experience"]
   [:div
    (map
     (fn [{:keys [job_name date title description]}]
       [:div {:key (str job_name title)}
        [:h3 job_name]
        [:h4 title]
        [:span (str (:start date) "—" (:end date))]
        [:ul.job_description_list
         (when description
           (map (fn [s] [:li {:key s} s]) description))]
        ])
     [{:job_name "Google (Adecco On-site)"
       :date {:start "02/2018" :end "Current"}
       :title "Prototyper"
       :description ["Converting designs for face-based augmented reality into production-ready prototypes, as well as creating new effects and tools for the creation of AR expereinces."]}
      {:job_name "CircleCI"
       :date {:start "08/2017" :end "02/2018"}
       :title "Software Engineer"
       :description ["As a contractor (01-05, 2017) worked with lead designer to implement new UI & suggest/build UX improvements (web notifications, new site navigation hierarchy)"
                     "Currently working on frontend migration from Clojurescript to React & building new pricing UI"]}
      {:job_name "Brightlite Interactive"
       :date {:start "11/2017" :end "01/2018"}
       :title "AR UX Consultant"
       :description ["Made recommendations about physical layout, lighting conditions, and UX tradeoffs of AR markers"
                     "Provided feedback on structuring & editing the internals of Three & AR.js project for optimal tracking"]}
      {:job_name "Homies.io"
       :date {:start "09/2017" :end "11/2017"}
       :title "VR Programmer"
       :description ["Built an A-FrameVR prototype of a webVR art gallery"
                     "Loaded 360° videos & metadata from a database & created a multi-platform gallery to browse & view the content"]}
      ;; Cutting these last two down here to save space, may be relevant in the future though, or for CV!
      #_{:job_name "Code Now"
       :date {:start "06/2015" :end "08/2015"}
       :title "Web Designer & Developer"
       :description ["Overhauled the design, generation, and deployment of the curriculum website"
                     "Taught Swift & Ruby to high school students over the course of the summer, helped train new volunteer tutors"]}
      #_{:job_name "Oberlin Environmental Dashboard"
       :date {:start "08/2014" :end "06/2016"}
       :title "Designer & Developer"
       :description ["Help design and maintain environmentaldashboard.org using Wordpress and CSS"]}
      ])]
   [:hr]
   [:h2 "Skills"]
   [:div [:span.underline "Languages & frameworks:"] " Clojure & ClojureScript (Om, Om.Next, Reagent, Rum), Javascript (ES6, React, Three.js, A-FrameVR, ReactVR), C++ (OpenFrameworks), Rust (nannou), C# (Unity), Python, Ruby, C, Java"]
   ])
