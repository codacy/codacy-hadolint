FROM debian:stretch-slim AS builder

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN apt-get update \
 && apt-get install --no-install-recommends -y \
    build-essential=12.3 \
    libffi-dev=3.2.* \
    libgmp-dev=2:6.1.* \
    zlib1g-dev=1:1.2.* \
    curl=7.52.* \
    ca-certificates=* \
    git=1:2.11.* \
    netbase=5.4 \
 && curl -sSL https://get.haskellstack.org/ | sh \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/codacy-hadolint/
COPY codacy-hadolint/stack.yaml codacy-hadolint/package.yaml /opt/codacy-hadolint/
RUN stack --no-terminal --install-ghc test --only-dependencies

COPY codacy-hadolint /opt/codacy-hadolint
RUN stack install --ghc-options="-fPIC"
RUN useradd -ms /bin/bash -u 2004 docker

# COMPRESS WITH UPX
RUN curl -sSL https://github.com/upx/upx/releases/download/v3.94/upx-3.94-amd64_linux.tar.xz \
  | tar -x --xz --strip-components 1 upx-3.94-amd64_linux/upx \
  && ./upx --best --ultra-brute /root/.local/bin/codacy-hadolint

FROM alpine AS distro
LABEL maintainer="João Lopes <lopes@codacy.com>"
COPY --from=builder /root/.local/bin/codacy-hadolint /bin/
COPY --from=builder /etc/passwd/ /etc/passwd
COPY codacy-hadolint/docs/ /docs/
WORKDIR /src/
USER docker
ENTRYPOINT ["/bin/codacy-hadolint"]

