#!/bin/sh
#
# Builds Javadoc for the Java delegate system.
#

if [ $# -lt 1 ]
then
    echo "Usage: build_javadoc.sh <output dir>"
    exit 1
fi

javadoc --ignore-source-errors -d $1 \
    ./src/main/java/edu/illinois/library/cantaloupe/delegate/AbstractJavaDelegate.java \
    ./src/main/java/edu/illinois/library/cantaloupe/delegate/JavaContext.java \
    ./src/main/java/edu/illinois/library/cantaloupe/delegate/JavaDelegate.java \
    ./src/main/java/edu/illinois/library/cantaloupe/delegate/Logger.java
