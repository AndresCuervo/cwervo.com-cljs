#!/bin/bash

# Inspired by: https://github.com/timothypratley/whip/blob/master/deploy.sh
./copy-files.sh
git add docs

git commit -m "Deploy commit - ${date}"
git push
