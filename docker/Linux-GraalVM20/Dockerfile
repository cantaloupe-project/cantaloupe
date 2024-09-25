FROM ubuntu:lunar

ENV JAVA_HOME=/opt/graalvm-ce-java11-20.3.0
ENV GRAALVM_HOME=/opt/graalvm-ce-java11-20.3.0
ENV PATH=$PATH:/opt/graalvm-ce-java11-20.3.0/bin

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
  && rm -rf /var/lib/apt/lists/*

# Install TurboJpegProcessor dependencies
RUN mkdir -p /opt/libjpeg-turbo/lib
COPY docker/image_files/libjpeg-turbo/lib64 /opt/libjpeg-turbo/lib

# Install KakaduNativeProcessor dependencies
COPY dist/deps/Linux-x86-64/lib/* /usr/lib/

# Install GrokProcessor dependencies
#RUN wget -q https://github.com/GrokImageCompression/grok/releases/download/v7.6.5/libgrokj2k1_7.6.5-1_amd64.deb \
#    && wget -q https://github.com/GrokImageCompression/grok/releases/download/v7.6.5/grokj2k-tools_7.6.5-1_amd64.deb \
#    && dpkg -i --ignore-depends=libjpeg62-turbo ./grokj2k-tools_7.6.5-1_amd64.deb
#    && dpkg -i ./libgrokj2k1_7.6.5-1_amd64.deb \

# Install GraalVM
RUN wget -q https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.3.0/graalvm-ce-java11-linux-amd64-20.3.0.tar.gz \
  && tar xfz graalvm-ce-java11-linux-amd64-20.3.0.tar.gz \
  && mv graalvm-ce-java11-20.3.0 /opt

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
