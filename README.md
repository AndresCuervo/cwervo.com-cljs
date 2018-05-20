This is my personal website http://cwervo.com

Note: `copy-files.sh` copies files from Reagent to `/docs`, but you can use Make commands instead:

- Use `make` or `make build` to copy files from reagent into `/docs` to be hosted on Github on next push.
- Use `make serve` to preview the site

Notes about CLJS for A-Frame:
- ~~the CLJS compiler doesn't know to preserve the `el` in calls like `this.el.setAttribute(...)`, but you can get around this by using  the analogous:`this['el'].setAttribute(...)`~~
    - Actualllllly, this is solved by declaring `this.el` as an [extern](https://github.com/AndresCuervo/cwervo.com-cljs/blob/master/externs/aframe.js). Thanks to [this Lispcast post](http://www.lispcast.com/clojurescript-externs) for going through the finer points of externs :)
<!-- - `(aget this "el")` seems to work, over `(.-el this)` -->
<!-- - for some reason, even under a `this-as this`, `(.-el this)` nor `(.-el js/this)` work, (`el` is undefined), but after getting CLJSJS to compile `(aget js/this "el")` works! -->
