# Codacy-Hadolint Docker Container

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c5d062c19785439980803f4557f9e441)](https://www.codacy.com/gh/codacy/codacy-hadolint?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=codacy/codacy-hadolint&amp;utm_campaign=Badge_Grade)
[![Build Status](https://circleci.com/gh/codacy/codacy-hadolint.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/codacy/codacy-hadolint)
[![Docker Version](https://images.microbadger.com/badges/version/codacy/codacy-hadolint.svg)](https://microbadger.com/images/codacy/codacy-hadolint "Get your own version badge on microbadger.com")

[Docker](https://www.docker.com) container to allow support for [Hadolint](https://github.com/hadolint/hadolint) on Codacy.

## Dockerfile building

To build the Codacy-Hadolint dockerfile :

1. Clone the source:

    ``` sh
	$ git clone https://github.com/codacy/codacy-hadolint.git
	$ cd codacy-hadolint
    ```

2. Build the container:

    ``` sh
    $ docker build -t codacy-hadolint -f Dockerfile .
    ```

## Docs Generation

To update the Hadolint docs :

    $ ./scripts/generate.sh

## Update version
1. Update `.tool_version`

## Docs

[Tool Developer Guide](https://support.codacy.com/hc/en-us/articles/207994725-Tool-Developer-Guide)

[Tool Developer Guide - Using Scala](https://support.codacy.com/hc/en-us/articles/207280379-Tool-Developer-Guide-Using-Scala)

## Test

We use the [codacy-plugins-test](https://github.com/codacy/codacy-plugins-test) to test our external tools integration.
You can follow the instructions there to make sure your tool is working as expected.

## Agent Playbook: Updating This Repository End-to-End

This section is written for an AI coding agent (or a human) tasked with updating this repo — most commonly bumping the wrapped Hadolint version, but also base image / orb / dependency bumps. Follow it top to bottom.

### 1. What this repository is

This is a **Codacy engine**: a thin Go wrapper (`cmd/tool/main.go`, `internal/tool/*.go`, built on [`codacy-engine-golang-seed`](https://github.com/codacy/codacy-engine-golang-seed)) that packages the [Hadolint](https://github.com/hadolint/hadolint) Dockerfile linter (itself written in Haskell) as a Docker image Codacy's platform can run against a customer's Dockerfiles. Hadolint is **not compiled from source** — the final image copies the official prebuilt `hadolint/hadolint:$TOOL_VERSION` binary straight out of that upstream Docker image (see the `hadolint-cli` stage in the `Dockerfile`); the Go wrapper only shells out to it and translates results into Codacy's format.

`docs/` is machine-consumed configuration, similar to other rule-based Codacy engines:

- `docs/patterns.json` — the full list of Hadolint/ShellCheck rules ("patterns") Codacy knows about (IDs like `DL3000...`, `SC1000...`), their category/level/defaults. Generated file, do not hand-edit.
- `docs/description/description.json` + `docs/description/*.md` — one Markdown file per pattern, human-readable description shown in the Codacy UI. Generated file, do not hand-edit.
- `docs/tests/*` and `docs/multiple-tests/*` — fixtures used by `codacy-plugins-test` (`default-patterns`, `without-config-file`, `with-config-file`) to validate real output.
- `docs/tool-description.md` — short blurb about the tool, hand-maintained.

These generated docs come from a **separate Scala/sbt sub-project, `docs-generator/`** (`docs-generator/src/main/scala/GenerateDocs.scala` and `CreateMarkdowns.scala`), driven by `scripts/generate.sh`. That script downloads Hadolint's `README.md` and rule source files (`src/Hadolint/Rule/*.hs`) from the `hadolint/hadolint` GitHub repo at tag `v<TOOL_VERSION>`, plus clones the `hadolint.wiki` and `shellcheck.wiki` repos for rule prose, then runs `sbt run` in `docs-generator/` against those downloaded files to emit `docs/patterns.json` and `docs/description/*`. This means the generator needs **network access, git, and sbt/JVM** installed locally — it is also run automatically inside the `docs-generator` build stage of the `Dockerfile`, so a Docker build alone regenerates the docs baked into the shipped image, but committing the regenerated files to the repo still requires running `./scripts/generate.sh` locally.

### 2. Files that encode versions — check all of these on every update

| File | What it controls | What to check |
|---|---|---|
| `.tool_version` | The Hadolint version: used both as the Docker build `ARG TOOL_VERSION` (which upstream `hadolint/hadolint` image gets copied in) and as the git tag `scripts/generate.sh` downloads docs source from | Bump to the target version (bare number, e.g. `2.12.0`, no leading `v`). Confirm a matching `v<version>` tag/release exists in [hadolint/hadolint](https://github.com/hadolint/hadolint/releases). |
| `Dockerfile` → `ARG TOOL_VERSION` default | Duplicates `.tool_version` as the default build arg | Historically kept in sync with `.tool_version`; check both. |
| `go.mod` → `github.com/codacy/codacy-engine-golang-seed/v6` | Codacy's Go engine SDK | Check for newer versions on GitHub if asked to update it; unrelated to Hadolint version bumps. Run `go get -u ...` then `go mod tidy` if bumping. |
| `Dockerfile` → `golang:1.22-alpine3.20` (builder stage) | Go toolchain used to compile the wrapper binary | Keep in step with `go.mod`'s `go 1.22.3` directive; bump only if asked or if the Go SDK requires it. |
| `Dockerfile` → `sbtscala/scala-sbt:eclipse-temurin-jammy-...` (docs-generator stage) | JVM/sbt used to run the doc generator during the Docker build | Rarely needs touching. |
| `Dockerfile` → final `alpine:3.20` base image | Runtime OS for the shipped image | Bump only if asked, or if a CVE scan flags the current tag (see commit `5ff7fd6` for a precedent — "Use latest alpine for distro image"). |
| `.circleci/config.yml` → `codacy/base` orb | Shared CircleCI steps (checkout, versioning, docker build/publish, tagging) | Check the latest published version in the CircleCI orb registry; `git log -p .circleci/config.yml` shows prior bump history as a fallback reference. |
| `.circleci/config.yml` → `codacy/plugins-test` orb | Runs `codacy-plugins-test` in CI after the image is built | Same as above. |

### 3. Step-by-step update procedure

1. **Bump `.tool_version`** (and the `Dockerfile`'s `ARG TOOL_VERSION` default) to the target Hadolint version, scoped by the task.
2. **Regenerate the docs.** Requires `git`, network access, and `sbt`/JVM on `PATH`: run `./scripts/generate.sh` from the repo root. Review the diff in `docs/patterns.json` and `docs/description/*` for new/removed/renamed rules and stale fixture references in `docs/tests/` and `docs/multiple-tests/`.
3. **Build and vet the Go wrapper**: `go build ./...` and `go vet ./...` from the repo root (module is `github.com/codacy/codacy-hadolint`, Go 1.22).
4. **Build the Docker image**: `docker build -t codacy-hadolint .` — this exercises all four Docker stages (Go builder, docs-generator, hadolint-cli, compressor) and will fail fast if the `.tool_version` tag doesn't exist upstream.
5. **Run `codacy-plugins-test` locally** before pushing — clone https://github.com/codacy/codacy-plugins-test and run its DockerTest commands (this repo's CI runs the multi-test suite via `run_multiple_tests: true`, exercising `docs/tests/` and all three `docs/multiple-tests/*` fixture folders) against your local image tag.
6. **Iterate on failures**, re-running only the relevant test command after each fix.
7. **Commit** the version bump together with the regenerated `docs/` files in one change.
8. **Push and open a PR.** CI (`.circleci/config.yml`) runs `codacy/checkout_and_version` -> `publish_docker_local` (full `docker build` + `docker save`) -> `plugins_test` (multi-test mode) -> `codacy/publish_docker` (master only) -> `tag_version`.
9. **Poll the PR's real CI checks until they all pass — local validation is NOT the finish line.** After every push, run `gh pr checks <pr-url>` and keep re-polling (short sleep while any check is `pending`) until all checks finish. If a check fails, fetch its actual log (the CircleCI job log — don't guess), find the true root cause, fix it, push again (never `--no-verify`, never force-push), and re-poll. Repeat until every check is green. The CI environment's toolchain (Go version in the `golang:1.22-alpine3.20` builder stage, sbt/JVM version in the `docs-generator` stage) can differ subtly from a local setup, so a clean local `docker build` does not guarantee CI passes — it usually does here since CI runs the same `docker build`, but network-fetched inputs (the Hadolint release tag, the wiki repos `scripts/generate.sh` clones) can still behave differently between runs. Only stop iterating when every check passes, or you hit a genuine product/infra decision that needs a human — in which case explain it in the PR rather than guessing.

### 4. Definition of done

- `.tool_version` (and `Dockerfile`'s `ARG TOOL_VERSION` default) bumped to the target version.
- `docs/patterns.json` and `docs/description/*` regenerated via `./scripts/generate.sh` and committed, with any fixture inconsistencies in `docs/tests/`/`docs/multiple-tests/` resolved.
- `go build ./...` and `go vet ./...` pass locally.
- `docker build -t codacy-hadolint .` succeeds.
- `codacy-plugins-test` commands all pass locally against the freshly built image.
- **After pushing and opening/updating the PR, every CI check on it is green.** Poll `gh pr checks <pr-url>` and iterate on any failure (fetch the real CI log, fix, push, re-poll) until all pass — a passing local build is not sufficient on its own; verify against the real CI run (see step 9 above).

## What is Codacy

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features

- Identify new Static Analysis issues
- Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
- Auto-comments on Commits and Pull Requests
- Integrations with Slack, HipChat, Jira, YouTrack
- Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
