FROM ubuntu:latest

ENV JAVA_HOME=/opt/graalvm-ce-java11-20.1.0
ENV GRAALVM_HOME=/opt/graalvm-ce-java11-20.1.0
ENV PATH=$PATH:/opt/graalvm-ce-java11-20.1.0/bin

# Install various dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
        curl \
		ffmpeg \
		maven \
		libopenjp2-tools \
  && rm -rf /var/lib/apt/lists/*

# Install TurboJpegProcessor dependencies
RUN mkdir -p /opt/libjpeg-turbo/lib
COPY docker/Linux-JDK11/image_files/libjpeg-turbo/lib64 /opt/libjpeg-turbo/lib

# Install KakaduNativeProcessor dependencies
COPY dist/deps/Linux-x86-64/lib/* /usr/lib/

# Install GraalVM
RUN curl -OL --silent https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.1.0/graalvm-ce-java11-linux-amd64-20.1.0.tar.gz \
  && tar xfz graalvm-ce-java11-linux-amd64-20.1.0.tar.gz \
  && mv graalvm-ce-java11-20.1.0 /opt

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
