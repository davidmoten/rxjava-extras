#!/bin/bash
set -e
mvn site
cd ../davidmoten.github.io
git pull
mkdir -p rxjava-extras
cp -r ../rxjava-extras/target/site/* rxjava-extras/
git add .
git commit -am "update site reports"
git push
