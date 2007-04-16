#!/bin/bash

# Maven 2 and Emma don't seem to play nicely together, so this
# hacked up but functional little script does the coverage report.

cd "$(dirname "$0")/../.."

emma="$JAVA_HOME/emma/lib/emma.jar"
testng="$(echo "$JAVA_HOME/testng/"testng-*-jdk15.jar)"

# force build
mvn compile resources:testResources compiler:testCompile

mkdir build/emma
cd build/emma

java -cp "$emma" emma instr \
    -ip ../classes \
    -d ../instr-classes

cd ../..

java -cp "build/instr-classes:build/classes:build/test-classes:$emma:$testng" org.testng.TestNG \
    -d build/test-output \
    test/res/testng.xml

mv coverage.ec build/emma
cd build/emma

java -cp "$emma" emma report \
    -r html \
    -in coverage.em \
    -in coverage.ec \
    -sp ../../src/java

