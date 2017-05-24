FROM openjdk:8-jdk-alpine

# Cantaloupe docker starter script
# For pyramidal tiffs using JAI processor and filesystem resolver and -cache

# Build and run: 
# $ sudo docker build -t cantaloupe .
# $ sudo docker run -d --rm -p 8183:8182 -v ./images:/var/lib/cantaloupe/images --name cantaloupe cantaloupe

ENV CANTALOUPE_VERSION 3.2.2
EXPOSE 8182

RUN apk add --update -X http://dl-cdn.alpinelinux.org/alpine/edge/community \
    curl \
    graphicsmagick

# run non priviledged
#RUN groupadd -r www-data && useradd -r -g www-data cantaloupe
RUN adduser -S cantaloupe

#
# Cantaloupe
#
WORKDIR /tmp
RUN curl -OL https://github.com/medusa-project/cantaloupe/releases/download/v$CANTALOUPE_VERSION/Cantaloupe-$CANTALOUPE_VERSION.zip \
 && mkdir -p /usr/local/ \
 && cd /usr/local \
 && unzip /tmp/Cantaloupe-$CANTALOUPE_VERSION.zip \
 && ln -s Cantaloupe-$CANTALOUPE_VERSION cantaloupe \
 && rm -rf /tmp/Cantaloupe-$CANTALOUPE_VERSION \
 && rm /tmp/Cantaloupe-$CANTALOUPE_VERSION.zip

# upcoming docker releases: use --chown=cantaloupe
COPY cantaloupe.properties /etc/cantaloupe.properties 
RUN mkdir -p /var/log/cantaloupe \
 && mkdir -p /var/cache/cantaloupe \
 && chown -R cantaloupe /var/log/cantaloupe \
 && chown -R cantaloupe /var/cache/cantaloupe \
 && chown cantaloupe /etc/cantaloupe.properties

#
# JAI native medialib (non functional)
#
# ENV JAI_PKG jai-1_1_3-lib-linux-amd64
# RUN curl -OL http://download.java.net/media/jai/builds/release/1_1_3/$JAI_PKG.tar.gz \
#  && tar xvfz /tmp/$JAI_PKG.tar.gz \
#  && cp jai-1_1_3/lib/*.jar $JAVA_HOME/jre/lib/ext/ \
#  && cp jai-1_1_3/lib/*.so $JAVA_HOME/jre/lib/amd64/ \
#  && rm $JAI_PKG.tar.gz \
#  && rm -rf jai-1_1_3

USER cantaloupe 
#VOLUME ["/var/lib/cantaloupe/images", "/var/log/cantaloupe", "/var/cache/cantaloupe"]
CMD ["sh", "-c", "java -Dcantaloupe.config=/etc/cantaloupe.properties -Xmx2g -jar /usr/local/cantaloupe/Cantaloupe-$CANTALOUPE_VERSION.war"]

