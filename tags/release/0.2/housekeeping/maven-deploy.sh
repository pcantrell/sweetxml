#!/bin/bash

function die()
    {
    echo "MAVEN RELEASE FAILED: $1" >&2
    exit 1
    }
    
home="$PWD"

for module in . core-java maven ant; do
    echo
    echo
    echo "Deploying Maven artifacts for $module..."
    echo
    cd "$home/$module" || die "No such dir $home/$module"
    mvn -DperformRelease=true deploy || die "mvn failed"
done
