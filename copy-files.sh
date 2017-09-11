#!/bin/bash
set -e
lein clean
lein cljsbuild once min

rm -rf docs/*
cp -r resources/public/* docs
mv docs/_index.html docs/index.html
cp target/cljsbuild/public/js/app.js docs/js/app.js
