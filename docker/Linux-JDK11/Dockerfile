FROM openjdk:11

# Install various dependencies

RUN apt-get update && apt-get install -y --no-install-recommends \
        curl \
		ffmpeg \
		maven \
		wget \
		build-essential \
		cmake \
  		libopenjp2-tools \
  && rm -rf /var/lib/apt/lists/*

# Install TurboJpegProcessor dependencies
RUN mkdir -p /opt/libjpeg-turbo/lib
COPY docker/Linux-JDK11/image_files/libjpeg-turbo/lib64 /opt/libjpeg-turbo/lib

# Install KakaduNativeProcessor dependencies
COPY dist/deps/Linux-x86-64/lib/* /usr/lib/


# Install Grok
RUN     wget -q https://github.com/GrokImageCompression/grok/archive/v7.6.0.tar.gz \
        && tar xzf v7.6.0.tar.gz \
        && cd grok-7.6.0 \
        && cmake . \
        && make -j6 \
        && make install \
        && ldconfig \
        && grk_decompress -h

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
