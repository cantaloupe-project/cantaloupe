FROM ubuntu:jammy

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH=$PATH:${JAVA_HOME}bin:/opt/maven/bin
ENV LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu
ARG DEBIAN_FRONTEND=noninteractive

# Install various dependencies:
# * ca-certificates is needed by wget
# * ffmpeg is needed by FfmpegProcessor
# * wget download stuffs in this dockerfile
# * libopenjp2-tools is needed by OpenJpegProcessor
# * All the rest is needed by GrokProcessor
RUN apt-get update 
RUN apt-get install -y --no-install-recommends \
  ca-certificates \
  ffmpeg \
  wget \
  liblcms2-dev \
  libpng-dev \
  libzstd-dev \
  libtiff-dev \
  libjpeg-dev \
  zlib1g-dev \
  libwebp-dev \
  libimage-exiftool-perl \
  adduser \
  openjdk-21-jdk \ 
  maven \ 
  #libgrokj2k1 \
 # grokj2k-tools \
  libturbojpeg \
  libturbojpeg-dev 

RUN apt-get install -y libopenjp2-tools 
  
RUN rm -rf /var/lib/apt/lists/*

# Install KakaduNativeProcessor dependencies
COPY dist/deps/Jammy-x86-64/lib/* /usr/lib/

# A non-root user is needed for some FilesystemSourceTest tests to work.
ARG user=cantaloupe
ARG home=/home/$user
RUN adduser --home $home $user
RUN chown -R $user $home
USER $user
WORKDIR $home

COPY ./pom.xml pom.xml
RUN mvn --quiet dependency:resolve

# Copy the code
COPY --chown=cantaloupe docker/image_files/test.properties test.properties
COPY --chown=cantaloupe ./src src

# Use this to run directly from docker:
#COPY --chown=cantaloupe . .
#RUN mvn package -DskipTests
#ENTRYPOINT java -Dcantaloupe.config=./cantaloupe.properties  -Xmx2g -jar target/cantaloupe-6.0-SNAPSHOT.jar

# Use this for automated testing:
ENTRYPOINT mvn --batch-mode test -Pfreedeps -Dtest="!GrokProcessorTest,!Azure*Test"

#ENTRYPOINT mvn test -e -Dtest=edu.illinois.library.cantaloupe.perf.processor.codec.bmp.BMPImageReaderPerformance