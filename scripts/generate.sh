mkdir -p .tmp
version=`cat .hadolint-version`
curl -sSL https://raw.githubusercontent.com/hadolint/hadolint/v${version}/README.md -o .tmp/Hadolint.md
mkdir -p codacy-hadolint/docs
mkdir -p codacy-hadolint/docs/description

cd docs-generator && \
sbt "run ../.tmp/Hadolint.md ../codacy-hadolint/docs"