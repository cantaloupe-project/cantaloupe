FROM ubuntu:resolute

ENV JAVA_HOME=/opt/jdk
ENV PATH=$PATH:/opt/jdk/bin:/opt/maven/bin
ARG DEBIAN_FRONTEND=noninteractive

# Install various dependencies:
# * ca-certificates is needed by wget
# * ffmpeg is needed by FfmpegProcessor
# * wget download stuffs in this dockerfile
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
  && rm -rf /var/lib/apt/lists/*

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
