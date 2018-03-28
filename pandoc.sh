#!/bin/sh

exec pandoc \
  --toc \
  --number-sections \
  --section-divs \
  --standalone \
  --filter includes.hs \
  --template template.html \
  -f markdown \
  -t html \
  "$@"

