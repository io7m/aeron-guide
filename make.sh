#!/bin/sh -ex

if [ $# -ne 1 ]
then
  echo "usage: out-dir" 1>&2
  exit 1
fi

OUTDIR=$1
shift

rm -rfv "${OUTDIR}"
mkdir -p "${OUTDIR}/aeron-guide"

rm -rfv fragments
mkdir -p fragments

./fragments.sh
touch fragments/fragments.txt

export DOCUMENT_TIME=$(head -n 1 "published.txt") || exit 1
export DOCUMENT_TITLE=$(head -n 1 "title.txt")    || exit 1

pandoc \
  --toc \
  --number-sections \
  --section-divs \
  --standalone \
  --filter includes.hs \
  --template document-pandoc-template.html \
  --variable generation_date="${GENERATION_DATE}" \
  -f markdown \
  -t html document.md > "${OUTDIR}/aeron-guide/index.xhtml" || exit 1

cp document.css   "${OUTDIR}"
cp normal_nat.png "${OUTDIR}"

find "${OUTDIR}" -exec touch -d "${DOCUMENT_TIME}" -m --no-create {} \; || exit 1
find "${OUTDIR}" -exec touch -d "${DOCUMENT_TIME}" -a --no-create {} \; || exit 1

pushd "${OUTDIR}"
java ../java/ReproducibleZip.java "aeron-guide" "${DOCUMENT_TIME}" document.zip || exit 1
popd

./sign.sh "${OUTDIR}/document.zip" || exit 1
mv "${OUTDIR}/document.zip" "${OUTDIR}/aeron-guide/document.zip" || exit 1
mv "${OUTDIR}/document.zip.asc" "${OUTDIR}/aeron-guide/document.zip.asc" || exit 1

find "${OUTDIR}" -exec touch -d "${DOCUMENT_TIME}" -m --no-create {} \; || exit 1
find "${OUTDIR}" -exec touch -d "${DOCUMENT_TIME}" -a --no-create {} \; || exit 1

