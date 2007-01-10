#!/bin/bash

version="$1"
if [ ! -n "$version" ]; then
    echo "ERROR: specify version"
    exit 1
fi

function die()
    {
    echo "MAVEN BUNDLE FAILED: $1" >&2
    exit 1
    }
    
home="$PWD"

bundle="$HOME/innig/tmp/maven-sweetxml-bundles-$version/"
rm -rf "$bundle"
(mkdir "$bundle" &&
sweetxml pom.sxml &&
jar cvf "$bundle/sweetxml-parent-pom-$version-bundle.jar" pom.xml &&
rm pom.xml) || dir "couldn't create parent pom bundle"

for module in core-java maven ant; do
    echo
    echo
    echo "Creating Maven repository bundle for $module..."
    echo
    cd "$home/$module"  || die "No such dir $local_rep"
    cp "$home/LICENSE.html" LICENSE.txt
    mvn source:jar javadoc:jar repository:bundle-create || die "mvn failed"
    mv build/*-"$version"-bundle.jar "$bundle" || die "mv jar failed"
    rm LICENSE.txt
done

cd "$home"
