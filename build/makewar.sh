#!/bin/sh

rm -r target
mvn package -Dmaven.test.skip=true
