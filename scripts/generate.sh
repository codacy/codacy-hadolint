#!/usr/bin/env bash

rm -rf .tmp
mkdir -p .tmp
version=$(cat .hadolint-version)
curl -sSL https://raw.githubusercontent.com/hadolint/hadolint/v${version}/README.md -o .tmp/Hadolint.md
git clone git@github.com:hadolint/hadolint.git .tmp/hadolintRepo && cd ./tmp/hadolintRepo && git checkout v${version} && cd ../..

rm -rf .tmp/hadolintRules
rm -rf .tmp/shellcheckRules
git clone https://github.com/hadolint/hadolint.wiki.git .tmp/markdown/hadolintRules
git clone https://github.com/koalaman/shellcheck.wiki.git .tmp/markdown/shellcheckRules
mkdir -p codacy-hadolint/docs
mkdir -p codacy-hadolint/docs/description


cd docs-generator \
sbt "run ../.tmp docs"

rm -rf ../.tmp
