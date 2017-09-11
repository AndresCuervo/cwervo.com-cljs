This is my personal website http://cwervo.com

Notes about CLJS for A-Frame:
- the CLJS compiler doesn't know to preserve the `el` in calls like `this.el.setAttribute(...)`, but you can get around this by using  the analogous:`this['el'].setAttribute(...)`
<!-- - `(aget this "el")` seems to work, over `(.-el this)` -->
<!-- - for some reason, even under a `this-as this`, `(.-el this)` nor `(.-el js/this)` work, (`el` is undefined), but after getting CLJSJS to compile `(aget js/this "el")` works! -->
