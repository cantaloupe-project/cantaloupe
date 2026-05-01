# Build stage: compile grok JPEG 2000 tools from source.
# libgrokj2k1 and grokj2k-tools are not available in Ubuntu resolute; they
# were dropped from Debian testing due to an unresolved armhf build failure
# (https://bugs.debian.org/1085165) and were never synced into Ubuntu 25.04+.
FROM ubuntu:resolute AS grok-builder

ARG DEBIAN_FRONTEND=noninteractive
ARG GROK_VERSION=20.3.1

RUN apt-get update && apt-get install -y --no-install-recommends \
  ca-certificates \
  cmake \
  g++ \
  git \
  make \
  wget \
  liblcms2-dev \
  libpng-dev \
  libzstd-dev \
  libtiff-dev \
  libjpeg-dev \
  zlib1g-dev \
  libwebp-dev \
  && rm -rf /var/lib/apt/lists/*

# source-full.tar.gz includes all submodules (unlike GitHub's auto-generated archives)
RUN mkdir /grok-src && \
    wget -qO /tmp/grok.tar.gz \
      "https://github.com/GrokImageCompression/grok/releases/download/v${GROK_VERSION}/source-full.tar.gz" && \
    tar xf /tmp/grok.tar.gz -C /grok-src --strip-components=1 && \
    rm /tmp/grok.tar.gz

RUN cmake -B /grok-build -S /grok-src \
      -DCMAKE_BUILD_TYPE=Release \
      -DBUILD_SHARED_LIBS=OFF \
      -DBUILD_TESTING=OFF \
      -DGRK_BUILD_DOC=OFF && \
    cmake --build /grok-build --parallel $(nproc) && \
    cmake --install /grok-build --prefix /grok-install

FROM ubuntu:resolute

ENV JAVA_HOME=/opt/jdk
ENV PATH=$PATH:/opt/jdk/bin:/opt/maven/bin
ARG DEBIAN_FRONTEND=noninteractive

# Install various dependencies:
# * ca-certificates is needed by wget
# * ffmpeg is needed by FfmpegProcessor
# * wget downloads stuff in this dockerfile
# * libopenjp2-tools is needed by OpenJpegProcessor
# * All the rest is needed by GrokProcessor
RUN apt-get update && apt-get install -y --no-install-recommends \
  ca-certificates \
  ffmpeg \
  wget \
  libopenjp2-tools \
  liblcms2-dev \
  libpng-dev \
  libzstd-dev \
  libtiff-dev \
  libjpeg-dev \
  zlib1g-dev \
  libwebp-dev \
  libimage-exiftool-perl \
  adduser \
  openjdk-25-jdk \
  libturbojpeg0 \
  libturbojpeg0-dev \
  maven \
  && rm -rf /var/lib/apt/lists/* \
  && ln -sf "$(dirname $(dirname $(readlink -f /usr/bin/javac)))" /opt/jdk

# Copy grok binaries from the builder stage.
# Built with BUILD_SHARED_LIBS=OFF so libgrokj2k is statically linked into
# the binaries and does not need to be present at runtime.
COPY --from=grok-builder /grok-install/bin/ /usr/local/bin/

# Install KakaduNativeProcessor dependencies
COPY dist/deps/Linux-x86-64/lib/* /usr/lib/

# Ensure libturbojpeg can be found
ENV LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:${LD_LIBRARY_PATH}

# JDK 25 is the default JDK for Ubuntu 26.04 (resolute)

# A non-root user is needed for some FilesystemSourceTest tests to work.
ARG user=cantaloupe
ARG home=/home/$user
RUN adduser --home $home $user
RUN chown -R $user $home
USER $user
WORKDIR $home

# Install application dependencies
COPY ./pom.xml pom.xml
RUN mvn --quiet dependency:resolve

# Copy the code
COPY --chown=cantaloupe docker/image_files/test.properties test.properties
COPY --chown=cantaloupe ./src src

ENTRYPOINT mvn --batch-mode test -Pfreedeps
# Run individual test
# ENTRYPOINT mvn test -e -Dtest=edu.illinois.library.cantaloupe.processor.codec.jpeg.TurboJPEGImageReaderTest
