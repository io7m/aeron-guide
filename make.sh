#!/bin/sh

if [ $# -ne 1 ]
then
  echo "usage: out-dir" 1>&2
  exit 1
fi

if [ -z "${DOCUMENT_PANDOC_TEMPLATE}" ]
then
  echo "DOCUMENT_PANDOC_TEMPLATE is not set" 1>&2
  exit 1
fi

OUTDIR=$1
shift

rm -rfv fragments
mkdir -p fragments

./fragments.sh
touch fragments/fragments.txt

GENERATION_DATE=$(date "+%Y-%m-%dT%H:%M:%S%z")

pandoc \
  --toc \
  --number-sections \
  --section-divs \
  --standalone \
  --filter includes.hs \
  --template "${DOCUMENT_PANDOC_TEMPLATE}" \
  --variable generation_date="${GENERATION_DATE}" \
  -f markdown \
  -t html document.md > "${OUTDIR}/index.xhtml" || exit 1

cp document.css   "${OUTDIR}"
cp normal_nat.png "${OUTDIR}"
