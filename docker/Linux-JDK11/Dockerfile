FROM openjdk:11

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

# A non-root user is needed for some FilesystemSourceTest tests to work.
ARG user=cantaloupe
ARG home=/home/$user
RUN adduser --home $home $user
RUN chown -R $user $home
USER $user
WORKDIR $home

# Add mirrors for maven central as the one referenced in ImageIO-EXT's POM is
# offline. TODO: come back in the future and check whether this is still necessary
RUN mkdir $home/.m2
COPY docker/Linux-JDK11/image_files/.m2/settings.xml $home/.m2/settings.xml

# Install application dependencies
COPY ./pom.xml pom.xml
RUN mvn --quiet dependency:resolve

# Copy the code
COPY --chown=cantaloupe docker/Linux-JDK11/image_files/test.properties test.properties
COPY --chown=cantaloupe ./src src

ENTRYPOINT mvn test -Pfreedeps
