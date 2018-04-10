#!/bin/sh

GENERATION_DATE=$(date "+%Y-%m-%dT%H:%M:%S%z")

exec pandoc \
  --toc \
  --number-sections \
  --section-divs \
  --standalone \
  --filter includes.hs \
  --template template.html \
  --variable generation_date="${GENERATION_DATE}" \
  -f markdown \
  -t html \
  "$@"

