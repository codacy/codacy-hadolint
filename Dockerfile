ARG TOOL_VERSION=2.12.0

# Development image used to build the codacy-hadolint wrapper
# Explicitly adding go.mod and go.sum avoids re-downloading dependencies on every build
# Go builds static binaries by default, -ldflags="-s -w" strips debug information and reduces the binary size

FROM golang:1.22-alpine3.20 as builder

WORKDIR /src

COPY go.mod go.mod
COPY go.sum go.sum
RUN go mod download

COPY cmd cmd
COPY internal internal
RUN go build -o bin/codacy-hadolint -ldflags="-s -w" ./cmd/tool

# Generate the documentation

FROM sbtscala/scala-sbt:eclipse-temurin-jammy-11.0.22_7_1.9.8_2.13.12 as docs-generator

WORKDIR /app

COPY .tool_version .tool_version
COPY docs docs
COPY docs-generator docs-generator
COPY scripts scripts
RUN ./scripts/generate.sh

# Hadolint official image used to copy the hadolint binary

FROM hadolint/hadolint:$TOOL_VERSION as hadolint-cli

# Compress binaries for smaller image size

FROM alpine:3.20 as compressor

RUN apk add --no-cache upx

COPY --from=builder /src/bin/codacy-hadolint /src/bin/codacy-hadolint
RUN upx --lzma /src/bin/codacy-hadolint

# Final published image for the codacy-hadolint wrapper
# Tries to be as small as possible with only the Go static binary, the docs and the hadolint binary

FROM alpine:3.20

RUN adduser -u 2004 -D docker

COPY --from=docs-generator --chown=docker:docker /app/docs /docs
COPY --from=hadolint-cli /bin/hadolint /usr/bin/hadolint
COPY --from=compressor /src/bin /dist/bin

CMD [ "/dist/bin/codacy-hadolint" ]
