FROM openjdk:11

# Install various dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
        curl \
        graphicsmagick \
        imagemagick \
		ffmpeg \
		maven \
		libopenjp2-tools \
		redis-server \
  && rm -rf /var/lib/apt/lists/*

# Install TurboJpegProcessor dependencies
RUN mkdir -p /opt/libjpeg-turbo/lib
COPY docker/test/image_files/libjpeg-turbo/lib64 /opt/libjpeg-turbo/lib

# Install KakaduNativeProcessor & KakaduDemoProcessor dependencies
COPY dist/deps/Linux-x86-64/bin/* /usr/bin/
COPY dist/deps/Linux-x86-64/lib/* /usr/lib/

# A non-root user is needed for some FilesystemSourceTest tests to work.
ARG user=cantaloupe
ARG home=/home/$user
RUN adduser --home $home $user
RUN chown -R $user $home
USER $user
WORKDIR $home

# Add mirrors for maven central since they were offline
# Thanks to https://github.com/Amsterdam/iiif-image-server/pull/23/commits/9a356ce27b44313b28fd62d38ea346a18b3540ce
# TODO: come back in the future and check whether this is still necessary
RUN mkdir ~/.m2 && cp /etc/maven/settings.xml ~/.m2/settings.xml && \
    MIRRORS="\n    <mirror>\n      <id>central<\/id>\n      <name>central<\/name>\n      <url>https:\/\/repo1.maven.org\/maven2<\/url>\n      <mirrorOf>central.maven.org<\/mirrorOf>\n    <\/mirror>\n    <mirror>\n      <id>osgeo-release<\/id>\n      <name>OSGeo Repository<\/name>\n      <url>https:\/\/repo.osgeo.org\/repository\/release\/<\/url>\n      <mirrorOf>osgeo<\/mirrorOf>\n    <\/mirror>\n    <mirror>\n      <id>geoserver-releases<\/id>\n      <name>Boundless Repository<\/name>\n      <url>https:\/\/repo.osgeo.org\/repository\/Geoserver-releases\/<\/url>\n      <mirrorOf>boundless<\/mirrorOf>\n    <\/mirror>\n" && \
    sed -i "s/<mirrors>/<mirrors>\n$MIRRORS/" ~/.m2/settings.xml

# Install application dependencies
COPY ./pom.xml pom.xml
RUN mvn dependency:resolve

# Install Minio S3 server for S3SourceTest and S3CacheTest
ARG s3=$home/s3
RUN mkdir -p $s3/.minio.sys/config $s3/test.cantaloupe.library.illinois.edu
COPY docker/test/image_files/minio_config.json $s3/.minio.sys/config/config.json
RUN curl -O https://dl.minio.io/server/minio/release/linux-amd64/minio
RUN chmod +x minio

# Copy the code
COPY --chown=cantaloupe docker/test/image_files/test.properties test.properties
COPY --chown=cantaloupe ./src src
