FROM ubuntu:latest

ARG DEBIAN_FRONTEND=noninteractive

# Install various dependencies

RUN apt-get update && apt-get install -y --no-install-recommends \
        openjdk-11-jdk-headless \
		ffmpeg \
		maven \
		wget \
  		libopenjp2-tools \
    && rm -rf /var/lib/apt/lists/*

# Install TurboJpegProcessor dependencies
RUN mkdir -p /opt/libjpeg-turbo/lib
COPY docker/Linux-JDK11/image_files/libjpeg-turbo/lib64 /opt/libjpeg-turbo/lib

# Install KakaduNativeProcessor dependencies
COPY dist/deps/Linux-x86-64/lib/* /usr/lib/

# Install GrokProcessor dependencies
RUN wget -q https://github.com/GrokImageCompression/grok/releases/download/v7.6.1/libgrokj2k1_7.6.1-1_amd64.deb \
    && wget -q https://github.com/GrokImageCompression/grok/releases/download/v7.6.1/grokj2k-tools_7.6.1-1_amd64.deb \
    && dpkg -i ./libgrokj2k1_7.6.1-1_amd64.deb \
    && dpkg -i --ignore-depends=libjpeg62-turbo ./grokj2k-tools_7.6.1-1_amd64.deb

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
COPY --chown=cantaloupe docker/Linux-JDK11/image_files/test.properties test.properties
COPY --chown=cantaloupe ./src src

ENTRYPOINT mvn --batch-mode test -Pfreedeps
