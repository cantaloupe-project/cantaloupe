FROM ubuntu:noble

ENV JAVA_HOME=/opt/graalvm-community-openjdk-21.0.2+13.1
ENV GRAALVM_HOME=/opt/graalvm-community-openjdk-21.0.2+13.1
ENV PATH=$PATH:/opt/graalvm-community-openjdk-21.0.2+13.1/bin

ARG DEBIAN_FRONTEND=noninteractive

# Install various dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
  ffmpeg \
  maven \
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
  libgrokj2k1 \
  grokj2k-tools \
  adduser \
  libturbojpeg \
  libturbojpeg-dev \ 
  && rm -rf /var/lib/apt/lists/*

# Install KakaduNativeProcessor dependencies
COPY dist/deps/Linux-x86-64/lib/* /usr/lib/

# Ensure libturbojpeg can be found
ENV LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:${LD_LIBRARY_PATH}

# Install GrokProcessor dependencies
#RUN wget -q https://github.com/GrokImageCompression/grok/releases/download/v7.6.5/libgrokj2k1_7.6.5-1_amd64.deb \
#    && wget -q https://github.com/GrokImageCompression/grok/releases/download/v7.6.5/grokj2k-tools_7.6.5-1_amd64.deb \
#    && dpkg -i --ignore-depends=libjpeg62-turbo ./grokj2k-tools_7.6.5-1_amd64.deb
#    && dpkg -i ./libgrokj2k1_7.6.5-1_amd64.deb \

# Install GraalVM
RUN wget -q https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.2/graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz \
  && tar xfz graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz \
  && mv graalvm-community-openjdk-21.0.2+13.1 /opt

# A non-root user is needed for some FilesystemSourceTest tests to work.
ARG user=cantaloupe
ARG home=/home/${user}
RUN useradd -m -d "${home}" -s /bin/bash -U "${user}"
USER $user
WORKDIR $home

# Install application dependencies
COPY ./pom.xml pom.xml
RUN mvn --quiet dependency:resolve

# Copy the code
COPY --chown=cantaloupe docker/image_files/test.properties test.properties
COPY --chown=cantaloupe ./src src

ENTRYPOINT mvn --batch-mode test -Pfreedeps