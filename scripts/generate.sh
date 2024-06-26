#!/usr/bin/env bash

mkdir -p .tmp
version=$(cat .tool_version)
curl -sSL https://raw.githubusercontent.com/hadolint/hadolint/v${version}/README.md -o .tmp/Hadolint.md
rm -rf .tmp/hadolintRules
git clone https://github.com/hadolint/hadolint.wiki.git .tmp/hadolintRules
for f in .tmp/hadolintRules/*.md;
    do curl -sSL https://raw.githubusercontent.com/hadolint/hadolint/v${version}/src/Hadolint/Rule/${f:19:6}.hs >> .tmp/Rules.hs
done
#The pattern DL3061 doesn't exist in hadolint.wiki, so we're getting the pattern here
curl -sSL https://raw.githubusercontent.com/hadolint/hadolint/v${version}/src/Hadolint/Rule/DL3061.hs >> .tmp/Rules.hs
rm -rf .tmp/shellcheckRules
git clone https://github.com/koalaman/shellcheck.wiki.git .tmp/shellcheckRules
mkdir -p docs
mkdir -p docs/description

cd docs-generator && \
sbt "run ../.tmp/Hadolint.md ../.tmp/Rules.hs ../docs ../.tmp"

rm -rf .tmp
