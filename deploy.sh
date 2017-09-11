#!/bin/bash

# From: https://github.com/timothypratley/whip/blob/master/deploy.sh
set -e
lein clean
lein cljsbuild once min
cd resources/public
git init
git add .
git commit -m "Deploy to GitHub Pages"
git push --force --quiet "git@github.com:timothypratley/whip.git" master:gh-pages
rm -fr resources/public/.git
