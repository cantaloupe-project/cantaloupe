FROM ubuntu:noble

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

# Install various other dependencies that aren't in apt
# Install OpenJDK
#https://download.java.net/java/GA/jdk25.0.2/b1e0dfa218384cb9959bdcb897162d4e/10/GPL/openjdk-25.0.2_linux-x64_bin.tar.gz
RUN wget -q https://download.java.net/java/GA/jdk25.0.2/b1e0dfa218384cb9959bdcb897162d4e/10/GPL/openjdk-25.0.2_linux-x64_bin.tar.gz \
  && tar xfz openjdk-25.0.2_linux-x64_bin.tar.gz \
  && mv jdk-25.0.2 /opt/jdk \
  # Install a newer Maven than the one in apt
  && wget -q https://dlcdn.apache.org/maven/maven-3/3.9.15/binaries/apache-maven-3.9.15-bin.tar.gz \
  && tar xfz apache-maven-3.9.15-bin.tar.gz \
  && mv apache-maven-3.9.15 /opt/maven \
  && rm apache-maven-3.9.15-bin.tar.gz

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
