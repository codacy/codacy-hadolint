#!/usr/bin/env bash

mkdir -p .tmp
version=$(cat .hadolint-version)
curl -sSL https://raw.githubusercontent.com/hadolint/hadolint/v${version}/README.md -o .tmp/Hadolint.md
curl -sSL https://raw.githubusercontent.com/hadolint/hadolint/v${version}/src/Hadolint/Rules.hs -o .tmp/Rules.hs
rm -rf .tmp/hadolintRules
rm -rf .tmp/shellcheckRules
git clone https://github.com/hadolint/hadolint.wiki.git .tmp/hadolintRules
git clone https://github.com/koalaman/shellcheck.wiki.git .tmp/shellcheckRules
mkdir -p codacy-hadolint/docs
mkdir -p codacy-hadolint/docs/description

cd docs-generator && \
sbt "run ../.tmp/Hadolint.md ../.tmp/Rules.hs ../codacy-hadolint/docs ../.tmp"

rm -rf .tmp
