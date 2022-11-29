FROM debian:buster-20211011-slim AS builder

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN apt-get update \
  && apt-get install --no-install-recommends -y \
  build-essential=12.6 \
  libffi-dev=3.2.* \
  libgmp-dev=2:6.1.* \
  zlib1g-dev=1:1.2.* \
  curl=7.64.* \
  ca-certificates \
  git=1:2.20.* \
  netbase=5.6 \
  && curl -sSL https://get.haskellstack.org/ | sh \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/codacy-hadolint/
COPY codacy-hadolint/stack.yaml codacy-hadolint/package.yaml /opt/codacy-hadolint/
RUN stack --no-terminal --install-ghc test --only-dependencies

COPY codacy-hadolint /opt/codacy-hadolint
RUN stack install --ghc-options="-fPIC"

# COMPRESS WITH UPX
RUN curl -sSL https://github.com/upx/upx/releases/download/v3.94/upx-3.94-amd64_linux.tar.xz \
  | tar -x --xz --strip-components 1 upx-3.94-amd64_linux/upx \
  && ./upx --best --ultra-brute /root/.local/bin/codacy-hadolint

FROM alpine:3.17.0 AS distro
COPY --from=builder /root/.local/bin/codacy-hadolint /bin/
RUN adduser -D -u 2004 docker
COPY codacy-hadolint/docs/ /docs/
RUN ["chown", "-R", "docker:docker", "/docs"]
WORKDIR /src/
USER docker
ENTRYPOINT ["/bin/codacy-hadolint"]

